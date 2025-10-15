// app/src/main/java/com/example/finalprojectappraisal/utils/MapIntentUtils.java
package com.example.finalprojectappraisal.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MapIntentUtils {

    // ===========================================
    // 1. Google Maps (מתודה קיימת, מעודכנת)
    // ===========================================
    public static void openAddressInMaps(Context context, String address) {
        if (address == null || address.trim().isEmpty()) {
            Toast.makeText(context, "כתובת ריקה, לא ניתן לנווט.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // מקודד את הכתובת ל-URL
            String mapQuery = URLEncoder.encode(address, StandardCharsets.UTF_8.name());
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + mapQuery);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

            // מנסה לפתוח ישירות את אפליקציית Google Maps
            mapIntent.setPackage("com.google.android.apps.maps");
            mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            } else {
                Toast.makeText(context, "אפליקציית גוגל מפות אינה מותקנת.", Toast.LENGTH_SHORT).show();
                // אם גוגל מפות לא מותקנת: ניתן לבצע כאן פולבק לדפדפן אם רוצים, אבל כרגע הדיאלוג מטפל בבחירה
            }
        } catch (Exception e) {
            Toast.makeText(context, "שגיאה בפתיחת גוגל מפות.", Toast.LENGTH_LONG).show();
        }
    }

    // ===========================================
    // 2. Waze (מתודה חדשה)
    // ===========================================
    public static void openAddressInWaze(Context context, String address) {
        if (address == null || address.trim().isEmpty()) {
            Toast.makeText(context, "כתובת ריקה, לא ניתן לנווט.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Waze משתמש ב-URL scheme. הכתובת מוצפנת ב-URL
            String wazeUri = "waze://?q=" + URLEncoder.encode(address, StandardCharsets.UTF_8.name()) + "&navigate=yes";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(wazeUri));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "אפליקציית Waze אינה מותקנת.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "שגיאה בפתיחת Waze.", Toast.LENGTH_LONG).show();
        }
    }

    // ===========================================
    // 3. Govmap (מתודה חדשה)
    // ===========================================
    public static void openAddressInGovmap(Context context, String address) {
        if (address == null || address.trim().isEmpty()) {
            Toast.makeText(context, "כתובת ריקה, לא ניתן לפתוח.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // פתיחת מפת הממשלה (Govmap) בדפדפן
            String govmapUrl = "https://www.govmap.gov.il/?q=" + URLEncoder.encode(address, StandardCharsets.UTF_8.name());
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(govmapUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "שגיאה בפתיחת Govmap.", Toast.LENGTH_LONG).show();
        }
    }
}