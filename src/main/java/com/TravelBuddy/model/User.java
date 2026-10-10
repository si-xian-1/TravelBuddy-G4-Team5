package com.TravelBuddy.model;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.*;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Validation checking used for create and update traveler
 * User
 */
public class User implements UserDetails {
    public static final String PASSWORD_MASK = "........";
    private Integer userId;

    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username can be at most 50 characters")
    private String username;

    @Size(max = 8, message = "Mobile number can be at most 8 characters")
    @Pattern(regexp = "^\\d+$", message = "Mobile number should only contain numbers")
    private String mobileNumber;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 255, message = "Password can be at most 255 characters")
    private String password;
    private String role;

    /* Ensure location must be checked */
    @AssertTrue(message = "Invalid consent location")
    private Boolean hasConsentLocation;

    @Size(max = 50, message = "Emergency contact name can be at most 50 characters")
    private String emergencyContactName;

    @Size(max = 8, message = "Emergency contact mobile number can be at most 8 characters")
    @Pattern(regexp = "^\\d*$", message = "Emergency contact mobile number should only contain numbers")
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

    /* Return a collection of authorities (roles) granted to the user.
    */
    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String authority = "ROLE_" + role;
        return Arrays.asList(new SimpleGrantedAuthority(authority));
    }

    /*
    The various is___Expired() methods return a boolean to indicate whether
    or not the user’s account is enabled or expired.
    */
    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return true;
    }
}
