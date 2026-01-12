package com.example.finalprojectappraisal.classifer;

import com.example.finalprojectappraisal.model.Image;

import java.util.ArrayList;
import java.util.List;

public class ImageCategorySection {
    public final String title;
    public final Image.Category category;
    public final String prompt;
    public final List<Image> images = new ArrayList<>();

    // ✅ NEW: extra identity for sections that share the same category (BEDROOM/BATHROOM)
    public final Image.Subcategory subCategory; // MASTER / GUEST / NONE
    public final int bedroomIndex;              // 0 = not numbered, 1..N = bedroom number

    // Backward compatible ctor (keeps existing code working)
    public ImageCategorySection(String title, Image.Category category, String prompt) {
        this(title, category, prompt, Image.Subcategory.NONE, 0);
    }

    public ImageCategorySection(String title,
                                Image.Category category,
                                String prompt,
                                Image.Subcategory subCategory,
                                int bedroomIndex) {
        this.title = title;
        this.category = category;
        this.prompt = prompt;
        this.subCategory = (subCategory != null) ? subCategory : Image.Subcategory.NONE;
        this.bedroomIndex = bedroomIndex;
    }
}
