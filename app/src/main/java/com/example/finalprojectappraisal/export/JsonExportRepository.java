package com.example.finalprojectappraisal.export;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository שמטפל בשמירת ה-JSON כתת-אוסף (subcollection) תחת פרויקט.
 * המבנה: projects/{projectId}/json_exports/{exportId}
 * בדיוק כמו: projects/{projectId}/images/{imageId}
 */
public class JsonExportRepository {

    private static final String TAG = "JsonExportRepository";
    private static final String PROJECTS_COLLECTION = "projects";
    private static final String JSON_EXPORTS_SUBCOLLECTION = "json_exports";

    private final FirebaseFirestore db;

    public JsonExportRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public JsonExportRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * שומר את ה-JSON כתת-אוסף תחת הפרויקט.
     * המבנה: projects/{projectId}/json_exports/{exportId}
     * בדיוק כמו: projects/{projectId}/images/{imageId}
     *
     * @param projectId מזהה הפרויקט
     * @param jsonObject אובייקט JSON לשמירה
     * @param listener קולבק לתוצאה
     */
    public void saveJsonExport(String projectId, JSONObject jsonObject, OnCompleteListener<Void> listener) {
        if (projectId == null || projectId.trim().isEmpty()) {
            if (listener != null) {
                listener.onComplete(Tasks.forException(new IllegalArgumentException("Project ID cannot be empty")));
            }
            return;
        }

        if (jsonObject == null) {
            if (listener != null) {
                listener.onComplete(Tasks.forException(new IllegalArgumentException("JSON object cannot be null")));
            }
            return;
        }

        try {
            // יצירת מפת נתונים לשמירה
            Map<String, Object> exportData = new HashMap<>();

            // המרת JSONObject למפה
            Map<String, Object> jsonMap = jsonToMap(jsonObject);
            exportData.put("json_data", jsonMap);

            // מטאדאטה
            exportData.put("project_id", projectId);
            exportData.put("export_timestamp", System.currentTimeMillis());
            exportData.put("export_status", "completed");
            exportData.put("version", "1.0");

            // 🔄 שמירה לתת-אוסף: projects/{projectId}/json_exports/latest
            // שימוש ב-ID קבוע כדי שכל ייצוא חוזר יעדכן את אותו מסמך
            DocumentReference docRef = db.collection(PROJECTS_COLLECTION)
                    .document(projectId)
                    .collection(JSON_EXPORTS_SUBCOLLECTION)
                    .document("visit_json");

            docRef.set(exportData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "JSON export saved to subcollection: " + docRef.getPath());
                        } else {
                            Log.e(TAG, "Failed to save JSON export for project: " + projectId, task.getException());
                        }
                        if (listener != null) {
                            listener.onComplete(task);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing JSON export data", e);
            if (listener != null) {
                listener.onComplete(Tasks.forException(e));
            }
        }
    }

    /**
     * שומר JSON גולמי (String) לתת-אוסף תחת הפרויקט
     */
    public void saveJsonExportRaw(String projectId, String jsonString, OnCompleteListener<Void> listener) {
        if (projectId == null || projectId.trim().isEmpty()) {
            if (listener != null) {
                listener.onComplete(Tasks.forException(new IllegalArgumentException("Project ID cannot be empty")));
            }
            return;
        }

        Map<String, Object> exportData = new HashMap<>();
        exportData.put("json_raw", jsonString != null ? jsonString : "");
        exportData.put("project_id", projectId);
        exportData.put("export_timestamp", System.currentTimeMillis());
        exportData.put("export_status", "completed");
        exportData.put("version", "1.0");

        // שמירה לתת-אוסף
        DocumentReference docRef = db.collection(PROJECTS_COLLECTION)
                .document(projectId)
                .collection(JSON_EXPORTS_SUBCOLLECTION)
                .document("latest");
        docRef.set(exportData).addOnCompleteListener(listener);
    }

    /**
     * מעדכן סטטוס ייצוא קיים בתת-אוסף
     */
    public void updateExportStatus(String projectId, String exportId, String status, OnCompleteListener<Void> listener) {
        if (projectId == null || projectId.trim().isEmpty() || exportId == null || exportId.trim().isEmpty()) {
            if (listener != null) {
                listener.onComplete(Tasks.forException(new IllegalArgumentException("Project ID and Export ID cannot be empty")));
            }
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("export_status", status);
        updates.put("last_update", System.currentTimeMillis());

        DocumentReference docRef = db.collection(PROJECTS_COLLECTION)
                .document(projectId)
                .collection(JSON_EXPORTS_SUBCOLLECTION)
                .document(exportId);
        docRef.update(updates).addOnCompleteListener(task -> {
            if (listener != null) {
                listener.onComplete(task);
            }
        });
    }

    /**
     * בודק האם קיים ייצוא כלשהו לפרויקט (בתת-אוסף)
     */
    public void hasExport(String projectId, OnCompleteListener<Boolean> listener) {
        if (projectId == null || projectId.trim().isEmpty()) {
            if (listener != null) {
                listener.onComplete(Tasks.forResult(false));
            }
            return;
        }

        // בדיקה האם יש מסמכים בתת-אוסף json_exports
        db.collection(PROJECTS_COLLECTION)
                .document(projectId)
                .collection(JSON_EXPORTS_SUBCOLLECTION)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    boolean hasExports = task.isSuccessful() &&
                            task.getResult() != null &&
                            !task.getResult().isEmpty();
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(hasExports));
                    }
                });
    }

    /**
     * מוחק ייצוא ספציפי מתת-אוסף
     */
    public void deleteExport(String projectId, String exportId, OnCompleteListener<Void> listener) {
        if (projectId == null || projectId.trim().isEmpty() || exportId == null || exportId.trim().isEmpty()) {
            if (listener != null) {
                listener.onComplete(Tasks.forException(new IllegalArgumentException("Project ID and Export ID cannot be empty")));
            }
            return;
        }

        db.collection(PROJECTS_COLLECTION)
                .document(projectId)
                .collection(JSON_EXPORTS_SUBCOLLECTION)
                .document(exportId)
                .delete()
                .addOnCompleteListener(listener);
    }

    /**
     * ממיר JSONObject למפת HashMap רקורסיבית
     */
    private Map<String, Object> jsonToMap(JSONObject json) throws Exception {
        Map<String, Object> map = new HashMap<>();
        java.util.Iterator<String> keys = json.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);

            if (value instanceof JSONObject) {
                map.put(key, jsonToMap((JSONObject) value));
            } else {
                map.put(key, value);
            }
        }

        return map;
    }

    /**
     * מחזיר את שם תת-האוסף (לשימוש חיצוני)
     */
    public static String getSubcollectionName() {
        return JSON_EXPORTS_SUBCOLLECTION;
    }
}
