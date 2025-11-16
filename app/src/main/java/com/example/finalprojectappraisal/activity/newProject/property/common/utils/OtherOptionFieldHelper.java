package com.example.finalprojectappraisal.activity.newProject.property.common.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper for handling "Other" option in single-choice lists.
 * If the user selects "אחר", it opens a free-text dialog and returns the typed value.
 */
public final class OtherOptionFieldHelper {

    public interface OnValueChosen {
        void onValue(String value);
    }

    private static final String OTHER_LABEL = "אחר";

    private OtherOptionFieldHelper() {
        // no instances
    }

    /**
     * Handle a single selection that may be "אחר".
     *
     * @param context       Activity/Context for dialogs
     * @param fieldTitle    logical field title (used in dialog title)
     * @param selected      the value selected from the list
     * @param currentValue  current stored value (for prefill)
     * @param callback      called with final value (either selected or free text)
     */
    public static void handleSelection(
            @NonNull Context context,
            @NonNull String fieldTitle,
            @NonNull String selected,
            @Nullable String currentValue,
            @NonNull OnValueChosen callback
    ) {
        // If not "אחר" → simply return the selected value
        if (!OTHER_LABEL.equals(selected)) {
            callback.onValue(selected);
            return;
        }

        // "אחר" → ask for free text
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        if (currentValue != null && !currentValue.isEmpty() && !OTHER_LABEL.equals(currentValue)) {
            input.setText(currentValue);
            input.setSelection(currentValue.length());
        }

        new AlertDialog.Builder(context)
                .setTitle(fieldTitle + " - אחר")
                .setView(input)
                .setPositiveButton("אישור", (d, w) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        callback.onValue(text);
                    }
                    // If empty → do nothing (caller keeps old value)
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}
