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
3. Send every supported media type at 0 B, one chunk, many chunks, and 8 MB;
   reject 8 MB + 1 B. Interrupt sender and receiver halfway through.
4. Verify received file hash, MIME type, filename escaping, memory recovery, and
   that incomplete files cannot be opened as complete.
5. Record voice with permission denied, silence, incoming phone interruption,
   Bluetooth headset, 60-second auto-stop, and peer loss during transfer.

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
6. Battery soak: 2-hour and 8-hour discovery/relay sessions; record drain and
   thermal behavior. Test Android battery optimization killing the app.
7. Congestion: simultaneous SOS, team command, chat, and direct media transfer.

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
