package com.example.finalprojectappraisal.database;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.finalprojectappraisal.model.Appraiser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AppraiserRepository {

    private static AppraiserRepository instance;
    private final FirebaseFirestore db;

    private AppraiserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized AppraiserRepository getInstance() {
        if (instance == null) {
            instance = new AppraiserRepository();
        }
        return instance;
    }

    /**
     * מקבל את פרטי השמאי לפי ה-UID שלו
     */
    public LiveData<Appraiser> getCurrentAppraiser(String uid) {
        MutableLiveData<Appraiser> appraiserLiveData = new MutableLiveData<>();

        if (uid == null || uid.trim().isEmpty()) {
            appraiserLiveData.setValue(null);
            return appraiserLiveData;
        }

        db.collection("appraisers")
                .document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc != null && doc.exists()) {
                            Appraiser appraiser = doc.toObject(Appraiser.class);
                            appraiserLiveData.setValue(appraiser);
                        } else {
                            appraiserLiveData.setValue(null);
                        }
                    } else {
                        appraiserLiveData.setValue(null);
                    }
                });

        return appraiserLiveData;
    }

    /**
     * דוגמה לפונקציה להוספת או עדכון שמאי במסד הנתונים
     */
    public void saveAppraiser(Appraiser appraiser, OnCompleteListener<Void> listener) {
        if (appraiser == null || appraiser.getAppraiserId() == null || appraiser.getAppraiserId().trim().isEmpty()) {
            if (listener != null) {
                listener.onComplete(com.google.android.gms.tasks.Tasks.forException(
                        new IllegalArgumentException("Appraiser or UID is null or empty")));
            }
            return;
        }

        db.collection("appraisers")
                .document(appraiser.getAppraiserId())
                .set(appraiser)
                .addOnCompleteListener(listener);
    }
}
