package com.library.gui.admin;

import com.library.dao.UserDAO;
import com.library.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class UserManagementPanel extends JPanel {
    private JTable userTable;
    private JTextField searchField;
    private JButton addButton, editButton, deleteButton, refreshButton;
    private DefaultTableModel model;

    public UserManagementPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(240, 248, 255));

        // Title Label
        JLabel titleLabel = new JLabel("User Management", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 102, 204));
        add(titleLabel, BorderLayout.NORTH);

        // Table Panel
        String[] columns = {"ID", "Name", "Email", "Role"};
        model = new DefaultTableModel(columns, 0);
        userTable = new JTable(model);
        userTable.setRowHeight(30);
        userTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(userTable);
        add(scrollPane, BorderLayout.CENTER);

        // Controls Panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(1, 5, 10, 10));
        controlPanel.setBackground(new Color(240, 248, 255));

        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchField.setToolTipText("Search by Name, Email, or Role...");
        controlPanel.add(searchField);

        addButton = new JButton("Add User");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");
        refreshButton = new JButton("Refresh");

        addButton.setBackground(new Color(0, 153, 76));
        editButton.setBackground(new Color(255, 204, 0));
        deleteButton.setBackground(new Color(204, 0, 0));
        refreshButton.setBackground(new Color(0, 102, 204));

        addButton.setForeground(Color.BLACK);
        editButton.setForeground(Color.BLACK);
        deleteButton.setForeground(Color.BLACK);
        refreshButton.setForeground(Color.BLACK);

        controlPanel.add(addButton);
        controlPanel.add(editButton);
        controlPanel.add(deleteButton);
        controlPanel.add(refreshButton);

        add(controlPanel, BorderLayout.SOUTH);

        // Load user data
        loadUsers();

        // Button actions
        addButton.addActionListener(e -> addUser());
        editButton.addActionListener(e -> editUser());
        deleteButton.addActionListener(e -> deleteUser());
        refreshButton.addActionListener(e -> loadUsers());
        searchField.addActionListener(e -> searchUsers());
    }

    public void loadUsers() {
        model.setRowCount(0);
        List<User> users = UserDAO.getAllUsers();
        for (User user : users) {
            model.addRow(new Object[]{user.getId(), user.getName(), user.getEmail(), user.getRole()});
        }
    }

    private void addUser() {
        new UserFormDialog(null, this);
    }

    private void editUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to edit.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId = (int) model.getValueAt(selectedRow, 0);
        User user = UserDAO.getUserById(userId);
        new UserFormDialog(user, this);
    }

    private void deleteUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId = (int) model.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this user?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            UserDAO.deleteUser(userId);
            loadUsers();
        }
    }

    private void searchUsers() {
        String query = searchField.getText().trim();
        model.setRowCount(0);
        List<User> users = UserDAO.searchUsers(query);
        for (User user : users) {
            model.addRow(new Object[]{user.getId(), user.getName(), user.getEmail(), user.getRole()});
        }
    }
}
