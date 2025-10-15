package com.example.finalprojectappraisal.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.utils.MapIntentUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import java.util.stream.Collectors; // נצטרך את זה אם נציג את השמאים השותפים
// import com.bumptech.glide.Glide; // אם תרצי טעינת תמונות ממוזערות

public class ProjectsAdapter extends RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder> {

    public interface ProjectActionListener {
        void onEdit(Project project);
        void onImages(Project project);
        void onReport(Project project);
        void onDelete(Project project);
        void onAssignAppraiser(Project project);

    }

    private final Context context;
    private final ProjectActionListener listener;

    // מחזיקים עותק פרטי כדי למנוע שינויים חיצוניים ברשימה
    private final List<Project> projects = new ArrayList<>();

    // הוספה חדשה: הרשאות משתמש
    private boolean isAdmin = false;
    private String currentUserId = null;

    // Constructor המקורי (נשאר לתאימות לאחור)
    public ProjectsAdapter(List<Project> initial, ProjectActionListener listener, Context context) {
        this(initial, listener, context, false, null);
    }

    // Constructor חדש עם הרשאות
    public ProjectsAdapter(List<Project> initial, ProjectActionListener listener, Context context,
                           boolean isAdmin, String currentUserId) {
        this.listener = listener;
        this.context = context;
        this.isAdmin = isAdmin;
        this.currentUserId = currentUserId;
        setHasStableIds(true); // מאפשר אנימציות טובות וסקרול חלק
        if (initial != null) {
            this.projects.addAll(initial);
        }
    }

