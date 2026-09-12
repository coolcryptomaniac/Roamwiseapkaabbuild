#!/usr/bin/env node
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const source = readFileSync('native/nearby/nearby-mesh.js', 'utf8');
const synced = readFileSync('www/nearby-mesh.js', 'utf8');
const nativePlugin = readFileSync('native/nearby/NearbyMeshPlugin.java', 'utf8');

function accessFor({ status, current, storage = {}, unlocked = false }) {
  const values = new Map(Object.entries(storage));
  const localStorage = {
    getItem(key) { return values.has(key) ? values.get(key) : null; },
    setItem(key, value) { values.set(key, String(value)); },
    removeItem(key) { values.delete(key); }
  };
  const window = {
    _proUnlocked: unlocked,
    rwStatusLabel() { return status; },
    rwIsPro() { return current; }
  };
  const document = { readyState: 'loading', addEventListener() {} };
  vm.runInNewContext(source, { window, document, localStorage, Date, Math, console, setTimeout, clearTimeout, setInterval, clearInterval, Promise });
  return window.RWTrailMesh.access();
}

const free = accessFor({ status: { code: 'free' }, current: false });
assert.equal(free.full, false);
assert.equal(free.practice, false);

const accountTrial = accessFor({
  status: { code: 'trial' },
  current: true,
  storage: { rw_trial_until: String(Date.now() + 864e5) }
});
assert.equal(accountTrial.accountTrial, true);
assert.equal(accountTrial.practice, true);
assert.equal(accountTrial.full, false, 'an ordinary account trial must not unlock paid Trail Mesh features');

const paid = accessFor({ status: { code: 'pro' }, current: true });
assert.equal(paid.paidPro, true);
assert.equal(paid.full, true);

assert.equal(source, synced, 'native and packaged Trail Mesh JavaScript must be byte-identical');
for (const field of ['trailMeshPlan', 'trailMeshTrialSelected', 'trailMeshTrialUntil', 'trailMeshLicenseUntil', 'trailMeshOrganizationName']) {
  assert.match(source, new RegExp(field));
}
assert.match(source, /No swipe matching/);
assert.match(source, /function requireOperator/);
assert.match(nativePlugin, /public void disconnect\(PluginCall call\)/);
assert.match(nativePlugin, /disconnectFromEndpoint/);
assert.match(nativePlugin, /trustedSession/);
assert.match(nativePlugin, /scheduleReconnect/);
assert.match(nativePlugin, /autoReconnectScope/);
assert.match(nativePlugin, /manuallyDisconnected/);
assert.match(source, /automatic rejoin is active/);
assert.match(source, /audioBitsPerSecond:32000/);
assert.doesNotMatch(source, /id="rwmEco" type="checkbox" checked/);

console.log('Trail Mesh access, trust boundary and packaged-source checks passed.');
