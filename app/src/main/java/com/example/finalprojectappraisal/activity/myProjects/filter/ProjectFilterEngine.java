package com.example.finalprojectappraisal.activity.myProjects.filter;

import androidx.annotation.NonNull;

import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.utils.GeoUtils;
import com.example.finalprojectappraisal.utils.StatusMapper;
import com.example.finalprojectappraisal.utils.TextNormalizer;

import java.lang.reflect.Method;
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

        final String q          = TextNormalizer.normalizeOrNull(f.getTextQuery());
        final String cityQ      = TextNormalizer.normalizeOrNull(f.getCity());
        final String streetQ    = TextNormalizer.normalizeOrNull(f.getStreet());
        final String houseQ     = TextNormalizer.normalizeOrNull(f.getHouseNumber());
        final String gushQ      = TextNormalizer.normalizeOrNull(f.getGush());
        final String parcelQ    = TextNormalizer.normalizeOrNull(f.getParcel());
        final Set<String> statuses = f.getStatuses();

        for (Project p : base) {
            if (p == null) continue;

            // 1) Status (OR בתוך הסט) — קוד קנוני בעברית
            if (!statuses.isEmpty()) {
                String st = statusName(p);
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

            // 3) Location radius (future)
            if (f.getCenterLat() != null && f.getCenterLng() != null && f.getRadiusKm() != null) {
                Double lat = null, lng = null;
                if (lat == null || lng == null) continue;
                double d = GeoUtils.distanceKm(f.getCenterLat(), f.getCenterLng(), lat, lng);
                if (d > f.getRadiusKm()) continue;
            }

            // 4) Free-text on full address
            if (q != null && !q.isEmpty()) {
                String addr = TextNormalizer.normalizeOrEmpty(p.getFullAddress());
                if (!addr.contains(q)) continue;
            }

            // 5) Address fields
            if (cityQ != null) {
                String src = normalize(firstNonEmpty(
                        read(p, "getCity"),
                        read(p, "getAddressCity"),
                        p.getFullAddress()
                ));
                if (src == null || !src.contains(cityQ)) continue;
            }

            if (streetQ != null) {
                String src = normalize(firstNonEmpty(
                        read(p, "getStreet"),
                        read(p, "getAddressStreet"),
                        read(p, "getStreetName"),
                        read(p, "getBuildingEntry"),
                        p.getFullAddress()
                ));
                if (src == null || !src.contains(streetQ)) continue;
            }

            if (houseQ != null) {
                String src = normalize(firstNonEmpty(
                        read(p, "getBuildingNumber"),
                        read(p, "getHouseNumber"),
                        p.getFullAddress()
                ));
                if (src == null || !src.contains(houseQ)) continue;
            }

            // 6) Cadastral: gush / parcel
            if (gushQ != null) {
                String src = normalize(firstNonEmpty(
                        read(p, "getMainParcel"),
                        read(p, "getGush")
                ));
                if (src == null || !src.contains(gushQ)) continue;
            }

            if (parcelQ != null) {
                String src = normalize(firstNonEmpty(
                        read(p, "getLotNumber"),
                        read(p, "getSubParcel"),
                        read(p, "getParcel")
                ));
                if (src == null || !src.contains(parcelQ)) continue;
            }

            out.add(p);
        }

        // 7) Sorting
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

    /** Canonical Hebrew status for comparison. */
    private static String statusName(Project p) {
        if (p == null) return null;
        String s = p.getProjectStatus();
        if (s == null) return null;
        return StatusMapper.canonicalCode(s);
    }

    private static String normalize(String s) { return TextNormalizer.normalizeOrNull(s); }

    private static String firstNonEmpty(String... arr) {
        if (arr == null) return null;
        for (String s : arr) if (s != null && !s.trim().isEmpty()) return s;
        return null;
    }

    /**
     * Reflection helper:
     * read(p, "getBankDetails", "getMainParcel") → calls p.getBankDetails().getMainParcel()
     * returns String.valueOf(value) or null on any error.
     */
    private static String read(Object root, String... getters) {
        if (root == null || getters == null || getters.length == 0) return null;
        Object obj = root;
        try {
            for (String g : getters) {
                if (obj == null) return null;
                Method m = obj.getClass().getMethod(g);
                obj = m.invoke(obj);
            }
            return (obj == null) ? null : String.valueOf(obj);
        } catch (Throwable t) {
            return null;
        }
    }
}
