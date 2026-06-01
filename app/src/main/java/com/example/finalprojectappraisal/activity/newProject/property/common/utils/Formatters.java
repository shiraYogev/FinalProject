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

    /** חיבור ריצוף: 'סוג מידה' או רק 'סוג' אם אין מידה; אם אין סוג מחזיר null */
    public static String combineFlooring(String type, String size) {
        if (type == null || type.trim().isEmpty()) return null;
        String t = type.trim();
        if (size == null || size.trim().isEmpty()) return t;
        return t + " " + size.trim();
    }

    /**
     * מאחד ארונות + משטח עבודה למחרוזת אחידה. מחזיר null אם אין שום מידע.
     * התבנית: "ארונות ... עם משטח עבודה מ..." או רק "ארונות ..." אם אין משטח.
     */
    public static String combineKitchen(String cabinets, String worktop) {
        String c = stripTrailing(cabinets);
        String w = stripTrailing(worktop);

        if (c == null && w == null) return null;

        StringBuilder sb = new StringBuilder();
        if (c != null) {
            sb.append(c);
        }

        String decoratedWorktop = decorateWorktop(w);
        if (decoratedWorktop != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(decoratedWorktop);
        }

        if (sb.length() == 0) return null;
        if (sb.charAt(sb.length() - 1) != '.') {
            sb.append('.');
        }
        return sb.toString();
    }

    /** מפענח מחרוזת "ארונות ... עם משטח עבודה מ..." לשני רכיבים נפרדים. */
    public static KitchenParts splitKitchen(String combined) {
        String original = trimToNull(combined);
        if (original == null) {
            return new KitchenParts(null, null, null);
        }

        String cabinets = null;
        String worktop = null;

        String text = original;
        int joinIdx = text.indexOf(" עם משטח עבודה");
        if (joinIdx >= 0) {
            cabinets = stripTrailing(text.substring(0, joinIdx));
            worktop = stripWorktopDecoration(text.substring(joinIdx).trim());
        } else if (text.contains("משטח")) {
            // פורמט ישן: "ארונות: X, משטח עבודה: Y"
            String[] parts = text.split(",");
            for (String part : parts) {
                String p = part.trim();
                if (p.startsWith("ארונות")) {
                    p = p.replaceFirst("^ארונות[:\\s]*", "");
                    cabinets = stripTrailing(p);
                } else if (p.startsWith("משטח")) {
                    p = p.replaceFirst("^משטח(?: עבודה)?[:\\s]*", "");
                    worktop = stripTrailing(p);
                }
            }
            if (cabinets == null && parts.length > 0) {
                String candidate = parts[0].trim();
                if (!candidate.startsWith("משטח")) {
                    cabinets = stripTrailing(candidate);
                }
            }
        } else {
            cabinets = stripTrailing(text);
        }

        return new KitchenParts(cabinets, worktop, original);
    }

    /** הופך ערך בסיסי של משטח לעיצוב "עם משטח עבודה מ..." */
    public static String decorateWorktop(String worktop) {
        String base = trimToNull(worktop);
        if (base == null) return null;

        if (base.startsWith("עם ")) {
            return stripTrailing(base);
        }

        if (base.startsWith("משטח עבודה")) {
            return stripTrailing("עם " + base);
        }

        String withM = base.startsWith("מ") ? base : "מ" + base;
        return stripTrailing("עם משטח עבודה " + withM);
    }

    /** מסיר את הקידומת "עם משטח עבודה" ומחזיר ערך בסיסי (למשל "שיש"). */
    public static String stripWorktopDecoration(String decorated) {
        String t = trimToNull(decorated);
        if (t == null) return null;

        if (t.startsWith("עם ")) {
            t = t.substring(3).trim();
        }
        if (t.startsWith("משטח עבודה")) {
            t = t.substring("משטח עבודה".length()).trim();
        }
        if (t.startsWith("מ") && t.length() > 1) {
            t = t.substring(1).trim();
        }
        return stripTrailing(t);
    }

    /** האם טקסט ארונות מרמז שאין משטח עבודה מותקן. */
    public static boolean cabinetsImplyNoWorktop(String cabinets) {
        String c = stripTrailing(cabinets);
        if (c == null) return false;
        if (c.contains("אין עבודה")) return true;
        if ("טרם הותקן".equals(c) || "בבנייה".equals(c)) return true;
        return "אין ארונות".equals(c);
    }

    /** חלקי מטבח מפוענחים. */
    public static final class KitchenParts {
        public final String cabinets;
        public final String worktop;
        public final String original;

        public KitchenParts(String cabinets, String worktop, String original) {
            this.cabinets = stripTrailing(cabinets);
            this.worktop = stripTrailing(worktop);
            this.original = trimToNull(original);
        }

        /** מחזיר מחרוזת תקנית; אם שניהם ריקים מחזיר את המקור. */
        public String normalized() {
            String combined = combineKitchen(cabinets, worktop);
            if (combined != null) return combined;
            return original;
        }
    }

    private static String stripTrailing(String s) {
        String t = trimToNull(s);
        if (t == null) return null;
        int end = t.length();
        while (end > 0) {
            char ch = t.charAt(end - 1);
            if (Character.isWhitespace(ch) || ch == '.' || ch == ',') {
                end--;
            } else {
                break;
            }
        }
        return trimToNull(t.substring(0, end));
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
