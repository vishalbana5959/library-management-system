package com.library.gui.admin;

import com.library.dao.UserDAO;
import com.library.model.User;

import javax.swing.*;
import java.awt.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class UserFormDialog extends JDialog {
    private JTextField nameField, emailField;
    private JPasswordField passwordField;
    private JComboBox<String> roleComboBox;
    private JButton saveButton, cancelButton;
    private User user;
    private UserManagementPanel parentPanel;

    public UserFormDialog(User user, UserManagementPanel parentPanel) {
        this.user = user;
        this.parentPanel = parentPanel;

        setTitle(user == null ? "Add User" : "Edit User");
        setSize(450, 350);
        setModal(true);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Name:"), gbc);
        nameField = new JTextField(20);
        gbc.gridx = 1;
        add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Email:"), gbc);
        emailField = new JTextField(20);
        gbc.gridx = 1;
        add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Password:"), gbc);
        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("Role:"), gbc);
        roleComboBox = new JComboBox<>(new String[]{"admin", "user"});
        gbc.gridx = 1;
        add(roleComboBox, gbc);

        JPanel buttonPanel = new JPanel();
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        if (user != null) {
            nameField.setText(user.getName());
            emailField.setText(user.getEmail());
            roleComboBox.setSelectedItem(user.getRole());
        }

        saveButton.addActionListener(e -> saveUser());
        cancelButton.addActionListener(e -> dispose());

        setVisible(true);
    }

    private void saveUser() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = (String) roleComboBox.getSelectedItem();

        if (name.isEmpty() || email.isEmpty() || (user == null && password.isEmpty())) {
            JOptionPane.showMessageDialog(this, "All fields must be filled.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Timestamp createdAt = user == null ? Timestamp.valueOf(LocalDateTime.now()) : user.getCreatedAt();

        if (user == null) {
            User newUser = new User(0, name, email, password, role, createdAt);
            UserDAO.addUser(newUser);
        } else {
            user.setName(name);
            user.setEmail(email);
            if (!password.isEmpty()) user.setPassword(password);
            user.setRole(role);
            UserDAO.updateUser(user);
        }

        parentPanel.loadUsers();
        dispose();
    }
}
