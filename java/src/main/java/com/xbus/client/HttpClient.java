package com.xbus.client;

import com.google.api.client.http.*;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.xbus.result.XBusException;
import com.xbus.result.ErrorCode;
import com.xbus.result.Response;
import com.xbus.result.Result;

import java.io.IOException;
import java.util.Random;

/**
 * Created by lolynx on 6/11/16.
 */
public class HttpClient {
    static final GsonFactory GSON_FACTORY = new GsonFactory();
    static Random random = new Random(System.currentTimeMillis());

    HttpRequestFactory httpRequestFactory;
    String[] endpoints;

    void initHttp(HttpTransport httpTransport, String[] endpoints) {
        httpRequestFactory = httpTransport.createRequestFactory(new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
                request.setParser(new JsonObjectParser(GSON_FACTORY));
            }
        });
        if (endpoints == null || endpoints.length == 0) {
            throw new RuntimeException("empty xbus endpoints");
        }
        this.endpoints = endpoints;
    }

    class Url extends GenericUrl {
        public Url(String path) {
            super(endpoints[random.nextInt(endpoints.length)] + path);
        }
    }

    protected <T extends Result> T get(GenericUrl url, Class<? extends Response<T>> cls) throws XBusException {
        Response<T> response;
        try {
            HttpRequest request = httpRequestFactory.buildGetRequest(url);
            response = request.execute().parseAs(cls);
        } catch (IOException e) {
            throw new XBusException(ErrorCode.IOError, e);
        }
        return response.getResult();
    }

    protected <T extends Result> T delete(GenericUrl url, Class<? extends Response<T>> cls) throws XBusException {
        Response<T> response;
        try {
            HttpRequest request = httpRequestFactory.buildDeleteRequest(url);
            response = request.execute().parseAs(cls);
        } catch (IOException e) {
            throw new XBusException(ErrorCode.IOError, e);
        }
        return response.getResult();
    }

    protected <T extends Result> T post(GenericUrl url, HttpContent content, Class<? extends Response<T>> cls) throws XBusException {
        Response<T> response;
        try {
            HttpRequest request = httpRequestFactory.buildPostRequest(url, content);
            response = request.execute().parseAs(cls);
        } catch (IOException e) {
            throw new XBusException(ErrorCode.IOError, e);
        }
        return response.getResult();
    }

    protected <T extends Result> T put(GenericUrl url, HttpContent content, Class<? extends Response<T>> cls) throws XBusException {
        Response<T> response;
        try {
            HttpRequest request = httpRequestFactory.buildPutRequest(url, content);
            response = request.execute().parseAs(cls);
        } catch (IOException e) {
            throw new XBusException(ErrorCode.IOError, e);
        }
        return response.getResult();
    }
}
