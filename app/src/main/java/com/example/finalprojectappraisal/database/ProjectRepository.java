package com.example.finalprojectappraisal.database;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.database.updater.ProjectUpdateManager;
import com.example.finalprojectappraisal.database.validator.ProjectDataValidator;
import com.example.finalprojectappraisal.model.Appraiser;
import com.example.finalprojectappraisal.model.BankDetails;
import com.example.finalprojectappraisal.model.Client;
import com.example.finalprojectappraisal.model.Image;
import com.example.finalprojectappraisal.model.Project;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Main repository class that handles Firebase Firestore operations for projects.
 * Uses Singleton pattern and delegates complex operations to specialized managers.
 * Focuses on core CRUD operations and data flow management.
 */
public class ProjectRepository {

    private static final String TAG = "ProjectRepository";
    private static ProjectRepository instance;
    private final FirebaseFirestore db;
    private final ProjectUpdateManager updateManager;
    private String currentProjectId;

    private final FirebaseStorage storage;
    private final StorageReference storageRef;

    // LiveData for reactive programming
    private final MutableLiveData<Project> currentProject = new MutableLiveData<>();
    private final MutableLiveData<List<Project>> allProjects = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Listener for live queries
    private ListenerRegistration projectsListener;
    private String lastUsedUserIdForListening = null;


    private ProjectRepository() {
        db = FirebaseFirestore.getInstance();
        updateManager = new ProjectUpdateManager(db, errorMessage);
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    /**
     * Gets the singleton instance of ProjectRepository
     */
    public static synchronized ProjectRepository getInstance() {
        if (instance == null) {
            instance = new ProjectRepository();
        }
        return instance;
    }

    // ========================= PROJECT CREATION =========================

    public void createNewProject(@NonNull Project project, @Nullable OnCompleteListener<Void> listener) {
        if (project == null) {
            handleError("Project cannot be null", listener);
            return;
        }

        ProjectDataValidator.ValidationResult validation = ProjectDataValidator.validateProject(project);
        if (!validation.isValid()) {
            handleError("Validation failed: " + validation.getErrorsAsString(), listener);
            return;
        }

        DocumentReference newProjectRef = db.collection(FirestoreConstants.COLLECTION_PROJECTS).document();
        String generatedId = newProjectRef.getId();
        project.setProjectId(generatedId);

        newProjectRef.set(project)
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> errorMessage.setValue(FirestoreConstants.ERROR_CREATING_PROJECT + ": " + e.getMessage()));
    }

