package com.example.finalprojectappraisal.model;

import com.google.firebase.firestore.PropertyName;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Project class represents a property appraisal project in the system. Each project involves
 * evaluating multiple properties, and the appraiser evaluates and generates reports for the properties under
 * this project.
 *
 * This class stores details about the project, including its name, description, status, and the properties
 * associated with the project. A project can have multiple properties and may be assigned to specific appraisers
 * for evaluation.
 */

public class Project {

    // Project identification
    private String projectId;

    // Client and appraiser details
    private Client client; // The client requesting the appraisal
    //private Appraiser appraiser; // The appraiser performing the appraisal
    @SerializedName("appraiser_id")
    private String appraiserId;


    // Creation and update timestamps
    private long creationDate;
    private long lastUpdateDate;

    // Property details
    @SerializedName("full_address")
    private String fullAddress;

    @SerializedName("property_location")
    private String location;

    @SerializedName("building_type")
    private String buildingType;

    @SerializedName("physical_condition")
    private String buildingCondition;

    @SerializedName("number_of_floors")
    private String numberOfFloors;

    @SerializedName("building_entry")
    private String buildingEntry;

    @SerializedName("building_number")
    private String buildingNumber;

    @SerializedName("zone_number")
    private String zoneNumber;

    @SerializedName("building_city_plan_number")
    private String buildingCityPlanNumber;

    @SerializedName("environment_characteristics")
    private String environmentCharacteristics;

    @SerializedName("maintenance")
    private String maintenance;

    @SerializedName("construction_material")
    private String constructionMaterial;


    // Apartment details
    @SerializedName("apartment_number(municipal_form)")
    private String apartmentNumber;

    @SerializedName("apartment_story")
    private String floorNumber;

    @SerializedName("number_of_rooms")
    private String numberOfRooms;

    @SerializedName("registered_apartment_area")
    private String registeredArea;

    @SerializedName("gross_apartment_area")
    private String grossArea;


    // Physical features of the apartment
    @SerializedName("apartment_flooring")
    private String flooringType;

    @SerializedName("apartment_kitchen")
    private String kitchenCondition;

    @SerializedName("apartment_main_entrance_door")
    private String entranceDoorCondition;

    @SerializedName("apartment_interior_doors_and_frames")
    private String interiorDoorCondition;

    @SerializedName("apartment_windows")
    private String windowType;

    @SerializedName("has_bars")
    private boolean hasBars;

    @SerializedName("apartment_directions")
    private List<String> airDirection;


    // Additional facilities and services
    @SerializedName("has_elevator")
    private boolean hasElevator;

    @SerializedName("has_storage")
    private boolean hasStorageRoom;

    @SerializedName("apartment_air_conditioning")
    private String hasAirConditioning;

    @SerializedName("has_parking")
    private boolean hasParking;

    @SerializedName("central_heating_or_fireplace")
    private boolean hasCentralHeating;

    @SerializedName("apartment_includes")
    private String apartmentIncludes;

    @SerializedName("apartment_bathroom_fixtures")
    private String bathroomFixtures;


    // Property images
    private List<Image> propertyImages; // List of property images

    // Additional fields
    private String projectStatus; // Status of the project (e.g., in progress, completed)
    @SerializedName("property_summary")
    private String projectDescription;



    //////////     Constructors:     ////////////

    /**
     * Default constructor for Firestore
     */
    public Project() {
        this.creationDate = System.currentTimeMillis();
        this.lastUpdateDate = System.currentTimeMillis();
        this.projectStatus = "In Progress";
        this.propertyImages = new ArrayList<Image>();
    }

    /**
     * Constructor with required fields for initial project creation
     */
    public Project(String projectId) {
        this();
        this.projectId = projectId;
    }

