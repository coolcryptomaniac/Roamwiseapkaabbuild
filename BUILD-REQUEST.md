# RoamWise Android build request

Requested source: `coolcryptomaniac/roamwise@a9c98ff5d4c3b40f744e7d4f80f6134949606370`

Purpose: sync the merged Rave to Hell audio theme, sound/SFX/haptics settings, humanized cinematic opening enhancement, Android-safe auth handling, browser payment handoff, and sign-in/payment self-help into the Android package and produce a fresh signed APK + AAB.

The existing build workflow will replace `www/` from `roamwise/main`, calculate a new monotonic versionCode/versionName, sync Capacitor, and build release APK/AAB artifacts.
