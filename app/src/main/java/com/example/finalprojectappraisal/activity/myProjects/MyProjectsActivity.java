package com.example.finalprojectappraisal.activity.myProjects;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.finalprojectappraisal.model.Project;
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
import java.util.List;
import java.util.Locale;

public class MyProjectsActivity extends AppCompatActivity
        implements FiltersBottomSheetDialogFragment.OnFiltersAppliedListener {

    private ProjectsAdapter adapter;
    private final List<Project> allProjects = new ArrayList<>();
    private EditText searchBar;
    private TextView txtEmpty;
    private SwipeRefreshLayout swipeRefresh;

    // פילטר מרכזי (ניטען מ-SharedPreferences בשלב onCreate)
    private ProjectFilter currentFilter;

    // צ'יפים פעילים (אם הוספת ל-XML)
    private ChipGroup chipsActiveFilters;
    private final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_projects);

        RecyclerView recyclerView = findViewById(R.id.recyclerMyProjects);
        searchBar         = findViewById(R.id.searchBar);
        txtEmpty          = findViewById(R.id.txtEmpty);
        swipeRefresh      = findViewById(R.id.swipeRefresh);
        chipsActiveFilters= findViewById(R.id.chipsActiveFilters); // יכול להיות null אם לא הוספת ל-XML

        // טען פילטר אחרון
        currentFilter = FilterPrefs.load(this);

        adapter = new ProjectsAdapter(allProjects, new ProjectsAdapter.ProjectActionListener() {
            @Override public void onEdit(Project project) {
                Intent intent = new Intent(MyProjectsActivity.this, UploadImagesActivity.class);
                intent.putExtra("projectId", project.getProjectId());
                startActivity(intent);
            }
            @Override public void onImages(Project project) { /* TODO */ }
            @Override public void onReport(Project project) { /* TODO */ }
            @Override public void onDelete(Project project) {
                ProjectRepository.getInstance().deleteProject(project.getProjectId(), task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MyProjectsActivity.this, "הפרויקט נמחק", Toast.LENGTH_SHORT).show();
                        reload(); // רענון אחרי מחיקה
                    } else {
                        Toast.makeText(MyProjectsActivity.this, "מחיקה נכשלה", Toast.LENGTH_SHORT).show();
                        stopRefreshing();
                    }
                });
            }
        }, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);

        // סנכרון שורת החיפוש עם מצב שמור
        if (currentFilter.getTextQuery() != null) {
            searchBar.setText(currentFilter.getTextQuery());
        }

        // Pull-to-refresh
        swipeRefresh.setOnRefreshListener(this::reload);

        // חיפוש לפי כתובת בלבד
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String normalized = TextNormalizer.normalizeOrNull(s == null ? null : s.toString());
                currentFilter.setTextQuery(normalized);
                FilterPrefs.save(MyProjectsActivity.this, currentFilter);
                recompute();
                renderActiveFilterChips();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnNewProject).setOnClickListener(v -> {
            // TODO: Intent למסך יצירת פרויקט
        });

        findViewById(R.id.btnFilters).setOnClickListener(v -> {
            new FiltersBottomSheetDialogFragment(currentFilter, this)
                    .show(getSupportFragmentManager(), "filters");
        });

        // טעינה ראשונית עם ספינר
        startRefreshing();
        subscribeProjectsLive(); // מאזין ל-LiveData
        fetchActiveProjects();   // בקשה לטעינה
    }

    /** מאזין ללייב-דאטה שמכילה את כל הפרויקטים */
    private void subscribeProjectsLive() {
        ProjectRepository.getInstance().getAllProjects().observe(this, projects -> {
            allProjects.clear();
            if (projects != null) allProjects.addAll(projects);
            recompute();
            Log.d("UIUpdate", "Loaded " + allProjects.size() + " active projects");
            stopRefreshing(); // להסתיר ספינר אחרי קבלת הדאטה
        });
    }

    /** בקשה לרענון/טעינה מול ה-Repository */
    private void fetchActiveProjects() {
        String appraiserIdStr = getCurrentAppraiserIdString();
        if (appraiserIdStr == null || appraiserIdStr.trim().isEmpty()) {
            Toast.makeText(this, "לא נמצא appraiserId למשתמש הנוכחי", Toast.LENGTH_SHORT).show();
            stopRefreshing();
            return;
        }
        ProjectRepository.getInstance().loadActiveProjectsForAppraiser(appraiserIdStr);
    }

    /** משמש ל-pull-to-refresh וגם למחיקות/עדכונים */
    private void reload() {
        startRefreshing();
        fetchActiveProjects();
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
        // searchBar.setText(""); // אם תרצי לאפס גם חיפוש
        recompute();
        renderActiveFilterChips();
    }

    private String getCurrentAppraiserIdString() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getUid() != null) return user.getUid();
        return null;
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
        if (chipsActiveFilters == null) return; // אין Container ב-XML – דלג
        chipsActiveFilters.removeAllViews();

        // 1) חיפוש
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

        // 2) סטטוסים
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

        // 3) טווח תאריכים
        Long from = currentFilter.getDateFromEpochMillis();
        Long to   = currentFilter.getDateToEpochMillis();
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

        // 4) שדה תאריך (אם לא ברירת מחדל)
        if (currentFilter.getDateField() == ProjectFilter.DateField.CREATED) {
            chipsActiveFilters.addView(
                    FilterChipUtils.makeEntryChip(this, "שדה: יצירה", () -> {
                        currentFilter.setDateField(ProjectFilter.DateField.LAST_UPDATE);
                        FilterPrefs.save(this, currentFilter);
                        recompute();
                    })
            );
        }

        // 5) מיון (אם שונה מברירת מחדל: LAST_UPDATE + DESC)
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
}
