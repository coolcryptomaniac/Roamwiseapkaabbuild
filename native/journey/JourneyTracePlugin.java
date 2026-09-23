package com.gyanverse.roamwise.journey;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import org.json.JSONArray;
import org.json.JSONObject;

@CapacitorPlugin(
    name = "JourneyTrace",
    permissions = {
        @Permission(alias = "fineLocation", strings = { Manifest.permission.ACCESS_FINE_LOCATION }),
        @Permission(alias = "coarseLocation", strings = { Manifest.permission.ACCESS_COARSE_LOCATION })
    }
)
public class JourneyTracePlugin extends Plugin {
    static final String PREFS = "rw_journey_trace";
    static final String KEY_POINTS = "points";
    static final String KEY_RUNNING = "running";
    static final String KEY_DESTINATION = "destination";

    @PluginMethod
    public void start(PluginCall call) {
        if (hasLocation()) {
            startService(call);
            return;
        }
        requestPermissionForAliases(new String[] { "fineLocation", "coarseLocation" }, call, "locationPermissionCallback");
    }

    @PermissionCallback
    private void locationPermissionCallback(PluginCall call) {
        if (!hasLocation()) {
            call.reject("Location permission was not granted. Journey Trace remains off.");
            return;
        }
        startService(call);
    }

    private boolean hasLocation() {
        return getPermissionState("fineLocation") == PermissionState.GRANTED
            || getPermissionState("coarseLocation") == PermissionState.GRANTED;
    }

    private void startService(PluginCall call) {
        String destination = safe(call.getString("destination", "Journey"));
        long intervalMs = Math.max(10000L, call.getLong("intervalMs", 15000L));
        float minDistance = Math.max(10f, call.getFloat("minDistanceMeters", 20f));

        Intent intent = new Intent(getContext(), JourneyTraceService.class);
        intent.setAction(JourneyTraceService.ACTION_START);
        intent.putExtra("destination", destination);
        intent.putExtra("intervalMs", intervalMs);
        intent.putExtra("minDistanceMeters", minDistance);
        androidx.core.content.ContextCompat.startForegroundService(getContext(), intent);

        JSObject out = statusObject();
        out.put("requested", true);
        call.resolve(out);
    }

    @PluginMethod
    public void stop(PluginCall call) {
        Intent intent = new Intent(getContext(), JourneyTraceService.class);
        intent.setAction(JourneyTraceService.ACTION_STOP);
        getContext().startService(intent);
        JSObject out = statusObject();
        out.put("requestedStop", true);
        call.resolve(out);
    }

    @PluginMethod
    public void status(PluginCall call) {
        call.resolve(statusObject());
    }

    @PluginMethod
    public void getBufferedPoints(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSArray points = new JSArray();
        try {
            JSONArray stored = new JSONArray(prefs.getString(KEY_POINTS, "[]"));
            for (int i = 0; i < stored.length(); i++) {
                JSONObject p = stored.optJSONObject(i);
                if (p == null) continue;
                JSObject out = new JSObject();
                out.put("lat", p.optDouble("lat"));
                out.put("lon", p.optDouble("lon"));
                out.put("accuracy", p.optDouble("accuracy"));
                if (!p.isNull("altitude")) out.put("altitude", p.optDouble("altitude"));
                if (!p.isNull("altitudeAccuracy")) out.put("altitudeAccuracy", p.optDouble("altitudeAccuracy"));
                if (!p.isNull("speed")) out.put("speed", p.optDouble("speed"));
                out.put("at", p.optLong("at"));
                points.put(out);
            }
        } catch (Exception ignored) {}
        JSObject result = statusObject();
        result.put("points", points);
        call.resolve(result);
    }

    @PluginMethod
    public void clearBufferedPoints(PluginCall call) {
        getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_POINTS, "[]").apply();
        call.resolve();
    }

    private JSObject statusObject() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSObject out = new JSObject();
        out.put("running", prefs.getBoolean(KEY_RUNNING, false));
        out.put("destination", prefs.getString(KEY_DESTINATION, ""));
        out.put("mode", "foreground-service");
        out.put("privacy", "device-local-buffer");
        return out;
    }

    private String safe(String value) {
        String v = value == null ? "Journey" : value.trim();
        if (v.length() > 80) v = v.substring(0, 80);
        return v.isEmpty() ? "Journey" : v;
    }
}