    public void createProjectWithAddress(@NonNull String fullAddress, @NonNull Project project, @Nullable OnCompleteListener<Void> listener) {
        if (fullAddress == null || fullAddress.trim().isEmpty()) {
            handleError("Full address cannot be null or empty", listener);
            return;
        }
        if (project == null) {
            handleError("Project cannot be null", listener);
            return;
        }

        ProjectDataValidator.ValidationResult validation = ProjectDataValidator.validateProject(project);
        if (!validation.isValid()) {
            handleError("Validation failed: " + validation.getErrorsAsString(), listener);
            return;
        }

        project.setProjectId(fullAddress);
        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(fullAddress)
                .set(project)
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> errorMessage.setValue(FirestoreConstants.ERROR_CREATING_PROJECT + ": " + e.getMessage()));
    }

    // ========================= PROJECT RETRIEVAL =========================

    public void getProject(@NonNull String projectId, @Nullable OnCompleteListener<DocumentSnapshot> listener) {
        if (projectId == null || projectId.trim().isEmpty()) {
            handleError(FirestoreConstants.ERROR_PROJECT_NOT_FOUND, null);
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void loadProject(@NonNull String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            errorMessage.setValue(FirestoreConstants.ERROR_PROJECT_NOT_FOUND);
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            Project project = documentSnapshot.toObject(Project.class);
                            if (project != null) {
                                currentProjectId = projectId;
                                currentProject.setValue(project);
                            } else {
                                errorMessage.setValue(FirestoreConstants.ERROR_PROJECT_NOT_FOUND_WITH_ID + projectId);
                            }
                        } catch (Exception e) {
                            errorMessage.setValue(FirestoreConstants.ERROR_LOADING_PROJECT + ": " + e.getMessage());
                        }
                    } else {
                        errorMessage.setValue(FirestoreConstants.ERROR_PROJECT_NOT_FOUND_WITH_ID + projectId);
                    }
                })
                .addOnFailureListener(e -> errorMessage.setValue(FirestoreConstants.ERROR_LOADING_PROJECT + ": " + e.getMessage()));
    }

    /**
     * טוענת פרויקטים שבהם המשתמש הוא:
     * 1. השמאי הראשי (appraiserId)
     * 2. אחד מהשמאים השותפים (co_appraiser_ids)
     *
     * @param userId ה-UID של המשתמש הנוכחי
     */
    public void loadProjectsForAppraiser(@NonNull String userId) {
        lastUsedUserIdForListening = userId;
        stopListening();

        Task<QuerySnapshot> query1 = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .whereEqualTo(FirestoreConstants.FIELD_APPRAISER_ID, userId)
                .get();

        Task<QuerySnapshot> query2 = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .whereArrayContains(FirestoreConstants.FIELD_CO_APPRAISER_IDS, userId)
                .get();

        Tasks.whenAllSuccess(query1, query2)
                .addOnSuccessListener(results -> {
                    Set<Project> combinedProjects = new HashSet<>();
                    for (Object result : results) {
                        QuerySnapshot snapshot = (QuerySnapshot) result;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Project p = safeProjectFrom(doc);
                            if (p != null) {
                                combinedProjects.add(p);
                            }
                        }
                    }

                    List<Project> sortedProjects = new ArrayList<>(combinedProjects);
                    sortedProjects.sort(Comparator.comparingLong(Project::getLastUpdateDateMillis).reversed());

                    allProjects.setValue(sortedProjects);
                    Log.d(TAG, "Loaded " + sortedProjects.size() + " projects for user " + userId + " (Primary/Co-Appraiser).");
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("שגיאה בטעינת פרויקטים עבור משתמש: " + e.getMessage());
                    allProjects.setValue(new ArrayList<>());
                    Log.e(TAG, "Error loading projects for user " + userId + ": " + e.getMessage());
                });
    }

    /**
     * מפסיק להאזין לכל הפרויקטים (מסיר את ה-projectsListener)
     */
    public void stopListening() {
        if (projectsListener != null) {
            projectsListener.remove();
            projectsListener = null;
            Log.d(TAG, "Stopped listening to projects.");
        }
    }

    /**
     * טעינת כל הפרויקטים עם מאזין חי (למנהלים בלבד)
     */
    public void loadAllProjectsWithListener() {
        stopListening();

        Query q = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .orderBy(FirestoreConstants.FIELD_LAST_UPDATE_DATE, Query.Direction.DESCENDING);

        projectsListener = q.addSnapshotListener((snap, err) -> {
            if (err != null) {
                if (err instanceof FirebaseFirestoreException &&
                        ((FirebaseFirestoreException) err).getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    Log.w(TAG, "Missing index for 'lastUpdateDate' field, falling back to unsorted query.");
                    loadAllProjectsWithoutOrder();
                    return;
                }
                Log.e(TAG, "listen error (all projects): " + err.getMessage());
                allProjects.setValue(new ArrayList<>());
                errorMessage.setValue("שגיאה בטעינת כל הפרויקטים: " + err.getMessage());
                return;
            }

            List<Project> out = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot d : snap.getDocuments()) {
                    Project p = safeProjectFrom(d);
                    if (p != null) out.add(p);
                }
            }
            Log.d(TAG, "Loaded (all projects) " + out.size() + " projects");
            allProjects.setValue(out);
        });
    }

    /**
     * טעינת כל הפרויקטים ללא מיון (במקרה של חוסר אינדקס)
     */
    private void loadAllProjectsWithoutOrder() {
        stopListening();

        Query q = db.collection(FirestoreConstants.COLLECTION_PROJECTS);

        projectsListener = q.addSnapshotListener((snap, err) -> {
            if (err != null) {
                Log.e(TAG, "listen error (all projects no order): " + err.getMessage());
                allProjects.setValue(new ArrayList<>());
                errorMessage.setValue("שגיאה בטעינת כל הפרויקטים: " + err.getMessage());
                return;
            }

            List<Project> out = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot d : snap.getDocuments()) {
                    Project p = safeProjectFrom(d);
                    if (p != null) out.add(p);
                }
            }

            out.sort(Comparator.comparingLong(Project::getLastUpdateDateMillis).reversed());

            Log.d(TAG, "Loaded (all projects no order) " + out.size() + " projects");
            allProjects.setValue(out);
        });
    }

    public void loadProjectsByStatus(@NonNull String status, @Nullable OnCompleteListener<List<Project>> listener) {
        if (status == null || status.trim().isEmpty()) {
            handleError("Status cannot be null or empty", null);
            return;
        }

        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .whereEqualTo(FirestoreConstants.FIELD_PROJECT_STATUS, status)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Project> projects = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        try {
                            Project project = document.toObject(Project.class);
                            if (project != null) {
                                projects.add(project);
                            }
                        } catch (Exception e) {
                            System.err.println("Error loading project " + document.getId() + ": " + e.getMessage());
                        }
                    }
                    if (listener != null) listener.onComplete(Tasks.forResult(projects));
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue(FirestoreConstants.ERROR_LOADING_PROJECTS + ": " + e.getMessage());
                    if (listener != null) listener.onComplete(Tasks.forException(e));
                });
    }

    // ========================= PROJECT UPDATES (DELEGATED) =========================

    public void saveClientDetails(@NonNull String projectId, @NonNull Client client, @Nullable OnCompleteListener<Void> listener) {
        ProjectDataValidator.ValidationResult validation = ProjectDataValidator.validateClient(client);
        if (!validation.isValid()) {
            handleError("Client validation failed: " + validation.getErrorsAsString(), listener);
            return;
        }
        updateManager.saveClientDetails(projectId, client, listener);
    }

    public void savePropertyDetails(@NonNull String projectId, @NonNull Map<String, Object> propertyDetails, @Nullable OnCompleteListener<Void> listener) {
        updateManager.savePropertyDetails(projectId, propertyDetails, listener);
    }

    public void saveApartmentDetails(@NonNull String projectId, @NonNull Map<String, Object> apartmentDetails, @Nullable OnCompleteListener<Void> listener) {
        ProjectDataValidator.ValidationResult validation = ProjectDataValidator.validateApartmentDetails(apartmentDetails);
        if (!validation.isValid()) {
            handleError("Apartment details validation failed: " + validation.getErrorsAsString(), listener);
            return;
        }

        updateManager.saveApartmentDetails(projectId, apartmentDetails, task -> {
            if (!task.isSuccessful()) {
                if (listener != null) listener.onComplete(task);
                return;
            }
            cleanupRootDuplicates(projectId, cleanupTask -> {
                if (listener != null) listener.onComplete(cleanupTask);
            });
        });
    }

    public void cleanupRootDuplicates(@NonNull String projectId, @Nullable OnCompleteListener<Void> listener) {
        updateManager.cleanupRootDuplicateFields(projectId, listener);
    }

    public void savePropertyFeatures(@NonNull String projectId, @NonNull Map<String, Object> features, @Nullable OnCompleteListener<Void> listener) {
        ProjectDataValidator.ValidationResult validation = ProjectDataValidator.validatePropertyFeatures(features);
        if (!validation.isValid()) {
            handleError("Property features validation failed: " + validation.getErrorsAsString(), listener);
            return;
        }
        updateManager.savePropertyFeatures(projectId, features, listener);
    }

    public void updateProjectStatus(@NonNull String projectId, @NonNull String status, @Nullable OnCompleteListener<Void> listener) {
        updateManager.updateProjectStatus(projectId, status, listener);
    }

    public void updateMultipleFields(@NonNull String projectId, @NonNull Map<String, Object> fieldsToUpdate, @Nullable OnCompleteListener<Void> listener) {
        updateManager.updateMultipleFields(projectId, fieldsToUpdate, listener);
    }

    // ========================= PROJECT DELETION =========================

    public void deleteProject(@NonNull String projectId, @Nullable OnCompleteListener<Void> listener) {
        if (projectId == null || projectId.trim().isEmpty()) {
            handleError(FirestoreConstants.ERROR_PROJECT_NOT_FOUND, listener);
            return;
        }

        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (projectId.equals(currentProjectId)) {
                            currentProjectId = null;
                            currentProject.setValue(null);
                        }
                        if (lastUsedUserIdForListening != null) {
                            loadProjectsForAppraiser(lastUsedUserIdForListening);
                        } else {
                            loadAllProjectsWithListener();
                        }
                        Log.d(TAG, "Project " + projectId + " deleted successfully.");
                    } else {
                        Log.e(TAG, "Error deleting project " + projectId + ": " + task.getException().getMessage());
                    }
                    if (listener != null) {
                        listener.onComplete(task);
                    }
                })
                .addOnFailureListener(e -> errorMessage.setValue(FirestoreConstants.ERROR_DELETING_PROJECT + ": " + e.getMessage()));
    }


    // ========================= IMAGE MANAGEMENT =========================
    /**
     * Saves image path fields into projects/{projectId}/images/main (merge)
     * Allowed keys: front_image, interior_image, tabu_crop_image
     */
    public void saveImagePaths(@NonNull String projectId,
                               @NonNull Map<String, Object> imagePaths,
                               OnCompleteListener<Void> listener) {
        if (projectId == null || projectId.trim().isEmpty()) {
            handleError("Project ID is required", listener);
            return;
        }
        if (imagePaths == null || imagePaths.isEmpty()) {
            handleError("imagePaths cannot be null or empty", listener);
            return;
        }
        updateManager.saveImagePaths(projectId, imagePaths, task -> {
            if (!task.isSuccessful()) {
                if (listener != null) listener.onComplete(task);
                return;
            }
            // ריענון פרויקט נוכחי אם הוא זה שעודכן
            if (projectId.equals(currentProjectId)) {
                refreshCurrentProject();
            }
            if (listener != null) listener.onComplete(task);
        });
    }


    // טוען תמונות משני מקורות: תת-האוסף images/* וגם המסמך images/main.
