package com.example.finalprojectappraisal.activity.myProjects.filter;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Simplified filter DTO for the "My Projects" screen.
 * AND between different fields; OR inside each Set field (e.g., statuses).
 * Time fields are epochMillis (Long) to avoid Date<->Long issues.
 */
public class ProjectFilter {

    // ---------- Free text ----------
    @Nullable
    private String textQuery; // normalized to lowercase outside

    // ---------- Status ----------
    // Store enum names (e.g., "ACTIVE", "DRAFT", "ARCHIVED")
    private final Set<String> statuses = new HashSet<>();

    // ---------- Dates ----------
    public enum DateField { LAST_UPDATE, CREATED }
    private DateField dateField = DateField.LAST_UPDATE;

    @Nullable
    private Long dateFromEpochMillis;

    @Nullable
    private Long dateToEpochMillis;

    // ---------- Location (radius from center) ----------
    @Nullable
    private Double centerLat;

    @Nullable
    private Double centerLng;

    @Nullable
    private Double radiusKm;

    // ---------- Sorting ----------
    public enum SortField { LAST_UPDATE, CREATED, CITY }
    private SortField sortBy = SortField.LAST_UPDATE;

    public enum SortDir { ASC, DESC }
    private SortDir sortDir = SortDir.DESC;

    // ================= Getters / Setters =================

    @Nullable
    public String getTextQuery() {
        return textQuery;
    }

    public void setTextQuery(@Nullable String textQuery) {
        this.textQuery = textQuery;
    }

    public Set<String> getStatuses() {
        return statuses;
    }

    public DateField getDateField() {
        return dateField;
    }

    public void setDateField(DateField dateField) {
        this.dateField = dateField;
    }

    @Nullable
    public Long getDateFromEpochMillis() {
        return dateFromEpochMillis;
    }

    public void setDateFromEpochMillis(@Nullable Long dateFromEpochMillis) {
        this.dateFromEpochMillis = dateFromEpochMillis;
    }

    @Nullable
    public Long getDateToEpochMillis() {
        return dateToEpochMillis;
    }

    public void setDateToEpochMillis(@Nullable Long dateToEpochMillis) {
        this.dateToEpochMillis = dateToEpochMillis;
    }

    @Nullable
    public Double getCenterLat() {
        return centerLat;
    }

    public void setCenterLat(@Nullable Double centerLat) {
        this.centerLat = centerLat;
    }

    @Nullable
    public Double getCenterLng() {
        return centerLng;
    }

    public void setCenterLng(@Nullable Double centerLng) {
        this.centerLng = centerLng;
    }

    @Nullable
    public Double getRadiusKm() {
        return radiusKm;
    }

    public void setRadiusKm(@Nullable Double radiusKm) {
        this.radiusKm = radiusKm;
    }

    public SortField getSortBy() {
        return sortBy;
    }

    public void setSortBy(SortField sortBy) {
        this.sortBy = sortBy;
    }

    public SortDir getSortDir() {
        return sortDir;
    }

    public void setSortDir(SortDir sortDir) {
        this.sortDir = sortDir;
    }

    /** Reset all fields to defaults. */
    public void clear() {
        textQuery = null;
        statuses.clear();

        dateField = DateField.LAST_UPDATE;
        dateFromEpochMillis = null;
        dateToEpochMillis = null;

        centerLat = null;
        centerLng = null;
        radiusKm = null;

        sortBy = SortField.LAST_UPDATE;
        sortDir = SortDir.DESC;
    }
}