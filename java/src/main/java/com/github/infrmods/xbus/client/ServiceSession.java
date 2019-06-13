package com.github.infrmods.xbus.client;

import com.github.infrmods.xbus.exceptions.ErrorCode;
import com.github.infrmods.xbus.exceptions.XBusException;
import com.github.infrmods.xbus.item.ZoneService;
import com.github.infrmods.xbus.item.ServiceDesc;
import com.github.infrmods.xbus.item.ServiceEndpoint;
import com.github.infrmods.xbus.result.LeaseGrantResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class ServiceSession {
    public static final int MIN_TTL = 10;
    public static final int MIN_KEEP_INTERVAL = 8;
    private static final int INTERVAL_ON_ERROR = 5;
    private static Logger logger = LoggerFactory.getLogger(ServiceSession.class);

    private XBusClient client;
    private ServiceEndpoint endpoint;
    private int ttl;
    private volatile Long leaseId = null;
    private volatile boolean closed = false;
    private ConcurrentHashMap<String, ServiceDesc> services = new ConcurrentHashMap<String, ServiceDesc>();

    private static int autoInterval(int ttl) {
        int interval = (int) (ttl * 0.8);
        if (ttl - interval > 30) {
            interval = ttl - 30;
        }
        return interval;
    }

    ServiceSession(XBusClient client, ServiceEndpoint endpoint, int ttl) throws XBusException {
        this(client, endpoint, ttl, autoInterval(ttl));
    }

    ServiceSession(XBusClient client, ServiceEndpoint endpoint, int ttl, int keepInterval) throws XBusException {
        if (ttl < MIN_TTL) {
            throw new XBusException(ErrorCode.InvalidParam, "invalid ttl");
        }
        if (keepInterval < MIN_KEEP_INTERVAL) {
            throw new XBusException(ErrorCode.InvalidParam, "invalid keepInterval");
        }

        this.client = client;
        this.endpoint = endpoint;
        this.ttl = ttl;
        new Thread(() -> {
            while (!closed) {
                int interval = keepInterval;
                try {
                    keep();
                } catch (Throwable t) {
                    interval = INTERVAL_ON_ERROR;
                    logger.warn("keep fail", t);
                }
                try {
                    Thread.sleep(interval * 1000);
                } catch (InterruptedException e) {
                    logger.warn("keepTask sleeping interrupted", e);
                    return;
                }
            }
        }).start();
    }

    private void keep() throws XBusException {
        if (leaseId != null) {
            try {
                client.keepAliveLease(leaseId);
                return;
            } catch (XBusException e) {
                if (!e.code.equals(ErrorCode.NotFound)) {
                    throw e;
                }
                leaseId = null;
            }
        }

        LeaseGrantResult result = client.grantLease(ttl);
        ServiceDesc[] desces;
        synchronized (ServiceSession.this) {
            leaseId = result.getLeaseId();
            desces = services.values().toArray(new ServiceDesc[0]);
        }
        client.plugAllWithLease(leaseId, null, desces, endpoint);
    }

    public void plug(ServiceDesc desc) {
        synchronized (this) {
            services.put(desc.getId(), desc);
            if (leaseId == null) {
                return;
            }
        }
        try {
            client.plugWithLease(leaseId, desc, endpoint);
        } catch (XBusException e) {
            logger.error("plug service fail", e);
        }
    }

    public void plugAll(ServiceDesc[] desces) {
        synchronized (this) {
            for (ServiceDesc desc : desces) {
                services.put(desc.getId(), desc);
            }
            if (leaseId == null) {
                return;
            }
        }
        try {
            client.plugAllWithLease(leaseId, null, desces, endpoint);
        } catch (XBusException e) {
            logger.error("plug services fail", e);
        }
    }

    public void unplug(String name, String version) throws XBusException {
        services.remove(ZoneService.genId(name, version));
        client.unplugService(name, version);
    }

    public void close() throws XBusException {
        closed = true;
        if (leaseId != null) {
            client.revokeLease(leaseId);
        }
    }
}
