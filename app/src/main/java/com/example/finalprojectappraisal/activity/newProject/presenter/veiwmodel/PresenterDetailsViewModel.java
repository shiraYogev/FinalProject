/**
 * Summary:
 * ViewModel for PresenterDetailsActivity. Loads existing project fields,
 * derives sensible defaults (creationDate -> appraisal_date), and persists changes
 * via ProjectRepository. Autofill now uses appraiser profile ONLY for the appraiser
 * full name (not the role) since role is entered by the user in the UI.
 *
 * Notes:
 * - All Firestore access is through repositories.
 * - Exposes a simple Form model and loading/error to the Activity.
 */

package com.example.finalprojectappraisal.activity.newProject.presenter.veiwmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.database.repository.AppraiserRepository;
import com.example.finalprojectappraisal.model.Appraiser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PresenterDetailsViewModel extends ViewModel {

    private final ProjectRepository projectRepo = ProjectRepository.getInstance();
    private final AppraiserRepository appraiserRepo = AppraiserRepository.getInstance();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String>  error   = new MutableLiveData<>(null);
    private final MutableLiveData<Form>    form    = new MutableLiveData<>(new Form());

    private String projectId;

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String>  getError()   { return error; }
    public LiveData<Form>    getForm()    { return form; }

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public void init(@NonNull String projectId, @Nullable String currentUserId) {
        this.projectId = projectId;
        loadProjectThenAutofill(currentUserId);
    }

    private void loadProjectThenAutofill(@Nullable String currentUserId) {
        loading.setValue(true);
        projectRepo.getProject(projectId, task -> {
            loading.setValue(false);
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                Form f = ensureForm();
                if (isEmpty(f.appraisalDate)) f.appraisalDate = DATE_FMT.format(new Date());
                form.setValue(f);
                autofillAppraiserNameIfMissing(currentUserId);
                return;
            }

            DocumentSnapshot doc = task.getResult();
            Form f = ensureForm();
            f.appraiserName = asStr(doc.getString("appraiser_name"));
            f.appraisalDate = asStr(doc.getString("appraisal_date"));
            f.appraiserRole = asStr(doc.getString("appraiser_role")); // role is UI-entered

            Object raw = doc.get("presenter_details");
            if (raw instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) raw;
                f.nameOfPresenter   = asStr(m.get("name_of_presenter"));
                f.idOfPresenter     = asStr(m.get("id_of_presenter"));
                f.typeOfPresenterId = asStr(m.get("type_of_presenter_id"));
                f.roleOfPresenter   = asStr(m.get("role_of_presenter"));
                f.holderStatus      = asStr(m.get("holder_status"));
            }

            if (isEmpty(f.appraisalDate)) {
                Long ms = doc.getLong("creationDate");
                f.appraisalDate = (ms != null) ? DATE_FMT.format(new Date(ms)) : DATE_FMT.format(new Date());
            }

            form.setValue(f);
            // Only autofill name from profile (not role)
            autofillAppraiserNameIfMissing(currentUserId);
        });
    }

    /** Autofills only the appraiser's full name (not the role). */
    public void autofillAppraiserNameIfMissing(@Nullable String userId) {
        Form f = ensureForm();
        boolean needName = isEmpty(f.appraiserName);
        if (!needName) return;
        if (userId == null || userId.trim().isEmpty()) return;

        appraiserRepo.getAppraiserById(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Appraiser a = task.getResult();
                if (a.getFullName() != null && isEmpty(f.appraiserName)) {
                    f.appraiserName = a.getFullName();
                    form.setValue(f);
                }
            }
        });
    }

    public LiveData<Boolean> save(@NonNull Form f) {
        MutableLiveData<Boolean> result = new MutableLiveData<>(false);

        Map<String, Object> flat = new HashMap<>();
        flat.put("appraiser_name", f.appraiserName);
        flat.put("appraisal_date", f.appraisalDate);
        flat.put("appraiser_role", f.appraiserRole); // kept as provided by UI

        Map<String, Object> presenter = new HashMap<>();
        presenter.put("name_of_presenter",    f.nameOfPresenter);
        presenter.put("id_of_presenter",      f.idOfPresenter);
        presenter.put("type_of_presenter_id", f.typeOfPresenterId);
        presenter.put("role_of_presenter",    f.roleOfPresenter);
        presenter.put("holder_status",        f.holderStatus);

        Map<String, Object> write = new HashMap<>(flat);
        write.put("presenter_details", presenter);

        loading.setValue(true);
        projectRepo.updateMultipleFields(projectId, write, task -> {
            loading.setValue(false);
            if (task != null && task.isSuccessful()) {
                result.setValue(true);
            } else {
                String msg = (task != null && task.getException() != null)
                        ? task.getException().getMessage() : "Unknown error";
                error.setValue("Failed to save: " + msg);
                result.setValue(false);
            }
        });
        return result;
    }

    // Helpers & form
    private Form ensureForm() { return form.getValue() != null ? form.getValue() : new Form(); }
    private static String asStr(Object o) { return o == null ? null : String.valueOf(o); }
    private static boolean isEmpty(@Nullable String s) { return s == null || s.trim().isEmpty(); }

    public static class Form {
        public String appraiserName;
        public String appraisalDate;
        public String appraiserRole; // UI-entered

        public String nameOfPresenter;
        public String idOfPresenter;
        public String typeOfPresenterId;
        public String roleOfPresenter;
        public String holderStatus;
    }
}
