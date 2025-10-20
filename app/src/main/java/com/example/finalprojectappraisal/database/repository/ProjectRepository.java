package com.example.finalprojectappraisal.database.repository;

import android.net.Uri;

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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class ProjectRepository {

    private static ProjectRepository instance;
    public static synchronized ProjectRepository getInstance() {
        if (instance == null) instance = new ProjectRepository();
        return instance;
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Services
    private final ProjectDataService dataService;
    private final ProjectMediaService mediaService;
    private final ProjectLiveListeners liveListeners;
    private final AppraiserRepository appraiserRepo;

    // Updater (קיים אצלך)
    private final ProjectUpdateManager updateManager;

    // State
    private final MutableLiveData<Project> currentProject = new MutableLiveData<>();
    private final MutableLiveData<List<Project>> allProjects = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private String currentProjectId;
    private String lastUsedUserIdForListening;
    private final ProjectAssignmentService assignmentService;

    private ProjectRepository() {
        this.dataService = new ProjectDataService(db, errorMessage);
        this.mediaService = new ProjectMediaService(db, errorMessage, this::refreshCurrentProject, () -> currentProjectId);
        this.liveListeners = new ProjectLiveListeners(db, dataService, allProjects, errorMessage);
        this.appraiserRepo = AppraiserRepository.getInstance();
        this.updateManager = new ProjectUpdateManager(db, errorMessage);
        this.assignmentService = new ProjectAssignmentService(db, errorMessage);

    }

    // ===== Creation =====
    public void createNewProject(@NonNull Project project, @Nullable OnCompleteListener<Void> listener) {
        if (project == null) {
            emitErr("Project cannot be null", listener);
            return;
        }
        var v = ProjectDataValidator.validateProject(project);
        if (!v.isValid()) {
            emitErr("Validation failed: " + v.getErrorsAsString(), listener);
            return;
        }
        var newRef = db.collection(FirestoreConstants.COLLECTION_PROJECTS).document();
        project.setProjectId(newRef.getId());
        newRef.set(project)
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> errorMessage.setValue(FirestoreConstants.ERROR_CREATING_PROJECT + ": " + e.getMessage()));
    }

    // ===== Retrieval =====
    public void getProject(@NonNull String projectId, @Nullable OnCompleteListener<DocumentSnapshot> listener) {
        dataService.getProject(projectId, listener);
    }

    public void loadProject(@NonNull String projectId) {
        if (projectId.trim().isEmpty()) {
            errorMessage.setValue(FirestoreConstants.ERROR_PROJECT_NOT_FOUND); return;
        }
        dataService.getProject(projectId, t -> {
            if (t.isSuccessful() && t.getResult()!=null && t.getResult().exists()) {
                Project p = dataService.safeProjectFrom(t.getResult());
                currentProjectId = projectId;
                currentProject.setValue(p);
            } else {
                errorMessage.setValue(FirestoreConstants.ERROR_PROJECT_NOT_FOUND_WITH_ID + projectId);
            }
        });
    }

    public void loadProjectsForAppraiser(@NonNull String userId) {
        lastUsedUserIdForListening = userId;
        stopListening();
        dataService.loadProjectsForAppraiser(userId, allProjects);
    }

    public void loadActiveProjectsForAppraiser(@NonNull String userId) {
        lastUsedUserIdForListening = userId;
        stopListening();
        dataService.loadActiveProjectsForAppraiser(userId, allProjects);
    }

    public void loadProjectsByStatus(@NonNull String status, @Nullable OnCompleteListener<List<Project>> listener) {
        dataService.loadProjectsByStatus(status, listener);
    }


    // ===== Live listeners =====
    public void loadAllProjectsWithListener() { stopListening(); liveListeners.listenAll(); }
    public void stopListening() { liveListeners.stop(); }

    // ===== Updates via UpdateManager =====
    public void saveClientDetails(@NonNull String projectId, @NonNull Client client, @Nullable OnCompleteListener<Void> l) {
        var v = ProjectDataValidator.validateClient(client);
        if (!v.isValid()) { emitErr("Client validation failed: " + v.getErrorsAsString(), l); return; }
        updateManager.saveClientDetails(projectId, client, l);
    }
    public void savePropertyDetails(@NonNull String projectId, @NonNull Map<String,Object> props, @Nullable OnCompleteListener<Void> l) {
        updateManager.savePropertyDetails(projectId, props, l);
    }
    public void saveApartmentDetails(@NonNull String projectId, @NonNull Map<String,Object> apt, @Nullable OnCompleteListener<Void> l) {
        var v = ProjectDataValidator.validateApartmentDetails(apt);
        if (!v.isValid()) { emitErr("Apartment details validation failed: " + v.getErrorsAsString(), l); return; }
        updateManager.saveApartmentDetails(projectId, apt, task -> {
            if (!task.isSuccessful()) { if (l!=null) l.onComplete(task); return; }
            updateManager.cleanupRootDuplicateFields(projectId, l);
        });
    }
    public void cleanupRootDuplicates(@NonNull String projectId, @Nullable OnCompleteListener<Void> l) {
        updateManager.cleanupRootDuplicateFields(projectId, l);
    }
    public void updateMultipleFields(@NonNull String projectId, @NonNull Map<String,Object> fields, @Nullable OnCompleteListener<Void> l) {
        updateManager.updateMultipleFields(projectId, fields, l);
    }

    public void saveBankDetailsToProject(@NonNull String projectId,
                                         @NonNull BankDetails bankDetails,
                                         @Nullable OnCompleteListener<Void> listener) {
        Map<String, Object> fields = Collections.singletonMap(
                FirestoreConstants.FIELD_BANK_DETAILS, bankDetails
        );
        updateManager.updateMultipleFields(projectId, fields, listener);
    }

    // ===== Deletion =====
    public void deleteProject(@NonNull String projectId, @Nullable OnCompleteListener<Void> l) {
        dataService.deleteProject(projectId, task -> {
            if (task.isSuccessful()) {
                if (projectId.equals(currentProjectId)) { currentProjectId = null; currentProject.setValue(null); }
                if (lastUsedUserIdForListening != null) loadProjectsForAppraiser(lastUsedUserIdForListening);
                else loadAllProjectsWithListener();
            }
            if (l != null) l.onComplete(task);
        });
    }

    // ===== Images & Documents =====
    public String buildImageStoragePath(@NonNull String projectId, @NonNull String imageId) {
        return mediaService.buildImageStoragePath(projectId, imageId);
    }
    public void saveImagePaths(@NonNull String projectId, @NonNull Map<String,Object> imagePaths, @Nullable OnCompleteListener<Void> l) {
        mediaService.saveImagePaths(projectId, imagePaths, l);
    }
    public void loadAllImagesForProject(@NonNull String projectId, @NonNull OnCompleteListener<List<Image>> l) {
        mediaService.loadAllImagesForProject(projectId, l);
    }
    public void uploadImageToStorage(@NonNull String projectId, @NonNull Uri localUri, @NonNull String imageId, @NonNull OnCompleteListener<String> l) {
        mediaService.uploadImageToStorage(projectId, localUri, imageId, l);
    }
    public void addImageToProject(String projectId, Image image, @Nullable OnCompleteListener<Void> l) {
        mediaService.addImageToProject(projectId, image, l);
    }
    public void deleteImageFromProject(@NonNull String projectId, @NonNull String imageId, @Nullable OnCompleteListener<Void> l) {
        mediaService.deleteImageFromProject(projectId, imageId, l);
    }
    public void getImagesForProject(@NonNull String projectId, @Nullable OnCompleteListener<List<Image>> l) {
        mediaService.getImagesForProject(projectId, l);
    }
    public void upsertPropertyImage(@NonNull String projectId, @NonNull String name, @NonNull String path, @Nullable OnCompleteListener<Void> l) {
        mediaService.upsertPropertyImage(projectId, name, path, l);
    }
    public void removePropertyImageIfMatches(@NonNull String projectId, @NonNull String name, @NonNull String expectedPath, @Nullable OnCompleteListener<Void> l) {
        mediaService.removePropertyImageIfMatches(projectId, name, expectedPath, l);
    }
    public void addPdfToProject(@NonNull String projectId, @NonNull Uri pdfUri, @NonNull String fileName, @Nullable OnCompleteListener<Void> l) {
        mediaService.addPdfToProject(projectId, pdfUri, fileName, l);
    }

    // ===== Summary fields =====
    public void getProjectFieldsForSummary(String projectId, OnCompleteListener<Map<String,Object>> l) {
        dataService.getProjectFieldsForSummary(projectId, l);
    }

    // ===== Appraisers (delegation) =====
    public void getUserPermissions(@NonNull String userId, @Nullable OnCompleteListener<Appraiser.AccessPermission> l) {
        appraiserRepo.getUserPermissions(userId, l);
    }
    public void getAllAppraisers(@Nullable OnCompleteListener<List<Appraiser>> l) {
        appraiserRepo.getAllAppraisers(l);
    }

    // ===== Assignments (co-appraisers) =====
    public void updateProjectAndAppraiserAssignments(
            @NonNull String projectId,
            @NonNull List<String> newCoAppraiserIds,
            @Nullable OnCompleteListener<Void> listener
    ) {
        assignmentService.updateProjectAndAppraiserAssignments(projectId, newCoAppraiserIds, listener);
    }

    // ===== Utils / state =====
    public void refreshCurrentProject() { if (currentProjectId != null) loadProject(currentProjectId); }
    public void projectExists(@NonNull String projectId, @Nullable OnCompleteListener<Boolean> l) { dataService.projectExists(projectId, l); }

    public LiveData<Project> getCurrentProject() { return currentProject; }
    public LiveData<List<Project>> getAllProjects() { return allProjects; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public String getCurrentProjectId() { return currentProjectId; }
    public void setCurrentProjectId(@Nullable String projectId) { this.currentProjectId = projectId; }

    private void emitErr(@NonNull String msg, @Nullable OnCompleteListener<Void> l) {
        errorMessage.setValue(msg);
        if (l != null) l.onComplete(Tasks.forException(new Exception(msg)));
    }
}
