package com.example.finalprojectappraisal.activity.newProject.property.common.state;

import com.example.finalprojectappraisal.model.Project;

public class ApartmentEditableState {
    public String entranceDoorCondition;
    public String windowType;
    public boolean hasCentralHeating;
    public String hasBars;
    public String hasAirConditioning;
    public String flooringType;
    public String flooringSize;
    public String interiorDoorCondition;
    public String kitchenCondition;
    public String bathroomFixtures;

    // ⬅️ changed: elevator as String, matches spinner values
    // allowed values: "אין", "יש (1)", "יש (2)", "יש (3)", "יש (4)"
    public String hasElevator;

    public boolean hasParking;
    public boolean hasStorage;

    public static ApartmentEditableState fromProject(Project p) {
        ApartmentEditableState s = new ApartmentEditableState();
        s.entranceDoorCondition = p.getEntranceDoorCondition();
        s.windowType            = p.getWindowType();
        s.hasCentralHeating     = p.isHasCentralHeating();
        s.hasBars               = p.getHasBars();
        s.hasAirConditioning    = p.isHasAirConditioning();

        String flooring = p.getFlooringType();
        if (flooring != null) {
            int i = flooring.lastIndexOf('('), j = flooring.lastIndexOf(')');
            if (i > 0 && j > i) {
                s.flooringType = flooring.substring(0, i).trim();
                s.flooringSize = flooring.substring(i + 1, j).trim();
            } else {
                s.flooringType = flooring.trim();
                s.flooringSize = null;
            }
        }

        s.interiorDoorCondition = p.getInteriorDoorCondition();
        s.kitchenCondition      = p.getKitchenCondition();
        s.bathroomFixtures      = p.getBathroomFixtures();

        // ⬅️ changed: read String from Project (not boolean)
        s.hasElevator           = p.getHasElevator(); // "אין", "יש (1)" וכו' או null

        s.hasParking            = p.isHasParking();
        s.hasStorage            = p.isHasStorageRoom();
        return s;
    }
}
