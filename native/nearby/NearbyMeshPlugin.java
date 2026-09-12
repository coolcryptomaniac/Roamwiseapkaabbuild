package com.gyanverse.roamwise.nearby;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.annotation.NonNull;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.common.api.ApiException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@CapacitorPlugin(
    name = "NearbyMesh",
    permissions = {
        @Permission(alias = "coarseLocation", strings = { Manifest.permission.ACCESS_COARSE_LOCATION }),
        @Permission(alias = "fineLocation", strings = { Manifest.permission.ACCESS_FINE_LOCATION }),
        @Permission(alias = "bluetoothNearby", strings = {
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        }),
        @Permission(alias = "nearbyWifi", strings = { Manifest.permission.NEARBY_WIFI_DEVICES })
    }
)
public class NearbyMeshPlugin extends Plugin {
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    private static final int MAX_MESSAGE_BYTES = 16 * 1024;
    private static final long[] RECONNECT_DELAYS_MS = { 1000L, 3000L, 7000L, 15000L, 30000L };
    private final Set<String> connected = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> pending = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> requesting = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> discovered = Collections.synchronizedSet(new HashSet<>());
    // Session-only trust: an endpoint enters this set only after the user
    // compares digits and accepts it. It is never persisted across stop/app exit.
    private final Set<String> trustedSession = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> manuallyDisconnected = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, Integer> reconnectAttempts = Collections.synchronizedMap(new HashMap<>());
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private ConnectionsClient client;
    private boolean running;
    private String localDisplayName = "RoamWise trekker";

    private ConnectionsClient client() {
        if (client == null) client = Nearby.getConnectionsClient(getContext());
        return client;
    }

    @PluginMethod
    public void requestMeshPermissions(PluginCall call) {
        // Android remembers grants. Resolve immediately after the first approval so
        // later Trail Mesh sessions never manufacture another permission prompt.
        if (hasRequiredPermission()) {
            call.resolve(permissionResult());
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionForAliases(new String[] { "bluetoothNearby", "nearbyWifi" }, call, "permissionCallback");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            requestPermissionForAliases(new String[] { "bluetoothNearby" }, call, "permissionCallback");
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
            requestPermissionForAliases(new String[] { "bluetoothNearby", "fineLocation" }, call, "permissionCallback");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissionForAlias("fineLocation", call, "permissionCallback");
        } else {
            requestPermissionForAlias("coarseLocation", call, "permissionCallback");
        }
    }

    @com.getcapacitor.annotation.PermissionCallback
    private void permissionCallback(PluginCall call) {
        JSObject result = permissionResult();
        if (Boolean.TRUE.equals(result.getBool("granted"))) call.resolve(result);
        else call.reject("Nearby permission was not granted; mesh remains off.");
    }

