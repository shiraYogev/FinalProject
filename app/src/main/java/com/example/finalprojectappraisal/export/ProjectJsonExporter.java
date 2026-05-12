package com.example.finalprojectappraisal.export;

import android.util.Log;

import com.example.finalprojectappraisal.model.BankDetails;
import com.example.finalprojectappraisal.model.Client;
import com.example.finalprojectappraisal.model.Image;
import com.example.finalprojectappraisal.model.Project;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * מנהל את כל תהליך הייצוא של פרויקט ל-JSON.
 * אוסף נתונים מכל מקורות המידע (פרויקט, בנק, תמונות) ויוצא אותם לקולקציה ייעודית.
 */
public class ProjectJsonExporter {

    private static final String TAG = "ProjectJsonExporter";

    private final FirebaseFirestore db;
    private final JsonExportRepository exportRepository;

    public interface ExportCallback {
        void onSuccess(String projectId);
        void onError(String errorMessage);
    }

    public ProjectJsonExporter() {
        this.db = FirebaseFirestore.getInstance();
        this.exportRepository = new JsonExportRepository();
    }

    /**
     * מבצע ייצוא מלא של פרויקט.
     * 1. טוען את נתוני הפרויקט
     * 2. טוען את נתוני הבנק
     * 3. טוען את התמונות
     * 4. יוצר JSON
     * 5. שומר לקולקציית הייצוא
     */
    public void exportProject(String projectId, ExportCallback callback) {
        if (projectId == null || projectId.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("Project ID is required");
            }
            return;
        }

        Log.d(TAG, "Starting export for project: " + projectId);

