package com.example.finalprojectappraisal.database.validator;

import com.example.finalprojectappraisal.model.Client;
import com.example.finalprojectappraisal.model.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validator class for project-related data validation.
 * Ensures data integrity before database operations.
 */
public final class ProjectDataValidator {

    // Private constructor to prevent instantiation
    private ProjectDataValidator() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    // ========================= PROJECT VALIDATION =========================

    /**
     * Validates a complete project object
     */
    public static ValidationResult validateProject(Project project) {
        List<String> errors = new ArrayList<>();

        if (project == null) {
            errors.add("Project cannot be null");
            return new ValidationResult(false, errors);
        }

        // Validate basic project information
//        if (isNullOrEmpty(project.getProjectId())) {
//            errors.add("Project ID is required");
//        }

        if (isNullOrEmpty(project.getFullAddress())) {
            errors.add("Full address is required");
        }

        // Validate client if present
        if (project.getClient() != null) {
            ValidationResult clientResult = validateClient(project.getClient());
            if (!clientResult.isValid()) {
                errors.addAll(clientResult.getErrors());
            }
        }

        // Validate numeric fields
        if (project.getNumberOfFloors() != null) {
            errors.add("Number of floors cannot be negative");
        }

        if (project.getFloorNumber() != null) {
            errors.add("Floor number cannot be negative");
        }

        if (project.getNumberOfRooms() != null) {
            errors.add("Number of rooms cannot be negative");
        }

        if (project.getRegisteredArea() != null) {
            errors.add("Registered area cannot be negative");
        }

        if (project.getGrossArea() != null ) {
            errors.add("Gross area cannot be negative");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    // ========================= CLIENT VALIDATION =========================

    /**
     * Validates client information
     */
    public static ValidationResult validateClient(Client client) {
        List<String> errors = new ArrayList<>();

        if (client == null) {
            errors.add("Client cannot be null");
            return new ValidationResult(false, errors);
        }

        if (isNullOrEmpty(client.getFullName())) {
            errors.add("Client first name is required");
        }

        // Validate phone number format (basic validation)
        if (!isNullOrEmpty(client.getPhoneNumber())) {
            if (!isValidPhoneNumber(client.getPhoneNumber())) {
                errors.add("Invalid phone number format");
            }
        }

        // Validate email format (basic validation)
        if (!isNullOrEmpty(client.getEmail())) {
            if (!isValidEmail(client.getEmail())) {
                errors.add("Invalid email format");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    // ========================= PROPERTY DETAILS VALIDATION =========================

    /**
     * Validates property details map
     */
    public static ValidationResult validatePropertyDetails(Map<String, Object> propertyDetails) {
        List<String> errors = new ArrayList<>();

        if (propertyDetails == null || propertyDetails.isEmpty()) {
            errors.add("Property details cannot be null or empty");
            return new ValidationResult(false, errors);
        }

        // Validate required fields
        if (!propertyDetails.containsKey("fullAddress") ||
                isNullOrEmpty((String) propertyDetails.get("fullAddress"))) {
            errors.add("Full address is required in property details");
        }

        // Validate numeric fields
        if (propertyDetails.containsKey("numberOfFloors")) {
            Object floors = propertyDetails.get("numberOfFloors");
            if (floors instanceof Number && ((Number) floors).intValue() < 0) {
                errors.add("Number of floors cannot be negative");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    // ========================= APARTMENT DETAILS VALIDATION =========================

    /**
     * Validates apartment details map
     */
    public static ValidationResult validateApartmentDetails(Map<String, Object> apartmentDetails) {
        List<String> errors = new ArrayList<>();

        if (apartmentDetails == null || apartmentDetails.isEmpty()) {
            errors.add("Apartment details cannot be null or empty");
            return new ValidationResult(false, errors);
        }

        // Validate numeric fields
        validateNumericField(apartmentDetails, "floorNumber", "Floor number", errors);
        validateNumericField(apartmentDetails, "numberOfRooms", "Number of rooms", errors);
        validateNumericField(apartmentDetails, "registeredArea", "Registered area", errors);
        validateNumericField(apartmentDetails, "grossArea", "Gross area", errors);

        return new ValidationResult(errors.isEmpty(), errors);
    }

    // ========================= FEATURES VALIDATION =========================

    /**
     * Validates property features map
     */
    public static ValidationResult validatePropertyFeatures(Map<String, Object> features) {
        List<String> errors = new ArrayList<>();

        if (features == null || features.isEmpty()) {
            errors.add("Property features cannot be null or empty");
            return new ValidationResult(false, errors);
        }

        // Validate air direction list if present
        if (features.containsKey("airDirection")) {
            Object airDirection = features.get("airDirection");
            if (airDirection != null && !(airDirection instanceof List)) {
                errors.add("Air direction must be a list");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    // ========================= HELPER METHODS =========================

    /**
     * Checks if a string is null or empty
     */
    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Basic phone number validation (Israeli format)
     */
    private static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return false;

        // Remove spaces and dashes
        String cleanNumber = phoneNumber.replaceAll("[\\s-]", "");

        // Israeli phone number patterns
        return cleanNumber.matches("^05\\d{8}$") ||  // Mobile: 05X-XXXXXXX
                cleanNumber.matches("^0[2-4,8-9]\\d{7}$") ||  // Landline: 0X-XXXXXXX
                cleanNumber.matches("^\\+972[2-9]\\d{7,8}$");  // International format
    }

    /**
     * Basic email validation
     */
    private static boolean isValidEmail(String email) {
        if (email == null) return false;

        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Validates a numeric field in a map
     */
    private static void validateNumericField(Map<String, Object> map, String fieldName,
                                             String displayName, List<String> errors) {
        if (map.containsKey(fieldName)) {
            Object value = map.get(fieldName);
            if (value instanceof Number && ((Number) value).doubleValue() < 0) {
                errors.add(displayName + " cannot be negative");
            }
        }
    }

    // ========================= VALIDATION RESULT CLASS =========================

    /**
     * Represents the result of a validation operation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public String getErrorsAsString() {
            return String.join(", ", errors);
        }

        @Override
        public String toString() {
            return "ValidationResult{" +
                    "valid=" + valid +
                    ", errors=" + errors +
                    '}';
        }
    }
}