// file: app/src/main/java/com/example/finalprojectappraisal/classifer/gemini/GeminiJsonParser.java
package com.example.finalprojectappraisal.classifer.gemini;

import com.example.finalprojectappraisal.model.Image;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** פריסה של תשובות JSON מג׳מיני + בניית שדות ל-DB/תיאור/aiClassifications. */
public final class GeminiJsonParser {

    private GeminiJsonParser() {}

    /** מפתחות Firestore לפי @PropertyName במודל Project */
    public interface FirestoreKeys {
        String APARTMENT_NUMBER        = "apartment_number(municipal_form)";
        String ENTRANCE_DOOR_CONDITION = "apartment_main_entrance_door";
        String BATHROOM_FIXTURES       = "apartment_bathroom_fixtures";
        String WINDOW_TYPE             = "apartment_windows";
        String HAS_CENTRAL_HEATING     = "central_heating_or_fireplace";
        String HAS_BARS                = "has_bars";
        String HAS_AIR_CONDITIONING    = "apartment_air_conditioning"; // String "כן"/"לא"
        String INTERIOR_DOOR_CONDITION = "apartment_interior_doors_and_frames";
        String FLOORING_TYPE           = "apartment_flooring";         // סוג + (מידה)
        String KITCHEN_CONDITION       = "apartment_kitchen";

        String HAS_ELEVATOR = "has_elevator";
        String HAS_PARKING  = "has_parking";
        String HAS_STORAGE  = "has_storage";
    }

    /** מפענח את תוצאת ג׳סון (כולל ניקוי ```json) ומחזיר Map לשמירה בפיירסטור. */
    public static Map<String, Object> parseToProjectFields(Image.Category category, String rawJsonLike) {
        Map<String, Object> fields = new HashMap<>();
        JSONObject j = toJson(rawJsonLike);
        if (j == null) return fields;

        switch (category) {
            case ENTRANCE_DOOR: {
                String apt  = optString(j, "apartmentNumber");
                String door = optString(j, "entranceDoorCondition");
                if (apt != null)  fields.put(FirestoreKeys.APARTMENT_NUMBER, apt);
                if (door != null) fields.put(FirestoreKeys.ENTRANCE_DOOR_CONDITION, door);
                break;
            }
            case BATHROOM: {
                String fixtures = optString(j, "bathroomFixtures");
                if (fixtures != null) fields.put(FirestoreKeys.BATHROOM_FIXTURES, fixtures);
                break;
            }
            case LIVING_ROOM: {
                String windowType = optString(j, "windowType");
                Boolean heating   = optBoolean(j, "hasCentralHeating");
                Boolean bars      = optBoolean(j, "hasBars");
                Boolean acBool    = optBoolean(j, "hasAirConditioning");
                String flType     = optString(j, "flooringType");
                String flSize     = optString(j, "flooringSize");

                if (windowType != null) fields.put(FirestoreKeys.WINDOW_TYPE, windowType);
                if (heating != null)    fields.put(FirestoreKeys.HAS_CENTRAL_HEATING, heating);
                if (bars != null)       fields.put(FirestoreKeys.HAS_BARS, bars);
                if (acBool != null)     fields.put(FirestoreKeys.HAS_AIR_CONDITIONING, acBool ? "כן" : "לא");
                if (flType != null)     fields.put(FirestoreKeys.FLOORING_TYPE, combineFlooring(flType, flSize));
                break;
            }
            case BEDROOM: {
                Boolean bars   = optBoolean(j, "hasBars");
                Boolean acBool = optBoolean(j, "hasAirConditioning");
                String doors   = optString(j, "interiorDoorCondition");
                String flType  = optString(j, "flooringType");
                String flSize  = optString(j, "flooringSize");

                if (bars != null)   fields.put(FirestoreKeys.HAS_BARS, bars);
                if (acBool != null) fields.put(FirestoreKeys.HAS_AIR_CONDITIONING, acBool ? "כן" : "לא");
                if (doors != null)  fields.put(FirestoreKeys.INTERIOR_DOOR_CONDITION, doors);
                if (flType != null) fields.put(FirestoreKeys.FLOORING_TYPE, combineFlooring(flType, flSize));
                break;
            }
            case KITCHEN: {
                String cabinets = optString(j, "cabinets");
                String worktop  = optString(j, "worktop");
                String kc = null;
                if (cabinets != null) kc = "ארונות: " + cabinets;
                if (worktop != null)  kc = (kc == null ? "" : kc + ", ") + "משטח: " + worktop;
                if (kc != null) fields.put(FirestoreKeys.KITCHEN_CONDITION, kc);
                break;
            }
            default:
                break;
        }
        return fields;
    }

    /** תיאור קריא לתמונה (בעברית) מתוך ה-JSON. */
    public static String buildDescription(Image.Category category, String rawJsonLike) {
        Map<String, String> kv = toDisplayMap(category, rawJsonLike);
        if (kv.isEmpty()) return "Classification completed";
        StringBuilder sb = new StringBuilder("זוהה: ");
        boolean first = true;
        for (Map.Entry<String, String> e : kv.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(e.getKey()).append(": ").append(e.getValue());
            first = false;
        }
        return sb.toString();
    }

