package com.TravelBuddy.model;

import java.util.Date;

public class Itinerary {

    private int itineraryId;
    private String displayName;
    private double latitude;
    private double longitude;
    private Date date;
    private String type;                    //hotel or flight or attraction
    private int userId;                     //foreign key

    public Itinerary() {
    }

    public Itinerary(int itineraryId, String displayName, double latitude, double longitude, Date date, String type, int userId) {
        this.itineraryId = itineraryId;
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
        this.type = type;
        this.userId = userId;
    }

    public Itinerary(String displayName, double latitude, double longitude, Date date, int userId) {
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
        this.userId = userId;
    }

    public int getItineraryId() {
        return itineraryId;
    }

    public void setItineraryId(int itineraryId) {
        this.itineraryId = itineraryId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userid) {
        this.userId = userid;
    }

}
