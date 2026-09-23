package com.gyanverse.roamwise.journey;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import org.json.JSONArray;
import org.json.JSONObject;

public class JourneyTraceService extends Service {
    public static final String ACTION_START = "com.gyanverse.roamwise.journey.START";
    public static final String ACTION_STOP = "com.gyanverse.roamwise.journey.STOP";
    private static final String CHANNEL_ID = "rw_journey_trace";
    private static final int NOTIFICATION_ID = 2609;
    private static final int MAX_POINTS = 2400;

    private FusedLocationProviderClient locations;
    private LocationCallback callback;
    private boolean receiving = false;

    @Override
    public void onCreate() {
        super.onCreate();
        locations = LocationServices.getFusedLocationProviderClient(this);
        createChannel();
        callback = new LocationCallback() {
            @Override public void onLocationResult(LocationResult result) {
                if (result == null) return;
                for (Location location : result.getLocations()) persist(location);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopTracking();
            stopSelf();
            return START_NOT_STICKY;
        }

        String destination = intent == null ? "Journey" : intent.getStringExtra("destination");
        long intervalMs = intent == null ? 15000L : Math.max(10000L, intent.getLongExtra("intervalMs", 15000L));
        float minDistance = intent == null ? 20f : Math.max(10f, intent.getFloatExtra("minDistanceMeters", 20f));
        setState(true, destination);
        startForeground(NOTIFICATION_ID, notification(destination));
        startTracking(intervalMs, minDistance);
        return START_NOT_STICKY;
    }

    private void startTracking(long intervalMs, float minDistance) {
        if (receiving) return;
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            setState(false, null);
            stopSelf();
            return;
        }

        LocationRequest request = new LocationRequest.Builder(
            fine ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            intervalMs
        ).setMinUpdateIntervalMillis(Math.max(8000L, intervalMs / 2))
         .setMinUpdateDistanceMeters(minDistance)
         .build();
        try {
            locations.requestLocationUpdates(request, callback, Looper.getMainLooper());
            receiving = true;
        } catch (SecurityException denied) {
            setState(false, null);
            stopSelf();
        }
    }

    private void stopTracking() {
        if (receiving) {
            locations.removeLocationUpdates(callback);
            receiving = false;
        }
        setState(false, null);
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    private void persist(Location location) {
        if (location == null || location.getAccuracy() > 120f) return;
        SharedPreferences prefs = getSharedPreferences(JourneyTracePlugin.PREFS, Context.MODE_PRIVATE);
        try {
            JSONArray old = new JSONArray(prefs.getString(JourneyTracePlugin.KEY_POINTS, "[]"));
            JSONObject p = new JSONObject();
            p.put("lat", location.getLatitude());
            p.put("lon", location.getLongitude());
            p.put("accuracy", location.getAccuracy());
            p.put("at", location.getTime() > 0 ? location.getTime() : System.currentTimeMillis());
            if (location.hasAltitude()) p.put("altitude", location.getAltitude()); else p.put("altitude", JSONObject.NULL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasVerticalAccuracy()) p.put("altitudeAccuracy", location.getVerticalAccuracyMeters()); else p.put("altitudeAccuracy", JSONObject.NULL);
            if (location.hasSpeed()) p.put("speed", location.getSpeed()); else p.put("speed", JSONObject.NULL);
            old.put(p);

            JSONArray trimmed = old;
            if (old.length() > MAX_POINTS) {
                trimmed = new JSONArray();
                for (int i = old.length() - MAX_POINTS; i < old.length(); i++) trimmed.put(old.get(i));
            }
            prefs.edit().putString(JourneyTracePlugin.KEY_POINTS, trimmed.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void setState(boolean running, String destination) {
        SharedPreferences.Editor edit = getSharedPreferences(JourneyTracePlugin.PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(JourneyTracePlugin.KEY_RUNNING, running);
        if (destination != null) edit.putString(JourneyTracePlugin.KEY_DESTINATION, destination);
        edit.apply();
    }

    private Notification notification(String destination) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pending = PendingIntent.getActivity(
            this, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        String label = destination == null || destination.trim().isEmpty() ? "your journey" : destination.trim();
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("RoamWise Journey Trace is active")
            .setContentText("Recording " + label + " · tap RoamWise to pause or finish")
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID, "Journey Trace", NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Visible while you explicitly record a RoamWise journey.");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        if (receiving) locations.removeLocationUpdates(callback);
        receiving = false;
        setState(false, null);
        super.onDestroy();
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) { return null; }
}
