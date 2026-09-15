# Trail Mesh field-release test plan

Automated CI proves source alignment, web injection, Java compilation, release
shrinking, signing, APK/AAB generation, and artifact verification. The cases
below require physical Android radios and must pass before production release.

## Required devices and terrain

- Four Android phones covering Android 10, 12, 14, and the current target API.
- At least two different manufacturers, including one aggressive battery saver.
- Open ground, forest/tree cover, ridge/valley obstruction, indoor shelter, and
  airplane mode with Wi-Fi/Bluetooth manually re-enabled.

## Connection and privacy

1. Deny every permission independently; verify the feature stays off and the
   rest of RoamWise works.
2. Accept matching verification digits; reject mismatched and unexpected peers.
3. Lock/unlock, background/foreground, rotate, and kill/relaunch during search.
4. Confirm Stop ends advertising/discovery and a trail name is not visible.
5. Confirm no permission prompt occurs before the user taps Allow & start.

## Direct messaging and media

1. Exchange Unicode, Hindi, emoji, empty, 4,000-character, and rapid messages.
2. Run network tests under good, weak, disconnected, and reconnecting links.
3. Send every supported media type at 0 B, one chunk, many chunks, 8 MB, and
   64 MB; show the warning above 8 MB and reject 64 MB + 1 B. Interrupt sender
   and receiver halfway through.
4. Verify received file hash, MIME type, filename escaping, memory recovery, and
   that incomplete files cannot be opened as complete.
5. Record voice with permission denied, silence, incoming phone interruption,
   Bluetooth headset, 60-second auto-stop, and peer loss during transfer.
6. On Android, verify the WebView microphone prompt appears only after tapping
   Record radio note, that a non-empty WebM/M4A/OGG clip is sent, and that the
   received audio control plays after the screen is unlocked. Test the native
   Trail character Read last reply action and confirm it falls back to browser
   speech with an explicit error when no TTS engine exists.

## Relay topology

1. Chain A—B—C where A cannot see C. Opt in on A and B; verify one delivery to C.
2. Ring A—B—C—A; verify duplicate IDs prevent repeated display and forwarding.
3. Chain longer than three hops; verify the fourth hop receives nothing.
4. Disable relay on each intermediate phone; verify forwarding stops immediately.
5. Partition and rejoin the group; verify cached IDs do not replay for ten minutes.
6. Confirm files, video, songs, voice chunks, ping, and pong are never relayed.
7. Flood 100 chat messages; verify the UI remains responsive and SOS is visible.

## Trekking, rescue, and blackout

1. Leader roll call with every member, missing member, duplicate names, and a
   leader phone failure. Confirm commands identify their sender.
2. SOS with GPS success, timeout, stale/disabled location, no peers, one hop,
   three hops, and network reconnection. Never claim guaranteed delivery.
3. Lost-trekker alert before/after the missing device comes into range.
4. Local alarm with silent mode, media volume zero, Do Not Disturb, vibration
   unavailable, screen locked, and low battery.
5. Full blackout: airplane mode, no SIM, no internet, cached web app only.
6. Pairing recovery: close the overlay while a request arrives, reopen it, and
   confirm the radar card still shows the digits; test manual copy/share and
   verify it never accepts a connection by itself.
7. Battery soak: 2-hour and 8-hour discovery/relay sessions; record drain and
   thermal behavior. Test Android battery optimization killing the app.
8. Congestion: simultaneous SOS, team command, chat, and direct media transfer.

## Radio diagnostics and group scale

- Verify the radar labels discovered versus verified endpoints and keeps the
  pairing digits visible until accept/reject or expiry.
- Confirm the UI does not invent RSSI, transmit power or a guaranteed metre
  range. Google Nearby does not expose a calibrated signal/range value through
  this bridge; record measured results separately for each phone and terrain.
- Test 30–100-person exercises as multiple small clusters with a leader, rear
  marker and spaced relay volunteers. Treat P2P_CLUSTER as best effort, not a
  promise that every phone is connected in one hop.
- Disconnect a peer for 1, 5 and 24 hours, reconnect, and verify queued chat,
  team and fun packets send once, while SOS is never silently queued.

