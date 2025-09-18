package com.example.finalprojectappraisal.activity.myProjects.filter;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.utils.FilterPrefs;
import com.example.finalprojectappraisal.utils.StatusMapper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Calendar;

public class FiltersBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public interface OnFiltersAppliedListener {
        void onApply(ProjectFilter filter);
        void onReset();
    }

    private final ProjectFilter filter;
    private final OnFiltersAppliedListener listener;

    public FiltersBottomSheetDialogFragment(@NonNull ProjectFilter seed,
                                            @NonNull OnFiltersAppliedListener listener) {
        this.filter = seed == null ? new ProjectFilter() : seed;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottom_sheet_filters, container, false);

        // --- Status chips from resources (dynamic) ---
        setupStatusChips(v);

        // --- Date field selection ---
        RadioGroup dateFieldGroup = v.findViewById(R.id.radio_date_field);
        if (filter.getDateField() == ProjectFilter.DateField.CREATED) {
            dateFieldGroup.check(R.id.date_created);
        } else {
            dateFieldGroup.check(R.id.date_last_update);
        }
        dateFieldGroup.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.date_created) {
                filter.setDateField(ProjectFilter.DateField.CREATED);
            } else {
                filter.setDateField(ProjectFilter.DateField.LAST_UPDATE);
            }
        });

        // --- Date range pickers ---
        TextView tvFrom = v.findViewById(R.id.tv_date_from);
        TextView tvTo   = v.findViewById(R.id.tv_date_to);
        bindDateLabels(tvFrom, tvTo);

        tvFrom.setOnClickListener(view -> pickDate(true, tvFrom));
        tvTo.setOnClickListener(view -> pickDate(false, tvTo));

        // --- Sort field ---
        RadioGroup sortGroup = v.findViewById(R.id.radio_sort);
        switch (filter.getSortBy()) {
            case CREATED:      sortGroup.check(R.id.sort_created); break;
            case CITY:         sortGroup.check(R.id.sort_city);    break; // CITY = לפי כתובת
            case LAST_UPDATE:
            default:           sortGroup.check(R.id.sort_last_update);
        }
        sortGroup.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.sort_created) filter.setSortBy(ProjectFilter.SortField.CREATED);
            else if (id == R.id.sort_city) filter.setSortBy(ProjectFilter.SortField.CITY);
            else filter.setSortBy(ProjectFilter.SortField.LAST_UPDATE);
        });

        // --- Sort direction (ASC/DESC) ---
        SwitchMaterial swDesc = v.findViewById(R.id.switch_sort_desc);
        swDesc.setChecked(filter.getSortDir() == ProjectFilter.SortDir.DESC);
        swDesc.setOnCheckedChangeListener((compoundButton, checked) ->
                filter.setSortDir(checked ? ProjectFilter.SortDir.DESC : ProjectFilter.SortDir.ASC)
        );

        // --- Buttons ---
        Button btnApply = v.findViewById(R.id.btn_apply);
        Button btnReset = v.findViewById(R.id.btn_reset);
        btnApply.setOnClickListener(view -> {
            FilterPrefs.save(requireContext(), filter);
            if (listener != null) listener.onApply(filter);
            dismiss();
        });
        btnReset.setOnClickListener(view -> {
            filter.clear();
            FilterPrefs.save(requireContext(), filter);
            if (listener != null) listener.onReset();
            dismiss();
        });

        return v;
    }

    /** יצירת צ'יפים מתוך @array/project_statuses + שמירה בקוד סטטוס (UPPERCASE) דרך StatusMapper */
    private void setupStatusChips(View root) {
        ChipGroup group = root.findViewById(R.id.chips_status);
        String[] labels = getResources().getStringArray(R.array.project_statuses);
        if (group == null || labels == null) return;

        group.removeAllViews();

        for (String label : labels) {
            final String code = StatusMapper.uiLabelToCode(label); // למשל "בטיפול" -> "IN PROGRESS"
            Chip chip = new Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter);
            chip.setText(label);
            chip.setCheckable(true);
            chip.setChecked(filter.getStatuses().contains(code));
            chip.setOnCheckedChangeListener((button, isChecked) -> {
                if (isChecked) filter.getStatuses().add(code);
                else filter.getStatuses().remove(code);
            });
            group.addView(chip);
        }
    }

    private void pickDate(boolean isFrom, TextView label) {
        Calendar cal = Calendar.getInstance();
        long preset = isFrom ? safe(filter.getDateFromEpochMillis()) : safe(filter.getDateToEpochMillis());
        if (preset > 0) cal.setTimeInMillis(preset);
        int y = cal.get(Calendar.YEAR), m = cal.get(Calendar.MONTH), d = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(requireContext(), (DatePicker view, int year, int month, int dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth, 0, 0, 0);
            long epoch = c.getTimeInMillis();
            if (isFrom) {
                filter.setDateFromEpochMillis(epoch);
            } else {
                // סוף יום
                filter.setDateToEpochMillis(epoch + 86_399_000L);
            }
            label.setText(DateFormat.format("dd.MM.yyyy", c));
        }, y, m, d);
        dlg.show();
    }

    private void bindDateLabels(TextView from, TextView to) {
        if (filter.getDateFromEpochMillis() != null) {
            from.setText(DateFormat.format("dd.MM.yyyy", filter.getDateFromEpochMillis()));
        }
        if (filter.getDateToEpochMillis() != null) {
            to.setText(DateFormat.format("dd.MM.yyyy", filter.getDateToEpochMillis()));
        }
    }

    private long safe(Long v) { return v == null ? 0L : v; }
}
