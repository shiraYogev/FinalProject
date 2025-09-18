package com.example.finalprojectappraisal.activity.myProjects.filter;

import androidx.annotation.NonNull;

import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.utils.GeoUtils;
import com.example.finalprojectappraisal.utils.TextNormalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

            // 1) Status (OR בתוך הסט). משווה לפי projectStatus כמחרוזת (case-insensitive).
            if (!statuses.isEmpty()) {
                String st = statusName(p); // UPPERCASE
                if (st == null || !statuses.contains(st)) continue;
            }

            // 2) Date range על השדה שנבחר
            Long from = f.getDateFromEpochMillis();
            Long to   = f.getDateToEpochMillis();
            long candidate = (f.getDateField() == ProjectFilter.DateField.LAST_UPDATE)
                    ? safeLong(p.getLastUpdateDateMillis())
                    : safeLong(p.getCreationDate());
            if (from != null && candidate < from) continue;
            if (to   != null && candidate > to)   continue;

            // 3) Location radius (km) — למימוש עתידי אם יש lat/lng במודל.
            // אם משתמש/ת סיפקה center+radius אבל למודל אין קואורדינטות, הפרויקט ייפסל.
            if (f.getCenterLat() != null && f.getCenterLng() != null && f.getRadiusKm() != null) {
                // אין במודל שדות קואורדינטות כרגע; משאירים כ-null כדי לא לעבור פילטר מיקום בטעות.
                // אם תוסיפי getLatitude()/getLongitude() בהמשך — החליפי כאן.
                Double lat = null;
                Double lng = null;
                if (lat == null || lng == null) continue;
                double d = GeoUtils.distanceKm(f.getCenterLat(), f.getCenterLng(), lat, lng);
                if (d > f.getRadiusKm()) continue;
            }

            // 4) Free-text — אך ורק על fullAddress לפי הבקשה
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
                // אין שדה עיר — נמיין לפי כתובת (A→Z)
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

    /** Reads projectStatus and returns UPPERCASE to match f.statuses entries. */
    private static String statusName(Project p) {
        if (p == null) return null;
        String s = p.getProjectStatus();
        if (s == null) return null;
        return s.toUpperCase(Locale.ROOT).trim();
    }
}