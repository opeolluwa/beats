package com.beats.app.hooks;



import android.app.Activity;
import android.os.Build;
import android.widget.Toast;

public class DefaultFailureListener implements APManager.OnFailureListener {
    public static final int REQUEST_CODE_WRITE_SETTINGS=12;
    private final Activity activity;

    public DefaultFailureListener(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onFailure(int failureCode,Exception e) {
        APManager.Utils utils = APManager.getApManager(activity).getUtils();
        switch (failureCode) {
            case APManager.ERROR_DISABLE_HOTSPOT:
                Toast.makeText(activity, "DISABLE HOTSPOT", Toast.LENGTH_LONG).show();
                activity.startActivity(utils.getTetheringSettingIntent());
                break;
            case APManager.ERROR_DISABLE_WIFI:
                Toast.makeText(activity, "DISCONNECT WIFI", Toast.LENGTH_LONG).show();
                utils.askForDisableWifi(activity);
                break;
            case APManager.ERROR_GPS_PROVIDER_DISABLED:
                Toast.makeText(activity, "ENABLE GPS", Toast.LENGTH_LONG).show();
                utils.askForGpsProvider(activity);
                break;
            case APManager.ERROR_LOCATION_PERMISSION_DENIED:
                Toast.makeText(activity, "ALLOW LOCATION PERMISSION", Toast.LENGTH_LONG).show();
                utils.askLocationPermission(activity, REQUEST_CODE_WRITE_SETTINGS);
                break;
            case APManager.ERROR_WRITE_SETTINGS_PERMISSION_REQUIRED:
                Toast.makeText(activity, "ALLOW WRITE SYSTEM SETTINGS PERMISSION", Toast.LENGTH_LONG).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    utils.askWriteSettingPermission(activity);
                }
                break;
        }
    }
}