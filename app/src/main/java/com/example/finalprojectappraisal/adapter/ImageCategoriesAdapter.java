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

    public interface OnAddImageListener {
        void onAddImage(@NonNull ImageCategorySection section);
    }

    /** Callback למחיקת תמונה – מועבר ל-Activity/Fragment כדי לבצע מחיקה מה-DB */
    public interface OnImageDeleteListener {
        void onDelete(@NonNull ImageCategorySection section,
                      @NonNull Image image,
                      int sectionIndex,
                      int imageIndex);
    }

    private final List<ImageCategorySection> categories;
    private final Context context;
    private final OnAddImageListener addImageListener;
    private final OnImageDeleteListener deleteListener;

    public ImageCategoriesAdapter(@NonNull List<ImageCategorySection> categories,
                                  @NonNull Context context,
                                  @NonNull OnAddImageListener addImageListener,
                                  @NonNull OnImageDeleteListener deleteListener) {
        this.categories = categories;
        this.context = context;
        this.addImageListener = addImageListener;
        this.deleteListener = deleteListener;
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

        // Adapter לתמונות בתוך הסקשן
        ImagePagerAdapter pagerAdapter = new ImagePagerAdapter(
                section.images,
                new ImagePagerAdapter.OnImageActionListener() {
                    @Override
                    public void onDelete(int imagePosition) {
                        // אל תמחוק כאן מה-UI: מעבירים ל-Activity שיטפל (כולל DB + Rollback)
                        int sectionIndex = holder.getAdapterPosition();
                        if (sectionIndex == RecyclerView.NO_POSITION) return;
                        if (imagePosition < 0 || imagePosition >= section.images.size()) return;

                        Image image = section.images.get(imagePosition);
                        if (deleteListener != null) {
                            deleteListener.onDelete(section, image, sectionIndex, imagePosition);
                        }
                    }

                    @Override
                    public void onDescriptionChanged(int imagePosition, String newText) {
                        if (imagePosition >= 0 && imagePosition < section.images.size()) {
                            section.images.get(imagePosition).setDescription(newText);
                            // אם תרצי לשמור ל-DB, תעשי זאת ב-Activity דרך callback נפרד
                        }
                    }

                    @Override
                    public void onImageClick(int imagePosition) {
                        // פתיחת תצוגה מורחבת/דיאלוג – אם תרצי, תעשי זאת ב-Activity דרך callback נוסף
                    }
                }
        );

        holder.viewPagerImages.setAdapter(pagerAdapter);

        // נוודא שאין callback ישן לפני שמרשמים חדש
        if (holder.pageChangeCallback != null) {
            holder.viewPagerImages.unregisterOnPageChangeCallback(holder.pageChangeCallback);
        }

        holder.pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int pos) {
                super.onPageSelected(pos);
                holder.updateDescriptionForPosition(section, pos);
            }
        };
        holder.viewPagerImages.registerOnPageChangeCallback(holder.pageChangeCallback);

        // עדכון ראשוני של ה-description לפי התמונה הראשונה/נוכחית
        int current = holder.viewPagerImages.getCurrentItem();
        holder.updateDescriptionForPosition(section, current);

        holder.btnAddImage.setOnClickListener(v -> {
            if (addImageListener != null) {
                addImageListener.onAddImage(section);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    /** רענון של קטגוריה מסוימת (למשל אחרי הוספה/מחיקה/סיווג ב-Activity) */
    public void notifyImageChanged(int categoryPosition) {
        notifyItemChanged(categoryPosition);
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle;
        TextView txtImageDescription;
        ViewPager2 viewPagerImages;
        Button btnAddImage;

        ViewPager2.OnPageChangeCallback pageChangeCallback;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtCategoryTitle);
            viewPagerImages = itemView.findViewById(R.id.viewPagerImages);
            btnAddImage = itemView.findViewById(R.id.btnAddImage);
            txtImageDescription = itemView.findViewById(R.id.txtImageDescription);
        }

        /**
         * Updates the description TextView according to the currently visible image in the section.
         */
        void updateDescriptionForPosition(@NonNull ImageCategorySection section, int position) {
            if (section.images == null || section.images.isEmpty()
                    || position < 0 || position >= section.images.size()) {
                txtImageDescription.setText("");
                txtImageDescription.setVisibility(View.GONE);
                return;
            }

            String desc = section.images.get(position).getDescription();
            if (desc == null || desc.trim().isEmpty()) {
                txtImageDescription.setText("");
                txtImageDescription.setVisibility(View.GONE);
            } else {
                txtImageDescription.setVisibility(View.VISIBLE);
                txtImageDescription.setText(desc);
            }
        }
    }
}
