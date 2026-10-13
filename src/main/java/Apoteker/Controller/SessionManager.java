package Apoteker.Controller;

import java.time.LocalDateTime;

public class SessionManager {
    private static SessionManager instance;
    private String loggedInUserId;
    private String loggedInUserName;
    private String loggedInUserFullName;
    private String loggedInUserRole;
    private LocalDateTime loginTime;
    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setLoggedInUser(String userId, String userName, String fullName, String userRole) {
        this.loggedInUserId = userId;
        this.loggedInUserName = userName;
        this.loggedInUserFullName = fullName;
        this.loggedInUserRole = userRole;
        this.loginTime = LocalDateTime.now();

        System.out.println("✅ SESSION SET:");
        System.out.println("   User ID: " + userId);
        System.out.println("   Username: " + userName);
        System.out.println("   Full Name: " + fullName);
        System.out.println("   Role: " + userRole);
        System.out.println("   Login Time: " + loginTime);
    }

    public String getLoggedInUserId() {
        return loggedInUserId;
    }
    public String getLoggedInUserName() {
        return loggedInUserName;
    }
    public String getLoggedInUserFullName() {
        return loggedInUserFullName;
    }
    public String getLoggedInUserRole() {
        return loggedInUserRole;
    }
    public LocalDateTime getLoginTime() {
        return loginTime;
    }
    public boolean isLoggedIn() {
        boolean status = loggedInUserId != null && !loggedInUserId.isEmpty() &&
                loggedInUserName != null && !loggedInUserName.isEmpty();
        System.out.println("🔍 SESSION CHECK: " + status);
        if (!status) {
            System.out.println("   Reason: userId=" + loggedInUserId + ", userName=" + loggedInUserName);
        }
        return status;
    }

    public void clearSession() {
        this.loggedInUserId = null;
        this.loggedInUserName = null;
        this.loggedInUserFullName = null;
        this.loggedInUserRole = null;
        this.loginTime = null;
        System.out.println("🚪 Session cleared - User logged out");
    }

    public void printCurrentSession() {
        System.out.println("📋 CURRENT SESSION STATUS:");
        System.out.println("   User ID: " + (loggedInUserId != null ? loggedInUserId : "NULL"));
        System.out.println("   Username: " + (loggedInUserName != null ? loggedInUserName : "NULL"));
        System.out.println("   Full Name: " + (loggedInUserFullName != null ? loggedInUserFullName : "NULL"));
        System.out.println("   Role: " + (loggedInUserRole != null ? loggedInUserRole : "NULL"));
        System.out.println("   Login Time: " + (loginTime != null ? loginTime : "NULL"));
        System.out.println("   Is Logged In: " + isLoggedIn());
        System.out.println("   Instance Hash: " + this.hashCode());
    }

    public String getDisplayName() {
        if (loggedInUserFullName != null && !loggedInUserFullName.trim().isEmpty()) {
            return loggedInUserFullName;
        }
        return loggedInUserName != null ? loggedInUserName : "Unknown User";
    }
}