// file: app/src/main/java/com/example/finalprojectappraisal/activity/newProject/images/viewmodel/UploadImagesViewModel.java
package com.example.finalprojectappraisal.activity.newProject.images.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary:
 * ViewModel for UploadImagesActivity. Owns all DB/Storage work:
 * - Load existing images for a project
 * - Upload a picked image to Firebase Storage
 * - Persist image document under projects/{id}/images/{imageId}
 * - Maintain property image arrays (front_image / interior_image)
 * - Delete image from project and cleanup arrays
 *
 * Notes:
 * - Activity handles UI and classification (Gemini) only.
 * - No Context is kept here; repositories perform async work.
 */
public class UploadImagesViewModel extends ViewModel {

    private final ProjectRepository repo = ProjectRepository.getInstance();

    private String projectId;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);
    private final MutableLiveData<List<Image>> existing = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Image> lastSavedImage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> lastDeleteOk = new MutableLiveData<>(null);

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<List<Image>> getExisting() {
        return existing;
    }

    public LiveData<Image> getLastSavedImage() {
        return lastSavedImage;
    }

    public LiveData<Boolean> getLastDeleteOk() {
        return lastDeleteOk;
    }

    public void init(@NonNull String projectId) {
        this.projectId = projectId;
        loadExistingImages();
    }

    public void loadExistingImages() {
        if (projectId == null) return;
        loading.setValue(true);
        repo.getImagesForProject(projectId, task -> {
            loading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                existing.setValue(task.getResult());
            } else {
                error.setValue("Failed to load existing images");
            }
        });
    }

    /**
     * Uploads local image → gets https URL → saves image doc → updates property image array.
     * Emits the final Image (with https url + storagePath) via getLastSavedImage().
     */
    public void uploadAndSave(@NonNull android.net.Uri localUri,
                              @NonNull Image.Category category,
                              @Nullable String promptForInfo) {
        if (projectId == null) {
            error.setValue("Missing projectId");
            return;
        }

        // create an Image instance with a generated ID (Image should generate one if missing)
        Image image = new Image();
        image.setProjectId(projectId);
        image.setCategory(category);
        image.setLocalUri(localUri.toString());
        // keep content Uri as url until https is obtained (Activity may use for immediate UI)
        image.setUrl(localUri.toString());

        loading.setValue(true);

        repo.uploadImageToStorage(projectId, localUri, image.getId(), task1 -> {
            if (!task1.isSuccessful() || task1.getResult() == null) {
                loading.setValue(false);
                error.setValue("Upload to storage failed");
                return;
            }
            String downloadUrl = task1.getResult();
            image.setUrl(downloadUrl);
            image.setStoragePath(repo.buildImageStoragePath(projectId, image.getId()));

            // Save image doc under subcollection
            repo.addImageToProject(projectId, image, task2 -> {
                if (!task2.isSuccessful()) {
                    loading.setValue(false);
                    error.setValue("Saving image to DB failed");
                    return;
                }

                // Update property image array (front_image / interior_image)
                String arrayField = arrayNameForCategory(category);
                if (arrayField == null) {
                    // no array update needed (e.g. קטגוריה OTHER)
                    loading.setValue(false);
                    lastSavedImage.setValue(image);
                    return;
                }

                repo.upsertPropertyImage(projectId, arrayField, downloadUrl, task3 -> {
                    loading.setValue(false);
                    if (!task3.isSuccessful()) {
                        error.setValue("Updating property image array failed");
                    }
                    lastSavedImage.setValue(image);
                });
            });
        });
    }

    /**
     * Deletes an image by id and cleans the property image array if it matches the url.
     * Emits success/failure via getLastDeleteOk().
     */
    public void deleteImage(@NonNull Image image) {
        if (projectId == null) {
            error.setValue("Missing projectId");
            lastDeleteOk.setValue(false);
            return;
        }
        String imageId = image.getId();
        if (imageId == null || imageId.trim().isEmpty()) {
            error.setValue("Missing imageId");
            lastDeleteOk.setValue(false);
            return;
        }

        loading.setValue(true);
        repo.deleteImageFromProject(projectId, imageId, task -> {
            if (!task.isSuccessful()) {
                loading.setValue(false);
                error.setValue("DB delete failed");
                lastDeleteOk.setValue(false);
                return;
            }

            // remove from property array if matches
            String arrayField = arrayNameForCategory(image.getCategory());
            String url = image.getUrl();
            if (arrayField == null || url == null || url.trim().isEmpty()) {
                loading.setValue(false);
                lastDeleteOk.setValue(true);
                return;
            }

            repo.removePropertyImageIfMatches(projectId, arrayField, url, t -> {
                loading.setValue(false);
                if (!t.isSuccessful()) {
                    error.setValue("Array cleanup failed");
                    lastDeleteOk.setValue(false);
                } else {
                    lastDeleteOk.setValue(true);
                }
            });
        });
    }

    @Nullable
    private String arrayNameForCategory(Image.Category category) {
        if (category == null) return null;
        switch (category) {
            case EXTERIOR:
                return FirestoreConstants.FIELD_FRONT_IMAGE;    // "front_image"
            case LIVING_ROOM:
            case BEDROOM:
            case KITCHEN:
            case BATHROOM:
                return FirestoreConstants.FIELD_INTERIOR_IMAGE; // "interior_image"
            // OTHER, VIEW, ENTRANCE_DOOR וכו' לא נכנסים למערך התמונות הראשיות
            default:
                return null;
        }
    }
}
