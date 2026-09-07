# Trail Mesh permission model

Trail Mesh uses Android's just-in-time permission pattern. Every runtime-sensitive capability is shown as **off until the user starts the feature that needs it**. The app checks the grant immediately before use, explains the reason in the feature screen, and continues with a degraded experience when the user declines.

## Capabilities used by RoamWise

| Capability | Android permission/capability | Requested when | If declined |
| --- | --- | --- | --- |
| Nearby radio mesh | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `NEARBY_WIFI_DEVICES` (version-dependent) | Connect → **Allow & start** | Trail Mesh stays stopped; normal RoamWise works |
| Nearby compatibility location | `ACCESS_COARSE_LOCATION` (Android ≤28) or `ACCESS_FINE_LOCATION` (Android 29–31), per Google Nearby’s legacy requirements | Connect → **Allow & start** on legacy devices | Nearby discovery cannot start on those legacy versions; Android 13+ does not request location for Nearby |
| Precise location | `ACCESS_FINE_LOCATION` | SOS only when the user chooses location-assisted SOS, or a separate navigation feature | SOS remains available without coordinates; user can describe location |
| Microphone | `RECORD_AUDIO` | Chat → **Record radio note** | Text chat remains available |
| WebView audio routing | `MODIFY_AUDIO_SETTINGS` (normal capability) | Declared for the WebView audio bridge; no runtime prompt | Recording/playback stays unavailable until the user grants microphone/audio access |
| Camera | `CAMERA` | Only a future in-app camera capture action | System file/photo picker remains available |
| Notifications | `POST_NOTIFICATIONS` | Only when an opt-in notification feature is enabled | In-app status and foreground UI remain available |
| Files and media | Android system picker / selected URI | Share → **Choose file** | File sharing is unavailable; no broad storage permission is needed |
| Network and vibration | Normal network access and `VIBRATE` | Online fallback, local alarm | Offline mesh/text and non-vibrating UI remain available |

Contacts, phone, SMS, background location, accessibility, package visibility, and storage-wide read permissions are intentionally not requested by Trail Mesh. They are not needed for its current functionality.

`ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` stay declared because Android requires a manifest declaration for the separate, user-selected precise-location SOS flow. The native Nearby start path does not request location on Android 13+; a location prompt is shown only when the SOS checkbox is selected.

## Pairing and media notes

When a nearby request arrives, the verification digits stay visible in the Nearby radar card until the request completes or expires. Compare the digits face to face, then accept. The **Share digits manually** action can copy the digits to the Android share sheet or clipboard for coordination; it never accepts a connection or bypasses verification.

The current bridge sends small BYTES chunks and assembles received files in WebView memory. It warns before any file over 8 MB and caps a safe transfer at 64 MB. Do not describe this release as supporting 2–10 GB transfers; that requires a native Google Nearby `Payload.fromFile` stream with resumable storage and a separate battery/data policy.

Voice notes use `RECORD_AUDIO` only while recording. Capacitor's WebView audio
bridge also needs the normal `MODIFY_AUDIO_SETTINGS` manifest capability; this
does not create a runtime prompt. Character playback prefers the installed
native Text-to-Speech engine and falls back to browser speech when available.
If neither exists, the health console reports the failure instead of leaving a
button spinning.

The current Nearby bridge intentionally has no duplex call transport. Its
`sendMessage` method is a small BYTES channel, not an audio stream. The UI's
Live call readiness check is therefore explanatory, not a fake call. A future
native stream implementation must be field-tested before enabling calling.

## Important Android limitation

Runtime prompts can only be shown for permissions declared in the installed APK/AAB manifest. A later feature that needs a new permission category requires a new release containing that declaration. Declaring unrelated sensitive permissions in advance to avoid a future release is not policy-safe and would increase user distrust and Play disclosure scope.

## Release and Play policy

The in-app disclosure appears immediately before a sensitive runtime request and includes what is accessed, why, and what happens if the user declines. The Play Console Data Safety form and privacy policy must match the behavior of the released artifact and its SDKs. Trail Mesh does not request background location or claim guaranteed rescue delivery.