    /**
     * Constructor with all fields for complete project initialization
     */
    public Project(String projectId, Client client, String appraiserId, String fullAddress, String location,
                   String buildingType, String buildingCondition, String numberOfFloors,
                   String apartmentNumber, String floorNumber, String numberOfRooms,
                   String registeredArea, String grossArea,
                   String flooringType, String kitchenCondition,
                   String entranceDoorCondition, String interiorDoorCondition,
                   String windowType, boolean hasBars, List<String> airDirection,
                   boolean hasElevator, boolean hasStorageRoom,
                   String hasAirConditioning, boolean hasParking,
                   boolean hasCentralHeating, String apartmentIncludes,
                   String bathroomFixtures) {

        this(); // Initialize default values

        this.projectId = projectId;
        this.client = client;
        this.appraiserId = appraiserId;
        this.fullAddress = fullAddress;
        this.location = location;
        this.buildingType = buildingType;
        this.buildingCondition = buildingCondition;
        this.numberOfFloors = numberOfFloors;
        this.apartmentNumber = apartmentNumber;
        this.floorNumber = floorNumber;
        this.numberOfRooms = numberOfRooms;
        this.registeredArea = registeredArea;
        this.grossArea = grossArea;
        this.flooringType = flooringType;
        this.kitchenCondition = kitchenCondition;
        this.entranceDoorCondition = entranceDoorCondition;
        this.interiorDoorCondition = interiorDoorCondition;
        this.windowType = windowType;
        this.hasBars = hasBars;
        this.airDirection = airDirection;
        this.hasElevator = hasElevator;
        this.hasStorageRoom = hasStorageRoom;
        this.hasAirConditioning = hasAirConditioning;
        this.hasParking = hasParking;
        this.hasCentralHeating = hasCentralHeating;
        this.apartmentIncludes = apartmentIncludes;
        this.bathroomFixtures = bathroomFixtures;
    }
    // Constructor to initialize the project details
    public Project(String projectId, Client client, String appraiserId, String fullAddress, String location,
                   String buildingType, String buildingCondition, String numberOfFloors) {
        this.projectId = projectId;
        this.client = client;
        this.appraiserId = appraiserId;
        this.fullAddress = fullAddress;
        this.location = location;
        this.buildingType = buildingType;
        this.buildingCondition = buildingCondition;
        this.numberOfFloors = numberOfFloors;
        // שאר השדות (אם רוצים אפשר להוסיף כאן בהמשך)
    }

    public void updateLastUpdateDate() {
        this.lastUpdateDate = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
        updateLastUpdateDate();
    }


    public String getAppraiserId() {
        return appraiserId;
    }

    public void setAppraiserId(String appraiserId) {
        this.appraiserId = appraiserId;
    }


    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public long getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(long lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    @PropertyName("full_address")
    public String getFullAddress() {
        return fullAddress;
    }

    @PropertyName("full_address")
    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
        updateLastUpdateDate();
    }

    @PropertyName("property_location")
    public String getLocation() {
        return location;
    }

    @PropertyName("property_location")
    public void setLocation(String location) {
        this.location = location;
        updateLastUpdateDate();
    }

    @PropertyName("building_type")
    public String getBuildingType() {
        return buildingType;
    }

    @PropertyName("building_type")
    public void setBuildingType(String buildingType) {
        this.buildingType = buildingType;
        updateLastUpdateDate();
    }

    @PropertyName("physical_condition")
    public String getBuildingCondition() {
        return buildingCondition;
    }

    @PropertyName("physical_condition")
    public void setBuildingCondition(String buildingCondition) {
        this.buildingCondition = buildingCondition;
        updateLastUpdateDate();
    }

    @PropertyName("number_of_floors")
    public String getNumberOfFloors() {
        return numberOfFloors;
    }

    @PropertyName("number_of_floors")
    public void setNumberOfFloors(String numberOfFloors) {
        this.numberOfFloors = numberOfFloors;
        updateLastUpdateDate();
    }

    @PropertyName("apartment_number(municipal_form)")
    public String getApartmentNumber() {
        return apartmentNumber;
    }

    @PropertyName("apartment_number(municipal_form)")
    public void setApartmentNumber(String apartmentNumber) {
        this.apartmentNumber = apartmentNumber;
        updateLastUpdateDate();
    }

    @PropertyName("apartment_story")
    public String getFloorNumber() {
        return floorNumber;
    }

    @PropertyName("apartment_story")
    public void setFloorNumber(String floorNumber) {
        this.floorNumber = floorNumber;
        updateLastUpdateDate();
    }

    @PropertyName("number_of_rooms")
    public String getNumberOfRooms() {
        return numberOfRooms;
    }

    @PropertyName("number_of_rooms")
    public void setNumberOfRooms(String numberOfRooms) {
        this.numberOfRooms = numberOfRooms;
        updateLastUpdateDate();
    }

    @PropertyName("registered_apartment_area")
    public String getRegisteredArea() {
        return registeredArea;
    }

    @PropertyName("registered_apartment_area")
    public void setRegisteredArea(String registeredArea) {
        this.registeredArea = registeredArea;
        updateLastUpdateDate();
    }

