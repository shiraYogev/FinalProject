// file: app/src/main/java/com/example/finalprojectappraisal/activity/AllProjects/ProjectPreviewActivity.java
package com.example.finalprojectappraisal.activity.AllProjects;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Image;
import com.example.finalprojectappraisal.model.Project;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectPreviewActivity extends AppCompatActivity {

    private TextView txtTitle, txtStatus, txtClient, txtAddress, txtUpdated, txtImagesEmpty;
    private ProgressBar progressProject, progressImages;
    private RecyclerView rvImages, rvDetails;

    private ImagesGridAdapter imagesAdapter;
    private KeyValueAdapter detailsAdapter;

    private String projectId;
    private final ProjectRepository repo = ProjectRepository.getInstance();

    // ---------- helper specs for sections ----------
    static class FieldSpec {
        final String label;   // caption in Hebrew
        final String path;    // Firestore key (supports dot notation)
        FieldSpec(String label, String path) { this.label = label; this.path = path; }
    }
    static class SectionSpec {
        final String title;
        final List<FieldSpec> fields;
        SectionSpec(String title, List<FieldSpec> fields) { this.title = title; this.fields = fields; }
    }

    // ---------- sections (match your DB keys) ----------
    private static final List<SectionSpec> SECTIONS = Arrays.asList(
            // bankDetails.*
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

            // client.*
            new SectionSpec("פרטי לקוח", Arrays.asList(
                    new FieldSpec("שם הלקוח",    "client.fullName"),
                    new FieldSpec("ת״ז לקוח",    "client.clientId"),
                    new FieldSpec("טלפון",       "client.phoneNumber"),
                    new FieldSpec("אימייל",      "client.email")
            )),

            // presenter_details.*
            new SectionSpec("פרטי שמאי/מוסר", Arrays.asList(
                    new FieldSpec("שם מוסר",               "presenter_details.name_of_presenter"),
                    new FieldSpec("ת״ז מוסר",             "presenter_details.id_of_presenter"),
                    new FieldSpec("סוג מזהה מוסר",        "presenter_details.type_of_presenter_id"),
                    new FieldSpec("תפקיד המוסר",           "presenter_details.role_of_presenter"),
                    new FieldSpec("סטטוס מחזיק",          "presenter_details.holder_status")
            )),

            // root fields (registration + address)
            new SectionSpec("רישום וכתובת", Arrays.asList(
                    new FieldSpec("כתובת מלאה",               "full_address"),
                    new FieldSpec("בניין – כניסה",            "building_entry"),
                    new FieldSpec("בניין – מספר",             "building_number"),
                    new FieldSpec("מס׳ אזור",                 "zone_number"),
                    new FieldSpec("מס׳ תכנית בניין עיר",      "building_city_plan_number"),
                    new FieldSpec("תקציר נכס",                "property_summary")
            )),

            // property & environment (mix of root + property_details.*)
            new SectionSpec("מאפייני סביבה ומבנה", Arrays.asList(
                    new FieldSpec("מאפייני סביבה",       "environment_characteristics"),
                    new FieldSpec("מיקום הנכס",          "property_details.property_location"),
                    new FieldSpec("סוג בניין",            "property_details.building_type"),
                    new FieldSpec("מצב הבניין",          "property_details.physical_condition"),
                    new FieldSpec("תחזוקה",               "property_details.maintenance"),
                    new FieldSpec("חומרי בנייה",          "property_details.construction_material"),
                    new FieldSpec("חיפוי חוץ",            "property_details.external_cladding"),
                    new FieldSpec("מס׳ קומות בבניין",     "property_details.number_of_floors"),
                    new FieldSpec("יש מעלית",             "property_details.has_elevator")
            )),

            // apartment details (property_details.*)
            new SectionSpec("פרטי דירה", Arrays.asList(
                    new FieldSpec("מס׳ דירה (טופס עירייה)",     "property_details.apartment_number(municipal_form)"),
                    new FieldSpec("קומה",                         "property_details.apartment_story"),
                    new FieldSpec("מס׳ חדרים",                    "property_details.number_of_rooms"),
                    new FieldSpec("שטח רשום (מ\"ר)",             "property_details.registered_apartment_area"),
                    new FieldSpec("שטח ברוטו (מ\"ר)",            "property_details.gross_apartment_area"),
                    new FieldSpec("כיווני אוויר",                 "property_details.apartment_directions"),
                    new FieldSpec("ריצוף",                        "property_details.apartment_flooring"),
                    new FieldSpec("חלונות",                       "property_details.apartment_windows"),
                    new FieldSpec("מטבח",                         "property_details.apartment_kitchen"),
                    new FieldSpec("דלת כניסה",                    "property_details.apartment_main_entrance_door"),
                    new FieldSpec("דלתות/משקופים פנימיים",        "property_details.apartment_interior_doors_and_frames"),
                    new FieldSpec("אמבטיה/כלים סניטריים",         "property_details.apartment_bathroom_fixtures"),
                    new FieldSpec("כולל בדירה",                   "property_details.apartment_includes"),
                    new FieldSpec("סורגים",                       "property_details.has_bars"),
                    new FieldSpec("מיזוג אוויר",                  "property_details.apartment_air_conditioning"),
                    new FieldSpec("חניה",                         "property_details.has_parking"),
                    new FieldSpec("מחסן",                         "property_details.has_storage"),
                    new FieldSpec("חימום מרכזי/קמין",             "property_details.central_heating_or_fireplace")
            ))
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_preview);

        projectId = getIntent().getStringExtra("projectId");
        setTitle("צפייה בפרויקט");

        txtTitle = findViewById(R.id.txtTitle);
        txtStatus = findViewById(R.id.txtStatus);
        txtClient = findViewById(R.id.txtClient);
        txtAddress = findViewById(R.id.txtAddress);
        txtUpdated = findViewById(R.id.txtUpdated);
        txtImagesEmpty = findViewById(R.id.txtImagesEmpty);

        progressProject = findViewById(R.id.progressProject);
        progressImages = findViewById(R.id.progressImages);

        // Details table (RTL)
        rvDetails = findViewById(R.id.rvDetails);
        rvDetails.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        rvDetails.setLayoutManager(new LinearLayoutManager(this));
        detailsAdapter = new KeyValueAdapter();
        rvDetails.setAdapter(detailsAdapter);

        // Images grid
        rvImages = findViewById(R.id.rvImages);
        rvImages.setNestedScrollingEnabled(false);
        rvImages.setLayoutManager(new GridLayoutManager(this, 3));
        imagesAdapter = new ImagesGridAdapter(new ArrayList<>());
        rvImages.setAdapter(imagesAdapter);

        loadHeader();
        loadDetailsSummary();
        loadImages();
    }

    // --------- HEADER ---------
    private void loadHeader() {
        progressProject.setVisibility(View.VISIBLE);
        repo.getProject(projectId, task -> {
            progressProject.setVisibility(View.GONE);
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                txtTitle.setText("שגיאה בטעינת פרויקט");
                return;
            }
            bindHeader(task.getResult());
        });
    }

    private void bindHeader(DocumentSnapshot doc) {
        Project p = doc.toObject(Project.class);
        if (p == null) { txtTitle.setText("פרויקט לא נמצא"); return; }

        String address = safe(p.getFullAddress(), "—");
        String status  = safe(p.getProjectStatus(), "—");
        String client  = (p.getClient() != null && p.getClient().getFullName() != null) ? p.getClient().getFullName() : "—";

        Long ts = null;
        if (p.getLastUpdateDate() != null) ts = p.getLastUpdateDate().getTime();
        String last = (ts != null) ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()).format(new Date(ts)) : "—";

        txtTitle.setText(address);
        txtAddress.setText(address);
        txtStatus.setText("סטטוס: " + status);
        txtClient.setText("לקוח: " + client);
        txtUpdated.setText("עודכן: " + last);
    }

    // --------- DETAILS (sections & fields) ---------
    private void loadDetailsSummary() {
        repo.getProject(projectId, task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                detailsAdapter.submit(new ArrayList<>());
                return;
            }
            DocumentSnapshot doc = task.getResult();

            List<KeyValue> rows = new ArrayList<>();

            for (SectionSpec sec : SECTIONS) {
                // section header row (always shown)
                rows.add(KeyValue.header(sec.title));

                for (FieldSpec f : sec.fields) {
                    Object raw = doc.get(f.path);        // supports dot-notation
                    String display = toDisplayValue(raw);
                    if (display == null || display.trim().isEmpty()) display = "—";
                    rows.add(KeyValue.row(f.label, display));
                }
            }

            detailsAdapter.submit(rows);
        });
    }

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

    private static String safe(String s, String def) { return (s == null || s.trim().isEmpty()) ? def : s; }

    // --------- IMAGES ---------
    private void loadImages() {
        progressImages.setVisibility(View.VISIBLE);
        repo.loadAllImagesForProject(projectId, task -> {
            progressImages.setVisibility(View.GONE);

            List<Image> list = (task.isSuccessful() && task.getResult() != null)
                    ? task.getResult()
                    : java.util.Collections.emptyList();

            if (list.isEmpty()) {
                txtImagesEmpty.setText("אין תמונות להצגה");
                txtImagesEmpty.setVisibility(View.VISIBLE);
                imagesAdapter.submit(java.util.Collections.emptyList());
                return;
            }

            txtImagesEmpty.setVisibility(View.GONE);
            imagesAdapter.submit(list);
        });
    }

    /** --- Adapters --- */

    // Key/Value with header support
    static class KeyValueAdapter extends RecyclerView.Adapter<KeyValueAdapter.H> {
        private final List<KeyValue> data = new ArrayList<>();

        void submit(List<KeyValue> rows) {
            data.clear();
            if (rows != null) data.addAll(rows);
            notifyDataSetChanged();
        }

        @NonNull @Override public H onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_kv_row, parent, false);
            return new H(v);
        }

        @Override public void onBindViewHolder(@NonNull H h, int pos) {
            KeyValue kv = data.get(pos);

            // RTL align
            h.key.setTextDirection(View.TEXT_DIRECTION_RTL);
            h.value.setTextDirection(View.TEXT_DIRECTION_RTL);
            h.key.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            h.value.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);

            if (kv.isHeader) {
                // header style: bold key, empty value, extra top padding
                h.key.setText(kv.key);
                h.key.setTypeface(h.key.getTypeface(), android.graphics.Typeface.BOLD);
                h.value.setText("");
                h.itemView.setPadding(h.itemView.getPaddingLeft(),
                        (int) (h.itemView.getResources().getDisplayMetrics().density * 12),
                        h.itemView.getPaddingRight(),
                        h.itemView.getPaddingBottom());
            } else {
                // normal row
                h.key.setTypeface(null, android.graphics.Typeface.NORMAL);
                h.key.setText(kv.key + " :");
                h.value.setText(kv.value == null ? "—" : kv.value);
            }
        }

        @Override public int getItemCount() { return data.size(); }

        static class H extends RecyclerView.ViewHolder {
            TextView key, value;
            H(@NonNull View v) { super(v); key = v.findViewById(R.id.txtKey); value = v.findViewById(R.id.txtValue); }
        }
    }

    static class KeyValue {
        final boolean isHeader;
        final String key;
        final String value;
        private KeyValue(boolean isHeader, String key, String value) {
            this.isHeader = isHeader; this.key = key; this.value = value;
        }
        static KeyValue header(String title) { return new KeyValue(true, title, ""); }
        static KeyValue row(String key, String value) { return new KeyValue(false, key, value); }
    }

    // Images grid (URL/content://)
    static class ImagesGridAdapter extends RecyclerView.Adapter<ImagesGridAdapter.H> {
        private final List<Image> data = new ArrayList<>();

        ImagesGridAdapter(List<Image> init) {
            if (init != null) data.addAll(init);
        }

        void submit(List<Image> items) {
            data.clear();
            if (items != null) data.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull @Override public H onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image_square, parent, false);
            return new H(v);
        }

        @Override public void onBindViewHolder(@NonNull H h, int pos) {
            Image im = data.get(pos);
            String url = (im != null) ? im.getUrl() : null;

            if (url == null || url.trim().isEmpty()) {
                h.img.setImageResource(android.R.drawable.ic_menu_report_image);
            } else {
                Glide.with(h.img.getContext()).load(url).into(h.img);
            }
        }

        @Override public int getItemCount() { return data.size(); }

        static class H extends RecyclerView.ViewHolder {
            android.widget.ImageView img;
            H(@NonNull View v) { super(v); img = v.findViewById(R.id.image); }
        }
    }
}
