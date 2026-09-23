# Journey Trace native Android bridge

Journey Trace V2 uses a user-started **location foreground service** so an active trip can keep recording when the Capacitor WebView is backgrounded.

## Privacy and lifecycle

- It never starts at app launch, boot, sign-in or itinerary generation.
- The traveller starts it by tapping **Start Journey** inside RoamWise.
- Android shows an ongoing **Journey Trace is active** notification with an explicit Stop action while the native service is recording.
- Pause/Finish stops the native service.
- GPS points are buffered in app-private SharedPreferences and pulled back into the Journey Trace web layer. This plugin does not upload raw location.
- No ACCESS_BACKGROUND_LOCATION permission is requested by this implementation. The service must be started while the app is visible.
- The service is START_NOT_STICKY and stops when the app task is swiped away; Android is not asked to resurrect it after process death.

## Build integration

The Android build recreates the Capacitor platform on every CI run. After configure-nearby-mesh.mjs, run configure-journey-trace.mjs.

This adds the Play Services Location dependency, foreground-service permissions, the service manifest declaration, both Java files and JourneyTracePlugin registration in MainActivity.

## Capacitor API

Start with destination, intervalMs and minDistanceMeters; then use getBufferedPoints, clearBufferedPoints, status and stop.

The web layer still records foreground browser GPS. It merges the native buffer on pause/finish so a trip stays one continuous footprint.

## Play release note

Before shipping a release that includes this service, keep the Play Console Data Safety and foreground-service declaration aligned with the actual behavior. Do not describe this as hidden/background surveillance: it is an explicit traveller recording tool with an ongoing Android notification.
