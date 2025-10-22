// file: app/src/main/java/com/example/finalprojectappraisal/utils/StatusMapper.java
package com.example.finalprojectappraisal.utils;

import java.util.Locale;

/**
 * Normalizes/bridges status labels between UI, DB and legacy codes.
 * Canonical code is always a Hebrew label (e.g., "בעבודה").
 */
public class StatusMapper {

    /** Returns a canonical Hebrew code for any known label (Hebrew or English). */
    public static String canonicalCode(String input) {
        if (input == null) return "";
        String s = input.trim();
        if (s.isEmpty()) return s;

        // Upper only for matching English tokens; Hebrew unaffected by case.
        String u = s.toUpperCase(Locale.ROOT);

        // ---- canonical Hebrew statuses ----
        // quote
        if (u.equals("QUOTE") || s.equals("הצעת מחיר")) {
            return "הצעת מחיר";
        }
        // before / after visit
        if (u.equals("BEFORE VISIT") || s.equals("טרם ביקור")) {
            return "טרם ביקור";
        }
        if (u.equals("AFTER VISIT") || s.equals("לאחר ביקור")) {
            return "לאחר ביקור";
        }
        // in progress (older Hebrew "בטיפול" maps to "בעבודה")
        if (u.equals("IN PROGRESS") || s.equals("בעבודה") || s.equals("בטיפול")) {
            return "בעבודה";
        }
        // review/signoff
        if (u.equals("REVIEW SIGNOFF") || s.equals("בבדיקה שמאי חותם")) {
            return "בבדיקה שמאי חותם";
        }
        // completed
        if (u.equals("COMPLETED") || s.equals("הושלם")) {
            return "הושלם";
        }
        // optional legacy statuses
        if (u.equals("DRAFT") || s.equals("טיוטה")) {
            return "טיוטה";
        }
        if (u.equals("FROZEN") || s.equals("הוקפא")) {
            return "הוקפא";
        }
        if (u.equals("CANCELED") || s.equals("בוטל")) {
            return "בוטל";
        }

        // Unknown → keep as-is (already trimmed)
        return s;
    }

    /** UI label → code stored in filter (canonical Hebrew). */
    public static String uiLabelToCode(String label) {
        return canonicalCode(label);
    }

    /** Code (from DB/filter) → UI label (canonical Hebrew). */
    public static String codeToUiLabel(String code) {
        return canonicalCode(code);
    }
}
