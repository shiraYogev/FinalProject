package com.example.finalprojectappraisal.database.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.model.Project;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ProjectAssignmentService {

    private final FirebaseFirestore db;
    private final MutableLiveData<String> errorOut;

    ProjectAssignmentService(FirebaseFirestore db, MutableLiveData<String> errorOut) {
        this.db = db;
        this.errorOut = errorOut;
    }

    /**
     * מעדכן את רשימת השמאים השותפים בפרויקט, ומסנכרן את assignedProjects במסמכי ה-appraisers.
     */
    void updateProjectAndAppraiserAssignments(
            @NonNull String projectId,
            @NonNull List<String> newCoAppraiserIds,
            @Nullable OnCompleteListener<Void> listener
    ) {
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .get()
                .addOnSuccessListener((DocumentSnapshot documentSnapshot) -> {
                    Project existingProject = documentSnapshot.toObject(Project.class);
                    if (existingProject == null) {
                        completeWithErr(listener, FirestoreConstants.ERROR_PROJECT_NOT_FOUND_WITH_ID + projectId);
                        return;
                    }

                    // מי היה קודם
                    List<String> oldCoAppraiserIds = (existingProject.getCoAppraiserIds() != null)
                            ? existingProject.getCoAppraiserIds()
                            : new ArrayList<>();

                    // מי להסיר / מי להוסיף
                    Set<String> toRemoveFromAppraisers = new HashSet<>(oldCoAppraiserIds);
                    toRemoveFromAppraisers.removeAll(newCoAppraiserIds);

                    Set<String> toAddToAppraisers = new HashSet<>(newCoAppraiserIds);
                    toAddToAppraisers.removeAll(oldCoAppraiserIds);

                    // טיפול בשמאי ראשי
                    String mainAppraiserId = existingProject.getAppraiserId();
                    if (mainAppraiserId != null) {
                        // אם הוא לא היה ולא יהיה שותף – ודאי שמקבל השמה
                        if (!oldCoAppraiserIds.contains(mainAppraiserId) && !newCoAppraiserIds.contains(mainAppraiserId)) {
                            toAddToAppraisers.add(mainAppraiserId);
                        }
                        // אם היה שותף והוסר מהרשימה, אבל הוא עדיין השמאי הראשי — אל תסיר ממנו את ההשמה
                        else if (oldCoAppraiserIds.contains(mainAppraiserId) && !newCoAppraiserIds.contains(mainAppraiserId)) {
                            toRemoveFromAppraisers.remove(mainAppraiserId);
                        }
                    }

                    // עדכון הפרויקט עצמו
                    Map<String, Object> projectUpdates = new java.util.HashMap<>();
                    projectUpdates.put(FirestoreConstants.FIELD_CO_APPRAISER_IDS, newCoAppraiserIds);
                    projectUpdates.put(FirestoreConstants.FIELD_LAST_UPDATE_DATE,
                            com.google.firebase.firestore.FieldValue.serverTimestamp());

                    // בניית משימות עדכון למסמכי appraisers
                    List<Task<Void>> appraiserUpdateTasks = new ArrayList<>();

                    for (String appraiserId : toRemoveFromAppraisers) {
                        if (mainAppraiserId != null && mainAppraiserId.equals(appraiserId)) continue;
                        DocumentReference appraiserRef =
                                db.collection(FirestoreConstants.COLLECTION_APPRAISERS).document(appraiserId);
                        appraiserUpdateTasks.add(appraiserRef.update(
                                FirestoreConstants.FIELD_APPRAISER_ASSIGNED_PROJECTS,
                                com.google.firebase.firestore.FieldValue.arrayRemove(projectId)
                        ));
                    }

                    for (String appraiserId : toAddToAppraisers) {
                        DocumentReference appraiserRef =
                                db.collection(FirestoreConstants.COLLECTION_APPRAISERS).document(appraiserId);
                        appraiserUpdateTasks.add(appraiserRef.update(
                                FirestoreConstants.FIELD_APPRAISER_ASSIGNED_PROJECTS,
                                com.google.firebase.firestore.FieldValue.arrayUnion(projectId)
                        ));
                    }

                    // קודם עדכוני appraisers, ואז עדכון מסמך הפרויקט
                    Tasks.whenAll(appraiserUpdateTasks)
                            .continueWithTask(task -> {
                                if (!task.isSuccessful()) throw task.getException();
                                return db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                                        .document(projectId)
                                        .update(projectUpdates);
                            })
                            .addOnCompleteListener(task -> {
                                if (listener != null) listener.onComplete(task);
                            });

                })
                .addOnFailureListener(e -> completeWithErr(listener,
                        "שגיאה בקבלת פרטי הפרויקט לפני עדכון הקצאות: " + e.getMessage()));
    }

    private void completeWithErr(@Nullable OnCompleteListener<Void> listener, String msg) {
        if (errorOut != null) errorOut.setValue(msg);
        if (listener != null) listener.onComplete(Tasks.forException(new Exception(msg)));
    }
}
