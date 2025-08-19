package com.example.finalprojectappraisal.model;

import java.io.Serializable;
import java.util.List;

/**
 * The PhysicalFeatures class encapsulates the physical characteristics of an apartment or property,
 * including interior conditions and additional facilities.
 */
public class PhysicalFeatures implements Serializable {
    private String flooringType;         // סוג רצפה (למשל, אריחים, עץ)
    private String kitchenCondition;     // מצב המטבח (חדש, דורש שיפוץ וכו')
    private String entranceDoorCondition; // מצב דלת הכניסה
    private String interiorDoorCondition; // מצב דלתות פנימיות
    private String windowType;           // סוג חלונות (למשל, כפולים)
    private boolean hasBars;             // האם יש סורגים לחלונות
    private List<String> airDirection;   // כיווני אוויר (צפון, דרום וכו')

    // Additional facilities and services
    private boolean hasElevator;         // האם יש מעלית
    private boolean hasStorageRoom;      // האם קיים מחסן
    private boolean hasAirConditioning;  // האם יש מיזוג אוויר
    private boolean hasParking;          // האם קיים חניה
    private boolean hasCentralHeating;   // האם יש חימום מרכזי

    public PhysicalFeatures(String flooringType, String kitchenCondition, String entranceDoorCondition,
                            String interiorDoorCondition, String windowType, boolean hasBars, List<String> airDirection,
                            boolean hasElevator, boolean hasStorageRoom, boolean hasAirConditioning,
                            boolean hasParking, boolean hasCentralHeating) {
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
    }

    // Getters and setters

    public String getFlooringType() {
        return flooringType;
    }

    public void setFlooringType(String flooringType) {
        this.flooringType = flooringType;
    }

    public String getKitchenCondition() {
        return kitchenCondition;
    }

    public void setKitchenCondition(String kitchenCondition) {
        this.kitchenCondition = kitchenCondition;
    }

    public String getEntranceDoorCondition() {
        return entranceDoorCondition;
    }

    public void setEntranceDoorCondition(String entranceDoorCondition) {
        this.entranceDoorCondition = entranceDoorCondition;
    }

    public String getInteriorDoorCondition() {
        return interiorDoorCondition;
    }

    public void setInteriorDoorCondition(String interiorDoorCondition) {
        this.interiorDoorCondition = interiorDoorCondition;
    }

    public String getWindowType() {
        return windowType;
    }

    public void setWindowType(String windowType) {
        this.windowType = windowType;
    }

    public boolean isHasBars() {
        return hasBars;
    }

    public void setHasBars(boolean hasBars) {
        this.hasBars = hasBars;
    }

    public List<String> getAirDirection() {
        return airDirection;
    }

    public void setAirDirection(List<String> airDirection) {
        this.airDirection = airDirection;
    }

    public boolean isHasElevator() {
        return hasElevator;
    }

    public void setHasElevator(boolean hasElevator) {
        this.hasElevator = hasElevator;
    }

    public boolean isHasStorageRoom() {
        return hasStorageRoom;
    }

    public void setHasStorageRoom(boolean hasStorageRoom) {
        this.hasStorageRoom = hasStorageRoom;
    }

    public boolean isHasAirConditioning() {
        return hasAirConditioning;
    }

    public void setHasAirConditioning(boolean hasAirConditioning) {
        this.hasAirConditioning = hasAirConditioning;
    }

    public boolean isHasParking() {
        return hasParking;
    }

    public void setHasParking(boolean hasParking) {
        this.hasParking = hasParking;
    }

    public boolean isHasCentralHeating() {
        return hasCentralHeating;
    }

    public void setHasCentralHeating(boolean hasCentralHeating) {
        this.hasCentralHeating = hasCentralHeating;
    }
}
