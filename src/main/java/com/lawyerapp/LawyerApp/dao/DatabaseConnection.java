package com.lawyerapp.LawyerApp.dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    // Stringa URL corretta, con il fuso orario aggiunto
    private static final String URL = "jdbc:mysql://localhost:3306/lawyer_app?serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            updateSchema(conn);
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Database driver not found", e);
        }
    }

    private static void updateSchema(Connection conn) {
        try {
            // Verifica se la tabella clients esiste
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "clients", null);

            if (!tables.next()) {
                // Crea la tabella se non esiste
                try (Statement stmt = conn.createStatement()) {
                    String createTable = "CREATE TABLE clients (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "lawyer_id INT NOT NULL," +
                            "appointment_date DATE NOT NULL," +
                            "appointment_time TIME NOT NULL DEFAULT '09:00:00'," +
                            "name VARCHAR(100) NOT NULL," +
                            "surname VARCHAR(100) NOT NULL," +
                            "case_number VARCHAR(50)," +
                            "court VARCHAR(100)," +
                            "notes TEXT)";
                    stmt.executeUpdate(createTable);
                    System.out.println("Tabella clients creata");
                }
            } else {
                // Verifica se la colonna appointment_time esiste
                ResultSet columns = metaData.getColumns(null, null, "clients", "appointment_time");
                if (!columns.next()) {
                    // Se la colonna non esiste, creala
                    try (Statement stmt = conn.createStatement()) {
                        String sql = "ALTER TABLE clients ADD COLUMN appointment_time TIME NOT NULL DEFAULT '09:00:00'";
                        stmt.executeUpdate(sql);
                        System.out.println("Colonna appointment_time aggiunta");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore durante l'aggiornamento dello schema: " + e.getMessage());
        }
    }
}