package com.example.finalprojectappraisal.database.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.model.Client;
import com.example.finalprojectappraisal.model.Project;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;

import java.util.*;

class ProjectDataService {

    private static final String TAG = "ProjectDataService";

    private final FirebaseFirestore db;
    private final MutableLiveData<String> errorOut;

    ProjectDataService(FirebaseFirestore db, MutableLiveData<String> errorOut) {
        this.db = db; this.errorOut = errorOut;
    }

    // ===== Basic CRUD/Queries =====
    void getProject(@NonNull String projectId, @Nullable OnCompleteListener<DocumentSnapshot> l) {
        if (projectId.trim().isEmpty()) {
            if (l != null) l.onComplete(Tasks.forException(new Exception("Empty projectId")));
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId).get().addOnCompleteListener(l);
    }

    void deleteProject(@NonNull String projectId, @Nullable OnCompleteListener<Void> l) {
        if (projectId.trim().isEmpty()) {
            if (l != null) l.onComplete(Tasks.forException(new Exception("Empty projectId")));
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId).delete().addOnCompleteListener(l);
    }

    /** מחיקה עמוקה: תמונות ב-Storage + מסמכי images/* + הסרת שיוכים משמאים + מחיקת מסמך הפרויקט */
    void deleteProjectDeepAndCleanupAssignments(@NonNull String projectId,
                                                @Nullable OnCompleteListener<Void> listener) {
        final String TAG = "ProjectDataService";
        FirebaseStorage storage = FirebaseStorage.getInstance();

        Log.d(TAG, "deleteProjectDeepAndCleanupAssignments: start pid=" + projectId);

        // 1) אסוף נתיבי קבצים מתת-האוסף images/*
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(projectId)
                .collection(FirestoreConstants.SUBCOLLECTION_IMAGES)
                .get()
                .addOnCompleteListener(taskImgs -> {
                    List<Task<?>> storageDeletes = new ArrayList<>();

                    if (taskImgs.isSuccessful() && taskImgs.getResult() != null) {
                        for (DocumentSnapshot doc : taskImgs.getResult()) {
                            Map<String, Object> data = doc.getData();
                            if (data == null) continue;
                            for (Object v : data.values()) {
                                if (v instanceof String) {
                                    String pathOrUrl = (String) v;
                                    // מוחקים רק רפרנסים של Storage (לא https)
                                    if (!pathOrUrl.startsWith("http")) {
                                        try {
                                            storageDeletes.add(storage.getReference(pathOrUrl).delete());
                                        } catch (Exception ignore) {
                                            Log.w(TAG, "storage ref invalid: " + pathOrUrl);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "images subcollection list failed (continue anyway)", taskImgs.getException());
                    }

                    // 2) אחרי נסיון מחיקת קבצי Storage → מחק את מסמכי images/*
                    Tasks.whenAllComplete(storageDeletes).addOnCompleteListener(t1 -> {
                        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                                .document(projectId)
                                .collection(FirestoreConstants.SUBCOLLECTION_IMAGES)
                                .get()
                                .addOnCompleteListener(taskGetSub -> {
                                    List<Task<?>> docDeletes = new ArrayList<>();
                                    if (taskGetSub.isSuccessful() && taskGetSub.getResult() != null) {
                                        for (DocumentSnapshot d : taskGetSub.getResult()) {
                                            docDeletes.add(d.getReference().delete());
                                        }
                                    } else {
                                        Log.w(TAG, "fetch images docs for delete failed (continue)", taskGetSub.getException());
                                    }

                                    Tasks.whenAllComplete(docDeletes).addOnCompleteListener(t2 -> {
                                        // 3) הסרת projectId מ-activeProjects של כל השמאים שמכילים אותו
                                        db.collection(FirestoreConstants.COLLECTION_APPRAISERS)
                                                .whereArrayContains(FirestoreConstants.FIELD_APPRAISER_ASSIGNED_PROJECTS, projectId)
                                                .get()
                                                .addOnCompleteListener(taskApps -> {
                                                    List<Task<?>> appUpdates = new ArrayList<>();
                                                    if (taskApps.isSuccessful() && taskApps.getResult() != null) {
                                                        for (DocumentSnapshot appDoc : taskApps.getResult()) {
                                                            appUpdates.add(
                                                                    appDoc.getReference().update(
                                                                            FirestoreConstants.FIELD_APPRAISER_ASSIGNED_PROJECTS,
                                                                            FieldValue.arrayRemove(projectId)
                                                                    )
                                                            );
                                                        }
                                                    } else {
                                                        Log.w(TAG, "appraisers cleanup query failed (continue)", taskApps.getException());
                                                    }

                                                    Tasks.whenAllComplete(appUpdates).addOnCompleteListener(t3 -> {
                                                        // 4) מחיקת מסמך הפרויקט עצמו
                                                        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                                                                .document(projectId)
                                                                .delete()
                                                                .addOnCompleteListener(delTask -> {
                                                                    Log.d(TAG, "project doc delete done ok=" + delTask.isSuccessful());
                                                                    if (listener != null) {
                                                                        listener.onComplete((Task<Void>) delTask);
                                                                    }
                                                                });
                                                    });
                                                });
                                    });
                                });
                    });
                });
    }

    void projectExists(@NonNull String projectId, @Nullable OnCompleteListener<Boolean> l) {
        if (projectId.trim().isEmpty()) {
            if (l != null) l.onComplete(Tasks.forResult(false));
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId)
                .get().addOnCompleteListener(t -> {
                    boolean exists = t.isSuccessful() && t.getResult() != null && t.getResult().exists();
                    if (l != null) l.onComplete(Tasks.forResult(exists));
                });
    }

    void loadProjectsForAppraiser(@NonNull String userId, MutableLiveData<List<Project>> target) {
        Task<QuerySnapshot> q1 = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .whereEqualTo(FirestoreConstants.FIELD_APPRAISER_ID, userId).get();
        Task<QuerySnapshot> q2 = db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .whereArrayContains(FirestoreConstants.FIELD_CO_APPRAISER_IDS, userId).get();

        Tasks.whenAllSuccess(q1, q2).addOnSuccessListener(results -> {
            Set<Project> set = new HashSet<>();
            for (Object r : results) {
                for (DocumentSnapshot d : ((QuerySnapshot) r).getDocuments()) {
                    Project p = safeProjectFrom(d);
                    if (p != null) set.add(p);
                }
            }
            List<Project> list = new ArrayList<>(set);
            list.sort(Comparator.comparingLong(Project::getLastUpdateDateMillis).reversed());
            target.setValue(list);
        }).addOnFailureListener(e -> {
            errorOut.setValue("שגיאה בטעינת פרויקטים עבור משתמש: " + e.getMessage());
            target.setValue(new ArrayList<>());
        });
    }

    void loadActiveProjectsForAppraiser(@NonNull String userId, MutableLiveData<List<Project>> target) {
        // כרגע כמו למעלה (אפשר להוסיף סינון סטטוס אם תרצי)
        loadProjectsForAppraiser(userId, target);
    }

    void loadProjectsByStatus(@NonNull String status, @Nullable OnCompleteListener<List<Project>> l) {
        if (status.trim().isEmpty()) {
            if (l != null) l.onComplete(Tasks.forException(new Exception("Empty status")));
            return;
        }
        db.collection(FirestoreConstants.COLLECTION_PROJECTS)
                .whereEqualTo(FirestoreConstants.FIELD_PROJECT_STATUS, status)
                .get()
                .addOnSuccessListener(qs -> {
                    List<Project> out = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        try {
                            Project p = d.toObject(Project.class);
                            if (p != null) out.add(p);
                        } catch (Exception ignored) {}
                    }
                    if (l != null) l.onComplete(Tasks.forResult(out));
                })
                .addOnFailureListener(e -> { if (l != null) l.onComplete(Tasks.forException(e)); });
    }

    // ===== Summary / Projection =====
    void getProjectFieldsForSummary(@NonNull String projectId, @NonNull OnCompleteListener<Map<String,Object>> l) {
        db.collection(FirestoreConstants.COLLECTION_PROJECTS).document(projectId).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        l.onComplete(Tasks.forResult(Collections.emptyMap())); return;
                    }
                    DocumentSnapshot doc = task.getResult();
                    Map<String, Object> map = new HashMap<>();
                    putIfExists(doc, map, "number_of_rooms");
                    putIfExists(doc, map, "physical_condition");
                    putIfExists(doc, map, "has_elevator");
                    putIfExists(doc, map, "has_parking");
                    putIfExists(doc, map, "has_storage");
                    putIfExists(doc, map, "apartment_flooring");
                    putIfExists(doc, map, "apartment_windows");
                    putIfExists(doc, map, "apartment_kitchen");
                    putIfExists(doc, map, "apartment_bathroom_fixtures");
                    putIfExists(doc, map, "registered_apartment_area");
                    putIfExists(doc, map, "gross_apartment_area");
                    putIfExists(doc, map, "apartment_story");
                    putIfExists(doc, map, "apartment_number(municipal_form)");
                    putIfExists(doc, map, "property_location");
                    putIfExists(doc, map, "building_type");
                    putIfExists(doc, map, "number_of_floors");
                    putIfExists(doc, map, "apartment_directions");
                    l.onComplete(Tasks.forResult(map));
                });
    }

