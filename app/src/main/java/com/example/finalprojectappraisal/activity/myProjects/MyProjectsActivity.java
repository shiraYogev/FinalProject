package com.example.finalprojectappraisal.activity.myProjects;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.AllProjects.ProjectPreviewActivity;
import com.example.finalprojectappraisal.activity.myProjects.filter.FiltersBottomSheetDialogFragment;
import com.example.finalprojectappraisal.activity.myProjects.filter.ProjectFilter;
import com.example.finalprojectappraisal.activity.myProjects.filter.ui.FilterChipsController;
import com.example.finalprojectappraisal.activity.myProjects.note.AddNoteBottomSheetDialogFragment;
import com.example.finalprojectappraisal.activity.myProjects.viewmodel.MyProjectsViewModel;
import com.example.finalprojectappraisal.adapter.ProjectsAdapter;
import com.example.finalprojectappraisal.database.auth.AuthRepository;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Appraiser;
import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.activity.CompassMapActivity;
import com.example.finalprojectappraisal.utils.FilterPrefs;
import com.example.finalprojectappraisal.utils.MapIntentUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MyProjectsActivity extends AppCompatActivity
        implements FiltersBottomSheetDialogFragment.OnFiltersAppliedListener,
        AddNoteBottomSheetDialogFragment.OnNoteSavedListener {

    private static final String TAG_PREF   = "MyProjectsPrefilter";
    private static final int    REQ_MAP_LOC = 1002;
    private static final String TAG_REPORT = "MyProjectsReport";
    private static final String TAG_ACT = "MyProjectsActivity";

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
    private FusedLocationProviderClient fusedLocationClient;

    // EXTRAs that מסכים אחרים יכולים לשלוח
    public static final String EXTRA_PREFILTER_STATUS = "prefilter_status";
    public static final String EXTRA_PREFILTER_QUERY  = "prefilter_query";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_projects);

        // ===== Back button (header) =====
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                // חוזר למסך הקודם (בדרך כלל מסך הבית)
                finish();
            });
        }

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
        Log.d(TAG_PREF, "onCreate: loaded FilterPrefs: " + new com.google.gson.Gson().toJson(saved));

        // Clear stale textQuery (prevents empty results if something stuck from previous session)
        if (saved != null && saved.getTextQuery() != null && !saved.getTextQuery().isEmpty()) {
            Log.d(TAG_PREF, "Clearing stale textQuery from prefs: '" + saved.getTextQuery() + "'");
            searchBar.setText(""); // triggers TextWatcher and saves back to prefs
        }

        // UID from AuthRepository
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        currentUserId = AuthRepository.getInstance().getCurrentUserId();
        Log.d(TAG_ACT, "onCreate: currentUserId=" + currentUserId);
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            Toast.makeText(this, "⚠️ אינך מחוברת — ייתכן שיופיעו 0 פרויקטים", Toast.LENGTH_LONG).show();
        }
        vm.setUserId(currentUserId); // will check permissions and fetch projects

        // Adapter
        adapter = new ProjectsAdapter(allProjects, new ProjectsAdapter.ProjectActionListener() {

            @Override public void onCompass(Project project) {
                Log.d(TAG_ACT, "onCompass invoked from Adapter for pid=" + project.getProjectId());
                startActivity(new Intent(MyProjectsActivity.this, CompassMapActivity.class));
            }

            @Override public void onMap(Project project) {
                Log.d(TAG_ACT, "onMap invoked from Adapter for pid=" + project.getProjectId());
                openGovmapAtCurrentLocation();
            }

            @Override public void onEdit(Project project) {
                Intent intent = new Intent(MyProjectsActivity.this,
                        com.example.finalprojectappraisal.activity.newProject.images.UploadImagesActivity.class);
                intent.putExtra("projectId", project.getProjectId());
                startActivity(intent);
            }

            @Override public void onAddNote(Project project) {
                if (project == null || project.getProjectId() == null || project.getProjectId().trim().isEmpty()) {
                    Toast.makeText(MyProjectsActivity.this, "חסר מזהה פרויקט", Toast.LENGTH_SHORT).show();
                    return;
                }
                AddNoteBottomSheetDialogFragment
                        .newInstance(project.getProjectId(), project.getNote())
                        .show(getSupportFragmentManager(), "add_note");
            }

            @Override public void onReport(Project project) {
                if (project == null || project.getProjectId() == null || project.getProjectId().trim().isEmpty()) {
                    Log.e(TAG_REPORT, "onReport: missing projectId");
                    Toast.makeText(MyProjectsActivity.this, "חסר מזהה פרויקט לתצוגת דוח", Toast.LENGTH_SHORT).show();
                    return;
                }
                String pid = project.getProjectId();
                Log.d(TAG_REPORT, "User tapped report for projectId=" + pid);

                Intent intent = new Intent(MyProjectsActivity.this, ProjectPreviewActivity.class);
                intent.putExtra("projectId", pid);
                intent.putExtra("EXTRA_PROJECT_ID", pid);
                intent.putExtra("PROJECT_ID", pid);
                startActivity(intent);
            }

            @Override public void onDelete(Project project) { showDeleteConfirmationDialog(project); }
            @Override public void onAssignAppraiser(Project project) { showAssignAppraiserDialog(project); }
        }, this, /*isAdmin*/ false, currentUserId);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(
                this, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL));
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(0, 8, 0, 8);

        // Pull-to-refresh
        swipeRefresh.setOnRefreshListener(() -> {
            Log.d(TAG_ACT, "SwipeRefresh: vm.fetchProjects()");
            vm.fetchProjects();
        });

        // Repository projects → Activity holds full list → VM consumes
        ProjectRepository.getInstance().getAllProjects().observe(this, projects -> {
            int size = (projects == null ? 0 : projects.size());
            Log.d("MyProjectsRepo", "LiveData getAllProjects observed: size=" + size
                    + (size > 0 ? (", firstPid=" + (projects.get(0) != null ? projects.get(0).getProjectId() : "null")) : ""));
            allProjects.clear();
            if (projects != null) allProjects.addAll(projects);
            vm.setAllProjects(allProjects);
        });

        // Appraisers list
        vm.getAppraisers().observe(this, list -> {
            allAppraisers.clear();
            if (list != null) allAppraisers.addAll(list);
            Log.d(TAG_ACT, "Appraisers observed: size=" + (list == null ? 0 : list.size()));
        });
        vm.loadAllAppraisers();

        // Final filtered list from VM → Adapter + chips
        vm.getFiltered().observe(this, filtered -> {
            Log.d(TAG_PREF, "getFiltered() size=" + (filtered == null ? 0 : filtered.size())
                    + " | viewingAll=" + vm.getIsViewingAll().getValue()
                    + " | isAdmin=" + vm.getIsAdmin().getValue()
                    + " | filter=" + new com.google.gson.Gson().toJson(vm.getCurrentFilter()));
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
            Log.d(TAG_ACT, "isAdmin=" + admin);
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
                Log.d(TAG_PREF, "Search changed → vm.setTextQuery + save FilterPrefs: '" + s + "'");
            }
            @Override public void afterTextChanged(Editable s){}
        });

        // Filters bottom sheet
        findViewById(R.id.btnFilters).setOnClickListener(v -> {
            Log.d(TAG_PREF, "Open FiltersBottomSheetDialogFragment");
            new FiltersBottomSheetDialogFragment(vm.getCurrentFilter(), this)
                    .show(getSupportFragmentManager(), "filters");
        });

        findViewById(R.id.btnNewProject).setOnClickListener(v -> {
            Log.d(TAG_ACT, "User tapped 'New Project' button. Starting ClientDetailsActivity.");
            Intent intent = new Intent(MyProjectsActivity.this,
                    com.example.finalprojectappraisal.activity.newProject.client.ClientDetailsActivity.class);
            startActivity(intent);
        });

        // User switching
        AuthRepository.getInstance().getUserIdLive().observe(this, uid -> {
            boolean changed = (uid == null && currentUserId != null) || (uid != null && !uid.equals(currentUserId));
            Log.d(TAG_ACT, "Auth user changed? " + changed + " | old=" + currentUserId + " new=" + uid);
            if (changed) {
                currentUserId = uid;
                vm.setUserId(uid);
            }
        });

        // <<< NEW: apply prefilter from Intent (status OR query) >>>
        applyPrefilterIfAny();

        // בעיטה יזומה אחרי שכל ה-observers מחוברים
        Log.d(TAG_ACT, "KICK: viewingAll=true + fetchProjects()");
        vm.setViewingAll(true);
        vm.fetchProjects();
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

    // ===== OnNoteSavedListener callback =====
    @Override
    public void onNoteSaved(@NonNull String projectId, @NonNull String newNote) {
        Log.d(TAG_ACT, "onNoteSaved: pid=" + projectId + " len=" + newNote.length());
        if (vm != null) vm.fetchProjects();
    }

    // ===== Prefilter handling (STATUS or QUERY) =====
    private void applyPrefilterIfAny() {
        String preStatus = getIntent().getStringExtra(EXTRA_PREFILTER_STATUS);
        String preQuery  = getIntent().getStringExtra(EXTRA_PREFILTER_QUERY);
        Log.d(TAG_PREF, "applyPrefilterIfAny() got extras: prefilter_status=" + preStatus
                + ", prefilter_query=" + preQuery);

        // 1. אם הגיע סטטוס – נשמור התנהגות קיימת
        if (preStatus != null && !preStatus.trim().isEmpty()) {
            try {
                FilterPrefs.saveSingleStatus(this, preStatus);
                Log.d(TAG_PREF, "Saved preStatus to FilterPrefs: " + preStatus);

                ProjectFilter f = FilterPrefs.load(this);
                Log.d(TAG_PREF, "Loaded FilterPrefs after save: " + new com.google.gson.Gson().toJson(f));

                vm.setFilter(f);
                Log.d(TAG_PREF, "vm.setFilter(f) called.");

                vm.fetchProjects();
                Log.d(TAG_PREF, "vm.fetchProjects() called after setFilter (status).");
            } catch (Throwable t) {
                Log.e(TAG_PREF, "Error applying status prefilter", t);
            } finally {
                getIntent().removeExtra(EXTRA_PREFILTER_STATUS);
            }
            return;
        }

        // 2. אם הגיע טקסט לחיפוש (למשל מהמסך הראשי – שם/כתובת הפרויקט)
        if (preQuery != null && !preQuery.trim().isEmpty()) {
            Log.d(TAG_PREF, "Applying text prefilter from EXTRA_PREFILTER_QUERY: '" + preQuery + "'");
            // זה מפעיל את ה-TextWatcher → vm.setTextQuery + FilterPrefs.save
            searchBar.setText(preQuery);
            vm.fetchProjects();
            getIntent().removeExtra(EXTRA_PREFILTER_QUERY);
        } else {
            Log.d(TAG_PREF, "No prefilter extras. Using existing FilterPrefs only.");
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

    // מפה — GOVMAP במיקום הנוכחי
    private void openGovmapAtCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_MAP_LOC);
            return;
        }
        doOpenGovmapWithLocation();
    }

    @SuppressLint("MissingPermission")
    private void doOpenGovmapWithLocation() {
        // 1) Make sure location services (GPS) are actually turned on
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsOn = lm != null && (
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        if (!gpsOn) {
            new AlertDialog.Builder(this)
                    .setTitle("שירותי מיקום כבויים")
                    .setMessage("כדי לפתוח את המפה במיקום הנוכחי יש להפעיל את ה-GPS.")
                    .setPositiveButton("פתח הגדרות", (d, w) ->
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("ביטול", null)
                    .show();
            return;
        }

        // 2) Fast path — last known location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, last -> {
                    if (last != null) {
                        MapIntentUtils.openCurrentLocationInGovmap(
                                this, last.getLatitude(), last.getLongitude());
                    } else {
                        // 3) Active fix — request a single fresh update
                        requestSingleLocationUpdate();
                    }
                })
                .addOnFailureListener(e -> requestSingleLocationUpdate());
    }

    @SuppressLint("MissingPermission")
    private void requestSingleLocationUpdate() {
        Toast.makeText(this, "מאתר מיקום...", Toast.LENGTH_SHORT).show();

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMaxUpdates(1)
                .setDurationMillis(15000L)
                .build();

        final boolean[] handled = {false};
        LocationCallback callback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (handled[0]) return;
                handled[0] = true;
                fusedLocationClient.removeLocationUpdates(this);
                if (result.getLastLocation() != null) {
                    MapIntentUtils.openCurrentLocationInGovmap(
                            MyProjectsActivity.this,
                            result.getLastLocation().getLatitude(),
                            result.getLastLocation().getLongitude());
                } else {
                    Toast.makeText(MyProjectsActivity.this,
                            "לא ניתן לקבל מיקום — ודאי שה-GPS פעיל ונסי שוב", Toast.LENGTH_LONG).show();
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MAP_LOC
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            doOpenGovmapWithLocation();
        } else if (requestCode == REQ_MAP_LOC) {
            Toast.makeText(this, "נדרשת הרשאת מיקום לפתיחת המפה", Toast.LENGTH_SHORT).show();
        }
    }
}
