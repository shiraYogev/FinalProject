package com.example.finalprojectappraisal.classifer.aggregation;

import androidx.annotation.NonNull;

import com.example.finalprojectappraisal.classifer.gemini.GeminiJsonParser;
import com.example.finalprojectappraisal.model.Image;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates image-level classifications into final project-level fields.
 *
 * Examples:
 * - Living room / bedroom air conditioning: OR over all images ("כן" if any image has AC).
 * - Kitchen cabinets: combine all images into a single layout (upper/lower) + material.
 * - Bars in bedrooms/living rooms: "אין" / "יש" / "יש באופן חלקי".
 *
 * This class does NOT touch Firestore directly; it only returns a Map<String, Object>
 * with Firestore field names (keys from GeminiJsonParser.FirestoreKeys).
 */
public final class ProjectImageAggregator {

    private ProjectImageAggregator() {
        // utility, no instances
    }

    /**
     * Main entry point.
     *
     * @param images all images for the project (any category).
     * @return map of project fields to be updated in Firestore.
     */
    @NonNull
    public static Map<String, Object> buildAggregatedUpdates(@NonNull List<Image> images) {
        Map<String, Object> updates = new HashMap<>();

        aggregateAirConditioning(images, updates);
        aggregateBars(images, updates);
        aggregateKitchen(images, updates);

        // אפשר להוסיף כאן בעתיד עוד aggregator-ים (מעלית, נוף, וכו')
        return updates;
    }

    // =========================================================
    // 1) Air conditioning – OR across living rooms / bedrooms
    // =========================================================

    private static void aggregateAirConditioning(@NonNull List<Image> images,
                                                 @NonNull Map<String, Object> out) {
        int countYes = 0;
        int countNo  = 0;

        for (Image img : images) {
            if (img == null || img.getCategory() == null) continue;

            switch (img.getCategory()) {
                case LIVING_ROOM:
                case BEDROOM:
                    // Description is something like:
                    // "זוהה: חלונות: ..., ... , מיזוג אוויר: כן, ..."
                    Boolean ac = parseYesNoFromDescription(
                            img.getDescription(),
                            "מיזוג אוויר"
                    );
                    if (ac == null) break;
                    if (ac) countYes++;
                    else   countNo++;
                    break;
                default:
                    break;
            }
        }

        String finalAc = null;
        if (countYes > 0) {
            finalAc = "כן";
        } else if (countNo > 0) {
            finalAc = "לא";
        }

        if (finalAc != null) {
            out.put(GeminiJsonParser.FirestoreKeys.HAS_AIR_CONDITIONING, finalAc);
        }
    }

    // =========================================================
    // 2) Bars – "אין" / "יש" / "יש באופן חלקי"
    //    across living rooms + bedrooms
    // =========================================================

    private static void aggregateBars(@NonNull List<Image> images,
                                      @NonNull Map<String, Object> out) {
        int countYes = 0;
        int countNo  = 0;

        for (Image img : images) {
            if (img == null || img.getCategory() == null) continue;

            switch (img.getCategory()) {
                case LIVING_ROOM:
                case BEDROOM:
                    Boolean bars = parseYesNoFromDescription(
                            img.getDescription(),
                            "סורגים"
                    );
                    if (bars == null) break;
                    if (bars) countYes++;
                    else      countNo++;
                    break;
                default:
                    break;
            }
        }

        if (countYes == 0 && countNo == 0) {
            // No info at all – don't override existing project value
            return;
        }

        String finalBars;
        if (countYes > 0 && countNo > 0) {
            finalBars = "יש באופן חלקי";
        } else if (countYes > 0) {
            finalBars = "יש";
        } else {
            finalBars = "אין";
        }

        out.put(GeminiJsonParser.FirestoreKeys.HAS_BARS, finalBars);
    }

    // =========================================================
    // 3) Kitchen – cabinets + worktop aggregation
    //    using aiClassifications: CABINETS, WORKTOP
    // =========================================================

    private static void aggregateKitchen(@NonNull List<Image> images,
                                         @NonNull Map<String, Object> out) {
        CabinetsFlags flags = null;
        String bestWorktop = null;

        for (Image img : images) {
            if (img == null || img.getCategory() == null) continue;
            if (img.getCategory() != Image.Category.KITCHEN) continue;

            Map<Image.Subcategory, String> ai = img.getAiClassifications();
            if (ai == null || ai.isEmpty()) continue;

            // ---- Cabinets aggregation ----
            String cabinetsVal = ai.get(Image.Subcategory.CABINETS);
            if (cabinetsVal != null && !cabinetsVal.trim().isEmpty()) {
                flags = accumulateCabinets(flags, cabinetsVal.trim());
            }

            // ---- Worktop aggregation (simple first-non-null) ----
            String worktopVal = ai.get(Image.Subcategory.WORKTOP);
            if (worktopVal != null) {
                String w = worktopVal.trim();
                if (!w.isEmpty() && !"null".equalsIgnoreCase(w)) {
                    if (bestWorktop == null) {
                        bestWorktop = w;
                    }
                    // אם תרצי בעתיד לוגיקה יותר חכמה (עדיפות לסוג מסוים) – אפשר להרחיב פה
                }
            }
        }

        // Build final kitchenCondition (apartment_kitchen) string as in GeminiJsonParser
        String cabinetsStr = buildCabinetsString(flags);
        String kitchenCondition = null;

        if (cabinetsStr != null) {
            // cabinetsStr is either "אין ארונות" or "<LAYOUT> | <MATERIAL>"
            kitchenCondition = "ארונות: " + cabinetsStr;
        }
        if (bestWorktop != null) {
            kitchenCondition = (kitchenCondition == null ? "" : kitchenCondition + ", ")
                    + "משטח: " + bestWorktop;
        }

        if (kitchenCondition != null && !kitchenCondition.isEmpty()) {
            out.put(GeminiJsonParser.FirestoreKeys.KITCHEN_CONDITION, kitchenCondition);
        }
    }

