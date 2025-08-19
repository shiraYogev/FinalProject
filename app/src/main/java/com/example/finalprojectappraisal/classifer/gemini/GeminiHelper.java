package com.example.finalprojectappraisal.classifer.gemini;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiHelper {
    private static final String API_KEY = "AIzaSyDhFwyH9JqdiElWGTKMPBnw_fAYxhk5pYo"; // החליפי ב-API שלך
    private static final Executor executor = Executors.newSingleThreadExecutor();

    public interface ClassificationCallback {
        void onResult(String result);
        void onError(String error);
    }

    public static void classifyImage(Context context, Uri imageUri, String prompt, ClassificationCallback callback) {
        executor.execute(() -> {
            try {
                // 1. טעינת Bitmap מה־Uri (מתאים לכל סוגי קבצים)
                Bitmap bitmap = null;
                if ("file".equals(imageUri.getScheme())) {
                    bitmap = BitmapFactory.decodeFile(imageUri.getPath());
                } else {
                    InputStream in = context.getContentResolver().openInputStream(imageUri);
                    bitmap = BitmapFactory.decodeStream(in);
                    if (in != null) in.close();
                }

                if (bitmap == null) {
                    callback.onError("לא ניתן לטעון תמונה מה־Uri");
                    return;
                }

                // 2. יצירת מודל ג’מיני
                GenerativeModelFutures generativeModel = GenerativeModelFutures.from(
                        new GenerativeModel("gemini-1.5-pro", API_KEY)
                );

                // 3. הכנת Content (תמונה + prompt)
                Content content = new Content.Builder()
                        .addImage(bitmap)
                        .addText(prompt)
                        .build();

                // 4. קריאה ל־Gemini (בלוקינג — אם יש Future/Callback תשתמשי בו!)
                GenerateContentResponse response = generativeModel.generateContent(content).get();

                // 5. תוצאה
                String output = response.getText() != null ? response.getText().trim() : "לא הצלחתי לסווג";
                callback.onResult(output);
            } catch (Exception e) {
                callback.onError("שגיאה בסיווג Gemini: " + e.getMessage());
            }
        });
    }
}

