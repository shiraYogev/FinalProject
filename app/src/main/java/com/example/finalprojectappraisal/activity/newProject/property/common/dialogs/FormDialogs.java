package com.example.finalprojectappraisal.activity.newProject.property.common.dialogs;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;

import com.example.finalprojectappraisal.activity.newProject.property.common.utils.Formatters;

import java.util.List;

public final class FormDialogs {
    private FormDialogs() {}

    public interface YesNoListener { void onPicked(boolean selectedYes); }
    public interface StringPickListener { void onPicked(String selection); }

    public static void showYesNo(Context ctx, String title, boolean currentYes, YesNoListener listener) {
        String[] choices = new String[]{Formatters.YES, Formatters.NO};
        int checked = currentYes ? 0 : 1;
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setSingleChoiceItems(choices, checked, null)
                .setPositiveButton("בחר/י", (dialog, which) -> {
                    int sel = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    listener.onPicked(sel == 0);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    public static void showSingleChoice(Context ctx, String title, List<String> options, String currentValue, StringPickListener listener) {
        int checked = Math.max(0, options.indexOf(currentValue));
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setSingleChoiceItems(options.toArray(new String[0]), checked, null)
                .setPositiveButton("בחר/י", (dialog, which) -> {
                    int sel = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (sel >= 0 && sel < options.size()) {
                        listener.onPicked(options.get(sel));
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}