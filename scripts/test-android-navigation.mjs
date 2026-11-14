import test from 'node:test';import assert from 'node:assert/strict';import fs from 'node:fs';import vm from 'node:vm';
const code=fs.readFileSync('scripts/android-bridge.js','utf8');
function setup(pathname='/index.html',search='',hash=''){
 let click,opened=[],assigned=[];
 const replaced=[];const location={href:'https://localhost'+pathname+search+hash,origin:'https://localhost',pathname,search,hash,assign:u=>assigned.push(u),replace:u=>replaced.push(u)};
 const document={addEventListener:(type,fn)=>{if(type==='click')click=fn}};
 const window={Capacitor:{isNativePlatform:()=>true,Plugins:{Browser:{open:async({url})=>{opened.push(url)}}}}};
 vm.runInNewContext(code,{window,document,location,URL,Set});
 const fire=(href,target='_blank')=>{let prevented=false;const anchor={href,target,hasAttribute:()=>false};click({defaultPrevented:false,button:0,metaKey:false,ctrlKey:false,shiftKey:false,altKey:false,target:{closest:()=>anchor},preventDefault:()=>{prevented=true}});return prevented};
 return {fire,opened,assigned,replaced,hasClick:()=>typeof click==='function'};
}
test('same-site web link stays in native app',()=>{const x=setup();assert.equal(x.fire('https://www.roamwise.co.in/shop/?buy=hoodie'),true);assert.equal(x.assigned[0],'/shop/index.html?buy=hoodie');});
test('explicit external link opens native browser',()=>{const x=setup();assert.equal(x.fire('https://example.com/','_blank'),true);assert.equal(x.opened[0],'https://example.com/');});
test('external provider redirect is not hijacked',()=>{const x=setup();assert.equal(x.fire('https://payments.cashfree.com/','_self'),false);assert.equal(x.opened.length,0);});
test('not a browser plugin on web',()=>{let calls=0;vm.runInNewContext(code,{window:{Capacitor:{isNativePlatform:()=>false}},document:{addEventListener:()=>calls++},URL,Set});assert.equal(calls,0);});

test('folder links inside the app open the real page file',()=>{const x=setup();assert.equal(x.fire('https://localhost/wallet/','_self'),true);assert.equal(x.fire('https://localhost/wallet/#invite',''),true);assert.equal(x.fire('https://localhost/shop/?buy=cap',''),true);assert.deepEqual(x.assigned,['/wallet/index.html','/wallet/index.html#invite','/shop/index.html?buy=cap']);});
test('file links, the home page and app routes are left alone',()=>{const x=setup();assert.equal(x.fire('https://localhost/packages/amor-checkout.html','_self'),false);assert.equal(x.fire('https://localhost/','_self'),false);assert.equal(x.fire('https://localhost/pricing','_self'),false);assert.equal(x.assigned.length,0);});
test('a folder URL that loaded the home page by mistake is sent to its own page',()=>{const x=setup('/wallet/','?t=abc','#invite');assert.deepEqual(x.replaced,['/wallet/index.html?t=abc#invite']);assert.equal(x.hasClick(),false);const home=setup('/');assert.equal(home.replaced.length,0);assert.equal(home.hasClick(),true);});
test('sections left out of the app open on the website',()=>{const x=setup();assert.equal(x.fire('https://localhost/invest/','_self'),true);assert.equal(x.fire('https://localhost/staff/index.html',''),true);assert.deepEqual(x.opened,['https://www.roamwise.co.in/invest/','https://www.roamwise.co.in/staff/index.html']);assert.equal(x.assigned.length,0);});
