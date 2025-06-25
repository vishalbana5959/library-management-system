package com.library.gui;

import com.library.utils.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class RegistrationFrame extends JFrame {
    private static final Color PRIMARY_COLOR = new Color(0, 153, 76);
    private static final Color PRIMARY_HOVER_COLOR = new Color(0, 180, 90);
    private static final Color TITLE_COLOR = new Color(50, 65, 90);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 24); // Increased from 22
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 16); // Increased from 14
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 16); // Increased from 14
    private static final Font FIELD_FONT = new Font("Segoe UI", Font.PLAIN, 16); // New field font

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passField;
    private JPasswordField confirmPassField;

    public RegistrationFrame() {
        initializeUI();
        setupComponents();
        setVisible(true);
    }

    private void initializeUI() {
        setTitle("Library Management System - Registration");
        setSize(550, 500); // Increased width and height
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                new LoginFrame().setVisible(true);
            }
        });
    }

    private void setupComponents() {
        JPanel mainPanel = createMainPanel();
        GridBagConstraints gbc = createGridBagConstraints();

        addTitleLabel(mainPanel, gbc);
        addNameFields(mainPanel, gbc);
        addEmailFields(mainPanel, gbc);
        addPasswordFields(mainPanel, gbc);
        addConfirmPasswordFields(mainPanel, gbc);
        addRegisterButton(mainPanel, gbc);

        add(mainPanel);
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(25, 25, 25, 25) // Increased padding
        ));
        return panel;
    }

    private GridBagConstraints createGridBagConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Increased insets
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    private void addTitleLabel(JPanel panel, GridBagConstraints gbc) {
        JLabel titleLabel = new JLabel("Create New Account", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TITLE_COLOR);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(titleLabel, gbc);
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
    }

    private void addNameFields(JPanel panel, GridBagConstraints gbc) {
        JLabel nameLabel = new JLabel("Full Name:");
        nameLabel.setFont(LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(nameLabel, gbc);

        nameField = new JTextField(25); // Increased from 20
        nameField.setFont(FIELD_FONT);
        nameField.setPreferredSize(new Dimension(300, 30)); // Set explicit size
        gbc.gridx = 1;
        panel.add(nameField, gbc);
    }

    private void addEmailFields(JPanel panel, GridBagConstraints gbc) {
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(emailLabel, gbc);

        emailField = new JTextField(25); // Increased from 20
        emailField.setFont(FIELD_FONT);
        emailField.setPreferredSize(new Dimension(300, 30)); // Set explicit size
        gbc.gridx = 1;
        panel.add(emailField, gbc);
    }

    private void addPasswordFields(JPanel panel, GridBagConstraints gbc) {
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(passLabel, gbc);

        passField = new JPasswordField(25); // Increased from 20
        passField.setFont(FIELD_FONT);
        passField.setPreferredSize(new Dimension(300, 30)); // Set explicit size
        gbc.gridx = 1;
        panel.add(passField, gbc);
    }

    private void addConfirmPasswordFields(JPanel panel, GridBagConstraints gbc) {
        JLabel confirmPassLabel = new JLabel("Confirm Password:");
        confirmPassLabel.setFont(LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(confirmPassLabel, gbc);

        confirmPassField = new JPasswordField(25); // Increased from 20
        confirmPassField.setFont(FIELD_FONT);
        confirmPassField.setPreferredSize(new Dimension(300, 30)); // Set explicit size
        gbc.gridx = 1;
        panel.add(confirmPassField, gbc);
    }

    private void addRegisterButton(JPanel panel, GridBagConstraints gbc) {
        JButton registerButton = new JButton("Register");
        registerButton.setFont(BUTTON_FONT);
        registerButton.setBackground(PRIMARY_COLOR);
        registerButton.setForeground(Color.BLACK);
        registerButton.setFocusPainted(false);
        registerButton.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30)); // Increased padding
        registerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        registerButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                registerButton.setBackground(PRIMARY_HOVER_COLOR);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                registerButton.setBackground(PRIMARY_COLOR);
            }
        });

        registerButton.addActionListener(this::handleRegistration);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(registerButton, gbc);
    }

    private void handleRegistration(ActionEvent e) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        char[] password = passField.getPassword();
        char[] confirmPassword = confirmPassField.getPassword();

        if (!validateInputs(name, email, password, confirmPassword)) {
            return;
        }

        try {
            if (isEmailRegistered(email)) {
                JOptionPane.showMessageDialog(this,
                        "Email already registered!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (registerUser(name, email, new String(password))) {
                JOptionPane.showMessageDialog(this,
                        "Registration successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
                new LoginFrame().setVisible(true);
            }
        } catch (SQLException ex) {
            handleDatabaseError(ex);
        } finally {
            // Clear password fields for security
            Arrays.fill(password, '0');
            Arrays.fill(confirmPassword, '0');
        }
    }

    private boolean validateInputs(String name, String email, char[] password, char[] confirmPassword) {
        if (name.isEmpty() || email.isEmpty() || password.length == 0 || confirmPassword.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            JOptionPane.showMessageDialog(this,
                    "Invalid email format!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (password.length < 8) {
            JOptionPane.showMessageDialog(this,
                    "Password must be at least 8 characters long!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!Arrays.equals(password, confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                    "Passwords do not match!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private boolean isEmailRegistered(String email) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(
                     "SELECT id FROM users WHERE email = ?")) {

            checkStmt.setString(1, email);
            try (ResultSet rs = checkStmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean registerUser(String name, String email, String password) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement insertStmt = conn.prepareStatement(
                     "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'admin')")) {

            insertStmt.setString(1, name);
            insertStmt.setString(2, email);
            insertStmt.setString(3, hashPassword(password));

            return insertStmt.executeUpdate() > 0;
        }
    }

    private void handleDatabaseError(SQLException ex) {
        JOptionPane.showMessageDialog(this,
                "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new RegistrationFrame();
        });
    }
}