package com.example.finalprojectappraisal.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.database.updater.ProjectUpdateManager;
import com.example.finalprojectappraisal.database.validator.ProjectDataValidator;
import com.example.finalprojectappraisal.model.BankDetails;
import com.example.finalprojectappraisal.model.Client;
import com.example.finalprojectappraisal.model.Project;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.finalprojectappraisal.model.Image;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import android.net.Uri;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;



/**
 * Main repository class that handles Firebase Firestore operations for projects.
 * Uses Singleton pattern and delegates complex operations to specialized managers.
 * Focuses on core CRUD operations and data flow management.
 */
public class ProjectRepository {

    private static ProjectRepository instance;
    private final FirebaseFirestore db;
    private final ProjectUpdateManager updateManager;
    private String currentProjectId;

    // ADDED: Firebase Storage for documents (Bank details PDFs, etc.)
    private final FirebaseStorage storage;
    private final StorageReference storageRef;



    // LiveData for reactive programming
    private final MutableLiveData<Project> currentProject = new MutableLiveData<>();
    private final MutableLiveData<List<Project>> allProjects = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Listener for live queries (all projects / by appraiser)
    private ListenerRegistration allProjectsListener;
    // נשמור את ה-appraiserId האחרון ששימש לטעינה, כדי לרענן אחרי פעולות (מחיקה למשל)
    private String lastUsedAppraiserIdString = null;


