// file: app/src/main/java/com/example/finalprojectappraisal/utils/RepresentativePicker.java
package com.example.finalprojectappraisal.utils;

import android.net.Uri;

import com.example.finalprojectappraisal.model.Image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Picks up to N representative image URIs (one per category if possible),
 * using only fields that exist in your Image model: url, uploadDate (yyyy-MM-dd), isVerified, category.
 * Sort: verified (desc) -> uploadDate (desc). Falls back gracefully if fields missing.
 */
public final class RepresentativePicker {
    private RepresentativePicker() {}

    /** Adjust to your enum set/order */
    private static final List<Image.Category> ORDER = Arrays.asList(
            Image.Category.LIVING_ROOM,
            Image.Category.KITCHEN,
            Image.Category.BATHROOM,
            Image.Category.BEDROOM,
            Image.Category.VIEW,          // יש לך VIEW, אז נשאיר
            Image.Category.EXTERIOR,
            Image.Category.ENTRANCE_DOOR
    );

    public static List<Uri> pickUris(List<Image> all, int maxTotal) {
        if (all == null || all.isEmpty() || maxTotal <= 0) return Collections.emptyList();

        // Group by category
        Map<Image.Category, List<Image>> byCat = new EnumMap<>(Image.Category.class);
        for (Image img : all) {
            if (img == null || img.getCategory() == null) continue;
            byCat.computeIfAbsent(img.getCategory(), k -> new ArrayList<>()).add(img);
        }

        List<Uri> result = new ArrayList<>();

        // One representative per preferred category
        for (Image.Category cat : ORDER) {
            List<Image> list = byCat.get(cat);
            if (list == null || list.isEmpty()) continue;

            Image rep = pickBest(list);
            Uri u = toUri(rep);
            if (u != null) {
                result.add(u);
                if (result.size() >= maxTotal) return result;
            }
        }

        // If still not enough, fill from remaining images
        Set<String> seen = new HashSet<>();
        for (Uri u : result) seen.add(u.toString());

        for (Image img : all) {
            Uri u = toUri(img);
            if (u != null && !seen.contains(u.toString())) {
                result.add(u);
                seen.add(u.toString());
                if (result.size() >= maxTotal) break;
            }
        }

        return result;
    }

    /** Sorts by: verified desc -> uploadDate (yyyy-MM-dd) desc */
    private static Image pickBest(List<Image> list) {
        return list.stream()
                .sorted((a, b) -> {
                    int v = Boolean.compare(b.isVerified(), a.isVerified());
                    if (v != 0) return v;
                    // yyyy-MM-dd compares correctly as strings (lexicographic == chronological)
                    String db = safeDate(b.getUploadDate());
                    String da = safeDate(a.getUploadDate());
                    return db.compareTo(da); // desc
                })
                .findFirst()
                .orElse(null);
    }

    private static Uri toUri(Image img) {
        if (img == null) return null;
        String url = img.getUrl();
        if (url == null || url.trim().isEmpty()) return null;
        try {
            return Uri.parse(url.trim()); // works for https:// and gs://
        } catch (Throwable t) {
            return null;
        }
    }

    private static String safeDate(String d) {
        return (d == null || d.trim().isEmpty()) ? "" : d.trim();
    }
}
