package com.example.finalprojectappraisal.export;

import com.example.finalprojectappraisal.model.BankDetails;
import com.example.finalprojectappraisal.model.Client;
import com.example.finalprojectappraisal.model.Image;

import java.util.List;
import java.util.Map;

/**
 * מחלקה שמכילה את כל הנתונים הדרושים ליצוא JSON עבור אתר חיצוני.
 * אוספת נתונים מכל מקורות המידע השונים בפרויקט.
 */
public class ProjectJsonExportData {

    private String projectId;

    // Bank Details (מהקולקציה bank_details בפרויקט)
    private BankDetails bankDetails;

    // Presenter Details (מתת-האוסף presenter_details)
    private Map<String, Object> presenterDetails;

    // Appraiser info (משדות ישירים בפרויקט)
    private String appraiserName;
    private String appraisalDate;
    private String appraiserRole;

    // Client info
    private Client client;

    // Property details (משדות ישירים בפרויקט)
    private String propertySummary;
    private String fullAddress;
    private String buildingEntry;
    private String buildingNumber;
    private String zoneNumber;
    private String buildingCityPlanNumber;
    private String environmentCharacteristics;
    private String propertyLocation;
    private String buildingType;
    private String physicalCondition;
    private String maintenance;
    private String constructionMaterial;
    private String hasElevator;
    private String externalCladding;
    private String numberOfFloors;
    private String apartmentNumber;
    private String apartmentStory;
    private String numberOfRooms;
    private String apartmentIncludes;
    private List<String> apartmentDirections;
    private String apartmentKitchen;
    private String apartmentFlooring;
    private String apartmentMainEntranceDoor;
    private String apartmentInteriorDoorsAndFrames;
    private String apartmentBathroomFixtures;
    private String apartmentWindows;
    private String hasBars;
    private boolean centralHeatingOrFireplace;
    private boolean hasParking;
    private boolean hasStorage;
    private String apartmentAirConditioning;
    private String registeredApartmentArea;
    private String grossApartmentArea;

    // Tabu image URL
    private String tabuCropImage;

    // Images categorized
    private List<Image> allImages;
    private String frontImageUrl;
    private String interiorImageUrl;

    // Metadata
    private long exportTimestamp;
    private String exportStatus; // "pending", "completed", "failed"

    public ProjectJsonExportData() {
        this.exportTimestamp = System.currentTimeMillis();
        this.exportStatus = "pending";
    }

    // =================== GETTERS & SETTERS ===================

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public BankDetails getBankDetails() {
        return bankDetails;
    }

    public void setBankDetails(BankDetails bankDetails) {
        this.bankDetails = bankDetails;
    }

    public Map<String, Object> getPresenterDetails() {
        return presenterDetails;
    }

    public void setPresenterDetails(Map<String, Object> presenterDetails) {
        this.presenterDetails = presenterDetails;
    }

    public String getAppraiserName() {
        return appraiserName;
    }

    public void setAppraiserName(String appraiserName) {
        this.appraiserName = appraiserName;
    }

    public String getAppraisalDate() {
        return appraisalDate;
    }

    public void setAppraisalDate(String appraisalDate) {
        this.appraisalDate = appraisalDate;
    }

    public String getAppraiserRole() {
        return appraiserRole;
    }

