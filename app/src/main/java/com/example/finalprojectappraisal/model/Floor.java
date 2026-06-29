package com.example.finalprojectappraisal.model;

import java.util.Objects;

/**
 * מופע קומה בודדת בדירה: סוג + אינדקס (לקומות ממוספרות).
 * key() הוא המזהה היציב שנשמר על כל Image (למשל "GROUND", "FLOOR_1", "ATTIC").
 */
public class Floor {

    /** מזהה ברירת המחדל לדירה חד-קומתית. */
    public static final String DEFAULT_KEY = FloorType.MAIN.name();

    public final FloorType type;
    public final int index; // רלוונטי רק כש-type == FLOOR; אחרת 0

    public Floor(FloorType type, int index) {
        this.type = (type != null) ? type : FloorType.MAIN;
        this.index = index;
    }

    public Floor(FloorType type) {
        this(type, 0);
    }

    /** ברירת מחדל: קומה ראשית. */
    public static Floor main() {
        return new Floor(FloorType.MAIN, 0);
    }

    /** מזהה יציב לשמירה (לא לתצוגה). */
    public String key() {
        return (type == FloorType.FLOOR) ? "FLOOR_" + index : type.name();
    }

    /** תווית לתצוגה למשתמש. */
    public String label() {
        return (type == FloorType.FLOOR) ? "קומה " + index : type.label;
    }

    /** בונה Floor מתוך key ששמור על Image. */
    public static Floor fromKey(String key) {
        if (key == null || key.trim().isEmpty()) return main();
        if (key.startsWith("FLOOR_")) {
            try {
                int idx = Integer.parseInt(key.substring("FLOOR_".length()));
                return new Floor(FloorType.FLOOR, idx);
            } catch (NumberFormatException e) {
                return new Floor(FloorType.FLOOR, 1);
            }
        }
        try {
            return new Floor(FloorType.valueOf(key), 0);
        } catch (IllegalArgumentException e) {
            return main();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Floor)) return false;
        Floor floor = (Floor) o;
        return key().equals(floor.key());
    }

    @Override
    public int hashCode() {
        return Objects.hash(key());
    }
}
