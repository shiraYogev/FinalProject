package com.example.finalprojectappraisal.activity.newProject.property.common.utils;

public final class Formatters {
    private Formatters() {}

    public static final String EM_DASH = "—";
    public static final String YES = "כן";
    public static final String NO  = "לא";

    /** אם המחרוזת ריקה/null מחזיר מקף תצוגה */
    public static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? EM_DASH : s.trim();
    }

    /** המרה לבוליאן "כן"/"לא" */
    public static String boolText(boolean b) {
        return b ? YES : NO;
    }

    /** חיבור ריצוף: 'סוג (מידה)' או רק 'סוג' אם אין מידה; אם אין סוג מחזיר null */
    public static String combineFlooring(String type, String size) {
        if (type == null || type.trim().isEmpty()) return null;
        String t = type.trim();
        if (size == null || size.trim().isEmpty()) return t;
        return t + " (" + size.trim() + ")";
    }
}
