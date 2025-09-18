package com.example.finalprojectappraisal.activity.myProjects.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.finalprojectappraisal.activity.myProjects.filter.ProjectFilter;
import com.example.finalprojectappraisal.activity.myProjects.filter.ProjectFilterEngine;
import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.utils.TextNormalizer;

import java.util.ArrayList;
import java.util.List;

public class MyProjectsViewModel extends ViewModel {

    private final MutableLiveData<List<Project>> allProjects = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ProjectFilter> filter = new MutableLiveData<>(new ProjectFilter());
    private final MediatorLiveData<List<Project>> filtered = new MediatorLiveData<>();

    public MyProjectsViewModel() {
        filtered.addSource(allProjects, list -> recompute());
        filtered.addSource(filter, f -> recompute());
    }

    public LiveData<List<Project>> getFiltered() { return filtered; }

    public void setAllProjects(List<Project> list) {
        allProjects.setValue(list != null ? list : new ArrayList<>());
    }

    public void setFilter(ProjectFilter f) {
        filter.setValue(f != null ? f : new ProjectFilter());
    }

    public ProjectFilter getCurrentFilter() {
        ProjectFilter f = filter.getValue();
        return (f != null) ? f : new ProjectFilter();
    }

    /** חיפוש חופשי — אצלנו עובד רק על fullAddress במנוע */
    public void setTextQuery(String q) {
        ProjectFilter f = getCurrentFilter();
        f.setTextQuery(TextNormalizer.normalizeOrNull(q));
        filter.setValue(f);
    }

    private void recompute() {
        List<Project> base = allProjects.getValue();
        ProjectFilter f = getCurrentFilter();
        if (base == null) base = new ArrayList<>();
        filtered.setValue(ProjectFilterEngine.apply(base, f));
    }
}
