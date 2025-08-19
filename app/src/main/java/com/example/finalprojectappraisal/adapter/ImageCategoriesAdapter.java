package com.example.finalprojectappraisal.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.classifer.ImageCategorySection;
import com.example.finalprojectappraisal.model.Image;

import java.util.List;

public class ImageCategoriesAdapter extends RecyclerView.Adapter<ImageCategoriesAdapter.CategoryViewHolder> {

    private final List<ImageCategorySection> categories;
    private final Context context;
    private final OnAddImageListener addImageListener;

    public interface OnAddImageListener {
        void onAddImage(ImageCategorySection section);
    }

    public ImageCategoriesAdapter(List<ImageCategorySection> categories, Context context, OnAddImageListener listener) {
        this.categories = categories;
        this.context = context;
        this.addImageListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_category_section, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        ImageCategorySection section = categories.get(position);
        holder.txtTitle.setText(section.title);

        // יצירת ה-adapter עם reference לעמדה הנוכחית
        final int currentPosition = position;  // שמירת העמדה הנוכחית
        ImagePagerAdapter pagerAdapter = new ImagePagerAdapter(
                section.images,
                new ImagePagerAdapter.OnImageActionListener() {
                    @Override
                    public void onDelete(int imagePosition) {
                        section.images.remove(imagePosition);
                        // עדכון ה-ViewPager
                        holder.viewPagerImages.getAdapter().notifyItemRemoved(imagePosition);
                        // עדכון הכותרת אם צריך (למשל מספר תמונות)
                        notifyItemChanged(currentPosition);
                    }

                    @Override
                    public void onDescriptionChanged(int imagePosition, String newText) {
                        if (imagePosition < section.images.size()) {
                            section.images.get(imagePosition).setDescription(newText);
                        }
                    }

                    @Override
                    public void onImageClick(int imagePosition) {
                        // הגדלת תמונה או פעולות אחרות
                    }
                }
        );

        holder.viewPagerImages.setAdapter(pagerAdapter);
        holder.btnAddImage.setOnClickListener(v -> addImageListener.onAddImage(section));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    /**
     * רענון של קטגוריה מסוימת (למשל אחרי הוספת תמונה)
     */
    public void notifyImageChanged(int categoryPosition) {
        notifyItemChanged(categoryPosition);
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle;
        ViewPager2 viewPagerImages;
        Button btnAddImage;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtCategoryTitle);
            viewPagerImages = itemView.findViewById(R.id.viewPagerImages);
            btnAddImage = itemView.findViewById(R.id.btnAddImage);
        }
    }
}