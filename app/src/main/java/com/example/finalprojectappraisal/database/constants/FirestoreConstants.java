package com.example.finalprojectappraisal.database.constants;

/**
 * Constants class containing all Firestore-related constant values.
 * This centralizes all string literals and configuration values.
 */
public final class FirestoreConstants {

    // Private constructor to prevent instantiation
    private FirestoreConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    // Collection Names
    public static final String COLLECTION_PROJECTS = "projects";
    public static final String COLLECTION_CLIENTS = "clients";

    // Field Names - Project
    public static final String FIELD_PROJECT_ID = "projectId";
    public static final String FIELD_PROJECT_STATUS = "projectStatus";
    public static final String FIELD_FULL_ADDRESS = "fullAddress";
    public static final String FIELD_LOCATION = "location";
    public static final String FIELD_BUILDING_TYPE = "buildingType";
    public static final String FIELD_BUILDING_CONDITION = "buildingCondition";
    public static final String FIELD_NUMBER_OF_FLOORS = "numberOfFloors";

    // Field Names - Apartment
    public static final String FIELD_APARTMENT_NUMBER = "apartmentNumber";
    public static final String FIELD_FLOOR_NUMBER = "floorNumber";
    public static final String FIELD_NUMBER_OF_ROOMS = "numberOfRooms";
    public static final String FIELD_REGISTERED_AREA = "registeredArea";
    public static final String FIELD_GROSS_AREA = "grossArea";

    // Field Names - Features
    public static final String FIELD_FLOORING_TYPE = "flooringType";
    public static final String FIELD_KITCHEN_CONDITION = "kitchenCondition";
    public static final String FIELD_ENTRANCE_DOOR_CONDITION = "entranceDoorCondition";
    public static final String FIELD_INTERIOR_DOOR_CONDITION = "interiorDoorCondition";
    public static final String FIELD_WINDOW_TYPE = "windowType";
    public static final String FIELD_HAS_BARS = "hasBars";
    public static final String FIELD_AIR_DIRECTION = "airDirection";
    public static final String FIELD_HAS_ELEVATOR = "hasElevator";
    public static final String FIELD_HAS_STORAGE_ROOM = "hasStorageRoom";
    public static final String FIELD_HAS_AIR_CONDITIONING = "hasAirConditioning";
    public static final String FIELD_HAS_PARKING = "hasParking";
    public static final String FIELD_HAS_CENTRAL_HEATING = "hasCentralHeating";

    // Error Messages (Hebrew)
    public static final String ERROR_PROJECT_NOT_FOUND = "פרויקט לא קיים";
    public static final String ERROR_LOADING_PROJECT = "שגיאה בטעינת פרויקט";
    public static final String ERROR_LOADING_PROJECTS = "שגיאה בטעינת פרויקטים";
    public static final String ERROR_CREATING_PROJECT = "שגיאה ביצירת פרויקט";
    public static final String ERROR_UPDATING_PROJECT = "שגיאה בעדכון פרויקט";
    public static final String ERROR_DELETING_PROJECT = "שגיאה במחיקת פרויקט";
    public static final String ERROR_SAVING_PROJECT = "שגיאה בשמירת פרויקט";
    public static final String ERROR_UPDATING_STATUS = "שגיאה בעדכון סטטוס";
    public static final String ERROR_PROJECT_NOT_FOUND_WITH_ID = "לא נמצא פרויקט עם המזהה ";

    // Project Status Values
    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_IN_PROGRESS = "in_progress";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";
}