    // ===== Mapper: safe Document → Project (הקוד שהיה אצלך) =====
    Project safeProjectFrom(DocumentSnapshot d) {
        Project p;
        try {
            p = d.toObject(Project.class);
            if (p == null) { p = new Project(); Log.w(TAG, "toObject returned null for " + d.getId()); }
        } catch (Exception e) {
            Log.w(TAG, "toObject failed for " + d.getId() + ": " + e.getMessage());
            p = new Project();
            try { p.setProjectStatus((String) d.get(FirestoreConstants.FIELD_PROJECT_STATUS)); } catch (Exception ignore) {}
            try { p.setFullAddress((String) d.get(FirestoreConstants.FIELD_FULL_ADDRESS)); } catch (Exception ignore) {}
            try { p.setNote((String) d.get("note")); } catch (Exception ignore) {}
            try {
                Object clientObj = d.get("client");
                if (clientObj instanceof Map) {
                    Map<String, Object> cm = (Map<String, Object>) clientObj;
                    Client c = new Client();
                    try { c.setClientId((String) cm.get("id")); } catch (Exception ignore) {}
                    try { c.setFullName((String) cm.get("fullName")); } catch (Exception ignore) {}
                    try { c.setEmail((String) cm.get("email")); } catch (Exception ignore) {}
                    try { c.setPhoneNumber((String) cm.get("phoneNumber")); } catch (Exception ignore) {}
                    p.setClient(c);
                }
            } catch (Exception ignore) {}
        }

        try { if (p.getProjectId() == null || p.getProjectId().trim().isEmpty()) p.setProjectId(d.getId()); } catch (Exception ignore) {}

        try {
            Object ts = d.get("lastUpdateDate");
            long millis = 0L;
            if (ts instanceof Timestamp) millis = ((Timestamp) ts).toDate().getTime();
            else if (ts instanceof Long) millis = (Long) ts;
            else if (ts instanceof Double) millis = ((Double) ts).longValue();
            if (p.getLastUpdateDate() == null && millis > 0L) p.setLastUpdateDate(new Date(millis));
        } catch (Exception e) { Log.w(TAG, "lastUpdateDate parse failed for " + d.getId() + ": " + e.getMessage()); }

        try {
            Object co = d.get(FirestoreConstants.FIELD_CO_APPRAISER_IDS);
            if (co instanceof List) p.setCoAppraiserIds((List<String>) co);
        } catch (Exception e) { Log.w(TAG, "co_appraiser_ids parse failed: " + e.getMessage()); }

        try {
            Object a = d.get(FirestoreConstants.FIELD_APPRAISER_ID);
            if (a instanceof String) p.setAppraiserId((String) a);
        } catch (Exception e) { Log.w(TAG, "appraiser_id parse failed: " + e.getMessage()); }

        return p;
    }

    private static void putIfExists(DocumentSnapshot doc, Map<String,Object> out, String key) {
        if (doc.contains(key)) {
            Object v = doc.get(key);
            if (v != null) out.put(key, v);
        }
    }
}
