package com.filesync.app.hooks;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;


public class APManager {
    private static APManager apManager;
    private final Utils utils;

    private APManager(Context context) {
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.utils = new Utils();
    }

    /**
     * @param context should not be null
     * @return APManager
     */
    public static APManager getApManager(@NonNull Context context) {
        if (apManager == null) {
            apManager = new APManager(context);
        }
        return apManager;
    }

    private String ssid;
    private String password;

    /**
     * get ssid of recently created hotspot
     * @return SSID
     */
    public String getSSID() {
        return ssid;
    }

    /**
     * get password of recently created hotspot
     * @return PASSWORD
     */
    public String getPassword() {
        return password;
    }

    /**
     * Some android version requires gps provider to be in active mode to create access point (Hotspot).
     */
    public static final int ERROR_GPS_PROVIDER_DISABLED = 0;
    public static final int ERROR_LOCATION_PERMISSION_DENIED = 4;
    public static final int ERROR_DISABLE_HOTSPOT = 1;
    public static final int ERROR_DISABLE_WIFI = 5;
    public static final int ERROR_WRITE_SETTINGS_PERMISSION_REQUIRED = 6;
    public static final int ERROR_UNKNOWN = 3;

    private final WifiManager wifiManager;
    private final LocationManager locationManager;
    private WifiManager.LocalOnlyHotspotReservation reservation;


    public Utils getUtils() {
        return utils;
    }

    public void turnOnHotspot(Context context, OnSuccessListener onSuccessListener, OnFailureListener onFailureListener) {
        boolean providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (isDeviceConnectedToWifi()) {
            onFailureListener.onFailure(ERROR_DISABLE_WIFI,null);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (utils.checkLocationPermission(context) && providerEnabled && !isWifiApEnabled()) {
                try {
                    wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                        public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                            super.onStarted(reservation);
                            APManager.this.reservation = reservation;
                            try {
                                ssid = reservation.getWifiConfiguration().SSID;
                                password = reservation.getWifiConfiguration().preSharedKey;
                                onSuccessListener.onSuccess(ssid, password);
                            } catch (Exception e) {
                                e.printStackTrace();
                                onFailureListener.onFailure(ERROR_UNKNOWN,e);
                            }
                        }

                        public void onFailed(int reason) {
                            super.onFailed(reason);
                            onFailureListener.onFailure(reason == ERROR_TETHERING_DISALLOWED ? ERROR_DISABLE_HOTSPOT : ERROR_UNKNOWN,null);
                        }

                    }, new Handler(Looper.getMainLooper()));
                } catch (Exception e) {
                    onFailureListener.onFailure(ERROR_UNKNOWN,e);
                }
            } else if (!providerEnabled) {
                onFailureListener.onFailure(ERROR_GPS_PROVIDER_DISABLED,null);
            } else if (isWifiApEnabled()) {
                onFailureListener.onFailure(ERROR_DISABLE_HOTSPOT,null);
            } else {
                onFailureListener.onFailure(ERROR_LOCATION_PERMISSION_DENIED,null);
            }
        } else {
            if (!utils.checkLocationPermission(context)) {
                onFailureListener.onFailure(ERROR_LOCATION_PERMISSION_DENIED, null);
                return;
            }
            if (!utils.checkWriteSettingPermission(context)) {
                onFailureListener.onFailure(ERROR_WRITE_SETTINGS_PERMISSION_REQUIRED,null);
                return;
            }
            try {
                ssid = "AndroidAP_" + new Random().nextInt(10000);
                password = String.valueOf(new Random().nextInt(999999));
                WifiConfiguration wifiConfiguration = new WifiConfiguration();
                wifiConfiguration.SSID = ssid;
                wifiConfiguration.preSharedKey = password;
                wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiManager.setWifiEnabled(false);
                setWifiApEnabled(wifiConfiguration, true);
                onSuccessListener.onSuccess(ssid, password);
            } catch (Exception e) {
                e.printStackTrace();
                onFailureListener.onFailure(ERROR_LOCATION_PERMISSION_DENIED,e);
            }
        }
    }

    public void disableWifiAp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                reservation.close();
            } else {
                setWifiApEnabled(null, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isWifiApEnabled() {
        try {
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            return (boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Utility method to check device wifi is enabled and connected to any access point.
     *
     * @return connection status of wifi
     */
    public boolean isDeviceConnectedToWifi() {
        return wifiManager.getDhcpInfo().ipAddress != 0;
    }

    private void setWifiApEnabled(WifiConfiguration wifiConfiguration, boolean enable) throws Exception {
        Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
        method.invoke(wifiManager, wifiConfiguration, enable);
    }

    public WifiManager getWifiManager() {
        return wifiManager;
    }

    public interface OnFailureListener {
        void onFailure(int failureCode,@Nullable Exception e);
    }

    public interface OnSuccessListener {
        void onSuccess(@NonNull String ssid,@NonNull String password);
    }

    private String getRandomPassword() {
        try {
            MessageDigest ms = MessageDigest.getInstance("MD5");
            byte[] bytes = new byte[10];
            new Random().nextBytes(bytes);
            byte[] digest = ms.digest(bytes);
            BigInteger bigInteger = new BigInteger(1, digest);
            return bigInteger.toString(16).substring(0, 6);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "jfs82433#$2";
    }

    public static class Utils {
        public boolean checkLocationPermission(Context context) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        public void askLocationPermission(Activity activity, int requestCode) {
            ActivityCompat.requestPermissions(activity, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, requestCode);
        }

        @RequiresApi(Build.VERSION_CODES.M)
        public void askWriteSettingPermission(@NonNull Activity activity) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        }

        @RequiresApi(Build.VERSION_CODES.M)
        public boolean checkWriteSettingPermission(@NonNull Context context) {
            return Settings.System.canWrite(context);
        }

        public Intent getTetheringSettingIntent() {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
            return intent;
        }

        public void askForGpsProvider(Activity activity) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivity(intent);
        }

        public void askForDisableWifi(Activity activity) {
            activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
    }
}