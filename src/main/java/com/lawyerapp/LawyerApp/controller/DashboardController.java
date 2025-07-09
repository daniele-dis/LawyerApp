package com.lawyerapp.LawyerApp.controller;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.lawyerapp.LawyerApp.bin.AppAvv;
import com.lawyerapp.LawyerApp.dao.DataRepository;
import com.lawyerapp.LawyerApp.model.Client;
import com.lawyerapp.LawyerApp.service.PDFService;
import com.lawyerapp.LawyerApp.service.ReminderService;
import com.lawyerapp.LawyerApp.util.SessionManager;
import com.lawyerapp.LawyerApp.model.User;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.time.format.DateTimeFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.stage.StageStyle;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;


public class DashboardController {
    @FXML private TableView<Client> appointmentsTable;
    @FXML private TableColumn<Client, LocalDate> dateColumn;
    @FXML private TableColumn<Client, LocalTime> timeColumn;
    @FXML private TableColumn<Client, String> nameColumn;
    @FXML private TableColumn<Client, String> surnameColumn;
    @FXML private TableColumn<Client, String> caseColumn;
    @FXML private TableColumn<Client, String> courtColumn;
    @FXML private TableColumn<Client, String> notesColumn;
    @FXML private TableColumn<Client, Void> actionColumn;

    // Questi non sono più manipolati direttamente per la vista a tabella intera
    @FXML private AnchorPane tableContainer;
    @FXML private VBox tableSection;

    @FXML private TextField searchDate;
    @FXML private TextField searchTime;
    @FXML private TextField searchName;
    @FXML private TextField searchSurname;
    @FXML private TextField searchCaseNumber;
    @FXML private TextField searchCourt;
    @FXML private Button searchButton;
    @FXML private Button toggleTableButton;
    @FXML private ImageView tableIcon;
    @FXML private VBox searchContainer;
    @FXML private VBox mainContent;

    private final ReminderService reminderService = new ReminderService();
    private final PDFService pdfService = new PDFService();
    private final DataRepository dataRepository = AppAvv.getDataRepository();
    private final ObservableList<Client> appointments = FXCollections.observableArrayList();
    private boolean isTableExpanded = false;
    private Stage expandedTableStage; // Rinominato da fullScreenStage per chiarezza

    public void setUser(User user) {
        SessionManager.setCurrentUser(user);
        System.out.println("Utente impostato in DashboardController: " +
            (user != null ? user.getUsername() : "null"));

        loadAppointments();

        if (user != null) {
            reminderService.startDailyReminderCheck();
        }
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        loadAppointments();

        appointmentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        addSearchListeners();

        if (SessionManager.getCurrentUser() != null) {
            reminderService.startDailyReminderCheck();
        } else {
            System.out.println("Nessun utente loggato, servizio promemoria non avviato.");
        }

        setupTableToggle();
    }

    private void setupTableToggle() {
        try {
            Image expandImage = new Image(getClass().getResourceAsStream("/img/expand.png"));
            tableIcon.setImage(expandImage);

            toggleTableButton.setOnAction(e -> handleToggleTable());
        } catch (Exception e) {
            System.err.println("Errore caricamento icone: " + e.getMessage());
        }
    }

    // Metodo principale per aprire/chiudere la finestra dedicata alla tabella
    @FXML
    private void handleToggleTable() {
        if (!isTableExpanded) {
            expandTable();
        } else {
            collapseTable();
        }
        // NOTE: We no longer toggle isTableExpanded here directly.
        // It will be updated by expandTable/collapseTable methods.
    }

