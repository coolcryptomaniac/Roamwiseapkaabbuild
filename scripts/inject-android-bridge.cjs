/* Inject the Android-only navigation bridge into every copied HTML page.
 * The app navigates between distinct HTML documents, not a single SPA.
 */
const fs=require('node:fs'),path=require('node:path');const root=path.resolve('www');
const bridge='<script defer src="/android-bridge.js"></script>';
let updated=0,scanned=0;
function walk(dir){for(const ent of fs.readdirSync(dir,{withFileTypes:true})){const p=path.join(dir,ent.name);if(ent.isDirectory())walk(p);else if(ent.isFile()&&/\.html?$/i.test(ent.name)){scanned++;let s=fs.readFileSync(p,'utf8');if(s.includes('android-bridge.js'))continue;if(!/<\/head\s*>/i.test(s))continue;s=s.replace(/<\/head\s*>/i,bridge+'\n</head>');fs.writeFileSync(p,s);updated++;}}}
/* The build workflow refreshes www/ from the website without sync-roamwise-web.cjs, so install the bridge file here too. */
if(!fs.existsSync(path.join(root,'android-bridge.js'))&&fs.existsSync(path.resolve('scripts/android-bridge.js')))fs.copyFileSync(path.resolve('scripts/android-bridge.js'),path.join(root,'android-bridge.js'));
if(!fs.existsSync(path.join(root,'android-bridge.js')))throw Error('Android bridge missing');walk(root);
for(const p of ['index.html','shop/index.html','packages/index.html','treks/index.html']){const f=path.join(root,p);if(!fs.existsSync(f)||!fs.readFileSync(f,'utf8').includes('android-bridge.js'))throw Error('Android bridge missing on '+p);}
console.log('Android navigation bridge on',scanned,'HTML pages;',updated,'updated');
