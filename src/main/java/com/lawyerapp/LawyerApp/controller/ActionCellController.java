package com.lawyerapp.LawyerApp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import java.io.IOException;

import com.lawyerapp.LawyerApp.model.Client; // Import Client model

public class ActionCellController {
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    
    private Client client; // Cambiato da Object a Client
    private DashboardController dashboardController; // Riferimento al DashboardController

    // Setter per l'oggetto Client associato a questa riga
    public void setClient(Client client) {
        this.client = client;
    }

    // Setter per il DashboardController
    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @FXML
    private void handleEdit() {
        if (dashboardController != null && client != null) {
            dashboardController.editClient(client); // Delega al DashboardController
        } else {
            System.err.println("Errore: DashboardController o Client non impostato per l'azione di modifica.");
        }
    }

    @FXML
    private void handleDelete() {
        if (dashboardController != null && client != null) {
            dashboardController.deleteClient(client); // Delega al DashboardController
        } else {
            System.err.println("Errore: DashboardController o Client non impostato per l'azione di eliminazione.");
        }
    }
}