package com.TravelBuddy.model;

import com.fasterxml.jackson.annotation.*;

public class User {
    public static final String PASSWORD_MASK = "........";
    private int userId;
    private String username;
    private String mobileNumber;
    private String password;
    private String role;
    private Boolean hasConsentLocation;
    //emergency contact need to add to database
    private EmergencyContact emergencyContact;


    public User() {

    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMobileNumber() { 
        return mobileNumber; 
    } 
    
    public void setMobileNumber(String mobileNumber) { 
        this.mobileNumber = mobileNumber; 
    }
    
    @JsonIgnore
    // this will be ignore when converting from java to json
    public String getPassword() {
        return password;
    }

    @JsonGetter("password")
    public String getMaskedPassword() {
        return PASSWORD_MASK;
    }

    @JsonSetter("password")
    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean isHasConsentLocation() {
        return hasConsentLocation;
    }

    public void setHasConsentLocation(Boolean hasConsentLocation) {
        this.hasConsentLocation = hasConsentLocation;
    }
    public void setEmergencyContact(EmergencyContact emergencyContact) {
        this.emergencyContact = emergencyContact;
    }
    public void setEmergencyContact(String name, String mobileNumber){
        if (this.emergencyContact == null){
            this.emergencyContact = new EmergencyContact(name, mobileNumber);
        }
        else{
            if (name != null){
                emergencyContact.setName(name);

            }
            if (mobileNumber != null){
                emergencyContact.setMobileNumber(mobileNumber);

            }
        }
    }
    public EmergencyContact getEmergencyContact(){
        return this.emergencyContact;
    }
}
