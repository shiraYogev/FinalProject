// file: app/src/main/java/com/example/finalprojectappraisal/activity/AllProjects/viewmodel/AllProjectsViewModel.java
package com.example.finalprojectappraisal.activity.AllProjects.viewmodel;

import android.util.Log;

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

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the read-only projects list.
 *
 * Responsibilities:
 * - Mirrors the repository list into a MediatorLiveData (projects) so we can clear/override locally.
 * - Applies client-side filters via ProjectFilterEngine and exposes a filtered list.
 * - Decides what to load according to user permission (admin/all vs. user/active).
 *
 * Notes:
 * - Admins: loads all projects with a live listener.
 * - Regular users: loads only their active projects.
 * - Exposes loading, error, admin flag, raw projects, and filtered projects LiveData.
 */
public class AllProjectsViewModel extends ViewModel {

    private static final String TAG = "AllProjectsVM";

    private final ProjectRepository repo = ProjectRepository.getInstance();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isAdmin   = new MutableLiveData<>(false);
    private final MutableLiveData<String>  error     = new MutableLiveData<>(null);

    // Raw source as streamed from repository
    private final MediatorLiveData<List<Project>> projectsSource = new MediatorLiveData<>();

    // Current filter
    private final MutableLiveData<ProjectFilter> filter = new MutableLiveData<>(new ProjectFilter());

    // Filtered output
    private final MediatorLiveData<List<Project>> filtered = new MediatorLiveData<>();

    public AllProjectsViewModel() {
        Log.d(TAG, "init: wiring sources");

        // Bridge repo list → local source (+ log size on update)
        projectsSource.addSource(repo.getAllProjects(), list -> {
            int size = (list == null) ? 0 : list.size();
            Log.d(TAG, "sourceUpdate: repo list size=" + size);
            projectsSource.setValue(list);
        });

        // Any change in source/filter → recompute
        filtered.addSource(projectsSource, it -> recompute());
        filtered.addSource(filter, it -> recompute());
    }

    // ===== Exposed LiveData =====
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsAdmin()   { return isAdmin; }
    public LiveData<String>  getError()     { return error; }

    /** The Activity should observe this (already filtered). */
    public LiveData<List<Project>> getProjects() { return filtered; }

    public ProjectFilter getCurrentFilter() {
        ProjectFilter f = filter.getValue();
        return f != null ? f : new ProjectFilter();
    }

    public void setFilter(ProjectFilter f) {
        if (f == null) f = new ProjectFilter();
        Log.d(TAG, "setFilter: " + summarizeFilter(f));
        filter.setValue(f);
    }

    /** Convenience: update only the search text. */
    public void setTextQuery(@Nullable String q) {
        ProjectFilter f = getCurrentFilter();
        f.setTextQuery(q);
        Log.d(TAG, "setTextQuery: \"" + q + "\" → " + summarizeFilter(f));
        filter.setValue(f);
    }

    /** Entry point: decide what to load for this userId. */
    public void loadForUser(@Nullable String userId) {
        Log.d(TAG, "loadForUser: uid=" + userId);
        isLoading.setValue(true);

        if (userId == null || userId.trim().isEmpty()) {
            Log.d(TAG, "loadForUser: no user → stopListening + clear");
            repo.stopListening();
            projectsSource.setValue(new ArrayList<>()); // clear locally
            isAdmin.setValue(false);
            isLoading.setValue(false);
            return;
        }

        repo.getUserPermissions(userId, task -> {
            boolean admin = false;
            if (task.isSuccessful() && task.getResult() != null) {
                Appraiser.AccessPermission p = task.getResult();
                admin = (p == Appraiser.AccessPermission.ADMIN || p == Appraiser.AccessPermission.SUPER_ADMIN);
            }
            isAdmin.setValue(admin);
            Log.d(TAG, "permissions: isAdmin=" + admin);

            if (admin) {
                Log.d(TAG, "loading mode: loadAllProjectsWithListener()");
                repo.loadAllProjectsWithListener();
            } else {
                Log.d(TAG, "loading mode: loadActiveProjectsForAppraiser(uid)");
                repo.loadActiveProjectsForAppraiser(userId);
            }

            error.setValue(null);
            isLoading.setValue(false);
        });
    }

    /** Optional manual refresh (Pull-to-refresh). */
    public void reload(@Nullable String userId) {
        Boolean admin = isAdmin.getValue();
        Log.d(TAG, "reload: isAdmin=" + admin + " uid=" + userId);
        if (admin != null && admin) {
            Log.d(TAG, "reload: loadAllProjectsWithListener()");
            repo.loadAllProjectsWithListener();
        } else if (userId != null && !userId.trim().isEmpty()) {
            Log.d(TAG, "reload: loadActiveProjectsForAppraiser(uid)");
            repo.loadActiveProjectsForAppraiser(userId);
        } else {
            Log.d(TAG, "reload: no user → clear locally");
            projectsSource.setValue(new ArrayList<>());
        }
    }

    /** Recalculate filtered list based on current source + filter. */
    private void recompute() {
        List<Project> base = projectsSource.getValue();
        if (base == null) base = new ArrayList<>();
        ProjectFilter f = getCurrentFilter();

        int baseSize = base.size();
        List<Project> out = ProjectFilterEngine.apply(base, f);
        int outSize = (out == null) ? 0 : out.size();

        Log.d(TAG, "recompute: base=" + baseSize
                + " → out=" + outSize
                + " | filter=" + summarizeFilter(f));

        filtered.setValue(out);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "onCleared: stopListening()");
        repo.stopListening();
    }

    // ------- Debug helpers -------

    private String summarizeFilter(ProjectFilter f) {
        StringBuilder sb = new StringBuilder();
        sb.append("{q=").append(f.getTextQuery())
                .append(", statuses=").append(f.getStatuses())
                .append(", dateField=").append(f.getDateField())
                .append(", from=").append(f.getDateFromEpochMillis())
                .append(", to=").append(f.getDateToEpochMillis())
                .append(", sortBy=").append(f.getSortBy())
                .append(", sortDir=").append(f.getSortDir())
                .append('}');
        return sb.toString();
    }
}
