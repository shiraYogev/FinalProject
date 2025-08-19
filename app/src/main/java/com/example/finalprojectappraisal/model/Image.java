package com.example.finalprojectappraisal.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an image associated with a project/property.
 */
public class Image {

    public enum Category {
        EXTERIOR, LIVING_ROOM, KITCHEN, BATHROOM, BEDROOM, VIEW, DINING_ROOM, ENTRANCE_DOOR,
        BALCONY, STORAGE, HALLWAY, ENTRANCE, GARDEN, ELEVATOR, PARKING, INTERIOR_DOORS, OTHER
    }

    public enum Subcategory {
        CABINETS, WORKTOP, SINK, FLOOR, DOOR, WINDOW, LIGHTING, FURNITURE,
        SHOWER, BATHTUB, TOILET, CLOSET, SHELVES, APPLIANCES, WALLS, CEILING, OTHER
    }

    private String id;
    private String url;
    private String projectId;
    private Category category;
    private List<Subcategory> subcategories;
    private Map<Subcategory, String> aiClassifications; // Gemini output per subcategory
    private String description;
    private String uploadDate;
    private List<String> labels;
    private boolean isVerified;
    private Map<Subcategory, String> finalClassification; // manually edited final result

    // Full constructor
    public Image(String url, String projectId, Category category,
                 List<Subcategory> subcategories, Map<Subcategory, String> aiClassifications,
                 String description, List<String> labels, boolean isVerified,
                 Map<Subcategory, String> finalClassification) {
        this.id = UUID.randomUUID().toString();
        this.url = url;
        this.projectId = projectId;
        this.category = category;
        this.subcategories = (subcategories != null) ? subcategories : new ArrayList<>();
        this.aiClassifications = (aiClassifications != null) ? aiClassifications : new HashMap<>();
        this.description = description;
        this.uploadDate = java.time.LocalDate.now().toString();
        this.labels = (labels != null) ? labels : new ArrayList<>();
        this.isVerified = isVerified;
        this.finalClassification = (finalClassification != null) ? finalClassification : new HashMap<>();
    }

    // Empty constructor (Firestore/serialization) — initialize everything to avoid NPEs
    public Image() {
        this.id = UUID.randomUUID().toString();
        this.subcategories = new ArrayList<>();
        this.aiClassifications = new HashMap<>();
        this.labels = new ArrayList<>();
        this.finalClassification = new HashMap<>();
        this.uploadDate = java.time.LocalDate.now().toString();
        this.isVerified = false;
    }

