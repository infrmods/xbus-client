package com.xbus.client;

import com.xbus.result.XBusException;
import com.xbus.result.GetConfigResult;

/**
 * Created by lolynx on 6/11/16.
 */
public class ConfigClient extends HttpClient {
    public GetConfigResult getConfig(String name) throws XBusException {
        return get(new Url("/api/configs/" + name), GetConfigResult.RESPONSE.class);
    }
}
