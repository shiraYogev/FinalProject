package com.example.finalprojectappraisal.activity.myProjects.filter.ui;

import android.widget.EditText;

import com.example.finalprojectappraisal.activity.myProjects.filter.ProjectFilter;
import com.example.finalprojectappraisal.utils.FilterChipUtils;
import com.example.finalprojectappraisal.utils.FilterPrefs;
import com.example.finalprojectappraisal.utils.StatusMapper;
import com.google.android.material.chip.ChipGroup;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** אחראי אך ורק על רינדור צ'יפים לפילטרים. */
public final class FilterChipsController {
    private FilterChipsController(){}

    public static void render(Context ctx,
                              ChipGroup chips,
                              EditText searchBar,
                              ProjectFilter currentFilter,
                              Runnable onFilterChanged) {

        if (chips == null) return;
        chips.removeAllViews();

        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        String searchText = (searchBar != null) ? searchBar.getText().toString().trim() : "";
        if (!searchText.isEmpty()) {
            chips.addView(
                    FilterChipUtils.makeEntryChip(ctx, "חיפוש: " + searchText, () -> {
                        if (searchBar != null) searchBar.setText("");
                        currentFilter.setTextQuery(null);
                        FilterPrefs.save(ctx, currentFilter);
                        onFilterChanged.run();
                    })
            );
        }

        for (String code : currentFilter.getStatuses()) {
            String label = StatusMapper.codeToUiLabel(code);
            chips.addView(
                    FilterChipUtils.makeEntryChip(ctx, "סטטוס: " + label, () -> {
                        currentFilter.getStatuses().remove(code);
                        FilterPrefs.save(ctx, currentFilter);
                        onFilterChanged.run();
                    })
            );
        }

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
            chips.addView(
                    FilterChipUtils.makeEntryChip(ctx, txt, () -> {
                        currentFilter.setDateFromEpochMillis(null);
                        currentFilter.setDateToEpochMillis(null);
                        FilterPrefs.save(ctx, currentFilter);
                        onFilterChanged.run();
                    })
            );
        }

        if (currentFilter.getDateField() == ProjectFilter.DateField.CREATED) {
            chips.addView(
                    FilterChipUtils.makeEntryChip(ctx, "שדה: יצירה", () -> {
                        currentFilter.setDateField(ProjectFilter.DateField.LAST_UPDATE);
                        FilterPrefs.save(ctx, currentFilter);
                        onFilterChanged.run();
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
            chips.addView(
                    FilterChipUtils.makeEntryChip(ctx, "מיון: " + sortLabel + " " + arrow, () -> {
                        currentFilter.setSortBy(ProjectFilter.SortField.LAST_UPDATE);
                        currentFilter.setSortDir(ProjectFilter.SortDir.DESC);
                        FilterPrefs.save(ctx, currentFilter);
                        onFilterChanged.run();
                    })
            );
        }
    }
}
