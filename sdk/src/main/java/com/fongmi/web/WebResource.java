package com.fongmi.web;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

/**
 * webResource 资源代理 URL 生成器。
 * <p>
 * fongmi/fengmi 系壳在 9978~9998 端口范围内启动 NanoHTTPD。
 * 本工具类:
 * <ol>
 *   <li>优先用壳注入的端口（{@link #setPort(int)}）</li>
 *   <li>否则尝试从 catvod Spider 的 LocalServer 探测</li>
 *   <li>都没拿到就回退默认 9978</li>
 * </ol>
 * 实际 GET /webResource?url=... 的 handler 是壳内置的 NanoHTTPD 提供，
 * SDK 不实现该 endpoint。
 */
public final class WebResource {

    private static volatile int overridePort = -1;

    private WebResource() {}

    /** 壳显式告诉 SDK 当前 NanoHTTPD 端口。 */
    public static void setPort(int port) {
        overridePort = port;
    }

    /**
     * 取当前实际端口。
     * @param ctx 可选，用于探测
     */
    public static int portOrDefault(Context ctx) {
        if (overridePort > 0) return overridePort;
        // 探测: 尝试连接 9978~9998
        if (ctx != null) {
            int port = probePort();
            if (port > 0) {
                overridePort = port;
                return port;
            }
        }
        return 9978;
    }

    private static int probePort() {
        // 通过反射尝试读取 catvod 内部 LocalServer.getPort()
        try {
            Class<?> cls = Class.forName("com.github.catvod.utils.LocalServer");
            Object port = cls.getMethod("getPort").invoke(null);
            if (port instanceof Integer) return (Integer) port;
        } catch (Throwable ignored) {}
        return -1;
    }

    /**
     * 生成 /webResource URL 字符串。
     *
     * @param url 原始资源 URL
     * @param options 包含 headers 和 credentials 字段
     */
    public static String wrap(Context ctx, String url, JSONObject options) {
        if (TextUtils.isEmpty(url)) return url;
        StringBuilder sb = new StringBuilder();
        sb.append("http://127.0.0.1:").append(portOrDefault(ctx)).append("/webResource?url=")
          .append(encode(url));
        if (options != null) {
            if (options.has("headers")) {
                String headers;
                try {
                    headers = options.get("headers").toString();
                } catch (JSONException e) {
                    headers = options.optString("headers");
                }
                if (!TextUtils.isEmpty(headers)) {
                    sb.append("&headers=").append(encode(headers));
                }
            }
            if ("include".equalsIgnoreCase(options.optString("credentials"))) {
                sb.append("&credentials=include");
            }
        }
        return sb.toString();
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
