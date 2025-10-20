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

        // טען פילטר אחרון ל־VM
        ProjectFilter saved = FilterPrefs.load(this);
        vm.setFilter(saved);

        // UID דרך שכבת האימות
        currentUserId = AuthRepository.getInstance().getCurrentUserId();
        vm.setUserId(currentUserId);    // יבדוק הרשאות ויטעין פרויקטים בהתאם

        // Adapter (הרשאות יעדכנו בהאזנה ל־isAdmin)
        adapter = new ProjectsAdapter(allProjects, new ProjectsAdapter.ProjectActionListener() {
            @Override public void onEdit(Project project) {
                Intent intent = new Intent(MyProjectsActivity.this, UploadImagesActivity.class);
                intent.putExtra("projectId", project.getProjectId());
                startActivity(intent);
            }
            @Override public void onImages(Project project) { /* TODO */ }
            @Override public void onReport(Project project) { /* TODO */ }
            @Override public void onDelete(Project project) { showDeleteConfirmationDialog(project); }
            @Override public void onAssignAppraiser(Project project) { showAssignAppraiserDialog(project); }
        }, this, /*isAdmin*/ false, currentUserId);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);

        // Pull-to-refresh
        swipeRefresh.setOnRefreshListener(vm::fetchProjects);

        // האזן לרשימת הפרויקטים מה-Repository → העבר ל-VM
        ProjectRepository.getInstance().getAllProjects().observe(this, projects -> {
            allProjects.clear();
            if (projects != null) allProjects.addAll(projects);
            vm.setAllProjects(allProjects);
            Log.d("UIUpdate", "Loaded " + allProjects.size() + " projects");
        });

        // רשימת שמאים מה־VM (מגיעה דרך ה־Repository)
        vm.getAppraisers().observe(this, list -> {
            allAppraisers.clear();
            if (list != null) allAppraisers.addAll(list);
        });
        vm.loadAllAppraisers();

        // פילטור סופי מה־VM → עדכון Adapter וצ'יפים
        vm.getFiltered().observe(this, filtered -> {
            adapter.updateData(filtered);
            updateEmptyState();
            FilterChipsController.render(
                    this, chipsActiveFilters, searchBar, vm.getCurrentFilter(),
                    () -> vm.setFilter(vm.getCurrentFilter()) // טריגר רענון
            );
        });

        // הרשאות ADMIN → מצב תצוגה וכפתורים
        vm.getIsAdmin().observe(this, isAdmin -> {
            boolean admin = isAdmin != null && isAdmin;
            adapter.updatePermissions(admin, currentUserId);
            chipGroupViewMode.setVisibility(admin ? View.VISIBLE : View.GONE);
            if (admin) setupViewModeChips(); else chipGroupViewMode.clearCheck();
        });

        // מצב רענון ל־spinner
        vm.getIsRefreshing().observe(this, refreshing -> {
            if (refreshing != null && refreshing) startRefreshing(); else stopRefreshing();
        });

        // טקסט חיפוש → VM + שמירה ל־Prefs
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            @Override public void onTextChanged(CharSequence s,int st,int b,int c){
                vm.setTextQuery(s == null ? null : s.toString());
                FilterPrefs.save(MyProjectsActivity.this, vm.getCurrentFilter());
            }
            @Override public void afterTextChanged(Editable s){}
        });

        // דיאלוג פילטרים
        findViewById(R.id.btnFilters).setOnClickListener(v ->
                new FiltersBottomSheetDialogFragment(vm.getCurrentFilter(), this)
                        .show(getSupportFragmentManager(), "filters"));

        // (אופציונלי) החלפת משתמש בזמן אמת
        AuthRepository.getInstance().getUserIdLive().observe(this, uid -> {
            boolean changed = (uid == null && currentUserId != null) || (uid != null && !uid.equals(currentUserId));
            if (changed) {
                currentUserId = uid;
                vm.setUserId(uid); // יבדוק הרשאות ויטעין מחדש
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ProjectRepository.getInstance().stopListening();
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

    // ===== עזרי UI =====
    private void setupViewModeChips() {
        chipGroupViewMode.setOnCheckedChangeListener((group, checkedId) ->
                vm.setViewingAll(checkedId == R.id.chipAllProjects));
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

    // ===== מחיקה =====
    private void showDeleteConfirmationDialog(Project project) {
        new AlertDialog.Builder(this)
                .setTitle("אישור מחיקת פרויקט")
                .setMessage("האם אתה בטוח שברצונך למחוק את הפרויקט '" + project.getFullAddress() + "'? פעולה זו אינה ניתנת לשיחזור.")
                .setPositiveButton("מחק", (d, w) -> vm.deleteProject(project.getProjectId()))
                .setNegativeButton("ביטול", null)
                .show();
    }

    // ===== הקצאת שמאים =====
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
                checked[i] = false; // יוצר הפרויקט לא שותף
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
