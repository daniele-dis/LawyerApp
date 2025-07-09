package com.lawyerapp.LawyerApp.service;

import com.lawyerapp.LawyerApp.bin.AppAvv;
import com.lawyerapp.LawyerApp.dao.DataRepository;
import com.lawyerapp.LawyerApp.model.User;
import com.lawyerapp.LawyerApp.util.SessionManager;

public class AuthService {
    private final DataRepository dataRepository = AppAvv.getDataRepository();
    
    public User login(String username, String password) {
        User user = dataRepository.authenticate(username, password);
        if (user != null) {
            // Salva l'utente nella sessione
            SessionManager.setCurrentUser(user);
        }
        return user;
    }
    
    public boolean register(User user) {
        if (dataRepository.usernameExists(user.getUsername())) {
            return false;
        }
        dataRepository.addUser(user);
        return true;
    }
}