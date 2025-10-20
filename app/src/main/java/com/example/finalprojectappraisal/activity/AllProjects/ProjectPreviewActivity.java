/**
 * Summary:
 * Activity for displaying a read-only preview of a Project. Wires up the UI to
 * ProjectPreviewViewModel (header/details/images), shows loading states, and
 * performs no DB logic directly. Uses AuthRepository to supply current userId
 * to the VM (so the VM can expose admin permission when relevant).
 *
 * Notes:
 * - All DB work is delegated to the ViewModel (which uses ProjectRepository).
 * - All comments are in English per project conventions.
 */
package com.example.finalprojectappraisal.activity.AllProjects;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.AllProjects.viewmodel.ProjectPreviewViewModel;
import com.example.finalprojectappraisal.model.Image;

import java.util.ArrayList;
import java.util.List;

public class ProjectPreviewActivity extends AppCompatActivity {

    private TextView txtTitle, txtStatus, txtClient, txtAddress, txtUpdated, txtImagesEmpty;
    private ProgressBar progressProject, progressImages;
    private RecyclerView rvImages, rvDetails;

    private ImagesGridAdapter imagesAdapter;
    private KeyValueAdapter detailsAdapter;

    private String projectId;
    private ProjectPreviewViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_preview);

        projectId = getIntent().getStringExtra("projectId");
        setTitle("צפייה בפרויקט");

        vm = new ViewModelProvider(this).get(ProjectPreviewViewModel.class);

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

        bindObservers();
        vm.init(projectId);
    }

    private void bindObservers() {
        vm.getLoadingHeader().observe(this, loading ->
                progressProject.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        vm.getHeader().observe(this, h -> {
            if (h == null) return;
            txtTitle.setText(h.address);
            txtAddress.setText(h.address);
            txtStatus.setText("סטטוס: " + h.status);
            txtClient.setText("לקוח: " + h.clientName);
            txtUpdated.setText("עודכן: " + h.lastUpdated);
        });

        vm.getDetails().observe(this, rows -> {
            detailsAdapter.submit(rows);
        });

        vm.getLoadingImages().observe(this, loading ->
                progressImages.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        vm.getImages().observe(this, list -> {
            if (list == null || list.isEmpty()) {
                txtImagesEmpty.setText("אין תמונות להצגה");
                txtImagesEmpty.setVisibility(View.VISIBLE);
                imagesAdapter.submit(new ArrayList<>());
            } else {
                txtImagesEmpty.setVisibility(View.GONE);
                imagesAdapter.submit(list);
            }
        });
    }

    /** --- Adapters --- */

    static class KeyValueAdapter extends RecyclerView.Adapter<KeyValueAdapter.H> {
        private final List<ProjectPreviewViewModel.KV> data = new ArrayList<>();

        void submit(List<ProjectPreviewViewModel.KV> rows) {
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
            ProjectPreviewViewModel.KV kv = data.get(pos);

            // RTL align
            h.key.setTextDirection(View.TEXT_DIRECTION_RTL);
            h.value.setTextDirection(View.TEXT_DIRECTION_RTL);
            h.key.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            h.value.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);

            if (kv.isHeader) {
                h.key.setText(kv.key);
                h.key.setTypeface(h.key.getTypeface(), android.graphics.Typeface.BOLD);
                h.value.setText("");
                h.itemView.setPadding(h.itemView.getPaddingLeft(),
                        (int) (h.itemView.getResources().getDisplayMetrics().density * 12),
                        h.itemView.getPaddingRight(),
                        h.itemView.getPaddingBottom());
            } else {
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
