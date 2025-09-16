package com.example.finalprojectappraisal.model;


/**
 * The Admin class represents an administrator in the system who manages the overall functioning
 * of the application. The admin has control over the projects, appraisers, and clients within the system.
 * This class is responsible for managing user access, overseeing the status of projects, and maintaining
 * relationships with appraisers and clients.
 *
 * The admin has permissions to create, edit, or delete projects, and assign or manage appraisers and clients.
 */

public class Admin {

    // The email address of the admin (cannot be changed after initialization)
    private final String email;

    // Boolean flag indicating if the admin has super admin privileges
    private final boolean isSuperAdmin;

    /**
     * Constructor for the Admin class.
     *
     * @param email        The email address of the admin.
     * @param isSuperAdmin A boolean indicating if the admin is a super admin.
     */
    public Admin(String email, boolean isSuperAdmin) {
        this.email = email;
        this.isSuperAdmin = isSuperAdmin;
    }

    /**
     * Gets the email address of the admin.
     *
     * @return The email of the admin.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Checks if the admin is a super admin.
     *
     * @return true if the admin is a super admin, false otherwise.
     */
    public boolean isSuperAdmin() {
        return isSuperAdmin;
    }
}

