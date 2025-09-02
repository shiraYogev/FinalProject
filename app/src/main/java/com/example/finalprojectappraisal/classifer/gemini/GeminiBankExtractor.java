package com.example.finalprojectappraisal.classifer.gemini;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.example.finalprojectappraisal.model.BankDetails;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiBankExtractor {
    private static final String API_KEY = "AIzaSyDhFwyH9JqdiElWGTKMPBnw_fAYxhk5pYo"; // החליפי ב-API שלך
    private static final String TAG = "GeminiBankExtractor";
    private static final Executor executor = Executors.newSingleThreadExecutor();

    public interface BankDataExtractionCallback {
        void onSuccess(BankDetails bankDetails);
        void onError(String error);
    }

    /**
     * חילוץ נתונים מקובץ PDF של פרטי הבנק
     */
    public static void extractBankDetailsFromPDF(Context context, Uri pdfUri, BankDataExtractionCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting bank details extraction from PDF: " + pdfUri);

                // המרת הדף הראשון של ה-PDF לתמונה
                Bitmap pdfBitmap = convertPdfToBitmap(context, pdfUri);
                if (pdfBitmap == null) {
                    callback.onError("לא ניתן להמיר את ה-PDF לתמונה");
                    return;
                }

                // יצירת prompt מפורט לחילוץ נתונים
                String prompt = createBankDataExtractionPrompt();

                // שליחה ל-Gemini
                extractDataWithGemini(pdfBitmap, prompt, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error extracting bank details", e);
                callback.onError("שגיאה בחילוץ נתוני הבנק: " + e.getMessage());
            }
        });
    }

    /**
     * המרת הדף הראשון של PDF לתמונה
     */
    private static Bitmap convertPdfToBitmap(Context context, Uri pdfUri) {
        try {
            ParcelFileDescriptor fileDescriptor = context.getContentResolver().openFileDescriptor(pdfUri, "r");
            if (fileDescriptor == null) {
                Log.e(TAG, "Cannot open PDF file descriptor");
                return null;
            }

            PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);
            PdfRenderer.Page page = pdfRenderer.openPage(0); // דף ראשון

            // יצירת Bitmap בגודל מתאים
            Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            page.close();
            pdfRenderer.close();
            fileDescriptor.close();

            Log.d(TAG, "Successfully converted PDF to bitmap");
            return bitmap;

        } catch (IOException e) {
            Log.e(TAG, "Error converting PDF to bitmap", e);
            return null;
        }
    }

    /**
     * יצירת prompt לחילוץ נתוני הבנק
     */
    private static String createBankDataExtractionPrompt() {
        return "אתה מומחה לחילוץ נתונים ממסמכים בנקיים בעברית. אנא חלץ את הנתונים הבאים ממסמך הבנק בתמונה והחזר תשובה בפורמט JSON בלבד:\n" +
                "\n" +
                "{\n" +
                "    \"bank_name\": \"שם הבנק בעברית\",\n" +
                "    \"branch_name\": \"שם הסניף בעברית\",\n" +
                "    \"branch_email\": \"כתובת אימייל של הסניף\",\n" +
                "    \"banker_name\": \"שם הבנקאי בעברית\",\n" +
                "    \"document_date_gre\": \"תאריך המסמך בפורמט YYYY-MM-DD\",\n" +
                "    \"document_date_he\": \"תאריך המסמך בעברית\",\n" +
                "    \"valuation_number\": \"מספר השמאי\",\n" +
                "    \"loan_number\": \"מספר ההלוואה (מספרים בלבד)\",\n" +
                "    \"type_of_loan\": \"סוג ההלוואה בעברית\",\n" +
                "    \"page1_header\": \"כותרת עמוד 1 בעברית\",\n" +
                "    \"lot_number\": \"מספר החלקה (מספרים בלבד)\",\n" +
                "    \"main_parcel\": \"חלקה ראשית (מספרים בלבד)\",\n" +
                "    \"sub_parcel\": \"חלקת משנה (מספרים בלבד)\",\n" +
                "    \"short_address\": \"כתובת קצרה בעברית\",\n" +
                "    \"loaner_name\": \"שם הלווה בעברית\",\n" +
                "    \"loaner_id\": \"מספר זהות הלווה (9 ספרות)\",\n" +
                "    \"purpose_of_loan\": \"מטרת ההלוואה בעברית\",\n" +
                "    \"identity_of_customer\": \"זהות הלקוח בעברית\",\n" +
                "    \"appraisal_final_date\": \"תאריך סיום השמאות בפורמט DD/MM/YYYY\"\n" +
                "}\n" +
                "\n" +
                "חשוב:\n" +
                "1. החזר רק JSON תקין ללא טקסט נוסף\n" +
                "2. אם שדה לא קיים במסמך, השתמש ב-null\n" +
                "3. וודא שמספרי זהות הם בדיוק 9 ספרות\n" +
                "4. תאריכים צריכים להיות בפורמט המבוקש בדיוק\n" +
                "5. אל תוסיף שדות שלא מבוקשים\n";
    }

    /**
     * שליחה ל-Gemini וחילוץ הנתונים
     */
    private static void extractDataWithGemini(Bitmap bitmap, String prompt, BankDataExtractionCallback callback) {
        try {
            Log.d(TAG, "Sending image to Gemini for bank data extraction");

            // יצירת מודל Gemini
            GenerativeModelFutures generativeModel = GenerativeModelFutures.from(
                    new GenerativeModel("gemini-1.5-pro", API_KEY)
            );

            // יצירת Content עם תמונה ו-prompt
            Content content = new Content.Builder()
                    .addImage(bitmap)
                    .addText(prompt)
                    .build();

            // שליחה ל-Gemini
            GenerateContentResponse response = generativeModel.generateContent(content).get();
            String jsonResponse = response.getText();

            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                callback.onError("לא התקבלה תשובה מ-Gemini");
                return;
            }

            Log.d(TAG, "Received response from Gemini: " + jsonResponse);

            // ניקוי התשובה (הסרת markdown אם קיים)
            jsonResponse = cleanJsonResponse(jsonResponse);

            // המרה ל-BankDetails object
            BankDetails bankDetails = parseJsonToBankDetails(jsonResponse);

            if (bankDetails != null) {
                Log.d(TAG, "Successfully extracted bank details: " + bankDetails.toString());
                callback.onSuccess(bankDetails);
            } else {
                callback.onError("לא ניתן לפרסר את התשובה מ-Gemini");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in Gemini extraction", e);
            callback.onError("שגיאה בחילוץ נתונים עם Gemini: " + e.getMessage());
        }
    }

    /**
     * ניקוי תשובת JSON מ-Gemini
     */
    private static String cleanJsonResponse(String response) {
        // הסרת markdown אם קיים
        response = response.replaceAll("```json", "").replaceAll("```", "").trim();

        // חיפוש ה-JSON הראשון במחרוזת
        int startIndex = response.indexOf("{");
        int endIndex = response.lastIndexOf("}");

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            response = response.substring(startIndex, endIndex + 1);
        }

        return response.trim();
    }

    /**
     * פיענוח JSON ל-BankDetails object
     */
    private static BankDetails parseJsonToBankDetails(String jsonString) {
        try {
            Gson gson = new Gson();
            BankDetails bankDetails = gson.fromJson(jsonString, BankDetails.class);

            // ולידציה בסיסית
            if (bankDetails != null) {
                // ניקוי ערכי null מיותרים
                cleanBankDetailsValues(bankDetails);
                return bankDetails;
            }

        } catch (JsonSyntaxException e) {
            Log.e(TAG, "JSON parsing error: " + e.getMessage());
            Log.e(TAG, "Problematic JSON: " + jsonString);
        }

        return null;
    }

    /**
     * ניקוי ערכים ב-BankDetails
     */
    private static void cleanBankDetailsValues(BankDetails bankDetails) {
        // ניקוי רווחים מיותרים ו-null values
        if (bankDetails.getBankName() != null) {
            bankDetails.setBankName(bankDetails.getBankName().trim());
        }
        if (bankDetails.getBranchName() != null) {
            bankDetails.setBranchName(bankDetails.getBranchName().trim());
        }
        if (bankDetails.getLoanerId() != null) {
            // ודא שמספר הזהות מכיל רק ספרות
            String cleanId = bankDetails.getLoanerId().replaceAll("[^0-9]", "");
            bankDetails.setLoanerId(cleanId.length() == 9 ? cleanId : null);
        }
        if (bankDetails.getLoanNumber() != null) {
            // ודא שמספר ההלוואה מכיל רק ספרות
            bankDetails.setLoanNumber(bankDetails.getLoanNumber().replaceAll("[^0-9]", ""));
        }
        if (bankDetails.getLotNumber() != null) {
            bankDetails.setLotNumber(bankDetails.getLotNumber().replaceAll("[^0-9]", ""));
        }
        if (bankDetails.getMainParcel() != null) {
            bankDetails.setMainParcel(bankDetails.getMainParcel().replaceAll("[^0-9]", ""));
        }
        if (bankDetails.getSubParcel() != null) {
            bankDetails.setSubParcel(bankDetails.getSubParcel().replaceAll("[^0-9]", ""));
        }
    }
}