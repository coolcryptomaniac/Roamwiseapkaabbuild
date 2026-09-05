# RoamWise → Capacitor Android app with REAL GPS

This turns your web app into a proper Android app where "Near Me" GPS actually
works (native permission prompt + accurate location), while keeping everything
else exactly as it is. You run these steps once on any computer with Node + the
Android SDK (or use a free cloud builder — see bottom).

## What you have in this folder
- `www/` — your web app (app.js already has native-GPS support baked in)
- `capacitor.config.json` — app id, name, geolocation plugin config
- `package.json` — the dependencies
- this guide

## One-time setup on your machine

1. Install Node.js (nodejs.org) and Java 17 + Android command-line tools.
   (Easiest all-in-one: install **Android Studio** once — it bundles the SDK.
   You won't code in it; you just need its SDK + one build command.)

2. In this folder, run:
   ```
   npm install
   npx cap add android
   npx cap sync android
   ```
   This generates the native `android/` project with the geolocation plugin.

3. **Add the location permissions** (Capacitor's geolocation plugin usually adds
   them, but confirm) — open `android/app/src/main/AndroidManifest.xml` and make
   sure these two lines are inside `<manifest>`:
   ```xml
   <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
   <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
   ```

4. **Use your existing signing key** so it's an update, not a new app. Copy your
   `rw.keystore` into `android/app/` and add to `android/app/build.gradle`:
   ```gradle
   android {
     signingConfigs {
       release {
         storeFile file('rw.keystore')
         storePassword 'roamwise2026'
         keyAlias 'roamwise'
         keyPassword 'roamwise2026'
       }
     }
     buildTypes { release { signingConfig signingConfigs.release } }
     defaultConfig {
       applicationId "com.gyanverse.roamwise"
       versionCode 77          // must be higher than your last upload
       versionName "16.0"
     }
   }
   ```

5. **Build the AAB:**
   ```
   cd android
   ./gradlew bundleRelease
   ```
   Output: `android/app/build/outputs/bundle/release/app-release.aab` — upload
   that to Play. GPS will now prompt and work in the app.

## To update the app later (after web changes)
Just re-copy the new web files into `www/`, bump `versionCode`, and re-run
`npx cap sync android` + `./gradlew bundleRelease`. That's your new pipeline.

## No computer set up? Use a free cloud builder
- **GitHub Actions** (free) — push this folder to a GitHub repo; a workflow can
  run the Gradle build in the cloud and hand you the AAB. (I can write the
  workflow file if you want this route.)
- **Capacitor's Appflow / Ionic** or **Codemagic** — free tiers build Capacitor
  apps in the cloud without you installing anything.

## Honest note
The `www/` files here are ready and GPS-aware. The one thing that cannot happen
in this chat is the final Gradle/native compile — that needs a real Android build
environment. Everything up to that point is done for you.

# Trail Mesh: offline trekking coordination

Android builds include an opt-in `NearbyMesh` Capacitor plugin backed by Google
Nearby Connections and the `P2P_CLUSTER` topology. It connects multiple nearby
devices without internet, but it is not a guaranteed long-distance or satellite
SOS system and it does not relay messages across unconnected hops.

The feature never starts in the background and never requests permissions at
launch. The trekking UI must explain the feature, then call:

```js
await RWNearbyMesh.requestPermissions();
await RWNearbyMesh.start('Trail name');
```

Listen for `verificationRequired`, compare the displayed verification code on
both devices, and call `accept(endpointId)` only after the trekkers confirm it.
Each radio message is limited to 16 KiB. The Trail Mesh UI splits selected
files into smaller messages and caps a transfer at 8 MB. Keep both phones
nearby and the app open until a transfer finishes. Call `stop()` when the
trekking session ends.

Useful events are `peerFound`, `peerLost`, `verificationRequired`,
`connectionChanged`, `messageReceived`, `meshState`, and `meshError`.

## User tutorial

1. Open **Trail Mesh** from the Android drawer or floating Trail Mesh button.
2. On every participating phone, enter a recognizable trail name and tap
   **Allow & start**. Approve Android's Nearby Devices and location prompts.
3. Compare the verification digits displayed on both phones in person. Accept
   only when they match. Reject unexpected requests.
4. Use:
   - **Chat** for internet-free nearby messages and recorded voice notes.
   - **Team** for roll calls, regroup/hold-position instructions, and local
     lost-trekker alerts.
   - **SOS** to alert connected nearby phones, attach current GPS when
     available, sound a local alarm, or open the existing RoamWise SOS screen.
   - **Share** for a photo, short video, song, voice note, PDF, or text file up
     to 8 MB.
   - **Test network** to confirm a verified peer can receive and reply.
5. Tusk understands “open Trail Mesh”, “mesh status”, “find lost trekker”, and
   “nearby SOS”. It opens the relevant screen; the user must still confirm any
   safety-sensitive broadcast.
6. Tap **Stop** when the trip ends.

## Safety and product limits

- Trail Mesh uses local Bluetooth and Wi-Fi radios. Range changes with terrain,
  bodies, weather, and phone hardware. It is not satellite communication.
- This release connects nearby peers but does not relay messages through
  intermediate phones. “Find lost trekker” works only when both phones are
  running Trail Mesh and come within radio range.
- SOS delivery is not guaranteed. Users should contact local emergency services
  by phone/SMS or another official channel whenever available.
- Voice is intentionally push-to-talk recording rather than an unvalidated
  full-duplex call. Media transfers are foreground, local, and best for short
  clips; they are not streaming.
- A display name is advertised locally only while Trail Mesh is running. No
  connection is accepted silently and no permission is requested at launch.
