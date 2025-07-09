package com.lawyerapp.LawyerApp.bin;

import com.lawyerapp.LawyerApp.dao.DataRepository; // Ho mantenuto il tuo pacchetto 'dao' per DataRepository
import com.lawyerapp.LawyerApp.util.SessionManager; // Necessario se SessionManager viene usato nel progetto

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur; // Importa GaussianBlur
import javafx.stage.Stage;
import javafx.util.Duration;           // Importa Duration

import java.io.IOException;
import java.util.Objects;              // Importa Objects

public class AppAvv extends Application {

    private static Stage primaryStage;
    private static DataRepository dataRepository;

    public AppAvv() {
        // Il costruttore di AppAvv è chiamato prima di start().
        // Inizializzazioni leggere o setup statico.
    }

    // Metodo per accedere al repository da altre classi
    public static DataRepository getDataRepository() {
        // Questo è una salvaguardia. dataRepository dovrebbe essere sempre inizializzato
        // nel metodo start(). Se viene chiamato prima, ci sarebbe un problema.
        if (dataRepository == null) {
            System.err.println("ATTENZIONE: DataRepository richiesto prima dell'inizializzazione completa dell'applicazione.");
            // Puoi gestire questo caso in modo più robusto in produzione.
            dataRepository = new DataRepository(); // Tentativo di fallback per evitare NullPointerException
        }
        return dataRepository;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }


    @Override
    public void start(Stage stage) { // Ho rinominato 'primaryStage' in 'stage' per chiarezza
        AppAvv.primaryStage = stage; // Assegna lo stage primario alla variabile statica

        try {
            // Inizializza il repository dati UNA SOLA VOLTA all'avvio dell'applicazione.
            dataRepository = new DataRepository();
            System.out.println("Repository dati inizializzato. Clienti caricati: " + dataRepository.getClientCount());

            // --- 1. CARICA LA SCHERMATA DI LOGIN IMMEDIATAMENTE ---
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent loginRoot = loader.load(); // Questo è il nodo radice del tuo login.fxml

            // Applica il foglio di stile CSS al nodo radice del login
            String cssPath = Objects.requireNonNull(getClass().getResource("/css/style.css"),
                                                    "CSS file not found: /css/style.css").toExternalForm();
            loginRoot.getStylesheets().add(cssPath);

            // --- 2. IMPOSTA LA SFOCATURA INIZIALE E LA SCENA SULLO STAGE ---
            // La schermata di login è immediatamente visibile, ma sfocata.
            GaussianBlur blurEffect = new GaussianBlur(20); // Inizia con una sfocatura alta (es. raggio 20)
            loginRoot.setEffect(blurEffect); // Applica l'effetto al nodo radice del login

            Scene loginScene = new Scene(loginRoot, 1280, 768); // Crea la scena del login
            stage.setScene(loginScene); // Imposta immediatamente la scena di login sullo stage
            stage.setTitle("Lawyer App - Accedi"); // Titolo finale
            stage.setResizable(true); // Rendi la finestra ridimensionabile
            stage.show(); // Mostra lo stage immediatamente con la login sfocata

            // --- 3. DEFINISCI L'ANIMAZIONE DI RIMozione DELLA SFOCATURA ---
            // Animazione di De-blur: la sfocatura si riduce gradualmente
            Timeline blurTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(blurEffect.radiusProperty(), 20)), // Inizia con sfocatura 20
                new KeyFrame(Duration.seconds(1.5), new KeyValue(blurEffect.radiusProperty(), 0)) // Termina con sfocatura 0 (dopo 1.5 secondi)
            );

            // --- 4. AZIONI QUANDO L'ANIMAZIONE È COMPLETATA ---
            // Quando l'animazione di sfocatura è completata, rimuovi l'effetto per performance.
            blurTimeline.setOnFinished(event -> {
                loginRoot.setEffect(null); // Rimuovi l'effetto GaussianBlur una volta che non è più necessario
                System.out.println("Animazione di de-blur completata.");
            });

            // --- 5. AVVIA L'ANIMAZIONE ---
            blurTimeline.play(); // Avvia l'animazione di rimozione della sfocatura
            
            // Salva i dati all'uscita dell'applicazione
            stage.setOnCloseRequest(event -> {
                System.out.println("Chiusura applicazione - salvataggio dati");
                if (dataRepository != null) {
                    dataRepository.saveData(); // Assicurati che saveData() esista e faccia ciò che serve
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Errore critico durante il caricamento di login.fxml o del CSS: " + e.getMessage());
            // Mostra un Alert all'utente in caso di errore grave che impedisce l'avvio
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Errore di Avvio");
            alert.setHeaderText("Impossibile avviare l'applicazione.");
            alert.setContentText("Si è verificato un errore critico durante il caricamento dell'interfaccia utente: " + e.getMessage());
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Errore generico durante l'inizializzazione dell'applicazione: " + e.getMessage());
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Errore di Inizializzazione");
            alert.setHeaderText("L'applicazione non può essere avviata.");
            alert.setContentText("Si è verificato un errore inaspettato: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}