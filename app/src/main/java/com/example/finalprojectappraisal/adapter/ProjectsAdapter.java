package com.example.finalprojectappraisal.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
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

import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;


import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.utils.MapIntentUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

// Glide - ודאי שיש תלות ב-gradle (ראו בסוף)
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectsAdapter extends RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder> {

    private static final String TAG_AD = "MyProjectsAdapter";

    public interface ProjectActionListener {
        void onEdit(Project project);
        void onAddNote(Project project);   // ברור יותר מ-onImages
        void onReport(Project project);
        void onDelete(Project project);
        void onAssignAppraiser(Project project);
        void onCompass(Project project);
    }

    private final Context context;
    private final ProjectActionListener listener;

    private final List<Project> projects = new ArrayList<>();
    private boolean isAdmin = false;
    private String currentUserId = null;

    // מטמון URL של תמונת חזית לכל פרויקט
    private static final LruCache<String, String> frontImageCache = new LruCache<>(100);

    public ProjectsAdapter(List<Project> initial, ProjectActionListener listener, Context context) {
        this(initial, listener, context, false, null);
    }

    public ProjectsAdapter(List<Project> initial, ProjectActionListener listener, Context context,
                           boolean isAdmin, String currentUserId) {
        this.listener = listener;
        this.context = context;
        this.isAdmin = isAdmin;
        this.currentUserId = currentUserId;
        setHasStableIds(true);
        if (initial != null) this.projects.addAll(initial);
        Log.d(TAG_AD, "ctor: initSize=" + this.projects.size());
    }

    public void updatePermissions(boolean isAdmin, String currentUserId) {
        this.isAdmin = isAdmin;
        this.currentUserId = currentUserId;
        Log.d(TAG_AD, "updatePermissions: isAdmin=" + isAdmin + " currentUserId=" + currentUserId);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG_AD, "onCreateViewHolder");
        View view = LayoutInflater.from(context).inflate(R.layout.item_project_card, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projects.get(position);
        String pid = (project != null ? project.getProjectId() : null);
        Log.d(TAG_AD, "onBindViewHolder pos=" + position + " pid=" + pid);

        // Placeholder ברירת מחדל לתמונה (עד שנטען)
        holder.imageThumb.setImageResource(android.R.drawable.ic_menu_gallery);

        // כתובת
        holder.txtAddress.setText(safeOrDash(project != null ? project.getFullAddress() : null));

        // הערה (הצג/הסתר לפי תוכן)
        String note = project != null ? project.getNote() : null;
        if (TextUtils.isEmpty(note)) {
            holder.txtNote.setVisibility(View.GONE);
        } else {
            holder.txtNote.setVisibility(View.VISIBLE);
            holder.txtNote.setText(note);
        }

        // כפתור מפה
        String address = (project != null) ? project.getFullAddress() : null;
        holder.btnMap.setEnabled(!TextUtils.isEmpty(address));
        holder.btnMap.setOnClickListener(v -> {
            Log.d(TAG_AD, "click: btnMap pid=" + pid + " addr=" + address);
            showMapOptionsDialog(address);
        });

        // <--- הוסף את הטיפול בכפתור המצפן כאן:
        holder.btnCompass.setOnClickListener(v -> {
            Log.d(TAG_AD, "click: btnCompass pid=" + pid);
            if (listener != null && project != null) {
                listener.onCompass(project);
            }
        });

        // סטטוס
        String status = project != null ? project.getProjectStatus() : null;
        applyStatusStyle(holder.txtStatus, status);

        // שם לקוח
        String clientName = (project != null && project.getClient() != null)
                ? project.getClient().getFullName()
                : "";
        holder.txtClient.setText(safeOrDash(clientName));

        // תאריך עדכון
        long lastUpdate = (project != null) ? project.getLastUpdateDateMillis() : 0L;
        holder.txtDate.setText(lastUpdate > 0 ? "עודכן: " + formatDate(lastUpdate) : "");

        // אינדיקציה למנהלים
        if (isAdmin && project != null && project.getAppraiserId() != null) {
            if (!project.getAppraiserId().equals(currentUserId)) {
                holder.txtClient.append(" (פרויקט של שמאי אחר)");
            }
        }

        // הרשאות לעריכה
        boolean canEdit = determineEditPermission(project);
        Log.d(TAG_AD, "onBindViewHolder pid=" + pid + " canEdit=" + canEdit + " isAdmin=" + isAdmin);

        // הקצה שמאי — רק למנהלים
        if (isAdmin && project != null) {
            holder.btnAssignAppraiser.setVisibility(View.VISIBLE);
            holder.btnAssignAppraiser.setOnClickListener(v -> {
                Log.d(TAG_AD, "click: assignAppraiser pid=" + pid);
                if (listener != null) listener.onAssignAppraiser(project);
            });
        } else {
            holder.btnAssignAppraiser.setVisibility(View.GONE);
        }

        // נראות כפתורי עריכה/מחיקה
        holder.btnEdit.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        holder.btnDelete.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        holder.btnChangeStatus.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        holder.btnMore.setVisibility(canEdit ? View.VISIBLE : View.GONE);

        // כפתורי צפייה/פעולות כלליות
        holder.btnAddNote.setVisibility(View.VISIBLE);
        holder.btnReport.setVisibility(View.VISIBLE);
        holder.btnMap.setVisibility(View.VISIBLE);
        holder.btnCompass.setVisibility(View.VISIBLE);

        if (canEdit) {
            holder.btnEdit.setOnClickListener(v -> {
                Log.d(TAG_AD, "click: edit menu pid=" + pid);
                showEditMenu(v, project);
            });
            holder.btnMore.setOnClickListener(v -> {
                Log.d(TAG_AD, "click: more menu pid=" + pid);
                showEditMenu(v, project);
            });
            holder.btnChangeStatus.setOnClickListener(v -> {
                Log.d(TAG_AD, "click: changeStatus pid=" + pid);
                if (project == null) return;
                showStatusDialog(project, holder);
            });
            holder.btnDelete.setOnClickListener(v -> {
                Log.d(TAG_AD, "click: delete pid=" + pid);
                if (listener != null && project != null) listener.onDelete(project);
            });
        }

        // הוסף הערה
        holder.btnAddNote.setOnClickListener(v -> {
            Log.d(TAG_AD, "click: addNote pid=" + pid);
            if (listener != null && project != null) listener.onAddNote(project);
        });

        // דו"ח
        holder.btnReport.setOnClickListener(v -> {
            Log.d(TAG_AD, "click: report pid=" + pid);
            if (listener != null && project != null) listener.onReport(project);
        });

        // טעינת תמונת חזית (front_image) אם קיימת
        loadFrontImage(project, holder.imageThumb);
    }

    /** דיאלוג בחירת אפליקציית מפה */
    private void showMapOptionsDialog(String address) {
        if (TextUtils.isEmpty(address)) {
            Toast.makeText(context, "כתובת לא זמינה לפתיחת מפה.", Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] options = new String[] { "גוגל מפות (ניווט)", "Waze (ניווט)", "Govmap (ממשלתי)" };
        new AlertDialog.Builder(context)
                .setTitle("בחר אפליקציית מפה")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) MapIntentUtils.openAddressInMaps(context, address);
                    else if (which == 1) MapIntentUtils.openAddressInWaze(context, address);
                    else MapIntentUtils.openAddressInGovmap(context, address);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    /** הרשאת עריכה למשתמש הנוכחי */
    private boolean determineEditPermission(Project project) {
        if (project == null || currentUserId == null) return false;
        if (isAdmin) return true;
        return project.isAppraiserAssigned(currentUserId);
    }

    @Override public int getItemCount() { return projects.size(); }

    @Override
    public long getItemId(int position) {
        Project p = projects.get(position);
        String id = p != null ? p.getProjectId() : null;
        return (id != null) ? id.hashCode() : RecyclerView.NO_ID;
    }

    /** עדכון נתונים עם DiffUtil */
    public void updateData(List<Project> newProjects) {
        List<Project> next = (newProjects != null) ? new ArrayList<>(newProjects) : new ArrayList<>();
        Log.d(TAG_AD, "updateData: old=" + this.projects.size() + " new=" + next.size());
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new ProjectDiff(this.projects, next), true);
        this.projects.clear();
        this.projects.addAll(next);
        diff.dispatchUpdatesTo(this);
    }

    /** שינוי סטטוס + שמירה ל-DB */
    private void showStatusDialog(Project project, ProjectViewHolder holder) {
        if (project == null || TextUtils.isEmpty(project.getProjectId())) return;

        final String[] statuses = context.getResources().getStringArray(R.array.project_statuses);
        if (statuses == null || statuses.length == 0) {
            Toast.makeText(context, "לא הוגדרו סטטוסים", Toast.LENGTH_SHORT).show();
            return;
        }

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
                    Log.d(TAG_AD, "saveStatus pid=" + project.getProjectId() + " → " + newStatus);

                    java.util.Map<String, Object> fields = new java.util.HashMap<>();
                    fields.put(FirestoreConstants.FIELD_PROJECT_STATUS, newStatus);
                    fields.put(FirestoreConstants.FIELD_LAST_UPDATE_DATE,
                            com.google.firebase.firestore.FieldValue.serverTimestamp());

                    ProjectRepository.getInstance()
                            .updateMultipleFields(project.getProjectId(), fields, task -> {
                                Log.d(TAG_AD, "saveStatus.onComplete ok=" + task.isSuccessful());
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

    /** תפריט עריכה */
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

            } else if (id == R.id.action_edit_apartment) {
                intent = new Intent(context,
                        com.example.finalprojectappraisal.activity.newProject.property.activity.ApartmentDetailsActivity.class);

            } else if (id == R.id.action_edit_property) {
                intent = new Intent(context,
                        com.example.finalprojectappraisal.activity.newProject.property.activity.PropertyDescriptionActivity.class);

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

    // ===== טעינת תמונת חזית =====

    /** טוען תמונת חזית (front_image) אם קיימת ב-Firestore; עם מטמון והגנות ל-Recycling */
    private void loadFrontImage(Project project, ImageView target) {
        if (project == null || TextUtils.isEmpty(project.getProjectId()) || target == null) {
            if (target != null) target.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }
        final String pid = project.getProjectId();

        // הגנה מפני Recycling: נסמן את ה-ImageView בפרויקט הנוכחי
        target.setTag(pid);

        // 1) אם כבר יש בקאש — נטען מיד
        String cachedUrl = frontImageCache.get(pid);
        if (!TextUtils.isEmpty(cachedUrl)) {
            if (pid.equals(target.getTag())) {
                Glide.with(context).load(cachedUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(target);
            }
            return;
        }

        // 2) נשלוף את המסמך images/main ונקרא את front_image
        FirebaseFirestore.getInstance()
                .collection(FirestoreConstants.COLLECTION_PROJECTS)
                .document(pid)
                .collection(FirestoreConstants.SUBCOLLECTION_IMAGES)
                .document(FirestoreConstants.IMAGES_DOC_MAIN)
                .get()
                .addOnSuccessListener(doc -> handleFrontImageDoc(doc, pid, target))
                .addOnFailureListener(e -> {
                    if (pid.equals(target.getTag())) {
                        target.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                });
    }

    /** טיפול במסמך images/main שהוחזר: המרה ל-URL וטעינה */
    private void handleFrontImageDoc(DocumentSnapshot doc, String pid, ImageView target) {
        if (doc == null || !doc.exists()) {
            if (pid.equals(target.getTag())) {
                target.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            return;
        }
        String pathOrUrl = doc.getString(FirestoreConstants.FIELD_FRONT_IMAGE);

        if (TextUtils.isEmpty(pathOrUrl)) {
            if (pid.equals(target.getTag())) {
                target.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            return;
        }

        // אם זה כבר URL (http/https) — נטען מייד
        if (pathOrUrl.startsWith("http")) {
            frontImageCache.put(pid, pathOrUrl);
            if (pid.equals(target.getTag())) {
                Glide.with(context).load(pathOrUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(target);
            }
            return;
        }

        // אם זה נתיב לאחסון (gs:// או נתיב יחסי) — נמיר ל-Download URL
        try {
            FirebaseStorage.getInstance().getReference(pathOrUrl)
                    .getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        String url = (uri != null) ? uri.toString() : null;
                        if (!TextUtils.isEmpty(url)) {
                            frontImageCache.put(pid, url);
                            if (pid.equals(target.getTag())) {
                                Glide.with(context).load(url)
                                        .placeholder(android.R.drawable.ic_menu_gallery)
                                        .into(target);
                            }
                        } else {
                            if (pid.equals(target.getTag())) {
                                target.setImageResource(android.R.drawable.ic_menu_gallery);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (pid.equals(target.getTag())) {
                            target.setImageResource(android.R.drawable.ic_menu_gallery);
                        }
                    });
        } catch (Exception ex) {
            if (pid.equals(target.getTag())) {
                target.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
    }

    // ===== Utils =====

    private String safeOrDash(String s) { return TextUtils.isEmpty(s) ? "—" : s; }

    private String formatDate(long epochMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("he", "IL"));
        return sdf.format(new Date(epochMillis));
    }

    private void applyStatusStyle(TextView tv, String statusRaw) {
        // ודאי שיש רקע (shape) אחד בסיסי
        if (tv.getBackground() == null) {
            tv.setBackgroundResource(R.drawable.bg_status_pill);
        }

        String s = (statusRaw == null) ? "" : statusRaw.trim();
        int bgRes, fgRes; String label = s;

        // עדכני את המחרוזות כך שיתאימו בדיוק למה שנשמר ב-DB
        switch (s) {
            case "הצעת מחיר":
                bgRes = R.color.status_quote_bg;       fgRes = R.color.status_quote_fg;       break;
            case "טרם ביקור":
                bgRes = R.color.status_pre_visit_bg;   fgRes = R.color.status_pre_visit_fg;   break;
            case "לאחר ביקור":
                bgRes = R.color.status_post_visit_bg;  fgRes = R.color.status_post_visit_fg;  break;
            case "בעבודה":
                bgRes = R.color.status_in_progress_bg; fgRes = R.color.status_in_progress_fg; break;
            case "בבדיקה שמאי חותם":
                bgRes = R.color.status_reviewer_bg;    fgRes = R.color.status_reviewer_fg;    break;
            case "הושלם":
                bgRes = R.color.status_done_bg;        fgRes = R.color.status_done_fg;        break;
            default:
                bgRes = R.color.status_quote_bg;       fgRes = R.color.status_quote_fg;       label = "סטטוס";
        }

        int bg = ContextCompat.getColor(tv.getContext(), bgRes);
        int fg = ContextCompat.getColor(tv.getContext(), fgRes);
        ViewCompat.setBackgroundTintList(tv, ColorStateList.valueOf(bg));
        tv.setTextColor(fg);
        tv.setText(label);
    }


    // ===== ViewHolder =====
    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb;
        TextView txtAddress, txtNote, txtStatus, txtClient, txtDate;
        Button btnEdit, btnAddNote, btnReport, btnDelete, btnChangeStatus, btnMap, btnAssignAppraiser, btnCompass;
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
            btnAddNote = itemView.findViewById(R.id.btnAddNote); // ודאי שה-XML מעודכן
            btnReport  = itemView.findViewById(R.id.btnReport);
            btnDelete  = itemView.findViewById(R.id.btnDelete);
            btnMore    = itemView.findViewById(R.id.btnMore);
            btnChangeStatus = itemView.findViewById(R.id.btnChangeStatus);
            btnMap     = itemView.findViewById(R.id.btnMap);
            btnAssignAppraiser = itemView.findViewById(R.id.btnAssignAppraiser);
            btnCompass = itemView.findViewById(R.id.btnCompass);
        }
    }

    // ===== DiffUtil Callback (אתחול בטוח) =====
    private static class ProjectDiff extends DiffUtil.Callback {
        private final List<Project> oldList = new ArrayList<>();
        private final List<Project> newList = new ArrayList<>();

        ProjectDiff(List<Project> oldList, List<Project> newList) {
            if (oldList != null) this.oldList.addAll(oldList);
            if (newList != null) this.newList.addAll(newList);
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

            if (!eq(o.getFullAddress(), n.getFullAddress())) return false;

            String oc = (o.getClient() != null) ? o.getClient().getFullName() : null;
            String nc = (n.getClient() != null) ? n.getClient().getFullName() : null;
            if (!eq(oc, nc)) return false;

            if (!eq(o.getProjectStatus(), n.getProjectStatus())) return false;
            if (o.getLastUpdateDateMillis() != n.getLastUpdateDateMillis()) return false;
            if (!eq(o.getNote(), n.getNote())) return false;

            List<String> ocos = o.getCoAppraiserIds();
            List<String> ncos = n.getCoAppraiserIds();
            if (ocos != null && ncos != null) {
                if (!ocos.equals(ncos)) return false;
            } else if (ocos != ncos) {
                return false;
            }

            return true;
        }

        private static boolean eq(String a, String b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            return a.equals(b);
        }
    }
}
