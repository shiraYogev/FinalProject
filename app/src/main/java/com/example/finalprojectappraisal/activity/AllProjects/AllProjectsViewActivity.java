// file: app/src/main/java/com/example/finalprojectappraisal/activity/AllProjects/AllProjectsViewActivity.java
package com.example.finalprojectappraisal.activity.AllProjects;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.AllProjects.viewmodel.AllProjectsViewModel;
import com.example.finalprojectappraisal.activity.myProjects.filter.FiltersBottomSheetDialogFragment;
import com.example.finalprojectappraisal.activity.myProjects.filter.ProjectFilter;
import com.example.finalprojectappraisal.activity.myProjects.filter.ui.FilterChipsController;
import com.example.finalprojectappraisal.adapter.ProjectsReadOnlyAdapter;
import com.example.finalprojectappraisal.database.auth.AuthRepository;
import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.utils.FilterPrefs;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays a read-only list of projects (all projects).
 * Delegates data loading and filtering to AllProjectsViewModel.
 * No direct DB logic here.
 *
 * Notes:
 * - Observes VM filtered LiveData (Mediator over repository + filter).
 * - Reuses the same filter toolkit as "My Projects" (BottomSheet + Chips + Engine).
 */
public class AllProjectsViewActivity extends AppCompatActivity
        implements ProjectsReadOnlyAdapter.ViewClickListener,
        FiltersBottomSheetDialogFragment.OnFiltersAppliedListener {

    private RecyclerView rv;
    private ProgressBar progress;
    private TextView empty;

    private EditText searchBar;
    private ChipGroup chipsActiveFilters;
    private View btnFilters;

    private ProjectsReadOnlyAdapter adapter;
    private AllProjectsViewModel vm;

    private String currentUserId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_projects_view);

        // Status bar overlay height handling
        View overlay = findViewById(R.id.status_bar_overlay);
        if (overlay != null) {
            ViewCompat.setOnApplyWindowInsetsListener(overlay, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                overlay.getLayoutParams().height = top;
                overlay.requestLayout();
                return insets;
            });
        }

        setTitle("כל הפרויקטים (צפייה)");

        // Bind views
        rv = findViewById(R.id.rvProjects);
        progress = findViewById(R.id.progress);
        empty = findViewById(R.id.txtEmpty);

        // Filter UI
        searchBar = findViewById(R.id.searchBar);
        chipsActiveFilters = findViewById(R.id.chipsActiveFilters);
        btnFilters = findViewById(R.id.btnFilters);

        // Adapter
        adapter = new ProjectsReadOnlyAdapter(new ArrayList<>(), this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // ViewModel
        vm = new ViewModelProvider(this).get(AllProjectsViewModel.class);

        // Load last used filter into VM
        ProjectFilter saved = FilterPrefs.load(this);
        vm.setFilter(saved);

        // Observe loading
        vm.getIsLoading().observe(this, isLoading ->
                progress.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE)
        );

        // Observe (optional) error
        vm.getError().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                empty.setText("שגיאה בטעינה: " + msg);
                empty.setVisibility(View.VISIBLE);
            }
        });

        // Observe filtered projects and render + chips
        vm.getProjects().observe(this, items -> {
            render(items);
            FilterChipsController.render(
                    this, chipsActiveFilters, searchBar, vm.getCurrentFilter(),
                    () -> vm.setFilter(vm.getCurrentFilter()) // trigger recompute
            );
        });

        // Search bar → update filter in VM + persist
        if (searchBar != null) {
            searchBar.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    ProjectFilter f = vm.getCurrentFilter();
                    f.setTextQuery(s == null ? null : s.toString());
                    vm.setFilter(f);
                    FilterPrefs.save(AllProjectsViewActivity.this, vm.getCurrentFilter());
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        // Open filters bottom sheet
        if (btnFilters != null) {
            btnFilters.setOnClickListener(v ->
                    new FiltersBottomSheetDialogFragment(
                            vm.getCurrentFilter(),
                            this // callbacks implemented below
                    ).show(getSupportFragmentManager(), "filters")
            );
        }

        // Initial load (align with permissions logic in VM)
        currentUserId = AuthRepository.getInstance().getCurrentUserId();
        vm.loadForUser(currentUserId);

        // React to real-time auth changes
        AuthRepository.getInstance().getUserIdLive().observe(this, uid -> {
            boolean changed = (uid == null && currentUserId != null) || (uid != null && !uid.equals(currentUserId));
            if (changed) {
                currentUserId = uid;
                vm.loadForUser(currentUserId);
            }
        });
    }

    private void render(List<Project> items) {
        progress.setVisibility(View.GONE);
        if (items == null || items.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            adapter.submit(new ArrayList<>());
        } else {
            empty.setVisibility(View.GONE);
            adapter.submit(items);
        }
    }

    @Override
    public void onView(Project p) {
        // Navigate to read-only preview
        Intent i = new Intent(this, ProjectPreviewActivity.class);
        i.putExtra("projectId", p.getProjectId());
        startActivity(i);
    }

    // ===== FiltersBottomSheetDialogFragment callbacks =====
    @Override
    public void onApply(ProjectFilter filter) {
        vm.setFilter(filter);
        FilterPrefs.save(this, vm.getCurrentFilter());
    }

    @Override
    public void onReset() {
        vm.setFilter(new ProjectFilter());
        FilterPrefs.save(this, vm.getCurrentFilter());
    }
}
