// file: app/src/main/java/com/example/finalprojectappraisal/activity/AllProjects/viewmodel/ProjectPreviewViewModel.java
package com.example.finalprojectappraisal.activity.AllProjects.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.text.format.DateFormat;

import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Appraiser;
import com.example.finalprojectappraisal.model.Image;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

/**
 * ViewModel for the Project Preview screen.
 * Loads and formats project header, details-by-sections, and images.
 * All DB access goes via ProjectRepository.
 */
public class ProjectPreviewViewModel extends ViewModel {

    // ===== Logging helpers =====
    private static final String TAG = "ProjectPreviewVM";
    private static final boolean LOG_VERBOSE = true;

    // We keep this flag, but lookups now go through getByPath(...) which always tries fallbacks.
    private static final boolean ENABLE_SMART_LOOKUP = true;

    private void logD(String msg) { if (LOG_VERBOSE) android.util.Log.d(TAG, msg); }
    private void logW(String msg) { android.util.Log.w(TAG, msg); }
    private void logE(String msg, Throwable t) { android.util.Log.e(TAG, msg, t); }

    private String preview(Object v) {
        if (v == null) return "null";
        String s = String.valueOf(v);
        if (s.length() > 120) s = s.substring(0, 120) + "…(" + s.length() + ")";
        return s.replace("\n", "\\n");
    }

    // Repository
    private final ProjectRepository repo = ProjectRepository.getInstance();

    // Loading flags
    private final MutableLiveData<Boolean> loadingHeader = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loadingImages = new MutableLiveData<>(false);

    // UI models
    private final MutableLiveData<ProjectHeaderUi> header = new MutableLiveData<>();
    private final MutableLiveData<List<Image>> images = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<KV>> details = new MutableLiveData<>(new ArrayList<>());

    // Sectioned (flat) list of images: headers + photos
    private final MutableLiveData<List<UiImageItem>> sectionedImages = new MutableLiveData<>(new ArrayList<>());

    // Admin permission
    private final MutableLiveData<Boolean> isAdmin = new MutableLiveData<>(false);

    private final MediatorLiveData<com.example.finalprojectappraisal.model.Project> repoCurrentProject = new MediatorLiveData<>();

    // --- Expose LiveData to the UI ---
    public LiveData<Boolean> getLoadingHeader() { return loadingHeader; }
    public LiveData<Boolean> getLoadingImages() { return loadingImages; }
    public LiveData<ProjectHeaderUi> getHeader() { return header; }
    public LiveData<List<Image>> getImages() { return images; }
    public LiveData<List<KV>> getDetails() { return details; }
    public LiveData<Boolean> getIsAdmin() { return isAdmin; }
    public LiveData<List<UiImageItem>> getSectionedImages() { return sectionedImages; }

    /** Initialize loads once the projectId is known. */
    public void init(@NonNull String projectId) {
        loadHeader(projectId);
        loadDetails(projectId);
        loadImages(projectId);
    }

