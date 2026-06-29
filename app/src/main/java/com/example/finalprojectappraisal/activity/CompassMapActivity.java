package com.example.finalprojectappraisal.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.utils.ItmConverter;
import com.example.finalprojectappraisal.views.CompassView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;

public class CompassMapActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "CompassMapActivity";
    private static final int    REQUEST_LOCATION = 1001;

    private WebView       webViewGovmap;
    private CompassView   compassView;
    private LinearLayout  layoutLoading;

    private SensorManager sensorManager;
    private Sensor        rotationSensor;
    private Sensor        magneticSensor;
    private Sensor        accelerometerSensor;

    private final float[] gravity    = new float[3];
    private final float[] geomagnetic= new float[3];
    private boolean hasGravity    = false;
    private boolean hasMagnetic   = false;

    private float currentAzimuth = 0f;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass_map);

        webViewGovmap  = findViewById(R.id.webViewGovmap);
        compassView    = findViewById(R.id.compassView);
        layoutLoading  = findViewById(R.id.layoutLoading);

        MaterialButton btnBack = findViewById(R.id.btnBackCompass);
        btnBack.setOnClickListener(v -> finish());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupSensors();
        fetchLocationAndLoadMap();
    }

    // ────────────────────────────────────────────────────────────────
    // Location → load map
    // ────────────────────────────────────────────────────────────────

    private void fetchLocationAndLoadMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            return;
        }
        doFetchLocation();
    }

    @SuppressLint("MissingPermission")
    private void doFetchLocation() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        Log.d(TAG, "Got location: " + location.getLatitude() + ", " + location.getLongitude());
                        loadGovmap(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.w(TAG, "Location is null, trying last known");
                        fusedLocationClient.getLastLocation()
                                .addOnSuccessListener(this, last -> {
                                    if (last != null) {
                                        loadGovmap(last.getLatitude(), last.getLongitude());
                                    } else {
                                        Toast.makeText(this, "לא נמצא מיקום GPS, מציג מרכז ישראל", Toast.LENGTH_LONG).show();
                                        loadGovmap(31.7683, 35.2137); // Jerusalem default
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Location fetch failed", e);
                    Toast.makeText(this, "שגיאה בקבלת מיקום", Toast.LENGTH_SHORT).show();
                    loadGovmap(31.7683, 35.2137);
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            doFetchLocation();
        } else {
            Toast.makeText(this, "נדרשת הרשאת מיקום", Toast.LENGTH_LONG).show();
            loadGovmap(31.7683, 35.2137);
        }
    }

    // ────────────────────────────────────────────────────────────────
    // WebView — GOVMAP
    // ────────────────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private void loadGovmap(double lat, double lon) {
        double[] itm  = ItmConverter.wgs84ToItm(lat, lon);
        long    itmX  = Math.round(itm[0]);
        long    itmY  = Math.round(itm[1]);

        // URL method (no token required) — center on current ITM coordinates.
        // c = x,y (New Israeli Grid), z = zoom level, b = background type.
        String url = "https://www.govmap.gov.il/?c=" + itmX + "," + itmY + "&z=8&b=0";

        // Allow inspecting the WebView via chrome://inspect for debugging
        WebView.setWebContentsDebuggingEnabled(true);

        WebSettings settings = webViewGovmap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // GOVMAP refuses to render inside an Android WebView (its UA contains "; wv").
        // Present a standard mobile Chrome User-Agent so it loads like a normal browser.
        settings.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

        Log.d(TAG, "Loading GOVMAP URL: " + url);

        webViewGovmap.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                return false; // keep all navigation inside WebView
            }

            @Override
            public void onPageFinished(WebView view, String pageUrl) {
                Log.d(TAG, "onPageFinished: " + pageUrl);
                layoutLoading.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.e(TAG, "WebView error: " + error.getDescription()
                        + " code=" + error.getErrorCode()
                        + " url=" + request.getUrl());
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                Log.e(TAG, "WebView HTTP error: " + errorResponse.getStatusCode()
                        + " url=" + request.getUrl());
            }
        });

        webViewGovmap.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d(TAG, "GOVMAP console: " + cm.message()
                        + " @ " + cm.sourceId() + ":" + cm.lineNumber());
                return true;
            }
        });

        webViewGovmap.loadUrl(url);
    }

    // ────────────────────────────────────────────────────────────────
    // Compass – SensorManager
    // ────────────────────────────────────────────────────────────────

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        rotationSensor      = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        magneticSensor      = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            // Fallback to accelerometer + magnetic field
            if (accelerometerSensor != null)
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
            if (magneticSensor != null)
                sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {

            case Sensor.TYPE_ROTATION_VECTOR: {
                float[] rotMatrix = new float[9];
                SensorManager.getRotationMatrixFromVector(rotMatrix, event.values);
                float[] orientation = new float[3];
                SensorManager.getOrientation(rotMatrix, orientation);
                float azimuthRad = orientation[0];
                float azimuthDeg = (float) Math.toDegrees(azimuthRad);
                if (azimuthDeg < 0) azimuthDeg += 360f;
                smoothAndSet(azimuthDeg);
                break;
            }

            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, gravity, 0, 3);
                hasGravity = true;
                computeFromAccelMag();
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, geomagnetic, 0, 3);
                hasMagnetic = true;
                computeFromAccelMag();
                break;
        }
    }

    private void computeFromAccelMag() {
        if (!hasGravity || !hasMagnetic) return;
        float[] R = new float[9];
        float[] I = new float[9];
        if (!SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) return;
        float[] orientation = new float[3];
        SensorManager.getOrientation(R, orientation);
        float deg = (float) Math.toDegrees(orientation[0]);
        if (deg < 0) deg += 360f;
        smoothAndSet(deg);
    }

    private void smoothAndSet(float newAzimuth) {
        // Low-pass filter for smooth rotation
        float diff = newAzimuth - currentAzimuth;
        if (diff > 180f)  diff -= 360f;
        if (diff < -180f) diff += 360f;
        currentAzimuth += diff * 0.15f;
        if (currentAzimuth < 0) currentAzimuth += 360f;
        if (currentAzimuth >= 360f) currentAzimuth -= 360f;
        compassView.setAzimuth(currentAzimuth);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* no-op */ }

    @Override
    public void onBackPressed() {
        if (webViewGovmap != null && webViewGovmap.canGoBack()) {
            webViewGovmap.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
