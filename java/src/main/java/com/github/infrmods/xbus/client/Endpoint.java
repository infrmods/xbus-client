package com.github.infrmods.xbus.client;

/**
 * Created by lo on 4/7/17.
 */
public class Endpoint {
    private String host;
    private Integer port;

    public Endpoint(String s) {
        if (s.startsWith("https://")) {
            s = s.substring("https://".length());
        }
        String[] parts = s.split(":", 2);
        host = parts[0];
        if (parts.length == 2) {
            port = Integer.valueOf(parts[1]);
        }
    }

    public static Endpoint[] convert(String endpoints[]) {
        Endpoint[] result = new Endpoint[endpoints.length];
        for (int i = 0; i < endpoints.length; i++) {
            result[i] = new Endpoint(endpoints[i]);
        }
        return result;
    }

    public Endpoint(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
