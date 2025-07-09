package com.lawyerapp.LawyerApp.controller;

import com.lawyerapp.LawyerApp.bin.AppAvv;
import com.lawyerapp.LawyerApp.dao.DataRepository;
import com.lawyerapp.LawyerApp.model.Client;
import com.lawyerapp.LawyerApp.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.time.LocalTime;
import java.time.LocalDate;

public class AppointmentController {
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<Integer> hourCombo;
    @FXML private ComboBox<Integer> minuteCombo;
    @FXML private TextField nameField;
    @FXML private TextField surnameField;
    @FXML private TextField caseNumberField;
    @FXML private TextField courtField;
    @FXML private TextArea notesArea;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Client clientToEdit;
    private final DataRepository dataRepository = AppAvv.getDataRepository();

    public void setClientToEdit(Client client) {
        this.clientToEdit = client;
        populateForm();
    }

    @FXML
    public void initialize() {
        // Popola le combo box per le ore (0-23)
        for (int i = 0; i < 24; i++) {
            hourCombo.getItems().add(i);
        }

        // Popola le combo box per i minuti (0, 5, 10, ..., 55)
        for (int i = 0; i < 60; i += 5) {
            minuteCombo.getItems().add(i);
        }

        // Formattatore per mostrare i valori a due cifre
        StringConverter<Integer> twoDigitFormatter = new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                if (object == null) return null;
                return String.format("%02d", object);
            }

            @Override
            public Integer fromString(String string) {
                if (string == null || string.isEmpty()) return null;
                try {
                    return Integer.parseInt(string);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        };

        hourCombo.setConverter(twoDigitFormatter);
        minuteCombo.setConverter(twoDigitFormatter);

        // Imposta valori predefiniti per nuovi appuntamenti
        if (clientToEdit == null) {
            hourCombo.setValue(9);       // Ora predefinita: 09
            minuteCombo.setValue(0);      // Minuto predefinito: 00
            datePicker.setValue(LocalDate.now()); // Data predefinita: oggi
        }
    }

    private void populateForm() {
        if (clientToEdit != null) {
            // Popola i campi con i dati del cliente esistente
            datePicker.setValue(clientToEdit.getAppointmentDate());

            if (clientToEdit.getAppointmentTime() != null) {
                hourCombo.setValue(clientToEdit.getAppointmentTime().getHour());
                minuteCombo.setValue(clientToEdit.getAppointmentTime().getMinute());
            } else {
                // Valori predefiniti se l'orario non è presente
                hourCombo.setValue(9);
                minuteCombo.setValue(0);
            }

            nameField.setText(clientToEdit.getName());
            surnameField.setText(clientToEdit.getSurname());
            caseNumberField.setText(clientToEdit.getCaseNumber());
            courtField.setText(clientToEdit.getCourt());
            notesArea.setText(clientToEdit.getNotes());
        }
    }

    @FXML
    private void handleSave() {
        // Validazione campi obbligatori
        Integer selectedHour = hourCombo.getValue();
        Integer selectedMinute = minuteCombo.getValue();
        LocalDate selectedDate = datePicker.getValue();

        if (selectedDate == null || selectedHour == null || selectedMinute == null ||
            nameField.getText().trim().isEmpty() ||
            surnameField.getText().trim().isEmpty()) {
            showAlert("Errore di Validazione", 
                      "Per favore, compila tutti i campi obbligatori (Data, Ora, Nome, Cognome).");
            return;
        }

        // Crea o aggiorna l'oggetto Client
        Client client = (clientToEdit != null) ? clientToEdit : new Client();
        client.setLawyerId(SessionManager.getCurrentUser().getId());
        client.setAppointmentDate(selectedDate);
        client.setAppointmentTime(LocalTime.of(selectedHour, selectedMinute));
        client.setName(nameField.getText().trim());
        client.setSurname(surnameField.getText().trim());
        client.setCaseNumber(caseNumberField.getText().trim());
        client.setCourt(courtField.getText().trim());
        client.setNotes(notesArea.getText().trim());

        // Salva usando DataRepository
        if (clientToEdit != null) {
            dataRepository.updateClient(client);
        } else {
            dataRepository.addClient(client);
        }

        showAlert("Successo", "Appuntamento salvato correttamente!");
        closeWindow();
    }
    
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}