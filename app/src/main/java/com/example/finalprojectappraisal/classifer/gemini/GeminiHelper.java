package com.example.finalprojectappraisal.classifer.gemini;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.example.finalprojectappraisal.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Stable Gemini helper:
 * - Retries on transient errors (503/429/timeouts/SDK deserialization) with exponential backoff + jitter
 * - Fallback chain between models (2.5-flash -> 2.0-flash -> 1.5-flash)
 * - Preserves the same public API as before
 */
public class GeminiHelper {
    private static final String TAG = "GeminiHelper";
    private static final boolean DEBUG = true; // flip to false in production

    private static final String API_KEY =
            BuildConfig.GEMINI_API_KEY != null ? BuildConfig.GEMINI_API_KEY.trim() : "";

    // single worker for image loading + model call orchestration
    private static final Executor executor = Executors.newSingleThreadExecutor();
    // scheduler for delayed retries
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Retry/fallback settings
    private static final int MAX_RETRIES = 4;          // per model
    private static final long BASE_DELAY_MS = 600;     // initial backoff delay
    private static final Random RNG = new Random();

    // Preferred model order (fast vision → older fallbacks)
    private static final String[] MODEL_CHAIN = new String[]{
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash"
    };

    public interface ClassificationCallback {
        void onResult(String result);
        void onError(String error);
    }

    /** Public entry point: keeps your original signature. */
    public static void classifyImage(Context context, Uri imageUri, String prompt, ClassificationCallback callback) {
        executor.execute(() -> {
            // 0) API key sanity
            if (API_KEY == null || API_KEY.trim().isEmpty()) {
                logE("API key is missing. Did you set GEMINI_API_KEY in local.properties and sync?", null);
                if (callback != null) {
                    callback.onError("API key חסר. ודאי שהגדרת GEMINI_API_KEY ב-local.properties וסנכרנת Gradle.");
                }
                return;
            }
            final String key = API_KEY.trim();
            final String masked = key.substring(0, Math.min(4, key.length())) + "****" + key.substring(Math.max(key.length() - 3, 4));
            logD("API key present? true, masked: " + masked);

            // 1) Kick off attempt with fallback chain
            attemptWithFallback(context, imageUri, prompt, callback, /*modelIndex*/0, /*attempt*/0);
        });
    }

    // ========================= Core retry/fallback flow =========================

    private static void attemptWithFallback(Context ctx,
                                            Uri uri,
                                            String prompt,
                                            ClassificationCallback cb,
                                            int modelIndex,
                                            int attempt) {
        final String modelName = MODEL_CHAIN[Math.min(modelIndex, MODEL_CHAIN.length - 1)];

        // Real single attempt
        callModelOnce(ctx, uri, prompt, modelName, new ClassificationCallback() {
            @Override public void onResult(String rawJson) {
                if (cb != null) cb.onResult(rawJson);
            }

            @Override public void onError(String error) {
                RetryDecision d = classifyErrorForRetry(error);
                // Retry same model?
                if (d.retryable && attempt < MAX_RETRIES) {
                    long delay = backoffWithJitter(attempt);
                    logD("Transient error (" + d.reason + "), retry " + (attempt + 1) + "/" + MAX_RETRIES +
                            " in " + delay + "ms, model=" + modelName);
                    scheduler.schedule(() ->
                                    attemptWithFallback(ctx, uri, prompt, cb, modelIndex, attempt + 1),
                            delay, TimeUnit.MILLISECONDS);
                    return;
                }

                // Fallback to next model?
                if (modelIndex + 1 < MODEL_CHAIN.length) {
                    logD("Switching model due to: " + d.reason + " | Trying fallback " + MODEL_CHAIN[modelIndex + 1]);
                    attemptWithFallback(ctx, uri, prompt, cb, modelIndex + 1, 0);
                    return;
                }

                // Give up
                if (cb != null) cb.onError("Gemini unavailable: " + d.reason + " (after retries & fallbacks)");
            }
        });
    }

    private static long backoffWithJitter(int attempt) {
        long exp = (long) (BASE_DELAY_MS * Math.pow(2, attempt));
        long jitter = RNG.nextInt(250); // up to 250ms random
        return exp + jitter;
    }

