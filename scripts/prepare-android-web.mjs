#!/usr/bin/env node
import { copyFileSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const SITE_ORIGIN = 'https://www.roamwise.co.in';
const edits = [
  {
    file: 'www/js/audio/cues.js',
    pattern: /var source\s*=\s*['"]assets\/audio\/['"]\s*\+\s*base\s*\+\s*_rwCueFormat\s*;/,
    replacement: "var source = '__SITE__/assets/audio/'+base+_rwCueFormat;",
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
  const routed = replacement.replace('__SITE__', SITE_ORIGIN);
  if (!pattern.test(source)) {
    if (source.includes(routed)) {
      console.log(`Already routed ${label} to the web origin in ${file}`);
      continue;
    }
    throw new Error(`Could not route ${label} on demand in ${file}; source layout changed`);
  }
  const output = source.replace(pattern, routed);
  writeFileSync(file, output);
  console.log(`Routed ${label} to the web origin from ${file}`);
}

// Capacitor's local asset server treats directory URLs such as /guides/ as
// app-shell routes and falls back to index.html. That made the Android-only
// "Browse 36 Destination Guides" action reopen an unstyled copy of the
// planner. Content pages live on the public web origin (where their canonical
// styles, media and fresh SEO updates are deployed), so route every content
// link there while leaving app routes such as / and /pricing untouched.
function htmlFiles(directory) {
  return readdirSync(directory).flatMap((name) => {
    const path = join(directory, name);
    return statSync(path).isDirectory()
      ? htmlFiles(path)
      : path.endsWith('.html') ? [path] : [];
  });
}

let routedContentLinks = 0;
let existingRoutedContentLinks = 0;
for (const file of htmlFiles('www')) {
  const source = readFileSync(file, 'utf8');
  existingRoutedContentLinks += (source.match(/href=(['"])https:\/\/www\.roamwise\.co\.in\/(guides|blog|trips)(?:\/[^'"#?]*)?(?:[?#][^'"]*)?\1/gi) || []).length;
  const output = source.replace(
    /href=(['"])\/(guides|blog|trips)(\/[^'"#?]*)?([?#][^'"]*)?\1/gi,
    (_match, quote, section, path = '/', suffix = '') => {
      routedContentLinks += 1;
      return `href=${quote}${SITE_ORIGIN}/${section}${path}${suffix}${quote}`;
    }
  );
  if (output !== source) writeFileSync(file, output);
}
if (!routedContentLinks) {
  if (existingRoutedContentLinks) console.log(`Android content links already routed (${existingRoutedContentLinks})`);
  else throw new Error('Could not route Android content links; no guide/blog/trip links were found');
} else {
  console.log(`Routed ${routedContentLinks} guide/blog/trip links to ${SITE_ORIGIN}`);
}

copyFileSync('native/nearby/nearby-mesh.js', 'www/nearby-mesh.js');
const indexFile = 'www/index.html';
const index = readFileSync(indexFile, 'utf8');
if (!index.includes('nearby-mesh.js')) {
  if (!index.includes('</body>')) throw new Error('Could not install Nearby mesh bridge: </body> missing');
  writeFileSync(indexFile, index.replace('</body>', '  <script src="nearby-mesh.js" defer></script>\n</body>'));
}
console.log('Installed the opt-in Nearby trekking mesh web bridge');
