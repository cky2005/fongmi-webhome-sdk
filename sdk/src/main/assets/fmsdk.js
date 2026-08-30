/* FongMi WebHome SDK — injected into WebView as window.fm / window.fongmi
 * Compatible with the fongmi/fengmi WebHome SDK contract.
 * Designed to work on Android 7+ (Chromium 51+ baseline) — no ?. ?? ??= etc.
 */
(function () {
  if (window.__fmSdkInjected) return;
  window.__fmSdkInjected = true;

  // -------- helpers --------
  function quote(s) {
    s = s == null ? '' : String(s);
    return '"' + s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\n/g, '\\n').replace(/\r/g, '\\r') + '"';
  }
  function safeJson(s) {
    if (s == null) return null;
    try { return JSON.parse(s); } catch (e) { return null; }
  }
  function extend(target, source) {
    if (!source) return target;
    for (var k in source) if (Object.prototype.hasOwnProperty.call(source, k)) target[k] = source[k];
    return target;
  }
  function headersToObject(h) {
    var o = {};
    if (!h) return o;
    if (typeof Headers !== 'undefined' && h instanceof Headers) {
      h.forEach(function (v, k) { o[k] = v; });
    } else {
      for (var k in h) o[k] = h[k];
    }
    return o;
  }
  function isAbsoluteUrl(s) {
    return /^https?:\/\//i.test(s || '');
  }
  function responseOf(raw) {
    // bridge 返回 { ok, status, url, body, error?, headers? }
    var o = safeJson(raw);
    if (!o) {
      return { status: 0, text: '', bytes: null, ok: false, error: 'bad bridge response' };
    }
    var body = o.body || '';
    var bytes;
    if (body && /^data:application\/octet-stream;base64,/.test(body)) {
      try { bytes = atob(body.split(',', 2)[1]); } catch (e) { bytes = body; }
    } else if (o.isBase64) {
      try { bytes = atob(body); } catch (e) { bytes = body; }
    }
    var text = bytes != null
      ? (function () { try { return new TextDecoder('utf-8').decode(new Uint8Array(bytes.length)); } catch (e) { return bytes; } })()
      : body;
    return {
      status: o.status || 0,
      url: o.url || '',
      text: text,
      bytes: bytes,
      json: function () { return safeJson(text); },
      ok: o.ok === true || (o.status >= 200 && o.status < 300),
      error: o.error || ''
    };
  }

  // -------- callback / hydrate --------
  var callbacks = {};
  var seq = 0;
  function invoke(method, payload) {
    return new Promise(function (resolve, reject) {
      var id = 'fm_' + Date.now() + '_' + (++seq);
      callbacks[id] = { resolve: resolve, reject: reject };
      try {
        window.fongmiBridge.invoke(id, method, JSON.stringify(payload || {}));
      } catch (e) {
        delete callbacks[id];
        reject(e);
      }
    });
  }
  function hydrate(data) {
    if (!data || !data.__fmResultId) return data;
    var id = data.__fmResultId;
    var total = window.fongmiBridge.resultLength(id);
    var text = '';
    for (var s = 0; s < total; s += 60000) text += window.fongmiBridge.resultChunk(id, s);
    window.fongmiBridge.clearResult(id);
    return safeJson(text);
  }

  window.fongmiNative = {
    resolve: function (id, data) {
      var c = callbacks[id];
      if (!c) return;
      delete callbacks[id];
      try { c.resolve(hydrate(data)); } catch (e) { c.reject(e); }
    },
    reject: function (id, err) {
      var c = callbacks[id];
      if (!c) return;
      delete callbacks[id];
      c.reject(new Error(err || ''));
    }
  };

  // -------- history hook --------
  if (!window.__fmUrlHook && window.history) {
    window.__fmUrlHook = true;
    var emit = function () {
      try { window.dispatchEvent(new CustomEvent('fmurlchange', { detail: { url: location.href } })); }
      catch (e) {}
    };
    var rawPush = history.pushState;
    var rawReplace = history.replaceState;
    history.pushState = function () { var r = rawPush.apply(this, arguments); emit(); return r; };
    history.replaceState = function () { var r = rawReplace.apply(this, arguments); emit(); return r; };
    window.addEventListener('popstate', emit);
  }

  // -------- net --------
  var net = {
    request: function (url, options) {
      options = options || {};
      return invoke('net.request', extend({ url: url }, options))
        .then(responseOf);
    },
    resourceUrl: function (url, options) {
      return window.fongmiBridge.resourceUrl(url, JSON.stringify(options || {}));
    }
  };

  // -------- player --------
  var player = {
    playUrl: function (url, title, options) {
      return invoke('player.playUrl', extend({}, options || {}, { url: url || '', title: title || '' }));
    },
    playVod: function (siteKey, vodId, title, pic, options) {
      return invoke('player.playVod', extend({}, options || {}, {
        siteKey: siteKey || '', vodId: vodId || '',
        title: title || '', pic: pic || ''
      }));
    },
    playVodInline: function (payload) {
      return invoke('player.playVodInline', payload || {});
    },
    preloadArtwork: function (pic, wallPic) {
      return invoke('player.preloadArtwork', { pic: pic || '', wallPic: wallPic || '' });
    },
    control: function (action) {
      return invoke('player.control', { action: action || '' });
    },
    status: function () {
      return invoke('player.status', {});
    }
  };

  // -------- cache --------
  var cache = {
    get: function (key, rule) { return invoke('cache.get', { key: key || '', rule: rule || '' }); },
    set: function (key, value, rule) { return invoke('cache.set', { key: key || '', value: value == null ? '' : value, rule: rule || '' }); },
    del: function (key, rule) { return invoke('cache.del', { key: key || '', rule: rule || '' }); }
  };

  // -------- pan --------
  var pan = {
    check: function (items) { return invoke('pan.check', { items: items || [] }); },
    play: function (payload) { return invoke('pan.play', payload || {}); }
  };

  // -------- ext --------
  var ext = {
    info: function () { return invoke('ext.info', {}); },
    log: function (message, data) { return invoke('ext.log', { message: message || '', data: data == null ? '' : (typeof data === 'string' ? data : JSON.stringify(data)) }); },
    toast: function (message) { return invoke('ext.toast', { message: message || '' }); }
  };

  // -------- ui --------
  var ui = {
    setToolbar: function (visible) { return invoke('ui.setToolbar', { visible: visible !== false }); },
    setChrome: function (options) { return invoke('ui.setChrome', options || {}); },
    restoreChrome: function () { return invoke('ui.restoreChrome', {}); },
    getViewport: function () { return invoke('ui.getViewport', {}); }
  };

  // -------- app --------
  var app = {
    search: function (keyword, options) { return invoke('app.search', extend({}, options || {}, { keyword: keyword || '' })); },
    openVod: function () { return invoke('app.openVod', {}); },
    openLive: function () { return invoke('app.openLive', {}); },
    openKeep: function () { return invoke('app.openKeep', {}); },
    openSetting: function () { return invoke('app.openSetting', {}); },
    history: function () { return invoke('app.history', {}); }
  };

  // -------- device/site/config --------
  var device = { info: function () { return invoke('device.info', {}); } };
  var site   = { info: function () { return invoke('site.info', {}); } };
  var config = { info: function () { return invoke('config.info', {}); } };

  // -------- navigation --------
  var navigation = {
    back:   function () { return invoke('navigation.back', {}); },
    reload: function () { return invoke('navigation.reload', {}); }
  };

  // -------- exposed --------
  window.fongmiClient = window.fongmiClient || { mode: 'normal', isLeanback: false };

  // Short alias
  window.fm = {
    req: net.request,
    res: net.resourceUrl,
    play: player.playUrl,
    vod: player.playVod,
    vodInline: player.playVodInline,
    preloadArtwork: player.preloadArtwork,
    ctrl: player.control,
    stat: player.status,
    search: app.search,
    openVod: app.openVod,
    openLive: app.openLive,
    openKeep: app.openKeep,
    openSetting: app.openSetting,
    history: app.history,
    pan: { check: pan.check, play: pan.play },
    check: pan.check,
    cache: cache,
    ext: ext,
    ui: ui,
    device: device.info,
    site: site.info,
    config: config.info,
    back: navigation.back,
    reload: navigation.reload
  };

  // Full namespace
  window.fongmi = {
    invoke: invoke,
    net: net,
    player: player,
    cache: cache,
    pan: pan,
    ext: ext,
    ui: ui,
    app: app,
    device: device,
    site: site,
    config: config,
    navigation: navigation
  };
  window.FM = window.fongmi;

  // Mark native
  if (document.documentElement) document.documentElement.classList.add('fm-native');

  // Dispatch ready event
  try { window.dispatchEvent(new CustomEvent('fmsdk')); } catch (e) {}
})();
