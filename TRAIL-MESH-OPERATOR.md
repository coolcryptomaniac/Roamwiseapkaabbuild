# Trail Mesh Pro and Operator

Trail Mesh is packaged as an offline field-coordination layer, not a public
traveller-matching network. Emergency access is never used as a paywall.

## Access model

| Access | Included | Excluded |
| --- | --- | --- |
| Safety Free | Verified pairing, nearby SOS, local alarm, lost-trekker alert, basic safety text, radio check, voluntary personal check-in | Voice/media, multi-hop relay, entertainment, operator command and exports |
| Selected Practice Trial | Safety Free plus role setup, health checks and a limited offline character practice flow during the existing account trial | Relay, media, exports and operator command |
| Selected Operator Trial | Server-approved organisation and expiry; expedition roles, nearby roster, voluntary check-ins, roll calls, checkpoints and up to 3 local incident records for up to 12 nearby profiles | Relay, voice/media and exports |
| Pro | Safety Free plus voice notes, selected-file transfer, three-hop lightweight relay, trail characters and camp activities | Organisation roster command and operator export unless separately licensed |
| Operator | Pro plus expedition roster, role-aware prompts, roll calls, checkpoints, incident log, CSV export and shift handover | Satellite rescue, guaranteed delivery, continuous tracking and medical decision support |

Selected Operator Trial and Operator are server-backed account grants. The app
reads these fields from `users/{uid}`:

- `trailMeshPlan`: `operator` for a paid operator seat.
- `trailMeshTrialSelected`: `true` only for an approved trial account.
- `trailMeshTrialUntil`: Firestore Timestamp for the selected trial expiry.
- `trailMeshLicenseUntil`: optional Firestore Timestamp for a paid seat expiry.
- `trailMeshOrganizationName`: verified operator name shown in the app.

The web admin console owns these fields. Firestore rules deny self-service
creation or modification of every field above.

Selected-trial and Operator access must be verified online before departure in
the current app session. The nearby safety core remains available if that check
cannot run. Before using licensing in remote commercial operations, replace
this pilot behaviour with a short-lived, signed offline licence that can be
verified without weakening expiry or organisation controls.

## Operator workflow

1. The administrator verifies the trekking company and grants a short selected
   trial or paid Operator seat from Users → Mesh access.
2. Before departure, each phone chooses a trail alias and a field role: trekker,
   leader, rear marker, field medic or base operations.
3. The team adds an expedition label and a 4–12 character team code. The team
   code is a roster label, not a password.
4. Phones start Trail Mesh, compare the Google Nearby verification digits face
   to face and accept only matching devices.
5. Field profiles are shared only after a verified connection. Different team
   codes are visibly flagged.
6. The leader requests roll calls and broadcasts named checkpoints. Members tap
   Safe, Delayed, Resting or Need help; no continuous location is collected.
7. An authorised trial/operator device may keep a small local incident record.
   Paid Operator can deliberately export CSV and a plain-text shift handover.
8. At expedition closeout, Erase local expedition data clears the local role,
   roster, checkpoints, incidents, chat, peer history and queued messages.

## Trust boundary

- No swipe matching, age/gender discovery, public profiles, followers, public
  feed, contact upload, reward wheel or engagement dark patterns.
- A field alias is preferred over a legal name. The UI tells operators not to
  enter government IDs, diagnoses or unnecessary personal information.
- The app does not silently accept peers. A user can disconnect a verified phone
  at any time.
- Incident records do not broadcast automatically and never upload from Trail
  Mesh. Export is a deliberate paid-operator action.
- Lightweight relays remain opt-in and paid. File chunks, voice notes, ping and
  typing packets are not relayed.
- Trail Mesh is short-range phone radio, not satellite communication or a rescue
  dispatch service. Trek operators still need trained leaders, medical protocol,
  evacuation plans and the appropriate satellite/official communication layer.

## Pilot offer for Indian trek operators

Use a controlled 30-day pilot with two departures and 12–20 devices per
departure. The selected-trial build itself caps the visible operator roster at
12 profiles; expand only after field validation. Measure:

- verified pairing success at the trailhead;
- roll-call completion time at named checkpoints;
- reconnect and message delivery in forest, ridge and camp conditions;
- battery drain over 2-hour and 8-hour sessions;
- false/duplicate alerts and radio congestion;
- leader confidence and time saved during handover;
- privacy comprehension and closeout deletion success.

Do not sell a guaranteed range, 30–100 phone single-hop topology, continuous
calling, multi-gigabyte transfer or rescue-delivery SLA until native transport
and physical field tests prove those claims.


## Connection continuity contract

- The first connection always requires both phones to compare and accept the
  Nearby verification digits.
- A successfully verified endpoint is remembered in native memory only while
  that Trail Mesh radio session is running. If terrain briefly breaks the link,
  both phones retry with bounded backoff and automatically accept only that same
  current-session endpoint when it returns.
- Tapping **Disconnect**, tapping **Stop**, or the app process ending clears that
  trust. A later session requires digit verification again.
- The two-hour battery timer is optional and off by default. Android can still
  suspend or kill an app, radios can be switched off, and mountain terrain can
  exceed local radio range; therefore RoamWise does not promise uninterrupted
  coverage or guaranteed delivery.
- Safety text queued while no verified peer is connected flushes after rejoin.
  Voice uses short 48 kHz mono Opus-preferred radio notes with echo cancellation,
  noise suppression and automatic gain control. Full-duplex calls remain
  disabled until a separately tested native STREAM transport exists.
