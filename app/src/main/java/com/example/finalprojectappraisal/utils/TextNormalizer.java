package com.example.finalprojectappraisal.utils;

public class TextNormalizer {
    /** Normalize for contains() search: lowercase, collapse whitespace, strip quotes/tabs/newlines. */
    public static String normalizeOrEmpty(String s) {
        if (s == null) return "";
        String t = s.toLowerCase()
                .replace('\n', ' ')
                .replace('\t', ' ')
                .replace('"', ' ')
                .replace('\'', ' ');
        t = t.replaceAll("\\s+", " ").trim();
        return t;
    }

    public static String normalizeOrNull(String s) {
        String t = normalizeOrEmpty(s);
        return t.isEmpty() ? null : t;
    }
}
