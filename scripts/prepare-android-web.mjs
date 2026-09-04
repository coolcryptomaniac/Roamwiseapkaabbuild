#!/usr/bin/env node
import { copyFileSync, readFileSync, writeFileSync } from 'node:fs';

const SITE_ORIGIN = 'https://www.roamwise.co.in';
const edits = [
  {
    file: 'www/app.js',
    pattern: /node\.src\s*=\s*['"]assets\/audio\/['"]\s*\+\s*base\s*\+\s*_rwCueFormat\s*;/,
    replacement: "node.src = '__SITE__/assets/audio/'+base+_rwCueFormat;",
    label: 'event audio'
  },
  {
    file: 'www/platform-v5/audio-only.js',
    pattern: /var AMBIENT_BASE\s*=\s*['"]assets\/audio\/ambient-theme-30s['"]\s*;/,
    replacement: "var AMBIENT_BASE = '__SITE__/assets/audio/ambient-theme-30s';",
    label: 'ambient audio'
  },
  {
    file: 'www/itinerary-library/preset-loader.js',
    pattern: /pdf\s*:\s*ROOT\s*\+\s*v\.pdf/,
    replacement: "pdf:new URL(v.pdf,'__SITE__/itinerary-library/').href",
    label: 'preset PDFs'
  }
];

for (const { file, pattern, replacement, label } of edits) {
  const source = readFileSync(file, 'utf8');
  if (!pattern.test(source)) {
    throw new Error(`Could not route ${label} on demand in ${file}; source layout changed`);
  }
  const output = source.replace(pattern, replacement.replace('__SITE__', SITE_ORIGIN));
  writeFileSync(file, output);
  console.log(`Routed ${label} to the web origin from ${file}`);
}

copyFileSync('native/nearby/nearby-mesh.js', 'www/nearby-mesh.js');
const indexFile = 'www/index.html';
const index = readFileSync(indexFile, 'utf8');
if (!index.includes('nearby-mesh.js')) {
  if (!index.includes('</body>')) throw new Error('Could not install Nearby mesh bridge: </body> missing');
  writeFileSync(indexFile, index.replace('</body>', '  <script src="nearby-mesh.js" defer></script>\n</body>'));
}
console.log('Installed the opt-in Nearby trekking mesh web bridge');
