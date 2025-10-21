/**
 * Summary:
 * ViewModel for the Project Preview screen. Loads and formats project header,
 * details (by sections/fields), and images. Uses ProjectRepository as the single
 * DB access layer. Also exposes admin permission using AuthRepository (via the
 * Activity that provides the current userId).
 *
 * Notes:
 * - All formatting and dot-notation extraction happen here (keeps Activity lean).
 * - Exposes LiveData for loading states, header model, details rows, image list,
 *   sectioned images (headers + photos), and admin permission flag.
 */

package com.example.finalprojectappraisal.activity.AllProjects.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Appraiser;
import com.example.finalprojectappraisal.model.Image;
import com.example.finalprojectappraisal.model.Project;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProjectPreviewViewModel extends ViewModel {

    // Repositories (DB is always routed via ProjectRepository)
    private final ProjectRepository repo = ProjectRepository.getInstance();

    // Loading flags
    private final MutableLiveData<Boolean> loadingHeader = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loadingImages = new MutableLiveData<>(false);

    // UI models
    private final MutableLiveData<ProjectHeaderUi> header = new MutableLiveData<>();
    private final MutableLiveData<List<Image>> images = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<KV>> details = new MutableLiveData<>(new ArrayList<>());

    // NEW: sectioned (flat) list of images: headers + photos
    private final MutableLiveData<List<UiImageItem>> sectionedImages = new MutableLiveData<>(new ArrayList<>());

    // Admin permission (set via checkPermissionsFor(userId) from Activity)
    private final MutableLiveData<Boolean> isAdmin = new MutableLiveData<>(false);

    // (Optional) hook to current project object if needed in the future
    private final MediatorLiveData<Project> repoCurrentProject = new MediatorLiveData<>();

    // --- Expose LiveData to the UI ---
    public LiveData<Boolean> getLoadingHeader() { return loadingHeader; }
    public LiveData<Boolean> getLoadingImages() { return loadingImages; }
    public LiveData<ProjectHeaderUi> getHeader() { return header; }
    public LiveData<List<Image>> getImages() { return images; } // kept for backwards compatibility
    public LiveData<List<KV>> getDetails() { return details; }
    public LiveData<Boolean> getIsAdmin() { return isAdmin; }
    public LiveData<List<UiImageItem>> getSectionedImages() { return sectionedImages; }

    /** Initialize loads once the projectId is known (Activity calls this). */
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
        repo.getProject(projectId, task -> {
            loadingHeader.setValue(false);
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                header.setValue(ProjectHeaderUi.error("Failed to load project"));
                return;
            }
            bindHeader(task.getResult());
        });
    }

    private void bindHeader(DocumentSnapshot doc) {
        Project p = doc.toObject(Project.class);
        if (p == null) {
            header.setValue(ProjectHeaderUi.error("Project not found"));
            return;
        }

        String address = orDash(p.getFullAddress());
        String status  = orDash(p.getProjectStatus());
        String client  = (p.getClient() != null && p.getClient().getFullName() != null)
                ? p.getClient().getFullName() : "—";

        Long ts = (p.getLastUpdateDate() != null) ? p.getLastUpdateDate().getTime() : null;
        String last = (ts != null)
                ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
                .format(new Date(ts))
                : "—";

        header.setValue(new ProjectHeaderUi(address, status, client, last, false, null));
    }

    // --- Details (sections & fields via dot-notation) ---
    private void loadDetails(String projectId) {
        repo.getProject(projectId, task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                details.setValue(new ArrayList<>());
                return;
            }
            DocumentSnapshot doc = task.getResult();
            List<KV> rows = new ArrayList<>();

            for (SectionSpec sec : SECTIONS) {
                rows.add(KV.header(sec.title));
                for (FieldSpec f : sec.fields) {
                    Object raw = doc.get(f.path);
                    String display = toDisplayValue(raw);
                    if (display == null || display.trim().isEmpty()) display = "—";
                    rows.add(KV.row(f.label, display));
                }
            }
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
        String s = String.valueOf(val).trim();
        return s.isEmpty() ? null : s;
    }

    // --- Images ---
    private void loadImages(String projectId) {
        loadingImages.setValue(true);
        repo.loadAllImagesForProject(projectId, task -> {
            loadingImages.setValue(false);
            List<Image> list = (task.isSuccessful() && task.getResult() != null)
                    ? task.getResult() : new ArrayList<>();
            images.setValue(list); // keep plain list
            sectionedImages.setValue(buildSectionedImages(list)); // build headers + photos
        });
    }

    /** Build a flat list: [Header, Photo, Photo, Header, Photo, ...] by category. */
    private List<UiImageItem> buildSectionedImages(@NonNull List<Image> all) {
        List<UiImageItem> out = new ArrayList<>();
        if (all.isEmpty()) return out;

        // Desired category order (normalize to these keys)
        List<String> desiredOrder = Arrays.asList(
                "FRONT",        // חזית / בניין
                "LIVING_ROOM",  // סלון
                "KITCHEN",      // מטבח
                "BATHROOM",     // חדר רחצה
                "BEDROOM",      // חדרי שינה
                "VIEW",         // נוף
                "DOCUMENT",     // מסמכים (אופציונלי אם יש)
                "OTHER"         // אחר
        );

        // Group by category with stable insertion order
        Map<String, List<Image>> byCategory = new LinkedHashMap<>();
        for (String key : desiredOrder) byCategory.put(key, new ArrayList<>());

        // Distribute images to buckets
        for (Image im : all) {
            String key = safeCategory(im);
            if (!byCategory.containsKey(key)) {
                byCategory.put(key, new ArrayList<>()); // for any unexpected category
            }
            byCategory.get(key).add(im);
        }

        // Emit only non-empty buckets, in desired order first, then any extras
        for (String key : desiredOrder) {
            List<Image> bucket = byCategory.get(key);
            if (bucket == null || bucket.isEmpty()) continue;
            out.add(UiImageItem.header(heTitle(key)));
            for (Image im : bucket) out.add(UiImageItem.photo(im));
        }
        for (Map.Entry<String, List<Image>> e : byCategory.entrySet()) {
            String key = e.getKey();
            if (desiredOrder.contains(key)) continue;
            List<Image> bucket = e.getValue();
            if (bucket == null || bucket.isEmpty()) continue;
            out.add(UiImageItem.header(heTitle(key)));
            for (Image im : bucket) out.add(UiImageItem.photo(im));
        }

        return out;
    }

    /** Normalize category from model to an uppercase key that matches desiredOrder. */
    private String safeCategory(@Nullable Image im) {
        if (im == null) return "OTHER";
        try {
            Object catObj = im.getCategory(); // Enum or String
            if (catObj == null) return "OTHER";
            String s = String.valueOf(catObj).trim();
            if (s.isEmpty()) return "OTHER";
            // Normalize to UPPER_SNAKE_CASE-like
            s = s.toUpperCase(Locale.ROOT)
                    .replace(' ', '_')
                    .replace('-', '_')
                    .replace('/', '_');
            return s.isEmpty() ? "OTHER" : s;
        } catch (Throwable t) {
            return "OTHER";
        }
    }


    /** Hebrew title per normalized category key. */
    private String heTitle(String key) {
        switch (key) {
            case "FRONT":        return "חזית / בניין";
            case "LIVING_ROOM":  return "סלון";
            case "KITCHEN":      return "מטבח";
            case "BATHROOM":     return "חדר רחצה";
            case "BEDROOM":      return "חדרי שינה";
            case "VIEW":         return "נוף";
            case "DOCUMENT":     return "מסמכים";
            default:             return "אחר";
        }
    }

    // --- Helpers & UI models ---
    private static String orDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s;
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
        final String path;       // Original dotted path (for logs)
        final String[] tokens;   // Safe tokens for FieldPath.of(...)

        FieldSpec(String label, String path) {
            this.label = label;
            this.path  = path;
            this.tokens = splitToTokens(path);
        }

        private static String[] splitToTokens(String dotted) {
            if (dotted == null || dotted.trim().isEmpty()) return new String[0];
            // Split only by dots. Special chars like () - / remain inside the token.
            // Example: "property_details.apartment_number(municipal_form)"
            // -> ["property_details","apartment_number(municipal_form)"]
            return dotted.split("\\.");
        }
    }


    static class SectionSpec {
        final String title;
        final List<FieldSpec> fields;
        SectionSpec(String title, List<FieldSpec> fields) { this.title = title; this.fields = fields; }
    }

    // Sections copied from Activity to VM to keep UI thin
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
                    new FieldSpec("בניין – כניסה",            "building_entry"),
                    new FieldSpec("בניין – מספר",             "building_number"),
                    new FieldSpec("מס׳ אזור",                 "zone_number"),
                    new FieldSpec("מס׳ תכנית בניין עיר",      "building_city_plan_number"),
                    new FieldSpec("תקציר נכס",                "property_summary")
            )),
            new SectionSpec("מאפייני סביבה ומבנה", Arrays.asList(
                    new FieldSpec("מאפייני סביבה",       "environment_characteristics"),
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
