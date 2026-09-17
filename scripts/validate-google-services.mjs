import fs from 'node:fs';
import {pathToFileURL} from 'node:url';

function normalizeSha1(value=''){
  return String(value).replace(/[^a-fA-F0-9]/g,'').toUpperCase();
}

export function validateGoogleServices(config,{projectId,packageName,requiredSha1s=[]}){
  if(!config||config.project_info?.project_id!==projectId){
    throw new Error(`google-services.json must be from Firebase project ${projectId}`);
  }
  const clients=Array.isArray(config.client)?config.client:[];
  const appClients=clients.filter(client=>client?.client_info?.android_client_info?.package_name===packageName);
  if(!appClients.length)throw new Error(`google-services.json has no Android app for ${packageName}`);

  const oauthClients=appClients.flatMap(client=>Array.isArray(client.oauth_client)?client.oauth_client:[]);
  const webClient=oauthClients.find(client=>client?.client_type===3&&client.client_id);
  if(!webClient){
    throw new Error('google-services.json has no Web OAuth client (client_type 3); default_web_client_id would be missing');
  }

  const androidSha1s=new Set(oauthClients
    .filter(client=>client?.client_type===1&&client?.android_info?.package_name===packageName)
    .map(client=>normalizeSha1(client.android_info.certificate_hash))
    .filter(Boolean));
  if(!androidSha1s.size){
    throw new Error(`google-services.json has no Android OAuth certificate for ${packageName}; add the signing SHA-1 in Firebase and download it again`);
  }

  for(const required of requiredSha1s.map(normalizeSha1).filter(Boolean)){
    if(!androidSha1s.has(required)){
      throw new Error(`google-services.json does not contain required signing SHA-1 ${required.match(/.{2}/g).join(':')}`);
    }
  }
  return {webClientId:webClient.client_id,androidSha1s:[...androidSha1s]};
}

function readFlag(args,name){
  const index=args.indexOf(name);
  return index>=0?args[index+1]:'';
}
function readFlags(args,name){
  const values=[];
  for(let i=0;i<args.length;i++)if(args[i]===name)values.push(args[i+1]||'');
  return values;
}

if(process.argv[1]&&import.meta.url===pathToFileURL(process.argv[1]).href){
  const args=process.argv.slice(2);
  const file=args[0];
  const projectId=readFlag(args,'--project');
  const packageName=readFlag(args,'--package');
  if(!file||!projectId||!packageName){
    console.error('Usage: node scripts/validate-google-services.mjs <file> --project <id> --package <name> [--sha1 <fingerprint> ...]');
    process.exit(2);
  }
  try{
    const config=JSON.parse(fs.readFileSync(file,'utf8'));
    const result=validateGoogleServices(config,{projectId,packageName,requiredSha1s:readFlags(args,'--sha1')});
    console.log(`Validated native Google OAuth: Web client present; ${result.androidSha1s.length} Android signing certificate(s) present.`);
  }catch(error){
    console.error(`Native Google OAuth configuration error: ${error.message}`);
    process.exit(1);
  }
}
