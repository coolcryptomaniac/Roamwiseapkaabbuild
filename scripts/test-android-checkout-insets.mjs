#!/usr/bin/env node
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const configurator = readFileSync('scripts/configure-nearby-mesh.mjs', 'utf8');
const paymentPlugin = readFileSync('native/payment/CashfreePaymentPlugin.java', 'utf8');
const capacitorConfig = JSON.parse(readFileSync('capacitor.config.json', 'utf8'));

assert.match(configurator, /WindowInsetsCompat\.Type\.systemBars\(\)/);
assert.match(configurator, /WindowInsetsCompat\.Type\.displayCutout\(\)/);
assert.match(configurator, /final View webContainer = \(View\) webView\.getParent\(\)/);
assert.match(configurator, /setOnApplyWindowInsetsListener\(webContainer/);
assert.match(configurator, /container\.setPadding\(safe\.left, safe\.top, safe\.right, bottom\)/);
assert.match(configurator, /WindowInsetsCompat\.Type\.ime\(\)/);
assert.match(configurator, /Insets\.NONE/);
assert.match(configurator, /requestApplyInsets\(webContainer\)/);
assert.doesNotMatch(configurator, /setOnApplyWindowInsetsListener\(webView/);
assert.equal(capacitorConfig.plugins.SystemBars.insetsHandling, 'disable');
assert.equal(capacitorConfig.plugins.SystemBars.style, 'DARK');
assert.match(configurator, /registerPlugin\(CashfreePaymentPlugin\.class\)/);
assert.match(configurator, /com\.cashfree\.pg:api:2\.5\.0/);
assert.match(configurator, /android:scheme="upi"/);
assert.match(configurator, /android:scheme="phonepe"/);

assert.match(paymentPlugin, /@CapacitorPlugin\(name = "CashfreePayment"\)/);
assert.match(paymentPlugin, /CFPaymentGatewayService\.getInstance\(\)\.doPayment/);
assert.match(paymentPlugin, /notifyListeners\("paymentVerify"/);
assert.match(paymentPlugin, /notifyListeners\("paymentFailure"/);
assert.doesNotMatch(paymentPlugin, /client-secret|CASHFREE_SECRET/i);

console.log('Android root-inset and native Cashfree contracts are present');
