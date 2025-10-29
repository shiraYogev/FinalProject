package com.example.finalprojectappraisal.activity.myProjects;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.myProjects.filter.FiltersBottomSheetDialogFragment;
import com.example.finalprojectappraisal.activity.myProjects.filter.ProjectFilter;
import com.example.finalprojectappraisal.activity.myProjects.filter.ui.FilterChipsController;
import com.example.finalprojectappraisal.activity.myProjects.viewmodel.MyProjectsViewModel;
import com.example.finalprojectappraisal.activity.newProject.images.UploadImagesActivity;
import com.example.finalprojectappraisal.activity.AllProjects.ProjectPreviewActivity; // <<< NEW
import com.example.finalprojectappraisal.adapter.ProjectsAdapter;
import com.example.finalprojectappraisal.database.auth.AuthRepository;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Appraiser;
import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.utils.FilterPrefs;
import com.google.android.material.chip.ChipGroup;

import android.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MyProjectsActivity extends AppCompatActivity
        implements FiltersBottomSheetDialogFragment.OnFiltersAppliedListener {

    private static final String TAG_PREF = "MyProjectsPrefilter";
    private static final String TAG_REPORT = "MyProjectsReport"; // <<< NEW

    private MyProjectsViewModel vm;

    private ProjectsAdapter adapter;
    private final List<Project> allProjects = new ArrayList<>();
    private final List<Appraiser> allAppraisers = new ArrayList<>();

    private EditText searchBar;
    private TextView txtEmpty;
    private SwipeRefreshLayout swipeRefresh;
    private ChipGroup chipsActiveFilters;
    private ChipGroup chipGroupViewMode;

    private String currentUserId = null;

    // EXTRA name that Home page sends
    public static final String EXTRA_PREFILTER_STATUS = "prefilter_status";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_projects);

        vm = new ViewModelProvider(this).get(MyProjectsViewModel.class);

        RecyclerView recyclerView = findViewById(R.id.recyclerMyProjects);
        searchBar            = findViewById(R.id.searchBar);
        txtEmpty             = findViewById(R.id.txtEmpty);
        swipeRefresh         = findViewById(R.id.swipeRefresh);
        chipsActiveFilters   = findViewById(R.id.chipsActiveFilters);
        chipGroupViewMode    = findViewById(R.id.chipGroupViewMode);

        // Load last filter into VM
        ProjectFilter saved = FilterPrefs.load(this);
        vm.setFilter(saved);
        Log.d(TAG_PREF, "onCreate: loaded last FilterPrefs: " + new com.google.gson.Gson().toJson(saved));

        // UID from AuthRepository
        currentUserId = AuthRepository.getInstance().getCurrentUserId();
        vm.setUserId(currentUserId); // will check permissions and fetch projects
        Log.d(TAG_PREF, "onCreate: currentUserId=" + currentUserId);

        // Adapter
        adapter = new ProjectsAdapter(allProjects, new ProjectsAdapter.ProjectActionListener() {
            @Override public void onEdit(Project project) {
                Intent intent = new Intent(MyProjectsActivity.this, UploadImagesActivity.class);
                intent.putExtra("projectId", project.getProjectId());
                startActivity(intent);
            }
            @Override public void onImages(Project project) { /* TODO */ }

            @Override public void onReport(Project project) { // <<< NEW
                if (project == null || project.getProjectId() == null || project.getProjectId().trim().isEmpty()) {
                    Log.e(TAG_REPORT, "onReport: missing projectId");
                    Toast.makeText(MyProjectsActivity.this, "חסר מזהה פרויקט לתצוגת דוח", Toast.LENGTH_SHORT).show();
                    return;
                }
                String pid = project.getProjectId();
                Log.d(TAG_REPORT, "User tapped report for projectId=" + pid);

                Intent intent = new Intent(MyProjectsActivity.this, ProjectPreviewActivity.class);
                // שולחים כמה מפתחות נפוצים כדי להתאים ללוגיקה קיימת:
                intent.putExtra("projectId", pid);         // מקובל אצלך במסכים אחרים
                intent.putExtra("EXTRA_PROJECT_ID", pid);  // אם הActivity משתמש בקבוע הזה
                intent.putExtra("PROJECT_ID", pid);        // גיבוי נוסף
                startActivity(intent);
            }

            @Override public void onDelete(Project project) { showDeleteConfirmationDialog(project); }
            @Override public void onAssignAppraiser(Project project) { showAssignAppraiserDialog(project); }
        }, this, /*isAdmin*/ false, currentUserId);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);

        // Pull-to-refresh
        swipeRefresh.setOnRefreshListener(() -> {
            Log.d(TAG_PREF, "SwipeRefresh: vm.fetchProjects()");
            vm.fetchProjects();
        });

        // Repository projects → Activity holds full list → VM consumes
        ProjectRepository.getInstance().getAllProjects().observe(this, projects -> {
            allProjects.clear();
            if (projects != null) allProjects.addAll(projects);
            vm.setAllProjects(allProjects);
            Log.d(TAG_PREF, "Repo getAllProjects observed: size=" + allProjects.size());
        });

        // Appraisers list
        vm.getAppraisers().observe(this, list -> {
            allAppraisers.clear();
            if (list != null) allAppraisers.addAll(list);
            Log.d(TAG_PREF, "Appraisers observed: size=" + allAppraisers.size());
        });
        vm.loadAllAppraisers();

        // Final filtered list from VM → Adapter + chips
        vm.getFiltered().observe(this, filtered -> {
            Log.d(TAG_PREF, "getFiltered() size=" + (filtered == null ? 0 : filtered.size()));
            adapter.updateData(filtered);
            updateEmptyState();
            FilterChipsController.render(
                    this, chipsActiveFilters, searchBar, vm.getCurrentFilter(),
                    () -> {
                        Log.d(TAG_PREF, "ChipsController triggered vm.setFilter()");
                        vm.setFilter(vm.getCurrentFilter());
                    }
            );
        });

        // ADMIN perms → view mode
        vm.getIsAdmin().observe(this, isAdmin -> {
            boolean admin = isAdmin != null && isAdmin;
            Log.d(TAG_PREF, "isAdmin=" + admin);
            adapter.updatePermissions(admin, currentUserId);
            chipGroupViewMode.setVisibility(admin ? View.VISIBLE : View.GONE);
            if (admin) setupViewModeChips(); else chipGroupViewMode.clearCheck();
        });

        // Refreshing spinner state
        vm.getIsRefreshing().observe(this, refreshing -> {
            if (refreshing != null && refreshing) startRefreshing(); else stopRefreshing();
        });

        // Search text → VM + save to prefs
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            @Override public void onTextChanged(CharSequence s,int st,int b,int c){
                vm.setTextQuery(s == null ? null : s.toString());
                FilterPrefs.save(MyProjectsActivity.this, vm.getCurrentFilter());
                Log.d(TAG_PREF, "Search changed → vm.setTextQuery + save FilterPrefs");
            }
            @Override public void afterTextChanged(Editable s){}
        });

        // Filters bottom sheet
        findViewById(R.id.btnFilters).setOnClickListener(v -> {
            Log.d(TAG_PREF, "Open FiltersBottomSheetDialogFragment");
            new FiltersBottomSheetDialogFragment(vm.getCurrentFilter(), this)
                    .show(getSupportFragmentManager(), "filters");
        });

        // User switching
        AuthRepository.getInstance().getUserIdLive().observe(this, uid -> {
            boolean changed = (uid == null && currentUserId != null) || (uid != null && !uid.equals(currentUserId));
            if (changed) {
                Log.d(TAG_PREF, "UserId changed: " + currentUserId + " → " + uid);
                currentUserId = uid;
                vm.setUserId(uid);
            }
        });

        // <<< NEW: apply prefilter from Intent (one-shot) >>>
        applyPrefilterIfAny();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ProjectRepository.getInstance().stopListening();
    }

    // ===== FiltersBottomSheetDialogFragment callbacks =====
    @Override
    public void onApply(ProjectFilter filter) {
        Log.d(TAG_PREF, "BottomSheet onApply: " + new com.google.gson.Gson().toJson(filter));
        vm.setFilter(filter);
        FilterPrefs.save(this, vm.getCurrentFilter());
    }

    @Override
    public void onReset() {
        Log.d(TAG_PREF, "BottomSheet onReset -> new ProjectFilter()");
        vm.setFilter(new ProjectFilter());
        FilterPrefs.save(this, vm.getCurrentFilter());
    }

    // ===== Prefilter handling (NEW) =====
    private void applyPrefilterIfAny() {
        String preStatus = getIntent().getStringExtra(EXTRA_PREFILTER_STATUS);
        Log.d(TAG_PREF, "applyPrefilterIfAny() got extra prefilter_status=" + preStatus);

        if (preStatus == null || preStatus.trim().isEmpty()) {
            Log.d(TAG_PREF, "No prefilter_status extra. Using existing FilterPrefs only.");
            return;
        }

        try {
            // Save only statuses list while preserving other fields
            FilterPrefs.saveSingleStatus(this, preStatus);
            Log.d(TAG_PREF, "Saved preStatus to FilterPrefs: " + preStatus);

            // Load updated filter and apply to VM
            ProjectFilter f = FilterPrefs.load(this);
            Log.d(TAG_PREF, "Loaded FilterPrefs after save: " + new com.google.gson.Gson().toJson(f));

            vm.setFilter(f);
            Log.d(TAG_PREF, "vm.setFilter(f) called.");

            // Ensure refresh (in case VM relies on a new fetch)
            vm.fetchProjects();
            Log.d(TAG_PREF, "vm.fetchProjects() called after setFilter.");

        } catch (Throwable t) {
            Log.e(TAG_PREF, "Error applying prefilter", t);
        } finally {
            // prevent re-applying on back/rotation
            getIntent().removeExtra(EXTRA_PREFILTER_STATUS);
        }
    }

    // ===== UI helpers =====
    private void setupViewModeChips() {
        chipGroupViewMode.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(TAG_PREF, "ViewMode changed: checkedId=" + checkedId);
            vm.setViewingAll(checkedId == R.id.chipAllProjects);
        });
    }

    private void startRefreshing() {
        if (!swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(true);
    }

    private void stopRefreshing() {
        if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
    }

    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        if (txtEmpty != null) txtEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    // ===== Delete =====
    private void showDeleteConfirmationDialog(Project project) {
        new AlertDialog.Builder(this)
                .setTitle("אישור מחיקת פרויקט")
                .setMessage("האם אתה בטוח שברצונך למחוק את הפרויקט '" + project.getFullAddress() + "'? פעולה זו אינה ניתנת לשיחזור.")
                .setPositiveButton("מחק", (d, w) -> vm.deleteProject(project.getProjectId()))
                .setNegativeButton("ביטול", null)
                .show();
    }

    // ===== Assign co-appraisers =====
    private void showAssignAppraiserDialog(Project project) {
        if (project == null || allAppraisers.isEmpty()) {
            Toast.makeText(this, "לא ניתן להקצות שמאים כרגע. נסה שוב מאוחר יותר.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] appraiserNames = allAppraisers.stream().map(Appraiser::getFullName).toArray(String[]::new);
        List<String> allAppraiserIds = allAppraisers.stream().map(Appraiser::getAppraiserId).collect(Collectors.toList());
        boolean[] checked = new boolean[allAppraiserIds.size()];
        List<String> currentCo = project.getCoAppraiserIds();

        for (int i = 0; i < allAppraiserIds.size(); i++) {
            if (project.getAppraiserId().equals(allAppraiserIds.get(i))) {
                checked[i] = false; // owner is not a "co"
            } else {
                checked[i] = currentCo.contains(allAppraiserIds.get(i));
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("בחר שמאים שותפים לפרויקט")
                .setMultiChoiceItems(appraiserNames, checked, (dialog, which, isChecked) -> {
                    if (project.getAppraiserId().equals(allAppraiserIds.get(which))) {
                        Toast.makeText(this, "יוצר הפרויקט הוא תמיד חלק מהצוות ואינו ניתן להסרה כשותף", Toast.LENGTH_LONG).show();
                        ((AlertDialog) dialog).getListView().setItemChecked(which, false);
                    } else {
                        checked[which] = isChecked;
                    }
                })
                .setPositiveButton("שמור", (dialog, id) -> {
                    List<String> selected = new ArrayList<>();
                    for (int i = 0; i < allAppraiserIds.size(); i++) {
                        if (checked[i] && !project.getAppraiserId().equals(allAppraiserIds.get(i))) {
                            selected.add(allAppraiserIds.get(i));
                        }
                    }
                    vm.updateCoAppraisers(project.getProjectId(), selected);
                })
                .setNegativeButton("ביטול", (dialog, id) -> { })
                .create().show();
    }
}
