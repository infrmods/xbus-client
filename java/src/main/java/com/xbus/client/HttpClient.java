package com.xbus.client;

import com.google.api.client.http.*;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.xbus.exceptions.XBusException;
import com.xbus.exceptions.ErrorCode;
import com.xbus.result.Response;
import com.xbus.result.Result;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by lolynx on 6/11/16.
 */
class HttpClient {
    private static final GsonFactory GSON_FACTORY = new GsonFactory();
    private static Random random = new Random(System.currentTimeMillis());

    private HttpRequestFactory httpRequestFactory;
    private String[] endpoints;

    void initHttp(HttpRequestFactory httpRequestFactory, String[] endpoints) {
        this.httpRequestFactory = httpRequestFactory;
        if (endpoints == null || endpoints.length == 0) {
            throw new RuntimeException("empty xbus endpoints");
        }
        this.endpoints = endpoints;
    }

    class Url extends GenericUrl {
        Url(String path) {
            super(endpoints[random.nextInt(endpoints.length)] + path);
        }
    }

    private <T extends Result> T getResult(HttpRequest request, Class<? extends Response<T>> cls) throws XBusException, IOException {
        try {
            return request.execute().parseAs(cls).getResult();
        } catch (HttpResponseException e) {
            if (e.getHeaders() != null && "application/json".equals(e.getHeaders().getContentType())) {
                String content = e.getContent();
                if (content != null && content.length() > 0) {
                    return GSON_FACTORY.fromString(content, cls).getResult();
                }
            }
            throw new XBusException(ErrorCode.IOError, e);
        }
    }

    <T extends Result> T get(GenericUrl url, Class<? extends Response<T>> cls) throws XBusException {
        try {
            return getResult(httpRequestFactory.buildGetRequest(url), cls);
        } catch (IOException e) {
            throw new XBusException(ErrorCode.IOError, e);
        }
    }

    <T extends Result> T delete(GenericUrl url, Class<? extends Response<T>> cls) throws XBusException {
        try {
            return getResult(httpRequestFactory.buildDeleteRequest(url), cls);
        } catch (IOException e) {
            throw new XBusException(ErrorCode.IOError, e);
        }
    }

    <T extends Result> T post(GenericUrl url, HttpContent content, Class<? extends Response<T>> cls) throws XBusException {
        try {
            return getResult(httpRequestFactory.buildPostRequest(url, content), cls);
        } catch (IOException e) {
            throw new XBusException(ErrorCode.IOError, e);
        }
    }

    <T extends Result> T put(GenericUrl url, HttpContent content, Class<? extends Response<T>> cls) throws XBusException {
        try {
            return getResult(httpRequestFactory.buildPutRequest(url, content), cls);
        } catch (IOException e) {
            throw new XBusException(ErrorCode.IOError, e);
        }
    }

    static class UrlFormBuilder {
        Map<String, Object> data = new HashMap<String, Object>();

        UrlFormBuilder put(String name, Object value) {
            data.put(name, value);
            return this;
        }

        UrlFormBuilder putIfNotNull(String name, Object value) {
            if (value != null) {
                data.put(name, value);
            }
            return this;
        }

        HttpContent build() {
            return new UrlEncodedContent(data);
        }
    }

    UrlFormBuilder newUrlForm() {
        return new UrlFormBuilder();
    }
}
