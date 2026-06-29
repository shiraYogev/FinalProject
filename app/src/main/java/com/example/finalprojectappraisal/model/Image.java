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
        BALCONY, STORAGE, HALLWAY, ENTRANCE, GARDEN, ELEVATOR, PARKING, INTERIOR_DOORS, OTHER,
        PANTRY, YARD, DINING_AREA,

        // ===== קטגוריות חוץ חדשות (מפרט) =====
        ROOF, SHARED_ROOF, STAIRWELL, LOBBY, ACCESS_PATH, EXTERNAL_FACILITIES,

        // ===== קטגוריות פנים חדשות (מפרט) =====
        FLOORING, CEILING, AIR_CONDITIONING, INTERIOR_CLADDING, CABINETS, INTERIOR_FINISH,
        TOILET, SAFE_ROOM, DEFECTS

    }

    public enum Subcategory {
        CABINETS, WORKTOP, SINK, FLOOR, DOOR, WINDOW, LIGHTING, FURNITURE,
        SHOWER, BATHTUB, TOILET, CLOSET, SHELVES, APPLIANCES, WALLS, CEILING, OTHER, NONE,
        MASTER,       // הורים (חדר שינה או שירותים)
        GUEST,        // שירותי אורחים
    }

    private String id;
    /** Remote display URL (prefer Firebase Storage downloadUrl). */
    private String url;
    /** Original local content:// URI for fast/local preview (optional). */
    private String localUri;
    /** Storage path inside Firebase Storage, e.g. projects/{pid}/images/{id}.jpg (optional). */
    private String storagePath;

    private String projectId;
    private Category category;
    private List<Subcategory> subcategories;
    private Map<Subcategory, String> aiClassifications; // Gemini output per subcategory
    private String description;
    private String uploadDate;
    private List<String> labels;
    private boolean isVerified;
    private int bedroomIndex = 0; // 0 = לא חדר שינה מספרי, 1 = חדר שינה 1, 2 = חדר שינה 2...
    private Subcategory subCategory = Subcategory.NONE;
    /** מזהה הקומה אליה שייכת התמונה (למשל "GROUND", "FLOOR_1"). null/ריק = קומה ראשית. */
    private String floor = Floor.DEFAULT_KEY;
    /** מזהה היחידה אליה שייכת התמונה (לדירה מחולקת). null/ריק = היחידה הראשית. */
    private String unitId = Unit.DEFAULT_KEY;

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
        this();
        if (map == null) return;

        Object idObj = map.get("id");
        if (idObj instanceof String) this.id = (String) idObj;

        this.url = (String) map.get("url");
        // support legacy alternative keys if ever used
        if (this.url == null) this.url = (String) map.get("downloadUrl");

        this.localUri = (String) map.get("localUri");
        this.storagePath = (String) map.get("storagePath");

        this.projectId = (String) map.get("projectId");

        Object catObj = map.get("category");
        if (catObj instanceof String) {
            try { this.category = Category.valueOf((String) catObj); } catch (Exception ignored) {}
        }

        Object subcatsObj = map.get("subcategories");
        if (subcatsObj instanceof List<?>) {
            this.subcategories.clear();
            for (Object o : (List<?>) subcatsObj) {
                if (o instanceof String) {
                    try { this.subcategories.add(Subcategory.valueOf((String) o)); } catch (Exception ignored) {}
                }
            }
        }

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

        Object idxObj = map.get("bedroomIndex");
        if (idxObj instanceof Long) this.bedroomIndex = ((Long) idxObj).intValue();
        else if (idxObj instanceof Double) this.bedroomIndex = ((Double) idxObj).intValue();

        Object scObj = map.get("subCategory");
        if (scObj instanceof String) {
            try { this.subCategory = Subcategory.valueOf((String) scObj); } catch (Exception ignored) {}
        }
        if (this.subCategory == null) this.subCategory = Subcategory.NONE;

        Object floorObj = map.get("floor");
        if (floorObj instanceof String && !((String) floorObj).trim().isEmpty()) {
            this.floor = (String) floorObj;
        }
        if (this.floor == null || this.floor.trim().isEmpty()) this.floor = Floor.DEFAULT_KEY;

        Object unitObj = map.get("unitId");
        if (unitObj instanceof String && !((String) unitObj).trim().isEmpty()) {
            this.unitId = (String) unitObj;
        }
        if (this.unitId == null || this.unitId.trim().isEmpty()) this.unitId = Unit.DEFAULT_KEY;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("url", url);
        map.put("localUri", localUri);
        map.put("storagePath", storagePath);
        map.put("projectId", projectId);
        map.put("category", category != null ? category.name() : null);

        List<String> subcatNames = new ArrayList<>();
        for (Subcategory sub : getSubcategories()) subcatNames.add(sub.name());
        map.put("subcategories", subcatNames);

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

        map.put("bedroomIndex", bedroomIndex);
        map.put("subCategory", (subCategory != null ? subCategory.name() : Subcategory.NONE.name()));
        map.put("floor", (floor != null && !floor.trim().isEmpty()) ? floor : Floor.DEFAULT_KEY);
        map.put("unitId", (unitId != null && !unitId.trim().isEmpty()) ? unitId : Unit.DEFAULT_KEY);

        return map;
    }

    // ---------- Safe getters ----------
    public String getId() { return id; }
    public String getUrl() { return url; }
    public String getLocalUri() { return localUri; }
    public String getStoragePath() { return storagePath; }
    public String getProjectId() { return projectId; }
    public Category getCategory() { return category; }
    public List<Subcategory> getSubcategories() { if (subcategories == null) subcategories = new ArrayList<>(); return subcategories; }
    public Map<Subcategory, String> getAiClassifications() { if (aiClassifications == null) aiClassifications = new HashMap<>(); return aiClassifications; }
    public String getDescription() { return description; }
    public String getUploadDate() { return uploadDate; }
    public List<String> getLabels() { if (labels == null) labels = new ArrayList<>(); return labels; }
    public boolean isVerified() { return isVerified; }
    public Map<Subcategory, String> getFinalClassification() { if (finalClassification == null) finalClassification = new HashMap<>(); return finalClassification; }

    // ---------- Setters ----------
    public void setId(String id) { this.id = id; }
    public void setUrl(String url) { this.url = url; }
    public void setLocalUri(String localUri) { this.localUri = localUri; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public void setCategory(Category category) { this.category = category; }
    public void setSubcategories(List<Subcategory> subcategories) { this.subcategories = (subcategories != null) ? subcategories : new ArrayList<>(); }
    public void setAiClassifications(Map<Subcategory, String> aiClassifications) { this.aiClassifications = (aiClassifications != null) ? aiClassifications : new HashMap<>(); }
    public void setDescription(String description) { this.description = description; }
    public void setUploadDate(String uploadDate) { this.uploadDate = (uploadDate != null) ? uploadDate : java.time.LocalDate.now().toString(); }
    public void setLabels(List<String> labels) { this.labels = (labels != null) ? labels : new ArrayList<>(); }
    public void setVerified(boolean verified) { isVerified = verified; }
    public void setFinalClassification(Map<Subcategory, String> finalClassification) { this.finalClassification = (finalClassification != null) ? finalClassification : new HashMap<>(); }
    public int getBedroomIndex() { return bedroomIndex; }
    public void setBedroomIndex(int index) { this.bedroomIndex = index; }

    public Subcategory getSubCategory() { return subCategory; }
    public void setSubCategory(Subcategory sc) { this.subCategory = sc; }

    public String getFloor() { return (floor != null && !floor.trim().isEmpty()) ? floor : Floor.DEFAULT_KEY; }
    public void setFloor(String floor) { this.floor = (floor != null && !floor.trim().isEmpty()) ? floor : Floor.DEFAULT_KEY; }

    public String getUnitId() { return (unitId != null && !unitId.trim().isEmpty()) ? unitId : Unit.DEFAULT_KEY; }
    public void setUnitId(String unitId) { this.unitId = (unitId != null && !unitId.trim().isEmpty()) ? unitId : Unit.DEFAULT_KEY; }

}
