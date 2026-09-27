#!/usr/bin/env node
import { copyFileSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';

const buildFile = 'android/app/build.gradle';
const manifestFile = 'android/app/src/main/AndroidManifest.xml';
const mainActivityFile = 'android/app/src/main/java/com/gyanverse/roamwise/MainActivity.java';
const pluginDir = 'android/app/src/main/java/com/gyanverse/roamwise/nearby';
const paymentPluginDir = 'android/app/src/main/java/com/gyanverse/roamwise/payment';

let build = readFileSync(buildFile, 'utf8');
const dependency = 'implementation "com.google.android.gms:play-services-nearby:19.5.0"';
if (!build.includes(dependency)) {
  build = build.replace(/dependencies\s*\{/, `dependencies {\n    ${dependency}`);
}
const cashfreeDependency = 'implementation "com.cashfree.pg:api:2.5.0"';
if (!build.includes(cashfreeDependency)) {
  build = build.replace(/dependencies\s*\{/, `dependencies {\n    ${cashfreeDependency}`);
}
writeFileSync(buildFile, build);

let manifest = readFileSync(manifestFile, 'utf8');
const marker = '    <!-- RoamWise Nearby trekking mesh: requested only after explicit user action. -->';
const permissions = `${marker}
    <!-- Google Nearby checks these normal Wi‑Fi capabilities on current devices. -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <!-- Nearby location compatibility is needed only on legacy Android releases. -->
    <!-- Fine/coarse location remain declared for the separate, user-selected
         precise-location SOS flow. Nearby does not request them on Android 13+. -->
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <!-- Requested by the WebView only when the user records a radio note. -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <!-- Capacitor's WebView audio bridge requests this alongside RECORD_AUDIO. -->
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" android:minSdkVersion="31" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" android:minSdkVersion="31" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:minSdkVersion="31" android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" android:minSdkVersion="32" />
    <uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" android:minSdkVersion="37" />
    <uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />
    <uses-feature android:name="android.hardware.wifi.direct" android:required="false" />
`;
if (!manifest.includes(marker)) {
  manifest = manifest.replace(/\n\s*<application/, `\n${permissions}\n    <application`);
} else {
  manifest = manifest.replace(/    <!-- RoamWise Nearby trekking mesh:[\s\S]*?\n\s*<application/, `${permissions}\n    <application`);
}
const cashfreeQueriesMarker = '    <!-- Cashfree Android SDK: discover installed UPI apps only during checkout. -->';
const cashfreeQueries = `${cashfreeQueriesMarker}
    <queries>
        <intent><action android:name="android.intent.action.VIEW" /><data android:scheme="upi" /></intent>
        <intent><action android:name="android.intent.action.VIEW" /><data android:scheme="tez" /></intent>
        <intent><action android:name="android.intent.action.VIEW" /><data android:scheme="gpay" /></intent>
        <intent><action android:name="android.intent.action.VIEW" /><data android:scheme="phonepe" /></intent>
        <intent><action android:name="android.intent.action.VIEW" /><data android:scheme="paytmmp" /></intent>
        <intent><action android:name="android.intent.action.VIEW" /><data android:scheme="credpay" /></intent>
    </queries>
`;
if (!manifest.includes(cashfreeQueriesMarker)) {
  manifest = manifest.replace(/\n\s*<application/, `\n${cashfreeQueries}\n    <application`);
}
writeFileSync(manifestFile, manifest);

writeFileSync(mainActivityFile, `package com.gyanverse.roamwise;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.getcapacitor.BridgeActivity;
import com.gyanverse.roamwise.nearby.NearbyMeshPlugin;
import com.gyanverse.roamwise.payment.CashfreePaymentPlugin;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(NearbyMeshPlugin.class);
        registerPlugin(CashfreePaymentPlugin.class);
        super.onCreate(savedInstanceState);

        // Android 15/16 enforce edge-to-edge for current targets. StatusBar's
        // overlaysWebView=false no longer changes layout, so apply real system-bar
        // and display-cutout insets to the entire WebView on every device shape.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        final WebView webView = getBridge().getWebView();
        webView.setBackgroundColor(android.graphics.Color.rgb(7, 9, 15));
        ViewCompat.setOnApplyWindowInsetsListener(webView, (View view, WindowInsetsCompat insets) -> {
            Insets safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(webView);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
    }
}
`);

mkdirSync(pluginDir, { recursive: true });
copyFileSync('native/nearby/NearbyMeshPlugin.java', `${pluginDir}/NearbyMeshPlugin.java`);
mkdirSync(paymentPluginDir, { recursive: true });
copyFileSync('native/payment/CashfreePaymentPlugin.java', `${paymentPluginDir}/CashfreePaymentPlugin.java`);
console.log('Configured Android safe insets, Google Nearby and native Cashfree checkout');
