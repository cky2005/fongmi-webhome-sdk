package com.fongmi.web;

public final class FmHttpResponse {

    public final int status;
    public final String url;
    public final String text;
    public final String base64;
    public final String error;

    private FmHttpResponse(int status, String url, String text, String base64, String error) {
        this.status = status;
        this.url = url;
        this.text = text;
        this.base64 = base64;
        this.error = error;
    }

    public boolean ok() { return status >= 200 && status < 300; }

    public static FmHttpResponse success(int status, String url, String body) {
        return new FmHttpResponse(status, url, body, null, null);
    }

    public static FmHttpResponse successBytes(int status, String url, String base64) {
        return new FmHttpResponse(status, url, null, base64, null);
    }

    public static FmHttpResponse failure(String error) {
        return new FmHttpResponse(0, "", null, null, error);
    }

    public String body(String responseType) {
        if ("base64".equalsIgnoreCase(responseType)) return base64;
        return text;
    }
}
