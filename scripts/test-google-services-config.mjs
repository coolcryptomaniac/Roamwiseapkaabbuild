import test from 'node:test';
import assert from 'node:assert/strict';
import {validateGoogleServices} from './validate-google-services.mjs';

const upload='FF:81:A4:C5:7E:F0:FD:0B:CB:C5:2B:A6:5F:A1:FE:12:B6:2B:E4:F8';
const play='5A:18:6E:DB:7D:4B:85:50:38:5B:DF:C7:F7:9A:26:C9:F8:AC:66:07';

function fixture({web=true,playCertificate=true}={}){
  const oauth=[{
    client_id:'android-upload.apps.googleusercontent.com',client_type:1,
    android_info:{package_name:'com.gyanverse.roamwise',certificate_hash:upload.replaceAll(':','')}
  }];
  if(playCertificate)oauth.push({
    client_id:'android-play.apps.googleusercontent.com',client_type:1,
    android_info:{package_name:'com.gyanverse.roamwise',certificate_hash:play.replaceAll(':','')}
  });
  if(web)oauth.push({client_id:'web.apps.googleusercontent.com',client_type:3});
  return {project_info:{project_id:'roamwisepro'},client:[{
    client_info:{android_client_info:{package_name:'com.gyanverse.roamwise'}},oauth_client:oauth
  }]};
}

test('accepts web, upload, and Play signing OAuth clients',()=>{
  const result=validateGoogleServices(fixture(),{
    projectId:'roamwisepro',packageName:'com.gyanverse.roamwise',requiredSha1s:[upload,play]
  });
  assert.equal(result.webClientId,'web.apps.googleusercontent.com');
  assert.equal(result.androidSha1s.length,2);
});

test('rejects a config without the web OAuth client used for ID tokens',()=>{
  assert.throws(()=>validateGoogleServices(fixture({web:false}),{
    projectId:'roamwisepro',packageName:'com.gyanverse.roamwise',requiredSha1s:[upload,play]
  }),/Web OAuth client/);
});

test('rejects a stale config missing the Play app-signing certificate',()=>{
  assert.throws(()=>validateGoogleServices(fixture({playCertificate:false}),{
    projectId:'roamwisepro',packageName:'com.gyanverse.roamwise',requiredSha1s:[upload,play]
  }),/does not contain required signing SHA-1/);
});