    // ---------------------------------------------------------
    // Helpers for cabinets aggregation
    // ---------------------------------------------------------

    private static class CabinetsFlags {
        boolean hasUpper;
        boolean hasLower;
        String material; // first non-"לא ידוע" material
    }

    /**
     * Accumulate cabinets info from a single image string.
     * Format is defined in KITCHEN_PROMPT:
     * - "אין ארונות"
     * - "<LAYOUT> | <MATERIAL>"
     *   where LAYOUT ∈ {"עליונים בלבד","תחתונים בלבד","עליונים ותחתונים"}
     */
    private static CabinetsFlags accumulateCabinets(CabinetsFlags acc, String cabinetsVal) {
        if (cabinetsVal == null || cabinetsVal.trim().isEmpty()) return acc;

        String v = cabinetsVal.trim();
        if ("אין ארונות".equals(v)) {
            // No upper/lower; no positive contribution
            if (acc == null) acc = new CabinetsFlags();
            return acc;
        }

        String[] parts = v.split("\\|");
        String layout = parts[0].trim();
        String material = (parts.length > 1) ? parts[1].trim() : null;

        boolean upper = false;
        boolean lower = false;

        switch (layout) {
            case "עליונים בלבד":
                upper = true;
                break;
            case "תחתונים בלבד":
                lower = true;
                break;
            case "עליונים ותחתונים":
                upper = true;
                lower = true;
                break;
            default:
                // Unexpected layout – ignore layout, keep only material if needed
                break;
        }

        if (acc == null) acc = new CabinetsFlags();
        acc.hasUpper = acc.hasUpper || upper;
        acc.hasLower = acc.hasLower || lower;

        if (material != null && !material.isEmpty()) {
            if (acc.material == null || "לא ידוע".equals(acc.material)) {
                acc.material = material;
            }
        }

        return acc;
    }

    /**
     * Convert accumulated flags to a single cabinets string.
     */
    private static String buildCabinetsString(CabinetsFlags flags) {
        if (flags == null) return null;

        if (!flags.hasUpper && !flags.hasLower) {
            return "אין ארונות";
        }

        String layout;
        if (flags.hasUpper && flags.hasLower) {
            layout = "עליונים ותחתונים";
        } else if (flags.hasUpper) {
            layout = "עליונים בלבד";
        } else {
            layout = "תחתונים בלבד";
        }

        if (flags.material == null || flags.material.isEmpty()) {
            return layout + " | לא ידוע";
        }
        return layout + " | " + flags.material;
    }

    // ---------------------------------------------------------
    // Generic helpers for parsing description text
    // ---------------------------------------------------------

    /**
     * Parses a yes/no field from description built by GeminiJsonParser.buildDescription, e.g.:
     * "זוהה: ... , מיזוג אוויר: כן, סורגים: לא, ..."
     *
     * @param description full description string
     * @param label       Hebrew label, e.g. "מיזוג אוויר" or "סורגים"
     * @return Boolean.TRUE / Boolean.FALSE / null if not found or unparseable.
     */
    private static Boolean parseYesNoFromDescription(String description, String label) {
        String value = extractValueFromDescription(description, label);
        if (value == null) return null;

        // Be tolerant: "כן", "יש", "לא", "אין"
        if (value.startsWith("כן") || value.startsWith("יש")) {
            return Boolean.TRUE;
        }
        if (value.startsWith("לא") || value.startsWith("אין")) {
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * Extracts the value after "label:" until the next comma or end of string.
     * Example: desc = "זוהה: חלונות: X, מיזוג אוויר: כן, סורגים: לא"
     * extractValueFromDescription(desc, "מיזוג אוויר") -> "כן"
     */
    private static String extractValueFromDescription(String description, String label) {
        if (description == null || label == null) return null;

        String needle = label + ":";
        int idx = description.indexOf(needle);
        if (idx < 0) return null;

        int start = idx + needle.length();

        // skip whitespace
        while (start < description.length()
                && Character.isWhitespace(description.charAt(start))) {
            start++;
        }

        int end = description.indexOf(',', start);
        if (end < 0) end = description.length();

        String raw = description.substring(start, end).trim();
        return raw.isEmpty() ? null : raw;
    }
}
