package com.TravelBuddy.model.enums;

public enum EventType {
    TC("Tropical Cyclones"),
    EQ("Earthquakes"),
    FL("Floods"),
    VO("Volcanoes"),
    WF("Wild Fires"),
    DR("Droughts");

    private String disasterName;

    private EventType(String disasterName) {
        this.disasterName = disasterName;
    }

    public String getDisasterName() {
        return disasterName;
    }
}
