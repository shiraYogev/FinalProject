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
 * - Kitchen cabinets: combine all images into a single layout (upper/lower) + worktop.
 * - Bars in bedrooms/living rooms: "אין" / "יש" / "יש באופן חלקי".
 * - Interior doors (bedrooms): prefer any real value over "טרם הותקנו".
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
        aggregateInteriorDoors(images, updates);

        // אפשר להרחיב פה בעתיד לעוד שדות
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
                case BEDROOM: {
                    // Description looks like:
                    // "זוהה: חלונות: ..., ... , מיזוג אוויר: כן, ..."
                    Boolean ac = parseYesNoFromDescription(
                            img.getDescription(),
                            "מיזוג אוויר"
                    );
                    if (ac == null) break;
                    if (ac) countYes++;
                    else    countNo++;
                    break;
                }
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
                case BEDROOM: {
                    Boolean bars = parseYesNoFromDescription(
                            img.getDescription(),
                            "סורגים"
                    );
                    if (bars == null) break;
                    if (bars) countYes++;
                    else      countNo++;
                    break;
                }
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
    //    (no material type, only layout: עליונים/תחתונים/אין)
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
                    // אפשר להרחיב לוגיקה אם תרצי עדיפויות לסוגים מסוימים
                }
            }
        }

        // build final "apartment_kitchen" string like GeminiJsonParser does
        String cabinetsStr = buildCabinetsString(flags);
        String kitchenCondition = null;

        if (cabinetsStr != null) {
            // לדוגמה: "ארונות: עליונים ותחתונים"
            kitchenCondition = "ארונות: " + cabinetsStr;
        }
        if (bestWorktop != null) {
            // מוסיפים "משטח: X"
            kitchenCondition = (kitchenCondition == null ? "" : kitchenCondition + ", ")
                    + "משטח: " + bestWorktop;
        }

        if (kitchenCondition != null && !kitchenCondition.isEmpty()) {
            out.put(GeminiJsonParser.FirestoreKeys.KITCHEN_CONDITION, kitchenCondition);
        }
    }

    // ---------------------------------------------------------
    // Helpers for cabinets aggregation (layout only)
    // ---------------------------------------------------------

    private static class CabinetsFlags {
        boolean hasUpper;
        boolean hasLower;
    }

    /**
     * Accumulate cabinets info from a single image string.
     * With the new KITCHEN_PROMPT we expect:
     * - "אין ארונות"
     * - "עליונים בלבד"
     * - "תחתונים בלבד"
     * - "עליונים ותחתונים"
     */
    private static CabinetsFlags accumulateCabinets(CabinetsFlags acc, String cabinetsVal) {
        if (cabinetsVal == null || cabinetsVal.trim().isEmpty()) return acc;

        String v = cabinetsVal.trim();
        if (acc == null) acc = new CabinetsFlags();

        switch (v) {
            case "אין ארונות":
                // no contribution to upper/lower
                break;
            case "עליונים בלבד":
                acc.hasUpper = true;
                break;
            case "תחתונים בלבד":
                acc.hasLower = true;
                break;
            case "עליונים ותחתונים":
                acc.hasUpper = true;
                acc.hasLower = true;
                break;
            default:
                // unexpected string – ignore
                break;
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
        if (flags.hasUpper && flags.hasLower) {
            return "עליונים ותחתונים";
        }
        if (flags.hasUpper) {
            return "עליונים בלבד";
        }
        return "תחתונים בלבד";
    }

    // =========================================================
    // 4) Interior doors – take any non-"טרם הותקנו" if exists
    //    based mainly on BEDROOM images
    // =========================================================

    private static void aggregateInteriorDoors(@NonNull List<Image> images,
                                               @NonNull Map<String, Object> out) {
        String bestDoor = null;          // first seen value (could be "טרם הותקנו")
        String bestNonPending = null;    // first value that is NOT "טרם הותקנו"

        for (Image img : images) {
            if (img == null || img.getCategory() == null) continue;

            // כרגע דלתות פנים מסווגות בחדרי שינה
            if (img.getCategory() != Image.Category.BEDROOM) continue;

            String doorVal = null;

            // 1) קודם מנסה מה-aiClassifications (Subcategory.DOOR)
            Map<Image.Subcategory, String> ai = img.getAiClassifications();
            if (ai != null && !ai.isEmpty()) {
                String v = ai.get(Image.Subcategory.DOOR);
                if (v != null && !v.trim().isEmpty()) {
                    doorVal = v.trim();
                }
            }

            // 2) אם לא מצאנו ב-aiClassifications – ננסה מה-description ("דלתות פנים: X")
            if (doorVal == null || doorVal.isEmpty()) {
                String fromDesc = extractValueFromDescription(img.getDescription(), "דלתות פנים");
                if (fromDesc != null && !fromDesc.trim().isEmpty()) {
                    doorVal = fromDesc.trim();
                }
            }

            if (doorVal == null || doorVal.isEmpty()) continue;

            // זוכרים את הערך הראשון שנתקלנו בו (למקרה שכולם "טרם הותקנו")
            if (bestDoor == null) {
                bestDoor = doorVal;
            }

            // אם זה משהו אמיתי ולא "טרם הותקנו" – נשמור כעדיפות
            if (!"טרם הותקנו".equals(doorVal)) {
                if (bestNonPending == null) {
                    bestNonPending = doorVal;
                }
            }
        }

        String finalVal = null;
        if (bestNonPending != null) {
            finalVal = bestNonPending;
        } else if (bestDoor != null) {
            finalVal = bestDoor; // כנראה "טרם הותקנו" או ערך יחיד
        }

        if (finalVal != null) {
            out.put(GeminiJsonParser.FirestoreKeys.INTERIOR_DOOR_CONDITION, finalVal);
        }
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
