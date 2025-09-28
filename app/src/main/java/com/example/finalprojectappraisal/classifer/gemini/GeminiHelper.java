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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiHelper {
    private static final String TAG = "GeminiHelper";
    private static final boolean DEBUG = true; // flip to false in production

    private static final String API_KEY =
            BuildConfig.GEMINI_API_KEY != null ? BuildConfig.GEMINI_API_KEY.trim() : "";

    private static final Executor executor = Executors.newSingleThreadExecutor();

    public interface ClassificationCallback {
        void onResult(String result);
        void onError(String error);
    }

    public static void classifyImage(Context context, Uri imageUri, String prompt, ClassificationCallback callback) {
        executor.execute(() -> {
            // 0) Verify API key & print masked (no secrets in logs)
            if (API_KEY == null || API_KEY.trim().isEmpty()) {
                Log.e(TAG, "API key is missing. Did you set GEMINI_API_KEY in local.properties and sync?");
                callback.onError("API key חסר. ודאי שהגדרת GEMINI_API_KEY ב-local.properties וסנכרנת Gradle.");
                return;
            }
            String key = API_KEY.trim();
            String masked = key.substring(0, Math.min(4, key.length())) + "****" + key.substring(Math.max(key.length() - 3, 4));
            Log.d(TAG, "API key present? true, masked: " + masked);

            final String modelName = "gemini-2.5-flash"; // or "gemini-2.5-pro"

            try {
                // 1) Load bitmap (with logging)
                Bitmap bitmap = loadBitmap(context, imageUri);
                if (bitmap == null) {
                    Log.e(TAG, "Failed to load bitmap from Uri: " + imageUri);
                    callback.onError("לא ניתן לטעון תמונה מה־Uri: " + imageUri);
                    return;
                }

                // Log request metadata
                Log.d(TAG, "Model: " + modelName);
                Log.d(TAG, "Image Uri: " + imageUri); // or use a safeUri(...) helper if you added one
                Log.d(TAG, "Bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight() +
                        ", approxJPEG=" + approxJpegSizeKB(bitmap) + "KB");
                Log.d(TAG, "Prompt length: " + (prompt == null ? 0 : prompt.length()));

                // 2) Create model
                GenerativeModelFutures generativeModel = GenerativeModelFutures.from(
                        new GenerativeModel(modelName, key)
                );

                // 3) Build content
                Content content = new Content.Builder()
                        .addImage(bitmap)
                        .addText(prompt == null ? "" : prompt)
                        .build();

                // 4) Call model (blocking) + detailed error capture
                GenerateContentResponse response = generativeModel.generateContent(content).get();

                // 5) Extract text
                String output = response.getText() != null ? response.getText().trim() : null;
                if (output == null || output.isEmpty()) {
                    Log.e(TAG, "Empty response text");
                    callback.onError("לא התקבלה תשובה מהמודל (Response text ריק)");
                } else {
                    Log.d(TAG, "Success. First 200 chars: " + output.substring(0, Math.min(200, output.length())));
                    callback.onResult(output);
                }

            } catch (ExecutionException ee) {
                // Unwrap cause to see the real server/network error
                Throwable cause = (ee.getCause() != null) ? ee.getCause() : ee;
                String chain = buildCauseChain(cause);
                Log.e(TAG, "ExecutionException (wrapped): " + chain, cause);
                callback.onError("שגיאת Gemini (ExecutionException): " + chain);

            } catch (Exception e) {
                String chain = buildCauseChain(e);
                Log.e(TAG, "General exception: " + chain, e);
                callback.onError("שגיאה בסיווג Gemini: " + chain);
            }
        });
    }

    // ---------- Helpers (logging, bitmap, etc.) ----------

    private static Bitmap loadBitmap(Context context, Uri imageUri) throws Exception {
        // Prefer content resolver (works with SAF, Google Photos, etc.)
        if ("file".equalsIgnoreCase(imageUri.getScheme())) {
            return BitmapFactory.decodeFile(imageUri.getPath());
        } else {
            try (InputStream in = context.getContentResolver().openInputStream(imageUri)) {
                return in == null ? null : BitmapFactory.decodeStream(in);
            }
        }
    }

    private static String safeUri(Uri uri) {
        // Avoid logging sensitive query params
        if (uri == null) return "null";
        return uri.getScheme() + "://" + (uri.getAuthority() != null ? uri.getAuthority() : "") + uri.getPath();
    }

    private static int approxJpegSizeKB(Bitmap bmp) {
        // Rough size estimate for logging only
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            return baos.size() / 1024;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static String buildCauseChain(Throwable t) {
        // Build "Class: message | caused by: Class: message | ..."
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
            // In production you may want to avoid printing stacktraces
            Log.e(TAG, msg);
        }
    }
}
