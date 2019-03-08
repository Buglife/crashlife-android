package com.buglife.crashlife.android.example;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.buglife.crashlife.sdk.Crashlife;
import com.buglife.crashlife.sdk.DebugMenuActivity;

import java.io.File;
import java.util.HashMap;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {
    boolean hasStarted = false;
    LocationManager mLocationManager;
    LocationListener mLocationListener;

    private native void crashMe();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView introTextView = findViewById(R.id.intro_text_view);
        introTextView.setText(getIntroText());

        Crashlife.leaveFootprint("onCreate happened");
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("meta", "data");
        Crashlife.leaveFootprint("onCreate happened with metadata", metadata);
        Crashlife.leaveFootprint("More metadata", metadata);
    }

    public void showDebugMenuButtonTapped(View view) {
        Intent intent = new Intent(this, DebugMenuActivity.class);
        startActivity(intent);
    }

    public void reportCaughtExceptionButtonTapped(View view) {
        Crashlife.logException(new RuntimeException("This is a test"));
    }

    private String getIntroText() {
        return "Tap the button below to report a bug.";
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
}
