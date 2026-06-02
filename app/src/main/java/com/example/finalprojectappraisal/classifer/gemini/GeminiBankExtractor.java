package com.example.finalprojectappraisal.classifer.gemini;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.finalprojectappraisal.model.BankDetails;
import com.google.firebase.vertexai.FirebaseVertexAI;
import com.google.firebase.vertexai.java.GenerativeModelFutures;
import com.google.firebase.vertexai.type.Content;
import com.google.firebase.vertexai.type.GenerateContentResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiBankExtractor {
    private static final String TAG = "GeminiBankExtractor";
    private static final Executor executor = Executors.newSingleThreadExecutor();

    public interface BankDataExtractionCallback {
        void onSuccess(BankDetails bankDetails);
        void onError(String error);
    }

    /**
     * חילוץ נתונים מקובץ PDF של פרטי הבנק (שליחת הקובץ ישירות)
     */
    public static void extractBankDetailsFromPDF(Context context, Uri pdfUri, BankDataExtractionCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting bank details extraction from PDF Uri: " + pdfUri);

                // יצירת prompt מפורט לחילוץ נתונים
                String prompt = createBankDataExtractionPrompt();

                // שליחה ל-Gemini עם ה-Uri של הקובץ
                extractDataWithGemini(context, pdfUri, prompt, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error extracting bank details", e);
                callback.onError("שגיאה בחילוץ נתוני הבנק: " + e.getMessage());
            }
        });
    }

    /**
     * יצירת prompt לחילוץ נתוני הבנק - מתוקן לפי הדרישות
     */
    private static String createBankDataExtractionPrompt() {
        return "אתה מומחה לחילוץ נתונים ממסמכים בנקיים בעברית. אנא חלץ את הנתונים הבאים ממסמך הבנק והחזר תשובה בפורמט JSON בלבד:\n" +
                "\n" +
                "{\n" +
                "    \"bank_name\": \"שם הבנק בעברית\",\n" +
                "    \"branch_name\": \"שם הסניף בעברית\",\n" +
                "    \"branch_email\": \"כתובת אימייל של הסניף\",\n" +
                "    \"banker_name\": \"שם הבנקאי בעברית (מופיע בתחתית המסמך האחרון לפני 'בכבוד רב')\",\n" +
                "    \"document_date_gre\": \"תאריך המסמך בפורמט YYYY-MM-DD (התאריך שמופיע בראש המסמך)\",\n" +
                "    \"document_date_he\": \"תאריך המסמך בפורמט DD/MM/YYYY\",\n" +
                "    \"valuation_number\": \"מספר השומה בפורמט יישוב/שנה/מספר (למשל: עפולה/25/10761) - חפש בתחילת המסמך השני\",\n" +
                "    \"loan_number\": \"מס' תיק - המספר הארוך שמופיע ליד 'מס' תיק' (9-10 ספרות)\",\n" +
                "    \"type_of_loan\": \"סוג ההלוואה (מוכוונת/לא מוכוונת) - מופיע ליד 'סוג ההלוואה:'\",\n" +
                "    \"page1_header\": \"כותרת עמוד 1 (נספח ל...)\",\n" +
                "    \"lot_number\": \"מספר הגוש (מספרים בלבד) - מופיע בטבלה תחת 'גוש'\",\n" +
                "    \"main_parcel\": \"חלקה ראשית (מספרים בלבד) - מופיע בטבלה תחת 'חלקה'\",\n" +
                "    \"sub_parcel\": \"תת חלקה (מספרים בלבד) - מופיע בטבלה תחת 'תת חלקה'\",\n" +
                "    \"short_address\": \"כתובת מלאה (רחוב מספר, יישוב)\",\n" +
                "    \"loaner_name\": \"שם הלווה בעברית - מופיע ליד 'שם הלווה:'\",\n" +
                "    \"loaner_id\": \"מס' זהות הלווה (9 ספרות) - מופיע ליד 'מס' זהות:'\",\n" +
                "    \"purpose_of_loan\": \"מטרת השמאות (למשל: רכישת נכס) - מופיע ליד 'מטרת השמאות:'\",\n" +
                "    \"identity_of_customer\": \"שם הלווה כפי שמופיע בתחילת המסמך השני, ליד 'לכבוד' או 'שם הלוואה:'\",\n" +
                "    \"appraisal_final_date\": \"המועד הקצוב לשמאות בפורמט DD/MM/YYYY - חפש במסמך השני את השדה 'המועד הקצוב לשמאות:'\"\n" +
                "}\n" +
                "\n" +
                "הנחיות קריטיות:\n" +
                "1. החזר רק JSON תקין ללא כל טקסט נוסף, הסברים, או markdown\n" +
                "2. אם שדה לא קיים או לא ברור במסמך, השתמש ב-null\n" +
                "3. valuation_number - זה המספר שמופיע בפורמט יישוב/שנה/מספר (למשל: עפולה/25/10761)\n" +
                "4. loan_number - זה מס' תיק (המספר הארוך, לא מס' הלוואה שהוא 1)\n" +
                "5. identity_of_customer - שם הלווה מהמסמך השני (לא שמות הדיירים)\n" +
                "6. appraisal_final_date - המועד הקצוב לשמאות מהמסמך השני\n" +
                "7. תאריכים בפורמט DD/MM/YYYY עם אפסים מובילים (למשל: 11/09/2025)\n" +
                "8. תאריכים בפורמט YYYY-MM-DD עם מקפים (למשל: 2025-09-11)\n" +
                "9. מספרי זהות חייבים להיות בדיוק 9 ספרות\n" +
                "10. שדות מספריים (lot_number, main_parcel, sub_parcel) - רק מספרים\n" +
                "11. המסמך מכיל מספר עמודים - חפש מידע גם בעמוד השני\n";
    }

    /**
     * המרת PDF Uri לבייטים
     */
    private static byte[] readPdfBytes(Context context, Uri pdfUri) throws Exception {
        try (InputStream inputStream = context.getContentResolver().openInputStream(pdfUri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            if (inputStream == null) {
                throw new Exception("לא ניתן לפתוח את הקובץ");
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toByteArray();
        }
    }

    /**
     * שליחה ל-Gemini וחילוץ הנתונים מה-PDF
     */
    private static void extractDataWithGemini(Context context, Uri pdfUri, String prompt, BankDataExtractionCallback callback) {
        try {
            Log.d(TAG, "Reading PDF file bytes");

            // קריאת הקובץ כבייטים
            byte[] pdfBytes = readPdfBytes(context, pdfUri);

            Log.d(TAG, "PDF file size: " + pdfBytes.length + " bytes");
            Log.d(TAG, "Sending PDF file to Gemini for bank data extraction");

            // יצירת מודל Gemini
            GenerativeModelFutures generativeModel = GenerativeModelFutures.from(
                    FirebaseVertexAI.getInstance().generativeModel("gemini-2.0-flash")
            );

            // יצירת Content עם הבייטים של ה-PDF והפרומפט
            Content content = new Content.Builder()
                    .addInlineData(pdfBytes, "application/pdf")
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
     * ניקוי ערכים ב-BankDetails - מבוטל זמנית לצורך דיבוג
     */
    private static void cleanBankDetailsValues(BankDetails bankDetails) {
        // לא מבצעים שום ניקוי או ולידציה
        // כל הנתונים עוברים כמו שהם מ-Gemini
        Log.d(TAG, "Skipping validation - passing raw data from Gemini");
    }
}