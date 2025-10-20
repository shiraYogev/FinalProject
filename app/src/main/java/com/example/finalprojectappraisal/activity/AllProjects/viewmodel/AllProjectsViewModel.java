/**
 * Summary:
 * ViewModel for the read-only projects list. It mirrors the repository list
 * into a MediatorLiveData so we can also clear/override it locally (without
 * calling setValue on the repository's LiveData which is not allowed).
 *
 * Notes:
 * - Admins: loads all projects with a live listener.
 * - Regular users: loads only their active projects.
 * - Exposes loading, error, admin flag, and a projects LiveData sourced from repo.
 */

package com.example.finalprojectappraisal.activity.AllProjects.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Appraiser;
import com.example.finalprojectappraisal.model.Project;

import java.util.ArrayList;
import java.util.List;

public class AllProjectsViewModel extends ViewModel {

    private final ProjectRepository repo = ProjectRepository.getInstance();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isAdmin   = new MutableLiveData<>(false);
    private final MutableLiveData<String>  error     = new MutableLiveData<>(null);

    // Mirror of repository projects so we can clear/override locally
    private final MediatorLiveData<List<Project>> projects = new MediatorLiveData<>();

    public AllProjectsViewModel() {
        // Bridge repo list into our own MediatorLiveData
        projects.addSource(repo.getAllProjects(), projects::setValue);
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsAdmin()   { return isAdmin; }
    public LiveData<String>  getError()     { return error; }
    public LiveData<List<Project>> getProjects() { return projects; }

    /** Entry point: decide what to load for this userId. */
    public void loadForUser(@Nullable String userId) {
        isLoading.setValue(true);

        if (userId == null || userId.trim().isEmpty()) {
            // No user → stop listening and show empty state locally
            repo.stopListening();
            projects.setValue(new ArrayList<>()); // clear locally (OK!)
            isAdmin.setValue(false);
            isLoading.setValue(false);
            return;
        }

        // Check permission and then load from repository
        repo.getUserPermissions(userId, task -> {
            boolean admin = false;
            if (task.isSuccessful() && task.getResult() != null) {
                Appraiser.AccessPermission p = task.getResult();
                admin = (p == Appraiser.AccessPermission.ADMIN || p == Appraiser.AccessPermission.SUPER_ADMIN);
            }
            isAdmin.setValue(admin);

            if (admin) {
                repo.loadAllProjectsWithListener();
            } else {
                repo.loadActiveProjectsForAppraiser(userId);
            }

            error.setValue(null);
            isLoading.setValue(false);
        });
    }

    /** Optional manual refresh (e.g., pull-to-refresh). */
    public void reload(@Nullable String userId) {
        Boolean admin = isAdmin.getValue();
        if (admin != null && admin) {
            repo.loadAllProjectsWithListener();
        } else if (userId != null && !userId.trim().isEmpty()) {
            repo.loadActiveProjectsForAppraiser(userId);
        } else {
            // No user: show empty locally
            projects.setValue(new ArrayList<>());
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repo.stopListening();
    }
}
