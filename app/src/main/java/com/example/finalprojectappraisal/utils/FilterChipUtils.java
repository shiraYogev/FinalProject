package com.example.finalprojectappraisal.utils;

import android.content.Context;

import com.google.android.material.chip.Chip;

public class FilterChipUtils {

    /** יוצר Chip מסוג Entry עם אייקון סגירה (X) ורוחב/גובה תקינים למגע */
    public static Chip makeEntryChip(Context ctx, String text, Runnable onClose) {
        Chip c = new Chip(ctx, null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Entry);
        c.setText(text);
        c.setCheckable(false);
        c.setClickable(true);
        c.setCloseIconVisible(true);
        c.setCloseIconResource(android.R.drawable.ic_menu_close_clear_cancel);
        c.setOnCloseIconClickListener(v -> {
            if (onClose != null) onClose.run();
        });
        return c;
    }
}
