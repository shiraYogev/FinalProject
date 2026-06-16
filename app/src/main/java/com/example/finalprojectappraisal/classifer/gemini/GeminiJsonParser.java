// file: app/src/main/java/com/example/finalprojectappraisal/classifer/gemini/GeminiJsonParser.java
package com.example.finalprojectappraisal.classifer.gemini;

import com.example.finalprojectappraisal.activity.newProject.property.common.utils.Choices;
import com.example.finalprojectappraisal.activity.newProject.property.common.utils.Formatters;
import com.example.finalprojectappraisal.model.Image;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String NUMBER_OF_FLOORS = "number_of_floors";
        String EXTERNAL_CLADDING = "external_cladding";
        String BUILDING_TYPE = "building_type";
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
            case ELEVATOR: {
                String rawElevator = firstNonEmpty(
                        optString(j, "elevatorCount"),
                        optString(j, "elevators"),
                        optString(j, "elevator"),
                        optString(j, "count"),
                        optString(j, "numberOfElevators"),
                        optFromNumber(j, "elevatorCount"),
                        optFromNumber(j, "count")
                );

                String elevators = normalizeElevator(rawElevator);
                if (elevators != null) {
                    fields.put(FirestoreKeys.HAS_ELEVATOR, elevators);
                    fields.put("property_details." + FirestoreKeys.HAS_ELEVATOR, elevators);
                }

                String rawFloors = firstNonEmpty(
                        optString(j, "buildingFloors"),
                        optString(j, "floors"),
                        optString(j, "numberOfFloors"),
                        optFromNumber(j, "buildingFloors"),
                        optFromNumber(j, "floors"),
                        optFromNumber(j, "numberOfFloors")
                );

                String normalizedFloors = normalizeFloors(rawFloors);
                if (normalizedFloors != null) {
                    fields.put(FirestoreKeys.NUMBER_OF_FLOORS, normalizedFloors);
                    fields.put("property_details." + FirestoreKeys.NUMBER_OF_FLOORS, normalizedFloors);
                }
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
                if (Formatters.cabinetsImplyNoWorktop(cabinets)) {
                    worktop = null;
                }
                String kc = Formatters.combineKitchen(cabinets, worktop);
                if (kc != null) fields.put(FirestoreKeys.KITCHEN_CONDITION, kc);
                break;
            }
            case HALLWAY: {
                String rawDoor = firstNonEmpty(
                        optString(j, "interiorDoorType"),
                        optString(j, "interiorDoor"),
                        optString(j, "doorType")
                );

                String doors = normalizeInteriorDoor(rawDoor);
                if (doors != null) {
                    fields.put(FirestoreKeys.INTERIOR_DOOR_CONDITION, doors);
                }
                break;
            }
            case EXTERIOR: {
                String rawCladding = firstNonEmpty(
                        optString(j, "externalCladding"),
                        optString(j, "exteriorCladding"),
                        optString(j, "facadeCladding"),
                        optString(j, "cladding")
                );

                String cladding = normalizeExternalCladding(rawCladding);
                if (cladding != null) {
                    fields.put("property_details." + FirestoreKeys.EXTERNAL_CLADDING, cladding);
                }

                String rawBuildingType = firstNonEmpty(
                        optString(j, "buildingType"),
                        optString(j, "building_type"),
                        optString(j, "structureType")
                );

                String buildingType = normalizeBuildingType(rawBuildingType);
                if (buildingType != null) {
                    fields.put("property_details." + FirestoreKeys.BUILDING_TYPE, buildingType);
                }
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
            case ELEVATOR: {
                String rawElevator = firstNonEmpty(
                        optString(j, "elevatorCount"),
                        optString(j, "elevators"),
                        optString(j, "elevator"),
                        optString(j, "count"),
                        optString(j, "numberOfElevators"),
                        optFromNumber(j, "elevatorCount"),
                        optFromNumber(j, "count")
                );

                String elevators = normalizeElevator(rawElevator);
                if (elevators != null) kv.put("מספר מעליות", elevators);

                String rawFloors = firstNonEmpty(
                        optString(j, "buildingFloors"),
                        optString(j, "floors"),
                        optString(j, "numberOfFloors"),
                        optFromNumber(j, "buildingFloors"),
                        optFromNumber(j, "floors"),
                        optFromNumber(j, "numberOfFloors")
                );

                String normalizedFloors = normalizeFloors(rawFloors);
                if (normalizedFloors != null) kv.put("מספר קומות בבניין", normalizedFloors);
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
                if (worktop != null && !Formatters.cabinetsImplyNoWorktop(cabinets)) {
                    kv.put("משטח עבודה", Formatters.decorateWorktop(worktop));
                }
                break;
            }
            case HALLWAY: {
                String rawDoor = firstNonEmpty(
                        optString(j, "interiorDoorType"),
                        optString(j, "interiorDoor"),
                        optString(j, "doorType")
                );

                String doors = normalizeInteriorDoor(rawDoor);
                if (doors != null) kv.put("דלתות פנים", doors);
                break;
            }
            case EXTERIOR: {
                String rawCladding = firstNonEmpty(
                        optString(j, "externalCladding"),
                        optString(j, "exteriorCladding"),
                        optString(j, "facadeCladding"),
                        optString(j, "cladding")
                );

                String cladding = normalizeExternalCladding(rawCladding);
                if (cladding != null) kv.put("חיפוי חיצוני", cladding);

                String rawBuildingType = firstNonEmpty(
                        optString(j, "buildingType"),
                        optString(j, "building_type"),
                        optString(j, "structureType")
                );

                String buildingType = normalizeBuildingType(rawBuildingType);
                if (buildingType != null) kv.put("סוג בניין", buildingType);
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
                if (worktop != null && !Formatters.cabinetsImplyNoWorktop(cabinets)) {
                    out.put(Image.Subcategory.WORKTOP, worktop);
                }
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
            case HALLWAY: {
                String rawDoor = firstNonEmpty(
                        optString(j, "interiorDoorType"),
                        optString(j, "interiorDoor"),
                        optString(j, "doorType")
                );

                String doors = normalizeInteriorDoor(rawDoor);
                if (doors != null) out.put(Image.Subcategory.DOOR, doors);
                break;
            }
            case BATHROOM: {
                String fixtures = optString(j, "bathroomFixtures");
                if (fixtures != null) out.put(Image.Subcategory.TOILET, fixtures);
                break;
            }
            case EXTERIOR: {
                String rawCladding = firstNonEmpty(
                        optString(j, "externalCladding"),
                        optString(j, "exteriorCladding"),
                        optString(j, "facadeCladding"),
                        optString(j, "cladding")
                );

                String cladding = normalizeExternalCladding(rawCladding);
                if (cladding != null) out.put(Image.Subcategory.WALLS, cladding);

                String rawBuildingType = firstNonEmpty(
                        optString(j, "buildingType"),
                        optString(j, "building_type"),
                        optString(j, "structureType")
                );

                String buildingType = normalizeBuildingType(rawBuildingType);
                if (buildingType != null) out.put(Image.Subcategory.OTHER, buildingType);
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
        return type + " " + size.trim();
    }

    /** מבטיח שערך המעלית תואם לאופציות Choices.ELEVATOR_OPTIONS. */
    private static String normalizeElevator(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;

        if ("אין".equals(value) || "0".equals(value)) {
            return "אין";
        }

        if (value.matches("יש \\(\\d+\\)")) {
            return value;
        }

        // חיפוש מספר בכל טקסט חופשי (למשל "two elevators" → 2)
        Matcher matcher = Pattern.compile("(\\d+)").matcher(value);
        if (matcher.find()) {
            int count;
            try {
                count = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                count = 0;
            }
            if (count <= 0) {
                return "אין";
            }
            if (count > 4) {
                count = 4; // גבול עליון לפי ה-Choices
            }
            return "יש (" + count + ")";
        }

        String lowered = value.toLowerCase();
        if ("yes".equals(lowered) || "יש".equals(lowered)) {
            return "יש (1)";
        }
        if ("no".equals(lowered) || "none".equals(lowered) || "לא".equals(lowered)) {
            return "אין";
        }

        return value;
    }

    private static String normalizeFloors(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;

        // אם מדובר במספר נקי
        if (trimmed.matches("\\d+")) {
            return clampFloors(trimmed);
        }

        // חיפוש ספרות בתוך טקסט ("12 קומות", "12 floors" וכו')
        Matcher matcher = Pattern.compile("(\\d+)").matcher(trimmed);
        if (matcher.find()) {
            return clampFloors(matcher.group(1));
        }

        return null;
    }

    private static String clampFloors(String digits) {
        if (digits == null) return null;
        try {
            int value = Integer.parseInt(digits);
            if (value <= 0) return null;
            if (value > 120) value = 120;
            return String.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeExternalCladding(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;

        for (String option : Choices.EXTERNAL_CLADDING_OPTIONS) {
            if (option.equals(trimmed)) {
                return option;
            }
            if (option.replace(" ", "").equals(trimmed.replace(" ", ""))) {
                return option;
            }
        }

        String lowered = trimmed.toLowerCase(Locale.ROOT);

        if (lowered.contains("construction") || lowered.contains("בבנייה") || lowered.contains("under construction")) {
            return "בבנייה";
        }
        if (lowered.contains("mosaic") || lowered.contains("פסיפס")) {
            return "פסיפס";
        }
        if (lowered.contains("jerusalem")) {
            return "אבן ירושלמית";
        }
        if (lowered.contains("marble") || lowered.contains("stone") || lowered.contains("limestone")) {
            return "אבן שיש";
        }
        if (lowered.contains("metal") || lowered.contains("steel") || lowered.contains("tin") || lowered.contains("pach")) {
            return "פח/ מתכת";
        }
        if (lowered.contains("bare") || lowered.contains("exposed") || lowered.contains("no cladding")) {
            return "חשוף ללא חיפוי";
        }
        if (lowered.contains("colored") || lowered.contains("color") || lowered.contains("stucco") || lowered.contains("צבעוני")) {
            return "טייח צבעוני";
        }
        if (lowered.contains("plaster") || lowered.contains("טיח") || lowered.contains("טייח")) {
            if (lowered.contains("granolite") || lowered.contains("גרנול")) {
                return "גרנולייט משולב טייח";
            }
            return "טייח רגיל";
        }
        if (lowered.contains("spritz") || lowered.contains("spray")) {
            return "שפריץ";
        }
        if (lowered.contains("granolite") || lowered.contains("גרנול")) {
            return "גרנולייט";
        }

        return "אחר";
    }

    private static String normalizeBuildingType(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;

        for (String option : Choices.BUILDING_TYPES) {
            if (option.equals(trimmed)) {
                return option;
            }
            if (option.replace(" ", "").equals(trimmed.replace(" ", ""))) {
                return option;
            }
        }

        String lowered = trimmed.toLowerCase(Locale.ROOT);

        if (lowered.contains("apartment") || lowered.contains("multi") || lowered.contains("condo") || lowered.contains("complex")) {
            return "בית משותף";
        }

        if (lowered.contains("detached") || lowered.contains("house") || lowered.contains("villa") || lowered.contains("cottage") || lowered.contains("bungalow") || lowered.contains("single")) {
            return "צמוד לקרקע";
        }

        if (lowered.contains("farm") || lowered.contains("estate") || lowered.contains("agric") || lowered.contains("ranch")) {
            return "נחלה ומרכיביה";
        }

        if (lowered.contains("industrial") || lowered.contains("factory") || lowered.contains("warehouse") || lowered.contains("plant")) {
            return "מבנה תעשייתי";
        }

        boolean mentionsLightConstruction = lowered.contains("light") || lowered.contains("prefab") || lowered.contains("modular") || lowered.contains("prefabricated") || lowered.contains("temporary") || lowered.contains("portable");
        boolean mentionsWood = lowered.contains("wood") || lowered.contains("timber") || lowered.contains("log");
        boolean mentionsMetal = lowered.contains("metal") || lowered.contains("steel") || lowered.contains("iron") || lowered.contains("aluminum");

        if (mentionsLightConstruction && mentionsWood) {
            return "בניה קלה מעץ";
        }

        if (mentionsLightConstruction && mentionsMetal) {
            return "בניה קלה ממתכת";
        }

        if (mentionsLightConstruction) {
            return "בניה קלה";
        }

        if (mentionsWood) {
            return "בניה קלה מעץ";
        }

        if (mentionsMetal) {
            return "בניה קלה ממתכת";
        }

        if (lowered.contains("unknown") || lowered.contains("other") || lowered.contains("misc")) {
            return "אחר";
        }

        return "אחר";
    }

    private static String normalizeInteriorDoor(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;

        if (Choices.INTERIOR_DOOR_TYPES.contains(trimmed)) {
            return trimmed;
        }

        String lowered = trimmed.toLowerCase(Locale.ROOT);

        if (lowered.contains("פולימ")) return "דמוי עץ/פולימרי";
        if (lowered.contains("קו אפס") || lowered.contains("flush") || lowered.contains("concealed")) {
            return "מעוצב/ קו אפס";
        }
        if (lowered.contains("טרם") && lowered.contains("ותק")) return "טרם הותקנו";
        if (lowered.contains("בבנ")) return "בבנייה";

        boolean hasWood = lowered.contains("wood") || lowered.contains("עץ");
        boolean hasMetal = lowered.contains("metal") || lowered.contains("מתכת");
        boolean artisan = lowered.contains("artisan") || lowered.contains("אומן") || lowered.contains("craft");

        if (hasWood && hasMetal) {
            return "משולב עץ ומתכת";
        }
        if (hasMetal) {
            return artisan ? "מתכת אומן" : "מתכת";
        }
        if (hasWood) {
            return artisan ? "עץ אומן" : "עץ";
        }

        return "אחר";
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v == null) continue;
            String trimmed = v.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return null;
    }

    private static String optFromNumber(JSONObject j, String key) {
        if (!j.has(key) || j.isNull(key)) return null;
        Object v = j.opt(key);
        if (v instanceof Number) {
            return String.valueOf(((Number) v).intValue());
        }
        return null;
    }
}
