# Native Google sign-in setup

The Android app now uses the native Google account chooser and bridges the returned Google ID token into the existing Firebase JavaScript session. This avoids the unsupported Firebase web-popup flow inside Android WebView.

## One-time Firebase Console setup

1. Open Firebase project `roamwisepro`.
2. In **Project settings > Your apps**, add or open the Android app:
   - Package name: `com.gyanverse.roamwise`
3. Add the current release signing certificate fingerprints:
   - SHA-1: `FF:81:A4:C5:7E:F0:FD:0B:CB:C5:2B:A6:5F:A1:FE:12:B6:2B:E4:F8`
   - SHA-256: `65:3F:96:EC:2B:3D:84:72:EC:BA:8A:7C:73:B4:F2:98:B9:BE:90:57:7B:4E:0E:42:D7:37:00:C4:0C:6E:D1:62`
4. In **Authentication > Sign-in method**, enable Google.
5. Download the updated `google-services.json`.
6. Supply it to this repository using one of:
   - Preferred: base64-encode the complete file and save it as the Actions secret `GOOGLE_SERVICES_JSON_B64`.
   - Simple alternative: upload `google-services.json` to the repository root. Firebase client configuration is not a server secret, but repository history remains public.
7. Run the **Build RoamWise Android** workflow.

If the Play Store uses Play App Signing, also add the Play Console **App signing key certificate** SHA-1 in Firebase and download a fresh `google-services.json`.

## Verification

On Android, **Continue with Google** must show the native Google account chooser and return directly to RoamWise. It must not navigate the WebView to Firebase or leave the user stranded in Chrome.

Email/password accounts are now signed out until `emailVerified` is true. The UI provides resend-verification and password-reset actions. Facebook and Apple buttons are intentionally removed until those providers are configured.
