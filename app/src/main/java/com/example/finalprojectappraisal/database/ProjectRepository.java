package com.example.finalprojectappraisal.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.database.updater.ProjectUpdateManager;
import com.example.finalprojectappraisal.database.validator.ProjectDataValidator;
import com.example.finalprojectappraisal.model.Client;
import com.example.finalprojectappraisal.model.Project;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.finalprojectappraisal.model.Image;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.OnFailureListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // LiveData for reactive programming
    private final MutableLiveData<Project> currentProject = new MutableLiveData<>();
    private final MutableLiveData<List<Project>> allProjects = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private ProjectRepository() {
        db = FirebaseFirestore.getInstance();
        updateManager = new ProjectUpdateManager(db, errorMessage);
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
        ProjectDataValidator.ValidationResult validation = ProjectDataValidator.validatePropertyDetails(propertyDetails);
        if (!validation.isValid()) {
            handleError("Property details validation failed: " + validation.getErrorsAsString(), listener);
            return;
        }

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

        updateManager.saveApartmentDetails(projectId, apartmentDetails, listener);
    }

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
                        // Refresh projects list
                        loadAllProjects();
                    }
                    if (listener != null) {
                        listener.onComplete(task);
                    }
                })
                .addOnFailureListener(e -> errorMessage.setValue(FirestoreConstants.ERROR_DELETING_PROJECT + ": " + e.getMessage()));
    }

    // ========================= IMAGE MANAGEMENT =========================


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
    public void getImagesForProject(String projectId, OnCompleteListener<QuerySnapshot> listener) {
        if (projectId == null) {
            handleError("Project ID is required", null);
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .collection("images")
                .get()
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> errorMessage.setValue("שגיאה בטעינת תמונות: " + e.getMessage()));
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
}