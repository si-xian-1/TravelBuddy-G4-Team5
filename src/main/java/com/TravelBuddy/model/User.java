package com.TravelBuddy.model;

public class User {
    private int userId;
    private String username;
    private String mobileNumber;
    private String password;
    private String role;
    private boolean hasConsentLocation;
    //emergency contact
    private EmergencyContact emergencyContact;

    public User() {
    }

    public User(int userId, String username, String mobileNumber,
            String password, String role, boolean hasConsentLocation) {
        this.userId = userId;
        this.username = username;
        this.mobileNumber = mobileNumber;
        this.password = password;
        this.role = role;
        this.hasConsentLocation = hasConsentLocation;
    }

    public User(String username, String mobileNumber, String password,
            boolean hasConsentLocation) {
        this.username = username;
        this.mobileNumber = mobileNumber;
        this.password = password;
        this.hasConsentLocation = hasConsentLocation;
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

    public boolean isHasConsentLocation() {
        return hasConsentLocation;
    }

    public void setHasConsentLocation(boolean hasConsentLocation) {
        this.hasConsentLocation = hasConsentLocation;
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