// מוסף סינון: רק מחרוזות שמתחילות ב-"http" (מתעלם מ-content://).
    public void loadAllImagesForProject(
            @NonNull String projectId,
            @NonNull com.google.android.gms.tasks.OnCompleteListener<List<Image>> listener) {

        if (projectId.trim().isEmpty()) {
            listener.onComplete(Tasks.forResult(new ArrayList<>()));
            return;
        }

        Task<QuerySnapshot> tSub = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .collection("images")
                .get();

        Task<DocumentSnapshot> tMain = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .collection("images")
                .document("main")
                .get();

        Tasks.whenAllComplete(tSub, tMain)
                .addOnCompleteListener(done -> {
                    List<Image> out = new ArrayList<>();
                    java.util.HashSet<String> dedup = new java.util.HashSet<>(); // מניעת כפולים לפי URL

                    // 1) מסמכים בתת-האוסף (כולל "main" אם קיים שם כ-document)
                    if (tSub.isSuccessful() && tSub.getResult() != null) {
                        for (QueryDocumentSnapshot doc : tSub.getResult()) {
                            Map<String, Object> data = doc.getData();
                            if (data == null) continue;

                            // קודם ננסה למפות לאובייקט Image
                            try {
                                Image im = new Image(data);
                                String u = im.getUrl();
                                if (u != null && u.startsWith("http") && dedup.add(u)) {
                                    im.setProjectId(projectId);
                                    out.add(im);
                                }
                            } catch (Exception ignore) {}

                            // ואז נסרוק כל שדה – כל String שמתחיל ב-http
                            for (Map.Entry<String, Object> e : data.entrySet()) {
                                Object v = e.getValue();

                                // ערכי רשימה (front_image/interior_image הם לעתים Arrays)
                                if (v instanceof java.util.List<?>) {
                                    for (Object item : (List<?>) v) {
                                        if (item instanceof String) {
                                            String s = (String) item;
                                            if (s.startsWith("http") && dedup.add(s)) {
                                                Image im2 = new Image();
                                                im2.setProjectId(projectId);
                                                im2.setUrl(s);
                                                out.add(im2);
                                            }
                                        }
                                    }
                                    continue;
                                }

                                // ערך בודד
                                if (v instanceof String) {
                                    String s = (String) v;
                                    if (s.startsWith("http") && dedup.add(s)) {
                                        Image im2 = new Image();
                                        im2.setProjectId(projectId);
                                        im2.setUrl(s);
                                        out.add(im2);
                                    }
                                }
                            }
                        }
                    }

                    // 2) המסמך images/main — אם הוא קיים – ניקח רק http(s) (תומך גם במערכים)
                    if (tMain.isSuccessful() && tMain.getResult() != null && tMain.getResult().exists()) {
                        Map<String, Object> mainData = tMain.getResult().getData();
                        if (mainData != null) {
                            for (Map.Entry<String, Object> e : mainData.entrySet()) {
                                Object v = e.getValue();

                                if (v instanceof java.util.List<?>) {
                                    for (Object item : (List<?>) v) {
                                        if (item instanceof String) {
                                            String s = (String) item;
                                            if (s.startsWith("http") && dedup.add(s)) {
                                                Image im = new Image();
                                                im.setProjectId(projectId);
                                                im.setUrl(s);
                                                out.add(im);
                                            }
                                        }
                                    }
                                    continue;
                                }

                                if (v instanceof String) {
                                    String s = (String) v;
                                    if (s.startsWith("http") && dedup.add(s)) {
                                        Image im = new Image();
                                        im.setProjectId(projectId);
                                        im.setUrl(s);
                                        out.add(im);
                                    }
                                }
                            }
                        }
                    }

                    listener.onComplete(Tasks.forResult(out));
                })
                .addOnFailureListener(e -> listener.onComplete(Tasks.forException(e)));
    }


    // מיקום אחיד לתמונות ב-Storage
    public String buildImageStoragePath(@NonNull String projectId, @NonNull String imageId) {
        return "projects/" + projectId + "/images/" + imageId; // אין חובה לסיומת
    }

    /**
     * Uploads a local content:// image to Firebase Storage and returns the downloadUrl (https).
     */
    public void uploadImageToStorage(@NonNull String projectId,
                                     @NonNull android.net.Uri localUri,
                                     @NonNull String imageId,
                                     @NonNull com.google.android.gms.tasks.OnCompleteListener<String> listener) {

        String path = buildImageStoragePath(projectId, imageId);
        StorageReference ref = storageRef.child(path);

        ref.putFile(localUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(uri ->
                                listener.onComplete(Tasks.forResult(uri.toString()))
                        ).addOnFailureListener(e ->
                                listener.onComplete(Tasks.forException(e))
                        )
                )
                .addOnFailureListener(e -> listener.onComplete(Tasks.forException(e)));
    }

    /**
     * Convenience: update a single image path field key->value
     * fieldName must be one of:
     * FirestoreConstants.FIELD_FRONT_IMAGE / FIELD_INTERIOR_IMAGE / FIELD_TABU_CROP_IMAGE
     */
    public void updateSingleImagePath(@NonNull String projectId,
                                      @NonNull String fieldName,
                                      @NonNull String path,
                                      OnCompleteListener<Void> listener) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put(fieldName, path);
        saveImagePaths(projectId, data, listener);
    }
    public void addImageToProject(String projectId, Image image, OnCompleteListener<Void> listener) {
        if (projectId == null || image == null) {
            handleError("Project ID and Image are required", listener);
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .collection("images")
                .document(image.getId())
                .set(image.toMap()) // ממפה את Image ל-Map לשמירה ב-DB
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> errorMessage.setValue("שגיאה בהעלאת תמונה: " + e.getMessage()));
    }

    public void updateImageInProject(@NonNull String projectId, @NonNull Image image, @Nullable OnCompleteListener<Void> listener) {
        if (projectId == null || image == null) {
            handleError("Project ID and Image are required", listener);
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .collection(FirestoreConstants.SUBCOLLECTION_IMAGES) // <-- שינוי כאן
                .document(image.getId())
                .set(image.toMap())
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> errorMessage.setValue("שגיאה בעדכון תמונה: " + e.getMessage()));
    }

    public void deleteImageFromProject(@NonNull String projectId, @NonNull String imageId, @Nullable OnCompleteListener<Void> listener) {
        if (projectId == null || imageId == null) {
            handleError("Project ID and Image ID are required", listener);
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .collection(FirestoreConstants.SUBCOLLECTION_IMAGES) // <-- שינוי כאן
                .document(imageId)
                .delete()
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> errorMessage.setValue("שגיאה במחיקת תמונה: " + e.getMessage()));
    }

    public void getImagesForProject(@NonNull String projectId, @Nullable OnCompleteListener<List<Image>> listener) {
        if (projectId == null) {
            handleError("Project ID is required", null);
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .collection(FirestoreConstants.SUBCOLLECTION_IMAGES) // <-- שינוי כאן
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Exception e = (task.getException() != null) ? task.getException() : new Exception("images fetch failed");
                        if (listener != null) listener.onComplete(Tasks.forException(e));
                        return;
                    }
                    List<Image> out = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        Map<String, Object> data = doc.getData();
                        if (data == null) continue;
                        try {
                            out.add(new Image(data));
                        } catch (Exception ignore) {
                            Log.e(TAG, "Error parsing image " + doc.getId() + ": " + ignore.getMessage());
                        }
                    }
                    if (listener != null) listener.onComplete(Tasks.forResult(out));
                }).addOnFailureListener(e -> {
                    if (listener != null) listener.onComplete(Tasks.forException(e));
                });
    }

    public void upsertPropertyImage(@NonNull String projectId, @NonNull String name, @NonNull String path, @Nullable OnCompleteListener<Void> listener) {
        if (projectId.trim().isEmpty() || name.trim().isEmpty() || path.trim().isEmpty()) {
            handleError("projectId/name/path are required", listener);
            return;
        }
        updateManager.upsertPropertyImage(projectId, name, path, task -> {
            if (projectId.equals(currentProjectId)) refreshCurrentProject();
            if (listener != null) listener.onComplete(task);
        });
    }

    public void removePropertyImageIfMatches(@NonNull String projectId, @NonNull String name, @NonNull String expectedPath, @Nullable OnCompleteListener<Void> listener) {
        if (projectId.trim().isEmpty() || name.trim().isEmpty() || expectedPath.trim().isEmpty()) {
            handleError("projectId/name/expectedPath are required", listener);
            return;
        }
        updateManager.removePropertyImageIfMatches(projectId, name, expectedPath, task -> {
            if (projectId.equals(currentProjectId)) refreshCurrentProject();
            if (listener != null) listener.onComplete(task);
        });
    }

    /** Minimal set of structured fields recommended for the summary prompt. */
    public void getProjectFieldsForSummary(String projectId, OnCompleteListener<Map<String, Object>> listener) {
        db.collection(FirestoreConstants.COLLECTION_PROJECTS) // << במקום "projects" קבוע
                .document(projectId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        listener.onComplete(Tasks.forResult(Collections.emptyMap()));
                        return;
                    }
                    DocumentSnapshot doc = task.getResult();
                    Map<String, Object> map = new HashMap<>();

                    // >>> התאמה לסכמה שלך (Project.java) לפי @PropertyName / @SerializedName:
                    putIfExists(doc, map, "number_of_rooms");            // מס' חדרים
                    putIfExists(doc, map, "physical_condition");         // מצב הבניין
                    putIfExists(doc, map, "has_elevator");               // מעלית
                    putIfExists(doc, map, "has_parking");                // חניה
                    putIfExists(doc, map, "has_storage");                // מחסן
                    putIfExists(doc, map, "apartment_flooring");         // ריצוף
                    putIfExists(doc, map, "apartment_windows");          // חלונות
                    putIfExists(doc, map, "apartment_kitchen");          // מטבח
                    putIfExists(doc, map, "apartment_bathroom_fixtures");// אמבטיה/כלים סניטריים

                    // תוספות שימושיות להצגה:
                    putIfExists(doc, map, "registered_apartment_area");  // שטח רשום
                    putIfExists(doc, map, "gross_apartment_area");       // שטח ברוטו
                    putIfExists(doc, map, "apartment_story");            // קומה
                    putIfExists(doc, map, "apartment_number(municipal_form)"); // מס' דירה (טופס עירייה)
                    putIfExists(doc, map, "property_location");          // מיקום
                    putIfExists(doc, map, "building_type");              // סוג בניין
                    putIfExists(doc, map, "number_of_floors");           // מס' קומות בבניין
                    putIfExists(doc, map, "apartment_directions");       // כיווני אוויר (List)

                    listener.onComplete(Tasks.forResult(map));
                });
    }

    private static void putIfExists(DocumentSnapshot doc, Map<String, Object> out, String key) {
        if (doc.contains(key)) {
            Object v = doc.get(key);
            if (v != null) out.put(key, v);
        }
    }

    // ========================= UTILITY METHODS =========================

    public void projectExists(@NonNull String projectId, @Nullable OnCompleteListener<Boolean> listener) {
        if (projectId == null || projectId.trim().isEmpty()) {
            if (listener != null) listener.onComplete(Tasks.forResult(false));
            return;
        }

        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (listener != null) listener.onComplete(Tasks.forResult(task.getResult().exists()));
                    } else {
                        if (listener != null) listener.onComplete(Tasks.forResult(false));
                    }
                });
    }

    private Project safeProjectFrom(DocumentSnapshot d) {
        Project p;
        try {
            p = d.toObject(Project.class);
            if (p == null) {
                p = new Project();
                Log.w(TAG, "toObject returned null for " + d.getId() + ", creating empty project.");
            }
        } catch (Exception e) {
            Log.w(TAG, "toObject failed for " + d.getId() + ": " + e.getMessage() + ", manually parsing basic fields.");
            p = new Project();

            try { p.setProjectStatus((String) d.get(FirestoreConstants.FIELD_PROJECT_STATUS)); } catch (Exception ignore) {}
            try { p.setFullAddress((String) d.get(FirestoreConstants.FIELD_FULL_ADDRESS)); } catch (Exception ignore) {}
            try { p.setNote((String) d.get("note")); } catch (Exception ignore) {} // "note" נשאר כמחרוזת אם הוא לא קבוע

            try {
                Object clientObj = d.get("client"); // "client" נשאר כמחרוזת אם הוא לא קבוע
                if (clientObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cm = (Map<String, Object>) clientObj;
                    Client c = new Client();
                    // ודא שהשדות האלה מוגדרים גם ב-FirestoreConstants אם הם קיימים
                    try { c.setClientId((String) cm.get("id")); } catch (Exception ignore) {}
                    try { c.setFullName((String) cm.get("fullName")); } catch (Exception ignore) {}
                    try { c.setEmail((String) cm.get("email")); } catch (Exception ignore) {}
                    try { c.setPhoneNumber((String) cm.get("phoneNumber")); } catch (Exception ignore) {}
                    p.setClient(c);
                }
            } catch (Exception ignore) {}
        }

        try {
            if (p.getProjectId() == null || p.getProjectId().trim().isEmpty()) {
                p.setProjectId(d.getId());
            }
        } catch (Exception ignore) {}

        try {
            Object ts = d.get("lastUpdateDate"); // "lastUpdateDate" אם לא קבוע
            long millis = 0L;
            if (ts instanceof Timestamp) {
                millis = ((Timestamp) ts).toDate().getTime();
            } else if (ts instanceof Long) {
                millis = (Long) ts;
            } else if (ts instanceof Double) {
                millis = ((Double) ts).longValue();
            }
            if (p.getLastUpdateDate() == null && millis > 0L) {
                p.setLastUpdateDate(new Date(millis));
            }
        } catch (Exception e) {
            Log.w(TAG, "lastUpdateDate parse failed for " + d.getId() + ": " + e.getMessage());
        }

        try {
            Object coAppraiserIdsObj = d.get(FirestoreConstants.FIELD_CO_APPRAISER_IDS);
            if (coAppraiserIdsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> ids = (List<String>) coAppraiserIdsObj;
                p.setCoAppraiserIds(ids);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load co_appraiser_ids for " + d.getId() + ": " + e.getMessage());
        }

        try {
            Object appraiserIdObj = d.get(FirestoreConstants.FIELD_APPRAISER_ID);
            if (appraiserIdObj instanceof String) {
                p.setAppraiserId((String) appraiserIdObj);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load appraiser_id for " + d.getId() + ": " + e.getMessage());
        }


        return p;
    }

    public void refreshCurrentProject() {
        if (currentProjectId != null) {
            loadProject(currentProjectId);
        }
    }

    public void clearCache() {
        currentProjectId = null;
        currentProject.setValue(null);
        allProjects.setValue(new ArrayList<>());
        errorMessage.setValue(null);
        stopListening();
    }

    private void handleError(@NonNull String message, @Nullable OnCompleteListener<Void> listener) {
        errorMessage.setValue(message);
        if (listener != null) {
            listener.onComplete(Tasks.forException(new Exception(message)));
        }
    }

    // ========================= GETTERS AND SETTERS =========================

    public LiveData<Project> getCurrentProject() {
        return currentProject;
    }

    public LiveData<List<Project>> getAllProjects() {
        return allProjects;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public String getCurrentProjectId() {
        return currentProjectId;
    }

    public void setCurrentProjectId(@Nullable String projectId) {
        this.currentProjectId = projectId;
    }

    public ProjectUpdateManager getUpdateManager() {
        return updateManager;
    }

    public void addPdfToProject(@NonNull String projectId, @NonNull Uri pdfUri, @NonNull String fileName, @Nullable OnCompleteListener<Void> listener) {
        if (projectId == null || pdfUri == null || fileName == null) {
            handleError("Project ID, PDF URI, and file name are required", listener);
            return;
        }
        StorageReference pdfRef = storageRef.child("projects/" + projectId + "/documents/" + fileName);
        pdfRef.putFile(pdfUri)
                .addOnSuccessListener(taskSnapshot -> {
                    pdfRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                Map<String, Object> docData = new HashMap<>();
                                docData.put("fileName", fileName);
                                docData.put("url", uri.toString());
                                docData.put("timestamp", new Date());
                                db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                                        .document(projectId)
                                        .collection("documents") // "documents" אם לא קבוע
                                        .add(docData)
                                        .addOnSuccessListener(documentReference -> {
                                            if (listener != null) {
                                                listener.onComplete(Tasks.forResult(null));
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            errorMessage.setValue("שגיאה בשמירת פרטי המסמך: " + e.getMessage());
                                            if (listener != null) {
                                                listener.onComplete(Tasks.forException(e));
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> {
                                errorMessage.setValue("שגיאה בהשגת URL: " + e.getMessage());
                                if (listener != null) {
                                    listener.onComplete(Tasks.forException(e));
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("שגיאה בהעלאת מסמך: " + e.getMessage());
                    if (listener != null) {
                        listener.onComplete(Tasks.forException(e));
                    }
                });
    }

    public void saveBankDetailsToProject(@NonNull String projectId, @NonNull BankDetails bankDetails, @Nullable OnCompleteListener<Void> listener) {
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .update("bankDetails", bankDetails) // "bankDetails" אם לא קבוע
                .addOnCompleteListener(listener);
    }

    /**
     * שליפת הרשאות משתמש לפי UID
     */
    public void getUserPermissions(@NonNull String userId, @Nullable OnCompleteListener<Appraiser.AccessPermission> listener) {
        if (userId == null || userId.trim().isEmpty()) {
            if (listener != null) listener.onComplete(Tasks.forResult(Appraiser.AccessPermission.VIEWER));
            return;
        }

        db.collection(FirestoreConstants.COLLECTION_APPRAISERS) // <-- שינוי כאן
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.w(TAG, "Appraiser document not found for user: " + userId + ". Defaulting to VIEWER.");
                        if (listener != null) listener.onComplete(Tasks.forResult(Appraiser.AccessPermission.VIEWER));
                        return;
                    }

                    try {
                        Appraiser appraiser = documentSnapshot.toObject(Appraiser.class);
                        Appraiser.AccessPermission permission = (appraiser != null && appraiser.getAccessPermissions() != null)
                                ? appraiser.getAccessPermissions()
                                : Appraiser.AccessPermission.USER;

                        if (listener != null) listener.onComplete(Tasks.forResult(permission));
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing appraiser permissions for " + userId + ": " + e.getMessage());
                        if (listener != null) listener.onComplete(Tasks.forResult(Appraiser.AccessPermission.USER));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user permissions for " + userId + ": " + e.getMessage());
                    if (listener != null) listener.onComplete(Tasks.forResult(Appraiser.AccessPermission.USER));
                });
    }

    /**
     * ** 🆕 מתודה חדשה: טעינת כל השמאים במערכת **
     * (צריך קולקציית "appraisers" ב-Firestore).
     *
     * @param listener מאזין לקבלת רשימת השמאים.
     */
    public void getAllAppraisers(@Nullable OnCompleteListener<List<Appraiser>> listener) {
        db.collection(FirestoreConstants.COLLECTION_APPRAISERS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Appraiser> appraisers = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            try {
                                Appraiser appraiser = doc.toObject(Appraiser.class);
                                if (appraiser != null) {
                                    appraiser.setAppraiserId(doc.getId()); // ודא שיש ID
                                    appraisers.add(appraiser);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing appraiser document " + doc.getId() + ": " + e.getMessage());
                            }
                        }
                        if (listener != null) listener.onComplete(Tasks.forResult(appraisers));
                    } else {
                        Log.e(TAG, "Failed to get all appraisers: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        if (listener != null) listener.onComplete(Tasks.forException(task.getException() != null ? task.getException() : new Exception("Failed to load appraisers")));
                    }
                });
    }

    /**
     * טוענת פרויקטים **פעילים** שבהם המשתמש הוא:
     * 1. השמאי הראשי (appraiserId)
     * 2. אחד מהשמאים השותפים (co_appraiser_ids)
     * ושהסטטוס שלהם אינו "completed" או "cancelled".
     *
     * @param userId ה-UID של המשתמש הנוכחי
     */
    public void loadActiveProjectsForAppraiser(@NonNull String userId) {
        lastUsedUserIdForListening = userId;
        stopListening();

        // שאילתה 1: שמאי ראשי, פרויקטים פעילים
        Task<QuerySnapshot> query1 = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .whereEqualTo(FirestoreConstants.FIELD_APPRAISER_ID, userId)
                .whereNotIn(FirestoreConstants.FIELD_PROJECT_STATUS,
                        List.of(FirestoreConstants.STATUS_COMPLETED, FirestoreConstants.STATUS_CANCELLED)) // <-- הוספת תנאי סינון
                .get();

        // שאילתה 2: שמאי שותף, פרויקטים פעילים
        Task<QuerySnapshot> query2 = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .whereArrayContains(FirestoreConstants.FIELD_CO_APPRAISER_IDS, userId)
                .whereNotIn(FirestoreConstants.FIELD_PROJECT_STATUS,
                        List.of(FirestoreConstants.STATUS_COMPLETED, FirestoreConstants.STATUS_CANCELLED)) // <-- הוספת תנאי סינון
                .get();

        Tasks.whenAllSuccess(query1, query2)
                .addOnSuccessListener(results -> {
                    Set<Project> combinedProjects = new HashSet<>();
                    for (Object result : results) {
                        QuerySnapshot snapshot = (QuerySnapshot) result;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Project p = safeProjectFrom(doc);
                            if (p != null) {
                                combinedProjects.add(p);
                            }
                        }
                    }

                    List<Project> sortedProjects = new ArrayList<>(combinedProjects);
                    sortedProjects.sort(Comparator.comparingLong(Project::getLastUpdateDateMillis).reversed());

                    allProjects.setValue(sortedProjects);
                    Log.d(TAG, "Loaded " + sortedProjects.size() + " ACTIVE projects for user " + userId + " (Primary/Co-Appraiser).");
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("שגיאה בטעינת פרויקטים פעילים עבור משתמש: " + e.getMessage());
                    allProjects.setValue(new ArrayList<>());
                    Log.e(TAG, "Error loading active projects for user " + userId + ": " + e.getMessage());
                });
    }

    /**
     * מעדכן את רשימת השמאים השותפים בפרויקט, וכן מעדכן את רשימת הפרויקטים המוקצים
     * (בתוך activeProjects) במסמכי השמאים הרלוונטיים.
     *
     * @param projectId ה-ID של הפרויקט לעדכון.
     * @param newCoAppraiserIds רשימת ה-UID של השמאים שנבחרו להיות שותפים כעת.
     * @param listener מאזין להשלמת הפעולה.
     */
    public void updateProjectAndAppraiserAssignments(
            @NonNull String projectId,
            @NonNull List<String> newCoAppraiserIds,
            @Nullable OnCompleteListener<Void> listener
    ) {
        // שלב 1: קבל את הפרויקט הנוכחי כדי לדעת מי היו השמאים הקודמים
        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Project existingProject = documentSnapshot.toObject(Project.class);
                    if (existingProject == null) {
                        handleError(FirestoreConstants.ERROR_PROJECT_NOT_FOUND_WITH_ID + projectId, listener);
                        return;
                    }

                    // שמאים שהיו קודם ואינם ברשימה החדשה (צריך להסיר מהם את הפרויקט)
                    List<String> oldCoAppraiserIds = (existingProject.getCoAppraiserIds() != null)
                            ? existingProject.getCoAppraiserIds() : new ArrayList<>();

                    Set<String> toRemoveFromAppraisers = new HashSet<>(oldCoAppraiserIds);
                    toRemoveFromAppraisers.removeAll(newCoAppraiserIds);

                    // שמאים חדשים שנוספו (צריך להוסיף להם את הפרויקט)
                    Set<String> toAddToAppraisers = new HashSet<>(newCoAppraiserIds);
                    toAddToAppraisers.removeAll(oldCoAppraiserIds);

                    // וודא שגם יוצר הפרויקט מטופל, אם הוא לא נמצא ב-coAppraiserIds אבל צריך להיות לו ProjectID
                    // הוסף את יוצר הפרויקט לרשימת השמאים שצריך לעדכן (אם לא ברשימה הנוכחית של co-appraisers)
                    String mainAppraiserId = existingProject.getAppraiserId();
                    if (mainAppraiserId != null) {
                        // אם הוא השמאי הראשי, וודא שהפרויקט מוקצה לו
                        if (!oldCoAppraiserIds.contains(mainAppraiserId) && !newCoAppraiserIds.contains(mainAppraiserId)) {
                            // אם הוא לא היה ברשימת השותפים הקודמת וגם לא ברשימה החדשה (כשותף),
                            // וודא שהפרויקט נוסף ל-activeProjects שלו.
                            toAddToAppraisers.add(mainAppraiserId);
                        } else if (oldCoAppraiserIds.contains(mainAppraiserId) && !newCoAppraiserIds.contains(mainAppraiserId)) {
                            // אם הוא היה שותף קודם והוסר מרשימת השותפים החדשה, אבל הוא עדיין השמאי הראשי,
                            // וודא שהוא לא מוסר מהפרויקטים שהוקצו לו.
                            // במקרה כזה, הסר אותו מ-toRemoveFromAppraisers
                            toRemoveFromAppraisers.remove(mainAppraiserId);
                        }
                    }


                    // שלב 2: עדכן את הפרויקט עצמו
                    Map<String, Object> projectUpdates = new HashMap<>();
                    projectUpdates.put(FirestoreConstants.FIELD_CO_APPRAISER_IDS, newCoAppraiserIds);
                    projectUpdates.put(FirestoreConstants.FIELD_LAST_UPDATE_DATE, com.google.firebase.firestore.FieldValue.serverTimestamp());

                    // שלב 3: צור רשימת משימות לעדכוני שמאים
                    List<Task<Void>> appraiserUpdateTasks = new ArrayList<>();

                    // הסר את הפרויקט מהשמאים שהוסרו (אם הפרויקט לא הוקצה להם דרך appraiserId)
                    for (String appraiserId : toRemoveFromAppraisers) {
                        // וודא שאנחנו לא מנסים להסיר את הפרויקט מהשמאי הראשי אם הוא נמחק מרשימת השותפים
                        if (mainAppraiserId != null && mainAppraiserId.equals(appraiserId)) {
                            continue; // השמאי הראשי תמיד מקושר לפרויקט
                        }
                        DocumentReference appraiserRef = db.collection(FirestoreConstants.COLLECTION_APPRAISERS).document(appraiserId);
                        appraiserUpdateTasks.add(appraiserRef.update(
                                FirestoreConstants.FIELD_APPRAISER_ASSIGNED_PROJECTS,
                                com.google.firebase.firestore.FieldValue.arrayRemove(projectId)
                        ));
                    }

                    // הוסף את הפרויקט לשמאים החדשים
                    for (String appraiserId : toAddToAppraisers) {
                        DocumentReference appraiserRef = db.collection(FirestoreConstants.COLLECTION_APPRAISERS).document(appraiserId);
                        appraiserUpdateTasks.add(appraiserRef.update(
                                FirestoreConstants.FIELD_APPRAISER_ASSIGNED_PROJECTS,
                                com.google.firebase.firestore.FieldValue.arrayUnion(projectId)
                        ));
                    }

                    // שלב 4: בצע את כל העדכונים
                    Tasks.whenAll(appraiserUpdateTasks)
                            .continueWithTask(task -> {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }
                                return db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId).update(projectUpdates);
                            })
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Successfully updated project assignments and appraiser lists for project " + projectId);
                                } else {
                                    Log.e(TAG, "Failed to update project assignments or appraiser lists for project " + projectId + ": " + task.getException().getMessage());
                                    errorMessage.setValue("שגיאה בעדכון הקצאות פרויקט: " + task.getException().getMessage());
                                }
                                if (listener != null) listener.onComplete(task);
                            });

                })
                .addOnFailureListener(e -> {
                    handleError("שגיאה בקבלת פרטי הפרויקט לפני עדכון הקצאות: " + e.getMessage(), listener);
                });
    }
}