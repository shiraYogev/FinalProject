package com.example.finalprojectappraisal.activity.myProjects;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.stream.Collectors;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.myProjects.filter.FiltersBottomSheetDialogFragment;
import com.example.finalprojectappraisal.activity.myProjects.filter.ProjectFilter;
import com.example.finalprojectappraisal.activity.myProjects.filter.ProjectFilterEngine;
import com.example.finalprojectappraisal.activity.newProject.images.UploadImagesActivity;
import com.example.finalprojectappraisal.adapter.ProjectsAdapter;
import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.model.Appraiser;
import com.example.finalprojectappraisal.utils.FilterChipUtils;
import com.example.finalprojectappraisal.utils.FilterPrefs;
import com.example.finalprojectappraisal.utils.StatusMapper;
import com.example.finalprojectappraisal.utils.TextNormalizer;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.AlertDialog;

public class MyProjectsActivity extends AppCompatActivity
        implements FiltersBottomSheetDialogFragment.OnFiltersAppliedListener {

    private ProjectsAdapter adapter;
    private final List<Project> allProjects = new ArrayList<>();
    private EditText searchBar;
    private TextView txtEmpty;
    private SwipeRefreshLayout swipeRefresh;

    // פילטר מרכזי (ניטען מ-SharedPreferences בשלב onCreate)
    private ProjectFilter currentFilter;

    // צ'יפים פעילים
    private ChipGroup chipsActiveFilters;
    private final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    // משתנים עבור כפתורי עריכה למנהלים
    private ChipGroup chipGroupViewMode;
    private boolean isViewingAllProjects = false;

    // הרשאות משתמש
    private boolean isAdmin = false;
    private String currentUserId = null;

    // רשימה של כל השמאים במערכת (לשימוש בדיאלוג הקצאה)
    private List<Appraiser> allAppraisers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_projects);

        RecyclerView recyclerView = findViewById(R.id.recyclerMyProjects);
        searchBar = findViewById(R.id.searchBar);
        txtEmpty = findViewById(R.id.txtEmpty);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        chipsActiveFilters = findViewById(R.id.chipsActiveFilters);
        chipGroupViewMode = findViewById(R.id.chipGroupViewMode);

        // טען פילטר אחרון
        currentFilter = FilterPrefs.load(this);

        // קבל את המשתמש הנוכחי
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : null;

        // אתחל adapter עם הרשאות ברירת מחדל
        adapter = new ProjectsAdapter(allProjects, new ProjectsAdapter.ProjectActionListener() {
            @Override
            public void onEdit(Project project) {
                Intent intent = new Intent(MyProjectsActivity.this, UploadImagesActivity.class);
                intent.putExtra("projectId", project.getProjectId());
                startActivity(intent);
            }

            @Override
            public void onImages(Project project) { /* TODO */ }

            @Override
            public void onReport(Project project) { /* TODO */ }

            @Override
            public void onDelete(Project project) {
                //ProjectRepository.getInstance().deleteProject(project.getProjectId(), task -> {
                //    if (task.isSuccessful()) {
                //        Toast.makeText(MyProjectsActivity.this, "הפרויקט נמחק", Toast.LENGTH_SHORT).show();
                //        reload(); // רענון אחרי מחיקה
                //    } else {
                //        Toast.makeText(MyProjectsActivity.this, "מחיקה נכשלה", Toast.LENGTH_SHORT).show();
                //        stopRefreshing();
                //    }
                //});

                // ** קוד חדש: הצגת תיבת דו-שיח לאישור מחיקה **
                showDeleteConfirmationDialog(project);
            }

            @Override
            public void onAssignAppraiser(Project project) { // ** 🆕 מימוש המתודה החדשה **
                showAssignAppraiserDialog(project);
            }
        }, this, isAdmin, currentUserId);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);

        // בדוק הרשאות משתמש לפני טעינת הפרויקטים
        checkUserPermissions();

        // סנכרון שורת החיפוש עם מצב שמור
        if (currentFilter.getTextQuery() != null) {
            searchBar.setText(currentFilter.getTextQuery());
        }

        // Pull-to-refresh
        swipeRefresh.setOnRefreshListener(this::reload);

        // מאזין לשינויים בפרויקטים
        subscribeProjectsLive();
        // ** 🆕 טעינה של כל השמאים בפתיחת האפליקציה (רק פעם אחת) **
        loadAllAppraisers();

        // חיפוש לפי כתובת בלבד
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String normalized = TextNormalizer.normalizeOrNull(s == null ? null : s.toString());
                currentFilter.setTextQuery(normalized);
                FilterPrefs.save(MyProjectsActivity.this, currentFilter);
                recompute();
                renderActiveFilterChips();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btnNewProject).setOnClickListener(v -> {
            // TODO: Intent למסך יצירת פרויקט
        });

        findViewById(R.id.btnFilters).setOnClickListener(v -> {
            new FiltersBottomSheetDialogFragment(currentFilter, this)
                    .show(getSupportFragmentManager(), "filters");
        });
    }

    /** מאזין ללייב-דאטה שמכילה את כל הפרויקטים */
    private void subscribeProjectsLive() {
        ProjectRepository.getInstance().getAllProjects().observe(this, projects -> {
            allProjects.clear();
            if (projects != null) allProjects.addAll(projects);
            recompute();
            Log.d("UIUpdate", "Loaded " + allProjects.size() + " projects");
            stopRefreshing(); // להסתיר ספינר אחרי קבלת הדאטה
        });
    }

    /** **מתודה חדשה לטעינת פרויקטים לפי מצב הצפייה** */
    private void fetchProjects() {
        String appraiserIdStr = getCurrentAppraiserIdString();
        if (appraiserIdStr == null || appraiserIdStr.trim().isEmpty()) {
            Toast.makeText(this, "לא נמצא appraiserId למשתמש הנוכחי", Toast.LENGTH_SHORT).show();
            stopRefreshing();
            return;
        }

        if (isViewingAllProjects) {
            // מנהלים רואים את כל הפרויקטים
            ProjectRepository.getInstance().loadAllProjectsWithListener();
        } else {
            // כולם רואים את הפרויקטים הפעילים שלהם
            ProjectRepository.getInstance().loadActiveProjectsForAppraiser(appraiserIdStr);
        }
    }

    /** משמש ל-pull-to-refresh וגם למחיקות/עדכונים */
    private void reload() {
        startRefreshing();
        fetchProjects();
    }

    private void startRefreshing() {
        if (!swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(true);
    }

    private void stopRefreshing() {
        if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onApply(ProjectFilter filter) {
        this.currentFilter = filter;
        FilterPrefs.save(this, currentFilter);
        recompute();
        renderActiveFilterChips();
    }

    @Override
    public void onReset() {
        this.currentFilter = new ProjectFilter();
        FilterPrefs.save(this, currentFilter);
        recompute();
        renderActiveFilterChips();
    }

    private String getCurrentAppraiserIdString() {
        return currentUserId;
    }

    /** מריץ את מנוע הפילטרים על allProjects לפי currentFilter ומרענן UI */
    private void recompute() {
        List<Project> out = ProjectFilterEngine.apply(allProjects, currentFilter);
        adapter.updateData(out);
        updateEmptyState();
        renderActiveFilterChips();
    }

    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        if (txtEmpty != null) {
            txtEmpty.setVisibility(isEmpty ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }

    /** בונה צ'יפים לכל פילטר פעיל ומאפשר ניקוי מהיר ע"י X */
    private void renderActiveFilterChips() {
        if (chipsActiveFilters == null) return;
        chipsActiveFilters.removeAllViews();

        String searchText = searchBar != null ? searchBar.getText().toString().trim() : "";
        if (!searchText.isEmpty()) {
            chipsActiveFilters.addView(
                    FilterChipUtils.makeEntryChip(this, "חיפוש: " + searchText, () -> {
                        if (searchBar != null) searchBar.setText("");
                        currentFilter.setTextQuery(null);
                        FilterPrefs.save(this, currentFilter);
                        recompute();
                    })
            );
        }

        for (String code : currentFilter.getStatuses()) {
            String label = StatusMapper.codeToUiLabel(code);
            chipsActiveFilters.addView(
                    FilterChipUtils.makeEntryChip(this, "סטטוס: " + label, () -> {
                        currentFilter.getStatuses().remove(code);
                        FilterPrefs.save(this, currentFilter);
                        recompute();
                    })
            );
        }

        Long from = currentFilter.getDateFromEpochMillis();
        Long to = currentFilter.getDateToEpochMillis();
        if (from != null || to != null) {
            String txt;
            if (from != null && to != null) {
                txt = "טווח: " + df.format(new Date(from)) + "–" + df.format(new Date(to));
            } else if (from != null) {
                txt = "מ־" + df.format(new Date(from));
            } else {
                txt = "עד " + df.format(new Date(to));
            }
            chipsActiveFilters.addView(
                    FilterChipUtils.makeEntryChip(this, txt, () -> {
                        currentFilter.setDateFromEpochMillis(null);
                        currentFilter.setDateToEpochMillis(null);
                        FilterPrefs.save(this, currentFilter);
                        recompute();
                    })
            );
        }

        if (currentFilter.getDateField() == ProjectFilter.DateField.CREATED) {
            chipsActiveFilters.addView(
                    FilterChipUtils.makeEntryChip(this, "שדה: יצירה", () -> {
                        currentFilter.setDateField(ProjectFilter.DateField.LAST_UPDATE);
                        FilterPrefs.save(this, currentFilter);
                        recompute();
                    })
            );
        }

        boolean isDefaultSort = currentFilter.getSortBy() == ProjectFilter.SortField.LAST_UPDATE
                && currentFilter.getSortDir() == ProjectFilter.SortDir.DESC;
        if (!isDefaultSort) {
            String sortLabel;
            switch (currentFilter.getSortBy()) {
                case CREATED: sortLabel = "תאריך יצירה"; break;
                case CITY:    sortLabel = "כתובת (A→Z)"; break;
                default:      sortLabel = "עדכון אחרון";
            }
            String arrow = (currentFilter.getSortDir() == ProjectFilter.SortDir.DESC) ? "↓" : "↑";
            chipsActiveFilters.addView(
                    FilterChipUtils.makeEntryChip(this, "מיון: " + sortLabel + " " + arrow, () -> {
                        currentFilter.setSortBy(ProjectFilter.SortField.LAST_UPDATE);
                        currentFilter.setSortDir(ProjectFilter.SortDir.DESC);
                        FilterPrefs.save(this, currentFilter);
                        recompute();
                    })
            );
        }
    }

    // **חדש: מתודה לבדיקת הרשאות המשתמש והגדרת ה-UI**
    private void checkUserPermissions() {
        if (currentUserId == null) {
            fetchProjects();
            return;
        }

        ProjectRepository.getInstance().getUserPermissions(currentUserId, task -> {
            if (task.isSuccessful()) {
                Appraiser.AccessPermission permission = task.getResult();
                isAdmin = (permission == Appraiser.AccessPermission.ADMIN ||
                        permission == Appraiser.AccessPermission.SUPER_ADMIN);

                // עדכן את ה-adapter עם ההרשאות החדשות
                adapter.updatePermissions(isAdmin, currentUserId);

                if (isAdmin) {
                    chipGroupViewMode.setVisibility(View.VISIBLE);
                    setupViewModeChips();
                } else {
                    chipGroupViewMode.setVisibility(View.GONE);
                    isViewingAllProjects = false;
                }
            } else {
                // במקרה של שגיאה, נתייחס למשתמש כרגיל
                isAdmin = false;
                adapter.updatePermissions(false, currentUserId);
                chipGroupViewMode.setVisibility(View.GONE);
                isViewingAllProjects = false;
            }

            fetchProjects();
        });
    }

    // **חדש: מתודה להגדרת המאזין לכפתורי הבורר**
    private void setupViewModeChips() {
        chipGroupViewMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAllProjects) {
                isViewingAllProjects = true;
            } else {
                isViewingAllProjects = false;
            }
            reload();
        });
    }
    /** מציג תיבת דו-שיח לאישור מחיקת פרויקט */
    /** מציג תיבת דו-שיח לאישור מחיקת פרויקט */
    private void showDeleteConfirmationDialog(Project project) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("אישור מחיקת פרויקט")
                // ** כאן התיקון: שימוש ב-project.getFullAddress() במקום project.getAddress() **
                .setMessage("האם אתה בטוח שברצונך למחוק את הפרויקט '" + project.getFullAddress() + "'? פעולה זו אינה ניתנת לשיחזור.")
                .setPositiveButton("מחק", (dialog, which) -> {
                    // אם המשתמש אישר, בצע מחיקה
                    performProjectDeletion(project);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    /** מבצע את המחיקה בפועל לאחר האישור */
    private void performProjectDeletion(Project project) {
        ProjectRepository.getInstance().deleteProject(project.getProjectId(), task -> {
            if (task.isSuccessful()) {
                Toast.makeText(MyProjectsActivity.this, "הפרויקט נמחק בהצלחה", Toast.LENGTH_SHORT).show();
                reload(); // רענון אחרי מחיקה
            } else {
                Toast.makeText(MyProjectsActivity.this, "מחיקה נכשלה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                stopRefreshing();
            }
        });
    }

    // ----------------------------------------------------------------------
    // ** 🆕 מתודות חדשות: הקצאת שמאי **
    // ----------------------------------------------------------------------

    /**
     * מציג דיאלוג לבחירת שמאים שותפים לפרויקט.
     * מנהלים יכולים להקצות שמאים אחרים.
     * @param project הפרויקט שאליו מקצים שמאים.
     */
    private void showAssignAppraiserDialog(Project project) {
        if (project == null || allAppraisers.isEmpty()) {
            Toast.makeText(this, "לא ניתן להקצות שמאים כרגע. נסה שוב מאוחר יותר.", Toast.LENGTH_SHORT).show();
            return;
        }

        // רשימת שמות השמאים להצגה בדיאלוג
        String[] appraiserNames = allAppraisers.stream()
                .map(Appraiser::getFullName)
                .toArray(String[]::new);

        // רשימה של ה-IDs של השמאים עבור הלוגיקה
        List<String> allAppraiserIds = allAppraisers.stream()
                .map(Appraiser::getAppraiserId)
                .collect(Collectors.toList());

        // מצב הבחירה הנוכחי: אילו שמאים שותפים כבר מוקצים לפרויקט?
        boolean[] checkedAppraisers = new boolean[allAppraiserIds.size()];
        List<String> currentCoAppraiserIds = project.getCoAppraiserIds();

        for (int i = 0; i < allAppraiserIds.size(); i++) {
            // אל תציע את יוצר הפרויקט (appraiserId) כשמאי שותף, הוא כבר "שותף ראשי"
            if (project.getAppraiserId().equals(allAppraiserIds.get(i))) {
                checkedAppraisers[i] = false; // הוא לא יכול להיות שותף
            } else {
                checkedAppraisers[i] = currentCoAppraiserIds.contains(allAppraiserIds.get(i));
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("בחר שמאים שותפים לפרויקט")
                .setMultiChoiceItems(appraiserNames, checkedAppraisers,
                        (dialog, which, isChecked) -> {
                            // כפתור ביטול עבור יוצר הפרויקט
                            if (project.getAppraiserId().equals(allAppraiserIds.get(which))) {
                                Toast.makeText(MyProjectsActivity.this, "יוצר הפרויקט הוא תמיד חלק מהצוות ואינו ניתן להסרה כשותף", Toast.LENGTH_LONG).show();
                                ((AlertDialog) dialog).getListView().setItemChecked(which, false); // לבטל את הבחירה
                            } else {
                                checkedAppraisers[which] = isChecked;
                            }
                        })
                .setPositiveButton("שמור", (dialog, id) -> {
                    List<String> selectedAppraiserIds = new ArrayList<>();
                    for (int i = 0; i < allAppraiserIds.size(); i++) {
                        if (checkedAppraisers[i] && !project.getAppraiserId().equals(allAppraiserIds.get(i))) {
                            selectedAppraiserIds.add(allAppraiserIds.get(i));
                        }
                    }
                    // וודא שיוצר הפרויקט תמיד כלול בלוגיקה (גם אם לא ב-coAppraiserIds)
                    // אבל ה-coAppraiserIds הם רק "שותפים"
                    assignAppraiserToProject(project, selectedAppraiserIds);
                })
                .setNegativeButton("ביטול", (dialog, id) -> {
                    // ביטול
                });
        builder.create().show();
    }


    /**
     * שומר את רשימת השמאים השותפים המעודכנת לפרויקט ב-Firestore.
     * @param project הפרויקט לעדכון.
     * @param selectedCoAppraiserIds רשימת ה-UID של השמאים שנבחרו להיות שותפים.
     */
    private void assignAppraiserToProject(Project project, List<String> selectedCoAppraiserIds) {
        if (project == null || TextUtils.isEmpty(project.getProjectId())) {
            Toast.makeText(this, "שגיאה: פרטי פרויקט חסרים.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🆕 קריאה למתודה החדשה ב-ProjectRepository שמעדכנת גם את הפרויקט וגם את השמאים
        ProjectRepository.getInstance().updateProjectAndAppraiserAssignments(
                project.getProjectId(),
                selectedCoAppraiserIds,
                task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MyProjectsActivity.this, "שמאים שותפים עודכנו בהצלחה ✅", Toast.LENGTH_SHORT).show();
                        reload(); // ⬅️ השתמש ב-reload() אם זו המתודה שלך לרענון נתונים
                    } else {
                        Toast.makeText(MyProjectsActivity.this, "עדכון שמאים שותפים נכשל: " + task.getException().getMessage() + " ❌", Toast.LENGTH_LONG).show();
                        Log.e("MyProjectsActivity", "Error updating co-appraisers: " + task.getException().getMessage());
                    }
                });
    }

    // ** 🆕 מתודה חדשה: טעינת כל השמאים מה-Firestore **
    private void loadAllAppraisers() {
        ProjectRepository.getInstance().getAllAppraisers(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                allAppraisers.clear();
                allAppraisers.addAll(task.getResult());
                Log.d("MyProjectsActivity", "Loaded " + allAppraisers.size() + " appraisers.");
            } else {
                Log.e("MyProjectsActivity", "Failed to load appraisers: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                Toast.makeText(MyProjectsActivity.this, "שגיאה בטעינת השמאים במערכת", Toast.LENGTH_SHORT).show();
            }
        });
    }





}