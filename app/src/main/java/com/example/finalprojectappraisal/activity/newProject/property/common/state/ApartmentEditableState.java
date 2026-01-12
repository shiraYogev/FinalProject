package com.example.finalprojectappraisal.activity.newProject.property.common.state;

import androidx.annotation.Nullable;

import com.example.finalprojectappraisal.model.Project;

public class ApartmentEditableState {

    public String entranceDoorCondition;
    public String windowType;
    public boolean hasCentralHeating;
    public String hasBars;
    public String hasAirConditioning;   // "מלא / חלקי / אין"
    public String flooringType;
    public String flooringSize;
    public String interiorDoorCondition;
    public String kitchenCondition;
    public String bathroomFixtures;

    // Elevator as String: "אין", "יש (1)", "יש (2)", "יש (3)", "יש (4)"
    public String hasElevator;

    public boolean hasParking;
    public boolean hasStorage;

    public static ApartmentEditableState fromProject(@Nullable Project p) {
        ApartmentEditableState s = new ApartmentEditableState();
        if (p == null) {
            return s;
        }

        s.entranceDoorCondition = safe(p.getEntranceDoorCondition());
        s.windowType            = safe(p.getWindowType());
        s.hasCentralHeating     = p.isHasCentralHeating();
        s.hasBars               = safe(p.getHasBars());

        // ⬅️ כאן התיקון – להשתמש ב-isHasAirConditioning()
        s.hasAirConditioning    = safe(p.isHasAirConditioning());

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

        s.interiorDoorCondition = safe(p.getInteriorDoorCondition());
        s.kitchenCondition      = safe(p.getKitchenCondition());
        s.bathroomFixtures      = safe(p.getBathroomFixtures());

        s.hasElevator           = safe(p.getHasElevator());

        s.hasParking            = p.isHasParking();
        s.hasStorage            = p.isHasStorageRoom();

        return s;
    }

    private static String safe(@Nullable String v) {
        return v == null ? "" : v;
    }
}