    // מתודה לעדכון הרשאות (כדי לא ליצור adapter חדש בכל עדכון)
    public void updatePermissions(boolean isAdmin, String currentUserId) {
        this.isAdmin = isAdmin;
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
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

        // הערה (הסתרה אם ריק)
        String note = project != null ? project.getNote() : null;
        if (TextUtils.isEmpty(note)) {
            holder.txtNote.setVisibility(View.GONE);
        } else {
            holder.txtNote.setVisibility(View.VISIBLE);
            holder.txtNote.setText(note);
        }

        // 🗺️ כפתור מפה
        // 🗺️ כפתור מפה - עכשיו עם דיאלוג בחירה
        String address = (project != null) ? project.getFullAddress() : null;
        holder.btnMap.setEnabled(!TextUtils.isEmpty(address));
        // במקום קריאה ישירה, נפנה למתודה חדשה
        holder.btnMap.setOnClickListener(v -> showMapOptionsDialog(address));

        // סטטוס
        String status = project != null ? project.getProjectStatus() : null;
        holder.txtStatus.setText(!TextUtils.isEmpty(status) ? status : "סטטוס לא ידוע");

        // שם לקוח
        String clientName = (project != null && project.getClient() != null)
                ? project.getClient().getFullName()
                : "";
        holder.txtClient.setText(safeOrDash(clientName));

        // תאריך עדכון
        long lastUpdate = (project != null) ? project.getLastUpdateDateMillis() : 0L;
        holder.txtDate.setText(lastUpdate > 0 ? "עודכן: " + formatDate(lastUpdate) : "");

        // **הוספה חדשה: הצגת מי הוא בעל הפרויקט (למנהלים)**
        if (isAdmin && project != null && project.getAppraiserId() != null) {
            // אפשר להוסיף אינדיקטור או TextView נוסף
            // לדוגמה, שינוי צבע רקע או הוספת טקסט
            if (!project.getAppraiserId().equals(currentUserId)) {
                // זה פרויקט של מישהו אחר - אפשר להוסיף סימון
                holder.txtClient.append(" (פרויקט של שמאי אחר)");
            }
        }

        // **הוספה חדשה: קביעת הרשאות לכפתורים**
        boolean canEdit = determineEditPermission(project);

        // 🆕 כפתור "הקצה שמאי" גלוי רק למנהלים
        if (isAdmin && project != null) {
            holder.btnAssignAppraiser.setVisibility(View.VISIBLE);
            holder.btnAssignAppraiser.setOnClickListener(v -> {
                if (listener != null) listener.onAssignAppraiser(project);
            });
        } else {
            holder.btnAssignAppraiser.setVisibility(View.GONE);
        }


        // כפתורי עריכה ומחיקה - רק למי שמורשה
        holder.btnEdit.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        holder.btnDelete.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        holder.btnChangeStatus.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        holder.btnMore.setVisibility(canEdit ? View.VISIBLE : View.GONE);

        // כפתורי צפייה - גלויים לכולם
        holder.btnImages.setVisibility(View.VISIBLE);
        holder.btnReport.setVisibility(View.VISIBLE);
        holder.btnMap.setVisibility(View.VISIBLE);

        // כפתורי פעולה (רק אם יש הרשאה)
        if (canEdit) {
            holder.btnEdit.setOnClickListener(v -> showEditMenu(v, project));
            holder.btnMore.setOnClickListener(v -> showEditMenu(v, project));

            holder.btnChangeStatus.setOnClickListener(v -> {
                if (project == null) return;
                showStatusDialog(project, holder);
            });

            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null && project != null) listener.onDelete(project);
            });
        }

        // כפתורי צפייה (תמיד פעילים)
        holder.btnImages.setOnClickListener(v -> {
            if (listener != null && project != null) listener.onImages(project);
        });
        holder.btnReport.setOnClickListener(v -> {
            if (listener != null && project != null) listener.onReport(project);
        });
    }

    /**
     * מציג דיאלוג לבחירת אפליקציית מפה (Google Maps, Waze, Govmap)
     * וקורא למתודה המתאימה ב-MapIntentUtils
     */
    private void showMapOptionsDialog(String address) {
        if (TextUtils.isEmpty(address)) {
            Toast.makeText(context, "כתובת לא זמינה לפתיחת מפה.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] options = new String[] {
                "גוגל מפות (ניווט)",
                "Waze (ניווט)",
                "Govmap (מפה ממשלתית)"
        };

        new AlertDialog.Builder(context)
                .setTitle("בחר אפליקציית מפה")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // Google Maps - שימוש במתודה הקיימת
                            MapIntentUtils.openAddressInMaps(context, address);
                            break;
                        case 1:
                            // Waze - יש לוודא שהמתודה MapIntentUtils.openAddressInWaze קיימת
                            MapIntentUtils.openAddressInWaze(context, address);
                            break;
                        case 2:
                            // Govmap - יש לוודא שהמתודה MapIntentUtils.openAddressInGovmap קיימת
                            MapIntentUtils.openAddressInGovmap(context, address);
                            break;
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    /**
     * קובע האם למשתמש הנוכחי יש הרשאה לערוך את הפרויקט
     */
    private boolean determineEditPermission(Project project) {
        if (project == null || currentUserId == null) {
            return false;
        }

        if (isAdmin) {
            // מנהלים יכולים לערוך את כל הפרויקטים
            return true;
        } else {
            // משתמש רגיל יכול לערוך אם הוא השמאי הראשי או שמאי שותף
            return project.isAppraiserAssigned(currentUserId);
        }
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    // === Stable IDs כדי לעזור ל-RecyclerView לזהות פריטים ===
    @Override
    public long getItemId(int position) {
        Project p = projects.get(position);
        String id = p != null ? p.getProjectId() : null;
        return (id != null) ? id.hashCode() : RecyclerView.NO_ID;
    }

    // === עדכון רשימה עם DiffUtil (במקום notifyDataSetChanged) ===
    public void updateData(List<Project> newProjects) {
        List<Project> next = (newProjects != null) ? new ArrayList<>(newProjects) : new ArrayList<>();
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new ProjectDiff(this.projects, next), true);
        this.projects.clear();
        this.projects.addAll(next);
        diff.dispatchUpdatesTo(this);
    }

    // ===== שינוי סטטוס עם שמירה ל-DB בשדה projectStatus =====
    private void showStatusDialog(Project project, ProjectViewHolder holder) {
        if (project == null || TextUtils.isEmpty(project.getProjectId())) return;

        final String[] statuses = context.getResources().getStringArray(R.array.project_statuses);
        if (statuses == null || statuses.length == 0) {
            Toast.makeText(context, "לא הוגדרו סטטוסים", Toast.LENGTH_SHORT).show();
            return;
        }

        // סימון ברירת מחדל לפי הערך הנוכחי
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
                    String newStatus = statuses[selected[0]];

                    // נשמור ל-DB: projectStatus + lastUpdateDate מהשרת
                    java.util.Map<String, Object> fields = new java.util.HashMap<>();
                    fields.put(FirestoreConstants.FIELD_PROJECT_STATUS, newStatus);
                    fields.put("lastUpdateDate", com.google.firebase.firestore.FieldValue.serverTimestamp());

                    ProjectRepository.getInstance()
                            .updateMultipleFields(project.getProjectId(), fields, task -> {
                                if (task.isSuccessful()) {
                                    project.setProjectStatus(newStatus);
                                    holder.txtStatus.setText(newStatus);
                                    Toast.makeText(context, "סטטוס עודכן ל־" + newStatus, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, "עדכון סטטוס נכשל", Toast.LENGTH_SHORT).show();
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
                intent.putExtra("projectId", projectId);
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

    // ===== Utils =====

    private String safeOrDash(String s) {
        return TextUtils.isEmpty(s) ? "—" : s;
    }

    private String formatDate(long epochMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("he", "IL"));
        return sdf.format(new Date(epochMillis));
    }

    // ===== ViewHolder =====
    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb;
        TextView txtAddress, txtNote, txtStatus, txtClient, txtDate;
        Button btnEdit, btnImages, btnReport, btnDelete, btnChangeStatus, btnMap, btnAssignAppraiser;
        View btnMore;

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
            btnMap     = itemView.findViewById(R.id.btnMap);
            btnAssignAppraiser = itemView.findViewById(R.id.btnAssignAppraiser); // ** 🆕 אתחול הכפתור החדש **
            // txtAppraisers = itemView.findViewById(R.id.txtAppraisers); // אם הוספת אותו
        }
    }

    // ===== DiffUtil Callback =====
    /** מומלץ להשאיר כ-inner class כאן בתוך האדפטר. אם תרצי – אפשר להוציא לקובץ נפרד. */
    private static class ProjectDiff extends DiffUtil.Callback {
        private final List<Project> oldList;
        private final List<Project> newList;

        ProjectDiff(List<Project> oldList, List<Project> newList) {
            this.oldList = (oldList != null) ? oldList : new ArrayList<>();
            this.newList = (newList != null) ? newList : new ArrayList<>();
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            String a = oldList.get(oldItemPosition).getProjectId();
            String b = newList.get(newItemPosition).getProjectId();
            return a != null && a.equals(b);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Project o = oldList.get(oldItemPosition);
            Project n = newList.get(newItemPosition);

            // השוואה לפי שדות שמוצגים בכרטיס
            if (!eq(o.getFullAddress(), n.getFullAddress())) return false;

            String oc = (o.getClient() != null) ? o.getClient().getFullName() : null;
            String nc = (n.getClient() != null) ? n.getClient().getFullName() : null;
            if (!eq(oc, nc)) return false;

            if (!eq(o.getProjectStatus(), n.getProjectStatus())) return false;

            if (o.getLastUpdateDateMillis() != n.getLastUpdateDateMillis()) return false;

            if (!eq(o.getNote(), n.getNote())) return false;

            // ** 🆕 הוסף השוואה לשמאים שותפים **
            if (!o.getCoAppraiserIds().equals(n.getCoAppraiserIds())) return false;


            // אם יש עוד שדות שמופיעים ב-ViewHolder – אפשר להוסיף כאן
            return true;
        }

        private static boolean eq(String a, String b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            return a.equals(b); // אם תרצי ללא רישיות: a.equalsIgnoreCase(b)
        }
    }
}