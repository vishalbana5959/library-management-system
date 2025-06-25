package com.library.gui.admin;

import com.library.dao.BookDAO;
import com.library.dao.BorrowedBookDAO;
import com.library.dao.UserDAO;
import com.library.model.Book;
import com.library.model.User;
import com.library.utils.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class AdminBorrowBookPanel extends JPanel {
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JButton borrowButton, refreshButton;
    private JTextField searchField, userIdField;
    private JLabel statusLabel;
    private static final int BORROW_DURATION_DAYS = 14;

    public AdminBorrowBookPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Title Panel
        JLabel titleLabel = new JLabel("📖 Admin - Borrow Book", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(41, 128, 185));
        add(titleLabel, BorderLayout.NORTH);

        // Search Panel
        JPanel searchPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        searchPanel.setBackground(Color.WHITE);

        // User ID Panel
        JPanel userIdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userIdPanel.add(new JLabel("User ID:"));
        userIdField = new JTextField(10);
        userIdPanel.add(userIdField);
        searchPanel.add(userIdPanel);

        // Book Search Panel
        JPanel bookSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        JButton searchButton = new JButton("🔍 Search");
        searchButton.addActionListener(e -> searchBooks());
        bookSearchPanel.add(new JLabel("Search Book:"));
        bookSearchPanel.add(searchField);
        bookSearchPanel.add(searchButton);
        searchPanel.add(bookSearchPanel);

        add(searchPanel, BorderLayout.NORTH);

        // Table Setup
        tableModel = new DefaultTableModel(new String[]{"Book ID", "Title", "Author", "Availability"}, 0);
        bookTable = new JTable(tableModel);
        add(new JScrollPane(bookTable), BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        borrowButton = new JButton("📖 Borrow Selected Book");
        borrowButton.addActionListener(new BorrowBookAction());
        refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> loadAvailableBooks());
        statusLabel = new JLabel("", SwingConstants.CENTER);

        buttonPanel.add(borrowButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(statusLabel);
        add(buttonPanel, BorderLayout.SOUTH);

        loadAvailableBooks();
    }

    private void loadAvailableBooks() {
        tableModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT id, title, author, available FROM books WHERE available > 0")) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("available") > 0 ? "Available" : "Unavailable"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading books: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchBooks() {
        String keyword = searchField.getText().trim();
        tableModel.setRowCount(0);
        if (keyword.isEmpty()) {
            loadAvailableBooks();
            return;
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, title, author, available FROM books " +
                             "WHERE (title LIKE ? OR author LIKE ?) AND available > 0")) {
            stmt.setString(1, "%" + keyword + "%");
            stmt.setString(2, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("available") > 0 ? "Available" : "Unavailable"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error searching books: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private class BorrowBookAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            statusLabel.setText("");
            int selectedRow = bookTable.getSelectedRow();
            if (selectedRow == -1) {
                statusLabel.setText("Please select a book to borrow.");
                return;
            }

            int bookId = (int) tableModel.getValueAt(selectedRow, 0);
            String bookTitle = (String) tableModel.getValueAt(selectedRow, 1);

            String userIdText = userIdField.getText().trim();
            if (userIdText.isEmpty()) {
                statusLabel.setText("Please enter a User ID.");
                return;
            }

            try {
                int userId = Integer.parseInt(userIdText);
                User user = UserDAO.getUserById(userId);
                if (user == null) {
                    JOptionPane.showMessageDialog(null, "User not found!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Book book = BookDAO.getBookById(bookId);
                if (book == null || book.getAvailable() <= 0) {
                    JOptionPane.showMessageDialog(null, "Book not available!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Timestamp borrowDate = new Timestamp(System.currentTimeMillis());
                Timestamp dueDate = Timestamp.valueOf(LocalDate.now().plusDays(BORROW_DURATION_DAYS).atStartOfDay());

                int confirm = JOptionPane.showConfirmDialog(null,
                        "Borrow '" + bookTitle + "' for user " + user.getName() + " until " + dueDate + "?",
                        "Confirm Borrow", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    boolean success = BorrowedBookDAO.borrowBook(userId, bookId, borrowDate, dueDate);
                    if (success) {
                        JOptionPane.showMessageDialog(null,
                                "Book borrowed successfully! Due Date: " + dueDate,
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadAvailableBooks();
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "Failed to borrow book. It may no longer be available.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (NumberFormatException ex) {
                statusLabel.setText("Invalid User ID. Must be a number.");
            }
        }
    }
}