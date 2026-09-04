package com.gyanverse.roamwise.nearby;

import android.Manifest;
import android.os.Build;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@CapacitorPlugin(
    name = "NearbyMesh",
    permissions = {
        @Permission(alias = "legacyLocation", strings = { Manifest.permission.ACCESS_FINE_LOCATION }),
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
    private final Set<String> connected = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> pending = Collections.synchronizedSet(new HashSet<>());
    private ConnectionsClient client;
    private boolean running;

    private ConnectionsClient client() {
        if (client == null) client = Nearby.getConnectionsClient(getContext());
        return client;
    }

    @PluginMethod
    public void requestMeshPermissions(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionForAliases(new String[] { "bluetoothNearby", "nearbyWifi" }, call, "permissionCallback");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionForAlias("bluetoothNearby", call, "permissionCallback");
        } else {
            requestPermissionForAlias("legacyLocation", call, "permissionCallback");
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
                call.reject("Could not start Nearby mesh: " + error.getMessage(), null, error);
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
    public void stop(PluginCall call) {
        stopMesh();
        call.resolve(status());
    }

    private final EndpointDiscoveryCallback discoveryCallback = new EndpointDiscoveryCallback() {
        @Override public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
            JSObject event = new JSObject();
            event.put("endpointId", endpointId);
            event.put("displayName", info.getEndpointName());
            notifyListeners("peerFound", event, true);
            client().requestConnection(safeName(Build.MODEL), endpointId, lifecycle)
                .addOnFailureListener(error -> notifyError("connectionRequestFailed", endpointId));
        }
        @Override public void onEndpointLost(@NonNull String endpointId) {
            JSObject event = new JSObject(); event.put("endpointId", endpointId);
            notifyListeners("peerLost", event, true);
        }
    };

    private final ConnectionLifecycleCallback lifecycle = new ConnectionLifecycleCallback() {
        @Override public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo info) {
            pending.add(endpointId);
            JSObject event = new JSObject();
            event.put("endpointId", endpointId);
            event.put("displayName", info.getEndpointName());
            event.put("verificationCode", info.getAuthenticationDigits());
            event.put("incoming", info.isIncomingConnection());
            notifyListeners("verificationRequired", event, true);
        }
        @Override public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
            pending.remove(endpointId);
            boolean ok = result.getStatus().isSuccess();
            if (ok) connected.add(endpointId); else connected.remove(endpointId);
            JSObject event = new JSObject(); event.put("endpointId", endpointId); event.put("connected", ok);
            event.put("statusCode", result.getStatus().getStatusCode());
            notifyListeners("connectionChanged", event, true);
        }
        @Override public void onDisconnected(@NonNull String endpointId) {
            connected.remove(endpointId);
            JSObject event = new JSObject(); event.put("endpointId", endpointId); event.put("connected", false);
            notifyListeners("connectionChanged", event, true);
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            byte[] bytes = payload.asBytes();
            if (payload.getType() != Payload.Type.BYTES || bytes == null || bytes.length > MAX_MESSAGE_BYTES) return;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return getPermissionState("bluetoothNearby") == PermissionState.GRANTED;
        }
        return getPermissionState("legacyLocation") == PermissionState.GRANTED;
    }

    private JSObject permissionResult() {
        JSObject result = new JSObject();
        result.put("granted", hasRequiredPermission());
        return result;
    }

    private JSObject status() {
        JSObject result = permissionResult();
        result.put("running", running);
        result.put("connectedCount", connected.size());
        result.put("pendingCount", pending.size());
        result.put("topology", "P2P_CLUSTER");
        result.put("multiHopRelay", false);
        result.put("endpoints", new JSArray(new ArrayList<>(connected)));
        return result;
    }

    private void stopMesh() {
        if (client != null) {
            client.stopAdvertising(); client.stopDiscovery(); client.stopAllEndpoints();
        }
        running = false; connected.clear(); pending.clear();
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
