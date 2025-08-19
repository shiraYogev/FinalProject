package com.example.finalprojectappraisal.utils;

/**
 * The `Constants` class serves as a centralized location for all the constant values used throughout the application.
 *
 * By defining constants in one place, we ensure consistency and reduce the risk of errors due to duplicated values across the codebase.
 * This class holds values for API URLs, project statuses, error messages, and default settings that are used across different parts of the application.
 *
 * Using a dedicated class for constants also improves maintainability, as any updates to these values can be made in a single location,
 * without having to modify them in multiple places in the code.
 */
public class Constants {

    /**
     * Base URL for the API.
     * This is the root URL used for all API calls in the application.
     * It acts as the starting point from which specific endpoints are appended to make full API request URLs.
     * For example, to get all projects, the full URL might be: API_BASE_URL + "projects".
     */
    public static final String API_BASE_URL = "https://api.example.com/";

    /**
     * Project Status Constants.
     * These constants represent the possible stages of a project in the system.
     * They are used to track the current state of each project throughout its lifecycle.
     * Using constants for these values ensures that the status names are consistent across the application.
     *
     * The statuses are used to update the status of a project:
     * - `PROJECT_STATUS_DRAFT`: When the project is still being created and not yet submitted for review.
     * - `PROJECT_STATUS_READY_FOR_REVIEW`: When the project is completed and ready to be reviewed.
     * - `PROJECT_STATUS_COMPLETED`: When the project has been finished, and the appraisal process is complete.
     */
    public static final String PROJECT_STATUS_DRAFT = "Draft";  // The project is in the draft stage and not yet submitted for review.
    public static final String PROJECT_STATUS_READY_FOR_REVIEW = "Ready for review";  // The project is complete and awaiting review.
    public static final String PROJECT_STATUS_COMPLETED = "Completed";  // The project has been finished and appraisal has been completed.

    /**
     * Common Error Messages.
     * These constants store standard error messages used throughout the application.
     * They provide clear, user-friendly feedback when certain actions or inputs are invalid.
     * Centralizing error messages helps in maintaining consistency and simplifies any future changes to error text.
     *
     * Example usage:
     * - When an invalid email is entered, the message `ERROR_INVALID_EMAIL` will be displayed.
     * - When there is a network issue, the message `ERROR_NETWORK_FAILURE` will be shown.
     */
    public static final String ERROR_INVALID_EMAIL = "Invalid email format.";  // Message shown when the email provided does not match the correct format.
    public static final String ERROR_NETWORK_FAILURE = "Network error, please try again.";  // Message shown when there is an issue with the network connection.

    /**
     * Default Settings.
     * These constants store default values used in the application for configuration purposes.
     * For example, the default page size is used to control how many items are displayed per page in paginated views.
     *
     * The default settings are applied to make the application more efficient and consistent.
     * Example:
     * - `DEFAULT_PAGE_SIZE`: Controls how many items will appear on each page of a list or a search result.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;  // Default number of items to display per page in pagination.


    /**
     * Binary Feature Types Constants.
     * These constants represent binary features that either exist or do not exist (true/false).
     * For example:
     * - `elevator`: Is there an elevator in the building? (exists or not exists)
     * - `storage`: Does the property have storage space? (exists or not exists)
     * - `parking`: Does the property have parking space? (exists or not exists)
     * - `airCondition`: Is there air conditioning in the property? (exists or not exists)
     * - `centralHeating`: Does the property have central heating? (exists or not exists)
     */
    public static final String TYPE_ELEVATOR = "elevator";       // Elevator
    public static final String TYPE_STORAGE = "storage";         // Storage
    public static final String TYPE_PARKING = "parking";         // Parking
    public static final String TYPE_AIR_CONDITION = "airCondition"; // Air conditioning
    public static final String TYPE_CENTRAL_HEATING = "centralHeating"; // Central heating

    /**
     * Categorized Feature Types Constants.
     * These constants represent feature types with categorized values.
     * For example:
     * - `floorType`: The type of flooring (e.g., ceramic, wood, marble).
     * - `kitchenState`: The condition of the kitchen (e.g., modern, old).
     * - `doorType`: The type of door (e.g., wooden, glass, metal).
     * - `windowType`: The type of window (e.g., sliding, casement).
     * - `windowBars`: Whether the property has window bars or not.
     * - `airDirection`: The direction of airflow in the property (e.g., North, South).
     */
    public static final String TYPE_FLOOR = "floorType";          // Floor type
    public static final String TYPE_KITCHEN = "kitchenState";     // Kitchen condition
    public static final String TYPE_DOOR = "doorType";            // Door type
    public static final String TYPE_WINDOW = "windowType";        // Window type
    public static final String TYPE_WINDOW_BARS = "windowBars";   // Window bars
    public static final String TYPE_AIR_DIRECTION = "airDirection"; // Air direction

    /**
     * Feature Values Constants (for Binary Tags).
     * These constants represent possible values for binary features (exists or does not exist).
     * They are used for setting the binary state of a feature.
     *
     * Example usage:
     * - `VALUE_EXISTS`: Indicates that the feature is present (e.g., an elevator is present).
     * - `VALUE_NOT_EXISTS`: Indicates that the feature is absent (e.g., no parking space).
     */
    public static final String VALUE_EXISTS = "exists";           // Exists
    public static final String VALUE_NOT_EXISTS = "notExists";    // Does not exist
}
