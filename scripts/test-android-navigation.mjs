import test from 'node:test';import assert from 'node:assert/strict';import fs from 'node:fs';import vm from 'node:vm';
const code=fs.readFileSync('scripts/android-bridge.js','utf8');
function setup(){
 let click,opened=[],assigned=[];
 const location={href:'https://localhost/index.html',origin:'https://localhost',assign:u=>assigned.push(u)};
 const document={addEventListener:(type,fn)=>{if(type==='click')click=fn}};
 const window={Capacitor:{isNativePlatform:()=>true,Plugins:{Browser:{open:async({url})=>{opened.push(url)}}}}};
 vm.runInNewContext(code,{window,document,location,URL,Set});
 const fire=(href,target='_blank')=>{let prevented=false;const anchor={href,target,hasAttribute:()=>false};click({defaultPrevented:false,button:0,metaKey:false,ctrlKey:false,shiftKey:false,altKey:false,target:{closest:()=>anchor},preventDefault:()=>{prevented=true}});return prevented};
 return {fire,opened,assigned};
}
test('same-site web link stays in native app',()=>{const x=setup();assert.equal(x.fire('https://www.roamwise.co.in/shop/?buy=hoodie'),true);assert.equal(x.assigned[0],'/shop/?buy=hoodie');});
test('explicit external link opens native browser',()=>{const x=setup();assert.equal(x.fire('https://example.com/','_blank'),true);assert.equal(x.opened[0],'https://example.com/');});
test('external provider redirect is not hijacked',()=>{const x=setup();assert.equal(x.fire('https://payments.cashfree.com/','_self'),false);assert.equal(x.opened.length,0);});
test('not a browser plugin on web',()=>{let calls=0;vm.runInNewContext(code,{window:{Capacitor:{isNativePlatform:()=>false}},document:{addEventListener:()=>calls++},URL,Set});assert.equal(calls,0);});