    private static RetryDecision classifyErrorForRetry(String err) {
        if (err == null) return new RetryDecision(false, "unknown");
        String s = err.toLowerCase();

        // 503 / overloaded / unavailable
        if (s.contains("503") || s.contains("overloaded") || s.contains("unavailable")) {
            return new RetryDecision(true, "503 UNAVAILABLE/overloaded");
        }
        // 429 / rate limit
        if (s.contains("429") || s.contains("rate limit")) {
            return new RetryDecision(true, "429 rate limit");
        }
        // network/timeout family
        if (s.contains("timeout") || s.contains("timed out") || s.contains("failed to connect")
                || s.contains("connection reset") || s.contains("network")) {
            return new RetryDecision(true, "network/timeout");
        }
        // SDK deserialization issue you saw: missing error.details
        if (s.contains("missingfieldexception") || s.contains("field 'details' is required")) {
            return new RetryDecision(true, "sdk deserialization (details missing)");
        }
        // Default: non-retryable
        return new RetryDecision(false, s);
    }

    private static class RetryDecision {
        final boolean retryable;
        final String reason;
        RetryDecision(boolean r, String reason) { this.retryable = r; this.reason = reason; }
    }

    // ========================= Single attempt implementation =========================

    /** Performs a single model call with the given modelName; extracts raw text and returns. */
    private static void callModelOnce(Context context,
                                      Uri imageUri,
                                      String prompt,
                                      String modelName,
                                      ClassificationCallback callback) {
        try {
            // 1) Load bitmap
            Bitmap bitmap = loadBitmap(context, imageUri);
            if (bitmap == null) {
                String msg = "Failed to load bitmap from Uri: " + imageUri;
                logE(msg, null);
                if (callback != null) callback.onError("לא ניתן לטעון תמונה מה־Uri: " + imageUri);
                return;
            }

            // Request metadata logs
            logD("Model: " + modelName);
            logD("Image Uri: " + safeUri(imageUri));
            logD("Bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight() +
                    ", approxJPEG=" + approxJpegSizeKB(bitmap) + "KB");
            logD("Prompt length: " + (prompt == null ? 0 : prompt.length()));

            // 2) Build model
            GenerativeModelFutures generativeModel =
                    GenerativeModelFutures.from(new GenerativeModel(modelName, API_KEY));

            // 3) Build content
            Content content = new Content.Builder()
                    .addImage(bitmap)
                    .addText(prompt == null ? "" : prompt)
                    .build();

            // 4) Call model (blocking) + handle typical ExecutionException wrapping
            GenerateContentResponse response = generativeModel.generateContent(content).get();

            // 5) Extract text
            String output = response.getText() != null ? response.getText().trim() : null;
            if (output == null || output.isEmpty()) {
                String msg = "Empty response text";
                logE(msg, null);
                if (callback != null) callback.onError("לא התקבלה תשובה מהמודל (Response text ריק)");
            } else {
                if (DEBUG) {
                    logD("Success. First 200 chars: " + output.substring(0, Math.min(200, output.length())));
                }
                if (callback != null) callback.onResult(output);
            }

        } catch (ExecutionException ee) {
            // Unwrap cause for clearer diagnostics
            Throwable cause = (ee.getCause() != null) ? ee.getCause() : ee;
            String chain = buildCauseChain(cause);
            logE("ExecutionException (wrapped): " + chain, cause);
            if (callback != null) callback.onError(chain);

        } catch (Exception e) {
            String chain = buildCauseChain(e);
            logE("General exception: " + chain, e);
            if (callback != null) callback.onError(chain);
        }
    }

    // ========================= Utilities =========================

    private static Bitmap loadBitmap(Context context, Uri imageUri) throws Exception {
        if ("file".equalsIgnoreCase(imageUri.getScheme())) {
            return BitmapFactory.decodeFile(imageUri.getPath());
        } else {
            try (InputStream in = context.getContentResolver().openInputStream(imageUri)) {
                return in == null ? null : BitmapFactory.decodeStream(in);
            }
        }
    }

    private static String safeUri(Uri uri) {
        if (uri == null) return "null";
        return uri.getScheme() + "://" + (uri.getAuthority() != null ? uri.getAuthority() : "") + uri.getPath();
    }

    private static int approxJpegSizeKB(Bitmap bmp) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            return baos.size() / 1024;
        } catch (Throwable t) {
            return -1;
        }
    }

    /** Builds "Class: msg | caused by: Class: msg | ..." to make ExecutionException useful. */
    private static String buildCauseChain(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable cur = t;
        int depth = 0;
        while (cur != null && depth < 8) {
            if (depth > 0) sb.append(" | caused by: ");
            sb.append(cur.getClass().getSimpleName());
            if (cur.getMessage() != null) sb.append(": ").append(cur.getMessage());
            cur = cur.getCause();
            depth++;
        }
        return sb.toString();
    }

    private static void logD(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }

    private static void logE(String msg, Throwable t) {
        if (DEBUG) {
            if (t != null) Log.e(TAG, msg, t);
            else Log.e(TAG, msg);
        } else {
            Log.e(TAG, msg);
        }
    }
}
