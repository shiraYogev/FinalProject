package com.example.finalprojectappraisal.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
// import com.bumptech.glide.Glide; // השאירי אם תרצי טעינת תמונות ממוזערות
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.model.Project;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectsAdapter extends RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder> {

    public interface ProjectActionListener {
        void onEdit(Project project);
        void onImages(Project project);
        void onReport(Project project);
        void onDelete(Project project);
    }

    private List<Project> projects;
    private final ProjectActionListener listener;
    private final Context context;

    public ProjectsAdapter(List<Project> projects, ProjectActionListener listener, Context context) {
        this.projects = projects;
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_project_card, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projects.get(position);

        // כתובת
        holder.txtAddress.setText(safeOrDash(project != null ? project.getFullAddress() : null));

        // *** הערה אופציונלית – מוסתרת אם ריקה ***
        String note = project != null ? project.getNote() : null;
        if (TextUtils.isEmpty(note)) {
            holder.txtNote.setVisibility(View.GONE);
        } else {
            holder.txtNote.setVisibility(View.VISIBLE);
            holder.txtNote.setText(note);
        }

        // סטטוס
        String status = project != null ? project.getProjectStatus() : null;
        holder.txtStatus.setText(!TextUtils.isEmpty(status) ? status : "סטטוס לא ידוע");

        // שם לקוח
        String clientName = (project != null && project.getClient() != null)
                ? project.getClient().getFullName()
                : "";
        holder.txtClient.setText(safeOrDash(clientName));

        // תאריך עדכון (Long -> "dd/MM/yyyy")
        long lastUpdate = (project != null) ? project.getLastUpdateDate() : 0L;
        holder.txtDate.setText(lastUpdate > 0
                ? "עודכן: " + formatDate(lastUpdate)
                : "");

        // תמונה ממוזערת (לא חובה)
        // String imageUrl = project != null ? project.getFrontImageUrl() : null;
        // Glide.with(context)
        //        .load(imageUrl)
        //        .placeholder(R.drawable.ic_placeholder)
        //        .into(holder.imageThumb);

        // כפתורי פעולה (שומרים על ההתנהגות המקורית שלך)
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null && project != null) listener.onEdit(project);
        });
        holder.btnImages.setOnClickListener(v -> {
            if (listener != null && project != null) listener.onImages(project);
        });
        holder.btnReport.setOnClickListener(v -> {
            if (listener != null && project != null) listener.onReport(project);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null && project != null) listener.onDelete(project);
        });
    }

    @Override
    public int getItemCount() {
        return projects == null ? 0 : projects.size();
    }

    public void updateData(List<Project> newProjects) {
        this.projects = newProjects;
        notifyDataSetChanged();
    }

    // ===== Utils =====

    private String safeOrDash(String s) {
        return TextUtils.isEmpty(s) ? "—" : s;
    }

    private String formatDate(long epochMillis) {
        // אם אצלך זה שניות – המריא לפני: epochMillis *= 1000;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("he", "IL"));
        return sdf.format(new Date(epochMillis));
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb;
        TextView txtAddress, txtNote, txtStatus, txtClient, txtDate;
        Button btnEdit, btnImages, btnReport, btnDelete;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
            txtAddress = itemView.findViewById(R.id.txtAddress);
            txtNote    = itemView.findViewById(R.id.txtNote);   // ודאי שהוספת ב-XML של האייטם
            txtStatus  = itemView.findViewById(R.id.txtStatus);
            txtClient  = itemView.findViewById(R.id.txtClient);
            txtDate    = itemView.findViewById(R.id.txtDate);
            btnEdit    = itemView.findViewById(R.id.btnEdit);
            btnImages  = itemView.findViewById(R.id.btnImages);
            btnReport  = itemView.findViewById(R.id.btnReport);
            btnDelete  = itemView.findViewById(R.id.btnDelete);
        }
    }
}
