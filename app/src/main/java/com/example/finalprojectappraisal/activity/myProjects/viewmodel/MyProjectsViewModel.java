package com.example.finalprojectappraisal.activity.myProjects.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.finalprojectappraisal.activity.myProjects.filter.ProjectFilter;
import com.example.finalprojectappraisal.activity.myProjects.filter.ProjectFilterEngine;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Appraiser;
import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.utils.TextNormalizer;

import java.util.ArrayList;
import java.util.List;

/** "מוח המסך": מצב, סינון, הרשאות, טעינות ופעולות — עם לוגים. */
public class MyProjectsViewModel extends ViewModel {

    private static final String TAG_VM = "MyProjectsVM";

    // ==== State ====
    private final MutableLiveData<String> userId = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isAdmin = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isViewingAll = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);

    private final MutableLiveData<List<Project>> allProjects = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ProjectFilter> filter = new MutableLiveData<>(new ProjectFilter());
    private final MediatorLiveData<List<Project>> filtered = new MediatorLiveData<>();

    private final MutableLiveData<List<Appraiser>> appraisers = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toastMsg = new MutableLiveData<>(null);

    public MyProjectsViewModel() {
        android.util.Log.d(TAG_VM, "ctor()");
        filtered.addSource(allProjects, list -> recompute());
        filtered.addSource(filter, f -> recompute());
    }

    // ==== Public LiveData ====
    public LiveData<List<Project>> getFiltered() { return filtered; }
    public LiveData<Boolean> getIsAdmin() { return isAdmin; }
    public LiveData<Boolean> getIsViewingAll() { return isViewingAll; }
    public LiveData<Boolean> getIsRefreshing() { return isRefreshing; }
    public LiveData<List<Appraiser>> getAppraisers() { return appraisers; }
    public LiveData<String> getToastMsg() { return toastMsg; }

    // ==== Filter ops ====
    public void setFilter(ProjectFilter f) {
        filter.setValue(f != null ? f : new ProjectFilter());
        android.util.Log.d(TAG_VM, "setFilter: " + new com.google.gson.Gson().toJson(getCurrentFilter()));
    }

    public ProjectFilter getCurrentFilter() {
        return filter.getValue() != null ? filter.getValue() : new ProjectFilter();
    }

    public void setTextQuery(@Nullable String q) {
        ProjectFilter f = getCurrentFilter();
        f.setTextQuery(TextNormalizer.normalizeOrNull(q));
        filter.setValue(f);
        android.util.Log.d(TAG_VM, "setTextQuery: '" + q + "' → normalized='" + f.getTextQuery() + "'");
    }

    public void setAllProjects(List<Project> list) {
        List<Project> safe = (list != null ? list : new ArrayList<>());
        allProjects.setValue(safe);
        android.util.Log.d(TAG_VM, "setAllProjects: size=" + safe.size()
                + (safe.size() > 0 ? (", firstPid=" + (safe.get(0) != null ? safe.get(0).getProjectId() : "null")) : ""));
    }

    // ==== Screen mode / user ====
    public void setViewingAll(boolean value) {
        boolean cur = isViewingAll.getValue() != null && isViewingAll.getValue();
        if (value == cur) return;
        isViewingAll.setValue(value);
        android.util.Log.d(TAG_VM, "setViewingAll: " + value + " → fetchProjects()");
        fetchProjects();
    }

    public void setUserId(@Nullable String uid) {
        userId.setValue(uid);
        android.util.Log.d(TAG_VM, "setUserId: " + uid + " → checkPermissions()");
        checkPermissions();
    }

    // ==== Loads ====
    public void checkPermissions() {
        String uid = userId.getValue();
        if (uid == null || uid.trim().isEmpty()) {
            isAdmin.setValue(false);
            android.util.Log.w(TAG_VM, "checkPermissions: uid is null/empty → isAdmin=false; fetchProjects()");
            fetchProjects();
            return;
        }
        ProjectRepository.getInstance().getUserPermissions(uid, task -> {
            boolean admin = false;
            if (task.isSuccessful() && task.getResult() != null) {
                Appraiser.AccessPermission p = task.getResult();
                admin = (p == Appraiser.AccessPermission.ADMIN || p == Appraiser.AccessPermission.SUPER_ADMIN);
            }
            isAdmin.setValue(admin);
            android.util.Log.d(TAG_VM, "checkPermissions.onComplete: isAdmin=" + admin + " → fetchProjects()");
            fetchProjects();
        });
    }

    public void fetchProjects() {
        String uid = userId.getValue();
        Boolean viewAll = isViewingAll.getValue() != null && isViewingAll.getValue();
        android.util.Log.d(TAG_VM, "fetchProjects(): uid=" + uid + ", viewingAll=" + viewAll);

        isRefreshing.setValue(true);
        if (uid == null || uid.trim().isEmpty()) {
            ProjectRepository.getInstance().stopListening();
            allProjects.setValue(new ArrayList<>());
            isRefreshing.setValue(false);
            android.util.Log.w(TAG_VM, "fetchProjects(): uid empty → set empty list");
            return;
        }
        if (viewAll) {
            android.util.Log.d(TAG_VM, "fetchProjects(): loadAllProjectsWithListener()");
            ProjectRepository.getInstance().loadAllProjectsWithListener();
        } else {
            android.util.Log.d(TAG_VM, "fetchProjects(): loadActiveProjectsForAppraiser(uid)");
            ProjectRepository.getInstance().loadActiveProjectsForAppraiser(uid);
        }
        isRefreshing.setValue(false);
    }

    public void loadAllAppraisers() {
        android.util.Log.d(TAG_VM, "loadAllAppraisers()");
        ProjectRepository.getInstance().getAllAppraisers(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                appraisers.setValue(task.getResult());
                android.util.Log.d(TAG_VM, "loadAllAppraisers.onComplete: size=" + task.getResult().size());
            } else {
                toast("שגיאה בטעינת השמאים במערכת");
                android.util.Log.e(TAG_VM, "loadAllAppraisers.onComplete: FAILED", task.getException());
            }
        });
    }

    // ==== Actions ====
    public void deleteProject(@NonNull String projectId) {
        android.util.Log.d(TAG_VM, "deleteProject: pid=" + projectId);
        isRefreshing.setValue(true);
        ProjectRepository.getInstance().deleteProject(projectId, task -> {
            boolean ok = task.isSuccessful();
            android.util.Log.d(TAG_VM, "deleteProject.onComplete: ok=" + ok);
            if (ok) {
                toast("הפרויקט נמחק בהצלחה");
                fetchProjects();
            } else {
                toast("מחיקה נכשלה: " + (task.getException() != null ? task.getException().getMessage() : ""));
            }
            isRefreshing.setValue(false);
        });
    }

    public void updateCoAppraisers(@NonNull String projectId, @NonNull List<String> selectedCoAppraiserIds) {
        android.util.Log.d(TAG_VM, "updateCoAppraisers: pid=" + projectId + " selected=" + selectedCoAppraiserIds);
        ProjectRepository.getInstance().updateProjectAndAppraiserAssignments(
                projectId, selectedCoAppraiserIds, task -> {
                    boolean ok = task.isSuccessful();
                    android.util.Log.d(TAG_VM, "updateCoAppraisers.onComplete: ok=" + ok);
                    if (ok) {
                        toast("שמאים שותפים עודכנו בהצלחה ✅");
                        fetchProjects();
                    } else {
                        toast("עדכון שמאים שותפים נכשל: " + (task.getException() != null ? task.getException().getMessage() : "לא ידוע"));
                    }
                });
    }

    private void toast(String msg) { toastMsg.setValue(msg); }

    private void recompute() {
        List<Project> base = allProjects.getValue();
        ProjectFilter f = getCurrentFilter();
        if (base == null) base = new ArrayList<>();
        List<Project> out = ProjectFilterEngine.apply(base, f);
        filtered.setValue(out);

        android.util.Log.d(TAG_VM, "recompute: base=" + base.size()
                + ", filtered=" + (out == null ? 0 : out.size())
                + ", filter=" + new com.google.gson.Gson().toJson(f));
    }
}
