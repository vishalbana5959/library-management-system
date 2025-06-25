package com.library;

import com.library.gui.LoginFrame;
import com.library.model.UserSession;
import com.library.utils.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        initializeApplication();
    }

    private static void initializeApplication() {
        if (!initializeDatabaseConnection()) {
            return;
        }
        setupApplicationUI();
        startApplication();
    }

    private static boolean initializeDatabaseConnection() {
        try {
            Connection conn = DBConnection.getConnection();
            DBConnection.closeConnection(conn);
            System.out.println("Database connection established successfully.");
            return true;
        } catch (SQLException e) {
            showFatalErrorDialog(
                    "Database Connection Error",
                    "Failed to connect to database:\n" + e.getMessage() +
                            "\n\nPlease check:\n" +
                            "1. MySQL server is running\n" +
                            "2. Database credentials in config.properties\n" +
                            "3. Database schema exists (library_db)"
            );
            return false;
        }
    }

    private static void setupApplicationUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            configureUIStyles();
            UserSession.clearSession();
        } catch (Exception e) {
            System.err.println("UI Initialization Error: " + e.getMessage());
        }
    }

    private static void configureUIStyles() {
        // Unified dark theme styling
        Color primaryColor = new Color(50, 65, 90);
        Color secondaryColor = new Color(70, 90, 120);

        UIManager.put("Button.background", secondaryColor);
        UIManager.put("Button.foreground", Color.BLACK);
        UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("ComboBox.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Panel.background", Color.WHITE);
    }

    private static void startApplication() {
        SwingUtilities.invokeLater(() -> {
            try {
                LoginFrame loginFrame = new LoginFrame();
                centerFrameOnScreen(loginFrame);
                loginFrame.setVisible(true);
            } catch (Exception e) {
                showFatalErrorDialog(
                        "Application Startup Error",
                        "Failed to start application:\n" + e.getMessage()
                );
            }
        });
    }

    private static void centerFrameOnScreen(JFrame frame) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(
                (screenSize.width - frame.getWidth()) / 2,
                (screenSize.height - frame.getHeight()) / 2
        );
    }

    private static void showFatalErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(
                null,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
        );
        System.exit(1);
    }
}
