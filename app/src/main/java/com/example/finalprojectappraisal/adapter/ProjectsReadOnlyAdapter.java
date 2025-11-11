package com.example.finalprojectappraisal.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.model.Project;
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

    @NonNull @Override
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
        h.txtStatus.setText(statusRaw);               // הטקסט על ה-pill הוא שם הסטטוס עצמו
        tintStatusPill(h.txtStatus, statusRaw);       // צביעה (כמו ב"שלי")

        // ===== לקוח + עודכן =====
        String client = (p.getClient() != null && p.getClient().getFullName() != null)
                ? p.getClient().getFullName() : "לקוח לא ידוע";
        h.txtClient.setText("לקוח: " + client);

        Long tsMillis = null;
        try { if (p.getLastUpdateDate() != null) tsMillis = p.getLastUpdateDate().getTime(); } catch (Exception ignore) {}
        String last = (tsMillis != null)
                ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(tsMillis))
                : "—";
        h.txtUpdated.setText("עודכן: " + last);

        // ===== תמונת חזית (DB) =====
        // 1) שדות ישירים במודל אם קיימים: getFrontImageUrl / getMainImageUrl / getHeroImageUrl / getFrontImageStoragePath
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

        // 2) אם אין – נחלץ מתמונות הפרויקט לפי קטגוריה "חזית/FRONT/FACADE/EXTERIOR/ENTRANCE"
        if (isNotEmpty(directUrl)) {
            loadUrlInto(ctx, directUrl, h.imageThumb);
        } else if (isNotEmpty(directStoragePath)) {
            loadStorageInto(directStoragePath, h.imageThumb);
        } else {
            FrontImageCandidate cand = findFrontImageInImagesCollection(p);
            if (cand != null) {
                if (isNotEmpty(cand.url)) loadUrlInto(ctx, cand.url, h.imageThumb);
                else if (isNotEmpty(cand.storagePath)) loadStorageInto(cand.storagePath, h.imageThumb);
                else setPlaceholder(h.imageThumb);
            } else {
                setPlaceholder(h.imageThumb);
            }
        }

        // ===== לחיצה =====
        h.btnView.setOnClickListener(v -> { if (listener != null) listener.onView(p); });
    }

    @Override public int getItemCount() { return data.size(); }

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

    // ---------- Helpers: Status pill coloring ----------
    private void tintStatusPill(TextView v, String statusHe) {
        v.setBackgroundResource(R.drawable.bg_status_pill);
        int bg = Color.parseColor("#ECEFF1");
        int fg = Color.parseColor("#455A64");
        if (statusHe == null) statusHe = "";
        switch (statusHe.trim()) {
            case "הצעת מחיר":        bg = Color.parseColor("#E3F2FD"); fg = Color.parseColor("#1565C0"); break;
            case "טרם ביקור":        bg = Color.parseColor("#EDE7F6"); fg = Color.parseColor("#5E35B1"); break;
            case "לאחר ביקור":       bg = Color.parseColor("#E1F5FE"); fg = Color.parseColor("#0277BD"); break;
            case "בעבודה":           bg = Color.parseColor("#FFF3E0"); fg = Color.parseColor("#E65100"); break;
            case "בבדיקה שמאי חותם": bg = Color.parseColor("#FFF8E1"); fg = Color.parseColor("#FF8F00"); break;
            case "הושלם":            bg = Color.parseColor("#E8F5E9"); fg = Color.parseColor("#2E7D32"); break;
        }
        v.setBackgroundTintList(ColorStateList.valueOf(bg));
        v.setTextColor(fg);
    }

    private static String safe(String s, String def) { return (s == null || s.trim().isEmpty()) ? def : s; }
    private static boolean isNotEmpty(String s) { return s != null && !s.trim().isEmpty(); }
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

    // ---------- Front image discovery ----------
    private static class FrontImageCandidate {
        String url; String storagePath;
        FrontImageCandidate(String url, String storagePath) { this.url = url; this.storagePath = storagePath; }
    }

    /**
     * מחפש בתוך אוסף התמונות של הפרויקט תמונה בקטגוריה "חזית"/FRONT/FACADE/EXTERIOR/ENTRANCE.
     * עובד גם אם getImages() הוא List<Image> וגם אם הוא Map<String, Image>.
     * משתמש ברפלקציה כדי לא להיות תלוי בחתימות מדויקות של Image.
     */
    @SuppressWarnings("unchecked")
    private FrontImageCandidate findFrontImageInImagesCollection(Project p) {
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

        // עדיפות: קטגוריה "חזית"
        for (Object img : images) {
            if (isFrontCategory(callStringGetter(img, "getCategory"))) {
                String url = firstNonEmpty(
                        callStringGetter(img, "getDownloadUrl"),
                        callStringGetter(img, "getUrl")
                );
                String storage = firstNonEmpty(
                        callStringGetter(img, "getStoragePath"),
                        callStringGetter(img, "getPath")
                );
                if (isNotEmpty(url) || isNotEmpty(storage)) return new FrontImageCandidate(url, storage);
            }
        }

        // fallback: הראשונה עם URL/Storage בכלל
        for (Object img : images) {
            String url = firstNonEmpty(
                    callStringGetter(img, "getDownloadUrl"),
                    callStringGetter(img, "getUrl")
            );
            String storage = firstNonEmpty(
                    callStringGetter(img, "getStoragePath"),
                    callStringGetter(img, "getPath")
            );
            if (isNotEmpty(url) || isNotEmpty(storage)) return new FrontImageCandidate(url, storage);
        }

        return null;
    }

    private boolean isFrontCategory(String catRaw) {
        if (!isNotEmpty(catRaw)) return false;
        String c = catRaw.toUpperCase(Locale.ROOT);
        // תומך גם באנגלית וגם בעברית "חזית"
        return c.contains("FRONT") || c.contains("FACADE") || c.contains("EXTERIOR") || c.contains("ENTRANCE") || c.contains("חזית");
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
}