    private ProjectRepository() {
        db = FirebaseFirestore.getInstance();
        updateManager = new ProjectUpdateManager(db, errorMessage);  // Initialize Firebase Storage
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

    /**
     * Creates a new project in Firestore with auto-generated ID
     */
    public void createNewProject(Project project, OnCompleteListener<Void> listener) {
        if (project == null) {
            handleError("Project cannot be null", listener);
            return;
        }

        // Validate project data
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

    /**
     * Creates a project with specific address as document ID
     */
    public void createProjectWithAddress(String fullAddress, Project project, OnCompleteListener<Void> listener) {
        if (fullAddress == null || fullAddress.trim().isEmpty()) {
            handleError("Full address cannot be null or empty", listener);
            return;
        }

        if (project == null) {
            handleError("Project cannot be null", listener);
            return;
        }

        // Validate project data
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

    /**
     * Gets a project by ID and returns DocumentSnapshot
     */
    public void getProject(String projectId, OnCompleteListener<DocumentSnapshot> listener) {
        if (projectId == null || projectId.trim().isEmpty()) {
            handleError(FirestoreConstants.ERROR_PROJECT_NOT_FOUND, null);
            return;
        }

        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .get()
                .addOnCompleteListener(listener);
    }

    /**
     * Loads a project and updates current project LiveData
     */
    public void loadProject(String projectId) {
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
     * Loads all projects and updates allProjects LiveData
     */
    public void loadAllProjects() {
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Project> projects = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        try {
                            Project project = document.toObject(Project.class);
                            if (project != null) {
                                projects.add(project);
                                Log.d("FirestoreDebug", "Loaded project: " + project.getProjectId());
                            }
                        } catch (Exception e) {
                            Log.e("FirestoreDebug", "Error parsing project: " + e.getMessage());
                        }
                    }
                    Log.d("FirestoreDebug", "Loaded " + projects.size() + " projects from Firestore");
                    allProjects.setValue(projects);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreDebug", "Failed to load projects: " + e.getMessage());
                    errorMessage.setValue("Error loading projects: " + e.getMessage());
                });
    }

    /**
     * Loads all projects for a specific appraiser (by appraiserId) and keeps a live listener.
     * appraiserIdValue type MUST match Firestore field type (String or Number).
     */
    public void loadProjectsForAppraiser(Object appraiserIdValue) {
        // detach previous listener (avoid double updates)
        if (allProjectsListener != null) {
            allProjectsListener.remove();
            allProjectsListener = null;
        }
        if (appraiserIdValue == null) {
            allProjects.setValue(new ArrayList<>());
            errorMessage.setValue("appraiserIdValue is null");
            return;
        }
        Query q = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .whereEqualTo("appraiserId", appraiserIdValue)
                .orderBy("lastUpdateDate", Query.Direction.DESCENDING);
        allProjectsListener = q.addSnapshotListener((snap, err) -> {
            if (err != null) {
                Log.e("FirestoreDebug", "listen error: " + err.getMessage());
                allProjects.setValue(new ArrayList<>());
                errorMessage.setValue("שגיאה בטעינת פרויקטים: " + err.getMessage());
                return;
            }
            List<Project> out = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot d : snap.getDocuments()) {
                    Project p = safeProjectFrom(d);
                    if (p != null) out.add(p);
                }
            }
            Log.d("FirestoreDebug", "Loaded (by appraiser) " + out.size() + " projects");
            allProjects.setValue(out);
        });
    }

    public void loadActiveProjectsForAppraiser(String appraiserId) {
        String TAG = "ProjectRepository";
        Log.d(TAG, "Start loading active projects for appraiserId=" + appraiserId);

        db.collection("appraisers")
                .document(appraiserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Log.d(TAG, "Appraiser document fetch success. exists=" + snapshot.exists());

                    if (!snapshot.exists()) {
                        Log.w(TAG, "Appraiser document not found for id=" + appraiserId);
                        allProjects.setValue(new ArrayList<>());
                        return;
                    }

                    Object idsObj = snapshot.get("activeProjects");
                    List<String> projectIds = new ArrayList<>();
                    if (idsObj instanceof List) {
                        for (Object o : (List<?>) idsObj) {
                            if (o != null) projectIds.add(String.valueOf(o));
                        }
                    }

                    Log.d(TAG, "Parsed activeProjects: " + projectIds);

                    if (projectIds.isEmpty()) {
                        Log.d(TAG, "No active projects for appraiserId=" + appraiserId);
                        allProjects.setValue(new ArrayList<>());
                        return;
                    }

                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                    for (String projectId : projectIds) {
                        Log.d(TAG, "Fetching projectId=" + projectId);
                        tasks.add(db.collection("projects").document(projectId).get());
                    }

                    Tasks.whenAllSuccess(tasks)
                            .addOnSuccessListener(results -> {
                                Log.d(TAG, "Successfully fetched all projects. count=" + results.size());
                                List<Project> loaded = new ArrayList<>();
                                for (Object obj : results) {
                                    DocumentSnapshot doc = (DocumentSnapshot) obj;
                                    if (doc.exists()) {
                                        Project p = doc.toObject(Project.class);
                                        if (p != null) {
                                            p.setProjectId(doc.getId());
                                            loaded.add(p);
                                            Log.d(TAG, "Loaded project: " + doc.getId());
                                        } else {
                                            Log.w(TAG, "toObject returned null for projectId=" + doc.getId());
                                        }
                                    } else {
                                        Log.w(TAG, "Project document does not exist: " + doc.getId());
                                    }
                                }
                                allProjects.setValue(loaded);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error loading project documents", e);
                                allProjects.setValue(new ArrayList<>());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching appraiser document", e);
                    allProjects.setValue(new ArrayList<>());
                });
    }



    /** Convenience wrappers when you know the type */
    /** טען פרויקטים לפי appraiserId (String) עם מאזין חי. */
    // גרסה עם fallback: נסה עם orderBy, ואם יש FAILED_PRECONDITION -> נופל חזרה ללא מיון
    public void loadProjectsForAppraiserIdString(@NonNull String appraiserId) {
        lastUsedAppraiserIdString = appraiserId;
        if (allProjectsListener != null) { allProjectsListener.remove(); allProjectsListener = null; }
        Query q = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .whereEqualTo("appraiserId", appraiserId)
                .orderBy("lastUpdateDate", Query.Direction.DESCENDING);
        allProjectsListener = q.addSnapshotListener((snap, err) -> {
            if (err != null) {
                // אין אינדקס? ננסה מיד ללא מיון כדי שלא תהיי תקועה
                if (err instanceof FirebaseFirestoreException &&
                        ((FirebaseFirestoreException) err).getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    loadProjectsForAppraiserNoOrder(appraiserId); // <-- ראי מטה
                    return;
                }
                Log.e("FirestoreDebug", "listen error: " + err.getMessage());
                allProjects.setValue(new ArrayList<>());
                errorMessage.setValue("שגיאה בטעינת פרויקטים: " + err.getMessage());
                return;
            }
            List<Project> out = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot d : snap.getDocuments()) {
                    Project p = safeProjectFrom(d);
                    if (p != null) out.add(p);
                }
            }
            allProjects.setValue(out);
        });
    }


    // בלי מיון (עד שיש אינדקס)
    public void loadProjectsForAppraiserNoOrder(@NonNull String appraiserId) {
        if (allProjectsListener != null) { allProjectsListener.remove(); allProjectsListener = null; }
        Query q = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .whereEqualTo("appraiserId", appraiserId);
        allProjectsListener = q.addSnapshotListener((snap, err) -> {
            if (err != null) {
                Log.e("FirestoreDebug", "listen error(no order): " + err.getMessage());
                allProjects.setValue(new ArrayList<>());
                errorMessage.setValue("שגיאה בטעינת פרויקטים: " + err.getMessage());
                return;
            }
            List<Project> out = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot d : snap.getDocuments()) {
                    Project p = safeProjectFrom(d);
                    if (p != null) out.add(p);
                }
            }
            // מיון בצד הלקוח (רק לתצוגה)
            out.sort((a,b) -> Long.compare(
                    b.getLastUpdateDate() == 0 ? Long.MIN_VALUE : b.getLastUpdateDate(),
                    a.getLastUpdateDate() == 0 ? Long.MIN_VALUE : a.getLastUpdateDate()
            ));
            allProjects.setValue(out);
        });
    }
    public void loadProjectsForAppraiserIdLong(long appraiserId) {
        loadProjectsForAppraiser(appraiserId);
    }

    public void stopListening() {
        if (allProjectsListener != null) {
            allProjectsListener.remove();
            allProjectsListener = null;
        }
    }

    /**
     * Loads projects with a specific status
     */
    public void loadProjectsByStatus(String status, OnCompleteListener<List<Project>> listener) {
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
                    listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(projects));
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue(FirestoreConstants.ERROR_LOADING_PROJECTS + ": " + e.getMessage());
                    listener.onComplete(com.google.android.gms.tasks.Tasks.forException(e));
                });
    }

    // ========================= PROJECT UPDATES (DELEGATED) =========================

    /**
     * Saves client details to the project (delegated to UpdateManager)
     */
    public void saveClientDetails(String projectId, Client client, OnCompleteListener<Void> listener) {
        ProjectDataValidator.ValidationResult validation = ProjectDataValidator.validateClient(client);
        if (!validation.isValid()) {
            handleError("Client validation failed: " + validation.getErrorsAsString(), listener);
            return;
        }

        updateManager.saveClientDetails(projectId, client, listener);
    }

    /**
     * Saves property details to the project (delegated to UpdateManager)
     */
    public void savePropertyDetails(String projectId, Map<String, Object> propertyDetails, OnCompleteListener<Void> listener) {
        updateManager.savePropertyDetails(projectId, propertyDetails, listener);
    }

    /**
     * Saves apartment details to the project (delegated to UpdateManager)
     */
    public void saveApartmentDetails(String projectId, Map<String, Object> apartmentDetails, OnCompleteListener<Void> listener) {
        ProjectDataValidator.ValidationResult validation = ProjectDataValidator.validateApartmentDetails(apartmentDetails);
        if (!validation.isValid()) {
            handleError("Apartment details validation failed: " + validation.getErrorsAsString(), listener);
            return;
        }

        // 1) שמירה ל-property_details

        updateManager.saveApartmentDetails(projectId, apartmentDetails, task -> {
            if (!task.isSuccessful()) {
                if (listener != null) listener.onComplete(task);
                return;
            }
            // 2) ניקוי כפילויות מהשורש דרך ה-wrapper של ה-Repository
            cleanupRootDuplicates(projectId, cleanupTask -> {
                if (listener != null) listener.onComplete(cleanupTask);
            });
        });
    }
    public void cleanupRootDuplicates(String projectId,
                                      com.google.android.gms.tasks.OnCompleteListener<Void> listener) {
        updateManager.cleanupRootDuplicateFields(projectId, listener);
    }

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

    /**
     * Saves property features to the project (delegated to UpdateManager)
     */
    public void savePropertyFeatures(String projectId, Map<String, Object> features, OnCompleteListener<Void> listener) {
        ProjectDataValidator.ValidationResult validation = ProjectDataValidator.validatePropertyFeatures(features);
        if (!validation.isValid()) {
            handleError("Property features validation failed: " + validation.getErrorsAsString(), listener);
            return;
        }

        updateManager.savePropertyFeatures(projectId, features, listener);
    }

    /**
     * Updates project status (delegated to UpdateManager)
     */
    public void updateProjectStatus(String projectId, String status, OnCompleteListener<Void> listener) {
        updateManager.updateProjectStatus(projectId, status, listener);
    }

    /**
     * Updates multiple fields at once (delegated to UpdateManager)
     */
    public void updateMultipleFields(String projectId, Map<String, Object> fieldsToUpdate, OnCompleteListener<Void> listener) {
        updateManager.updateMultipleFields(projectId, fieldsToUpdate, listener);
    }

    // ========================= PROJECT DELETION =========================

    /**
     * Deletes a project from Firestore
     */
    public void deleteProject(String projectId, OnCompleteListener<Void> listener) {
        if (projectId == null || projectId.trim().isEmpty()) {
            handleError(FirestoreConstants.ERROR_PROJECT_NOT_FOUND, listener);
            return;
        }

        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Clear current project if it's the one being deleted
                        if (projectId.equals(currentProjectId)) {
                            currentProjectId = null;
                            currentProject.setValue(null);
                        }
                        if (lastUsedAppraiserIdString != null) {
                            loadProjectsForAppraiserIdString(lastUsedAppraiserIdString);
                        } else {
                            loadAllProjects(); // fallback אם לא השתמשנו במסנן
                        }
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

    public void updateImageInProject(String projectId, Image image, OnCompleteListener<Void> listener) {
        if (projectId == null || image == null) {
            handleError("Project ID and Image are required", listener);
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .collection("images")
                .document(image.getId())
                .set(image.toMap())
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> errorMessage.setValue("שגיאה בעדכון תמונה: " + e.getMessage()));
    }

    public void deleteImageFromProject(String projectId, String imageId, OnCompleteListener<Void> listener) {
        if (projectId == null || imageId == null) {
            handleError("Project ID and Image ID are required", listener);
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .collection("images")
                .document(imageId)
                .delete()
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> errorMessage.setValue("שגיאה במחיקת תמונה: " + e.getMessage()));
    }

    // שליפת כל התמונות של פרויקט:
    // שליפת כל התמונות כ-List<Image> (ללא toObject)
    public void getImagesForProject(String projectId, OnCompleteListener<List<Image>> listener) {
        if (projectId == null) {
            handleError("Project ID is required", null);
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .collection("images")
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Exception e = (task.getException() != null) ? task.getException() : new Exception("images fetch failed");
                        listener.onComplete(Tasks.forException(e));
                        return;
                    }
                    List<Image> out = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        Map<String, Object> data = doc.getData(); // <-- לא toObject(Image.class)
                        if (data == null) continue;
                        try {
                            out.add(new Image(data)); // <-- ממפה מתוך Map<String,Object>
                        } catch (Exception ignore) {}
                    }
                    listener.onComplete(Tasks.forResult(out));
                }) .addOnFailureListener(e -> listener.onComplete(Tasks.forException(e)));

    }

    public void upsertPropertyImage(@NonNull String projectId,
                                    @NonNull String name,
                                    @NonNull String path,
                                    OnCompleteListener<Void> listener) {
        if (projectId.trim().isEmpty() || name.trim().isEmpty() || path.trim().isEmpty()) {
            handleError("projectId/name/path are required", listener);
            return;
        }
        updateManager.upsertPropertyImage(projectId, name, path, task -> {
            if (projectId.equals(currentProjectId)) refreshCurrentProject();
            if (listener != null) listener.onComplete(task);
        });
    }
    public void removePropertyImageIfMatches(@NonNull String projectId,
                                             @NonNull String name,
                                             @NonNull String expectedPath,
                                             OnCompleteListener<Void> listener) {
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
        db.collection("projects").document(projectId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        listener.onComplete(Tasks.forResult(Collections.emptyMap()));
                        return;
                    }
                    DocumentSnapshot doc = task.getResult();
                    Map<String, Object> map = new HashMap<>();
                    // Adapt keys to your @PropertyName mapping:
                    putIfExists(doc, map, "rooms_count");
                    putIfExists(doc, map, "building_condition");
                    putIfExists(doc, map, "has_elevator");
                    putIfExists(doc, map, "has_parking");
                    putIfExists(doc, map, "has_storage");
                    putIfExists(doc, map, "apartment_flooring");
                    putIfExists(doc, map, "apartment_windows");
                    putIfExists(doc, map, "apartment_kitchen");
                    putIfExists(doc, map, "apartment_bathroom_fixtures");
                    // Add more if helpful
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

    /**
     * Checks if a project exists
     */
    public void projectExists(String projectId, OnCompleteListener<Boolean> listener) {
        if (projectId == null || projectId.trim().isEmpty()) {
            listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(false));
            return;
        }

        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(task.getResult().exists()));
                    } else {
                        listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(false));
                    }
                });
    }


    /** Safe mapping from DocumentSnapshot to Project:
     * - אם toObject נכשל, לא חוזרים ריקים: שולפים ידנית שדות עיקריים (status, address, note, client)
     * - משלים projectId מה-docId אם חסר
     * - lastUpdateDate נתמך כ-Timestamp/Long/Double
     */
    private Project safeProjectFrom(DocumentSnapshot d) {
        Project p;
        try {
            p = d.toObject(Project.class);
        } catch (Exception e) {
            Log.w("FirestoreDebug", "toObject failed for " + d.getId() + ": " + e.getMessage());
            p = new Project();
            // 👇 שליפה ידנית של שדות בסיסיים כדי שהכרטיס לא יהיה ריק
            try {
                Object st = d.get(FirestoreConstants.FIELD_PROJECT_STATUS);
                if (st instanceof String) {
                    p.setProjectStatus((String) st);
                }
            } catch (Exception ignore) {}

            try {
                Object addr = d.get(FirestoreConstants.FIELD_FULL_ADDRESS);
                if (addr instanceof String) {
                    p.setFullAddress((String) addr);
                }
            } catch (Exception ignore) {}

            try {
                Object note = d.get("note");
                if (note instanceof String) {
                    p.setNote((String) note);
                }
            } catch (Exception ignore) {}

            // client כמפה -> אובייקט Client (רק אם קיים)
            try {
                Object clientObj = d.get("client");
                if (clientObj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String,Object> cm = (java.util.Map<String,Object>) clientObj;
                    com.example.finalprojectappraisal.model.Client c = new com.example.finalprojectappraisal.model.Client();
                    Object id   = cm.get("id");
                    Object name = cm.get("fullName");
                    Object mail = cm.get("email");
                    Object phone= cm.get("phoneNumber");
                    if (id   instanceof String) c.setClientId((String) id);
                    if (name instanceof String) c.setFullName((String) name);
                    if (mail instanceof String) c.setEmail((String) mail);
                    if (phone instanceof String) c.setPhoneNumber((String) phone);
                    p.setClient(c);
                }
            } catch (Exception ignore) {}
        }

        if (p == null) p = new Project();

        // השלמת projectId מה-doc id אם חסר
        try {
            if (p.getProjectId() == null || p.getProjectId().trim().isEmpty()) {
                p.setProjectId(d.getId());
            }
        } catch (Exception ignore) {}

        // lastUpdateDate כ-Timestamp/Long/Double -> millis
        try {
            Object ts = d.get("lastUpdateDate");
            long millis = 0L;
            if (ts instanceof com.google.firebase.Timestamp) {
                millis = ((com.google.firebase.Timestamp) ts).toDate().getTime();
            } else if (ts instanceof Long) {
                millis = (Long) ts;
            } else if (ts instanceof Double) {
                millis = ((Double) ts).longValue();
            }
            if (p.getLastUpdateDate() == 0L && millis > 0L) {
                p.setLastUpdateDate(millis);
            }
        } catch (Exception e) {
            Log.w("FirestoreDebug", "lastUpdateDate parse failed for " + d.getId() + ": " + e.getMessage());
        }

        return p;
    }


    /**
     * Refreshes the current project data
     */
    public void refreshCurrentProject() {
        if (currentProjectId != null) {
            loadProject(currentProjectId);
        }
    }

    /**
     * Clears all cached data
     */
    public void clearCache() {
        currentProjectId = null;
        currentProject.setValue(null);
        allProjects.setValue(new ArrayList<>());
        errorMessage.setValue(null);
    }

    /**
     * Handles errors consistently
     */
    private void handleError(String message, OnCompleteListener<Void> listener) {
        errorMessage.setValue(message);
        if (listener != null) {
            listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception(message)));
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

    public void setCurrentProjectId(String projectId) {
        this.currentProjectId = projectId;
    }

    /**
     * Gets the update manager for advanced update operations
     */
    public ProjectUpdateManager getUpdateManager() {
        return updateManager;
    }

    public void addPdfToProject(String projectId, Uri pdfUri, String fileName, OnCompleteListener<Void> listener) {
        if (projectId == null || pdfUri == null || fileName == null) {
            handleError("Project ID, PDF URI, and file name are required", listener);
            return;
        }
        // יצירת רפרנס לקובץ ב-Firebase Storage
        StorageReference pdfRef = storageRef.child("projects/" + projectId + "/documents/" + fileName);
        // העלאת הקובץ
        pdfRef.putFile(pdfUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // קבלת URL של הקובץ שהועלה
                    pdfRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                // יצירת מודל והוספתו ל-Firestore
                                Map<String, Object> docData = new HashMap<>();
                                docData.put("fileName", fileName);
                                docData.put("url", uri.toString());
                                docData.put("timestamp", new Date());
                                db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                                        .document(projectId)
                                        .collection("documents") // קולקציה חדשה למסמכים
                                        .add(docData) // שימוש ב-add() ליצירת ID אוטומטי
                                        .addOnSuccessListener(documentReference -> {
                                            // המשימה הצליחה, מעבירים הודעת הצלחה חזרה ל-listener המקורי
                                            if (listener != null) {
                                                listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(null));
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            errorMessage.setValue("שגיאה בשמירת פרטי המסמך: " + e.getMessage());
                                            if (listener != null) {
                                                listener.onComplete(com.google.android.gms.tasks.Tasks.forException(e));
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> {
                                errorMessage.setValue("שגיאה בהשגת URL: " + e.getMessage());
                                if (listener != null) {
                                    listener.onComplete(com.google.android.gms.tasks.Tasks.forException(e));
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("שגיאה בהעלאת מסמך: " + e.getMessage());
                    if (listener != null) {
                        listener.onComplete(com.google.android.gms.tasks.Tasks.forException(e));
                    }
                });
    }
    public void saveBankDetailsToProject(String projectId, BankDetails bankDetails, OnCompleteListener<Void> listener) {
        db.collection("projects")
                .document(projectId)
                .update("bankDetails", bankDetails)
                .addOnCompleteListener(listener);
    }

}