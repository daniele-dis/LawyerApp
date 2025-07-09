package com.lawyerapp.LawyerApp.controller;

import java.io.IOException;
import java.net.URL;

import com.lawyerapp.LawyerApp.bin.AppAvv;
import com.lawyerapp.LawyerApp.model.User;
import com.lawyerapp.LawyerApp.service.AuthService;
import com.lawyerapp.LawyerApp.util.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class AuthController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink registerLink;
    @FXML private ImageView logoImageView;
    @FXML private TextField visiblePasswordField;
    @FXML private Button togglePasswordButton;
    @FXML private ImageView eyeIcon;
    
    private boolean passwordVisible = false;
    private final AuthService authService = new AuthService();
    
    @FXML
    public void initialize() {
        // Imposta il pulsante di login come predefinito
        loginButton.setDefaultButton(true);
        
        javafx.application.Platform.runLater(() -> {
            double width = logoImageView.getFitWidth();
            double height = logoImageView.getFitHeight();
            double radius = Math.min(width, height) / 2;
            Circle clip = new Circle(width / 2, height / 2, radius);
            logoImageView.setClip(clip);
            visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        });
        
        // Gestione tasto Invio per i campi
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());
        visiblePasswordField.setOnAction(e -> handleLogin());
        
        // Gestione tasto Invio per il link di registrazione
        registerLink.setOnAction(e -> handleRegister());
    }
    
    @FXML
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            visiblePasswordField.setText(passwordField.getText());
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);

            passwordField.setVisible(false);
            passwordField.setManaged(false);
        } else {
            passwordField.setText(visiblePasswordField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);

            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
        }

        URL imageUrl = getClass().getResource("/img/eye.png");

        if (imageUrl != null) {
            eyeIcon.setImage(new Image(imageUrl.toExternalForm()));
        } else {
            System.err.println("Immagine non trovata: eye.png");
        }
    }

    @FXML
    private void handleLogin() { 
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Errore", "Inserisci username e password");
            return;
        }
        
        User user = authService.login(username, password);
        if (user != null) {
            // DEBUG: Verifica se l'utente è stato impostato correttamente
            System.out.println("Utente loggato: " + user.getUsername() + " - ID: " + user.getId());
            System.out.println("SessionManager user: " + SessionManager.getCurrentUser());
            
            openDashboard();
        } else {
            showAlert("Errore", "Credenziali non valide");
        }
    }
    
    @FXML
    private void handleRegister() {
        try {
            Parent root = FXMLLoader.load(AppAvv.class.getResource("/fxml/registration.fxml"));
            Scene scene = new Scene(root, 1280, 768);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            Stage stage = (Stage) loginButton.getScene().getWindow();

            boolean wasFullScreen = stage.isFullScreen(); // salviamo prima dello switch

            stage.setScene(scene);

            // Rimandiamo il fullscreen dopo il rendering della scena
            if (wasFullScreen) {
                javafx.application.Platform.runLater(() -> stage.setFullScreen(true));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void openDashboard() {
        try {
            // Carica la dashboard con FXMLLoader per poter accedere al controller
            FXMLLoader loader = new FXMLLoader(AppAvv.class.getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            
            // Ottieni il controller della dashboard e imposta l'utente
            DashboardController dashboardController = loader.getController();
            dashboardController.setUser(SessionManager.getCurrentUser());
            
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 768);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Dashboard Avvocato");
            stage.setResizable(true);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Errore", "Impossibile aprire la dashboard: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}