    @PropertyName("gross_apartment_area")
    public String getGrossArea() {
        return grossArea;
    }

    @PropertyName("gross_apartment_area")
    public void setGrossArea(String grossArea) {
        this.grossArea = grossArea;
        updateLastUpdateDate();
    }

    @PropertyName("apartment_flooring")
    public String getFlooringType() {
        return flooringType;
    }

    @PropertyName("apartment_flooring")
    public void setFlooringType(String flooringType) {
        this.flooringType = flooringType;
        updateLastUpdateDate();
    }

    @PropertyName("apartment_kitchen")
    public String getKitchenCondition() {
        return kitchenCondition;
    }

    @PropertyName("apartment_kitchen")
    public void setKitchenCondition(String kitchenCondition) {
        this.kitchenCondition = kitchenCondition;
        updateLastUpdateDate();
    }

    @PropertyName("apartment_main_entrance_door")
    public String getEntranceDoorCondition() {
        return entranceDoorCondition;
    }

    @PropertyName("apartment_main_entrance_door")
    public void setEntranceDoorCondition(String entranceDoorCondition) {
        this.entranceDoorCondition = entranceDoorCondition;
        updateLastUpdateDate();
    }

    @PropertyName("apartment_interior_doors_and_frames")
    public String getInteriorDoorCondition() {
        return interiorDoorCondition;
    }

    @PropertyName("apartment_interior_doors_and_frames")
    public void setInteriorDoorCondition(String interiorDoorCondition) {
        this.interiorDoorCondition = interiorDoorCondition;
        updateLastUpdateDate();
    }

    @PropertyName("apartment_windows")
    public String getWindowType() {
        return windowType;
    }

    @PropertyName("apartment_windows")
    public void setWindowType(String windowType) {
        this.windowType = windowType;
        updateLastUpdateDate();
    }

    @PropertyName("has_bars")
    public boolean isHasBars() {
        return hasBars;
    }

    @PropertyName("has_bars")
    public void setHasBars(boolean hasBars) {
        this.hasBars = hasBars;
    }

    @PropertyName("apartment_directions")
    public List<String> getAirDirection() {
        return airDirection;
    }

    @PropertyName("apartment_directions")
    public void setAirDirection(List<String> airDirection) {
        this.airDirection = airDirection;
        updateLastUpdateDate();
    }

    public String getAirDirectionAsString() {
        return airDirection == null ? "" : String.join(", ", airDirection);
    }

    public void setAirDirectionFromString(String s) {
        if (s == null || s.trim().isEmpty()) { airDirection = new ArrayList<>(); return; }
        airDirection = Arrays.stream(s.split(","))
                .map(String::trim).filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }


    @PropertyName("has_elevator")
    public boolean isHasElevator() {
        return hasElevator;
    }

    @PropertyName("has_elevator")
    public void setHasElevator(boolean hasElevator) {
        this.hasElevator = hasElevator;
        updateLastUpdateDate();
    }

    @PropertyName("has_storage")
    public boolean isHasStorageRoom() {
        return hasStorageRoom;
    }

    @PropertyName("has_storage")
    public void setHasStorageRoom(boolean hasStorageRoom) {
        this.hasStorageRoom = hasStorageRoom;
        updateLastUpdateDate();
    }

    @PropertyName("apartment_air_conditioning")
    public String isHasAirConditioning() {
        return hasAirConditioning;
    }

    @PropertyName("apartment_air_conditioning")
    public void setHasAirConditioning(String hasAirConditioning) {
        this.hasAirConditioning = hasAirConditioning;
        updateLastUpdateDate();
    }

    @PropertyName("has_parking")
    public boolean isHasParking() {
        return hasParking;
    }

    @PropertyName("has_parking")
    public void setHasParking(boolean hasParking) {
        this.hasParking = hasParking;
        updateLastUpdateDate();
    }

    @PropertyName("central_heating_or_fireplace")
    public boolean isHasCentralHeating() {
        return hasCentralHeating;
    }

    @PropertyName("central_heating_or_fireplace")
    public void setHasCentralHeating(boolean hasCentralHeating) {
        this.hasCentralHeating = hasCentralHeating;
        updateLastUpdateDate();
    }

    public List<Image> getPropertyImages() {
        return propertyImages;
    }

