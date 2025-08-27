// file: .../classifer/gemini/GeminiSummaryService.java
package com.example.finalprojectappraisal.classifer.gemini;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

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

    private static final String API_KEY = "AIzaSyDhFwyH9JqdiElWGTKMPBnw_fAYxhk5pYo";
    private static final Executor EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private GeminiSummaryService() {}

    public static void generatePropertySummary(
            Context context,
            List<Uri> representativeImageUris,
            Map<String, Object> structuredFields,
            SummaryCallback callback
    ) {
        EXEC.execute(() -> {
            try {
                if (API_KEY == null || API_KEY.trim().isEmpty()) {
                    postError(callback, "Missing Gemini API key (BuildConfig.GEMINI_API_KEY).");
                    return;
                }

                GenerativeModelFutures model = GenerativeModelFutures.from(
                        new GenerativeModel("gemini-1.5-pro", API_KEY)
                );

                Content.Builder contentBuilder = new Content.Builder();

                // תמונות (מותר גם בלי תמונות)
                if (representativeImageUris != null) {
                    for (Uri uri : representativeImageUris) {
                        Bitmap bmp = loadBitmap(context, uri);
                        if (bmp != null) contentBuilder.addImage(bmp);
                    }
                }

                // פרומפט + שדות מבניים
                String prompt = GeminiPrompts.PROPERTY_SUMMARY_PROMPT;
                if (structuredFields != null && !structuredFields.isEmpty()) {
                    try {
                        JSONObject sf = new JSONObject(structuredFields);
                        prompt += "\n\nשדות מבניים (לקריאה בלבד, אם קיימים):\n" + sf.toString();
                    } catch (Exception ignore) { /* no-op */ }
                }
                contentBuilder.addText(prompt);

                // קריאה למודל (ברקע)
                GenerateContentResponse response = model.generateContent(contentBuilder.build()).get();
                String output = (response.getText() != null) ? response.getText().trim() : "";

                // פירסור ואימות
                GeminiSummaryParser.Result parsed = GeminiSummaryParser.parse(output);

                // חזרה ל־Main thread
                postSuccess(callback, parsed, output);

            } catch (Exception e) {
                postError(callback, "Gemini summary failed: " + e.getMessage());
            }
        });
    }

    private static void postSuccess(SummaryCallback cb, GeminiSummaryParser.Result res, String raw) {
        if (cb == null) return;
        MAIN.post(() -> cb.onSuccess(res, raw));
    }

    private static void postError(SummaryCallback cb, String msg) {
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
                URL url = new URL(uri.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setInstanceFollowRedirects(true);
                try (InputStream in = conn.getInputStream()) {
                    return BitmapFactory.decodeStream(in);
                } finally {
                    conn.disconnect();
                }
            }
        } catch (Exception ignore) {}
        return null;
    }
}
