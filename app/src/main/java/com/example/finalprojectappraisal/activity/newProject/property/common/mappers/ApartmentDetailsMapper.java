package com.example.finalprojectappraisal.activity.newProject.property.common.mappers;

import com.example.finalprojectappraisal.activity.newProject.property.common.state.ApartmentEditableState;
import com.example.finalprojectappraisal.activity.newProject.property.common.utils.Formatters;
import com.example.finalprojectappraisal.classifer.gemini.GeminiJsonParser;

import java.util.HashMap;
import java.util.Map;

public final class ApartmentDetailsMapper {
    private ApartmentDetailsMapper() {}

    public static Map<String, Object> toUpdates(ApartmentEditableState s) {
        Map<String, Object> m = new HashMap<>();
        if (s == null) return m;

        putIfNotEmpty(m, GeminiJsonParser.FirestoreKeys.ENTRANCE_DOOR_CONDITION, s.entranceDoorCondition);
        putIfNotEmpty(m, GeminiJsonParser.FirestoreKeys.WINDOW_TYPE,              s.windowType);
        putIfNotEmpty(m, GeminiJsonParser.FirestoreKeys.HAS_AIR_CONDITIONING,     s.hasAirConditioning);
        putIfNotEmpty(m, GeminiJsonParser.FirestoreKeys.INTERIOR_DOOR_CONDITION,  s.interiorDoorCondition);
        putIfNotEmpty(m, GeminiJsonParser.FirestoreKeys.KITCHEN_CONDITION,        s.kitchenCondition);
        putIfNotEmpty(m, GeminiJsonParser.FirestoreKeys.BATHROOM_FIXTURES,        s.bathroomFixtures);

        String flooring = Formatters.combineFlooring(s.flooringType, s.flooringSize);
        if (flooring != null && !flooring.trim().isEmpty()) {
            m.put(GeminiJsonParser.FirestoreKeys.FLOORING_TYPE, flooring);
        }

        m.put(GeminiJsonParser.FirestoreKeys.HAS_CENTRAL_HEATING, s.hasCentralHeating);
        m.put(GeminiJsonParser.FirestoreKeys.HAS_BARS,            s.hasBars);
        m.put(GeminiJsonParser.FirestoreKeys.HAS_ELEVATOR,        s.hasElevator);
        m.put(GeminiJsonParser.FirestoreKeys.HAS_PARKING,         s.hasParking);
        m.put(GeminiJsonParser.FirestoreKeys.HAS_STORAGE,         s.hasStorage);

        return m;
    }

    private static void putIfNotEmpty(Map<String, Object> map, String key, String value) {
        if (value == null) return;
        String v = value.trim();
        if (!v.isEmpty() && !Formatters.EM_DASH.equals(v)) map.put(key, v);
    }
}
