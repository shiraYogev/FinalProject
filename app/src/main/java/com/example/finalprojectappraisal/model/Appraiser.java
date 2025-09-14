package com.example.finalprojectappraisal.model;

import com.google.firebase.firestore.PropertyName;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Appraiser {

    public enum AccessPermission {
        ADMIN,
        USER,
        VIEWER
    }

    private String appraiserId;
    private String firstName;
    private String lastName;

    @SerializedName("appraiser_name")
    private String fullName;

    private String email;
    private String phoneNumber;

    // 🔁 תואם לשדה Firestore: appraisers/{uid}.activeProjects = array of projectIds (strings)
    private List<String> activeProjects;

    // אופציונלי: גם היסטוריה כמזהים; אם תרצי אובייקט עשיר—נגדיר מחלקה נפרדת בעתיד
    private List<String> appraisalHistory;

    private AccessPermission accessPermissions;

    public Appraiser() {
        // חשוב לאתחל כדי להימנע מ-NullPointer כשעובדים עם arrayUnion/contains וכד'
        this.activeProjects = new ArrayList<>();
        this.appraisalHistory = new ArrayList<>();
    }

    public Appraiser(String appraiserId,
                     String firstName,
                     String lastName,
                     String email,
                     String phoneNumber,
                     List<String> activeProjects,
                     List<String> appraisalHistory,
                     AccessPermission accessPermissions) {
        this.appraiserId = appraiserId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.activeProjects = (activeProjects != null) ? activeProjects : new ArrayList<>();
        this.appraisalHistory = (appraisalHistory != null) ? appraisalHistory : new ArrayList<>();
        this.accessPermissions = accessPermissions;
    }

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
        String fn = (firstName == null) ? "" : firstName;
        String ln = (lastName == null) ? "" : lastName;
        this.fullName = (fn + " " + ln).trim();
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

    // === התאמה לשדה בפיירסטור: מערך מזהי פרויקטים ===
    public List<String> getActiveProjects() {
        if (activeProjects == null) activeProjects = new ArrayList<>();
        return activeProjects;
    }

    public void setActiveProjects(List<String> activeProjects) {
        this.activeProjects = (activeProjects != null) ? activeProjects : new ArrayList<>();
    }

    public void addActiveProjectId(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) return;
        if (activeProjects == null) activeProjects = new ArrayList<>();
        if (!activeProjects.contains(projectId)) activeProjects.add(projectId);
    }

    public void removeActiveProjectId(String projectId) {
        if (activeProjects != null) activeProjects.remove(projectId);
    }

    // היסטוריה—כעת גם כמזהים (אפשר להחליף בעתיד לאובייקט עשיר)
    public List<String> getAppraisalHistory() {
        if (appraisalHistory == null) appraisalHistory = new ArrayList<>();
        return appraisalHistory;
    }

    public void setAppraisalHistory(List<String> appraisalHistory) {
        this.appraisalHistory = (appraisalHistory != null) ? appraisalHistory : new ArrayList<>();
    }

    public AccessPermission getAccessPermissions() {
        return accessPermissions;
    }

    public void setAccessPermissions(AccessPermission accessPermissions) {
        this.accessPermissions = accessPermissions;
    }

    public boolean isAdmin() {
        return accessPermissions == AccessPermission.ADMIN;
    }

    public boolean isUser() {
        return accessPermissions == AccessPermission.USER || accessPermissions == AccessPermission.ADMIN;
    }

    public boolean isViewer() {
        return accessPermissions == AccessPermission.VIEWER;
    }

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
                ", accessPermissions=" + accessPermissions +
                '}';
    }
}
