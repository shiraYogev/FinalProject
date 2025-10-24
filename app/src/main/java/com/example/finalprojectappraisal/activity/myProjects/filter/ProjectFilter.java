package com.example.finalprojectappraisal.activity.myProjects.filter;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Filter DTO for projects list.
 * AND between different fields; OR inside each Set field (e.g., statuses).
 * Time fields are epochMillis (Long) to avoid Date<->Long issues.
 */
public class ProjectFilter {

    // ---------- Free text (full address / generic search) ----------
    @Nullable
    private String textQuery;

    // ---------- Address-specific ----------
    @Nullable private String city;
    @Nullable private String street;
    @Nullable private String houseNumber;

    // ---------- Cadastral ----------
    @Nullable private String gush;     // גוש (Main parcel)
    @Nullable private String parcel;   // חלקה (Lot / sub parcel if needed)

    // ---------- Status ----------
    private final Set<String> statuses = new HashSet<>();

    // ---------- Dates ----------
    public enum DateField { LAST_UPDATE, CREATED }
    private DateField dateField = DateField.LAST_UPDATE;

    @Nullable private Long dateFromEpochMillis;
    @Nullable private Long dateToEpochMillis;

    // ---------- Location (radius from center) ----------
    @Nullable private Double centerLat;
    @Nullable private Double centerLng;
    @Nullable private Double radiusKm;

    // ---------- Sorting ----------
    public enum SortField { LAST_UPDATE, CREATED, CITY }
    private SortField sortBy = SortField.LAST_UPDATE;

    public enum SortDir { ASC, DESC }
    private SortDir sortDir = SortDir.DESC;

    // ================= Getters / Setters =================

    @Nullable public String getTextQuery() { return textQuery; }
    public void setTextQuery(@Nullable String textQuery) { this.textQuery = textQuery; }

    public Set<String> getStatuses() { return statuses; }

    public DateField getDateField() { return dateField; }
    public void setDateField(DateField dateField) { this.dateField = dateField; }

    @Nullable public Long getDateFromEpochMillis() { return dateFromEpochMillis; }
    public void setDateFromEpochMillis(@Nullable Long dateFromEpochMillis) { this.dateFromEpochMillis = dateFromEpochMillis; }

    @Nullable public Long getDateToEpochMillis() { return dateToEpochMillis; }
    public void setDateToEpochMillis(@Nullable Long dateToEpochMillis) { this.dateToEpochMillis = dateToEpochMillis; }

    @Nullable public Double getCenterLat() { return centerLat; }
    public void setCenterLat(@Nullable Double centerLat) { this.centerLat = centerLat; }

    @Nullable public Double getCenterLng() { return centerLng; }
    public void setCenterLng(@Nullable Double centerLng) { this.centerLng = centerLng; }

    @Nullable public Double getRadiusKm() { return radiusKm; }
    public void setRadiusKm(@Nullable Double radiusKm) { this.radiusKm = radiusKm; }

    public SortField getSortBy() { return sortBy; }
    public void setSortBy(SortField sortBy) { this.sortBy = sortBy; }

    public SortDir getSortDir() { return sortDir; }
    public void setSortDir(SortDir sortDir) { this.sortDir = sortDir; }

    // ---- New fields ----
    @Nullable public String getCity() { return city; }
    public void setCity(@Nullable String city) { this.city = city; }

    @Nullable public String getStreet() { return street; }
    public void setStreet(@Nullable String street) { this.street = street; }

    @Nullable public String getHouseNumber() { return houseNumber; }
    public void setHouseNumber(@Nullable String houseNumber) { this.houseNumber = houseNumber; }

    @Nullable public String getGush() { return gush; }
    public void setGush(@Nullable String gush) { this.gush = gush; }

    @Nullable public String getParcel() { return parcel; }
    public void setParcel(@Nullable String parcel) { this.parcel = parcel; }

    /** Reset all fields to defaults. */
    public void clear() {
        textQuery = null;

        city = null;
        street = null;
        houseNumber = null;

        gush = null;
        parcel = null;

        statuses.clear();

        dateField = DateField.LAST_UPDATE;
        dateFromEpochMillis = null;
        dateToEpochMillis   = null;

        centerLat = null;
        centerLng = null;
        radiusKm  = null;

        sortBy = SortField.LAST_UPDATE;
        sortDir = SortDir.DESC;
    }
}
