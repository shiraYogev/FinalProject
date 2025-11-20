// file: app/src/main/java/com/example/finalprojectappraisal/activity/AllProjects/ProjectPreviewActivity.java
package com.example.finalprojectappraisal.activity.AllProjects;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
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

    private SectionedImagesAdapter imagesAdapter; // sectioned headers + photos
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
        rvDetails.setNestedScrollingEnabled(false); // important inside NestedScrollView
        detailsAdapter = new KeyValueAdapter();
        rvDetails.setAdapter(detailsAdapter);

        // Images: sectioned (headers span 3, photos span 1)
        rvImages = findViewById(R.id.rvImages);
        rvImages.setNestedScrollingEnabled(false);

        GridLayoutManager glm = new GridLayoutManager(this, 3);
        imagesAdapter = new SectionedImagesAdapter();
        rvImages.setAdapter(imagesAdapter);

        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int vt = imagesAdapter.getItemViewType(position);
                return (vt == SectionedImagesAdapter.VT_HEADER) ? 3 : 1;
            }
        });
        rvImages.setLayoutManager(glm);

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

        vm.getDetails().observe(this, rows -> detailsAdapter.submit(rows));

        vm.getLoadingImages().observe(this, loading ->
                progressImages.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        vm.getSectionedImages().observe(this, list -> {
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

        @NonNull
        @Override
        public H onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_kv_row, parent, false);
            return new H(v);
        }

        @Override
        public void onBindViewHolder(@NonNull H h, int pos) {
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
                h.itemView.setPadding(
                        h.itemView.getPaddingLeft(),
                        (int) (h.itemView.getResources().getDisplayMetrics().density * 12),
                        h.itemView.getPaddingRight(),
                        h.itemView.getPaddingBottom()
                );
            } else {
                h.key.setTypeface(null, android.graphics.Typeface.NORMAL);
                // kv.key כבר כולל נקודתיים אם צריך, לכן לא מוסיפים " :"
                h.key.setText(kv.key);
                h.value.setText(kv.value == null ? "—" : kv.value);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class H extends RecyclerView.ViewHolder {
            TextView key, value;

            H(@NonNull View v) {
                super(v);
                key = v.findViewById(R.id.txtKey);
                value = v.findViewById(R.id.txtValue);
            }
        }
    }

    /**
     * Sectioned images adapter: category headers (full row) + photos (grid cells).
     * Requires:
     * - res/layout/item_image_header.xml  (TextView id: @id/txtHeader)
     * - res/layout/item_image_square.xml  (ImageView id: @id/image)
     * - res/layout/dialog_full_image.xml  (ImageView id: @id/fullImage, ImageButton id: @id/btnClose)
     */
    static class SectionedImagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        static final int VT_HEADER = 10;
        static final int VT_PHOTO = 11;

        private final List<ProjectPreviewViewModel.UiImageItem> data = new ArrayList<>();

        void submit(List<ProjectPreviewViewModel.UiImageItem> items) {
            data.clear();
            if (items != null) data.addAll(items);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            ProjectPreviewViewModel.UiImageItem it = data.get(position);
            return (it.type == ProjectPreviewViewModel.UiImageItem.TYPE_HEADER) ? VT_HEADER : VT_PHOTO;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VT_HEADER) {
                View v = android.view.LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_image_header, parent, false);
                return new HeaderVH(v);
            } else {
                View v = android.view.LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_image_square, parent, false);
                return new PhotoVH(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ProjectPreviewViewModel.UiImageItem it = data.get(position);

            if (holder instanceof HeaderVH) {
                HeaderVH hvh = (HeaderVH) holder;
                hvh.title.setText(it.title == null ? "—" : it.title);
            } else if (holder instanceof PhotoVH) {
                PhotoVH pvh = (PhotoVH) holder;
                String url = (it.image != null) ? it.image.getUrl() : null;

                if (url == null || url.trim().isEmpty()) {
                    pvh.img.setImageResource(android.R.drawable.ic_menu_report_image);
                    pvh.img.setOnClickListener(null);
                } else {
                    Glide.with(pvh.img.getContext()).load(url).into(pvh.img);

                    // Open full-screen dialog on click
                    pvh.img.setOnClickListener(v ->
                            showFullImageDialog(v.getContext(), url)
                    );
                }
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        /** Shows the tapped image in a full-screen dialog. */
        private void showFullImageDialog(Context context, String url) {
            if (url == null || url.trim().isEmpty()) return;

            Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_full_image);

            ImageView fullImage = dialog.findViewById(R.id.fullImage);
            ImageButton btnClose = dialog.findViewById(R.id.btnClose);

            Glide.with(context).load(url).into(fullImage);

            View.OnClickListener closeListener = v -> dialog.dismiss();
            btnClose.setOnClickListener(closeListener);
            fullImage.setOnClickListener(closeListener);

            dialog.show();
        }

        static class HeaderVH extends RecyclerView.ViewHolder {
            TextView title;

            HeaderVH(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.txtHeader);
            }
        }

        static class PhotoVH extends RecyclerView.ViewHolder {
            ImageView img;

            PhotoVH(@NonNull View v) {
                super(v);
                img = v.findViewById(R.id.image);
            }
        }
    }
}