    @SuppressWarnings("unchecked")
    public Image(Map<String, Object> map) {
        // defaults first
        this();
        if (map == null) return;

        Object idObj = map.get("id");
        if (idObj instanceof String) this.id = (String) idObj;

        this.url = (String) map.get("url");
        this.projectId = (String) map.get("projectId");

        Object catObj = map.get("category");
        if (catObj instanceof String) {
            try { this.category = Category.valueOf((String) catObj); } catch (Exception ignored) {}
        }

        // subcategories
        Object subcatsObj = map.get("subcategories");
        if (subcatsObj instanceof List<?>) {
            this.subcategories.clear();
            for (Object o : (List<?>) subcatsObj) {
                if (o instanceof String) {
                    try { this.subcategories.add(Subcategory.valueOf((String) o)); } catch (Exception ignored) {}
                }
            }
        }

        // aiClassifications
        Object aiObj = map.get("aiClassifications");
        if (aiObj instanceof Map<?, ?>) {
            this.aiClassifications.clear();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) aiObj).entrySet()) {
                Object k = e.getKey();
                Object v = e.getValue();
                if (k instanceof String && v instanceof String) {
                    try { this.aiClassifications.put(Subcategory.valueOf((String) k), (String) v); } catch (Exception ignored) {}
                }
            }
        }

        this.description = (String) map.get("description");

        Object uploadDateObj = map.get("uploadDate");
        this.uploadDate = (uploadDateObj instanceof String) ? (String) uploadDateObj : this.uploadDate;

        Object labelsObj = map.get("labels");
        if (labelsObj instanceof List<?>) {
            this.labels.clear();
            for (Object o : (List<?>) labelsObj) {
                if (o != null) this.labels.add(String.valueOf(o));
            }
        }

        Object verifiedObj = map.get("isVerified");
        this.isVerified = (verifiedObj instanceof Boolean) ? (Boolean) verifiedObj : false;

        // finalClassification
        Object finalObj = map.get("finalClassification");
        if (finalObj instanceof Map<?, ?>) {
            this.finalClassification.clear();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) finalObj).entrySet()) {
                Object k = e.getKey();
                Object v = e.getValue();
                if (k instanceof String && v instanceof String) {
                    try { this.finalClassification.put(Subcategory.valueOf((String) k), (String) v); } catch (Exception ignored) {}
                }
            }
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("url", url);
        map.put("projectId", projectId);
        map.put("category", category != null ? category.name() : null);

        // subcategories → List<String>
        List<String> subcatNames = new ArrayList<>();
        for (Subcategory sub : getSubcategories()) subcatNames.add(sub.name());
        map.put("subcategories", subcatNames);

        // aiClassifications → Map<String,String>
        Map<String, String> aiClassNames = new HashMap<>();
        for (Map.Entry<Subcategory, String> entry : getAiClassifications().entrySet())
            aiClassNames.put(entry.getKey().name(), entry.getValue());
        map.put("aiClassifications", aiClassNames);

        map.put("description", description);
        map.put("uploadDate", uploadDate);
        map.put("labels", new ArrayList<>(getLabels()));
        map.put("isVerified", isVerified);

        Map<String, String> finalClassNames = new HashMap<>();
        for (Map.Entry<Subcategory, String> entry : getFinalClassification().entrySet())
            finalClassNames.put(entry.getKey().name(), entry.getValue());
        map.put("finalClassification", finalClassNames);

        return map;
    }

    // ---------- Safe getters (never return null) ----------

    public String getId() { return id; }
    public String getUrl() { return url; }
    public String getProjectId() { return projectId; }
    public Category getCategory() { return category; }

    public List<Subcategory> getSubcategories() {
        if (subcategories == null) subcategories = new ArrayList<>();
        return subcategories;
    }

    public Map<Subcategory, String> getAiClassifications() {
        if (aiClassifications == null) aiClassifications = new HashMap<>();
        return aiClassifications;
    }

    public String getDescription() { return description; }
    public String getUploadDate() { return uploadDate; }

    public List<String> getLabels() {
        if (labels == null) labels = new ArrayList<>();
        return labels;
    }

    public boolean isVerified() { return isVerified; }

    public Map<Subcategory, String> getFinalClassification() {
        if (finalClassification == null) finalClassification = new HashMap<>();
        return finalClassification;
    }

    // ---------- Setters ----------

    public void setId(String id) { this.id = id; }
    public void setUrl(String url) { this.url = url; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public void setCategory(Category category) { this.category = category; }
    public void setSubcategories(List<Subcategory> subcategories) { this.subcategories = (subcategories != null) ? subcategories : new ArrayList<>(); }
    public void setAiClassifications(Map<Subcategory, String> aiClassifications) { this.aiClassifications = (aiClassifications != null) ? aiClassifications : new HashMap<>(); }
    public void setDescription(String description) { this.description = description; }
    public void setUploadDate(String uploadDate) { this.uploadDate = (uploadDate != null) ? uploadDate : java.time.LocalDate.now().toString(); }
    public void setLabels(List<String> labels) { this.labels = (labels != null) ? labels : new ArrayList<>(); }
    public void setVerified(boolean verified) { isVerified = verified; }
    public void setFinalClassification(Map<Subcategory, String> finalClassification) { this.finalClassification = (finalClassification != null) ? finalClassification : new HashMap<>(); }
}
