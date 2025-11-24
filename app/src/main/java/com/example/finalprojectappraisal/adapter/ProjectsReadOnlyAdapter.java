package com.example.finalprojectappraisal.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.model.Project;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProjectsReadOnlyAdapter extends RecyclerView.Adapter<ProjectsReadOnlyAdapter.Holder> {

    public interface ViewClickListener { void onView(Project p); }

    private final List<Project> data = new ArrayList<>();
    private final ViewClickListener listener;

    public ProjectsReadOnlyAdapter(List<Project> init, ViewClickListener listener) {
        if (init != null) data.addAll(init);
        this.listener = listener;
    }

    public void submit(List<Project> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_readonly, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        Context ctx = h.itemView.getContext();
        Project p = data.get(pos);

        // ===== כתובת =====
        h.txtTitle.setText(safe(p.getFullAddress(), "ללא כתובת"));

        // ===== סטטוס כ־pill (צבעוני) =====
        String statusRaw = safe(p.getProjectStatus(), "לא ידוע");
        h.txtStatus.setText(statusRaw);
        tintStatusPill(h.txtStatus, statusRaw);

        // ===== לקוח + עודכן =====
        String client = (p.getClient() != null && p.getClient().getFullName() != null)
                ? p.getClient().getFullName() : "לקוח לא ידוע";
        h.txtClient.setText("לקוח: " + client);

        Long tsMillis = null;
        try {
            if (p.getLastUpdateDate() != null) tsMillis = p.getLastUpdateDate().getTime();
        } catch (Exception ignore) {}
        String last = (tsMillis != null)
                ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(tsMillis))
                : "—";
        h.txtUpdated.setText("עודכן: " + last);

        // ===== תמונת חזית =====
        loadFrontImageForProject(p, h.imageThumb);

        // ===== לחיצה על "צפייה" =====
        h.btnView.setOnClickListener(v -> {
            if (listener != null) listener.onView(p);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    // ---------- Holder ----------
    static class Holder extends RecyclerView.ViewHolder {
        CardView card;
        TextView txtTitle, txtClient, txtStatus, txtUpdated;
        Button btnView;
        ImageView imageThumb;

        Holder(@NonNull View v) {
            super(v);
            card = v.findViewById(R.id.card);
            txtTitle = v.findViewById(R.id.txtTitle);
            txtClient = v.findViewById(R.id.txtClient);
            txtStatus = v.findViewById(R.id.txtStatus);
            txtUpdated = v.findViewById(R.id.txtUpdated);
            btnView = v.findViewById(R.id.btnView);
            imageThumb = v.findViewById(R.id.imageThumb);
        }
    }

    // =========================================================
    //              FRONT IMAGE LOADING PIPELINE
    // =========================================================

    /**
     * Loads a "front" image for a project into the given ImageView.
     * Pipeline:
     * 1) Try direct fields on Project (frontImageUrl, mainImageUrl, heroImageUrl, ...).
     * 2) Try images stored inside the Project object itself (getImages / getImagesList / images).
     * 3) Fallback to Firestore subcollection: /projects/{projectId}/images
     *    and choose a "front" image (category חזית / FRONT / EXTERIOR / FACADE / ENTRANCE),
     *    or first image if none is marked.
     */
    private void loadFrontImageForProject(Project p, ImageView iv) {
        // Always start from placeholder to avoid wrong images בגלל מחזוריות של RecyclerView
        setPlaceholder(iv);

        if (p == null) return;

        // ---- Step 1: direct URL / storagePath on Project model ----
        String directUrl = firstNonEmpty(
                callStringGetter(p, "getFrontImageUrl"),
                callStringGetter(p, "getMainImageUrl"),
                callStringGetter(p, "getHeroImageUrl")
        );
        String directStoragePath = firstNonEmpty(
                callStringGetter(p, "getFrontImageStoragePath"),
                callStringGetter(p, "getMainImageStoragePath"),
                callStringGetter(p, "getHeroImageStoragePath")
        );

        if (isNotEmpty(directUrl)) {
            loadUrlInto(iv.getContext(), directUrl, iv);
            return;
        } else if (isNotEmpty(directStoragePath)) {
            loadStorageInto(directStoragePath, iv);
            return;
        }

        // ---- Step 2: try images collection in Project object (if exists) ----
        FrontImageCandidate candFromObject = findFrontImageInProjectImagesCollection(p);
        if (candFromObject != null) {
            if (isNotEmpty(candFromObject.url)) {
                loadUrlInto(iv.getContext(), candFromObject.url, iv);
            } else if (isNotEmpty(candFromObject.storagePath)) {
                loadStorageInto(candFromObject.storagePath, iv);
            } else {
                setPlaceholder(iv);
            }
            return;
        }

        // ---- Step 3: Firestore subcollection /projects/{projectId}/images ----
        String projectId = p.getProjectId();
        if (!isNotEmpty(projectId)) {
            // no id → stay with placeholder
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("projects")
                .document(projectId)
                .collection("images")  // <== אם אצלך האוסף נקרא אחרת – לשנות כאן
                .limit(15)
                .get()
                .addOnSuccessListener(snap -> {
                    FrontImageCandidate candFs = pickFrontFromSnapshot(snap);
                    if (candFs != null) {
                        if (isNotEmpty(candFs.url)) {
                            loadUrlInto(iv.getContext(), candFs.url, iv);
                        } else if (isNotEmpty(candFs.storagePath)) {
                            loadStorageInto(candFs.storagePath, iv);
                        } else {
                            setPlaceholder(iv);
                        }
                    } else {
                        setPlaceholder(iv);
                    }
                })
                .addOnFailureListener(e -> {
                    // On error we just keep placeholder
                    setPlaceholder(iv);
                });
    }

    /**
     * מחפש בתוך results של Firestore תמונה בקטגוריה "חזית" / FRONT / FACADE / EXTERIOR / ENTRANCE.
     * אם אין – לוקח את התמונה הראשונה שיש לה url/path.
     */
    private FrontImageCandidate pickFrontFromSnapshot(QuerySnapshot snap) {
        if (snap == null || snap.isEmpty()) return null;

        // עדיפות: category "front"
        for (DocumentSnapshot doc : snap.getDocuments()) {
            String cat = doc.getString("category"); // <== אם אצלך השדה נקרא אחרת – לשנות כאן
            if (isFrontCategory(cat)) {
                String url = firstNonEmpty(
                        doc.getString("downloadUrl"), // <== לשנות אם השמות אחרים
                        doc.getString("url")
                );
                String storage = firstNonEmpty(
                        doc.getString("storagePath"),
                        doc.getString("path")
                );
                if (isNotEmpty(url) || isNotEmpty(storage)) {
                    return new FrontImageCandidate(url, storage);
                }
            }
        }

        // fallback: הראשונה עם url/path בכלל
        for (DocumentSnapshot doc : snap.getDocuments()) {
            String url = firstNonEmpty(
                    doc.getString("downloadUrl"),
                    doc.getString("url")
            );
            String storage = firstNonEmpty(
                    doc.getString("storagePath"),
                    doc.getString("path")
            );
            if (isNotEmpty(url) || isNotEmpty(storage)) {
                return new FrontImageCandidate(url, storage);
            }
        }

        return null;
    }

    // ---------- FRONT IMAGE FROM PROJECT OBJECT (reflection) ----------

    /**
     * מחפש בתוך אוסף התמונות של הפרויקט (אם יש שדה images בתוך המודל)
     * תמונה בקטגוריה "חזית"/FRONT/FACADE/EXTERIOR/ENTRANCE.
     * עובד גם אם getImages() מחזיר List<Image> וגם אם Map<String, Image>.
     */
    @SuppressWarnings("unchecked")
    private FrontImageCandidate findFrontImageInProjectImagesCollection(Project p) {
        if (p == null) return null;

        Object imagesObj = callAnyGetter(p, "getImages", "getImagesList", "images");
        List<Object> images = new ArrayList<>();

        try {
            if (imagesObj instanceof List) {
                images.addAll((List<Object>) imagesObj);
            } else if (imagesObj instanceof Map) {
                images.addAll(((Map<?, ?>) imagesObj).values());
            }
        } catch (Throwable ignore) {}

        if (images.isEmpty()) return null;

        // עדיפות: קטגוריה "חזית"
        for (Object img : images) {
            String cat = callStringGetter(img, "getCategory");
            if (isFrontCategory(cat)) {
                String url = firstNonEmpty(
                        callStringGetter(img, "getDownloadUrl"),
                        callStringGetter(img, "getUrl")
                );
                String storage = firstNonEmpty(
                        callStringGetter(img, "getStoragePath"),
                        callStringGetter(img, "getPath")
                );
                if (isNotEmpty(url) || isNotEmpty(storage)) {
                    return new FrontImageCandidate(url, storage);
                }
            }
        }

        // fallback: הראשונה עם url/path
        for (Object img : images) {
            String url = firstNonEmpty(
                    callStringGetter(img, "getDownloadUrl"),
                    callStringGetter(img, "getUrl")
            );
            String storage = firstNonEmpty(
                    callStringGetter(img, "getStoragePath"),
                    callStringGetter(img, "getPath")
            );
            if (isNotEmpty(url) || isNotEmpty(storage)) {
                return new FrontImageCandidate(url, storage);
            }
        }

        return null;
    }

    private boolean isFrontCategory(String catRaw) {
        if (!isNotEmpty(catRaw)) return false;
        String c = catRaw.toUpperCase(Locale.ROOT);
        // תומך גם באנגלית וגם בעברית "חזית"
        return c.contains("FRONT")
                || c.contains("FACADE")
                || c.contains("EXTERIOR")
                || c.contains("ENTRANCE")
                || c.contains("חזית");
    }

    // ---------- tiny reflection utils ----------
    private static String callStringGetter(Object obj, String method) {
        if (obj == null) return null;
        try {
            Method m = obj.getClass().getMethod(method);
            Object val = m.invoke(obj);
            return (val == null) ? null : String.valueOf(val);
        } catch (Throwable ignore) { return null; }
    }

    private static Object callAnyGetter(Object obj, String... methods) {
        if (obj == null) return null;
        for (String mName : methods) {
            try {
                Method m = obj.getClass().getMethod(mName);
                return m.invoke(obj);
            } catch (Throwable ignore) {}
        }
        return null;
    }

    // =========================================================
    //              STATUS PILL COLORING
    // =========================================================

    private void tintStatusPill(TextView v, String statusHe) {
        v.setBackgroundResource(R.drawable.bg_status_pill);
        int bg = Color.parseColor("#ECEFF1");
        int fg = Color.parseColor("#455A64");
        if (statusHe == null) statusHe = "";
        switch (statusHe.trim()) {
            case "הצעת מחיר":
                bg = Color.parseColor("#E3F2FD"); fg = Color.parseColor("#1565C0"); break;
            case "טרם ביקור":
                bg = Color.parseColor("#EDE7F6"); fg = Color.parseColor("#5E35B1"); break;
            case "לאחר ביקור":
                bg = Color.parseColor("#E1F5FE"); fg = Color.parseColor("#0277BD"); break;
            case "בעבודה":
                bg = Color.parseColor("#FFF3E0"); fg = Color.parseColor("#E65100"); break;
            case "בבדיקה שמאי חותם":
                bg = Color.parseColor("#FFF8E1"); fg = Color.parseColor("#FF8F00"); break;
            case "הושלם":
                bg = Color.parseColor("#E8F5E9"); fg = Color.parseColor("#2E7D32"); break;
        }
        v.setBackgroundTintList(ColorStateList.valueOf(bg));
        v.setTextColor(fg);
    }

    // =========================================================
    //              SIMPLE HELPERS
    // =========================================================

    private static String safe(String s, String def) {
        return (s == null || s.trim().isEmpty()) ? def : s;
    }

    private static boolean isNotEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String firstNonEmpty(String... arr) {
        if (arr == null) return null;
        for (String s : arr) if (isNotEmpty(s)) return s;
        return null;
    }

    // ---------- Image loading ----------
    private void setPlaceholder(ImageView iv) {
        iv.setImageResource(R.drawable.item_image_thumbnail);
    }

    private void loadUrlInto(Context ctx, String url, ImageView iv) {
        Glide.with(ctx)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.item_image_thumbnail)
                .error(R.drawable.item_image_thumbnail)
                .into(iv);
    }

    private void loadStorageInto(String storagePathOrGs, ImageView iv) {
        try {
            StorageReference ref;
            if (storagePathOrGs.startsWith("gs://")) {
                ref = FirebaseStorage.getInstance().getReferenceFromUrl(storagePathOrGs);
            } else {
                ref = FirebaseStorage.getInstance().getReference().child(storagePathOrGs);
            }
            ref.getDownloadUrl().addOnSuccessListener(uri ->
                    Glide.with(iv.getContext())
                            .load(uri)
                            .centerCrop()
                            .placeholder(R.drawable.item_image_thumbnail)
                            .error(R.drawable.item_image_thumbnail)
                            .into(iv)
            ).addOnFailureListener(e -> setPlaceholder(iv));
        } catch (Exception e) {
            setPlaceholder(iv);
        }
    }

    // ---------- inner DTO ----------
    private static class FrontImageCandidate {
        String url;
        String storagePath;
        FrontImageCandidate(String url, String storagePath) {
            this.url = url;
            this.storagePath = storagePath;
        }
    }
}