        // טעינת נתוני הפרויקט
        loadProjectData(projectId, new ProjectLoadCallback() {
            @Override
            public void onDataLoaded(ProjectJsonExportData exportData) {
                // יצירת JSON
                createAndSaveJson(projectId, exportData, callback);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load project data: " + error);
                if (callback != null) {
                    callback.onError("Failed to load project data: " + error);
                }
            }
        });
    }

    /**
     * טוען את כל נתוני הפרויקט ממקורות שונים
     */
    private void loadProjectData(String projectId, ProjectLoadCallback callback) {
        ProjectJsonExportData exportData = new ProjectJsonExportData();
        exportData.setProjectId(projectId);

        // טעינת מסמך הפרויקט הראשי
        db.collection("projects").document(projectId)
                .get()
                .addOnCompleteListener(projectTask -> {
                    if (!projectTask.isSuccessful() || projectTask.getResult() == null || !projectTask.getResult().exists()) {
                        callback.onError("Project not found");
                        return;
                    }

                    DocumentSnapshot projectDoc = projectTask.getResult();

                    // מילוי נתוני פרויקט
                    fillProjectData(exportData, projectDoc);

                    // טעינת תת-אוסף התמונות
                    loadImages(projectId, exportData, new ImagesLoadCallback() {
                        @Override
                        public void onImagesLoaded(List<Image> images) {
                            exportData.setAllImages(images);
                            callback.onDataLoaded(exportData);
                        }

                        @Override
                        public void onError(String error) {
                            // אם התמונות נכשלו, עדיין ממשיכים עם הפרויקט
                            exportData.setAllImages(new ArrayList<>());
                            callback.onDataLoaded(exportData);
                        }
                    });
                });
    }

    /**
     * טוען את התמונות מתת-האוסף
     */
    private void loadImages(String projectId, ProjectJsonExportData exportData, ImagesLoadCallback callback) {
        db.collection("projects").document(projectId)
                .collection("images")
                .get()
                .addOnCompleteListener(task -> {
                    List<Image> images = new ArrayList<>();

                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            try {
                                Image img = doc.toObject(Image.class);
                                if (img != null) {
                                    // וידוא שיש ID
                                    if (img.getId() == null || img.getId().isEmpty()) {
                                        img.setId(doc.getId());
                                    }
                                    images.add(img);
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Failed to parse image: " + doc.getId(), e);
                            }
                        }
                    }

                    callback.onImagesLoaded(images);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load images", e);
                    callback.onImagesLoaded(new ArrayList<>());
                });
    }

    /**
     * ממלא את נתוני הפרויקט ממסמך Firestore
     */
    private void fillProjectData(ProjectJsonExportData exportData, DocumentSnapshot doc) {
        // נתוני שמאי
        exportData.setAppraiserName(safeGetString(doc, "appraiser_name"));
        exportData.setAppraisalDate(safeGetString(doc, "appraisal_date"));
        exportData.setAppraiserRole(safeGetString(doc, "appraiser_role"));

        // נתוני לקוח
        try {
            Map<String, Object> clientMap = doc.get("client", Map.class);
            if (clientMap != null) {
                Client client = new Client();
                client.setClientId(safeMapValue(clientMap, "client_id"));
                client.setFullName(safeMapValue(clientMap, "full_name"));
                client.setEmail(safeMapValue(clientMap, "email"));
                client.setPhoneNumber(safeMapValue(clientMap, "phone_number"));
                exportData.setClient(client);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse client data", e);
        }

        // פרטי מוסר
        try {
            Map<String, Object> presenterMap = doc.get("presenter_details", Map.class);
            if (presenterMap != null) {
                exportData.setPresenterDetails(presenterMap);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse presenter details", e);
        }

        // פרטי בנק
        try {
            Map<String, Object> bankMap = doc.get("bank_details", Map.class);
            if (bankMap != null) {
                BankDetails bankDetails = new BankDetails();
                bankDetails.setBankName(safeMapValue(bankMap, "bank_name"));
                bankDetails.setBranchName(safeMapValue(bankMap, "branch_name"));
                bankDetails.setBranchEmail(safeMapValue(bankMap, "branch_email"));
                bankDetails.setBankerName(safeMapValue(bankMap, "banker_name"));
                bankDetails.setDocumentDateGre(safeMapValue(bankMap, "document_date_gre"));
                bankDetails.setDocumentDateHe(safeMapValue(bankMap, "document_date_he"));
                bankDetails.setValuationNumber(safeMapValue(bankMap, "valuation_number"));
                bankDetails.setLoanNumber(safeMapValue(bankMap, "loan_number"));
                bankDetails.setTypeOfLoan(safeMapValue(bankMap, "type_of_loan"));
                bankDetails.setPage1Header(safeMapValue(bankMap, "page1_header"));
                bankDetails.setLotNumber(safeMapValue(bankMap, "lot_number"));
                bankDetails.setMainParcel(safeMapValue(bankMap, "main_parcel"));
                bankDetails.setSubParcel(safeMapValue(bankMap, "sub_parcel"));
                bankDetails.setShortAddress(safeMapValue(bankMap, "short_address"));
                bankDetails.setLoanerName(safeMapValue(bankMap, "loaner_name"));
                bankDetails.setLoanerId(safeMapValue(bankMap, "loaner_id"));
                bankDetails.setPurposeOfLoan(safeMapValue(bankMap, "purpose_of_loan"));
                bankDetails.setIdentityOfCustomer(safeMapValue(bankMap, "identity_of_customer"));
                bankDetails.setAppraisalFinalDate(safeMapValue(bankMap, "appraisal_final_date"));
                exportData.setBankDetails(bankDetails);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse bank details", e);
        }

        // סיכום וכתובת
        exportData.setPropertySummary(safeGetString(doc, "property_summary"));
        exportData.setFullAddress(safeGetString(doc, "full_address"));
        exportData.setBuildingEntry(safeGetString(doc, "building_entry"));
        exportData.setBuildingNumber(safeGetString(doc, "building_number"));
        exportData.setZoneNumber(safeGetString(doc, "zone_number"));
        exportData.setBuildingCityPlanNumber(safeGetString(doc, "building_city_plan_number"));
        exportData.setEnvironmentCharacteristics(safeGetString(doc, "environment_characteristics"));
        exportData.setPropertyLocation(safeGetString(doc, "property_location"));
        exportData.setBuildingType(safeGetString(doc, "building_type"));
        exportData.setPhysicalCondition(safeGetString(doc, "physical_condition"));
        exportData.setMaintenance(safeGetString(doc, "maintenance"));
        exportData.setConstructionMaterial(safeGetString(doc, "construction_material"));
        exportData.setHasElevator(safeGetString(doc, "has_elevator"));
        exportData.setExternalCladding(safeGetString(doc, "external_cladding"));
        exportData.setNumberOfFloors(safeGetString(doc, "number_of_floors"));

        // פרטי דירה
        exportData.setApartmentNumber(safeGetString(doc, "apartment_number(municipal_form)"));
        exportData.setApartmentStory(safeGetString(doc, "apartment_story"));
        exportData.setNumberOfRooms(safeGetString(doc, "number_of_rooms"));
        exportData.setApartmentIncludes(safeGetString(doc, "apartment_includes"));
        exportData.setApartmentKitchen(safeGetString(doc, "apartment_kitchen"));
        exportData.setApartmentFlooring(safeGetString(doc, "apartment_flooring"));
        exportData.setApartmentMainEntranceDoor(safeGetString(doc, "apartment_main_entrance_door"));
        exportData.setApartmentInteriorDoorsAndFrames(safeGetString(doc, "apartment_interior_doors_and_frames"));
        exportData.setApartmentBathroomFixtures(safeGetString(doc, "apartment_bathroom_fixtures"));
        exportData.setApartmentWindows(safeGetString(doc, "apartment_windows"));
        exportData.setHasBars(safeGetString(doc, "has_bars"));
        exportData.setApartmentAirConditioning(safeGetString(doc, "apartment_air_conditioning"));
        exportData.setRegisteredApartmentArea(safeGetString(doc, "registered_apartment_area"));
        exportData.setGrossApartmentArea(safeGetString(doc, "gross_apartment_area"));

        // כיווני אוויר (רשימה)
        try {
            List<String> directions = doc.get("apartment_directions", List.class);
            exportData.setApartmentDirections(directions != null ? directions : new ArrayList<>());
        } catch (Exception e) {
            exportData.setApartmentDirections(new ArrayList<>());
        }

        // שדות בוליאניים
        Boolean centralHeating = doc.getBoolean("central_heating_or_fireplace");
        exportData.setCentralHeatingOrFireplace(centralHeating != null ? centralHeating : false);

        Boolean hasParking = doc.getBoolean("has_parking");
        exportData.setHasParking(hasParking != null ? hasParking : false);

        Boolean hasStorage = doc.getBoolean("has_storage");
        exportData.setHasStorage(hasStorage != null ? hasStorage : false);

        // תמונת טאבו
        exportData.setTabuCropImage(safeGetString(doc, "tabu_crop_image"));
    }

    /**
     * יוצר את ה-JSON ושומר אותו
     */
    private void createAndSaveJson(String projectId, ProjectJsonExportData exportData, ExportCallback callback) {
        try {
            // יצירת ה-JSON
            JSONObject json = ProjectJsonBuilder.buildExportJson(exportData);

            // שמירה
            exportRepository.saveJsonExport(projectId, json, task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Export completed successfully for project: " + projectId);
                    if (callback != null) {
                        callback.onSuccess(projectId);
                    }
                } else {
                    String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    Log.e(TAG, "Failed to save export: " + error);
                    if (callback != null) {
                        callback.onError("Failed to save export: " + error);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to create JSON", e);
            if (callback != null) {
                callback.onError("Failed to create JSON: " + e.getMessage());
            }
        }
    }

    // =================== Helpers ===================

    private String safeGetString(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value != null ? value : "";
    }

    private String safeMapValue(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return "";
        }
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    // =================== Callback Interfaces ===================

    private interface ProjectLoadCallback {
        void onDataLoaded(ProjectJsonExportData exportData);
        void onError(String error);
    }

    private interface ImagesLoadCallback {
        void onImagesLoaded(List<Image> images);
        void onError(String error);
    }
}
