package com.example.finalprojectappraisal.database.updater;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.database.updater.ProjectFieldUpdater;
import com.example.finalprojectappraisal.model.Client;
import com.example.finalprojectappraisal.model.Project;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager class responsible for handling all project update operations.
 * Centralizes update logic and provides a clean interface for project modifications.
 */
public class ProjectUpdateManager {

    private static final java.util.List<String> PD_KEYS_FROM_DETAILS_SCREEN =
            java.util.Arrays.asList(
                    // פרטים כלליים / בניין
                    "property_location",
                    "building_type",
                    "physical_condition",
                    "external_cladding",
                    "number_of_floors",
                    "building_entry",
                    "building_number",
                    "zone_number",
                    "building_city_plan_number",

                    // פרטי הדירה
                    "apartment_number(municipal_form)",
                    "apartment_story",
                    "number_of_rooms",

                    // מדדים (שטחים)
                    "registered_apartment_area",
                    "gross_apartment_area",

                    // כיווני אוויר
                    "apartment_directions"
            );

    // רשימת שדות כפולים שהופיעו בשורש וצריכים להימחק (יישארו רק תחת property_details)
    private static final java.util.List<String> ROOT_DUP_KEYS = java.util.Arrays.asList(
            // לפי הרשימה שנתת
            "apartment_story",
            "building_city_plan_number",
            "building_entry",
            "buildingEntry",          // וריאציה בקמל-קייס למקרה שהוזרק בעבר
            "building_number",
            "buildingNumber",         // וריאציה בקמל-קייס
            "building_type",
            "external_cladding",
            "externalCladding",       // וריאציה בקמל-קייס
            "gross_apartment_area",
            "number_of_floors",
            "number_of_rooms",
            "physical_condition",
            "property_location",
            "registered_apartment_area"
    );

    private final FirebaseFirestore db;
    private final MutableLiveData<String> errorMessage;

    public ProjectUpdateManager(FirebaseFirestore db, MutableLiveData<String> errorMessage) {
        this.db = db;
        this.errorMessage = errorMessage;
    }

    // ========================= CLIENT UPDATES =========================

    /**
     * Updates client details for a specific project
     */
    public void saveClientDetails(String projectId, Client client, OnCompleteListener<Void> listener) {
        updateProject(projectId, project -> project.setClient(client), listener);
    }

    // ========================= PROPERTY UPDATES =========================

    /**
     * Updates property details for a specific project
     */
    public void savePropertyDetails(String projectId,
                                    Map<String, Object> details,
                                    OnCompleteListener<Void> listener) {
        if (projectId == null || projectId.trim().isEmpty()) {
            if (listener != null) {
                listener.onComplete(com.google.android.gms.tasks.Tasks.forException(
                        new IllegalArgumentException("projectId is empty")));
            }
            return;
        }

        // מסנן null/""/[] כדי שלא נדרוס ערכים קיימים בריקים
        Map<String, Object> cleaned = cleanMap(details); // ודאי שמתודת עזר זו קיימת במחלקה

        final com.google.firebase.firestore.DocumentReference ref =
                db.collection(com.example.finalprojectappraisal.database.constants.FirestoreConstants.COLLECTION_PROJECTS)
                        .document(projectId);

        db.runTransaction((Transaction.Function<Void>) tr -> {
                    DocumentSnapshot snap = tr.get(ref);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> existingPd =
                            (Map<String, Object>) snap.get("property_details");

                    // nulls רק למפתחות שחסרים כרגע ב-DB ושלא מגיעים בעדכון הנוכחי
                    Map<String, Object> nullsPatch = new HashMap<>();
                    for (String key : PD_KEYS_FROM_DETAILS_SCREEN) { // ודאי שהרשימה הזו מוגדרת במחלקה
                        boolean missingInDb = (existingPd == null) || !existingPd.containsKey(key);
                        boolean notComingNow = !cleaned.containsKey(key);
                        if (missingInDb && notComingNow) {
                            nullsPatch.put(key, null);
                        }
                    }

                    // מאחדים: תחילה null לשדות חסרים, ואז הערכים המעודכנים
                    Map<String, Object> mergedDetails = new HashMap<>(nullsPatch);
                    mergedDetails.putAll(cleaned);

                    long now = System.currentTimeMillis();
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("property_details", mergedDetails);
                    payload.put("last_update", now);
                    payload.put("schema_version", 2);

                    tr.set(ref, payload, SetOptions.merge());
                    return null; // חשוב כדי שה-Task יהיה מסוג Void
                }).addOnCompleteListener(listener)
                .addOnFailureListener(e -> {
                    if (errorMessage != null) {
                        errorMessage.setValue("Save property details failed: " + e.getMessage());
                    }
                    android.util.Log.e("UpdateManager", "savePropertyDetails(tx) failed: " + e.getMessage());
                });
    }


    private Map<String, Object> cleanMap(Map<String, Object> src) {
        Map<String, Object> out = new java.util.HashMap<>();
        if (src == null) return out;
        for (Map.Entry<String, Object> e : src.entrySet()) {
            Object v = e.getValue();
            if (v == null) continue;
            if (v instanceof String && ((String) v).trim().isEmpty()) continue;
            if (v instanceof java.util.Collection && ((java.util.Collection<?>) v).isEmpty()) continue;
            out.put(e.getKey(), v);
        }
        return out;
    }

