package com.example.finalprojectappraisal.database.repository;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.model.Image;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.*;

class ProjectMediaService {

    private static final String TAG = "ProjectMediaService";

    private final FirebaseFirestore db;
    private final MutableLiveData<String> errorOut;
    private final Runnable refreshCurrentProject;
    private final CurrentProjectIdProvider idProvider;

    private final StorageReference storageRef = FirebaseStorage.getInstance().getReference();

    interface CurrentProjectIdProvider { String get(); }

    ProjectMediaService(FirebaseFirestore db,
                        MutableLiveData<String> errorOut,
                        Runnable refreshCurrentProject,
                        CurrentProjectIdProvider idProvider) {
        this.db = db; this.errorOut = errorOut; this.refreshCurrentProject = refreshCurrentProject; this.idProvider = idProvider;
    }

    // ===== images/main merge =====
    void saveImagePaths(@NonNull String projectId, @NonNull Map<String,Object> imagePaths, @Nullable OnCompleteListener<Void> l) {
        if (projectId.trim().isEmpty()) { emit("Project ID is required", l); return; }
        if (imagePaths.isEmpty()) { emit("imagePaths cannot be null or empty", l); return; }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .collection("images").document("main")
                .set(imagePaths, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && projectId.equals(idProvider.get())) refreshCurrentProject.run();
                    if (l != null) l.onComplete(task);
                })
                .addOnFailureListener(e -> errorOut.setValue("שגיאה בשמירת תמונות: " + e.getMessage()));
    }

    void loadAllImagesForProject(@NonNull String projectId, @NonNull OnCompleteListener<List<Image>> l) {
        if (projectId.trim().isEmpty()) { l.onComplete(Tasks.forResult(new ArrayList<>())); return; }

        Task<QuerySnapshot> tSub = db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId).collection("images").get();
        Task<DocumentSnapshot> tMain = db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId).collection("images").document("main").get();

        com.google.android.gms.tasks.Tasks.whenAllComplete(tSub, tMain).addOnCompleteListener(done -> {
            List<Image> out = new ArrayList<>();
            HashSet<String> dedup = new HashSet<>();

            if (tSub.isSuccessful() && tSub.getResult() != null) {
                for (QueryDocumentSnapshot doc : tSub.getResult()) {
                    Map<String,Object> data = doc.getData(); if (data == null) continue;

                    try {
                        Image im = new Image(data);
                        String u = im.getUrl();
                        if (u != null && u.startsWith("http") && dedup.add(u)) { im.setProjectId(projectId); out.add(im); }
                    } catch (Exception ignore) {}

                    for (Map.Entry<String,Object> e : data.entrySet()) {
                        Object v = e.getValue();
                        if (v instanceof List<?>) {
                            for (Object item : (List<?>) v) if (item instanceof String) {
                                String s = (String) item;
                                if (s.startsWith("http") && dedup.add(s)) { Image im2 = new Image(); im2.setProjectId(projectId); im2.setUrl(s); out.add(im2); }
                            }
                            continue;
                        }
                        if (v instanceof String) {
                            String s = (String) v;
                            if (s.startsWith("http") && dedup.add(s)) { Image im2 = new Image(); im2.setProjectId(projectId); im2.setUrl(s); out.add(im2); }
                        }
                    }
                }
            }

            if (tMain.isSuccessful() && tMain.getResult()!=null && tMain.getResult().exists()) {
                Map<String,Object> mainData = tMain.getResult().getData();
                if (mainData != null) {
                    for (Map.Entry<String,Object> e : mainData.entrySet()) {
                        Object v = e.getValue();
                        if (v instanceof List<?>) {
                            for (Object item : (List<?>) v) if (item instanceof String) {
                                String s = (String) item;
                                if (s.startsWith("http") && dedup.add(s)) { Image im = new Image(); im.setProjectId(projectId); im.setUrl(s); out.add(im); }
                            }
                            continue;
                        }
                        if (v instanceof String) {
                            String s = (String) v;
                            if (s.startsWith("http") && dedup.add(s)) { Image im = new Image(); im.setProjectId(projectId); im.setUrl(s); out.add(im); }
                        }
                    }
                }
            }
            l.onComplete(Tasks.forResult(out));
        }).addOnFailureListener(e -> l.onComplete(Tasks.forException(e)));
    }

    String buildImageStoragePath(@NonNull String projectId, @NonNull String imageId) {
        return "projects/" + projectId + "/images/" + imageId;
    }

    void uploadImageToStorage(@NonNull String projectId, @NonNull Uri localUri, @NonNull String imageId, @NonNull OnCompleteListener<String> l) {
        StorageReference ref = storageRef.child(buildImageStoragePath(projectId, imageId));
        ref.putFile(localUri)
                .addOnSuccessListener(ts -> ref.getDownloadUrl().addOnSuccessListener(uri -> l.onComplete(Tasks.forResult(uri.toString())))
                        .addOnFailureListener(e -> l.onComplete(Tasks.forException(e))))
                .addOnFailureListener(e -> l.onComplete(Tasks.forException(e)));
    }

    void addImageToProject(String projectId, Image image, @Nullable OnCompleteListener<Void> l) {
        if (projectId == null || image == null) { emit("Project ID and Image are required", l); return; }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .collection("images").document(image.getId())
                .set(image.toMap())
                .addOnCompleteListener(l)
                .addOnFailureListener(e -> errorOut.setValue("שגיאה בהעלאת תמונה: " + e.getMessage()));
    }

    void deleteImageFromProject(@NonNull String projectId, @NonNull String imageId, @Nullable OnCompleteListener<Void> l) {
        if (projectId == null || imageId == null) { emit("Project ID and Image ID are required", l); return; }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .collection(FirestoreConstants.SUBCOLLECTION_IMAGES).document(imageId)
                .delete().addOnCompleteListener(l)
                .addOnFailureListener(e -> errorOut.setValue("שגיאה במחיקת תמונה: " + e.getMessage()));
    }

    void getImagesForProject(@NonNull String projectId, @Nullable OnCompleteListener<List<Image>> l) {
        if (projectId == null) { if (l!=null) l.onComplete(Tasks.forException(new Exception("Project ID is required"))); return; }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .collection(FirestoreConstants.SUBCOLLECTION_IMAGES).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult()==null) {
                        Exception e = (task.getException()!=null) ? task.getException() : new Exception("images fetch failed");
                        if (l!=null) l.onComplete(Tasks.forException(e)); return;
                    }
                    List<Image> out = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        Map<String,Object> data = doc.getData(); if (data==null) continue;
                        try { out.add(new Image(data)); } catch (Exception ignore) {
                            Log.e(TAG, "Error parsing image "+doc.getId()+": "+ignore.getMessage());
                        }
                    }
                    if (l!=null) l.onComplete(Tasks.forResult(out));
                }).addOnFailureListener(e -> { if (l!=null) l.onComplete(Tasks.forException(e)); });
    }

    void upsertPropertyImage(@NonNull String projectId, @NonNull String name, @NonNull String path, @Nullable OnCompleteListener<Void> l) {
        if (projectId.trim().isEmpty() || name.trim().isEmpty() || path.trim().isEmpty()) { emit("projectId/name/path are required", l); return; }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .collection("images").document("main")
                .set(Collections.singletonMap(name, path), SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (projectId.equals(idProvider.get())) refreshCurrentProject.run();
                    if (l!=null) l.onComplete(task);
                });
    }

    void removePropertyImageIfMatches(@NonNull String projectId, @NonNull String name, @NonNull String expectedPath, @Nullable OnCompleteListener<Void> l) {
        if (projectId.trim().isEmpty() || name.trim().isEmpty() || expectedPath.trim().isEmpty()) { emit("projectId/name/expectedPath are required", l); return; }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .collection("images").document("main")
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists() && expectedPath.equals(doc.getString(name))) {
                        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                                .collection("images").document("main")
                                .update(name, FieldValue.delete())
                                .addOnCompleteListener(task -> {
                                    if (projectId.equals(idProvider.get())) refreshCurrentProject.run();
                                    if (l!=null) l.onComplete(task);
                                });
                    } else {
                        if (l!=null) l.onComplete(Tasks.forResult(null));
                    }
                }).addOnFailureListener(e -> { if (l!=null) l.onComplete(Tasks.forException(e)); });
    }

    // ===== PDF documents =====
    void addPdfToProject(@NonNull String projectId, @NonNull Uri pdfUri, @NonNull String fileName, @Nullable OnCompleteListener<Void> l) {
        if (projectId == null || pdfUri == null || fileName == null) {
            if (l!=null) l.onComplete(Tasks.forException(new Exception("Project ID, PDF URI, and file name are required")));
            return;
        }
        StorageReference pdfRef = storageRef.child("projects/" + projectId + "/documents/" + fileName);
        pdfRef.putFile(pdfUri)
                .addOnSuccessListener(ts -> pdfRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Map<String,Object> docData = new HashMap<>();
                    docData.put("fileName", fileName);
                    docData.put("url", uri.toString());
                    docData.put("timestamp", new Date());
                    db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                            .collection("documents").add(docData)
                            .addOnSuccessListener(r -> { if (l!=null) l.onComplete(Tasks.forResult(null)); })
                            .addOnFailureListener(e -> {
                                errorOut.setValue("שגיאה בשמירת פרטי המסמך: " + e.getMessage());
                                if (l!=null) l.onComplete(Tasks.forException(e));
                            });
                }).addOnFailureListener(e -> {
                    errorOut.setValue("שגיאה בהשגת URL: " + e.getMessage());
                    if (l!=null) l.onComplete(Tasks.forException(e));
                }))
                .addOnFailureListener(e -> {
                    errorOut.setValue("שגיאה בהעלאת מסמך: " + e.getMessage());
                    if (l!=null) l.onComplete(Tasks.forException(e));
                });
    }

    private void emit(String msg, @Nullable OnCompleteListener<Void> l) {
        errorOut.setValue(msg);
        if (l != null) l.onComplete(Tasks.forException(new Exception(msg)));
    }
}
