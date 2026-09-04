#!/usr/bin/env node
import { copyFileSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';

const buildFile = 'android/app/build.gradle';
const manifestFile = 'android/app/src/main/AndroidManifest.xml';
const mainActivityFile = 'android/app/src/main/java/com/gyanverse/roamwise/MainActivity.java';
const pluginDir = 'android/app/src/main/java/com/gyanverse/roamwise/nearby';

let build = readFileSync(buildFile, 'utf8');
const dependency = 'implementation "com.google.android.gms:play-services-nearby:19.5.0"';
if (!build.includes(dependency)) {
  build = build.replace(/dependencies\s*\{/, `dependencies {\n    ${dependency}`);
  writeFileSync(buildFile, build);
}

let manifest = readFileSync(manifestFile, 'utf8');
const marker = '    <!-- RoamWise Nearby trekking mesh: requested only after explicit user action. -->';
if (!manifest.includes(marker)) {
  const permissions = `${marker}
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" android:maxSdkVersion="31" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" android:maxSdkVersion="31" />
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:minSdkVersion="29" android:maxSdkVersion="31" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" android:minSdkVersion="31" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" android:minSdkVersion="31" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:minSdkVersion="31" android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" android:minSdkVersion="32" android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" android:minSdkVersion="37" />
`;
  manifest = manifest.replace(/\n\s*<application/, `\n${permissions}\n    <application`);
  writeFileSync(manifestFile, manifest);
}

writeFileSync(mainActivityFile, `package com.gyanverse.roamwise;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;
import com.gyanverse.roamwise.nearby.NearbyMeshPlugin;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(NearbyMeshPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
`);

mkdirSync(pluginDir, { recursive: true });
copyFileSync('native/nearby/NearbyMeshPlugin.java', `${pluginDir}/NearbyMeshPlugin.java`);
console.log('Configured Google Nearby Connections 19.5.0 and RoamWise NearbyMesh plugin');
