package com.example.finalprojectappraisal.model;

/**
 * סוגי קומות אפשריים לדירה (לבחירה בעת הוספת קומה).
 * MAIN היא ברירת המחדל לדירה רגילה (קומה אחת).
 */
public enum FloorType {
    BASEMENT("מרתף"),
    GROUND("קומת קרקע"),
    FLOOR("קומה"),        // קומה ממוספרת (קומה 1, קומה 2 ...)
    ATTIC("עליית גג"),
    ROOF("קומת גג"),
    MAIN("קומה ראשית");   // ברירת מחדל לדירה חד-קומתית

    public final String label;

    FloorType(String label) {
        this.label = label;
    }
}
