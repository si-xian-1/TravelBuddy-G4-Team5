package com.TravelBuddy.model;

public class EmergencyContact {
    private String name;
    private String mobileNumber;
    public EmergencyContact(String name, String mobileNumber){
        this.name = name;
        this.mobileNumber = mobileNumber;
    }
    public EmergencyContact(){
    }
    public String getName(){
        return this.name;
    }
    public String getMobileNumber(){
        return this.mobileNumber;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setMobileNumber(String mobileNumber){
        this.mobileNumber = mobileNumber;
    }
}