    /** מייצר מפה ידידותית לתצוגה (מפתחות בעברית) לשימוש ב-UI/Toast. */
    public static Map<String, String> toDisplayMap(Image.Category category, String rawJsonLike) {
        Map<String, String> kv = new LinkedHashMap<>();
        JSONObject j = toJson(rawJsonLike);
        if (j == null) return kv;

        switch (category) {
            case ENTRANCE_DOOR: {
                String apt  = optString(j, "apartmentNumber");
                String door = optString(j, "entranceDoorCondition");
                if (apt != null)  kv.put("מספר דירה", apt);
                if (door != null) kv.put("סוג דלת", door);
                break;
            }
            case BATHROOM: {
                String fixtures = optString(j, "bathroomFixtures");
                if (fixtures != null) kv.put("כלים סניטריים", fixtures);
                break;
            }
            case LIVING_ROOM: {
                String windowType = optString(j, "windowType");
                Boolean heating   = optBoolean(j, "hasCentralHeating");
                Boolean bars      = optBoolean(j, "hasBars");
                Boolean ac        = optBoolean(j, "hasAirConditioning");
                String flType     = optString(j, "flooringType");
                String flSize     = optString(j, "flooringSize");

                if (windowType != null) kv.put("חלונות", windowType);
                if (heating != null)    kv.put("הסקה מרכזית/קמין", heating ? "כן" : "לא");
                if (bars != null)       kv.put("סורגים", bars ? "כן" : "לא");
                if (ac != null)         kv.put("מיזוג אוויר", ac ? "כן" : "לא");
                if (flType != null) {
                    kv.put("ריצוף", combineFlooring(flType, flSize));
                    if (flSize != null) kv.put("מידת ריצוף", flSize);
                }
                break;
            }
            case BEDROOM: {
                Boolean bars   = optBoolean(j, "hasBars");
                Boolean ac     = optBoolean(j, "hasAirConditioning");
                String doors   = optString(j, "interiorDoorCondition");
                String flType  = optString(j, "flooringType");
                String flSize  = optString(j, "flooringSize");

                if (bars != null)  kv.put("סורגים", bars ? "כן" : "לא");
                if (ac != null)    kv.put("מיזוג אוויר", ac ? "כן" : "לא");
                if (doors != null) kv.put("דלתות פנים", doors);
                if (flType != null) {
                    kv.put("ריצוף", combineFlooring(flType, flSize));
                    if (flSize != null) kv.put("מידת ריצוף", flSize);
                }
                break;
            }
            case KITCHEN: {
                String cabinets = optString(j, "cabinets");
                String worktop  = optString(j, "worktop");
                if (cabinets != null) kv.put("ארונות", cabinets);
                if (worktop != null)  kv.put("משטח עבודה", worktop);
                break;
            }
            default:
                break;
        }
        return kv;
    }

    /** חילוץ aiClassifications לפי קטגוריה (לא שומר—רק מחזיר). */
    public static Map<Image.Subcategory, String> extractAiClassifications(Image.Category category, String rawJsonLike) {
        Map<Image.Subcategory, String> out = new HashMap<>();
        JSONObject j = toJson(rawJsonLike);
        if (j == null) return out;

        switch (category) {
            case ENTRANCE_DOOR: {
                String door = optString(j, "entranceDoorCondition");
                if (door != null) out.put(Image.Subcategory.DOOR, door);
                break;
            }
            case KITCHEN: {
                String cabinets = optString(j, "cabinets");
                String worktop  = optString(j, "worktop");
                if (cabinets != null) out.put(Image.Subcategory.CABINETS, cabinets);
                if (worktop != null)  out.put(Image.Subcategory.WORKTOP, worktop);
                break;
            }
            case LIVING_ROOM: {
                String win  = optString(j, "windowType");
                String flT  = optString(j, "flooringType");
                String flS  = optString(j, "flooringSize");
                if (win != null) out.put(Image.Subcategory.WINDOW, win);
                if (flT != null) out.put(Image.Subcategory.FLOOR, combineFlooring(flT, flS));
                break;
            }
            case BEDROOM: {
                String doors = optString(j, "interiorDoorCondition");
                String flT   = optString(j, "flooringType");
                String flS   = optString(j, "flooringSize");
                if (doors != null) out.put(Image.Subcategory.DOOR, doors);
                if (flT != null)   out.put(Image.Subcategory.FLOOR, combineFlooring(flT, flS));
                break;
            }
            case BATHROOM: {
                String fixtures = optString(j, "bathroomFixtures");
                if (fixtures != null) out.put(Image.Subcategory.TOILET, fixtures);
                break;
            }
            default:
                break;
        }
        return out;
    }

    // ---------- Utilities ----------

    /** מקבל מחרוזת עם ```json ... ``` או סתם טקסט, ומחזיר JSONObject (או null אם נכשל). */
    private static JSONObject toJson(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        // הסרת גדרות קוד אם קיימות
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "");
            if (t.endsWith("```")) t = t.substring(0, t.lastIndexOf("```"));
            t = t.trim();
        }
        // חילוץ הבלוק בין הסוגריים המסולסלים הראשונים/האחרונים
        int first = t.indexOf('{');
        int last  = t.lastIndexOf('}');
        if (first >= 0 && last > first) {
            t = t.substring(first, last + 1).trim();
        }
        try {
            return new JSONObject(t);
        } catch (JSONException e) {
            return null;
        }
    }

    private static String optString(JSONObject j, String key) {
        if (!j.has(key) || j.isNull(key)) return null;
        String s = j.optString(key, null);
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private static Boolean optBoolean(JSONObject j, String key) {
        if (!j.has(key) || j.isNull(key)) return null;
        Object v = j.opt(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) {
            String s = ((String) v).trim().toLowerCase();
            if ("true".equals(s))  return true;
            if ("false".equals(s)) return false;
        }
        return null;
    }

    /** מאחד סוג+מידה של ריצוף; אם אין מידה מחזיר רק סוג. */
    private static String combineFlooring(String type, String size) {
        if (type == null) return null;
        if (size == null || size.trim().isEmpty()) return type;
        return type + " (" + size.trim() + ")";
    }
}
