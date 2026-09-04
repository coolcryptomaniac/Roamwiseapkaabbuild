(function () {
  'use strict';

  function plugin() {
    return window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.NearbyMesh;
  }

  async function requirePlugin() {
    var p = plugin();
    if (!p) throw new Error('Nearby trekking mesh is available only in the RoamWise Android app.');
    return p;
  }

  window.RWNearbyMesh = {
    isAvailable: function () { return Boolean(plugin()); },
    requestPermissions: async function () { return (await requirePlugin()).requestMeshPermissions(); },
    start: async function (displayName) {
      return (await requirePlugin()).start({ displayName: String(displayName || 'RoamWise trekker').slice(0, 32) });
    },
    accept: async function (endpointId) { return (await requirePlugin()).acceptConnection({ endpointId: endpointId }); },
    reject: async function (endpointId) { return (await requirePlugin()).rejectConnection({ endpointId: endpointId }); },
    send: async function (message, endpointId) {
      return (await requirePlugin()).sendMessage({ message: message, endpointId: endpointId || null });
    },
    status: async function () { return (await requirePlugin()).getStatus(); },
    stop: async function () { return (await requirePlugin()).stop(); },
    on: async function (eventName, listener) { return (await requirePlugin()).addListener(eventName, listener); }
  };
})();
