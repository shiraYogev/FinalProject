package com.example.finalprojectappraisal.classifer;

import com.example.finalprojectappraisal.model.Image;

import java.util.ArrayList;
import java.util.List;

public class ImageCategorySection {
    public final String title;
    public final Image.Category category;
    public final String prompt;
    public final List<Image> images = new ArrayList<>();

    public ImageCategorySection(String title, Image.Category category, String prompt) {
        this.title = title;
        this.category = category;
        this.prompt = prompt;
    }
}

