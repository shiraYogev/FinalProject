package com.example.finalprojectappraisal.activity.myProjects.note;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.database.constants.FirestoreConstants;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class AddNoteBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public interface OnNoteSavedListener {
        void onNoteSaved(@NonNull String projectId, @NonNull String newNote);
    }

    private static final String TAG = "AddNoteBS";
    private static final String ARG_PROJECT_ID = "arg_project_id";
    private static final String ARG_CURRENT_NOTE = "arg_current_note";

    @Nullable private OnNoteSavedListener callback;

    public static AddNoteBottomSheetDialogFragment newInstance(@NonNull String projectId,
                                                               @Nullable String currentNote) {
        AddNoteBottomSheetDialogFragment f = new AddNoteBottomSheetDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_PROJECT_ID, projectId);
        b.putString(ARG_CURRENT_NOTE, currentNote);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // אם ה-Activity (או ה-Parent Fragment) מממשים את הממשק – נשתמש בהם כ-callback
        if (getParentFragment() instanceof OnNoteSavedListener) {
            callback = (OnNoteSavedListener) getParentFragment();
        } else if (context instanceof OnNoteSavedListener) {
            callback = (OnNoteSavedListener) context;
        } else {
            callback = null; // לא חובה – אפשר להסתמך על ה-LiveData של Firestore
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bs_add_note, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        EditText input = v.findViewById(R.id.input_note);
        MaterialButton btnSave = v.findViewById(R.id.btn_save_note);
        MaterialButton btnCancel = v.findViewById(R.id.btn_cancel_note);

        String projectId = getArguments() != null ? getArguments().getString(ARG_PROJECT_ID) : null;
        String currentNote = getArguments() != null ? getArguments().getString(ARG_CURRENT_NOTE) : null;

        if (!TextUtils.isEmpty(currentNote)) {
            input.setText(currentNote);
            input.setSelection(currentNote.length());
        }

        btnCancel.setOnClickListener(view -> dismiss());

        btnSave.setOnClickListener(view -> {
            if (projectId == null || projectId.trim().isEmpty()) {
                Toast.makeText(requireContext(), "חסר מזהה פרויקט", Toast.LENGTH_SHORT).show();
                return;
            }
            String newNote = input.getText() != null ? input.getText().toString().trim() : "";
            Log.d(TAG, "save note: pid=" + projectId + " len=" + newNote.length());

            Map<String, Object> fields = new HashMap<>();
            fields.put("note", newNote);
            fields.put(FirestoreConstants.FIELD_LAST_UPDATE_DATE, FieldValue.serverTimestamp());

            ProjectRepository.getInstance().updateMultipleFields(projectId, fields, task -> {
                boolean ok = task.isSuccessful();
                Log.d(TAG, "updateMultipleFields.onComplete ok=" + ok);
                if (ok) {
                    if (callback != null) {
                        callback.onNoteSaved(projectId, newNote);
                    }
                    Toast.makeText(requireContext(), "ההערה נשמרה ✅", Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    Toast.makeText(requireContext(), "שמירת ההערה נכשלה", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }
}
