package com.example.finalprojectappraisal.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * The Client class represents the property owner in the system. The client can own multiple properties
 * and may have projects involving appraisers to assess the value of these properties.
 *
 * This class stores the client's personal information, including name, contact details, and the list of
 * properties they own. The client interacts with appraisers to manage the appraisal process of their properties.
 */

public class Client {
    @SerializedName("client_id")
    private String clientId;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("phone_number")
    private String phoneNumber;      // Phone number (mobile and/or office)
    private List<Project> properties;  // List of properties owned by the client (linked to appraisal)

    // Constructor
    public Client(String clientId, String fullName, String email, String phoneNumber,
                  List<Project> properties) {
        this.clientId = clientId;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.properties = properties;
    }

    public Client() {
        // קונסטרקטור ריק דרוש ל-Firestore
    }


    // Getters and setters
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public List<Project> getProperties() {
        return properties;
    }

    public void setProperties(List<Project> properties) {
        this.properties = properties;
    }


    // Method to display client's details in a readable format
    @Override
    public String toString() {
        return "Client{" +
                "clientId='" + clientId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", properties=" + properties +
                '}';
    }
}