package com.example.finalprojectappraisal.utils;

import java.util.Locale;

public class StatusMapper {

    /** תווית UI בעברית -> קוד באנגלית/UPPERCASE לתוך המסנן/DB */
    public static String uiLabelToCode(String label) {
        if (label == null) return "";
        switch (label.trim()) {
            case "טיוטה":  return "DRAFT";
            case "בטיפול": return "IN PROGRESS";
            case "הושלם":  return "COMPLETED";
            case "הוקפא":  return "FROZEN";
            case "בוטל":   return "CANCELED";
            default:        return label.trim().toUpperCase(Locale.ROOT);
        }
    }

    /** קוד באנגלית/UPPERCASE -> תווית UI בעברית להצגה בצ'יפים */
    public static String codeToUiLabel(String code) {
        if (code == null) return "";
        switch (code.trim().toUpperCase(Locale.ROOT)) {
            case "DRAFT":        return "טיוטה";
            case "IN PROGRESS":  return "בטיפול";
            case "COMPLETED":    return "הושלם";
            case "FROZEN":       return "הוקפא";
            case "CANCELED":     return "בוטל";
            default:             return code;
        }
    }
}
