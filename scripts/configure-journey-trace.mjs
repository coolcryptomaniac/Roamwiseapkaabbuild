#!/usr/bin/env node
import { copyFileSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';

const buildFile='android/app/build.gradle';
const manifestFile='android/app/src/main/AndroidManifest.xml';
const mainActivityFile='android/app/src/main/java/com/gyanverse/roamwise/MainActivity.java';
const targetDir='android/app/src/main/java/com/gyanverse/roamwise/journey';

let build=readFileSync(buildFile,'utf8');
const dep='implementation "com.google.android.gms:play-services-location:21.3.0"';
if(!build.includes(dep)){
  build=build.replace(/dependencies\s*\{/, `dependencies {\n    ${dep}`);
  writeFileSync(buildFile,build);
}

let manifest=readFileSync(manifestFile,'utf8');
const perms=[
  '<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />',
  '<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />',
  '<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />',
  '<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />'
];
for(const perm of perms){
  const name=(perm.match(/android\.permission\.([A-Z_]+)/)||[])[1];
  if(name && !manifest.includes('android.permission.'+name)){
    manifest=manifest.replace(/\n\s*<application/, '\n    '+perm+'\n    <application');
  }
}
const service='<service android:name=".journey.JourneyTraceService" android:foregroundServiceType="location" android:exported="false" android:stopWithTask="true" />';
if(!manifest.includes('.journey.JourneyTraceService')){
  manifest=manifest.replace('</application>', '        '+service+'\n    </application>');
}
writeFileSync(manifestFile,manifest);

let activity=readFileSync(mainActivityFile,'utf8');
if(!activity.includes('JourneyTracePlugin')){
  activity=activity.replace(
    /import com\.gyanverse\.roamwise\.nearby\.NearbyMeshPlugin;\n/,
    'import com.gyanverse.roamwise.nearby.NearbyMeshPlugin;\nimport com.gyanverse.roamwise.journey.JourneyTracePlugin;\n'
  );
  activity=activity.replace(
    'registerPlugin(NearbyMeshPlugin.class);',
    'registerPlugin(NearbyMeshPlugin.class);\n        registerPlugin(JourneyTracePlugin.class);'
  );
  writeFileSync(mainActivityFile,activity);
}

mkdirSync(targetDir,{recursive:true});
copyFileSync('native/journey/JourneyTracePlugin.java',targetDir+'/JourneyTracePlugin.java');
copyFileSync('native/journey/JourneyTraceService.java',targetDir+'/JourneyTraceService.java');
console.log('Configured opt-in JourneyTrace foreground location service');
