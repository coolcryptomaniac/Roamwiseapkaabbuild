import test from 'node:test';import assert from 'node:assert/strict';import fs from 'node:fs';
test('actual Android bundle contains current RoamWise shop and AI widget',()=>{
 for(const f of ['index.html','app.js','shop/index.html','shop/checkout.js','embed/ailon-tusk.js','android-bridge.js'])assert.ok(fs.existsSync('www/'+f),'missing '+f);
 assert.match(fs.readFileSync('www/index.html','utf8'),/android-bridge\.js/);
 assert.match(fs.readFileSync('www/shop/checkout.js','utf8'),/payment_session_id/);
});
