package com.example.finalprojectappraisal.database.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.model.Project;
import com.google.firebase.firestore.*;

import java.util.*;

class ProjectLiveListeners {
    private final FirebaseFirestore db;
    private final ProjectDataService data;
    private final MutableLiveData<List<Project>> out;
    private final MutableLiveData<String> errorOut;
    private ListenerRegistration reg;

    ProjectLiveListeners(FirebaseFirestore db, ProjectDataService data,
                         MutableLiveData<List<Project>> out, MutableLiveData<String> errorOut) {
        this.db = db; this.data = data; this.out = out; this.errorOut = errorOut;
    }

    void stop() { if (reg != null) { reg.remove(); reg = null; } }

    void listenAll() {
        stop();
        Query q = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .orderBy(FirestoreConstants.FIELD_LAST_UPDATE_DATE, Query.Direction.DESCENDING);

        reg = q.addSnapshotListener((snap, err) -> {
            if (err != null) {
                if (err instanceof FirebaseFirestoreException &&
                        ((FirebaseFirestoreException) err).getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    listenAllNoOrder();
                    return;
                }
                errorOut.setValue("שגיאה בטעינת כל הפרויקטים: " + err.getMessage());
                out.setValue(new ArrayList<>());
                return;
            }
            List<Project> list = new ArrayList<>();
            if (snap != null) for (DocumentSnapshot d : snap.getDocuments()) {
                Project p = data.safeProjectFrom(d); if (p != null) list.add(p);
            }
            out.setValue(list);
        });
    }

    private void listenAllNoOrder() {
        stop();
        Query q = db.collection(FirestoreConstants.COLLECTION_PROJECTS);
        reg = q.addSnapshotListener((snap, err) -> {
            if (err != null) {
                errorOut.setValue("שגיאה בטעינת כל הפרויקטים: " + err.getMessage());
                out.setValue(new ArrayList<>()); return;
            }
            List<Project> list = new ArrayList<>();
            if (snap != null) for (DocumentSnapshot d : snap.getDocuments()) {
                Project p = data.safeProjectFrom(d); if (p != null) list.add(p);
            }
            list.sort(Comparator.comparingLong(Project::getLastUpdateDateMillis).reversed());
            out.setValue(list);
        });
    }
}
