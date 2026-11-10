/* Sync current public RoamWise site into the actual APK/AAB builder.
 * Do not sync secrets or backend code. Fail closed when source checkout fails.
 */
const fs=require('node:fs'),os=require('node:os'),path=require('node:path'),cp=require('node:child_process');
const root=path.resolve(__dirname,'..'),www=path.join(root,'www'),tmp=fs.mkdtempSync(path.join(os.tmpdir(),'roamwise-web-'));
const source=process.env.ROAMWISE_WEB_SOURCE||'https://github.com/coolcryptomaniac/roamwise.git';
function run(cmd,args,cwd){cp.execFileSync(cmd,args,{cwd,stdio:'inherit',timeout:180000});}
try{
 run('git',['clone','--depth','1','--branch','main',source,tmp],root);
 run(process.execPath,['tools/build-android-www.cjs'],tmp);
 const from=path.join(tmp,'www');
 for(const f of ['index.html','app.js','shop/index.html','shop/checkout.js','embed/ailon-tusk.js','packages/index.html','treks/index.html'])if(!fs.existsSync(path.join(from,f)))throw Error('Missing critical web asset: '+f);
 fs.mkdirSync(www,{recursive:true});
 const manifest=path.join(www,'.roamwise-synced.json');
 let old=[];try{old=JSON.parse(fs.readFileSync(manifest,'utf8'));}catch{}
 for(const f of old){if(typeof f==='string'&&!f.includes('..')&&!path.isAbsolute(f))fs.rmSync(path.join(www,f),{force:true});}
 const files=[];
 function copy(dir,rel=''){for(const ent of fs.readdirSync(dir,{withFileTypes:true})){const next=path.join(rel,ent.name),src=path.join(dir,ent.name);if(ent.isDirectory())copy(src,next);else if(ent.isFile()){const dst=path.join(www,next);fs.mkdirSync(path.dirname(dst),{recursive:true});fs.copyFileSync(src,dst);files.push(next);}}}
 copy(from);
 const html=path.join(www,'index.html');
 let text=fs.readFileSync(html,'utf8');
 const tag='<script defer src="/android-bridge.js"></script>';
 if(!text.includes('android-bridge.js'))text=text.replace('</head>',tag+'\n</head>');
 fs.writeFileSync(html,text);
 fs.copyFileSync(path.join(root,'scripts/android-bridge.js'),path.join(www,'android-bridge.js'));
 files.push('android-bridge.js');
 fs.writeFileSync(manifest,JSON.stringify(files));
 console.log('Synced',files.length,'current RoamWise assets into Android app');
}finally{fs.rmSync(tmp,{recursive:true,force:true});}
