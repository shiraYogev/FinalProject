package com.example.finalprojectappraisal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.classifer.ImageCategorySection;
import com.example.finalprojectappraisal.model.Floor;
import com.example.finalprojectappraisal.model.Image;
import com.example.finalprojectappraisal.model.Unit;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * קרוסלה אופקית של mini-cards לקטגוריות, עבור קומה מסוימת.
 * התמונות בכל כרטיס מסוננות לפי הקומה (floor). floor == null => חוץ (כל התמונות).
 */
public class MiniCardAdapter extends RecyclerView.Adapter<MiniCardAdapter.VH> {

    public interface OnMiniCardClick {
        void onClick(@NonNull ImageCategorySection section, @Nullable Unit unit, @Nullable Floor floor);
    }

    private final List<ImageCategorySection> sections;
    private final Unit unit;   // nullable (exterior)
    private final Floor floor; // nullable (exterior)
    private final OnMiniCardClick listener;

    public MiniCardAdapter(@NonNull List<ImageCategorySection> sections,
                           @Nullable Unit unit,
                           @Nullable Floor floor,
                           @NonNull OnMiniCardClick listener) {
        this.sections = sections;
        this.unit = unit;
        this.floor = floor;
        this.listener = listener;
    }

    /**
     * מחזיר את התמונות של section לפי יחידה + קומה.
     * unit == null ו-floor == null => חוץ (כל התמונות).
     */
    public static List<Image> imagesFor(@NonNull ImageCategorySection section,
                                        @Nullable Unit unit,
                                        @Nullable Floor floor) {
        if (unit == null && floor == null) return section.images;
        String unitKey = (unit != null) ? unit.key() : null;
        String floorKey = (floor != null) ? floor.key() : null;
        List<Image> result = new ArrayList<>();
        for (Image img : section.images) {
            if (img == null) continue;
            if (unitKey != null && !unitKey.equals(img.getUnitId())) continue;
            if (floorKey != null && !floorKey.equals(img.getFloor())) continue;
            result.add(img);
        }
        return result;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mini_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ImageCategorySection section = sections.get(position);
        h.label.setText(section.title);

        List<Image> imgs = imagesFor(section, unit, floor);
        boolean hasImages = !imgs.isEmpty();

        int strokeColor = ContextCompat.getColor(
                h.itemView.getContext(),
                hasImages ? R.color.brand_accent : R.color.fp_outline);
        h.card.setStrokeColor(strokeColor);

        if (hasImages) {
            h.count.setVisibility(View.VISIBLE);
            h.count.setText(String.valueOf(imgs.size()));

            Image first = imgs.get(0);
            String toLoad = (first.getUrl() != null && !first.getUrl().trim().isEmpty())
                    ? first.getUrl() : first.getLocalUri();

            h.thumb.setPadding(0, 0, 0, 0);
            h.thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(h.thumb.getContext())
                    .load(toLoad)
                    .placeholder(R.drawable.baseline_add_photo_alternate_24)
                    .into(h.thumb);
        } else {
            h.count.setVisibility(View.GONE);
            int pad = (int) (22 * h.itemView.getResources().getDisplayMetrics().density);
            h.thumb.setPadding(pad, pad, pad, pad);
            h.thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Glide.with(h.thumb.getContext()).clear(h.thumb);
            h.thumb.setImageResource(R.drawable.baseline_add_photo_alternate_24);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(section, unit, floor));
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView thumb;
        TextView count;
        TextView label;

        VH(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.miniCardRoot);
            thumb = itemView.findViewById(R.id.miniThumb);
            count = itemView.findViewById(R.id.miniCount);
            label = itemView.findViewById(R.id.miniLabel);
        }
    }
}
