package com.example.finalprojectappraisal.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

// import com.bumptech.glide.Glide; // השאירי אם תרצי טעינת תמונות ממוזערות
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.google.firebase.firestore.FieldValue;

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

        // סטטוס (מציגים את מה ששמור בפרויקט)
        String status = project != null ? project.getProjectStatus() : null;
        holder.txtStatus.setText(!TextUtils.isEmpty(status) ? status : "סטטוס לא ידוע");

        // שם לקוח
        String clientName = (project != null && project.getClient() != null)
                ? project.getClient().getFullName()
                : "";
        holder.txtClient.setText(safeOrDash(clientName));

        // תאריך עדכון ("dd/MM/yyyy")
        long lastUpdate = (project != null) ? project.getLastUpdateDateMillis() : 0L;
        holder.txtDate.setText(lastUpdate > 0 ? "עודכן: " + formatDate(lastUpdate) : "");

        // תמונה ממוזערת (לא חובה)
        // String imageUrl = project != null ? project.getFrontImageUrl() : null;
        // Glide.with(context)
        //        .load(imageUrl)
        //        .placeholder(R.drawable.ic_placeholder)
        //        .into(holder.imageThumb);

        // כפתורי פעולה
        holder.btnEdit.setOnClickListener(v -> showEditMenu(v, project));
        holder.btnMore.setOnClickListener(v -> showEditMenu(v, project));

        holder.btnImages.setOnClickListener(v -> {
            if (listener != null && project != null) listener.onImages(project);
        });
        holder.btnReport.setOnClickListener(v -> {
            if (listener != null && project != null) listener.onReport(project);
        });

        holder.btnChangeStatus.setOnClickListener(v -> {
            if (project == null) return;
            showStatusDialog(project, holder);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null && project != null) listener.onDelete(project);
        });
    }

    @Override
    public int getItemCount() {
        return projects == null ? 0 : projects.size();
    }

    // ===== שינוי סטטוס עם שמירה ל-DB בשדה projectStatus =====
    private void showStatusDialog(Project project, ProjectViewHolder holder) {
        if (project == null || project.getProjectId() == null || project.getProjectId().trim().isEmpty()) return;

        final String[] statuses = context.getResources().getStringArray(R.array.project_statuses);
        if (statuses == null || statuses.length == 0) {
            Toast.makeText(context, "לא הוגדרו סטטוסים", Toast.LENGTH_SHORT).show();
            return;
        }

        // סימון ברירת מחדל לפי ה-value הנוכחי בשדה (משווה טקסט כפי ששמור ב-DB)
        String current = project.getProjectStatus() == null ? "" : project.getProjectStatus();
        int checked = -1;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equalsIgnoreCase(current)) { checked = i; break; }
        }
        final int[] selected = { Math.max(checked, 0) };

        new AlertDialog.Builder(context)
                .setTitle("בחרי סטטוס לפרויקט")
                .setSingleChoiceItems(statuses, checked, (dialog, which) -> selected[0] = which)
                .setNegativeButton("ביטול", null)
                .setPositiveButton("שמירה", (dialog, which) -> {
                    String newStatus = statuses[selected[0]]; // הערך מה-arrays.xml

                    // נשמור ל-DB: projectStatus + lastUpdateDate מהשרת
                    java.util.Map<String, Object> fields = new java.util.HashMap<>();
                    fields.put(com.example.finalprojectappraisal.database.constants.FirestoreConstants.FIELD_PROJECT_STATUS, newStatus);
                    fields.put("lastUpdateDate", com.google.firebase.firestore.FieldValue.serverTimestamp());

                    com.example.finalprojectappraisal.database.ProjectRepository.getInstance()
                            .updateMultipleFields(project.getProjectId(), fields, task -> {
                                if (task.isSuccessful()) {
                                    // עדכון UI מידי; המאזין החי יעדכן גם
                                    project.setProjectStatus(newStatus);
                                    holder.txtStatus.setText(newStatus);
                                    android.widget.Toast.makeText(context, "סטטוס עודכן ל־" + newStatus, android.widget.Toast.LENGTH_SHORT).show();
                                } else {
                                    android.widget.Toast.makeText(context, "עדכון סטטוס נכשל", android.widget.Toast.LENGTH_SHORT).show();
                                }
                            });
                })

                .show();
    }

    // ===== תפריט עריכה לכל פרויקט =====
    private void showEditMenu(View anchor, Project project) {
        if (project == null) return;

        PopupMenu menu = new PopupMenu(context, anchor);
        menu.inflate(R.menu.menu_project_edit);

        menu.setOnMenuItemClickListener(item -> {
            Intent intent = null;
            String projectId = project.getProjectId();

            int id = item.getItemId();
            if (id == R.id.action_edit_client) {
                intent = new Intent(context,
                        com.example.finalprojectappraisal.activity.newProject.client.ClientDetailsActivity.class);
                intent.putExtra("projectId", project.getProjectId());
                context.startActivity(intent);
                return true;

            } else if (id == R.id.action_edit_apartment) {
                intent = new Intent(context,
                        com.example.finalprojectappraisal.activity.newProject.property.activity.ApartmentDetailsActivity.class);

            } else if (id == R.id.action_edit_property) {
                intent = new Intent(context,
                        com.example.finalprojectappraisal.activity.newProject.property.activity.PropertyDescriptionActivity.class);

            } else if (id == R.id.action_edit_bank) {
                intent = new Intent(context,
                        com.example.finalprojectappraisal.activity.newProject.bank.BankDetailsActivity.class);

            } else if (id == R.id.action_edit_images) {
                intent = new Intent(context,
                        com.example.finalprojectappraisal.activity.newProject.images.UploadImagesActivity.class);
            }

            if (intent != null) {
                intent.putExtra("projectId", projectId);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
            return true;
        });

        menu.show();
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
        Button btnEdit, btnImages, btnReport, btnDelete, btnChangeStatus;
        View btnMore; // כפתור ⋮

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
            txtAddress = itemView.findViewById(R.id.txtAddress);
            txtNote    = itemView.findViewById(R.id.txtNote);
            txtStatus  = itemView.findViewById(R.id.txtStatus);
            txtClient  = itemView.findViewById(R.id.txtClient);
            txtDate    = itemView.findViewById(R.id.txtDate);
            btnEdit    = itemView.findViewById(R.id.btnEdit);
            btnImages  = itemView.findViewById(R.id.btnImages);
            btnReport  = itemView.findViewById(R.id.btnReport);
            btnDelete  = itemView.findViewById(R.id.btnDelete);
            btnMore    = itemView.findViewById(R.id.btnMore);
            btnChangeStatus = itemView.findViewById(R.id.btnChangeStatus);
        }
    }
}
