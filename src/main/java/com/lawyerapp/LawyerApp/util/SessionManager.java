package com.lawyerapp.LawyerApp.util;

import com.lawyerapp.LawyerApp.model.User;

public class SessionManager {
    private static User currentUser;
    private static final SessionManager instance = new SessionManager();
    
    private SessionManager() {}  // Costruttore privato per singleton
    
    public static SessionManager getInstance() {
        return instance;
    }
    
    public static void setCurrentUser(User user) {
        currentUser = user;
    }
    
    public static User getCurrentUser() {
        return currentUser;
    }
    
    public static void clearSession() {
        currentUser = null;
    }
}