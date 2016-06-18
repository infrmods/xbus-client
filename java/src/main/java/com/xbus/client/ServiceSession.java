package com.xbus.client;

import com.xbus.exceptions.ErrorCode;
import com.xbus.exceptions.XBusException;
import com.xbus.item.ServiceDesc;
import com.xbus.item.ServiceEndpoint;

/**
 * Created by lolynx on 6/18/16.
 */
public class ServiceSession {
    public static final int MIN_TTL = 10;
    public static final int DEFAULT_TTL = 60;
    private XBusClient client;
    private int ttl;
    private Long leaseId = null;
    private volatile boolean stopFlag = false;

    public ServiceSession(XBusClient client) {
        this(client, DEFAULT_TTL);
    }

    public ServiceSession(final XBusClient client, final int ttl) {
        if (ttl < MIN_TTL) {
            throw new RuntimeException("ttl too small: " + ttl);
        }
        this.client = client;
        this.ttl = ttl;

        new Thread(new Runnable() {
            @Override
            public void run() {
                int sleepInterval = ttl - 5;
                while (!stopFlag) {
                    try {
                        keepAlive();
                    } catch (XBusException e) {
                        // TODO log
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(sleepInterval * 1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }).start();
    }

    public void plugService(ServiceDesc desc, ServiceEndpoint endpoint) throws XBusException {
        Long retLeaseId = client.plugService(desc, endpoint, ttl);
        if (leaseId == null ) {
            leaseId = retLeaseId;
        } else if (!leaseId.equals(retLeaseId)) {
            throw XBusException.newException(ErrorCode.Unknown, "leaseId changed");
        }
    }

    public void plugServices(ServiceDesc[] desces, ServiceEndpoint endpoint) throws XBusException {
        Long retLeaseId = client.plugServices(desces, endpoint, ttl);
        if (leaseId == null ) {
            leaseId = retLeaseId;
        } else if (!leaseId.equals(retLeaseId)) {
            throw XBusException.newException(ErrorCode.Unknown, "leaseId changed");
        }
    }

    public void unplugService(String name, String version) throws XBusException {
        client.unplugService(name, version);
    }

    public void keepAlive() throws XBusException {
        if (leaseId != null) {
            client.keepAliveLease(leaseId);
        }
    }

    public void close() throws XBusException {
        stopFlag = true;
        if (leaseId != null) {
            client.revokeLease(leaseId);
            leaseId = null;
        }
    }
}
