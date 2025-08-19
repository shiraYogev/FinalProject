package com.example.finalprojectappraisal.activity.newProject.property.common.utils;

import java.util.Arrays;
import java.util.List;

public final class Choices {

    private Choices(){}

    public static final List<String> YES_NO_STRINGS = Arrays.asList("כן", "לא");

    public static final List<String> ENTRANCE_DOOR_TYPES = Arrays.asList(
            "פלדלת", "פלדלת מעוצבת", "עץ", "מעוצבת ממתכת (חרש)", "דמוי עץ", "טרם הותקן"
    );

    public static final List<String> WINDOW_TYPES = Arrays.asList(
            "מסגרות אלומיניום משולבות זכוכית",
            "מסגרות עץ משולבות זכוכית",
            "מעורב מסגרות אלומיניום ועץ משולבות זכוכית",
            "מסגרות ברזל חרש משולבות זכוכית",
            "טרם הותקן"
    );

    public static final List<String> FLOORING_TYPES = Arrays.asList(
            "אבן טבעית","שיש","טרצו","גרניט","קרמיקה","פסיפס","פורצלן גרניט",
            "אריחי בטון","בטון מוחלק","פרקט","p.v.c","פרלטו","פורצלן דמוי פרקט","אחר","טרם רוצף"
    );

    public static final List<String> FLOORING_SIZES = Arrays.asList(
            "120X120","100X100","90X90","80X80","60X60","50X50","45X45","40X40","30X30",
            "45X90","60X120","22X90","20X120"
    );

    public static final List<String> KITCHEN_CABINETS = Arrays.asList(
            "עליונים בלבד", "תחתונים בלבד", "עליונים ותחתונים", "אין ארונות"
    );

    public static final List<String> KITCHEN_WORKTOPS = Arrays.asList(
            "שיש","עץ","מתכת"
    );

    public static final List<String> BATHROOM_FIXTURES = Arrays.asList(
            "חדשים","רגילים","ישנים","טרם חופה","בית המגורים טרם נבנה"
    );

    public static final List<String> INTERIOR_DOOR_TYPES = Arrays.asList(
            "מתכת","עץ","משולב עץ ומתכת","דמוי עץ-פולימרי","טרם הותקנו"
    );

    public static final List<String> PROPERTY_LOCATIONS = Arrays.asList("מרכזי","צדדי","סואן","אחר");

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

    public static final List<String> ROOMS_OPTIONS = Arrays.asList("1","2","3","4","5","6","7","8");

    public static final List<String> AIR_DIRECTIONS = Arrays.asList("צפון","דרום","מזרח","מערב");


}