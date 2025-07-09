package com.lawyerapp.LawyerApp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.lawyerapp.LawyerApp.dao.DataRepository;
import com.lawyerapp.LawyerApp.model.Client;
import com.lawyerapp.LawyerApp.model.User;
import com.lawyerapp.LawyerApp.util.SessionManager;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Modality;

public class ReminderService {
    private final DataRepository dataRepository = new DataRepository();
    private Timer timer;

    private static final int REMINDER_HOUR = 12;
    private static final int REMINDER_MINUTE = 0;

    private final AtomicBoolean isAlertShowing = new AtomicBoolean(false);

    public void startDailyReminderCheck() {
        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer(true); // Daemon thread

        System.out.println("DEBUG: ReminderService: Esecuzione immediata di checkTomorrowAppointments() all'avvio.");
        checkTomorrowAppointments();

        long initialDelayForMidday = getDelayUntilMidday();
        long period = 24 * 60 * 60 * 1000;

        System.out.println("DEBUG: ReminderService: Avvio schedulazione promemoria giornaliero. Ritardo iniziale: " + initialDelayForMidday + " ms, Periodo: " + period + " ms.");

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("DEBUG: ReminderService: Esecuzione schedulata di checkTomorrowAppointments() a mezzogiorno.");
                checkTomorrowAppointments();
            }
        }, initialDelayForMidday, period);
    }

    private void checkTomorrowAppointments() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            System.out.println("DEBUG: ReminderService: Nessun utente loggato, impossibile controllare gli appuntamenti.");
            return;
        }

        System.out.println("DEBUG: ReminderService: Controllo appuntamenti per l'utente: " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");

        List<Client> appointments = dataRepository.getClientsByLawyer(currentUser.getId())
            .stream()
            .filter(c ->
                c != null &&
                c.getAppointmentDate() != null &&
                c.getAppointmentDate().equals(LocalDate.now().plusDays(1)) &&
                c.getName() != null && !c.getName().isEmpty() &&
                c.getSurname() != null && !c.getSurname().isEmpty() &&
                c.getAppointmentTime() != null &&
                c.getId() > 0
            )
            .collect(Collectors.toList());

        if (!appointments.isEmpty()) {
            System.out.println("DEBUG: ReminderService: Trovati " + appointments.size() + " appuntamenti per domani.");
            Platform.runLater(() -> showReminder(appointments));
        } else {
            System.out.println("DEBUG: ReminderService: Nessun appuntamento valido trovato per domani.");
        }
    }

    private void showReminder(List<Client> appointments) {
        if (isAlertShowing.get()) {
            System.out.println("DEBUG: Un alert è già attivo. Richiesta ignorata.");
            return;
        }
        isAlertShowing.set(true);

        if (appointments == null || appointments.isEmpty()) {
            System.out.println("DEBUG: Nessun appuntamento da mostrare.");
            isAlertShowing.set(false);
            return;
        }

        StringBuilder message = new StringBuilder("Appuntamenti di domani:\n\n");
        for (Client client : appointments) {
            message.append("• ")
                .append(client.getName())
                .append(" ")
                .append(client.getSurname())
                .append(" - ")
                .append(client.getAppointmentDate())
                .append(" alle ")
                .append(client.getAppointmentTime())
                .append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Promemoria Appuntamenti");
        alert.setHeaderText("Hai " + appointments.size() + " appuntamenti per domani");
        alert.setContentText(message.toString());
        alert.initModality(Modality.APPLICATION_MODAL);

        alert.showAndWait();
        isAlertShowing.set(false);
        System.out.println("DEBUG: Promemoria mostrato e chiuso correttamente.");
    }


    private long getDelayUntilMidday() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime middayToday = now.withHour(REMINDER_HOUR).withMinute(REMINDER_MINUTE).withSecond(0).withNano(0);

        LocalDateTime middayNextRun = now.isBefore(middayToday)
            ? middayToday
            : middayToday.plusDays(1);

        long delay = ChronoUnit.MILLIS.between(now, middayNextRun);
        System.out.println("DEBUG: ReminderService: Calcolato ritardo per mezzogiorno: " + delay + " ms.");
        return delay;
    }

    private long getDelayUntil8AMForTesting() {
        System.out.println("DEBUG: ReminderService: Usando ritardo di test di 10 secondi.");
        return 10 * 1000;
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            System.out.println("DEBUG: ReminderService: Timer del promemoria arrestato.");
        }
    }
}
