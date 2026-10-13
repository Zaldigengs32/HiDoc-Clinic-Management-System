package Manajer.Controller;

import java.time.LocalDateTime;

public class SessionManager {
    private static SessionManager instance;
    private String loggedInUserName;
    private String loggedInUserFullName;
    private String loggedInUserRole;
    private LocalDateTime loginTime;

    private SessionManager() {
        // private constructor biar tidak bisa diinstansiasi langsung
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setLoggedInUserName(String username) {
        this.loggedInUserName = username;
    }

    public void setLoggedInUserFullName(String fullName) {
        this.loggedInUserFullName = fullName;
    }

    public void setLoggedInUserRole(String role) {
        this.loggedInUserRole = role;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
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
        return loggedInUserName != null && !loggedInUserName.isEmpty();
    }

    public void clearSession() {
        loggedInUserName = null;
        loggedInUserFullName = null;
        loggedInUserRole = null;
        loginTime = null;
    }

    public String getDisplayName() {
        if (loggedInUserFullName != null && !loggedInUserFullName.isEmpty()) {
            return loggedInUserFullName;
        }
        return loggedInUserName;
    }
}
