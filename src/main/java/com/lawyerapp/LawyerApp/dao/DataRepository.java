package com.lawyerapp.LawyerApp.dao;

import com.lawyerapp.LawyerApp.model.Client;
import com.lawyerapp.LawyerApp.model.User;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class DataRepository {
    private static final String DATA_FILE = "lawyer_data.dat";
    private final Map<Integer, User> users = new HashMap<>();
    private final List<Client> clients = new ArrayList<>();
    private int lastUserId = 1;
    private int lastClientId = 1;

    public DataRepository() {
        loadData();
    }

    // Client operations
    public void addClient(Client client) {
        client.setId(lastClientId++);
        clients.add(client);
        saveData();
    }

    public List<Client> getClientsByLawyer(int lawyerId) {
        return clients.stream()
            .filter(c -> c.getLawyerId() == lawyerId)
            .collect(Collectors.toList());
    }

    public boolean updateClient(Client updatedClient) {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).getId() == updatedClient.getId()) {
                clients.set(i, updatedClient);
                saveData();
                return true;
            }
        }
        return false;
    }
    
    public boolean deleteClient(int clientId) {
        boolean removed = clients.removeIf(c -> c.getId() == clientId);
        if (removed) {
            saveData();
        }
        return removed;
    }

    // User operations
    public void addUser(User user) {
        user.setId(lastUserId++);
        users.put(user.getId(), user);
        saveData();
    }

    public User authenticate(String username, String password) {
        return users.values().stream()
            .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
            .findFirst()
            .orElse(null);
    }
    
    public boolean usernameExists(String username) {
        return users.values().stream().anyMatch(u -> u.getUsername().equals(username));
    }
    
    public boolean emailExists(String email) {
        return users.values().stream().anyMatch(u -> u.getEmail().equals(email));
    }

    // Get client count for debugging
    public int getClientCount() {
        return clients.size();
    }

    // Data persistence - changed to public
    public void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            Map<String, Object> data = new HashMap<>();
            data.put("users", users);
            data.put("clients", clients);
            data.put("lastUserId", lastUserId);
            data.put("lastClientId", lastClientId);
            oos.writeObject(data);
        } catch (IOException e) {
            System.err.println("Errore salvataggio dati: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            Map<String, Object> data = (Map<String, Object>) ois.readObject();
            users.putAll((Map<Integer, User>) data.get("users"));
            clients.addAll((List<Client>) data.get("clients"));
            lastUserId = (int) data.get("lastUserId");
            lastClientId = (int) data.get("lastClientId");
        } catch (FileNotFoundException e) {
            System.out.println("Nessun dato esistente. Creazione nuovo archivio.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Errore caricamento dati: " + e.getMessage());
        }
    }
}