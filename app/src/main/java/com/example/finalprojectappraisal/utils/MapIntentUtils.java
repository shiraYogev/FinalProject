// app/src/main/java/com/example/finalprojectappraisal/utils/MapIntentUtils.java
package com.example.finalprojectappraisal.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class MapIntentUtils {

    public static void openAddressInMaps(Context context, String address) {
        if (address == null) address = "";
        address = address.trim();
        if (address.isEmpty()) {
            Toast.makeText(context, "לא נמצאה כתובת לפרויקט", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) Try Google Maps app (geo:)
        try {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
            Intent mapsApp = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapsApp.setPackage("com.google.android.apps.maps");
            mapsApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mapsApp);
            return;
        } catch (Exception ignore) { }

        // 2) Try any maps-capable app (geo:)
        try {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
            Intent anyMap = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            anyMap.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(anyMap);
            return;
        } catch (Exception ignore) { }

        // 3) Fallback to web (Google Maps web URL)
        try {
            Uri web = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(address));
            Intent webIntent = new Intent(Intent.ACTION_VIEW, web);
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(webIntent);
            return;
        } catch (Exception ignore) { }

        Toast.makeText(context, "אין אפליקציה מתאימה לפתיחת מפה", Toast.LENGTH_SHORT).show();
    }
}
