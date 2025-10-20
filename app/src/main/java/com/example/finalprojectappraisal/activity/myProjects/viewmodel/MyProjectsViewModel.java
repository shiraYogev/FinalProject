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
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

/** "מוח המסך": מצב, סינון, הרשאות, טעינות ופעולות. */
public class MyProjectsViewModel extends ViewModel {

    // ==== State ====
    private final MutableLiveData<String> userId = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isAdmin = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isViewingAll = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);

    private final MutableLiveData<List<Project>> allProjects = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ProjectFilter> filter = new MutableLiveData<>(new ProjectFilter());
    private final MediatorLiveData<List<Project>> filtered = new MediatorLiveData<>();

    private final MutableLiveData<List<Appraiser>> appraisers = new MutableLiveData<>(new ArrayList<>());

    // one-shot messages (אפשר להחליף ב-SingleLiveEvent אם יש לך)
    private final MutableLiveData<String> toastMsg = new MutableLiveData<>(null);

    public MyProjectsViewModel() {
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
    public void setFilter(ProjectFilter f) { filter.setValue(f != null ? f : new ProjectFilter()); }
    public ProjectFilter getCurrentFilter() { return filter.getValue() != null ? filter.getValue() : new ProjectFilter(); }
    public void setTextQuery(@Nullable String q) {
        ProjectFilter f = getCurrentFilter();
        f.setTextQuery(TextNormalizer.normalizeOrNull(q));
        filter.setValue(f);
    }

    public void setAllProjects(List<Project> list) {
        allProjects.setValue(list != null ? list : new ArrayList<>());
    }

    // ==== Screen mode / user ====
    public void setViewingAll(boolean value) {
        if (value == (isViewingAll.getValue() != null && isViewingAll.getValue())) return;
        isViewingAll.setValue(value);
        fetchProjects(); // שינוי מצב → טען מחדש
    }

    public void setUserId(@Nullable String uid) {
        userId.setValue(uid);
        checkPermissions(); // מעדכן isAdmin ואז קורא fetchProjects()
    }

    // ==== Loads ====
    public void checkPermissions() {
        String uid = userId.getValue();
        if (uid == null || uid.trim().isEmpty()) {
            isAdmin.setValue(false);
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
            fetchProjects();
        });
    }

    public void fetchProjects() {
        String uid = userId.getValue();
        isRefreshing.setValue(true);
        if (uid == null || uid.trim().isEmpty()) {
            // אין משתמש – ננקה רשימה
            ProjectRepository.getInstance().stopListening();
            allProjects.setValue(new ArrayList<>());
            isRefreshing.setValue(false);
            return;
        }
        Boolean viewAll = isViewingAll.getValue() != null && isViewingAll.getValue();
        if (viewAll) {
            ProjectRepository.getInstance().loadAllProjectsWithListener();
        } else {
            ProjectRepository.getInstance().loadActiveProjectsForAppraiser(uid);
        }
        // ProjectRepository יעדכן את ה-LiveData שלו; את שלנו נעדכן דרך ה-Activity observer שקיים
        isRefreshing.setValue(false);
    }

    public void loadAllAppraisers() {
        ProjectRepository.getInstance().getAllAppraisers(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                appraisers.setValue(task.getResult());
            } else {
                toast("שגיאה בטעינת השמאים במערכת");
            }
        });
    }

    // ==== Actions ====
    public void deleteProject(@NonNull String projectId) {
        isRefreshing.setValue(true);
        ProjectRepository.getInstance().deleteProject(projectId, task -> {
            if (task.isSuccessful()) {
                toast("הפרויקט נמחק בהצלחה");
                fetchProjects();
            } else {
                toast("מחיקה נכשלה: " + (task.getException() != null ? task.getException().getMessage() : ""));
            }
            isRefreshing.setValue(false);
        });
    }

    public void updateCoAppraisers(@NonNull String projectId, @NonNull List<String> selectedCoAppraiserIds) {
        ProjectRepository.getInstance().updateProjectAndAppraiserAssignments(
                projectId, selectedCoAppraiserIds, task -> {
                    if (task.isSuccessful()) {
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
        filtered.setValue(ProjectFilterEngine.apply(base, f));
    }
}
