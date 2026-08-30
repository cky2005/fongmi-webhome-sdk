# FongMi WebHome SDK — Complete API Reference

This document lists every method the SDK exposes to JavaScript, mapped to the underlying bridge call and the corresponding shell-side action handler method.

## 1. Network

### `fm.req(url, options)`

| Parameter | Type | Description |
|---|---|---|
| `url` | string | Request URL |
| `options.method` | string | GET/POST/PUT/PATCH/DELETE/HEAD/OPTIONS (default GET) |
| `options.headers` | object | Request headers |
| `options.body` | string | Request body (must be stringified already) |
| `options.responseType` | string | `text` / `json` / `base64` (default `text`) |
| `options.timeout` | number | Seconds (default 30) |
| `options.credentials` | string | `include` to attach WebView cookie |

Returns a Promise resolving to:
```js
{
    status: 200,        // HTTP status, 0 on error
    url: 'final url',   // After redirects
    text: '...',        // response body as text
    json: function,     // returns parsed JSON or null
    bytes: ...,         // when responseType=base64
    ok: true,           // status 2xx
    error: '...'        // error message if any
}
```

Java side: `FmActionHandler.http(url, method, headers, body, responseType, timeout, includeCookie)` → `FmHttpResponse`

### `fm.res(url, options)`

Generates a local `/webResource?url=...` URL for use as `<img src>` or `video.src`.

| Parameter | Type | Description |
|---|---|---|
| `url` | string | Original media URL |
| `options.headers` | object | Headers to send (e.g. Referer) |
| `options.credentials` | string | `include` to attach Cookie |

Returns string (not Promise). Java side: `FmActionHandler.resourceUrl(url, options)`.

## 2. Player

### `fm.play(url, title, options)`

Play a direct media URL. Java side: `FmActionHandler.playUrl(url, title, options)`.

### `fm.vod(siteKey, vodId, title, pic, options)`

Play a CSP VOD entry. Java side: `FmActionHandler.playVod(...)`.

### `fm.vodInline(payload)`

Play inline multi-episode. payload is:
```js
{
    vod_id, vod_name, vod_pic, wallPic, vod_play_from, mark,
    episodes: [{name, url, resolve?, pageUrl?}, ...]
}
```

Java side: `FmActionHandler.playVodInline(payload)`.

### `fm.preloadArtwork(pic, wallPic)`

Preload images for smoother playback. Java side: `FmActionHandler.preloadArtwork(...)`.

### `fm.ctrl(action)`

action: `play` / `pause` / `stop` / `prev` / `next` / `loop` / `replay`. Java side: `FmActionHandler.controlPlayer(action)`.

### `fm.stat()`

Returns `{state, speed, duration, position, url, title, artist, artwork}`. Java side: `FmActionHandler.playerStatus()`.

## 3. App

| Method | Java side |
|---|---|
| `fm.search(keyword, options)` | `FmActionHandler.search(...)` |
| `fm.openVod()` | `FmActionHandler.openVod()` |
| `fm.openLive()` | `FmActionHandler.openLive()` |
| `fm.openKeep()` | `FmActionHandler.openKeep()` |
| `fm.openSetting()` | `FmActionHandler.openSetting()` |
| `fm.history()` | `FmActionHandler.history()` |
| `fm.back()` | `FmActionHandler.navigationBack()` |
| `fm.reload()` | `FmActionHandler.navigationReload()` |

## 4. Cache

Native string-based persistence, isolated from WebView's `localStorage`.

```js
await fm.cache.set('user', 'alice', 'auth');     // stores as 'cache_auth_user'
const v = await fm.cache.get('user', 'auth');     // 'alice'
await fm.cache.del('user', 'auth');
```

Java side: `FmActionHandler.cacheGet/Set/Del(key, value, rule)`.

## 5. UI

| Method | Description |
|---|---|
| `fm.ui.setToolbar(visible)` | Show/hide native toolbar |
| `fm.ui.setChrome({mode, ...})` | Set chrome mode: `normal`/`edge`/`immersive`/`tv-normal`/`tv-toolbar-hidden`/`tv-overlay`/`tv-full` |
| `fm.ui.restoreChrome()` | Restore chrome to the previous mode |
| `fm.ui.getViewport()` | Get current viewport dimensions and safe areas |

Java side: `FmActionHandler.setToolbar/setChrome/restoreChrome/getViewport()`.

## 6. Device / Site / Config

```js
const device = await fm.device();
// { uuid, name, ip, type: 0|1, time, serial, eth, wlan }

const site = await fm.site();
// { key, name, homePage, type, header }

const config = await fm.config();
// { id, url, desc, driveCheck }
```

Java side: `FmActionHandler.deviceInfo/siteInfo/configInfo()`.

## 7. Extensions

For WebHome user scripts. Shell can implement to expose extension status.

```js
const info = await fm.ext.info();
// { siteKey, siteName, homePage, enabled, matched, ready }
await fm.ext.log('message', { foo: 1 });
await fm.ext.toast('Hello');
```

Java side: `FmActionHandler.extInfo/extLog/extToast()`.

## 8. Pan (Network Disk)

### `fm.pan.check(items)`

```js
const r = await fm.pan.check([
    { type: 'quark', url: 'https://pan.quark.cn/s/xxx', password: '' },
    { type: 'baidu', url: 'https://pan.baidu.com/s/xxx', password: 'abcd' }
]);
// r = { results: [{type, url, normalized_url, state, cache_hit, ...}] }
```

Java side: `FmActionHandler.panCheck(payload)`.

### `fm.pan.play(payload)`

Play network disk / magnet / ed2k / thunder / push URL. Java side: `FmActionHandler.panPlay(payload)`.

## 9. Events

| Event | When |
|---|---|
| `fmsdk` | After SDK is loaded and ready |
| `fmurlchange` | After `history.pushState`/`replaceState`/`popstate` |
| `popstate` | Browser navigation |

## 10. Inline Resolver (advanced)

When `fm.vodInline` is called with `episodes[i].resolve = true`, the native player page calls `window.__fmWebHomeInlineResolver(episode)` in the WebView when the user picks an episode. The resolver must return:

```js
{ url: '...', format: 'application/x-mpegURL', headers: {...}, credentials: 'include' }
```

The bridge waits up to 20 seconds for the resolver to return.
