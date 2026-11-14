/* Android WebView navigation guard: same-site links stay inside the app,
 * explicit external new-tab links open Chrome Custom Tabs when available.
 * Folder links (/wallet/, /shop/) are opened as their index.html, because the app's local
 * server would otherwise answer them with the home page.
 * Never intercept Cashfree redirects, auth popups, tel/mailto or form submits.
 */
(function(){
 'use strict';
 if(!window.Capacitor||!window.Capacitor.isNativePlatform||!window.Capacitor.isNativePlatform())return;
 /* Capacitor's local server answers a directory URL such as /wallet/ or /shop/?x=1 with the
  * ROOT index.html, which then cannot find its styles or scripts: an unstyled, stuck copy of
  * the planner. Every bundled page is a real file, so always ask for it by name. */
 function pageFile(pathname){
  if(!pathname||pathname==='/'||pathname.slice(-1)!=='/')return '';
  return pathname+'index.html';
 }
 var wrong=pageFile(location.pathname);
 if(wrong){location.replace(wrong+location.search+location.hash);return;}
 var allowed=new Set(['roamwise.co.in','www.roamwise.co.in']);
 /* Keep in step with the rsync --exclude list in .github/workflows/build-android.yml. */
 var webOnly=new Set(['admin','agent','deck','funding-model','generator','invest','nmims','staff']);
 document.addEventListener('click',function(ev){
  if(ev.defaultPrevented||ev.button!==0||ev.metaKey||ev.ctrlKey||ev.shiftKey||ev.altKey)return;
  var anchor=ev.target.closest&&ev.target.closest('a[href]');if(!anchor||anchor.hasAttribute('download'))return;
  var url;try{url=new URL(anchor.href,location.href);}catch{return;}
  if(!/^https?:$/.test(url.protocol))return;
  // Never rewrite OAuth, Cashfree, banking or other external payment/auth flows.
  if(allowed.has(url.hostname)){
   if(anchor.target==='_blank'||url.origin!==location.origin){
    ev.preventDefault();
    location.assign((pageFile(url.pathname)||url.pathname)+url.search+url.hash);
   }
   return;
  }
  /* Sections that are not shipped inside the app open on the website instead. */
  if(url.origin===location.origin&&webOnly.has(url.pathname.split('/')[1])){
   ev.preventDefault();
   var site='https://www.roamwise.co.in'+url.pathname+url.search+url.hash;
   var b=window.Capacitor.Plugins&&window.Capacitor.Plugins.Browser;
   if(b&&b.open)b.open({url:site}).catch(function(){location.assign(site);});else location.assign(site);
   return;
  }
  /* Same-app link to a folder page: open its index.html directly (no unstyled flash). */
  if(url.origin===location.origin&&anchor.target!=='_blank'&&pageFile(url.pathname)){
   ev.preventDefault();
   location.assign(pageFile(url.pathname)+url.search+url.hash);
   return;
  }
  if(anchor.target!=='_blank')return; // retain provider redirects and SDK-owned navigation
  var browser=window.Capacitor.Plugins&&window.Capacitor.Plugins.Browser;
  if(!browser||!browser.open)return;
  ev.preventDefault();
  browser.open({url:url.href}).catch(function(){location.assign(url.href);});
 },true);
})();
