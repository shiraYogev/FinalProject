package com.example.finalprojectappraisal.activity.AllProjects;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.model.Image;
import com.example.finalprojectappraisal.model.Project;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.material.imageview.ShapeableImageView;

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

    // הרשאות למדיה (Android 13+ / ישנות)
    private ActivityResultLauncher<String[]> permLauncher;

    // סדר תצוגת השדות (מפתחות Firestore → תווית בעברית)
    private static final LinkedHashMap<String, String> FIELD_LABELS = new LinkedHashMap<>();
    static {
        FIELD_LABELS.put("number_of_rooms", "מס׳ חדרים");
        FIELD_LABELS.put("physical_condition", "מצב הבניין");
        FIELD_LABELS.put("has_elevator", "מעלית");
        FIELD_LABELS.put("has_parking", "חניה");
        FIELD_LABELS.put("has_storage", "מחסן");
        FIELD_LABELS.put("apartment_flooring", "ריצוף");
        FIELD_LABELS.put("apartment_windows", "חלונות");
        FIELD_LABELS.put("apartment_kitchen", "מטבח");
        FIELD_LABELS.put("apartment_bathroom_fixtures", "אמבטיה/כלים סניטריים");
        FIELD_LABELS.put("registered_apartment_area", "שטח רשום (מ\"ר)");
        FIELD_LABELS.put("gross_apartment_area", "שטח ברוטו (מ\"ר)");
        FIELD_LABELS.put("apartment_story", "קומה");
        FIELD_LABELS.put("apartment_number(municipal_form)", "מס׳ דירה (טופס עירייה)");
        FIELD_LABELS.put("property_location", "מיקום");
        FIELD_LABELS.put("building_type", "סוג בניין");
        FIELD_LABELS.put("number_of_floors", "מס׳ קומות בבניין");
        FIELD_LABELS.put("apartment_directions", "כיווני אוויר");
    }

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

        // Details table
        rvDetails = findViewById(R.id.rvDetails);
        rvDetails.setLayoutManager(new LinearLayoutManager(this));
        detailsAdapter = new KeyValueAdapter();
        rvDetails.setAdapter(detailsAdapter);

        // Images grid
        rvImages = findViewById(R.id.rvImages);
        rvImages.setNestedScrollingEnabled(false);
        rvImages.setLayoutManager(new GridLayoutManager(this, 3));
        imagesAdapter = new ImagesGridAdapter(new ArrayList<>());
        rvImages.setAdapter(imagesAdapter);

        // הרשאות לתמונות (content://) ואז טעינה
        permLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> loadImages()
        );

        loadHeader();
        loadDetailsSummary();
        ensureImagesPermissionThenLoad(); // במקום loadImages() ישיר
    }

    // --------- HEADER (כותרת) ---------
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

    // --------- DETAILS (טבלת שדות) ---------
    private void loadDetailsSummary() {
        repo.getProjectFieldsForSummary(projectId, new OnCompleteListener<Map<String, Object>>() {
            @Override
            public void onComplete(Task<Map<String, Object>> task) {
                if (!task.isSuccessful() || task.getResult() == null || task.getResult().isEmpty()) {
                    detailsAdapter.submit(new ArrayList<>());
                    return;
                }
                Map<String, Object> raw = task.getResult();
                List<KeyValue> rows = new ArrayList<>();
                for (Map.Entry<String, String> e : FIELD_LABELS.entrySet()) {
                    String key = e.getKey();
                    if (!raw.containsKey(key)) continue;
                    Object val = raw.get(key);
                    String display = toDisplayValue(key, val);
                    if (display == null || display.trim().isEmpty()) continue;
                    rows.add(new KeyValue(e.getValue(), display));
                }
                detailsAdapter.submit(rows);
            }
        });
    }

    private String toDisplayValue(String key, Object val) {
        if (val == null) return null;

        // בוליאנים → כן/לא
        if (val instanceof Boolean) return ((Boolean) val) ? "כן" : "לא";

        // רשימות → פסיקים
        if (val instanceof List<?>) {
            List<?> list = (List<?>) val;
            List<String> parts = new ArrayList<>();
            for (Object o : list) if (o != null) parts.add(String.valueOf(o));
            return parts.isEmpty() ? null : String.join(", ", parts);
        }

        // כל השאר כטקסט
        String s = String.valueOf(val).trim();
        return s.isEmpty() ? null : s;
    }

    private static String safe(String s, String def) { return (s == null || s.trim().isEmpty()) ? def : s; }

    // --------- הרשאות תמונות ---------
    private void ensureImagesPermissionThenLoad() {
        String[] perms;
        if (Build.VERSION.SDK_INT >= 33) {
            perms = new String[]{ android.Manifest.permission.READ_MEDIA_IMAGES };
        } else {
            perms = new String[]{ android.Manifest.permission.READ_EXTERNAL_STORAGE };
        }

        boolean granted = true;
        for (String p : perms) {
            granted &= (ContextCompat.checkSelfPermission(this, p)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED);
        }
        if (granted) {
            loadImages();
        } else {
            permLauncher.launch(perms);
        }
    }

    // --------- IMAGES (גלריה) ---------
    private void loadImages() {
        progressImages.setVisibility(View.VISIBLE);

        repo.loadAllImagesForProject(projectId, task -> {
            progressImages.setVisibility(View.GONE);

            List<Image> list = (task.isSuccessful() && task.getResult() != null)
                    ? task.getResult()
                    : java.util.Collections.emptyList();

            Log.d("Preview", "images.size=" + list.size());

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

    // טבלת שדות (מפתח/ערך)
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
            h.key.setText(kv.key);
            h.value.setText(kv.value);
        }

        @Override public int getItemCount() { return data.size(); }

        static class H extends RecyclerView.ViewHolder {
            TextView key, value;
            H(@NonNull View v) { super(v); key = v.findViewById(R.id.txtKey); value = v.findViewById(R.id.txtValue); }
        }
    }

    static class KeyValue {
        final String key;
        final String value;
        KeyValue(String key, String value) { this.key = key; this.value = value; }
    }

    // גריד תמונות (URL/URI בלבד)
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

            RequestManager rm = Glide.with(h.img.getContext());
            rm.load(url) // Glide תומך ב-http/https/content://
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_delete)
                    .centerCrop()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e("ImagesAdapter", "Glide failed for: " + model, e);
                            return false; // תן ל-Glide להציג error drawable
                        }
                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(h.img);
        }

        @Override public int getItemCount() { return data.size(); }

        static class H extends RecyclerView.ViewHolder {
            ShapeableImageView img; // תואם ל-item_image_square.xml עם @id/image
            H(@NonNull View v) { super(v); img = v.findViewById(R.id.image); }
        }
    }
}
