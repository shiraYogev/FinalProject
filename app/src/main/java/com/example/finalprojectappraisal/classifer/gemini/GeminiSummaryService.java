// file: .../classifer/gemini/GeminiSummaryService.java
package com.example.finalprojectappraisal.classifer.gemini;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.vertexai.FirebaseVertexAI;
import com.google.firebase.vertexai.java.GenerativeModelFutures;
import com.google.firebase.vertexai.type.Content;
import com.google.firebase.vertexai.type.GenerateContentResponse;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class GeminiSummaryService {

    public interface SummaryCallback {
        void onSuccess(GeminiSummaryParser.Result result, String rawJson);
        void onError(String message);
    }

    private static final String TAG = "GeminiSummaryService";

    private static final Executor EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private GeminiSummaryService() {}

    public static void generatePropertySummary(
            Context context,
            List<Uri> representativeImageUris,
            Map<String, Object> structuredFields,
            SummaryCallback callback
    ) {
        Log.d(TAG, "generatePropertySummary() called. imagesCount="
                + (representativeImageUris == null ? 0 : representativeImageUris.size())
                + ", structuredFieldsCount=" + (structuredFields == null ? 0 : structuredFields.size()));

        EXEC.execute(() -> {
            try {
                Log.d(TAG, "Creating GenerativeModel: gemini-2.5-flash (Firebase Vertex AI)");
                GenerativeModelFutures model = GenerativeModelFutures.from(
                        FirebaseVertexAI.getInstance().generativeModel("gemini-2.5-flash")
                );

                Content.Builder contentBuilder = new Content.Builder();

                // תמונות (מותר גם בלי תמונות)
                if (representativeImageUris != null) {
                    for (Uri uri : representativeImageUris) {
                        Log.d(TAG, "Loading bitmap from uri: " + uri);
                        Bitmap bmp = loadBitmap(context, uri);
                        if (bmp != null) {
                            Log.d(TAG, "Bitmap loaded OK: " + uri + " size=" + bmp.getWidth() + "x" + bmp.getHeight());
                            contentBuilder.addImage(bmp);
                        } else {
                            Log.w(TAG, "Bitmap load failed, skipping: " + uri);
                        }
                    }
                } else {
                    Log.d(TAG, "No images passed (representativeImageUris is null)");
                }

                // פרומפט + שדות מבניים
                String prompt = GeminiPrompts.PROPERTY_SUMMARY_PROMPT;
                if (structuredFields != null && !structuredFields.isEmpty()) {
                    try {
                        JSONObject sf = new JSONObject(structuredFields);
                        String sfJson = sf.toString();
                        Log.d(TAG, "structuredFields JSON length=" + sfJson.length());
                        prompt += "\n\nשדות מבניים (לקריאה בלבד, אם קיימים):\n" + sfJson;
                    } catch (Exception jsonEx) {
                        Log.w(TAG, "structuredFields to JSON failed: " + jsonEx.getMessage());
                    }
                } else {
                    Log.d(TAG, "No structuredFields or empty map");
                }
                Log.d(TAG, "Prompt built. length=" + (prompt == null ? 0 : prompt.length()));
                contentBuilder.addText(prompt);

                // קריאה למודל (ברקע)
                Log.d(TAG, "Calling model.generateContent(...)");
                GenerateContentResponse response = model.generateContent(contentBuilder.build()).get();
                String output = (response.getText() != null) ? response.getText().trim() : "";
                Log.d(TAG, "Model response received. hasText=" + (output.length() > 0) + ", textLen=" + output.length());

                // פירסור ואימות
                GeminiSummaryParser.Result parsed = GeminiSummaryParser.parse(output);
                Log.d(TAG, "Parser returned. parsed != null ? " + (parsed != null));

                // חזרה ל־Main thread
                postSuccess(callback, parsed, output);

            } catch (Exception e) {
                Log.e(TAG, "Gemini summary failed", e);
                postError(callback, "Gemini summary failed: " + e.getMessage());
            }
        });
    }

    private static void postSuccess(SummaryCallback cb, GeminiSummaryParser.Result res, String raw) {
        Log.d(TAG, "postSuccess() called. rawLen=" + (raw == null ? 0 : raw.length()));
        if (cb == null) return;
        MAIN.post(() -> cb.onSuccess(res, raw));
    }

    private static void postError(SummaryCallback cb, String msg) {
        Log.e(TAG, "postError(): " + msg);
        if (cb == null) return;
        MAIN.post(() -> cb.onError(msg));
    }

    // תמיכה ב-file/content/http/https (לא gs://)
    private static Bitmap loadBitmap(Context context, Uri uri) {
        try {
            String scheme = uri.getScheme();
            if ("file".equalsIgnoreCase(scheme)) {
                return BitmapFactory.decodeFile(uri.getPath());
            } else if ("content".equalsIgnoreCase(scheme)) {
                try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                    return in != null ? BitmapFactory.decodeStream(in) : null;
                }
            } else if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                HttpURLConnection conn = (HttpURLConnection) new URL(uri.toString()).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setInstanceFollowRedirects(true);
                try (InputStream in = conn.getInputStream()) {
                    return BitmapFactory.decodeStream(in);
                } finally {
                    conn.disconnect();
                }
            } else {
                Log.w(TAG, "Unsupported URI scheme: " + scheme + " for " + uri);
            }
        } catch (Exception e) {
            Log.w(TAG, "loadBitmap failed for " + uri + ": " + e.getMessage());
        }
        return null;
    }
}
