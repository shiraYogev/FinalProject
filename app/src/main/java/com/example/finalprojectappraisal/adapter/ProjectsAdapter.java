package com.example.finalprojectappraisal.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.model.Project;
import java.util.List;

public class ProjectsAdapter extends RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder> {

    public interface ProjectActionListener {
        void onEdit(Project project);
        void onImages(Project project);
        void onReport(Project project);
        void onDelete(Project project);
    }

    private List<Project> projects;
    private ProjectActionListener listener;
    private Context context;

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
        holder.txtAddress.setText(project.getFullAddress());
        // סטטוס
        holder.txtStatus.setText(project.getProjectStatus() != null ? project.getProjectStatus() : "סטטוס לא ידוע");
        // שם לקוח
        holder.txtClient.setText(project.getClient() != null ? project.getClient().getFullName() : "");
        // תאריך עדכון
        holder.txtDate.setText(project.getLastUpdateDate() != 0 ? "עודכן: " + project.getLastUpdateDate() : "");

//        // תמונה ממוזערת (תמונת חזית אם קיימת, אחרת placeholder)
//        String imageUrl = (project.getFrontImageUrl() != null) ? project.getFrontImageUrl() : null;
//        Glide.with(context)
//                .load(imageUrl)
//                .placeholder(R.drawable.ic_placeholder)
//                .into(holder.imageThumb);

        // כפתורי פעולה
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(project));
        holder.btnImages.setOnClickListener(v -> listener.onImages(project));
        holder.btnReport.setOnClickListener(v -> listener.onReport(project));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(project));
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public void updateData(List<Project> newProjects) {
        this.projects = newProjects;
        notifyDataSetChanged();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb;
        TextView txtAddress, txtStatus, txtClient, txtDate;
        Button btnEdit, btnImages, btnReport, btnDelete;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
            txtAddress = itemView.findViewById(R.id.txtAddress);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtClient = itemView.findViewById(R.id.txtClient);
            txtDate = itemView.findViewById(R.id.txtDate);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnImages = itemView.findViewById(R.id.btnImages);
            btnReport = itemView.findViewById(R.id.btnReport);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
