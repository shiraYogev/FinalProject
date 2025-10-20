package com.example.finalprojectappraisal.database.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.model.Appraiser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AppraiserRepository {

    private static AppraiserRepository instance;
    public static synchronized AppraiserRepository getInstance() {
        if (instance == null) instance = new AppraiserRepository();
        return instance;
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private AppraiserRepository() {}

    public MutableLiveData<String> getErrorMessage() { return errorMessage; }

    // ===== Permissions =====
    public void getUserPermissions(@NonNull String userId,
                                   @Nullable OnCompleteListener<Appraiser.AccessPermission> listener) {
        if (userId.trim().isEmpty()) {
            if (listener != null) listener.onComplete(Tasks.forResult(Appraiser.AccessPermission.VIEWER));
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_APPRAISERS)
                .document(userId)
                .get()
                .addOnSuccessListener((DocumentSnapshot snap) -> {
                    if (!snap.exists()) {
                        if (listener != null) listener.onComplete(Tasks.forResult(Appraiser.AccessPermission.VIEWER));
                        return;
                    }
                    try {
                        Appraiser appraiser = snap.toObject(Appraiser.class);
                        Appraiser.AccessPermission p =
                                (appraiser != null && appraiser.getAccessPermissions() != null)
                                        ? appraiser.getAccessPermissions()
                                        : Appraiser.AccessPermission.USER;
                        if (listener != null) listener.onComplete(Tasks.forResult(p));
                    } catch (Exception e) {
                        errorMessage.setValue("Failed to parse appraiser permissions: " + e.getMessage());
                        if (listener != null) listener.onComplete(Tasks.forResult(Appraiser.AccessPermission.USER));
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Error fetching user permissions: " + e.getMessage());
                    if (listener != null) listener.onComplete(Tasks.forResult(Appraiser.AccessPermission.USER));
                });
    }

    // ===== List all appraisers =====
    public void getAllAppraisers(@Nullable OnCompleteListener<List<Appraiser>> listener) {
        db.collection(FirestoreConstants.COLLECTION_APPRAISERS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Appraiser> list = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            try {
                                Appraiser a = doc.toObject(Appraiser.class);
                                if (a != null) {
                                    a.setAppraiserId(doc.getId());
                                    list.add(a);
                                }
                            } catch (Exception e) {
                                errorMessage.setValue("Error parsing appraiser " + doc.getId() + ": " + e.getMessage());
                            }
                        }
                        if (listener != null) listener.onComplete(Tasks.forResult(list));
                    } else {
                        Exception ex = (task.getException() != null) ? task.getException() : new Exception("Failed to load appraisers");
                        errorMessage.setValue("Failed to get all appraisers: " + ex.getMessage());
                        if (listener != null) listener.onComplete(Tasks.forException(ex));
                    }
                });
    }
}