    /** Check admin permission for current user (provided by Activity via AuthRepository). */
    public void checkPermissionsFor(@Nullable String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            isAdmin.setValue(false);
            return;
        }
        repo.getUserPermissions(userId, task -> {
            boolean admin = false;
            if (task.isSuccessful() && task.getResult() != null) {
                Appraiser.AccessPermission p = task.getResult();
                admin = (p == Appraiser.AccessPermission.ADMIN || p == Appraiser.AccessPermission.SUPER_ADMIN);
            }
            isAdmin.setValue(admin);
        });
    }

    // --- Header load & bind ---
    private void loadHeader(String projectId) {
        loadingHeader.setValue(true);
        logD("loadHeader: projectId=" + projectId);
        long t0 = System.nanoTime();

        repo.getProject(projectId, task -> {
            long dtMs = (System.nanoTime() - t0) / 1_000_000;
            loadingHeader.setValue(false);

            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                logW("loadHeader: FAILED (exists=" + (task.getResult() != null && task.getResult().exists()) + ") in " + dtMs + "ms");
                header.setValue(ProjectHeaderUi.error("Failed to load project"));
                return;
            }
            DocumentSnapshot doc = task.getResult();
            logD("loadHeader: OK docId=" + doc.getId() + " in " + dtMs + "ms");
            bindHeader(doc);
        });
    }

    /** Build header model with robust key fallbacks. */
    private void bindHeader(@NonNull DocumentSnapshot doc) {
        String address = firstNonEmpty(
                doc.getString("fullAddress"),
                doc.getString("full_address"),
                doc.getString("address.full"),
                doc.getString("addressFull")
        );
        if (address == null || address.trim().isEmpty()) address = "—";

        String status = firstNonEmpty(
                doc.getString("projectStatus"),
                doc.getString("project_status")
        );
        if (status == null || status.trim().isEmpty()) status = "—";

        String clientName = firstNonEmpty(
                doc.getString("client.fullName"),
                doc.getString("clientName"),
                doc.getString("client_full_name"),
                "—"
        );

        long lastUpdatedMillis = firstNonZero(
                extractMillis(doc.get("lastUpdateDate")),
                extractMillis(doc.get("last_updated")),
                extractMillis(doc.get("updatedAt")),
                extractMillis(doc.get("lastUpdate"))
        );
        String lastUpdatedTxt  = formatMillis(lastUpdatedMillis);

        logD("bindHeader: address=" + preview(address) +
                " | status=" + preview(status) +
                " | client=" + preview(clientName) +
                " | lastUpdated=" + preview(lastUpdatedTxt));

        header.setValue(new ProjectHeaderUi(address, status, clientName, lastUpdatedTxt, false, null));
    }

    // --- Details (sections & fields via dot-notation) ---
    private void loadDetails(String projectId) {
        logD("loadDetails: projectId=" + projectId);
        long t0 = System.nanoTime();

        repo.getProject(projectId, task -> {
            long dtMs = (System.nanoTime() - t0) / 1_000_000;

            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                logW("loadDetails: FAILED in " + dtMs + "ms");
                details.setValue(new ArrayList<>());
                return;
            }

            DocumentSnapshot doc = task.getResult();
            Map<String, Object> flat = flatten(doc.getData());
            Map<String, String> normIndex = buildNormIndex(flat.keySet());
            logD("loadDetails: OK docId=" + doc.getId() + " • flatKeys=" + flat.size() + " in " + dtMs + "ms");

            List<KV> rows = new ArrayList<>();
            int missing = 0, matched = 0;

            for (SectionSpec sec : SECTIONS) {
                rows.add(KV.header(sec.title));
                logD("Section ▶ " + sec.title + " (fields=" + sec.fields.size() + ")");
                for (FieldSpec f : sec.fields) {

                    // --- unified robust lookup ---
                    Object raw = getByPath(doc, flat, normIndex, f.path);

                    String display = toDisplayValue(raw);
                    if (display == null || display.trim().isEmpty()) {
                        display = "—";
                        missing++;
                        if (raw == null) {
                            logD("  • " + f.path + " ⇒ NOT FOUND");
                        } else {
                            logD("  • " + f.path + " ⇒ FOUND but empty after formatting: " + preview(raw));
                        }
                    } else {
                        matched++;
                        logD("  • " + f.path + " ⇒ " + preview(display));
                    }
                    rows.add(KV.row(f.label, display));
                }
            }

            logD("loadDetails: done. matched=" + matched + ", missing=" + missing);
            details.setValue(rows);
        });
    }

    /** Converts raw Firestore value to display-friendly string. */
    private String toDisplayValue(Object val) {
        if (val == null) return null;
        if (val instanceof Boolean) return ((Boolean) val) ? "כן" : "לא";
        if (val instanceof List<?>) {
            List<?> list = (List<?>) val;
            if (list.isEmpty()) return null;
            List<String> parts = new ArrayList<>();
            for (Object o : list) if (o != null) parts.add(String.valueOf(o));
            return parts.isEmpty() ? null : String.join(", ", parts);
        }
        // Dates/Timestamps: format nicely
        if (val instanceof Timestamp) {
            return formatMillis(((Timestamp) val).toDate().getTime());
        }
        if (val instanceof Date) {
            return formatMillis(((Date) val).getTime());
        }
        if (val instanceof Number) {
            long n = ((Number) val).longValue();
            // Only treat as epochMillis if it's clearly in the far future threshold (avoid misclassifying regular IDs)
            // 3e12 ~ year 2065; typical "now" (~1.7e12) will be treated as plain number unless your date fields are proper Timestamp/Date.
            if (n > 3_000_000_000_000L) {
                return formatMillis(n);
            }
            return String.valueOf(n);
        }
        String s = String.valueOf(val).trim();
        return s.isEmpty() ? null : s;
    }

    // --- Images ---
    private void loadImages(String projectId) {
        loadingImages.setValue(true);
        logD("loadImages: projectId=" + projectId);
        long t0 = System.nanoTime();

        // קודם נטען את הפרויקט רק כדי להביא את tabu_crop_image (אם קיים)
        repo.getProject(projectId, taskProj -> {
            String tabuUrl = null;

            if (taskProj.isSuccessful() && taskProj.getResult() != null && taskProj.getResult().exists()) {
                Object tabuField = taskProj.getResult().get("tabu_crop_image");
                if (tabuField != null) {
                    tabuUrl = String.valueOf(tabuField).trim();
                    if (tabuUrl.isEmpty()) {
                        tabuUrl = null;
                    }
                } else {
                    logD("loadImages: no tabu_crop_image field on project");
                }
            } else {
                logW("loadImages: failed to load project for tabu_crop_image");
            }

            final String finalTabuUrl = tabuUrl; // לשימוש ב־lambda הפנימי

            // עכשיו נטען את רשימת התמונות הרגילות כמו קודם
            repo.loadAllImagesForProject(projectId, taskImg -> {
                long dtMs = (System.nanoTime() - t0) / 1_000_000;
                loadingImages.setValue(false);

                List<Image> list = (taskImg.isSuccessful() && taskImg.getResult() != null)
                        ? taskImg.getResult() : new ArrayList<>();
                if (list == null) list = new ArrayList<>();

                logD("loadImages: got " + list.size() + " images in " + dtMs + "ms; tabuUrl=" + finalTabuUrl);

                images.setValue(list); // נשמור את הרשימה הפשוטה
                List<UiImageItem> sectioned = buildSectionedImages(list, finalTabuUrl); // נבנה כותרות + תמונות
                sectionedImages.setValue(sectioned);
                logD("buildSectionedImages: flatItems=" + sectioned.size());
            });
        });
    }


    /** Build a flat list: [Header, Photo, Photo, Header, Photo, ...] by category + optional Tabu image. */
    private List<UiImageItem> buildSectionedImages(@NonNull List<Image> all, @Nullable String tabuUrl) {
        List<UiImageItem> out = new ArrayList<>();
        if (all.isEmpty() && (tabuUrl == null || tabuUrl.trim().isEmpty())) {
            return out;
        }

        // סדר קטגוריות קבוע (מהתשובה הקודמת, כולל דלת כניסה)
        List<String> desiredOrder = Arrays.asList(
                "FRONT",         // חזית / בניין
                "ENTRANCE_DOOR", // דלת כניסה
                "LIVING_ROOM",   // סלון
                "KITCHEN",       // מטבח
                "BATHROOM",      // חדר רחצה
                "BEDROOM",       // חדרי שינה
                "VIEW",          // נוף
                "DOCUMENT",      // מסמכים
                "OTHER"          // אחר
        );

        // יצירת באקטים ריקים מראש, כדי לשמור על סדר יציב
        Map<String, List<Image>> byCategory = new LinkedHashMap<>();
        for (String key : desiredOrder) {
            byCategory.put(key, new ArrayList<>());
        }

        // פיזור התמונות לבאקטים לפי safeCategory
        for (Image im : all) {
            String key = safeCategory(im); // תמיד מחזיר אחת מהקטגוריות למעלה
            List<Image> bucket = byCategory.get(key);
            if (bucket == null) {
                bucket = new ArrayList<>();
                byCategory.put(key, bucket);
            }
            bucket.add(im);
        }

        int buckets = 0, headers = 0, photos = 0;

        // מוציאים רק באקטים לא ריקים, בסדר הרצוי
        for (String key : desiredOrder) {
            List<Image> bucket = byCategory.get(key);
            if (bucket == null || bucket.isEmpty()) continue;

            buckets++;
            out.add(UiImageItem.header(heTitle(key)));
            headers++;

            for (Image im : bucket) {
                out.add(UiImageItem.photo(im));
                photos++;
            }
        }

        // --- הוספת תמונת הטאבו (אם יש URL) ---
        if (tabuUrl != null && !tabuUrl.trim().isEmpty()) {
            logD("buildSectionedImages: adding TABU image section with url=" + tabuUrl);

            out.add(UiImageItem.header("טאבו"));

            // ניצור אובייקט Image "וירטואלי" רק לצורך התצוגה
            Image tabuImage = new Image();
            // בהנחה שיש setter לכתובת; אם השם אצלך שונה – פשוט לשנות כאן.
            tabuImage.setUrl(tabuUrl.trim());

            out.add(UiImageItem.photo(tabuImage));
            headers++;
            photos++;
            buckets++; // אפשרי לספירה סטטיסטית
        }

        logD("sectioned: buckets=" + buckets + " | headers=" + headers + " | photos=" + photos);
        return out;
    }


    /** Normalize category from model into one of the canonical keys in desiredOrder. */
    private String safeCategory(@Nullable Image im) {
        if (im == null) return "OTHER";

        try {
            Object catObj = im.getCategory(); // Enum or String
            String raw = (catObj == null) ? "" : String.valueOf(catObj).trim();
            if (raw.isEmpty()) {
                logD("safeCategory: empty -> OTHER");
                return "OTHER";
            }

            String norm = raw.toUpperCase(Locale.ROOT)
                    .replace(' ', '_')
                    .replace('-', '_')
                    .replace('/', '_');

            String resultKey;

            // 1. Exact / common variants
            switch (norm) {
                case "FRONT":
                case "FRONT_VIEW":
                case "EXTERIOR":
                case "FACADE":
                case "BUILDING_FRONT":
                    resultKey = "FRONT";
                    break;

                case "ENTRANCE_DOOR":
                case "MAIN_ENTRANCE":
                case "ENTRANCE":
                    resultKey = "ENTRANCE_DOOR";
                    break;

                case "LIVING_ROOM":
                case "SALON":
                    resultKey = "LIVING_ROOM";
                    break;

                case "KITCHEN":
                    resultKey = "KITCHEN";
                    break;

                case "BATHROOM":
                case "BATH":
                case "TOILET":
                case "RESTROOM":
                    resultKey = "BATHROOM";
                    break;

                case "BEDROOM":
                case "MASTER_BEDROOM":
                case "KIDS_BEDROOM":
                    resultKey = "BEDROOM";
                    break;

                case "VIEW":
                case "BALCONY_VIEW":
                case "WINDOW_VIEW":
                    resultKey = "VIEW";
                    break;

                case "DOCUMENT":
                case "DOC":
                case "PDF":
                    resultKey = "DOCUMENT";
                    break;

                case "OTHER":
                    resultKey = "OTHER";
                    break;

                default:
                    // 2. Fuzzy matching by substrings
                    if (norm.contains("FRONT") || norm.contains("FACADE") || norm.contains("EXTERIOR")) {
                        resultKey = "FRONT";
                    } else if (norm.contains("ENTRANCE") || (norm.contains("DOOR") && !norm.contains("INTERIOR"))) {
                        resultKey = "ENTRANCE_DOOR";
                    } else if (norm.contains("LIVING") || norm.contains("SALON")) {
                        resultKey = "LIVING_ROOM";
                    } else if (norm.contains("KITCHEN")) {
                        resultKey = "KITCHEN";
                    } else if (norm.contains("BATH") || norm.contains("TOILET") || norm.contains("WC")) {
                        resultKey = "BATHROOM";
                    } else if (norm.contains("BEDROOM") || norm.contains("ROOM")) {
                        resultKey = "BEDROOM";
                    } else if (norm.contains("VIEW") || norm.contains("BALCONY") || norm.contains("WINDOW")) {
                        resultKey = "VIEW";
                    } else if (norm.contains("DOC") || norm.contains("FORM") || norm.contains("SIGNATURE")) {
                        resultKey = "DOCUMENT";
                    } else {
                        // 3. Anything else – treat as true "Other".
                        resultKey = "OTHER";
                    }
                    break;
            }

            logD("safeCategory: raw=" + raw + " | norm=" + norm + " | resultKey=" + resultKey);
            return resultKey;

        } catch (Throwable t) {
            logE("safeCategory: failed, default OTHER", t);
            return "OTHER";
        }
    }


    /** Hebrew title per normalized category key. */
    private String heTitle(String key) {
        switch (key) {
            case "FRONT":         return "חזית / בניין";
            case "ENTRANCE_DOOR": return "דלת כניסה";
            case "LIVING_ROOM":   return "סלון";
            case "KITCHEN":       return "מטבח";
            case "BATHROOM":      return "חדר רחצה";
            case "BEDROOM":       return "חדרי שינה";
            case "VIEW":          return "נוף";
            case "DOCUMENT":      return "מסמכים";
            default:              return "אחר";
        }
    }

    // --- Helpers & UI models ---
    private static String orDash(@Nullable String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s;
    }
    private static String safeStr(@Nullable String s) {
        return (s == null) ? "" : s.trim();
    }

    /** Safely extract epochMillis from a Firestore field that may be Long/Timestamp/Date/Double/null. */
    private long extractMillis(@Nullable Object value) {
        if (value == null) return 0L;
        if (value instanceof Long)      return (Long) value;
        if (value instanceof Double)    return ((Double) value).longValue();
        if (value instanceof Timestamp) return ((Timestamp) value).toDate().getTime();
        if (value instanceof Date)      return ((Date) value).getTime();
        return 0L;
    }

    /** Format millis to "dd.MM.yyyy HH:mm" or "—" if zero/invalid. */
    private String formatMillis(long millis) {
        if (millis <= 0L) return "—";
        return DateFormat.format("dd.MM.yyyy HH:mm", new Date(millis)).toString();
    }

    @Nullable
    private String firstNonEmpty(String... opts) {
        if (opts == null) return null;
        for (String s : opts) if (s != null && !s.trim().isEmpty()) return s.trim();
        return null;
    }
    private long firstNonZero(long... vals) {
        if (vals == null) return 0L;
        for (long v : vals) if (v > 0L) return v;
        return 0L;
    }

    public static class ProjectHeaderUi {
        public final String address;
        public final String status;
        public final String clientName;
        public final String lastUpdated;
        public final boolean isError;
        public final String errorText;

        public ProjectHeaderUi(String address, String status, String clientName,
                               String lastUpdated, boolean isError, String errorText) {
            this.address = address;
            this.status = status;
            this.clientName = clientName;
            this.lastUpdated = lastUpdated;
            this.isError = isError;
            this.errorText = errorText;
        }

        public static ProjectHeaderUi error(String msg) {
            return new ProjectHeaderUi("—","—","—","—", true, msg);
        }
    }

    public static class KV {
        public final boolean isHeader;
        public final String key;
        public final String value;
        private KV(boolean isHeader, String key, String value) { this.isHeader = isHeader; this.key = key; this.value = value; }
        public static KV header(String title) { return new KV(true, title, ""); }
        public static KV row(String key, String value) { return new KV(false, key, value); }
    }

    public static class UiImageItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_PHOTO  = 1;

        public final int type;
        @Nullable public final String title; // used if header
        @Nullable public final Image image;  // used if photo

        private UiImageItem(int type, @Nullable String title, @Nullable Image image) {
            this.type = type;
            this.title = title;
            this.image = image;
        }

        public static UiImageItem header(@NonNull String title) { return new UiImageItem(TYPE_HEADER, title, null); }
        public static UiImageItem photo(@NonNull Image image)   { return new UiImageItem(TYPE_PHOTO,  null,  image); }
    }

    static class FieldSpec {
        final String label;      // UI caption (Hebrew)
        final String path;       // Original dotted path
        final String[] tokens;   // Reserved for FieldPath.of(...) if needed later

        FieldSpec(String label, String path) {
            this.label = label;
            this.path  = path;
            this.tokens = splitToTokens(path);
        }

        private static String[] splitToTokens(String dotted) {
            if (dotted == null || dotted.trim().isEmpty()) return new String[0];
            return dotted.split("\\.");
        }
    }

    static class SectionSpec {
        final String title;
        final List<FieldSpec> fields;
        SectionSpec(String title, List<FieldSpec> fields) { this.title = title; this.fields = fields; }
    }

    // ========== Diagnostics-only lookup helpers ==========
    private static class LookupResult {
        final String realKey; final Object value;
        LookupResult(String realKey, Object value) { this.realKey = realKey; this.value = value; }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> flatten(Object root) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!(root instanceof Map)) return out;
        flattenRec("", (Map<String, Object>) root, out);
        return out;
    }

    @SuppressWarnings("unchecked")
    private void flattenRec(String prefix, Map<String, Object> m, Map<String, Object> out) {
        if (m == null) return;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (val instanceof Map) {
                flattenRec(path, (Map<String, Object>) val, out);
            } else {
                out.put(path, val);
            }
        }
    }

    private Map<String, String> buildNormIndex(java.util.Set<String> keys) {
        Map<String, String> idx = new LinkedHashMap<>();
        for (String k : keys) idx.put(norm(k), k);
        return idx;
    }

    private String norm(String s) {
        if (s == null) return "";
        return s.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String toCamel(String s) {
        if (s == null) return "";
        String[] parts = s.split("[_\\-\\s/]");
        if (parts.length == 0) return s;
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(parts[i].substring(0,1).toUpperCase(Locale.ROOT)).append(parts[i].substring(1));
        }
        return sb.toString();
    }

    private String toCamelTokens(String dotted) {
        String[] t = dotted.split("\\.");
        for (int i = 0; i < t.length; i++) t[i] = toCamel(t[i]);
        return String.join(".", t);
    }

    private String toSnake(String s) {
        if (s == null) return "";
        String r = s.replaceAll("([a-z])([A-Z])", "$1_$2");
        r = r.replace('-', '_').replace(' ', '_').replace('/', '_');
        return r.toLowerCase(Locale.ROOT);
    }

    private String toSnakeTokens(String dotted) {
        String[] t = dotted.split("\\.");
        for (int i = 0; i < t.length; i++) t[i] = toSnake(t[i]);
        return String.join(".", t);
    }

    private LookupResult smartLookup(Map<String, Object> flat, Map<String, String> normIndex, String path) {
        if (flat.containsKey(path)) return new LookupResult(path, flat.get(path));
        String camel = toCamelTokens(path);
        if (flat.containsKey(camel)) return new LookupResult(camel, flat.get(camel));
        String snake = toSnakeTokens(path);
        if (flat.containsKey(snake)) return new LookupResult(snake, flat.get(snake));
        String real = normIndex.get(norm(path));
        if (real != null && flat.containsKey(real)) return new LookupResult(real, flat.get(real));
        String real2 = normIndex.get(norm(camel));
        if (real2 != null && flat.containsKey(real2)) return new LookupResult(real2, flat.get(real2));
        String real3 = normIndex.get(norm(snake));
        if (real3 != null && flat.containsKey(real3)) return new LookupResult(real3, flat.get(real3));
        return null;
    }

    // ---------- Manual aliases for stubborn keys ----------
    private static final Map<String, List<String>> FIELD_ALIASES;
    static {
        Map<String, List<String>> m = new LinkedHashMap<>();
        // Address variants
        m.put("full_address", Arrays.asList("fullAddress", "address.full", "addressFull"));
        // Elevator variants
        m.put("has_elevator", Arrays.asList("property_details.has_elevator", "propertyDetails.hasElevator"));
        // Example for municipal form number
        m.put("property_details.apartment_number(municipal_form)",
                Arrays.asList("property_details.apartment_number_municipal_form",
                        "propertyDetails.apartmentNumberMunicipalForm"));
        // Add more aliases here as needed
        FIELD_ALIASES = Collections.unmodifiableMap(m);
    }

    /**
     * Unified robust getter that tries:
     * 1) direct path,
     * 2) aliases from FIELD_ALIASES,
     * 3) smartLookup (camel/snake/normalized),
     * 4) suffix fallback (e.g., match last tokens regardless of prefix).
     */
    @Nullable
    private Object getByPath(@NonNull DocumentSnapshot doc,
                             @NonNull Map<String,Object> flat,
                             @NonNull Map<String,String> normIndex,
                             @NonNull String path) {

        // 1) direct
        Object v = doc.get(path);
        if (v != null) return v;

        // 2) aliases
        List<String> aliases = FIELD_ALIASES.get(path);
        if (aliases != null) {
            for (String alt : aliases) {
                Object vv = doc.get(alt);
                if (vv != null) {
                    logD("  • " + path + " ⇒ FOUND via alias " + alt + " = " + preview(vv));
                    return vv;
                }
            }
        }

        // 3) smart lookup
        LookupResult lr = smartLookup(flat, normIndex, path);
        if (lr != null) return lr.value;

        // 4) suffix fallback (match by last token(s))
        String[] toks = path.split("\\.");
        List<String> suffixes = new ArrayList<>();
        if (toks.length >= 1) suffixes.add(norm(toks[toks.length-1]));
        if (toks.length >= 2) suffixes.add(norm(toks[toks.length-2] + "." + toks[toks.length-1]));

        for (Map.Entry<String,Object> e : flat.entrySet()) {
            String kNorm = norm(e.getKey());
            for (String suf : suffixes) {
                if (kNorm.endsWith(suf)) {
                    logD("  • " + path + " ⇒ FOUND by suffix as " + e.getKey() + " = " + preview(e.getValue()));
                    return e.getValue();
                }
            }
        }

        logD("  • " + path + " ⇒ NOT FOUND (after fallbacks)");
        return null;
    }

    // ===== Sections =====
    private static final List<SectionSpec> SECTIONS = Arrays.asList(
            new SectionSpec("פרטי בנק", Arrays.asList(
                    new FieldSpec("שם בנק",                  "bankDetails.bankName"),
                    new FieldSpec("שם סניף",                 "bankDetails.branchName"),
                    new FieldSpec("אימייל סניף",             "bankDetails.branchEmail"),
                    new FieldSpec("שם בנקאי",                "bankDetails.bankerName"),
                    new FieldSpec("תאריך מסמך (לועזי)",      "bankDetails.documentDateGre"),
                    new FieldSpec("תאריך מסמך (עברי)",       "bankDetails.documentDateHe"),
                    new FieldSpec("מס׳ שומה",                "bankDetails.valuationNumber"),
                    new FieldSpec("מס׳ הלוואה",              "bankDetails.loanNumber"),
                    new FieldSpec("סוג הלוואה",              "bankDetails.typeOfLoan"),
                    new FieldSpec("כותרת עמוד 1",            "bankDetails.page1Header"),
                    new FieldSpec("מס׳ חלקה",                "bankDetails.lotNumber"),
                    new FieldSpec("גוש עיקרי",               "bankDetails.mainParcel"),
                    new FieldSpec("תת־חלקה",                 "bankDetails.subParcel"),
                    new FieldSpec("כתובת מקוצרת",            "bankDetails.shortAddress"),
                    new FieldSpec("שם הלווה",                 "bankDetails.loanerName"),
                    new FieldSpec("תעודת זהות הלווה",         "bankDetails.loanerId"),
                    new FieldSpec("מטרת ההלוואה",            "bankDetails.purposeOfLoan"),
                    new FieldSpec("זהות הלקוח",               "bankDetails.identityOfCustomer"),
                    new FieldSpec("תאריך סופי לשומה",         "bankDetails.appraisalFinalDate")
            )),
            new SectionSpec("פרטי לקוח", Arrays.asList(
                    new FieldSpec("שם הלקוח",    "client.fullName"),
                    new FieldSpec("ת״ז לקוח",    "client.clientId"),
                    new FieldSpec("טלפון",       "client.phoneNumber"),
                    new FieldSpec("אימייל",      "client.email")
            )),
            new SectionSpec("פרטי שמאי/מוסר", Arrays.asList(
                    new FieldSpec("שם מוסר",               "presenter_details.name_of_presenter"),
                    new FieldSpec("ת״ז מוסר",             "presenter_details.id_of_presenter"),
                    new FieldSpec("סוג מזהה מוסר",        "presenter_details.type_of_presenter_id"),
                    new FieldSpec("תפקיד המוסר",           "presenter_details.role_of_presenter"),
                    new FieldSpec("סטטוס מחזיק",          "presenter_details.holder_status")
            )),
            new SectionSpec("רישום וכתובת", Arrays.asList(
                    new FieldSpec("כתובת מלאה",               "full_address"),
                    new FieldSpec("בניין – כניסה",            "property_details.building_entry"),
                    new FieldSpec("בניין – מספר",             "property_details.building_number"),
                    new FieldSpec("מס׳ אזור",                 "property_details.zone_number"),
                    new FieldSpec("מס׳ תכנית בניין עיר",      "property_details.building_city_plan_number"),
                    new FieldSpec("תקציר נכס",                "property_details.property_summary")
            )),
            new SectionSpec("מאפייני סביבה ומבנה", Arrays.asList(
                    new FieldSpec("מאפייני סביבה",       "property_details.environment_characteristics"),
                    new FieldSpec("מיקום הנכס",          "property_details.property_location"),
                    new FieldSpec("סוג בניין",            "property_details.building_type"),
                    new FieldSpec("מצב הבניין",          "property_details.physical_condition"),
                    new FieldSpec("תחזוקה",               "property_details.maintenance"),
                    new FieldSpec("חומרי בנייה",          "property_details.construction_material"),
                    new FieldSpec("חיפוי חוץ",            "property_details.external_cladding"),
                    new FieldSpec("מס׳ קומות בבניין",     "property_details.number_of_floors"),
                    new FieldSpec("יש מעלית",             "has_elevator")
            )),
            new SectionSpec("פרטי דירה", Arrays.asList(
                    new FieldSpec("מס׳ דירה (טופס עירייה)",     "property_details.apartment_number(municipal_form)"),
                    new FieldSpec("קומה",                         "property_details.apartment_story"),
                    new FieldSpec("מס׳ חדרים",                    "property_details.number_of_rooms"),
                    new FieldSpec("שטח רשום (מ\"ר)",             "property_details.registered_apartment_area"),
                    new FieldSpec("שטח ברוטו (מ\"ר)",            "property_details.gross_apartment_area"),
                    new FieldSpec("כיווני אוויר",                 "property_details.apartment_directions"),
                    new FieldSpec("ריצוף",                        "apartment_flooring"),
                    new FieldSpec("חלונות",                       "apartment_windows"),
                    new FieldSpec("מטבח",                         "apartment_kitchen"),
                    new FieldSpec("דלת כניסה",                    "apartment_main_entrance_door"),
                    new FieldSpec("דלתות/משקופים פנימיים",        "apartment_interior_doors_and_frames"),
                    new FieldSpec("אמבטיה/כלים סניטריים",         "apartment_bathroom_fixtures"),
                    new FieldSpec("כולל בדירה",                   "property_details.apartment_includes"),
                    new FieldSpec("סורגים",                       "has_bars"),
                    new FieldSpec("מיזוג אוויר",                  "apartment_air_conditioning"),
                    new FieldSpec("חניה",                         "has_parking"),
                    new FieldSpec("מחסן",                         "has_storage"),
                    new FieldSpec("חימום מרכזי/קמין",             "central_heating_or_fireplace")
            ))
    );
}
