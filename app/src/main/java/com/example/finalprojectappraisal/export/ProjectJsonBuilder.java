package com.example.finalprojectappraisal.export;

import com.example.finalprojectappraisal.model.Client;
import com.example.finalprojectappraisal.model.Image;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * בונה את מבנה ה-JSON לפי הפורמט הנדרש עבור אתר חיצוני.
 * ממיר את נתוני הפרויקט למבנה JSON סטנדרטי.
 */
public class ProjectJsonBuilder {

    /**
     * יוצר JSONObject מלא לפי הפורמט הנדרש
     */
    public static JSONObject buildExportJson(ProjectJsonExportData data) throws JSONException {
        JSONObject root = new JSONObject();

        // ===== Presenter Details (Appraiser info) =====
        addPresenterDetails(root, data);

        // ===== Property Summary & Description =====
        root.put("property_summary", safeString(data.getPropertySummary()));

        // ===== Address & Building Info =====
        addPropertyDetails(root, data);

        // ===== Images =====
        addImages(root, data);

        // ===== Apartment Details =====
        addApartmentDetails(root, data);

        // ===== Features (Boolean fields) =====
        addFeatures(root, data);

        // ===== Areas =====
        addAreas(root, data);

        return root;
    }

    private static void addPresenterDetails(JSONObject root, ProjectJsonExportData data) throws JSONException {
        // שמאי מרכזי
        root.put("appraiser_name", safeString(data.getAppraiserName()));
        root.put("appraisal_date", safeString(data.getAppraisalDate()));
        root.put("appraiser_role", safeString(data.getAppraiserRole()));

        // פרטי מוסר (מתת-האוסף presenter_details)
        Map<String, Object> presenter = data.getPresenterDetails();
        if (presenter != null) {
            root.put("name_of_presenter", safeString(getMapValue(presenter, "name_of_presenter")));
            root.put("id_of_presenter", safeString(getMapValue(presenter, "id_of_presenter")));
            root.put("type_of_presenter_id", safeString(getMapValue(presenter, "type_of_presenter_id")));
            root.put("role_of_presenter", safeString(getMapValue(presenter, "role_of_presenter")));
            root.put("holder_status", safeString(getMapValue(presenter, "holder_status")));
        } else {
            root.put("name_of_presenter", "");
            root.put("id_of_presenter", "");
            root.put("type_of_presenter_id", "");
            root.put("role_of_presenter", "");
            root.put("holder_status", "");
        }
    }

    private static void addPropertyDetails(JSONObject root, ProjectJsonExportData data) throws JSONException {
        root.put("full_address", safeString(data.getFullAddress()));
        root.put("building_entry", safeString(data.getBuildingEntry()));
        root.put("building_number", safeString(data.getBuildingNumber()));
        root.put("zone_number", safeString(data.getZoneNumber()));
        root.put("building_city_plan_number", safeString(data.getBuildingCityPlanNumber()));
        root.put("environment_characteristics", safeString(data.getEnvironmentCharacteristics()));
        root.put("property_location", safeString(data.getPropertyLocation()));
        root.put("building_type", safeString(data.getBuildingType()));
        root.put("physical_condition", safeString(data.getPhysicalCondition()));
        root.put("maintenance", safeString(data.getMaintenance()));
        root.put("construction_material", safeString(data.getConstructionMaterial()));
        root.put("has_elevator", safeString(data.getHasElevator()));
        root.put("external_cladding", safeString(data.getExternalCladding()));
        root.put("number_of_floors", safeString(data.getNumberOfFloors()));

        // תאריך מסמך רישום - לא ברור מאיפה, נשים ריק
        root.put("registration_document_date", "");
    }

    private static void addImages(JSONObject root, ProjectJsonExportData data) throws JSONException {
        // חיפוש תמונות חזית ופנים מתוך הרשימה
        String frontImage = data.getFrontImageUrl();
        String interiorImage = data.getInteriorImageUrl();

        // אם לא הוגדרו, נחפש ברשימת התמונות
        List<Image> images = data.getAllImages();
        if (images != null) {
            for (Image img : images) {
                if (img != null && img.getCategory() != null) {
                    String category = img.getCategory().name();

                    // חזית
                    if (frontImage == null || frontImage.isEmpty()) {
                        if (category.equals("EXTERIOR") || category.equals("FRONT") || category.equals("ENTRANCE")) {
                            frontImage = img.getUrl();
                        }
                    }

                    // פנים (סלון או חדר שינה)
                    if (interiorImage == null || interiorImage.isEmpty()) {
                        if (category.equals("LIVING_ROOM") || category.equals("BEDROOM") || category.equals("INTERIOR")) {
                            interiorImage = img.getUrl();
                        }
                    }
                }
            }
        }

        root.put("front_image", safeString(frontImage));
        root.put("interior_image", safeString(interiorImage));
    }

    private static void addApartmentDetails(JSONObject root, ProjectJsonExportData data) throws JSONException {
        root.put("apartment_number(municipal_form)", safeString(data.getApartmentNumber()));
        root.put("apartment_story", safeString(data.getApartmentStory()));
        root.put("number_of_rooms", safeString(data.getNumberOfRooms()));
        root.put("apartment_includes", safeString(data.getApartmentIncludes()));
        root.put("apartment_renovations", safeString(data.getApartmentRenovations()));

        // כיווני אוויר - מרשימה למחרוזת
        List<String> directions = data.getApartmentDirections();
        if (directions != null && !directions.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < directions.size(); i++) {
                sb.append(directions.get(i));
                if (i < directions.size() - 1) {
                    sb.append(", ");
                }
            }
            root.put("apartment_directions", sb.toString());
        } else {
            root.put("apartment_directions", "");
        }

        root.put("apartment_kitchen", safeString(data.getApartmentKitchen()));
        root.put("apartment_flooring", safeString(data.getApartmentFlooring()));
        root.put("apartment_main_entrance_door", safeString(data.getApartmentMainEntranceDoor()));
        root.put("apartment_interior_doors_and_frames", safeString(data.getApartmentInteriorDoorsAndFrames()));
        root.put("apartment_bathroom_fixtures", safeString(data.getApartmentBathroomFixtures()));
        root.put("apartment_windows", safeString(data.getApartmentWindows()));
        root.put("has_bars", safeString(data.getHasBars()));
        root.put("apartment_air_conditioning", safeString(data.getApartmentAirConditioning()));
    }

    private static void addFeatures(JSONObject root, ProjectJsonExportData data) throws JSONException {
        root.put("central_heating_or_fireplace", data.isCentralHeatingOrFireplace());
        root.put("has_parking", data.isHasParking());
        root.put("has_storage", data.isHasStorage());
    }

    private static void addAreas(JSONObject root, ProjectJsonExportData data) throws JSONException {
        // הוספת מ"ר אם חסר
        String registered = data.getRegisteredApartmentArea();
        String gross = data.getGrossApartmentArea();

        if (registered != null && !registered.isEmpty() && !registered.contains("מ\"ר")) {
            registered = registered + " מ\"ר";
        }
        if (gross != null && !gross.isEmpty() && !gross.contains("מ\"ר")) {
            gross = gross + " מ\"ר";
        }

        root.put("registered_apartment_area", safeString(registered));
        root.put("gross_apartment_area", safeString(gross));
    }

    // =================== Helpers ===================

    private static String safeString(String value) {
        return value != null ? value : "";
    }

    private static String getMapValue(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return "";
        }
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
}
