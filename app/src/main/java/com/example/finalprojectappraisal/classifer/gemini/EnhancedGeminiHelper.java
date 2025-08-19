package com.example.finalprojectappraisal.classifer.gemini;

import android.content.Context;
import android.net.Uri;

import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.model.Image;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Map;
import java.util.UUID;

/**
 * Flow:
 *  GeminiHelper.classifyImage → GeminiJsonParser (JSON) →
 *  update Image (description + aiClassifications) →
 *  save image (/images/<imageId>) → update project fields
 */
public class EnhancedGeminiHelper {

    public interface EnhancedClassificationCallback {
        void onResult(String rawResult, Map<String, String> parsedDisplayKv);
        void onError(String error);
        void onSavedToDatabase();
    }

    /** בוחר פרומפט לפי קטגוריית התמונה */
    public static void classifyImageAndSave(Context context,
                                            Uri imageUri,
                                            String projectId,
                                            Image image,
                                            EnhancedClassificationCallback callback) {
        String prompt = getPromptByCategory(image.getCategory());
        classifyImageAndSave(context, imageUri, projectId, image, prompt, callback);
    }

    /** גרסה שמעבירים לה פרומפט מפורש (למשל מה-ImageCategorySection) */
    public static void classifyImageAndSave(Context context,
                                            Uri imageUri,
                                            String projectId,
                                            Image image,
                                            String prompt,
                                            EnhancedClassificationCallback callback) {

        ensureImageId(image); // לוודא שיש imageId (ה-Repository משתמש בו במסמך)

        GeminiHelper.classifyImage(context, imageUri, prompt, new GeminiHelper.ClassificationCallback() {
            @Override
            public void onResult(String rawResult) {
                try {
                    // 1) מפה לשדות פרויקט (מפתחות Firestore)
                    Map<String, Object> fieldsToUpdate =
                            GeminiJsonParser.parseToProjectFields(image.getCategory(), rawResult);

                    // 2) תיאור + מפה עברית ל-UI + aiClassifications
                    String description =
                            GeminiJsonParser.buildDescription(image.getCategory(), rawResult);
                    Map<String, String> displayKv =
                            GeminiJsonParser.toDisplayMap(image.getCategory(), rawResult);

                    image.setDescription(description);
                    image.setVerified(true);
                    image.getAiClassifications().putAll(
                            GeminiJsonParser.extractAiClassifications(image.getCategory(), rawResult)
                    );

                    // 3) שמירה: קודם תמונה, אחר כך עדכון השדות בפרויקט
                    saveImageThenUpdateProject(projectId, image, fieldsToUpdate, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(Task<Void> task) {
                            if (task.isSuccessful()) {
                                if (callback != null) {
                                    callback.onSavedToDatabase();
                                    callback.onResult(rawResult, displayKv);
                                }
                            } else {
                                if (callback != null) {
                                    String msg = (task.getException() != null)
                                            ? task.getException().getMessage()
                                            : "Unknown error";
                                    callback.onError("Failed to save: " + msg);
                                }
                            }
                        }
                    });

                } catch (Exception e) {
                    if (callback != null) callback.onError("Error processing result: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    /** מוסיף/שומר את התמונה ואז מעדכן את שדות הפרויקט */
    private static void saveImageThenUpdateProject(String projectId,
                                                   Image image,
                                                   Map<String, Object> fieldsToUpdate,
                                                   OnCompleteListener<Void> listener) {
        ProjectRepository repo = ProjectRepository.getInstance();

        // 1) שמירת התמונה במסמך המשנה /images/<imageId>
        repo.addImageToProject(projectId, image, addTask -> {
            if (!addTask.isSuccessful()) {
                if (listener != null) listener.onComplete(addTask);
                return;
            }

            // 2) עדכון שדות הפרויקט (אם יש מה לעדכן)
            if (fieldsToUpdate == null || fieldsToUpdate.isEmpty()) {
                if (listener != null) listener.onComplete(addTask); // הצלחה ללא עדכון נוסף
                return;
            }

            repo.updateMultipleFields(projectId, fieldsToUpdate, listener);
        });
    }

    /** בחירת פרומפט לפי קטגוריה */
    private static String getPromptByCategory(Image.Category category) {
        switch (category) {
            case ENTRANCE_DOOR: return GeminiPrompts.ENTRANCE_DOOR_PROMPT;
            case KITCHEN:       return GeminiPrompts.KITCHEN_PROMPT;
            case LIVING_ROOM:   return GeminiPrompts.LIVING_ROOM_PROMPT;
            case BEDROOM:       return GeminiPrompts.BEDROOM_PROMPT;
            case BATHROOM:      return GeminiPrompts.BATHROOM_PROMPT;
            default:            return GeminiPrompts.FLOORING_PROMPT;
        }
    }

    /** אם חסר id בתמונה, ניצור UUID (בדרך־כלל יש כבר מהבנאי של Image) */
    private static void ensureImageId(Image image) {
        if (image.getId() == null || image.getId().trim().isEmpty()) {
            image.setId(UUID.randomUUID().toString());
        }
    }
}