    // ========================= APARTMENT UPDATES =========================

    /**
     * Updates apartment details for a specific project
     */
    public void saveApartmentDetails(String projectId, Map<String, Object> apartmentDetails, OnCompleteListener<Void> listener) {
        updateProject(projectId, project -> ProjectFieldUpdater.updateApartmentFields(project, apartmentDetails), listener);
    }

    // מוחק את השדות בשורש המסמך (לא נוגע ב-property_details.*)
    public void cleanupRootDuplicateFields(@NonNull String projectId,
                                           @androidx.annotation.Nullable com.google.android.gms.tasks.OnCompleteListener<Void> listener) {
        com.google.firebase.firestore.DocumentReference ref =
                db.collection(com.example.finalprojectappraisal.database.constants.FirestoreConstants.COLLECTION_PROJECTS)
                        .document(projectId);

        java.util.Map<String, Object> deletes = new java.util.HashMap<>();
        for (String key : ROOT_DUP_KEYS) {
            deletes.put(key, FieldValue.delete());
        }

        ref.update(deletes)
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> {
                    if (errorMessage != null) errorMessage.setValue("Cleanup root duplicates failed: " + e.getMessage());
                    android.util.Log.e("UpdateManager", "cleanupRootDuplicateFields failed: " + e.getMessage());
                });
    }

    // ========================= IMAGES UPDATES =========================

    /**
     * מוסיף או מעדכן (upsert) כניסה ב-array propertyImages לפי name.
     * אם קיים name זהה – נעדכן את ה-path; אחרת נוסיף אובייקט חדש.
     */
    public void upsertPropertyImage(@NonNull String projectId,
                                    @NonNull String name,
                                    @NonNull String path,
                                    OnCompleteListener<Void> listener) {

        DocumentReference ref = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snap = transaction.get(ref);

                    List<Map<String, Object>> arr =
                            (List<Map<String, Object>>) snap.get(FirestoreConstants.FIELD_PROPERTY_IMAGES);
                    if (arr == null) arr = new ArrayList<>();

                    boolean replaced = false;
                    for (int i = 0; i < arr.size(); i++) {
                        Map<String, Object> obj = arr.get(i);
                        if (name.equals(obj.get(FirestoreConstants.KEY_PROPIMG_NAME))) {
                            Map<String, Object> updated = new HashMap<>(obj);
                            updated.put(FirestoreConstants.KEY_PROPIMG_PATH, path);
                            arr.set(i, updated);
                            replaced = true;
                            break;
                        }
                    }
                    if (!replaced) {
                        Map<String, Object> newObj = new HashMap<>();
                        newObj.put(FirestoreConstants.KEY_PROPIMG_NAME, name);
                        newObj.put(FirestoreConstants.KEY_PROPIMG_PATH, path);
                        arr.add(newObj);
                    }

                    transaction.update(ref, FirestoreConstants.FIELD_PROPERTY_IMAGES, arr);
                    return null; // ← מחזיר Void
                })
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> {
                    if (errorMessage != null) {
                        errorMessage.setValue("שגיאה בשמירת propertyImages: " + e.getMessage());
                    }
                });
    }


    /**
     * מוחק מה-array את האובייקט עם name תואם, רק אם ה-path הנוכחי שווה ל-expectedPath.
     * מונע מצב שנמחוק ערך חדש שהתווסף מהר.
     */
    public void removePropertyImageIfMatches(@NonNull String projectId,
                                             @NonNull String name,
                                             @NonNull String expectedPath,
                                             OnCompleteListener<Void> listener) {

        DocumentReference ref = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snap = transaction.get(ref);

                    List<Map<String, Object>> arr =
                            (List<Map<String, Object>>) snap.get(FirestoreConstants.FIELD_PROPERTY_IMAGES);
                    if (arr == null || arr.isEmpty()) return null;

                    List<Map<String, Object>> newArr = new ArrayList<>();
                    for (Map<String, Object> obj : arr) {
                        String n = (String) obj.get(FirestoreConstants.KEY_PROPIMG_NAME);
                        String p = (String) obj.get(FirestoreConstants.KEY_PROPIMG_PATH);
                        if (name.equals(n) && expectedPath != null && expectedPath.equals(p)) {
                            continue; // מדלגים – זה הפריט שמוחקים
                        }
                        newArr.add(obj);
                    }

                    if (newArr.isEmpty()) {
                        transaction.update(ref, FirestoreConstants.FIELD_PROPERTY_IMAGES, FieldValue.delete());
                    } else {
                        transaction.update(ref, FirestoreConstants.FIELD_PROPERTY_IMAGES, newArr);
                    }
                    return null; // ← מחזיר Void
                })
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> {
                    if (errorMessage != null) {
                        errorMessage.setValue("שגיאה במחיקת propertyImages: " + e.getMessage());
                    }
                });
    }


    /**
     * Persists image paths into: projects/{projectId}/images/main (merge)
     * Expected fields: front_image, interior_image, tabu_crop_image (String paths)
     */
    public void saveImagePaths(@NonNull String projectId,
                               @NonNull Map<String, Object> imagePaths,
                               OnCompleteListener<Void> listener) {

        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .collection(FirestoreConstants.SUBCOLLECTION_IMAGES)
                .document(FirestoreConstants.IMAGES_DOC_MAIN)
                .set(imagePaths, SetOptions.merge())
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> {
                    if (errorMessage != null) {
                        errorMessage.setValue("שגיאה בשמירת נתיבי תמונות: " + e.getMessage());
                    }
                });
    }


    // ========================= FEATURE UPDATES =========================

    /**
     * Updates property features for a specific project
     */
    public void savePropertyFeatures(String projectId, Map<String, Object> features, OnCompleteListener<Void> listener) {
        updateProject(projectId, project -> ProjectFieldUpdater.updateFeatureFields(project, features), listener);
    }

    // ========================= STATUS UPDATES =========================

    /**
     * Updates the status of a specific project
     */
    public void updateProjectStatus(String projectId, String status, OnCompleteListener<Void> listener) {
        if (projectId == null || projectId.trim().isEmpty()) {
            handleError(FirestoreConstants.ERROR_PROJECT_NOT_FOUND, listener);
            return;
        }

        if (status == null || status.trim().isEmpty()) {
            handleError("Invalid status value", listener);
            return;
        }

        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .update(FirestoreConstants.FIELD_PROJECT_STATUS, status)
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> errorMessage.setValue(FirestoreConstants.ERROR_UPDATING_STATUS + ": " + e.getMessage()));
    }

    // ========================= BATCH UPDATES =========================

    /**
     * Updates multiple fields of a project in a single operation
     */
    public void updateMultipleFields(String projectId, Map<String, Object> fieldsToUpdate, OnCompleteListener<Void> listener) {
        if (projectId == null || projectId.trim().isEmpty()) {
            handleError(FirestoreConstants.ERROR_PROJECT_NOT_FOUND, listener);
            return;
        }

        if (fieldsToUpdate == null || fieldsToUpdate.isEmpty()) {
            handleError("No fields to update", listener);
            return;
        }

        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .update(fieldsToUpdate)
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> errorMessage.setValue(FirestoreConstants.ERROR_UPDATING_PROJECT + ": " + e.getMessage()));
    }

    // ========================= CORE UPDATE LOGIC =========================

    /**
     * Generic method for updating a project with custom logic
     */
    private void updateProject(String projectId, ProjectUpdater updater, OnCompleteListener<Void> listener) {
        if (projectId == null || projectId.trim().isEmpty()) {
            handleError(FirestoreConstants.ERROR_PROJECT_NOT_FOUND, listener);
            return;
        }

        if (updater == null) {
            handleError("Invalid update operation", listener);
            return;
        }

        getProject(projectId, task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                try {
                    Project project = task.getResult().toObject(Project.class);
                    if (project != null) {
                        updater.update(project);
                        saveProject(projectId, project, listener);
                    } else {
                        handleError(FirestoreConstants.ERROR_PROJECT_NOT_FOUND, listener);
                    }
                } catch (Exception e) {
                    handleError(FirestoreConstants.ERROR_LOADING_PROJECT + ": " + e.getMessage(), listener);
                }
            } else {
                handleError(FirestoreConstants.ERROR_LOADING_PROJECT, listener);
            }
        });
    }

    /**
     * Retrieves a project document from Firestore
     */
    private void getProject(String projectId, OnCompleteListener<DocumentSnapshot> listener) {
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .get()
                .addOnCompleteListener(listener);
    }

    /**
     * Saves a project to Firestore
     */
    private void saveProject(String projectId, Project project, OnCompleteListener<Void> listener) {
        DocumentReference ref = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId);

        Map<String, Object> stamp = new HashMap<>();
        stamp.put(FirestoreConstants.FIELD_LAST_UPDATE_DATE, FieldValue.serverTimestamp());

        // קודם מעדכן חותמת זמן, ואז ממזג את ה-POJO בלי למחוק שדות אחרים
        ref.update(stamp)
                .continueWithTask(t -> ref.set(project, SetOptions.merge()))
                .addOnCompleteListener(listener)
                .addOnFailureListener(e ->
                        errorMessage.setValue(FirestoreConstants.ERROR_SAVING_PROJECT + ": " + e.getMessage())
                );
    }


    /**
     * Handles errors by setting error message and notifying listener
     */
    private void handleError(String message, OnCompleteListener<Void> listener) {
        errorMessage.setValue(message);
        if (listener != null) {
            // Create a failed task for consistency
            listener.onComplete(createFailedTask(new Exception(message)));
        }
    }

    /**
     * Creates a failed Task for error handling
     */
    private Task<Void> createFailedTask(Exception exception) {
        return com.google.android.gms.tasks.Tasks.forException(exception);
    }

    // ========================= FUNCTIONAL INTERFACE =========================

    @FunctionalInterface
    public interface ProjectUpdater {
        void update(Project project);
    }
}