## Pro, selected trial and operator access

1. With no account entitlement, confirm pairing, basic safety text, radio check,
   lost-trekker alert, SOS, local alarm and personal check-in remain available.
2. Confirm voice notes, selected-file transfer, relay and Trail Fun show a clear
   paid-Pro gate without starting a permission request or radio action.
3. With the ordinary account trial, confirm only selected practice tools unlock;
   relay, media, exports and operator command stay locked.
4. Grant `trailMeshTrialSelected` plus a future `trailMeshTrialUntil` from the
   admin console. Confirm roll calls, checkpoints, roster and three local
   incident records work, while a fourth incident, exports and relay stay locked.
5. Change the selected-trial expiry to the past and verify access closes after an
   entitlement refresh without deleting Safety Free data.
6. Grant `trailMeshPlan: operator` with a future `trailMeshLicenseUntil`. Confirm
   full field controls, CSV export and shift handover work. Test an expired paid
   seat and a permanent seat with no expiry.
7. Attempt to write every Trail Mesh entitlement field from a non-admin account;
   Firestore rules must reject create and update. Confirm the admin write creates
   an `adminAuditLog` record.

## Role, roster and privacy operations

1. Save every role using Unicode aliases, valid/invalid team codes, empty
   expedition names and declined consent. No profile may be shared before the
   user saves consent.
2. Connect two teams with different team codes. The roster must flag the mismatch
   and ignore operator roll-call/checkpoint commands from the other code, while
   still allowing cross-team SOS and lost-trekker alerts.
3. Request a roll call and submit Safe, Delayed, Resting and Need help from
   separate phones. Confirm last-seen time and attention counts update once.
4. Disconnect an accepted peer and confirm the native endpoint closes, roster
   state becomes disconnected and the peer cannot send new packets.
5. Log low, medium, high and critical incidents. Confirm nothing is transmitted,
   no location prompt appears, CSV escapes quotes/newlines and selected trials
   cannot export.
6. Use Erase local expedition data and verify profile, roster, checkpoints,
   incidents, chat, remembered peers and queued messages are removed. Account
   entitlement and unrelated RoamWise data must remain intact.

## Calling readiness

- Run Live call readiness. It must report that the current BYTES-only bridge
  cannot carry a duplex audio stream; never show a false connected call. A
  future native stream release must add microphone disclosure, a five-minute
  warning, a thirty-minute hard cap, interruption handling and battery tests.

## Entertainment and misuse

1. Run photo challenge, quiz, story, and cheer across direct and relayed peers.
2. Verify repeated fun messages cannot obscure the latest SOS in the interface.
3. Reject hostile names, HTML/script text, malformed JSON, oversized packets,
   duplicate IDs, invalid TTL values, and unsupported packet types.

## Release gate

- No crash, ANR, silent connection acceptance, false delivery confirmation, or
  permission request at launch.
- SOS remains visually dominant and usable during chat/media congestion.
- APK/AAB CI is green and two complete field passes succeed on all four devices.
- Play Data safety and the public privacy policy describe Nearby Devices,
  microphone, optional location, user-initiated sharing, and retention behavior.


## Same-session reconnection drill

Run this on every release candidate with two real Android phones:

1. Start Trail Mesh on both phones, compare the digits and accept.
2. Send a radio check, then move one phone out of range without tapping
   Disconnect. Confirm both UIs show **automatic rejoin active**.
3. While separated, queue a safety text. Return the phone to range and verify it
   reconnects without a second prompt in the same running session, then verify
   the queued text arrives once.
4. Tap **Disconnect** on one phone. Keep both phones in range for two minutes and
   confirm it does not reconnect.
5. Stop and restart Trail Mesh. Confirm the same phone requires a fresh digit
   comparison; no trust may survive Stop or app-process death.
6. Repeat with Bluetooth toggled off/on, Wi-Fi toggled off/on, 1/5/15-minute
   separation, screen lock, battery saver, and the supported Android API range.
7. Record reconnect latency, failed attempts and battery change. A UI or unit
   test is not evidence of field radio quality; complete ridge/forest/camp
   trials before any operator claim.
