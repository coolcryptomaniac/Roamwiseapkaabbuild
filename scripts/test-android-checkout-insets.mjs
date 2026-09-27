#!/usr/bin/env node
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const configurator = readFileSync('scripts/configure-nearby-mesh.mjs', 'utf8');
const paymentPlugin = readFileSync('native/payment/CashfreePaymentPlugin.java', 'utf8');

assert.match(configurator, /WindowInsetsCompat\.Type\.systemBars\(\)/);
assert.match(configurator, /WindowInsetsCompat\.Type\.displayCutout\(\)/);
assert.match(configurator, /view\.setPadding\(safe\.left, safe\.top, safe\.right, safe\.bottom\)/);
assert.match(configurator, /registerPlugin\(CashfreePaymentPlugin\.class\)/);
assert.match(configurator, /com\.cashfree\.pg:api:2\.5\.0/);
assert.match(configurator, /android:scheme="upi"/);
assert.match(configurator, /android:scheme="phonepe"/);

assert.match(paymentPlugin, /@CapacitorPlugin\(name = "CashfreePayment"\)/);
assert.match(paymentPlugin, /CFPaymentGatewayService\.getInstance\(\)\.doPayment/);
assert.match(paymentPlugin, /notifyListeners\("paymentVerify"/);
assert.match(paymentPlugin, /notifyListeners\("paymentFailure"/);
assert.doesNotMatch(paymentPlugin, /client-secret|CASHFREE_SECRET/i);

console.log('Android safe-inset and native Cashfree contracts are present');
