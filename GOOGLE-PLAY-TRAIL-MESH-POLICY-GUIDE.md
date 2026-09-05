# Google Play policy update guide — Trail Mesh

Prepared 5 September 2026. This is the Trail Mesh delta, not a replacement for
the declarations already required by RoamWise account, Firebase, AI, analytics,
advertising, payment, chat, location, or other features.

## Public privacy-policy URL

Use exactly:

`https://www.roamwise.co.in/privacy.html`

The page must be live, public, non-geofenced, readable without login, and show
the Trail Mesh section before the Android update is submitted for review.

## A. Update the privacy-policy URL

1. Sign in to Google Play Console and select RoamWise.
2. Open **Policy and programs → App content**.
3. Find **Privacy policy** and select **Manage** (or **Start** if empty).
4. Paste `https://www.roamwise.co.in/privacy.html`.
5. Select **Save**. Do not send the change for review until the URL is live.

## B. Update Data safety

1. Stay in **Policy and programs → App content**.
2. Find **Data safety** and select **Manage**.
3. In **Data collection and security**, keep **Yes** for collecting or sharing
   required user-data types because RoamWise has account/cloud features and the
   Nearby SDK diagnostics described below.
4. Answer the encryption question based on the entire app—not Trail Mesh alone.
   Nearby Connections is encrypted, but select **Yes** only if every other
   collected data flow in the current APK is encrypted in transit.
5. Keep the deletion answer consistent with RoamWise's in-app deletion route and
   public account-deletion URL.
6. Add or review the following Trail Mesh-related data types. These conservative
   answers include user-initiated peer transfers even where Google may exempt a
   transfer from the separate “shared” label.

| Play data type | Collected | Shared | Processing | Required? | Purpose |
|---|---|---|---|---|---|
| Personal info → Name | Yes | No* | Ephemeral for Trail Mesh | Optional | App functionality |
| Location → Precise location | Yes | No* | Ephemeral | Optional | App functionality |
| Messages → Other in-app messages | Yes | No* | Ephemeral | Optional | App functionality |
| Health and fitness → Health info | Yes | No* | Ephemeral | Optional | App functionality |
| Photos and videos → Photos | Yes | No* | Ephemeral | Optional | App functionality |
| Photos and videos → Videos | Yes | No* | Ephemeral | Optional | App functionality |
| Audio files → Voice or sound recordings | Yes | No* | Ephemeral | Optional | App functionality |
| Audio files → Music files | Yes | No* | Ephemeral | Optional | App functionality |
| Audio files → Other audio files | Yes | No* | Ephemeral | Optional | App functionality |
| Files and docs → Files and docs | Yes | No* | Ephemeral | Optional | App functionality |
| Device or other IDs → Device or other IDs | Yes | Review SDK** | Ephemeral/SDK dependent | Optional** | App functionality; Analytics** |
| App info and performance → Diagnostics | Yes | Review SDK** | SDK dependent | Optional** | Analytics |

`*` Trail Mesh peer transfers are initiated by the user after prominent
disclosure and consent. Google states such transfers need not be labelled
“sharing.” If RoamWise separately sends the same type to Firebase, an AI
provider, advertising provider, analytics provider, or another organization,
the full-app answer may need **Shared: Yes**.

`**` Google says Nearby SDK usage analytics include discovery/connection
latency, reliability and throughput plus device model, country, build version
and package name, controlled by **Settings → Google → Usage & diagnostics**.
Review the APK's other SDKs before choosing the final sharing, required/optional
and processing answers.

7. For each selected data type, open **Start/Manage**, choose the correct
   collected/shared answer for the whole current app, select optional where every
   user can avoid that collection, and choose all applicable purposes.
8. Review **Store listing preview**. It must not claim “no data collected.”
9. Select **Save as draft** first. Recheck the installed release APK, manifest,
   Firebase/Google/advertising SDK disclosures and existing form answers.
10. When the privacy page is live and the answers match the release APK, select
    **Submit**.

## C. Prominent in-app disclosure

The Android Trail Mesh Connect screen now displays, before permissions and
discovery:

> RoamWise uses Nearby Devices access (Bluetooth and nearby Wi-Fi) to discover
> and communicate with verified RoamWise phones near you. Your chosen trail
> name is visible nearby while Trail Mesh runs. Text and alerts are transferred
> device-to-device. Microphone, precise location and selected-file access occur
> only when you separately choose a voice note, location-assisted SOS or file
> share. RoamWise does not upload these nearby transfers to its servers, but
> recipients may save or share them. You can stop discovery at any time.

It also identifies Google Nearby diagnostics, links the full privacy policy,
explains that delivery is not guaranteed, and requires **Allow & start** before
the Android permission request.

## D. Before sending for review

- Publish the web privacy-policy PR first and confirm both HTTP 200 and readable
  Trail Mesh text at the canonical URL.
- Install the exact signed APK/AAB candidate and capture screenshots showing the
  disclosure before the permission dialog and the Privacy Policy link.
- Confirm microphone permission appears only when recording, location only when
  attaching SOS location, and the system picker only after Share is selected.
- Recheck **Publishing overview → Changes ready to send for review**.
- Submit the policy changes and Android release together so declared behavior,
  in-app disclosure and the reviewed binary remain aligned.

This guide is operational guidance, not legal advice. The Play account owner is
responsible for ensuring the final form covers every behavior and SDK in every
app version currently distributed on Google Play.
