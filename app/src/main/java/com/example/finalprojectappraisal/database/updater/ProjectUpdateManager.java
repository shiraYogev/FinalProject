package com.example.finalprojectappraisal.database.updater;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.database.updater.ProjectFieldUpdater;
import com.example.finalprojectappraisal.model.Client;
import com.example.finalprojectappraisal.model.Project;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

/**
 * Manager class responsible for handling all project update operations.
 * Centralizes update logic and provides a clean interface for project modifications.
 */
public class ProjectUpdateManager {

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
    public void savePropertyDetails(String projectId, Map<String, Object> propertyDetails, OnCompleteListener<Void> listener) {
        updateProject(projectId, project -> ProjectFieldUpdater.updatePropertyFields(project, propertyDetails), listener);
    }

    // ========================= APARTMENT UPDATES =========================

    /**
     * Updates apartment details for a specific project
     */
    public void saveApartmentDetails(String projectId, Map<String, Object> apartmentDetails, OnCompleteListener<Void> listener) {
        updateProject(projectId, project -> ProjectFieldUpdater.updateApartmentFields(project, apartmentDetails), listener);
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
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .set(project)
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> errorMessage.setValue(FirestoreConstants.ERROR_SAVING_PROJECT + ": " + e.getMessage()));
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