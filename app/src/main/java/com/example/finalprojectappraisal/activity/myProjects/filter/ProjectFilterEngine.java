// file: app/src/main/java/com/example/finalprojectappraisal/activity/myProjects/filter/ProjectFilterEngine.java
package com.example.finalprojectappraisal.activity.myProjects.filter;

import androidx.annotation.NonNull;

import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.utils.GeoUtils;
import com.example.finalprojectappraisal.utils.StatusMapper;
import com.example.finalprojectappraisal.utils.TextNormalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class ProjectFilterEngine {

    /** Apply all filters client-side and return a new filtered + sorted list. */
    @NonNull
    public static List<Project> apply(@NonNull List<Project> base, @NonNull ProjectFilter f) {
        List<Project> out = new ArrayList<>(base.size());

        final String q = TextNormalizer.normalizeOrNull(f.getTextQuery());
        final Set<String> statuses = f.getStatuses();

        for (Project p : base) {
            if (p == null) continue;

            // 1) Status (OR inside the set). Compare using canonical Hebrew code.
            if (!statuses.isEmpty()) {
                String st = statusName(p); // canonical Hebrew
                if (st == null || !statuses.contains(st)) continue;
            }

            // 2) Date range
            Long from = f.getDateFromEpochMillis();
            Long to   = f.getDateToEpochMillis();
            long candidate = (f.getDateField() == ProjectFilter.DateField.LAST_UPDATE)
                    ? safeLong(p.getLastUpdateDateMillis())
                    : safeLong(p.getCreationDate());
            if (from != null && candidate < from) continue;
            if (to   != null && candidate > to)   continue;

            // 3) Location (future – currently skipped if no coords)
            if (f.getCenterLat() != null && f.getCenterLng() != null && f.getRadiusKm() != null) {
                Double lat = null;
                Double lng = null;
                if (lat == null || lng == null) continue;
                double d = GeoUtils.distanceKm(f.getCenterLat(), f.getCenterLng(), lat, lng);
                if (d > f.getRadiusKm()) continue;
            }

            // 4) Free-text on fullAddress only (normalized)
            if (q != null && !q.isEmpty()) {
                String addr = TextNormalizer.normalizeOrEmpty(p.getFullAddress());
                if (!addr.contains(q)) continue;
            }

            out.add(p);
        }

        // 5) Sorting
        Comparator<Project> cmp;
        switch (f.getSortBy()) {
            case CREATED:
                cmp = (a, b) -> Long.compare(safeLong(a.getCreationDate()), safeLong(b.getCreationDate()));
                break;
            case CITY:
                cmp = (a, b) -> safeStr(a.getFullAddress()).compareToIgnoreCase(safeStr(b.getFullAddress()));
                break;
            case LAST_UPDATE:
            default:
                cmp = (a, b) -> Long.compare(safeLong(a.getLastUpdateDateMillis()), safeLong(b.getLastUpdateDateMillis()));
        }
        Collections.sort(out, cmp);
        if (f.getSortDir() == ProjectFilter.SortDir.DESC) Collections.reverse(out);

        return out;
    }

    // ------- Helpers -------
    private static long safeLong(Long v) { return v == null ? 0L : v; }
    private static String safeStr(String s) { return s == null ? "" : s; }

    /** Reads projectStatus and returns a canonical Hebrew code via StatusMapper. */
    private static String statusName(Project p) {
        if (p == null) return null;
        String s = p.getProjectStatus();
        if (s == null) return null;
        return StatusMapper.canonicalCode(s);
    }
}