    public void setAppraiserRole(String appraiserRole) {
        this.appraiserRole = appraiserRole;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getPropertySummary() {
        return propertySummary;
    }

    public void setPropertySummary(String propertySummary) {
        this.propertySummary = propertySummary;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
    }

    public String getBuildingEntry() {
        return buildingEntry;
    }

    public void setBuildingEntry(String buildingEntry) {
        this.buildingEntry = buildingEntry;
    }

    public String getBuildingNumber() {
        return buildingNumber;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    public String getZoneNumber() {
        return zoneNumber;
    }

    public void setZoneNumber(String zoneNumber) {
        this.zoneNumber = zoneNumber;
    }

    public String getBuildingCityPlanNumber() {
        return buildingCityPlanNumber;
    }

    public void setBuildingCityPlanNumber(String buildingCityPlanNumber) {
        this.buildingCityPlanNumber = buildingCityPlanNumber;
    }

    public String getEnvironmentCharacteristics() {
        return environmentCharacteristics;
    }

    public void setEnvironmentCharacteristics(String environmentCharacteristics) {
        this.environmentCharacteristics = environmentCharacteristics;
    }

    public String getPropertyLocation() {
        return propertyLocation;
    }

    public void setPropertyLocation(String propertyLocation) {
        this.propertyLocation = propertyLocation;
    }

    public String getBuildingType() {
        return buildingType;
    }

    public void setBuildingType(String buildingType) {
        this.buildingType = buildingType;
    }

    public String getPhysicalCondition() {
        return physicalCondition;
    }

    public void setPhysicalCondition(String physicalCondition) {
        this.physicalCondition = physicalCondition;
    }

    public String getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(String maintenance) {
        this.maintenance = maintenance;
    }

    public String getConstructionMaterial() {
        return constructionMaterial;
    }

    public void setConstructionMaterial(String constructionMaterial) {
        this.constructionMaterial = constructionMaterial;
    }

    public String getHasElevator() {
        return hasElevator;
    }

    public void setHasElevator(String hasElevator) {
        this.hasElevator = hasElevator;
    }

    public String getExternalCladding() {
        return externalCladding;
    }

    public void setExternalCladding(String externalCladding) {
        this.externalCladding = externalCladding;
    }

    public String getNumberOfFloors() {
        return numberOfFloors;
    }

    public void setNumberOfFloors(String numberOfFloors) {
        this.numberOfFloors = numberOfFloors;
    }

    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public void setApartmentNumber(String apartmentNumber) {
        this.apartmentNumber = apartmentNumber;
    }

    public String getApartmentStory() {
        return apartmentStory;
    }

    public void setApartmentStory(String apartmentStory) {
        this.apartmentStory = apartmentStory;
    }

    public String getNumberOfRooms() {
        return numberOfRooms;
    }

    public void setNumberOfRooms(String numberOfRooms) {
        this.numberOfRooms = numberOfRooms;
    }

    public String getApartmentIncludes() {
        return apartmentIncludes;
    }

    public void setApartmentIncludes(String apartmentIncludes) {
        this.apartmentIncludes = apartmentIncludes;
    }

    public List<String> getApartmentDirections() {
        return apartmentDirections;
    }

    public void setApartmentDirections(List<String> apartmentDirections) {
        this.apartmentDirections = apartmentDirections;
    }

    public String getApartmentKitchen() {
        return apartmentKitchen;
    }

    public void setApartmentKitchen(String apartmentKitchen) {
        this.apartmentKitchen = apartmentKitchen;
    }

    public String getApartmentFlooring() {
        return apartmentFlooring;
    }

    public void setApartmentFlooring(String apartmentFlooring) {
        this.apartmentFlooring = apartmentFlooring;
    }

    public String getApartmentMainEntranceDoor() {
        return apartmentMainEntranceDoor;
    }

    public void setApartmentMainEntranceDoor(String apartmentMainEntranceDoor) {
        this.apartmentMainEntranceDoor = apartmentMainEntranceDoor;
    }

    public String getApartmentInteriorDoorsAndFrames() {
        return apartmentInteriorDoorsAndFrames;
    }

    public void setApartmentInteriorDoorsAndFrames(String apartmentInteriorDoorsAndFrames) {
        this.apartmentInteriorDoorsAndFrames = apartmentInteriorDoorsAndFrames;
    }

    public String getApartmentBathroomFixtures() {
        return apartmentBathroomFixtures;
    }

    public void setApartmentBathroomFixtures(String apartmentBathroomFixtures) {
        this.apartmentBathroomFixtures = apartmentBathroomFixtures;
    }

    public String getApartmentWindows() {
        return apartmentWindows;
    }

    public void setApartmentWindows(String apartmentWindows) {
        this.apartmentWindows = apartmentWindows;
    }

    public String getHasBars() {
        return hasBars;
    }

    public void setHasBars(String hasBars) {
        this.hasBars = hasBars;
    }

    public boolean isCentralHeatingOrFireplace() {
        return centralHeatingOrFireplace;
    }

    public void setCentralHeatingOrFireplace(boolean centralHeatingOrFireplace) {
        this.centralHeatingOrFireplace = centralHeatingOrFireplace;
    }

    public boolean isHasParking() {
        return hasParking;
    }

    public void setHasParking(boolean hasParking) {
        this.hasParking = hasParking;
    }

    public boolean isHasStorage() {
        return hasStorage;
    }

    public void setHasStorage(boolean hasStorage) {
        this.hasStorage = hasStorage;
    }

    public String getApartmentAirConditioning() {
        return apartmentAirConditioning;
    }

    public void setApartmentAirConditioning(String apartmentAirConditioning) {
        this.apartmentAirConditioning = apartmentAirConditioning;
    }

    public String getRegisteredApartmentArea() {
        return registeredApartmentArea;
    }

    public void setRegisteredApartmentArea(String registeredApartmentArea) {
        this.registeredApartmentArea = registeredApartmentArea;
    }

    public String getGrossApartmentArea() {
        return grossApartmentArea;
    }

    public void setGrossApartmentArea(String grossApartmentArea) {
        this.grossApartmentArea = grossApartmentArea;
    }

    public String getTabuCropImage() {
        return tabuCropImage;
    }

    public void setTabuCropImage(String tabuCropImage) {
        this.tabuCropImage = tabuCropImage;
    }

    public List<Image> getAllImages() {
        return allImages;
    }

    public void setAllImages(List<Image> allImages) {
        this.allImages = allImages;
    }

    public String getFrontImageUrl() {
        return frontImageUrl;
    }

    public void setFrontImageUrl(String frontImageUrl) {
        this.frontImageUrl = frontImageUrl;
    }

    public String getInteriorImageUrl() {
        return interiorImageUrl;
    }

    public void setInteriorImageUrl(String interiorImageUrl) {
        this.interiorImageUrl = interiorImageUrl;
    }

    public long getExportTimestamp() {
        return exportTimestamp;
    }

    public void setExportTimestamp(long exportTimestamp) {
        this.exportTimestamp = exportTimestamp;
    }

    public String getExportStatus() {
        return exportStatus;
    }

    public void setExportStatus(String exportStatus) {
        this.exportStatus = exportStatus;
    }
}