    private void expandTable() {
        if (expandedTableStage != null && expandedTableStage.isShowing()) {
            // If the stage is already showing, do nothing or bring to front
            expandedTableStage.toFront();
            return;
        }

        expandedTableStage = new Stage();
        expandedTableStage.setTitle("Tabella Appuntamenti");

        TableView<Client> standaloneTable = createStandaloneTableView();

        Button closeButton = new Button();
        try {
            Image collapseImage = new Image(getClass().getResourceAsStream("/img/collapse.png"));
            ImageView collapseIcon = new ImageView(collapseImage);
            collapseIcon.setFitWidth(24);
            collapseIcon.setFitHeight(24);
            closeButton.setGraphic(collapseIcon);
        } catch (Exception e) {
            System.err.println("Errore caricamento icona collapse: " + e.getMessage());
            closeButton.setText("X"); // Fallback in caso di errore
        }

        closeButton.getStyleClass().add("icon-button");
        // Ensure this button explicitly calls collapseTable, which will handle the flag
        closeButton.setOnAction(e -> collapseTable());

        HBox topBar = new HBox();
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.setPadding(new Insets(10));
        topBar.getChildren().add(closeButton);
        HBox.setHgrow(topBar, Priority.ALWAYS);

        VBox layout = new VBox();
        layout.getChildren().addAll(topBar, standaloneTable);
        VBox.setVgrow(standaloneTable, Priority.ALWAYS);

        Scene scene = new Scene(layout);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        expandedTableStage.setScene(scene);

        expandedTableStage.setWidth(1268);
        expandedTableStage.setHeight(720);
        expandedTableStage.centerOnScreen();

        // **CRITICAL CHANGE HERE:**
        // Set isTableExpanded to true immediately after showing the stage
        // and ensure it's reset to false when the window closes by any means.
        expandedTableStage.setOnHidden(e -> {
            isTableExpanded = false;
            updateIcon();
            System.out.println("Tabella espansa chiusa via onHidden. isTableExpanded=" + isTableExpanded);
        });

        expandedTableStage.show();
        isTableExpanded = true; // Set to true after showing the stage
        updateIcon(); // Update icon immediately after changing state
        System.out.println("Tabella espansa in una nuova finestra 1268x720. isTableExpanded=" + isTableExpanded);
    }

