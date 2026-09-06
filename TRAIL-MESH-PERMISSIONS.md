# Trail Mesh permission model

Trail Mesh uses Android's just-in-time permission pattern. Every runtime-sensitive capability is shown as **off until the user starts the feature that needs it**. The app checks the grant immediately before use, explains the reason in the feature screen, and continues with a degraded experience when the user declines.

## Capabilities used by RoamWise

| Capability | Android permission/capability | Requested when | If declined |
| --- | --- | --- | --- |
| Nearby radio mesh | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `NEARBY_WIFI_DEVICES` (version-dependent) | Connect → **Allow & start** | Trail Mesh stays stopped; normal RoamWise works |
| Nearby compatibility location | `ACCESS_COARSE_LOCATION` on Android 13+ affected Google Nearby builds; legacy versions use the documented fine/coarse variant | Connect → **Allow & start** | Nearby discovery cannot start |
| Precise location | `ACCESS_FINE_LOCATION` | SOS only when the user chooses location-assisted SOS, or a separate navigation feature | SOS remains available without coordinates; user can describe location |
| Microphone | `RECORD_AUDIO` | Chat → **Hold radio note** | Text chat remains available |
| Camera | `CAMERA` | Only a future in-app camera capture action | System file/photo picker remains available |
| Notifications | `POST_NOTIFICATIONS` | Only when an opt-in notification feature is enabled | In-app status and foreground UI remain available |
| Files and media | Android system picker / selected URI | Share → **Choose file** | File sharing is unavailable; no broad storage permission is needed |
| Network and vibration | Normal network access and `VIBRATE` | Online fallback, local alarm | Offline mesh/text and non-vibrating UI remain available |

Contacts, phone, SMS, background location, accessibility, package visibility, and storage-wide read permissions are intentionally not requested by Trail Mesh. They are not needed for its current functionality.

## Important Android limitation

Runtime prompts can only be shown for permissions declared in the installed APK/AAB manifest. A later feature that needs a new permission category requires a new release containing that declaration. Declaring unrelated sensitive permissions in advance to avoid a future release is not policy-safe and would increase user distrust and Play disclosure scope.

## Release and Play policy

The in-app disclosure appears immediately before a sensitive runtime request and includes what is accessed, why, and what happens if the user declines. The Play Console Data Safety form and privacy policy must match the behavior of the released artifact and its SDKs. Trail Mesh does not request background location or claim guaranteed rescue delivery.

