package com.example.finalprojectappraisal.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * יחידת דיור בתוך נכס (לדירה מחולקת).
 * index == 0 => היחידה הראשית (הדירה כולה, מצב לא-מחולק).
 * index >= 1 => יחידות נוספות ("יחידה א", "יחידה ב", ...).
 *
 * כל יחידה מחזיקה את רשימת הקומות שלה (כי גם ביחידה יכולות להיות מספר קומות).
 * key() הוא המזהה היציב שנשמר על כל Image (unitId).
 */
public class Unit {

    /** מזהה היחידה הראשית (דירה לא מחולקת / היחידה הראשונה). */
    public static final String DEFAULT_KEY = "MAIN";

    private static final String[] HEB_LETTERS = {
            "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט", "י",
            "יא", "יב", "יג", "יד", "טו", "טז", "יז", "יח", "יט", "כ"
    };

    public final String key;   // "MAIN", "UNIT_1", "UNIT_2", ...
    public final int index;    // 0 = ראשית, 1+ = יחידות נוספות

    /** הקומות של היחידה הזו (ברירת מחדל: קומה ראשית). */
    public final List<Floor> floors = new ArrayList<>();

    public Unit(String key, int index) {
        this.key = (key != null && !key.trim().isEmpty()) ? key : DEFAULT_KEY;
        this.index = index;
    }

    /** היחידה הראשית, עם קומה ראשית כברירת מחדל. */
    public static Unit main() {
        Unit u = new Unit(DEFAULT_KEY, 0);
        u.floors.add(Floor.main());
        return u;
    }

    /** יחידה ממוספרת חדשה (index >= 1), עם קומה ראשית כברירת מחדל. */
    public static Unit numbered(int index) {
        Unit u = new Unit("UNIT_" + index, index);
        u.floors.add(Floor.main());
        return u;
    }

    public String key() {
        return key;
    }

    public boolean isMain() {
        return index == 0;
    }

    /** תווית לתצוגה למשתמש. */
    public String label() {
        if (index <= 0) return "הדירה הראשית";
        String letter = (index - 1 < HEB_LETTERS.length) ? HEB_LETTERS[index - 1] : String.valueOf(index);
        return "יחידה " + letter;
    }

    /** בונה Unit מתוך key ששמור על Image. */
    public static Unit fromKey(String key) {
        if (key == null || key.trim().isEmpty() || DEFAULT_KEY.equals(key)) {
            return new Unit(DEFAULT_KEY, 0);
        }
        if (key.startsWith("UNIT_")) {
            try {
                int idx = Integer.parseInt(key.substring("UNIT_".length()));
                return new Unit(key, idx);
            } catch (NumberFormatException e) {
                return new Unit(key, 1);
            }
        }
        return new Unit(key, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Unit)) return false;
        Unit unit = (Unit) o;
        return key().equals(unit.key());
    }

    @Override
    public int hashCode() {
        return Objects.hash(key());
    }
}
