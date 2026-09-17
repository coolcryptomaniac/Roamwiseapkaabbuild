# Native Google sign-in setup

Web authentication source synced from `coolcryptomaniac/roamwise@7dc95a0b70198890cb15fe4eba4284f6cf6c692b`.

The Android app now uses the native Google account chooser and bridges the returned Google ID token into the existing Firebase JavaScript session. This avoids the unsupported Firebase web-popup flow inside Android WebView.

The build also enables `rgcfaIncludeGoogle` and packages the stable Credential Manager dependencies in every freshly generated Android project. Merely listing `google.com` in `capacitor.config.json` is not enough: without the Gradle flag, this plugin compiles against the Google classes but does not include them in the installed app.

## One-time Firebase Console setup

1. Open Firebase project `roamwisepro`.
2. In **Project settings > Your apps**, add or open the Android app:
   - Package name: `com.gyanverse.roamwise`
3. Add the current upload/release signing certificate fingerprints:
   - SHA-1: `FF:81:A4:C5:7E:F0:FD:0B:CB:C5:2B:A6:5F:A1:FE:12:B6:2B:E4:F8`
   - SHA-256: `65:3F:96:EC:2B:3D:84:72:EC:BA:8A:7C:73:B4:F2:98:B9:BE:90:57:7B:4E:0E:42:D7:37:00:C4:0C:6E:D1:62`
4. In **Authentication > Sign-in method**, enable Google.
5. Add the Google Play **App signing key certificate** fingerprints to the same Android app in Firebase:
   - SHA-1: `5A:18:6E:DB:7D:4B:85:50:38:5B:DF:C7:F7:9A:26:C9:F8:AC:66:07`
   - SHA-256: `22:84:6F:12:F7:FA:0D:E1:F6:3D:FF:79:15:79:FE:68:D1:C0:1E:46:A9:55:14:A3:40:49:48:4F:21:66:96:B5`
   These are different from the upload certificate above because Google Play re-signs the APK delivered to users.
6. Download a new `google-services.json` only after both SHA-1 certificates appear in Firebase.
7. Supply it to this repository using one of:
   - Preferred: base64-encode the complete file and save it as the Actions secret `GOOGLE_SERVICES_JSON_B64`.
   - Simple alternative: upload `google-services.json` to the repository root. Firebase client configuration is not a server secret, but repository history remains public.
8. Run the **Build RoamWise Android** workflow. The public Play SHA-1 is pinned in the workflow, which intentionally stops if its matching OAuth entry is absent rather than publishing another build whose Google button cannot work.

The build validates all three pieces needed by the native plugin: the Android package, a Web OAuth client used to obtain an ID token, and OAuth entries for both the upload and Play app-signing SHA-1 certificates.

## Verification

On Android, **Continue with Google** first uses Android Credential Manager. If that API fails without a useful result, RoamWise retries once with the supported legacy Google account chooser. Cancelling never opens a second chooser. A successful choice returns directly to RoamWise; it must not navigate the WebView to Firebase or leave the user stranded in Chrome.

Email/password accounts remain signed in before verification and can use ordinary features and buy Pro. Verification is required only for identity-sensitive actions such as referrals and creator/partner access. The UI provides resend-verification and password-reset actions. Facebook and Apple buttons are intentionally removed until those providers are configured.
