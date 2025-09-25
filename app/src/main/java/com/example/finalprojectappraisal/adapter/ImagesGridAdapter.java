package com.example.finalprojectappraisal.adapter;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.model.Image;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class ImagesGridAdapter extends RecyclerView.Adapter<ImagesGridAdapter.VH> {

    private final List<Image> data = new ArrayList<>();

    public void submit(List<Image> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_square, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Image it = data.get(pos);
        String url = (it != null) ? it.getUrl() : null;

        RequestManager rm = Glide.with(h.img.getContext());
        rm.load(url) // תומך http/https/content://
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_delete)
                .centerCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        android.util.Log.e("ImagesAdapter", "Glide failed for: " + model, e);
                        return false;
                    }
                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(h.img);
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ShapeableImageView img;
        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.image);
        }
    }
}
