package com.lawyerapp.LawyerApp.controller;

import java.io.IOException;

import com.lawyerapp.LawyerApp.bin.AppAvv;
import com.lawyerapp.LawyerApp.model.User;
import com.lawyerapp.LawyerApp.service.AuthService;

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
import javafx.stage.Stage;

public class RegistrationController {
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Hyperlink loginLink;
    
    private final AuthService authService = new AuthService();
    
    @FXML
    private TextField passwordVisibleField;

    @FXML
    private ImageView eyePasswordIcon;


    @FXML
    private TextField confirmPasswordVisibleField;

    @FXML
    private ImageView eyeConfirmIcon;

    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;
    
    @FXML
    private void initialize() {
        // Imposta il pulsante di registrazione come predefinito
        registerButton.setDefaultButton(true);
        
        // Sincronizza testo tra PasswordField e TextField
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        confirmPasswordVisibleField.textProperty().bindBidirectional(confirmPasswordField.textProperty());
        
        // Gestione tasto Invio per i campi
        fullNameField.setOnAction(e -> emailField.requestFocus());
        emailField.setOnAction(e -> usernameField.requestFocus());
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(e -> handleRegistration());
        
        // Gestione tasto Invio per i campi password visibili
        passwordVisibleField.setOnAction(e -> confirmPasswordField.requestFocus());
        confirmPasswordVisibleField.setOnAction(e -> handleRegistration());
        
        // Gestione tasto Invio per il link di accesso
        loginLink.setOnAction(e -> goToLogin());
    }
    
    @FXML
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            // Usa sempre la stessa icona, niente eye-slash
            eyePasswordIcon.setImage(new Image(getClass().getResourceAsStream("/img/eye.png")));
        } else {
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            eyePasswordIcon.setImage(new Image(getClass().getResourceAsStream("/img/eye.png")));
        }
    }

    @FXML
    private void toggleConfirmPasswordVisibility() {
        confirmPasswordVisible = !confirmPasswordVisible;
        if (confirmPasswordVisible) {
            confirmPasswordVisibleField.setVisible(true);
            confirmPasswordVisibleField.setManaged(true);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
            eyeConfirmIcon.setImage(new Image(getClass().getResourceAsStream("/img/eye.png")));
        } else {
            confirmPasswordVisibleField.setVisible(false);
            confirmPasswordVisibleField.setManaged(false);
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            eyeConfirmIcon.setImage(new Image(getClass().getResourceAsStream("/img/eye.png")));
        }
    }

    
    @FXML
    private void handleRegistration() {
        String fullName = fullNameField.getText();
        String email = emailField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (fullName.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showAlert("Errore", "Tutti i campi sono obbligatori");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showAlert("Errore", "Le password non coincidono");
            return;
        }
        
        if (password.length() < 8) {
            showAlert("Errore", "La password deve essere di almeno 8 caratteri");
            return;
        }
        
        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setPassword(password);
        
        try {
			if (authService.register(newUser)) {
			    showSuccess("Registrazione Completata", "Account creato con successo! Ora puoi effettuare il login.");
			    goToLogin();
			} else {
			    showAlert("Errore", "Registrazione fallita. Username o email già esistenti.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    @FXML
    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(AppAvv.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(root, 1280, 768);
            
            // Aggiungi il CSS alla scena
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}