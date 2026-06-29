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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Summary:
 * ViewModel for UploadImagesActivity. Owns all DB/Storage work:
 * - Load existing images for a project
 * - Upload a picked image to Firebase Storage
 * - Persist image document under projects/{id}/images/{imageId}
 * - Maintain property image arrays (front_image / interior_image)
 * - Delete image from project and cleanup arrays
 * - Update feature flags (hasParking / hasStorageRoom) based on image category
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

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<List<Image>> getExisting() { return existing; }
    public LiveData<Image> getLastSavedImage() { return lastSavedImage; }
    public LiveData<Boolean> getLastDeleteOk() { return lastDeleteOk; }

    public void init(@NonNull String projectId) {
        this.projectId = projectId;
        loadExistingImages();
    }

    public void loadExistingImages() {
        if (projectId == null) return;
        loading.setValue(true);
        repo.getImagesForProject(projectId, task -> {
            loading.setValue(false);

            if (!task.isSuccessful()) {
                error.setValue("Failed to load existing images");
                return;
            }

            List<Image> list = task.getResult();
            if (list == null) {
                list = new ArrayList<>();
            }

            existing.setValue(list);
            syncFeatureFlagsWithImages(list);
        });
    }

    // ✅ Backward compatible overload (in case some call site still uses old signature)
    public void uploadAndSave(@NonNull android.net.Uri localUri,
                              @NonNull Image.Category category,
                              @Nullable String promptForInfo) {
        uploadAndSave(localUri, category, promptForInfo, Image.Subcategory.NONE, 0,
                com.example.finalprojectappraisal.model.Floor.DEFAULT_KEY);
    }

    // ✅ Backward compatible overload (without floor → main floor)
    public void uploadAndSave(@NonNull android.net.Uri localUri,
                              @NonNull Image.Category category,
                              @Nullable String promptForInfo,
                              @NonNull Image.Subcategory subCategory,
                              int bedroomIndex) {
        uploadAndSave(localUri, category, promptForInfo, subCategory, bedroomIndex,
                com.example.finalprojectappraisal.model.Floor.DEFAULT_KEY);
    }

    // ✅ Backward compatible overload (without unit → main unit)
    public void uploadAndSave(@NonNull android.net.Uri localUri,
                              @NonNull Image.Category category,
                              @Nullable String promptForInfo,
                              @NonNull Image.Subcategory subCategory,
                              int bedroomIndex,
                              @Nullable String floorKey) {
        uploadAndSave(localUri, category, promptForInfo, subCategory, bedroomIndex, floorKey,
                com.example.finalprojectappraisal.model.Unit.DEFAULT_KEY);
    }

    /**
     * ✅ NEW:
     * Uploads local image → gets https URL → saves image doc → updates property image array.
     * Also persists subCategory + bedroomIndex + floor + unit to Firestore.
     */
    public void uploadAndSave(@NonNull android.net.Uri localUri,
                              @NonNull Image.Category category,
                              @Nullable String promptForInfo,
                              @NonNull Image.Subcategory subCategory,
                              int bedroomIndex,
                              @Nullable String floorKey,
                              @Nullable String unitId) {
        if (projectId == null) {
            error.setValue("Missing projectId");
            return;
        }

        Image image = new Image();
        image.setProjectId(projectId);
        image.setCategory(category);

        // ✅ persist "which room" identity
        image.setSubCategory(subCategory != null ? subCategory : Image.Subcategory.NONE);
        image.setBedroomIndex(bedroomIndex);
        image.setFloor(floorKey);
        image.setUnitId(unitId);

        image.setLocalUri(localUri.toString());
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

            repo.addImageToProject(projectId, image, task2 -> {
                if (!task2.isSuccessful()) {
                    loading.setValue(false);
                    error.setValue("Saving image to DB failed");
                    return;
                }

                String arrayField = arrayNameForCategory(category);

                if (arrayField == null) {
                    loading.setValue(false);
                    lastSavedImage.setValue(image);
                    updateFeatureFlagsForCategory(category);
                    return;
                }

                repo.upsertPropertyImage(projectId, arrayField, downloadUrl, task3 -> {
                    loading.setValue(false);
                    if (!task3.isSuccessful()) {
                        error.setValue("Updating property image array failed");
                    }
                    lastSavedImage.setValue(image);
                    updateFeatureFlagsForCategory(category);
                });
            });
        });
    }

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
            default:
                return null;
        }
    }

    private void syncFeatureFlagsWithImages(@NonNull List<Image> images) {
        if (projectId == null || images == null) return;

        boolean hasParking = false;
        boolean hasStorage = false;

        for (Image image : images) {
            if (image == null || image.getCategory() == null) continue;

            switch (image.getCategory()) {
                case PARKING:
                    hasParking = true;
                    break;
                case STORAGE:
                    hasStorage = true;
                    break;
                default:
                    break;
            }

            if (hasParking && hasStorage) {
                break;
            }
        }

        Map<String, Boolean> updates = new HashMap<>();
        updates.put(FirestoreConstants.FIELD_HAS_PARKING, hasParking);
        updates.put(FirestoreConstants.FIELD_HAS_PARKING_SNAKE, hasParking);
        updates.put(FirestoreConstants.FIELD_HAS_STORAGE_ROOM, hasStorage);
        updates.put(FirestoreConstants.FIELD_HAS_STORAGE_ROOM_SNAKE, hasStorage);

        pushFlagUpdates(updates, "Failed to sync parking/storage flags");
    }

    private void updateFeatureFlagsForCategory(@Nullable Image.Category category) {
        if (projectId == null || category == null) return;

        switch (category) {
            case STORAGE:
                Map<String, Boolean> storageUpdate = new HashMap<>();
                storageUpdate.put(FirestoreConstants.FIELD_HAS_STORAGE_ROOM, true);
                storageUpdate.put(FirestoreConstants.FIELD_HAS_STORAGE_ROOM_SNAKE, true);
                pushFlagUpdates(storageUpdate, "Failed to update storage flag");
                break;

            case PARKING:
                Map<String, Boolean> parkingUpdate = new HashMap<>();
                parkingUpdate.put(FirestoreConstants.FIELD_HAS_PARKING, true);
                parkingUpdate.put(FirestoreConstants.FIELD_HAS_PARKING_SNAKE, true);
                pushFlagUpdates(parkingUpdate, "Failed to update parking flag");
                break;

            default:
                break;
        }
    }

    private void pushFlagUpdates(@NonNull Map<String, Boolean> flags, @NonNull String errorMessage) {
        if (projectId == null || flags == null || flags.isEmpty()) return;

        Map<String, Object> payload = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : flags.entrySet()) {
            payload.put(entry.getKey(), entry.getValue());
        }

        repo.updateMultipleFields(projectId, payload, task -> {
            if (!task.isSuccessful()) {
                error.setValue(errorMessage);
            }
        });
    }
}
