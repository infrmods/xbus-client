package com.github.infrmods.xbus.item;

import java.util.HashMap;

public class Service {
    private String service;
    private HashMap<String, ZoneService> zones;

    public HashMap<String, ZoneService> getZones() {
        return zones;
    }

    public void setZones(HashMap<String, ZoneService> zones) {
        this.zones = zones;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }
}
