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
                                Map<String, Object> imgData = doc.getData();
                                if (imgData == null) continue;
                                Image img = new Image();
                                img.setId(doc.getId());
                                Object url = imgData.get("url");
                                if (url != null) img.setUrl(url.toString());
                                Object cat = imgData.get("category");
                                if (cat != null) {
                                    try {
                                        img.setCategory(Image.Category.valueOf(cat.toString()));
                                    } catch (IllegalArgumentException ex) {
                                        img.setCategory(Image.Category.OTHER);
                                    }
                                }
                                Object desc = imgData.get("description");
                                if (desc != null) img.setDescription(desc.toString());
                                images.add(img);
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
        Log.d(TAG, "=== fillProjectData START docId=" + doc.getId() + " ===");

        // שליפת כל הנתונים הגולמיים כ-Map פשוט (ללא deserialization)
        Map<String, Object> root = doc.getData();
        if (root == null) {
            Log.w(TAG, "fillProjectData: doc.getData() returned null!");
            return;
        }
        Log.d(TAG, "ROOT keys: " + root.keySet().toString());

        // שליפת property_details map
        Map<String, Object> pd = getNestedMap(root, "property_details");
        Log.d(TAG, "property_details keys: " + (pd != null ? pd.keySet().toString() : "NULL - not found!"));

        // שליפת bankDetails map
        Map<String, Object> bankMapRaw = getNestedMap(root, "bankDetails");
        Log.d(TAG, "bankDetails keys: " + (bankMapRaw != null ? bankMapRaw.keySet().toString() : "NULL - not found!"));

        // שליפת client map
        Map<String, Object> clientMapRaw = getNestedMap(root, "client");

        // שליפת presenter_details map
        Map<String, Object> presenterMapRaw = getNestedMap(root, "presenter_details");

        // נתוני שמאי (שורש)
        exportData.setAppraiserName(safeGetString(doc, "appraiser_name"));
        exportData.setAppraisalDate(safeGetString(doc, "appraisal_date"));
        exportData.setAppraiserRole(safeGetString(doc, "appraiser_role"));

        // נתוני לקוח
        try {
            if (clientMapRaw != null) {
                Client client = new Client();
                client.setClientId(firstNonEmpty(safeMapValue(clientMapRaw, "clientId"), safeMapValue(clientMapRaw, "client_id")));
                client.setFullName(firstNonEmpty(safeMapValue(clientMapRaw, "fullName"), safeMapValue(clientMapRaw, "full_name")));
                client.setEmail(safeMapValue(clientMapRaw, "email"));
                client.setPhoneNumber(firstNonEmpty(safeMapValue(clientMapRaw, "phoneNumber"), safeMapValue(clientMapRaw, "phone_number")));
                exportData.setClient(client);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse client data", e);
        }

        // פרטי מוסר
        if (presenterMapRaw != null) {
            exportData.setPresenterDetails(presenterMapRaw);
        }

        // פרטי בנק
        try {
            Map<String, Object> bankMap = bankMapRaw;
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

        // שדות מ-property_details עם fallback לשורש
        exportData.setFullAddress(safeGetString(doc, "full_address"));
        Log.d(TAG, "full_address=" + safeGetString(doc, "full_address"));
        exportData.setPropertySummary(pdOrRoot(pd, doc, "property_summary"));
        Log.d(TAG, "property_summary=" + pdOrRoot(pd, doc, "property_summary"));
        exportData.setBuildingEntry(pdOrRoot(pd, doc, "building_entry"));
        Log.d(TAG, "building_entry=" + pdOrRoot(pd, doc, "building_entry"));
        exportData.setBuildingNumber(pdOrRoot(pd, doc, "building_number"));
        exportData.setZoneNumber(pdOrRoot(pd, doc, "zone_number"));
        exportData.setBuildingCityPlanNumber(pdOrRoot(pd, doc, "building_city_plan_number"));
        exportData.setEnvironmentCharacteristics(pdOrRoot(pd, doc, "environment_characteristics"));
        exportData.setPropertyLocation(pdOrRoot(pd, doc, "property_location"));
        exportData.setBuildingType(pdOrRoot(pd, doc, "building_type"));
        exportData.setPhysicalCondition(pdOrRoot(pd, doc, "physical_condition"));
        exportData.setMaintenance(pdOrRoot(pd, doc, "maintenance"));
        exportData.setConstructionMaterial(pdOrRoot(pd, doc, "construction_material"));
        exportData.setExternalCladding(pdOrRoot(pd, doc, "external_cladding"));
        exportData.setNumberOfFloors(pdOrRoot(pd, doc, "number_of_floors"));
        exportData.setRegisteredApartmentArea(pdOrRoot(pd, doc, "registered_apartment_area"));
        exportData.setGrossApartmentArea(pdOrRoot(pd, doc, "gross_apartment_area"));

        // has_elevator - בשורש כ-string ("יש (2)" וכו')
        exportData.setHasElevator(safeGetString(doc, "has_elevator"));

        // פרטי דירה - property_details עם fallback לשורש
        // apartment_number - יש שני שמות אפשריים ב-property_details
        String aptNum = safeMapValue(pd, "apartment_number(municipal_form)");
        if (aptNum.isEmpty()) aptNum = safeMapValue(pd, "apartment_number_municipal_form");
        if (aptNum.isEmpty()) aptNum = safeGetString(doc, "apartment_number(municipal_form)");
        exportData.setApartmentNumber(aptNum);

        exportData.setApartmentStory(pdOrRoot(pd, doc, "apartment_story"));
        exportData.setNumberOfRooms(pdOrRoot(pd, doc, "number_of_rooms"));
        exportData.setApartmentIncludes(pdOrRoot(pd, doc, "apartment_includes"));
        exportData.setApartmentRenovations(pdOrRoot(pd, doc, "apartment_renovations"));

        // שדות AI - נמצאים בשורש בלבד
        exportData.setApartmentKitchen(safeGetString(doc, "apartment_kitchen"));
        exportData.setApartmentFlooring(safeGetString(doc, "apartment_flooring"));
        exportData.setApartmentMainEntranceDoor(safeGetString(doc, "apartment_main_entrance_door"));
        exportData.setApartmentInteriorDoorsAndFrames(safeGetString(doc, "apartment_interior_doors_and_frames"));
        exportData.setApartmentBathroomFixtures(safeGetString(doc, "apartment_bathroom_fixtures"));
        exportData.setApartmentWindows(safeGetString(doc, "apartment_windows"));
        exportData.setApartmentAirConditioning(safeGetString(doc, "apartment_air_conditioning"));
        exportData.setHasBars(safeGetString(doc, "has_bars"));

        // כיווני אוויר - property_details עם fallback לשורש
        try {
            List<String> directions = null;
            if (pd != null && pd.get("apartment_directions") instanceof List) {
                directions = (List<String>) pd.get("apartment_directions");
            }
            if (directions == null || directions.isEmpty()) {
                directions = doc.get("apartment_directions", List.class);
            }
            exportData.setApartmentDirections(directions != null ? directions : new ArrayList<>());
        } catch (Exception e) {
            exportData.setApartmentDirections(new ArrayList<>());
        }

        // שדות בוליאניים - בשורש
        Boolean centralHeating = doc.getBoolean("central_heating_or_fireplace");
        exportData.setCentralHeatingOrFireplace(centralHeating != null ? centralHeating : false);

        Boolean hasParking = doc.getBoolean("has_parking");
        exportData.setHasParking(hasParking != null ? hasParking : false);

        Boolean hasStorage = doc.getBoolean("has_storage");
        exportData.setHasStorage(hasStorage != null ? hasStorage : false);

        // תמונת טאבו (שורש)
        exportData.setTabuCropImage(safeGetString(doc, "tabu_crop_image"));
    }

    /** שולף nested Map מתוך root map ללא שגיאת generic type */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getNestedMap(Map<String, Object> root, String key) {
        if (root == null) return null;
        Object val = root.get(key);
        if (val instanceof Map) return (Map<String, Object>) val;
        return null;
    }

    /** שולף שדה מ-property_details, ואם ריק - מהשורש */
    private String pdOrRoot(Map<String, Object> pd, DocumentSnapshot doc, String key) {
        String val = safeMapValue(pd, key);
        if (!val.isEmpty()) return val;
        return safeGetString(doc, key);
    }

    /** מחזיר את הראשון שאינו ריק */
    private String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) return v;
        }
        return "";
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
