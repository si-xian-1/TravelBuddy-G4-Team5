package com.TravelBuddy.model;

import com.fasterxml.jackson.annotation.*;

public class User {
    public static final String PASSWORD_MASK = "........";
    private int userId;
    private String username;
    private String mobileNumber;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String role;
    private Boolean hasConsentLocation;
    private String emergencyContactName;
    private String emergencyContactNumber;

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
    
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getHasConsentLocation() {
        return hasConsentLocation;
    }

    public void setHasConsentLocation(Boolean hasConsentLocation) {
        this.hasConsentLocation = hasConsentLocation;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactMobileNumber() {
        return emergencyContactNumber;
    }

    public void setEmergencyContactMobileNumber(String emergencyContactNumber) {
        this.emergencyContactNumber = emergencyContactNumber;
    }
}