    @PluginMethod
    public void start(PluginCall call) {
        if (!hasRequiredPermission()) {
            call.reject("Nearby permissions are required. Call requestMeshPermissions after explaining why.");
            return;
        }
        if (running) {
            call.resolve(status());
            return;
        }
        String displayName = safeName(call.getString("displayName", "RoamWise trekker"));
        localDisplayName = displayName;
        manuallyDisconnected.clear();
        AdvertisingOptions advertising = new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        DiscoveryOptions discovery = new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        client().startAdvertising(displayName, getContext().getPackageName(), lifecycle, advertising)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return client().startDiscovery(getContext().getPackageName(), discoveryCallback, discovery);
            })
            .addOnSuccessListener(ignored -> {
                running = true;
                notifyListeners("meshState", status(), true);
                call.resolve(status());
            })
            .addOnFailureListener(error -> {
                client().stopAdvertising();
                int code = error instanceof ApiException ? ((ApiException) error).getStatusCode() : -1;
                String reason = code >= 0 ? ConnectionsStatusCodes.getStatusCodeString(code) : error.getClass().getSimpleName();
                call.reject("Could not start Nearby mesh [" + code + " " + reason + "]: " + error.getMessage(), null, error);
            });
    }

    @PluginMethod
    public void acceptConnection(PluginCall call) {
        String endpointId = call.getString("endpointId");
        if (!validEndpoint(endpointId) || !pending.remove(endpointId)) {
            call.reject("Unknown or expired endpoint.");
            return;
        }
        client().acceptConnection(endpointId, payloadCallback)
            .addOnSuccessListener(ignored -> call.resolve())
            .addOnFailureListener(error -> call.reject("Connection acceptance failed.", null, error));
    }

    @PluginMethod
    public void rejectConnection(PluginCall call) {
        String endpointId = call.getString("endpointId");
        if (!validEndpoint(endpointId)) { call.reject("Invalid endpoint."); return; }
        pending.remove(endpointId);
        client().rejectConnection(endpointId).addOnCompleteListener(ignored -> call.resolve());
    }

    @PluginMethod
    public void sendMessage(PluginCall call) {
        Object value = call.getData().opt("message");
        String body = value instanceof String ? (String) value : String.valueOf(value);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0 || bytes.length > MAX_MESSAGE_BYTES) {
            call.reject("Messages must be between 1 byte and 16 KiB.");
            return;
        }
        String endpointId = call.getString("endpointId");
        ArrayList<String> targets = new ArrayList<>();
        if (endpointId != null) {
            if (!connected.contains(endpointId)) { call.reject("Endpoint is not connected."); return; }
            targets.add(endpointId);
        } else {
            targets.addAll(connected);
        }
        if (targets.isEmpty()) { call.reject("No connected trekkers."); return; }
        client().sendPayload(targets, Payload.fromBytes(bytes))
            .addOnSuccessListener(ignored -> call.resolve())
            .addOnFailureListener(error -> call.reject("Message send failed.", null, error));
    }

    @PluginMethod
    public void getStatus(PluginCall call) { call.resolve(status()); }

    @PluginMethod
    public void disconnect(PluginCall call) {
        String endpointId = call.getString("endpointId");
        if (!validEndpoint(endpointId)) {
            call.reject("Invalid endpoint.");
            return;
        }
        manuallyDisconnected.add(endpointId);
        trustedSession.remove(endpointId);
        reconnectAttempts.remove(endpointId);
        pending.remove(endpointId);
        requesting.remove(endpointId);
        connected.remove(endpointId);
        client().disconnectFromEndpoint(endpointId);
        JSObject event = new JSObject();
        event.put("endpointId", endpointId);
        event.put("connected", false);
        notifyListeners("connectionChanged", event, true);
        call.resolve(status());
    }

    @PluginMethod
    public void openAppSettings(PluginCall call) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getContext().getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        stopMesh();
        call.resolve(status());
    }

    private void requestEndpoint(String endpointId, boolean reconnecting) {
        if (!validEndpoint(endpointId) || manuallyDisconnected.contains(endpointId)
                || connected.contains(endpointId) || pending.contains(endpointId)
                || !requesting.add(endpointId)) return;
        if (reconnecting) notifyReconnect(endpointId, "requesting", 0L);
        client().requestConnection(localDisplayName, endpointId, lifecycle)
            .addOnFailureListener(error -> {
                requesting.remove(endpointId);
                notifyError("connectionRequestFailed", endpointId);
                scheduleReconnect(endpointId);
            });
    }

    private void scheduleReconnect(String endpointId) {
        if (!running || !trustedSession.contains(endpointId) || manuallyDisconnected.contains(endpointId)) return;
        int attempt = reconnectAttempts.containsKey(endpointId) ? reconnectAttempts.get(endpointId) : 0;
        long delay = RECONNECT_DELAYS_MS[Math.min(attempt, RECONNECT_DELAYS_MS.length - 1)];
        reconnectAttempts.put(endpointId, attempt + 1);
        notifyReconnect(endpointId, discovered.contains(endpointId) ? "waiting" : "outOfRange", delay);
        reconnectHandler.postDelayed(() -> {
            if (!running || manuallyDisconnected.contains(endpointId) || connected.contains(endpointId)) return;
            if (discovered.contains(endpointId)) requestEndpoint(endpointId, true);
        }, delay);
    }

    private void notifyReconnect(String endpointId, String phase, long retryInMs) {
        JSObject event = new JSObject();
        event.put("endpointId", endpointId);
        event.put("connected", false);
        event.put("reconnecting", true);
        event.put("phase", phase);
        event.put("retryInMs", retryInMs);
        notifyListeners("connectionChanged", event, true);
    }

    private final EndpointDiscoveryCallback discoveryCallback = new EndpointDiscoveryCallback() {
        @Override public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
            discovered.add(endpointId);
            JSObject event = new JSObject();
            event.put("endpointId", endpointId);
            event.put("displayName", info.getEndpointName());
            event.put("reconnecting", trustedSession.contains(endpointId) && !manuallyDisconnected.contains(endpointId));
            notifyListeners("peerFound", event, true);
            requestEndpoint(endpointId, trustedSession.contains(endpointId));
        }
        @Override public void onEndpointLost(@NonNull String endpointId) {
            discovered.remove(endpointId);
            JSObject event = new JSObject();
            event.put("endpointId", endpointId);
            event.put("reconnecting", trustedSession.contains(endpointId) && !manuallyDisconnected.contains(endpointId));
            notifyListeners("peerLost", event, true);
        }
    };

    private final ConnectionLifecycleCallback lifecycle = new ConnectionLifecycleCallback() {
        @Override public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo info) {
            requesting.remove(endpointId);
            // A phone verified earlier in this still-running session may rejoin
            // automatically. A stop, app exit or manual removal clears that trust.
            if (trustedSession.contains(endpointId) && !manuallyDisconnected.contains(endpointId)) {
                notifyReconnect(endpointId, "verifyingSessionPeer", 0L);
                client().acceptConnection(endpointId, payloadCallback)
                    .addOnFailureListener(error -> {
                        notifyError("automaticReconnectAcceptanceFailed", endpointId);
                        scheduleReconnect(endpointId);
                    });
                return;
            }
            pending.add(endpointId);
            JSObject event = new JSObject();
            event.put("endpointId", endpointId);
            event.put("displayName", info.getEndpointName());
            event.put("verificationCode", info.getAuthenticationDigits());
            event.put("incoming", info.isIncomingConnection());
            event.put("reconnecting", false);
            notifyListeners("verificationRequired", event, true);
        }
        @Override public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
            requesting.remove(endpointId);
            pending.remove(endpointId);
            boolean ok = result.getStatus().isSuccess();
            if (ok) {
                connected.add(endpointId);
                trustedSession.add(endpointId);
                manuallyDisconnected.remove(endpointId);
                reconnectAttempts.remove(endpointId);
            } else {
                connected.remove(endpointId);
            }
            JSObject event = new JSObject();
            event.put("endpointId", endpointId);
            event.put("connected", ok);
            event.put("reconnecting", !ok && trustedSession.contains(endpointId));
            event.put("statusCode", result.getStatus().getStatusCode());
            notifyListeners("connectionChanged", event, true);
            if (!ok) scheduleReconnect(endpointId);
        }
        @Override public void onDisconnected(@NonNull String endpointId) {
            connected.remove(endpointId);
            JSObject event = new JSObject();
            event.put("endpointId", endpointId);
            event.put("connected", false);
            event.put("reconnecting", trustedSession.contains(endpointId) && !manuallyDisconnected.contains(endpointId));
            notifyListeners("connectionChanged", event, true);
            scheduleReconnect(endpointId);
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            if (payload.getType() != Payload.Type.BYTES) return;
            byte[] bytes = payload.asBytes();
            if (bytes == null || bytes.length > MAX_MESSAGE_BYTES) return;
            JSObject event = new JSObject(); event.put("endpointId", endpointId);
            event.put("message", new String(bytes, StandardCharsets.UTF_8));
            notifyListeners("messageReceived", event, true);
        }
        @Override public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {}
    };

    private boolean hasRequiredPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return getPermissionState("bluetoothNearby") == PermissionState.GRANTED
                && getPermissionState("nearbyWifi") == PermissionState.GRANTED;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            return getPermissionState("bluetoothNearby") == PermissionState.GRANTED;
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
            return getPermissionState("bluetoothNearby") == PermissionState.GRANTED
                && hasFineLocation();
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? hasFineLocation() : hasCoarseLocation();
    }

    private boolean hasCoarseLocation() {
        return getContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasFineLocation() {
        return getContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private JSObject permissionResult() {
        JSObject result = new JSObject();
        result.put("granted", hasRequiredPermission());
        result.put("sdkInt", Build.VERSION.SDK_INT);
        result.put("coarseLocation", getPermissionState("coarseLocation").toString());
        result.put("fineLocation", getPermissionState("fineLocation").toString());
        result.put("bluetoothNearby", getPermissionState("bluetoothNearby").toString());
        result.put("nearbyWifi", getPermissionState("nearbyWifi").toString());
        result.put("coarseLocationGranted", hasCoarseLocation());
        result.put("fineLocationGranted", hasFineLocation());
        // ACCESS_WIFI_STATE and CHANGE_WIFI_STATE are normal (non-runtime)
        // capabilities. Report the actual manifest/runtime result on every API
        // level so the health console can distinguish a stale build from a
        // permission prompt that the user can act on.
        result.put("accessWifiStateDeclared", getContext().checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED);
        result.put("changeWifiStateDeclared", getContext().checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private JSObject status() {
        JSObject result = permissionResult();
        result.put("running", running);
        result.put("connectedCount", connected.size());
        result.put("pendingCount", pending.size());
        result.put("reconnectingCount", Math.max(0, trustedSession.size() - connected.size()));
        result.put("autoReconnectScope", "verified-current-session");
        result.put("topology", "P2P_CLUSTER");
        result.put("multiHopRelay", false);
        result.put("endpoints", new JSArray(new ArrayList<>(connected)));
        return result;
    }

    private void stopMesh() {
        if (client != null) {
            client.stopAdvertising(); client.stopDiscovery(); client.stopAllEndpoints();
        }
        running = false;
        reconnectHandler.removeCallbacksAndMessages(null);
        connected.clear(); pending.clear(); requesting.clear(); discovered.clear();
        trustedSession.clear(); manuallyDisconnected.clear(); reconnectAttempts.clear();
        notifyListeners("meshState", status(), true);
    }

    private void notifyError(String type, String endpointId) {
        JSObject event = new JSObject(); event.put("type", type); event.put("endpointId", endpointId);
        notifyListeners("meshError", event, true);
    }

    private boolean validEndpoint(String value) { return value != null && value.matches("[A-Za-z0-9_-]{1,64}"); }
    private String safeName(String value) {
        String clean = value == null ? "RoamWise trekker" : value.replaceAll("[\\p{Cntrl}]", "").trim();
        return clean.isEmpty() ? "RoamWise trekker" : clean.substring(0, Math.min(clean.length(), 32));
    }

    @Override protected void handleOnDestroy() { stopMesh(); super.handleOnDestroy(); }
}
