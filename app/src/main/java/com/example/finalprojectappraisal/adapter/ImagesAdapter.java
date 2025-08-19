package com.example.finalprojectappraisal.adapter;

import android.util.Log;
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
import com.example.finalprojectappraisal.model.Image;
import java.util.List;

public class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.ImageViewHolder> {

    private final List<Image> images;

    public interface OnImageActionListener {
        void onApprove(Image image);
        void onEdit(Image image);
    }

    private OnImageActionListener listener;

    public void setOnImageActionListener(OnImageActionListener listener) {
        this.listener = listener;
    }

    public ImagesAdapter(List<Image> images) {
        this.images = images;
    }

    public void addImage(Image image) {
        images.add(image);
        notifyItemInserted(images.size() - 1);
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_thumbnail, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Image img = images.get(position);

        // טעינת התמונה
        Glide.with(holder.imageView.getContext())
                .load(img.getUrl())
                .placeholder(R.drawable.baseline_add_photo_alternate_24)
                .into(holder.imageView);

        // עדכון התיאור עם בדיקות נוספות
        String description = img.getDescription();
        if (description == null || description.isEmpty()) {
            description = "ממתין לסיווג...";
        }

        // לוג לבדיקה
        Log.d("ImagesAdapter", "מציג תיאור: " + description + " לפוזיציה: " + position);

        holder.txtClassification.setText(description);

        // כפתורים
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(img);
        });
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(img);
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView txtClassification;
        Button btnApprove, btnEdit;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewThumb);
            txtClassification = itemView.findViewById(R.id.txtClassification);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}
