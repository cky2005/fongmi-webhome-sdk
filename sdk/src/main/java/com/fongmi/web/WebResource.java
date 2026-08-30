package com.fongmi.web;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

public final class WebResource {

    private static volatile int overridePort = -1;

    private WebResource() {}

    public static void setPort(int port) {
        overridePort = port;
    }

    public static int portOrDefault(Context ctx) {
        if (overridePort > 0) return overridePort;
        try {
            Class<?> cls = Class.forName("com.github.catvod.utils.LocalServer");
            Object port = cls.getMethod("getPort").invoke(null);
            if (port instanceof Integer) return (Integer) port;
        } catch (Throwable ignored) {}
        return 9978;
    }

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
