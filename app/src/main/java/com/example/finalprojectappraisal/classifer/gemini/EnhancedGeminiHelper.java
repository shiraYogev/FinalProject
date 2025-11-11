package com.example.finalprojectappraisal.classifer.gemini;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Image;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Flow:
 *  GeminiHelper.classifyImage → GeminiJsonParser (JSON) →
 *  update Image (description + aiClassifications) →
 *  save image (/images/<imageId>) → update project fields
 */
public class EnhancedGeminiHelper {

    private static final String TAG = "EnhancedGeminiHelper";
    private static final boolean DEBUG = true;

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

        // traceId שמאפשר לעקוב בכל השרשרת (UI→Helper→Repo)
        String traceId = makeTraceId(projectId, image.getId());

        logD(traceId, "START classifyImageAndSave | projectId=" + projectId +
                " imageId=" + image.getId() + " category=" + image.getCategory() +
                " uri=" + safeUri(imageUri) + " promptLen=" + (prompt == null ? 0 : prompt.length()));

        GeminiHelper.classifyImage(context, imageUri, prompt, new GeminiHelper.ClassificationCallback() {
            @Override
            public void onResult(String rawResult) {
                logD(traceId, "Gemini onResult (len=" + (rawResult == null ? 0 : rawResult.length()) + ")");
                try {
                    // 1) מפה לשדות פרויקט (מפתחות Firestore)
                    Map<String, Object> fieldsToUpdate =
                            GeminiJsonParser.parseToProjectFields(image.getCategory(), rawResult);
                    logD(traceId, "parseToProjectFields size=" + (fieldsToUpdate == null ? 0 : fieldsToUpdate.size()));

                    // 2) תיאור + מפה עברית ל-UI + aiClassifications
                    String description =
                            GeminiJsonParser.buildDescription(image.getCategory(), rawResult);
                    Map<String, String> displayKv =
                            GeminiJsonParser.toDisplayMap(image.getCategory(), rawResult);
                    logD(traceId, "displayKv size=" + (displayKv == null ? 0 : displayKv.size()));

                    image.setDescription(description);
                    image.setVerified(true);
                    image.getAiClassifications().putAll(
                            GeminiJsonParser.extractAiClassifications(image.getCategory(), rawResult)
                    );
                    logD(traceId, "aiClassifications now size=" + image.getAiClassifications().size());

                    // 3) שמירה: קודם תמונה, אחר כך עדכון השדות בפרויקט
                    saveImageThenUpdateProject(projectId, image, fieldsToUpdate, traceId, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(Task<Void> task) {
                            if (task.isSuccessful()) {
                                logD(traceId, "SAVE OK: image saved and project fields updated (" +
                                        (fieldsToUpdate == null ? 0 : fieldsToUpdate.size()) + " fields)");
                                if (callback != null) {
                                    callback.onSavedToDatabase();
                                    callback.onResult(rawResult, displayKv);
                                }
                            } else {
                                String msg = (task.getException() != null)
                                        ? task.getException().getMessage()
                                        : "Unknown error";
                                logE(traceId, "SAVE FAILED: " + msg, task.getException());
                                if (callback != null) {
                                    callback.onError("Failed to save: " + msg);
                                }
                            }
                        }
                    });

                } catch (Exception e) {
                    logE(traceId, "Error processing result: " + e.getMessage(), e);
                    if (callback != null) callback.onError("Error processing result: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                logE(traceId, "Gemini onError: " + error, null);
                if (callback != null) callback.onError(error);
            }
        });
    }

    /** מוסיף/שומר את התמונה ואז מעדכן את שדות הפרויקט */
    private static void saveImageThenUpdateProject(String projectId,
                                                   Image image,
                                                   Map<String, Object> fieldsToUpdate,
                                                   String traceId,
                                                   OnCompleteListener<Void> listener) {
        ProjectRepository repo = ProjectRepository.getInstance();

        logD(traceId, "addImageToProject → projectId=" + projectId + " imageId=" + image.getId());
        repo.addImageToProject(projectId, image, addTask -> {
            if (!addTask.isSuccessful()) {
                String msg = addTask.getException() != null ? addTask.getException().getMessage() : "Unknown error";
                logE(traceId, "addImageToProject FAILED: " + msg, addTask.getException());
                if (listener != null) listener.onComplete(addTask);
                return;
            }
            logD(traceId, "addImageToProject OK");

            // 2) עדכון שדות הפרויקט (אם יש מה לעדכן)
            if (fieldsToUpdate == null || fieldsToUpdate.isEmpty()) {
                logD(traceId, "No project fields to update. Done.");
                if (listener != null) listener.onComplete(addTask); // הצלחה ללא עדכון נוסף
                return;
            }

            logD(traceId, "updateMultipleFields → fields=" + fieldsToUpdate.keySet());
            repo.updateMultipleFields(projectId, fieldsToUpdate, task -> {
                if (!task.isSuccessful()) {
                    String msg2 = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    logE(traceId, "updateMultipleFields FAILED: " + msg2, task.getException());
                } else {
                    logD(traceId, "updateMultipleFields OK");
                }
                if (listener != null) listener.onComplete(task);
            });
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

    private static String safeUri(Uri uri) {
        if (uri == null) return "null";
        String auth = (uri.getAuthority() != null ? uri.getAuthority() : "");
        String path = (uri.getPath() != null ? uri.getPath() : "");
        return uri.getScheme() + "://" + auth + path;
    }

    private static String makeTraceId(String projectId, String imageId) {
        String p = projectId == null ? "NA" : projectId.substring(Math.max(0, projectId.length() - 6));
        String i = imageId == null ? "NA" : imageId.substring(Math.max(0, imageId.length() - 6));
        return "EGH-" + p + "-" + i + "-" + System.currentTimeMillis()%100000;
    }

    private static void logD(String tid, String msg) {
        if (DEBUG) Log.d(TAG, "[" + tid + "] " + msg);
    }

    private static void logE(String tid, String msg, Throwable t) {
        if (DEBUG) {
            if (t != null) Log.e(TAG, "[" + tid + "] " + msg, t);
            else Log.e(TAG, "[" + tid + "] " + msg);
        } else {
            Log.e(TAG, "[" + tid + "] " + msg);
        }
    }
}
