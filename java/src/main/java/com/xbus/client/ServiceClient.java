package com.xbus.client;

import com.xbus.exceptions.XBusException;
import com.xbus.item.Service;

/**
 * Created by lolynx on 6/14/16.
 */
interface ServiceClient {
    Service getService(String name, String version) throws XBusException;

    Service watchService(String name, String version) throws XBusException;

    Service watchService(String name, String version, String timeout) throws XBusException;

    void plugService(Service service) throws XBusException;

    void plugService(Service service, Long ttl) throws XBusException;

    void unplugService(String name, String version) throws XBusException;

    void keepService(String name, String version) throws XBusException;

    void updateServiceConfig(String name, String version, String config) throws XBusException;
}
