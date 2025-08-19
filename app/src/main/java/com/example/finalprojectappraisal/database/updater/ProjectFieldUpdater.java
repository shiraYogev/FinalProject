package com.example.finalprojectappraisal.database.updater;

import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.database.constants.FirestoreConstants;

import java.util.List;
import java.util.Map;

/**
 * Utility class responsible for updating specific fields in Project objects.
 * Provides type-safe field updates with null checking and validation.
 */
public final class ProjectFieldUpdater {

    // Private constructor to prevent instantiation
    private ProjectFieldUpdater() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    /**
     * Updates property-related fields in the project
     */
    public static void updatePropertyFields(Project project, Map<String, Object> propertyDetails) {
        if (project == null || propertyDetails == null) {
            return;
        }

        updateStringField(project::setFullAddress, propertyDetails, FirestoreConstants.FIELD_FULL_ADDRESS);
        updateStringField(project::setLocation, propertyDetails, FirestoreConstants.FIELD_LOCATION);
        updateStringField(project::setBuildingType, propertyDetails, FirestoreConstants.FIELD_BUILDING_TYPE);
        updateStringField(project::setBuildingCondition, propertyDetails, FirestoreConstants.FIELD_BUILDING_CONDITION);
        updateStringField(project::setNumberOfFloors, propertyDetails, FirestoreConstants.FIELD_NUMBER_OF_FLOORS);
    }

    /**
     * Updates apartment-related fields in the project
     */
    public static void updateApartmentFields(Project project, Map<String, Object> apartmentDetails) {
        if (project == null || apartmentDetails == null) {
            return;
        }

        updateStringField(project::setApartmentNumber, apartmentDetails, FirestoreConstants.FIELD_APARTMENT_NUMBER);
        updateStringField(project::setFloorNumber, apartmentDetails, FirestoreConstants.FIELD_FLOOR_NUMBER);
        updateStringField(project::setNumberOfRooms, apartmentDetails, FirestoreConstants.FIELD_NUMBER_OF_ROOMS);
        updateStringField(project::setRegisteredArea, apartmentDetails, FirestoreConstants.FIELD_REGISTERED_AREA);
        updateStringField(project::setGrossArea, apartmentDetails, FirestoreConstants.FIELD_GROSS_AREA);
    }

    /**
     * Updates feature-related fields in the project
     */
    @SuppressWarnings("unchecked")
    public static void updateFeatureFields(Project project, Map<String, Object> features) {
        if (project == null || features == null) {
            return;
        }

        // Physical features
        updateStringField(project::setFlooringType, features, FirestoreConstants.FIELD_FLOORING_TYPE);
        updateStringField(project::setKitchenCondition, features, FirestoreConstants.FIELD_KITCHEN_CONDITION);
        updateStringField(project::setEntranceDoorCondition, features, FirestoreConstants.FIELD_ENTRANCE_DOOR_CONDITION);
        updateStringField(project::setInteriorDoorCondition, features, FirestoreConstants.FIELD_INTERIOR_DOOR_CONDITION);
        updateStringField(project::setWindowType, features, FirestoreConstants.FIELD_WINDOW_TYPE);

        updateBooleanField(project::setHasBars, features, FirestoreConstants.FIELD_HAS_BARS);

        // Handle air direction list
        if (features.containsKey(FirestoreConstants.FIELD_AIR_DIRECTION)) {
            Object airDirectionValue = features.get(FirestoreConstants.FIELD_AIR_DIRECTION);
            if (airDirectionValue instanceof List) {
                project.setAirDirection((List<String>) airDirectionValue);
            }
        }

        // Facilities
        updateBooleanField(project::setHasElevator, features, FirestoreConstants.FIELD_HAS_ELEVATOR);
        updateBooleanField(project::setHasStorageRoom, features, FirestoreConstants.FIELD_HAS_STORAGE_ROOM);
        updateStringField(project::setHasAirConditioning, features, FirestoreConstants.FIELD_HAS_AIR_CONDITIONING);
        updateBooleanField(project::setHasParking, features, FirestoreConstants.FIELD_HAS_PARKING);
        updateBooleanField(project::setHasCentralHeating, features, FirestoreConstants.FIELD_HAS_CENTRAL_HEATING);
    }





    // ========================= HELPER METHODS =========================

    /**
     * Safely updates a string field if the key exists and value is not null
     */
    private static void updateStringField(StringSetter setter, Map<String, Object> map, String key) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof String) {
                setter.set((String) value);
            }
        }
    }

    /**
     * Safely updates a boolean field if the key exists and value is not null
     */
    private static void updateBooleanField(BooleanSetter setter, Map<String, Object> map, String key) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Boolean) {
                setter.set((Boolean) value);
            }
        }
    }

    /**
     * Safely updates an integer field if the key exists and value is not null
     */
    private static void updateIntegerField(IntegerSetter setter, Map<String, Object> map, String key) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Number) {
                setter.set(((Number) value).intValue());
            }
        }
    }

    /**
     * Safely updates a double field if the key exists and value is not null
     */
    private static void updateDoubleField(DoubleSetter setter, Map<String, Object> map, String key) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Number) {
                setter.set(((Number) value).doubleValue());
            }
        }
    }

    // ========================= FUNCTIONAL INTERFACES =========================

    @FunctionalInterface
    public interface StringSetter {
        void set(String value);
    }

    @FunctionalInterface
    public interface BooleanSetter {
        void set(Boolean value);
    }

    @FunctionalInterface
    public interface IntegerSetter {
        void set(Integer value);
    }

    @FunctionalInterface
    public interface DoubleSetter {
        void set(Double value);
    }
}