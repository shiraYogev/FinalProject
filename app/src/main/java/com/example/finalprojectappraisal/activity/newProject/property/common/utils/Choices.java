package com.example.finalprojectappraisal.activity.newProject.property.common.utils;

import java.util.Arrays;
import java.util.List;

public final class Choices {

    private Choices(){}

    public static final List<String> YES_NO_STRINGS = Arrays.asList("כן", "לא");

    public static final List<String> ENTRANCE_DOOR_TYPES = Arrays.asList(
            "פלדה/ביטחון", "פלדה מעוצבת", "מעוצבת ממתכת (חרש)", "עץ אומן", "דמוי עץ",
            "מעוצבת (כנף וחצי)", "מעוצבת (כנף כפולה)", "טרם הותקן", "בבנייה"
    );

    public static final List<String> WINDOW_TYPES = Arrays.asList(
            "מסגרות אלומיניום משולבות זכוכית",
            "מסגרות עץ משולבות זכוכית",
            "מעורב מסגרות אלומיניום ועץ משולבות זכוכית",
            "מסגרות ברזל חרש משולבות זכוכית",
            "טרם הותקן"
    );

    public static final List<String> FLOORING_TYPES = Arrays.asList(
            "פורצלן גרניט","אבן טבעית/שיש","קרמיקה","טרצו","פסיפס",
            "אריחי בטון","בטון מוחלק","פרקט לביניציה","פרקט עץ","p.v.c",
            "לוחות דמוי עץ/אבן","פרלטו","פורצלן דמוי פרקט","ריצוף אפוקסי",
            "שטיח מקיר לקיר","טרם רוצף","בבנייה","אחר"
    );

    public static final List<String> FLOORING_SIZES = Arrays.asList(
            "120X240","120X120","100X100","90X90","80X80","60X60","50X50","45X45","40X40",
            "33X33","30X60","30X30","25X150","22X90","20X120","20X20","15X60","45X90","60X120"
    );

    public static final List<String> KITCHEN_CABINETS = Arrays.asList(
            "ארונות עץ עליונים ותחתונים",
            "ארונות עץ תחתונים",
            "ארונות עץ עליונים",
            "טרם הותקן",
            "בבנייה"
    );

    public static final List<String> KITCHEN_WORKTOPS = Arrays.asList(
            "שיש","עץ","מתכת"
    );

    public static final List<String> BATHROOM_FIXTURES = Arrays.asList(
            "כלים סניטריים- חדשים","כלים סניטריים- רגילים","כלים סניטריים- ישנים","טרם חופה","בית המגורים טרם נבנה"
    );

    public static final List<String> INTERIOR_DOOR_TYPES = Arrays.asList(
            "דמוי עץ/פולימרי", "עץ", "משולב עץ ומתכת", "מתכת", "עץ אומן",
            "מתכת אומן", "מעוצב/ קו אפס", "טרם הותקנו", "בבנייה", "אחר"
    );

    public static final List<String> PROPERTY_LOCATIONS = Arrays.asList("מרכזי","צדדי","סואן","אחר");

    // ✅ חדש: סוגי סביבה (לבחירה בשדה מאפייני סביבה)
    public static final List<String> ENVIRONMENT_TYPES = Arrays.asList(
            "מגורים","מסחר","תעשייה","משרדים","אחר"
    );

    public static final List<String> BUILDING_TYPES = Arrays.asList(
            "בית משותף","צמוד לקרקע","נחלה ומרכיביה","מבנה תעשייתי",
            "בניה קלה","בניה קלה מעץ","בניה קלה ממתכת","אחר"
    );

    public static final List<String> PHYSICAL_CONDITION_OPTIONS = Arrays.asList(
            "ישן","חדש מאוכלס","חדש לא מאוכלס","טרם נבנה","בבניה"
    );

    public static final List<String> FLOORS_REFERENCE_LEVELS = Arrays.asList(
            "מעל קומת קרקע","מעל קומת עמודים","מעל קומת מרתף",
            "מעל קומת מסחר","מעל קומת הכניסה","מעל הקרקע","אחר"
    );

    public static final List<String> ROOMS_OPTIONS = Arrays.asList(
            "1 חדר מגורים",
            "2 חדרי מגורים (כולל סלון)",
            "3 חדרי מגורים (כולל סלון)",
            "4 חדרי מגורים (כולל סלון)",
            "5 חדרי מגורים (כולל סלון)",
            "6 חדרי מגורים (כולל סלון)",
            "7 חדרי מגורים (כולל סלון)",
            "8 חדרי מגורים (כולל סלון)",
            "9 חדרי מגורים (כולל סלון)",
            "1.5 חדרי מגורים",
            "2.5 חדרי מגורים",
            "3.5 חדרי מגורים",
            "4.5 חדרי מגורים",
            "5.5 חדרי מגורים",
            "6.5 חדרי מגורים",
            "7.5 חדרי מגורים",
            "8.5 חדרי מגורים",
            "אחר"
    );

    public static final List<String> AIR_DIRECTIONS = Arrays.asList("צפון","דרום","מזרח","מערב");

    public static final List<String> EXTERNAL_CLADDING_OPTIONS = Arrays.asList(
            "בבנייה",
            "פסיפס",
            "אבן שיש",
            "אבן ירושלמית",
            "פח/ מתכת",
            "חשוף ללא חיפוי",
            "טייח רגיל",
            "טייח צבעוני",
            "שפריץ",
            "גרנולייט",
            "גרנולייט משולב טייח",
            "אחר"
    );

    public static final List<String> MAINTENANCE_OPTIONS = Arrays.asList(
            "רגילה","טובה","נמוכה","טרם נבנה","בבנייה","אחר"
    );

    public static final List<String> CONSTRUCTION_MATERIAL_OPTIONS = Arrays.asList(
            "בבנייה קונבנציונלית",
            "בלוקים ובטון",
            "בנייה קלה ממתכת",
            "בנייה קלה מעץ",
            "קונבנציונלי",
            "טרם נבנה"
    );

    // מעלית – טקסטואלי לפי התקן
    public static final List<String> ELEVATOR_OPTIONS = Arrays.asList(
            "אין",
            "יש (1)",
            "יש (2)",
            "יש (3)",
            "יש (4)"
    );

    // ✅ סורגים – "מלא / חלקי / אין"
    public static final List<String> BARS_LEVELS = Arrays.asList(
            "כן",
            "לא",
            "בחלק"
    );

    // ✅ מיזוג – "מלא / חלקי / אין"
    public static final List<String> AIR_CONDITIONING_LEVELS = Arrays.asList(
            "מלא",
            "חלקי",
            "אין"
    );



}
