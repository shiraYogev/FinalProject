package com.example.finalprojectappraisal.activity.myProjects.filter;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
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

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class FiltersBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final String TAG = "FiltersBS";

    public interface OnFiltersAppliedListener {
        void onApply(ProjectFilter filter);
        void onReset();
    }

    private final ProjectFilter filter;
    private final OnFiltersAppliedListener listener;

    // new inputs
    private EditText etCity, etStreet, etHouse, etGush, etParcel;

    public FiltersBottomSheetDialogFragment(@NonNull ProjectFilter seed,
                                            @NonNull OnFiltersAppliedListener listener) {
        this.filter = (seed == null) ? new ProjectFilter() : seed;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottom_sheet_filters, container, false);

        // ---- Address inputs ----
        etCity   = v.findViewById(R.id.et_city);
        etStreet = v.findViewById(R.id.et_street);
        etHouse  = v.findViewById(R.id.et_house);
        etGush   = v.findViewById(R.id.et_gush);
        etParcel = v.findViewById(R.id.et_parcel);

        if (etCity   != null) etCity.setText(safe(filter.getCity()));
        if (etStreet != null) etStreet.setText(safe(filter.getStreet()));
        if (etHouse  != null) etHouse.setText(safe(filter.getHouseNumber()));
        if (etGush   != null) etGush.setText(safe(filter.getGush()));
        if (etParcel != null) etParcel.setText(safe(filter.getParcel()));

        // --- Status chips ---
        setupStatusChips(v);

        // --- Date field + date range ---
        setupDateSection(v);

        // --- Sort section ---
        setupSortSection(v);

        // --- Buttons ---
        Button btnApply = v.findViewById(R.id.btn_apply);
        Button btnReset = v.findViewById(R.id.btn_reset);

        btnApply.setOnClickListener(view -> {
            // read current text inputs into filter
            filter.setCity(text(etCity));
            filter.setStreet(text(etStreet));
            filter.setHouseNumber(text(etHouse));
            filter.setGush(text(etGush));
            filter.setParcel(text(etParcel));

            // rebuild statuses from UI (safety)
            ChipGroup group = v.findViewById(R.id.chips_status);
            Set<String> rebuilt = rebuildStatusesFromUi(group);
            filter.getStatuses().clear();
            filter.getStatuses().addAll(rebuilt);

            Log.d(TAG, "APPLY: " + summarizeFilter(filter));
            FilterPrefs.save(requireContext(), filter);
            if (listener != null) listener.onApply(filter);
            dismiss();
        });

        btnReset.setOnClickListener(view -> {
            Log.d(TAG, "RESET (before): " + summarizeFilter(filter));
            filter.clear();
            Log.d(TAG, "RESET (after): " + summarizeFilter(filter));
            FilterPrefs.save(requireContext(), filter);
            if (listener != null) listener.onReset();
            dismiss();
        });

        Log.d(TAG, "OPENED with: " + summarizeFilter(filter));
        return v;
    }

    // ---------- Status chips ----------
    private void setupStatusChips(View root) {
        ChipGroup group = root.findViewById(R.id.chips_status);
        if (group == null) return;

        group.setSingleSelection(false);
        group.setSelectionRequired(false);

        String[] labels = getResources().getStringArray(R.array.project_statuses);
        if (labels == null) labels = new String[0];
        Log.d(TAG, "setupStatusChips: labels=" + Arrays.toString(labels));

        group.removeAllViews();

        for (String label : labels) {
            final String code = StatusMapper.uiLabelToCode(label);

            Chip chip = new Chip(requireContext(), null,
                    com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter);
            chip.setText(label);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setEnsureMinTouchTargetSize(true);

            chip.setChecked(filter.getStatuses().contains(code));

            chip.setOnCheckedChangeListener((button, isChecked) -> {
                if (isChecked) filter.getStatuses().add(code);
                else           filter.getStatuses().remove(code);
                Log.d(TAG, "chip: \"" + label + "\" code=" + code + " -> " + isChecked
                        + " | statuses=" + filter.getStatuses());
            });
            group.addView(chip);
        }

        Log.d(TAG, "setupStatusChips: initial statuses=" + filter.getStatuses());
    }

    private Set<String> rebuildStatusesFromUi(@Nullable ChipGroup group) {
        Set<String> out = new HashSet<>();
        if (group == null) return out;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip) {
                Chip c = (Chip) child;
                if (c.isChecked()) {
                    String label = String.valueOf(c.getText());
                    String code = StatusMapper.uiLabelToCode(label);
                    if (code != null && !code.trim().isEmpty()) out.add(code);
                }
            }
        }
        Log.d(TAG, "rebuildStatusesFromUi: " + out);
        return out;
    }

    // ---------- Dates ----------
    private void setupDateSection(View v) {
        RadioGroup dateFieldGroup = v.findViewById(R.id.radio_date_field);
        if (filter.getDateField() == ProjectFilter.DateField.CREATED) {
            dateFieldGroup.check(R.id.date_created);
        } else {
            dateFieldGroup.check(R.id.date_last_update);
        }
        dateFieldGroup.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.date_created) filter.setDateField(ProjectFilter.DateField.CREATED);
            else                         filter.setDateField(ProjectFilter.DateField.LAST_UPDATE);
        });

        TextView tvFrom = v.findViewById(R.id.tv_date_from);
        TextView tvTo   = v.findViewById(R.id.tv_date_to);
        bindDateLabels(tvFrom, tvTo);

        tvFrom.setOnClickListener(view -> pickDate(true, tvFrom));
        tvTo.setOnClickListener(view -> pickDate(false, tvTo));
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
            if (isFrom) filter.setDateFromEpochMillis(epoch);
            else        filter.setDateToEpochMillis(epoch + 86_399_000L);
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

    // ---------- Sort ----------
    private void setupSortSection(View v) {
        RadioGroup sortGroup = v.findViewById(R.id.radio_sort);
        switch (filter.getSortBy()) {
            case CREATED:      sortGroup.check(R.id.sort_created); break;
            case CITY:         sortGroup.check(R.id.sort_city);    break;
            case LAST_UPDATE:
            default:           sortGroup.check(R.id.sort_last_update);
        }
        sortGroup.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.sort_created) filter.setSortBy(ProjectFilter.SortField.CREATED);
            else if (id == R.id.sort_city) filter.setSortBy(ProjectFilter.SortField.CITY);
            else                            filter.setSortBy(ProjectFilter.SortField.LAST_UPDATE);
        });

        SwitchMaterial swDesc = v.findViewById(R.id.switch_sort_desc);
        swDesc.setChecked(filter.getSortDir() == ProjectFilter.SortDir.DESC);
        swDesc.setOnCheckedChangeListener((compoundButton, checked) ->
                filter.setSortDir(checked ? ProjectFilter.SortDir.DESC : ProjectFilter.SortDir.ASC)
        );
    }

    // ---------- Utils ----------
    private String text(@Nullable EditText et) {
        return et == null ? null : et.getText().toString().trim();
    }
    private String safe(@Nullable String s) { return s == null ? "" : s; }
    private long safe(@Nullable Long v) { return v == null ? 0L : v; }

    private String summarizeFilter(ProjectFilter f) {
        return "{q=" + f.getTextQuery()
                + ", city=" + f.getCity()
                + ", street=" + f.getStreet()
                + ", house=" + f.getHouseNumber()
                + ", gush=" + f.getGush()
                + ", parcel=" + f.getParcel()
                + ", statuses=" + f.getStatuses()
                + ", dateField=" + f.getDateField()
                + ", from=" + f.getDateFromEpochMillis()
                + ", to=" + f.getDateToEpochMillis()
                + ", sortBy=" + f.getSortBy()
                + ", sortDir=" + f.getSortDir()
                + "}";
    }
}