    public void setPropertyImages(List<Image> propertyImages) {
        this.propertyImages = propertyImages;
        updateLastUpdateDate();
    }

    public void addPropertyImage(Image image) {
        if (this.propertyImages == null) {
            this.propertyImages = new ArrayList<>();
        }
        this.propertyImages.add(image);
        updateLastUpdateDate();
    }

    public String getProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(String projectStatus) {
        this.projectStatus = projectStatus;
        updateLastUpdateDate();
    }

    @PropertyName("property_summary")
    public String getProjectDescription() {
        return projectDescription;
    }

    @PropertyName("property_summary")
    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
        updateLastUpdateDate();
    }

    @PropertyName("apartment_includes")
    public String getApartmentIncludes() {
        return apartmentIncludes;
    }

    @PropertyName("apartment_includes")
    public void setApartmentIncludes(String apartmentIncludes) {
        this.apartmentIncludes = apartmentIncludes;
        updateLastUpdateDate();
    }

    @PropertyName("apartment_bathroom_fixtures")
    public String getBathroomFixtures() {
        return bathroomFixtures;
    }

    @PropertyName("apartment_bathroom_fixtures")
    public void setBathroomFixtures(String bathroomFixtures) {
        this.bathroomFixtures = bathroomFixtures;
        updateLastUpdateDate();
    }

    public String getBuildingEntry() { return buildingEntry; }
    public void setBuildingEntry(String buildingEntry) { this.buildingEntry = buildingEntry; }

    public String getBuildingNumber() { return buildingNumber; }
    public void setBuildingNumber(String buildingNumber) { this.buildingNumber = buildingNumber; }

    public String getZoneNumber() { return zoneNumber; }
    public void setZoneNumber(String zoneNumber) { this.zoneNumber = zoneNumber; }

    public String getBuildingCityPlanNumber() { return buildingCityPlanNumber; }
    public void setBuildingCityPlanNumber(String buildingCityPlanNumber) { this.buildingCityPlanNumber = buildingCityPlanNumber; }

    public String getEnvironmentCharacteristics() { return environmentCharacteristics; }
    public void setEnvironmentCharacteristics(String environmentCharacteristics) { this.environmentCharacteristics = environmentCharacteristics; }

    public String getMaintenance() { return maintenance; }
    public void setMaintenance(String maintenance) { this.maintenance = maintenance; }

    public String getConstructionMaterial() { return constructionMaterial; }
    public void setConstructionMaterial(String constructionMaterial) { this.constructionMaterial = constructionMaterial; }

    @Override
    public String toString() {
        return "Project{" +
                "projectId='" + projectId + '\'' +
                ", client=" + (client != null ? client.getFullName() : "null") +
                ", appraiserId='" + (appraiserId != null ? appraiserId : "null") + '\'' +
                ", creationDate=" + creationDate +
                ", lastUpdateDate=" + lastUpdateDate +
                ", fullAddress='" + fullAddress + '\'' +
                ", location='" + location + '\'' +
                ", buildingType='" + buildingType + '\'' +
                ", buildingCondition='" + buildingCondition + '\'' +
                ", numberOfFloors='" + numberOfFloors + '\'' +
                ", apartmentNumber='" + apartmentNumber + '\'' +
                ", floorNumber='" + floorNumber + '\'' +
                ", numberOfRooms='" + numberOfRooms + '\'' +
                ", registeredArea='" + registeredArea + '\'' +
                ", grossArea='" + grossArea + '\'' +
                ", flooringType='" + flooringType + '\'' +
                ", kitchenCondition='" + kitchenCondition + '\'' +
                ", entranceDoorCondition='" + entranceDoorCondition + '\'' +
                ", interiorDoorCondition='" + interiorDoorCondition + '\'' +
                ", windowType='" + windowType + '\'' +
                ", hasBars=" + hasBars +
                ", airDirection=" + airDirection +
                ", hasElevator=" + hasElevator +
                ", hasStorageRoom=" + hasStorageRoom +
                ", hasAirConditioning=" + hasAirConditioning +
                ", hasParking=" + hasParking +
                ", hasCentralHeating=" + hasCentralHeating +
                ", apartmentIncludes='" + apartmentIncludes + '\'' +
                ", bathroomFixtures='" + bathroomFixtures + '\'' +
                ", projectStatus='" + projectStatus + '\'' +
                ", projectDescription='" + projectDescription + '\'' +
                ", propertyImages=" + propertyImages +
                '}';
    }

}