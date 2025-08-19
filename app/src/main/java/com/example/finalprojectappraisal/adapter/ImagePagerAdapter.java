package com.example.finalprojectappraisal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.model.Image;

import java.util.List;

public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.PagerViewHolder> {
    public interface OnImageActionListener {
        void onDelete(int position);
        void onDescriptionChanged(int position, String newText);
        void onImageClick(int position);
    }

    private final List<Image> images;
    private final OnImageActionListener listener;

    public ImagePagerAdapter(List<Image> images, OnImageActionListener listener) {
        this.images = images;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_in_pager, parent, false);
        return new PagerViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PagerViewHolder holder, int position) {
        Image img = images.get(position);

        Glide.with(holder.imageView.getContext())
                .load(img.getUrl())
                .placeholder(R.drawable.baseline_add_photo_alternate_24)
                .into(holder.imageView);

        holder.editDescription.setText(img.getDescription());

        holder.editDescription.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && listener != null)
                listener.onDescriptionChanged(position, holder.editDescription.getText().toString());
        });

        holder.btnDeleteImage.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(position);
        });

        holder.imageView.setOnClickListener(v -> {
            if (listener != null) listener.onImageClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class PagerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton btnDeleteImage;
        EditText editDescription;

        PagerViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            btnDeleteImage = itemView.findViewById(R.id.btnDeleteImage);
            editDescription = itemView.findViewById(R.id.editDescription);
        }
    }
}
