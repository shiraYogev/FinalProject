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
import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.model.Image;
import com.example.finalprojectappraisal.model.Project;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProjectPreviewActivity extends AppCompatActivity {

    private TextView txtTitle, txtStatus, txtClient, txtAddress, txtUpdated, txtImagesEmpty;
    private ProgressBar progressProject, progressImages;
    private RecyclerView rvImages, rvDetails;

    private ImagesGridAdapter imagesAdapter;
    private KeyValueAdapter detailsAdapter;

    private String projectId;
    private final ProjectRepository repo = ProjectRepository.getInstance();

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

        // טבלת פרטים (RTL)
        rvDetails = findViewById(R.id.rvDetails);
        rvDetails.setLayoutManager(new LinearLayoutManager(this));
        detailsAdapter = new KeyValueAdapter();
        rvDetails.setAdapter(detailsAdapter);

        // גריד תמונות
        rvImages = findViewById(R.id.rvImages);
        rvImages.setNestedScrollingEnabled(false);
        rvImages.setLayoutManager(new GridLayoutManager(this, 3));
        imagesAdapter = new ImagesGridAdapter(new ArrayList<>());
        rvImages.setAdapter(imagesAdapter);

        loadHeader();
        loadDetails();    // ← בונה בלוקים מלאים מתוך כל הדוקומנט
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
        String last = (ts != null)
                ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()).format(new Date(ts))
                : "—";

        txtTitle.setText(address);
        txtAddress.setText(address);
        txtStatus.setText("סטטוס: " + status);
        txtClient.setText("לקוח: " + client);
        txtUpdated.setText("עודכן: " + last);
    }

    // --------- DETAILS (בלוקים + שורות) ---------
    private void loadDetails() {
        // נשלוף את כל המסמך כדי לכסות את כל השדות ב-JSON
        repo.getProject(projectId, task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                detailsAdapter.submit(new ArrayList<>());
                return;
            }
            Map<String, Object> m = task.getResult().getData();
            if (m == null) m = new LinkedHashMap<>();

            List<Row> rows = new ArrayList<>();

            // === פרטי בנק ===
            rows.add(Row.header("פרטי בנק"));
            addIfNotEmpty(rows, "שם בנק", m.get("bank_name"));
            addIfNotEmpty(rows, "שם סניף", m.get("branch_name"));
            addIfNotEmpty(rows, "אימייל הסניף", m.get("branch_email"));
            addIfNotEmpty(rows, "שם בנקאי", m.get("banker_name"));
            addIfNotEmpty(rows, "תאריך מסמך (לועזי)", m.get("document_date_gre"));
            addIfNotEmpty(rows, "תאריך מסמך (עברי)", m.get("document_date_he"));
            addIfNotEmpty(rows, "מס׳ שומה", m.get("valuation_number"));
            addIfNotEmpty(rows, "מס׳ הלוואה", m.get("loan_number"));
            addIfNotEmpty(rows, "סוג הלוואה", m.get("type_of_loan"));
            addIfNotEmpty(rows, "כותרת עמוד 1", m.get("page1_header"));

            // === פרטי שמאי/מוסר ===
            rows.add(Row.header("פרטי שמאי/מוסר"));
            addIfNotEmpty(rows, "שם שמאי", m.get("appraiser_name"));
            addIfNotEmpty(rows, "תאריך שמאות", m.get("appraisal_date"));
            addIfNotEmpty(rows, "תפקיד שמאי", m.get("appraiser_role"));
            addIfNotEmpty(rows, "שם מוסר", m.get("name_of_presenter"));
            addIfNotEmpty(rows, "ת״ז מוסר", m.get("id_of_presenter"));
            addIfNotEmpty(rows, "סוג תעודה", m.get("type_of_presenter_id"));
            addIfNotEmpty(rows, "תפקיד מוסר", m.get("role_of_presenter"));
            addIfNotEmpty(rows, "מצב מחזיק", m.get("holder_status"));
            addIfNotEmpty(rows, "תאריך סופי לשומה", m.get("appraisal_final_date"));

            // === רישום וכתובת ===
            rows.add(Row.header("רישום וכתובת"));
            addIfNotEmpty(rows, "מס׳ חלקה", m.get("lot_number"));
            addIfNotEmpty(rows, "גוש ראשי", m.get("main_parcel"));
            addIfNotEmpty(rows, "גוש משנה", m.get("sub_parcel"));
            addIfNotEmpty(rows, "כתובת קצרה", m.get("short_address"));
            addIfNotEmpty(rows, "כתובת מלאה", m.get("full_address"));
            addIfNotEmpty(rows, "כניסה", m.get("building_entry"));
            addIfNotEmpty(rows, "מס׳ בניין", m.get("building_number"));
            addIfNotEmpty(rows, "מס׳ אזור", m.get("zone_number"));
            addIfNotEmpty(rows, "תכנית עיר", m.get("building_city_plan_number"));
            addIfNotEmpty(rows, "תאריך מסמכי רישום", m.get("registration_document_date"));
            addIfNotEmpty(rows, "תקציר נכס", m.get("property_summary"));

            // === מאפייני סביבה ומבנה ===
            rows.add(Row.header("מאפייני סביבה ומבנה"));
            addIfNotEmpty(rows, "מאפייני סביבה", m.get("environment_characteristics"));
            addIfNotEmpty(rows, "מיקום", m.get("property_location"));
            addIfNotEmpty(rows, "סוג בניין", m.get("building_type"));
            addIfNotEmpty(rows, "מצב פיזי", m.get("physical_condition"));
            addIfNotEmpty(rows, "תחזוקה", m.get("maintenance"));
            addIfNotEmpty(rows, "חומר בניה", m.get("construction_material"));
            addIfNotEmpty(rows, "חיפוי חוץ", m.get("external_cladding"));
            addIfNotEmpty(rows, "מס׳ קומות", m.get("number_of_floors"));

            // === דירה (נתונים כלליים) ===
            rows.add(Row.header("פרטי דירה"));
            addIfNotEmpty(rows, "מס׳ דירה (טופס עירייה)", m.get("apartment_number(municipal_form)"));
            addIfNotEmpty(rows, "קומה", m.get("apartment_story"));
            addIfNotEmpty(rows, "מס׳ חדרים", m.get("number_of_rooms"));
            addIfNotEmpty(rows, "כולל", m.get("apartment_includes"));
            addIfNotEmpty(rows, "כיווני אוויר", m.get("apartment_directions"));
            addIfNotEmpty(rows, "שטח רשום (מ\"ר)", m.get("registered_apartment_area"));
            addIfNotEmpty(rows, "שטח ברוטו (מ\"ר)", m.get("gross_apartment_area"));

            // === חומרים וגמרים ===
            rows.add(Row.header("חומרים וגמרים"));
            addIfNotEmpty(rows, "מטבח", m.get("apartment_kitchen"));
            addIfNotEmpty(rows, "ריצוף", m.get("apartment_flooring"));
            addIfNotEmpty(rows, "דלת כניסה", m.get("apartment_main_entrance_door"));
            addIfNotEmpty(rows, "דלתות/משקופים פנימיים", m.get("apartment_interior_doors_and_frames"));
            addIfNotEmpty(rows, "חלונות", m.get("apartment_windows"));

            // === מתקנים ===
            rows.add(Row.header("מתקנים"));
            addYesNo(rows, "מעלית", m.get("has_elevator"));
            addYesNo(rows, "חניה", m.get("has_parking"));
            addYesNo(rows, "מחסן", m.get("has_storage"));
            addYesNo(rows, "חימום מרכזי/קמין", m.get("central_heating_or_fireplace"));
            addIfNotEmpty(rows, "מיזוג אוויר", m.get("apartment_air_conditioning"));
            addYesNo(rows, "סורגים", m.get("has_bars"));

            detailsAdapter.submit(rows);
        });
    }

    private static void addIfNotEmpty(List<Row> rows, String label, Object value) {
        if (value == null) return;
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return;
        rows.add(Row.line(label + " : " + s));
    }

    private static void addYesNo(List<Row> rows, String label, Object value) {
        boolean b = false;
        if (value instanceof Boolean) b = (Boolean) value;
        else if (value != null) b = "true".equalsIgnoreCase(String.valueOf(value));
        rows.add(Row.line(label + " : " + (b ? "כן" : "לא")));
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

    // שורות (כותרת או שורה רגילה — טקסט בודד, RTL)
    static class KeyValueAdapter extends RecyclerView.Adapter<KeyValueAdapter.H> {
        private final List<Row> data = new ArrayList<>();

        void submit(List<Row> rows) {
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
            Row row = data.get(pos);

            // reset
            h.txtHeader.setVisibility(View.GONE);
            h.txtLine.setVisibility(View.GONE);
            h.divider.setVisibility(View.VISIBLE);

            ViewGroup.MarginLayoutParams rootLp = (ViewGroup.MarginLayoutParams) h.root.getLayoutParams();
            if (rootLp != null) { rootLp.topMargin = 0; h.root.setLayoutParams(rootLp); }

            if (row.isHeader) {
                h.txtHeader.setText(row.text);
                h.txtHeader.setVisibility(View.VISIBLE);
                h.divider.setVisibility(View.GONE);
                if (rootLp != null) { rootLp.topMargin = (pos == 0) ? 8 : 16; h.root.setLayoutParams(rootLp); }
            } else {
                h.txtLine.setText(row.text);
                h.txtLine.setVisibility(View.VISIBLE);
            }
        }

        @Override public int getItemCount() { return data.size(); }

        static class H extends RecyclerView.ViewHolder {
            View root, divider;
            TextView txtHeader, txtLine;
            H(@NonNull View v) {
                super(v);
                root = v.findViewById(R.id.rowRoot);
                txtHeader = v.findViewById(R.id.txtHeader);
                txtLine = v.findViewById(R.id.txtLine);
                divider = v.findViewById(R.id.divider);
            }
        }
    }

    static class Row {
        final String text;
        final boolean isHeader;
        Row(String t, boolean h) { text = t; isHeader = h; }
        static Row header(String t) { return new Row(t, true); }
        static Row line(String t) { return new Row(t, false); }
    }

    // גריד תמונות
    static class ImagesGridAdapter extends RecyclerView.Adapter<ImagesGridAdapter.H> {
        private final List<Image> data = new ArrayList<>();
        ImagesGridAdapter(List<Image> init) { if (init != null) data.addAll(init); }
        void submit(List<Image> items) { data.clear(); if (items != null) data.addAll(items); notifyDataSetChanged(); }

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
