package com.example.finalprojectappraisal.model;

import com.google.firebase.firestore.PropertyName;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * The Appraiser class represents a property appraiser in the system who is responsible for evaluating
 * properties and providing appraisals. Each appraiser can work on multiple projects and assess various properties.
 *
 * This class stores the appraiser's personal information (name, email, contact details), as well as the list
 * of projects they are involved in. The appraiser performs property evaluations and generates reports based on
 * the findings.
 */

public class Appraiser {

    public enum AccessPermission {
        ADMIN,
        USER,
        VIEWER
    }

    private String appraiserId;             // Unique identifier for the appraiser
    private String firstName;               // First name of the appraiser
    private String lastName;                // Last name of the appraiser
    @SerializedName("appraiser_name")
    private String fullName;                // Full name of the appraiser (firstName + lastName)
    private String email;                   // Email address for contact
    private String phoneNumber;             // Phone number (mobile and/or office)
    private List<Project> activeProjects;    // List of project IDs the appraiser is currently working on
    private List<Project> appraisalHistory;  // List of appraisals made by the appraiser, including property details
    private AccessPermission accessPermissions;     // Access level for the appraiser (determined by email)

    // Default constructor (required for Firestore)
    public Appraiser() {
    }

    // Constructor
    public Appraiser(String appraiserId, String firstName, String lastName, String email,
                     String phoneNumber, List<Project> activeProjects,
                     List<Project> appraisalHistory, AccessPermission accessPermissions) {
        this.appraiserId = appraiserId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = firstName + " " + lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.activeProjects = activeProjects;
        this.appraisalHistory = appraisalHistory;
        this.accessPermissions = accessPermissions;
    }

    // Getters and setters
    public String getAppraiserId() {
        return appraiserId;
    }

    public void setAppraiserId(String appraiserId) {
        this.appraiserId = appraiserId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        updateFullName();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        updateFullName();
    }

    @PropertyName("appraiser_name")
    public String getFullName() {
        return fullName;
    }

    @PropertyName("appraiser_name")
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    private void updateFullName() {
        if (firstName != null && lastName != null) {
            this.fullName = firstName + " " + lastName;
        }
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

    public List<Project> getActiveProjects() {
        return activeProjects;
    }

    public void setActiveProjects(List<Project> activeProjects) {
        this.activeProjects = activeProjects;
    }

    public List<Project> getAppraisalHistory() {
        return appraisalHistory;
    }

    public void setAppraisalHistory(List<Project> appraisalHistory) {
        this.appraisalHistory = appraisalHistory;
    }

    public AccessPermission getAccessPermissions() {
        return accessPermissions;
    }

    public void setAccessPermissions(AccessPermission accessPermissions) {
        this.accessPermissions = accessPermissions;
    }

    /**
     * Check if this appraiser has admin privileges
     */
    public boolean isAdmin() {
        return accessPermissions == AccessPermission.ADMIN;
    }

    /**
     * Check if this appraiser has user privileges (can edit projects they're assigned to)
     */
    public boolean isUser() {
        return accessPermissions == AccessPermission.USER || accessPermissions == AccessPermission.ADMIN;
    }

    /**
     * Check if this appraiser has only viewer privileges
     */
    public boolean isViewer() {
        return accessPermissions == AccessPermission.VIEWER;
    }

    // Method to display appraiser's details in a readable format
    @Override
    public String toString() {
        return "Appraiser{" +
                "appraiserId='" + appraiserId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", activeProjects=" + activeProjects +
                ", appraisalHistory=" + appraisalHistory +
                ", accessPermissions='" + accessPermissions + '\'' +
                '}';
    }
}