package com.fongmi.web;

import androidx.annotation.Nullable;

/**
 * fm.req 调用的 HTTP 响应。
 */
public final class FmHttpResponse {

    public final int status;          // 0 表示异常
    public final String url;          // 最终 URL（重定向后）
    public final String text;         // 文本或 JSON 字符串（responseType=text/json 时）
    public final String base64;       // 原始字节 base64（responseType=base64 时）
    public final String error;        // 异常时为错误信息

    private FmHttpResponse(int status, String url, String text, String base64, String error) {
        this.status = status;
        this.url = url;
        this.text = text;
        this.base64 = base64;
        this.error = error;
    }

    public boolean ok() {
        return status >= 200 && status < 300;
    }

    public static FmHttpResponse success(int status, String url, String body) {
        return new FmHttpResponse(status, url, body, null, null);
    }

    public static FmHttpResponse successBytes(int status, String url, String base64) {
        return new FmHttpResponse(status, url, null, base64, null);
    }

    public static FmHttpResponse failure(String error) {
        return new FmHttpResponse(0, "", null, null, error);
    }

    @Nullable
    public String body(String responseType) {
        if ("base64".equalsIgnoreCase(responseType)) return base64;
        return text;
    }
}