    // Crea una nuova istanza di TableView, copiando proprietà delle colonne e dati
    private TableView<Client> createStandaloneTableView() {
        TableView<Client> table = new TableView<>();
        table.setItems(appointmentsTable.getItems()); // Usa i dati attuali

        // Itera sulle colonne esistenti per copiarne le proprietà
        for (TableColumn<Client, ?> originalCol : appointmentsTable.getColumns()) {
            // We need to handle the actionColumn specifically because its cell factory
            // depends on DashboardController.this for edit/delete actions.
            // For other columns, a direct copy of cellValueFactory is usually sufficient.
            if (originalCol.getId() != null && originalCol.getId().equals(actionColumn.getId())) {
                TableColumn<Client, Void> newActionCol = new TableColumn<>("Azioni");
                newActionCol.setPrefWidth(originalCol.getPrefWidth());
                newActionCol.setMinWidth(originalCol.getMinWidth());
                newActionCol.setMaxWidth(originalCol.getMaxWidth());
                newActionCol.setResizable(originalCol.isResizable());
                newActionCol.setVisible(originalCol.isVisible());
                newActionCol.setSortable(originalCol.isSortable());

                // Re-apply the cell factory for the action column
                newActionCol.setCellFactory(param -> {
                    return new TableCell<Client, Void>() {
                        @Override
                        protected void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || getTableView().getItems().get(getIndex()) == null) {
                                setGraphic(null);
                            } else {
                                try {
                                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ActionCell.fxml"));
                                    HBox root = loader.load();
                                    ActionCellController controller = loader.getController();
                                    controller.setClient(getTableView().getItems().get(getIndex()));
                                    controller.setDashboardController(DashboardController.this);
                                    setGraphic(root);
                                } catch (IOException e) {
                                    System.err.println("Errore caricamento ActionCell FXML nella tabella espansa: " + e.getMessage());
                                    HBox hbox = new HBox(5);
                                    Button editBtn = new Button("Edit");
                                    Button deleteBtn = new Button("Delete");
                                    editBtn.setOnAction(event -> editClient(getTableView().getItems().get(getIndex())));
                                    deleteBtn.setOnAction(event -> deleteClient(getTableView().getItems().get(getIndex())));
                                    hbox.getChildren().addAll(editBtn, deleteBtn);
                                    setGraphic(hbox);
                                }
                            }
                        }
                    };
                });
                table.getColumns().add(newActionCol);
            } else {
                TableColumn<Client, ?> newCol = new TableColumn<>(originalCol.getText());
                newCol.setPrefWidth(originalCol.getPrefWidth());
                newCol.setMinWidth(originalCol.getMinWidth());
                newCol.setMaxWidth(originalCol.getMaxWidth());
                newCol.setResizable(originalCol.isResizable());
                newCol.setVisible(originalCol.isVisible());
                newCol.setSortable(originalCol.isSortable());

                // It's safer to use the specific type for setCellValueFactory if possible
                // For generic cases, casting to javafx.util.Callback is needed.
                newCol.setCellValueFactory((javafx.util.Callback) originalCol.getCellValueFactory());

                // Copy the cell factory for formatting (like timeColumn)
                newCol.setCellFactory((javafx.util.Callback) originalCol.getCellFactory());

                newCol.getStyleClass().addAll(originalCol.getStyleClass());

                table.getColumns().add((TableColumn<Client, ?>) newCol);
            }
        }

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }


    private void collapseTable() {
        if (expandedTableStage != null) {
            expandedTableStage.close();
            // isTableExpanded will be set to false by the onHidden listener
            // updateIcon() will also be called by the onHidden listener
            System.out.println("Tabella espansa chiusa via pulsante interno. isTableExpanded=" + isTableExpanded);
        }
    }

    private void updateIcon() {
        try {
            String iconPath = isTableExpanded ?
                "/img/collapse.png" :
                "/img/expand.png";
            Image icon = new Image(getClass().getResourceAsStream(iconPath));
            tableIcon.setImage(icon);
        } catch (Exception e) {
            System.err.println("Errore aggiornamento icona: " + e.getMessage());
        }
    }

    private void addSearchListeners() {
        searchDate.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        searchTime.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        searchName.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        searchSurname.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        searchCaseNumber.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        searchCourt.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
    }

    private void setupTableColumns() {
        dateColumn.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getAppointmentDate()));

        timeColumn.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getAppointmentTime()));

        nameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getName()));

        surnameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getSurname()));

        caseColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getCaseNumber()));

        courtColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getCourt()));

        notesColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getNotes()));

        timeColumn.setCellFactory(column -> {
            return new TableCell<Client, LocalTime>() {
                private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

                @Override
                protected void updateItem(LocalTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    } else {
                        setText(item.format(formatter));
                    }
                }
            };
        });

        // Set an ID for the action column to easily identify it in createStandaloneTableView
        actionColumn.setId("actionColumn");
        actionColumn.setCellFactory(param -> {
            return new TableCell<Client, Void>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableView().getItems().get(getIndex()) == null) {
                        setGraphic(null);
                    } else {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ActionCell.fxml"));
                            HBox root = loader.load();
                            ActionCellController controller = loader.getController();
                            controller.setClient(getTableView().getItems().get(getIndex()));
                            controller.setDashboardController(DashboardController.this);
                            setGraphic(root);
                        } catch (IOException e) {
                            System.err.println("Errore caricamento ActionCell FXML: " + e.getMessage());
                            HBox hbox = new HBox(5);
                            Button editBtn = new Button("Edit");
                            Button deleteBtn = new Button("Delete");
                            editBtn.setOnAction(event -> editClient(getTableView().getItems().get(getIndex())));
                            deleteBtn.setOnAction(event -> deleteClient(getTableView().getItems().get(getIndex())));
                            hbox.getChildren().addAll(editBtn, deleteBtn);
                            setGraphic(hbox);
                        }
                    }
                }
            };
        });
    }

    private void loadAppointments() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            System.err.println("Errore: Tentativo di caricare appuntamenti senza un utente loggato.");
            appointments.clear();
            appointmentsTable.setItems(appointments);
            return;
        }

        int lawyerId = currentUser.getId();
        List<Client> clientList = dataRepository.getClientsByLawyer(lawyerId);
        appointments.setAll(clientList);
        appointmentsTable.setItems(appointments);
    }

    public void editClient(Client client) {
        try {
            FXMLLoader loader = new FXMLLoader(AppAvv.class.getResource("/fxml/appointment_form.fxml"));
            Parent root = loader.load();

            AppointmentController controller = loader.getController();
            controller.setClientToEdit(client);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Modifica Appuntamento");
            stage.setOnHidden(e -> loadAppointments());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Errore", "Impossibile aprire l'editor: " + e.getMessage());
        }
    }

    public void deleteClient(Client client) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Conferma eliminazione");
        confirm.setHeaderText("Eliminare " + client.getName() + " " + client.getSurname() + "?");
        confirm.setContentText("L'operazione è irreversibile");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (dataRepository.deleteClient(client.getId())) {
                loadAppointments();
            } else {
                showAlert("Errore", "Eliminazione fallita");
            }
        }
    }

    @FXML
    private void handleNewAppointment() {
        try {
            FXMLLoader loader = new FXMLLoader(AppAvv.class.getResource("/fxml/appointment_form.fxml"));
            Parent root = loader.load();

            Scene newScene = new Scene(root);
            newScene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            Stage stage = new Stage();
            stage.setScene(newScene);
            stage.setTitle("Nuovo Appuntamento");
            stage.setOnHidden(e -> loadAppointments());
            stage.show();
        }
        catch (IOException e) {
            e.printStackTrace();
            showAlert("Errore", "Impossibile aprire la finestra del nuovo appuntamento: " + e.getMessage());
        }
    }

    @FXML
    private void handlePrint() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salva Report PDF Generale");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        Window window = appointmentsTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(window);

        if (file != null) {
            try {
                pdfService.generateClientReport(file);
                showAlert("Successo", "Report generale generato con successo!");
            } catch (IOException e) {
                showAlert("Errore", "Impossibile generare il report generale: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSearch() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            System.err.println("Errore: Tentativo di ricerca senza un utente loggato.");
            appointments.clear();
            appointmentsTable.setItems(appointments);
            return;
        }

        String date = searchDate.getText().trim();
        String time = searchTime.getText().trim();
        String name = searchName.getText().trim();
        String surname = searchSurname.getText().trim();
        String caseNumber = searchCaseNumber.getText().trim();
        String court = searchCourt.getText().trim();

        int lawyerId = currentUser.getId();
        List<Client> allClients = dataRepository.getClientsByLawyer(lawyerId);

        List<Client> searchResults = allClients.stream()
            .filter(c ->
                (date.isEmpty() || c.getAppointmentDate().toString().contains(date)) &&
                (time.isEmpty() || c.getAppointmentTime().toString().contains(time)) &&
                (name.isEmpty() || c.getName().toLowerCase().contains(name.toLowerCase())) &&
                (surname.isEmpty() || c.getSurname().toLowerCase().contains(surname.toLowerCase())) &&
                (caseNumber.isEmpty() || (c.getCaseNumber() != null && c.getCaseNumber().toLowerCase().contains(caseNumber.toLowerCase()))) &&
                (court.isEmpty() || (c.getCourt() != null && c.getCourt().toLowerCase().contains(court.toLowerCase())))
            )
            .collect(Collectors.toList());

        appointments.setAll(searchResults);
        appointmentsTable.setItems(appointments);
    }


    @FXML
    private void handleLogout() {
        reminderService.stop();
        SessionManager.clearSession();

        try {
            Parent root = FXMLLoader.load(AppAvv.class.getResource("/fxml/login.fxml"));
            Stage stage = (Stage) appointmentsTable.getScene().getWindow();

            Scene loginScene = new Scene(root, 1280, 768);

            loginScene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(loginScene);
            stage.setTitle("Lawyer App - Accesso");
            stage.setResizable(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}