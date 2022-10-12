package br.com.asfdev.chainway.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionsChecker {

    private final String[] permissions = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN};
    private final Activity activity;

    public PermissionsChecker(Activity activity) {
        this.activity = activity;
    }

    public void call() {
        List<String> permissionsQueue = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), permission) == PackageManager.PERMISSION_DENIED) {
                permissionsQueue.add(permission);
            }
        }
        if(!permissionsQueue.isEmpty()){
            ActivityCompat.requestPermissions(activity, permissionsQueue.toArray(new String[0]), 1);
        }

    }
}