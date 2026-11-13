/* Android WebView navigation guard: same-site links stay inside the app,
 * explicit external new-tab links open Chrome Custom Tabs when available.
 * Never intercept Cashfree redirects, auth popups, tel/mailto or form submits.
 */
(function(){
 'use strict';
 if(!window.Capacitor||!window.Capacitor.isNativePlatform||!window.Capacitor.isNativePlatform())return;
 var allowed=new Set(['roamwise.co.in','www.roamwise.co.in']);
 document.addEventListener('click',function(ev){
  if(ev.defaultPrevented||ev.button!==0||ev.metaKey||ev.ctrlKey||ev.shiftKey||ev.altKey)return;
  var anchor=ev.target.closest&&ev.target.closest('a[href]');if(!anchor||anchor.hasAttribute('download'))return;
  var url;try{url=new URL(anchor.href,location.href);}catch{return;}
  if(!/^https?:$/.test(url.protocol))return;
  // Never rewrite OAuth, Cashfree, banking or other external payment/auth flows.
  if(allowed.has(url.hostname)){
   if(anchor.target==='_blank'||url.origin!==location.origin){
    ev.preventDefault();
    location.assign(url.pathname+url.search+url.hash);
   }
   return;
  }
  if(anchor.target!=='_blank')return; // retain provider redirects and SDK-owned navigation
  var browser=window.Capacitor.Plugins&&window.Capacitor.Plugins.Browser;
  if(!browser||!browser.open)return;
  ev.preventDefault();
  browser.open({url:url.href}).catch(function(){location.assign(url.href);});
 },true);
})();
