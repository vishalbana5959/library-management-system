package com.library.gui.admin;

import com.library.dao.BorrowedBookDAO;
import com.library.dao.UserDAO;
import com.library.model.BorrowedBook;
import com.library.model.User;
import com.library.utils.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class AdminReturnBookPanel extends JPanel {
    private JTable borrowedBooksTable;
    private DefaultTableModel tableModel;
    private JButton returnButton, refreshButton;
    private JTextField userIdField;
    private JLabel statusLabel;

    public AdminReturnBookPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(240, 240, 240));

        // Title Panel
        JLabel titleLabel = new JLabel("🔄 Admin - Return Book", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 153, 255));
        add(titleLabel, BorderLayout.NORTH);

        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("User ID:"));
        userIdField = new JTextField(10);
        searchPanel.add(userIdField);
        JButton searchButton = new JButton("🔍 Search");
        searchButton.addActionListener(e -> loadBorrowedBooks());
        searchPanel.add(searchButton);
        add(searchPanel, BorderLayout.NORTH);

        // Table Setup
        tableModel = new DefaultTableModel(new String[]{"Borrow ID", "Book Title", "Author", "Borrow Date", "Due Date", "Fine"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Integer.class; // Borrow ID
                if (columnIndex == 3 || columnIndex == 4) return Date.class; // Dates
                return String.class;
            }
        };
        borrowedBooksTable = new JTable(tableModel);
        borrowedBooksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(borrowedBooksTable), BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        returnButton = new JButton("📕 Return Selected Book");
        returnButton.addActionListener(new ReturnBookAction());
        refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> loadBorrowedBooks());
        statusLabel = new JLabel("", SwingConstants.CENTER);

        buttonPanel.add(returnButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(statusLabel);
        add(buttonPanel, BorderLayout.SOUTH);

        loadBorrowedBooks();
    }

    private void loadBorrowedBooks() {
        tableModel.setRowCount(0);
        String userIdText = userIdField.getText().trim();

        if (userIdText.isEmpty()) {
            statusLabel.setText("Please enter a User ID to search.");
            return;
        }

        try {
            int userId = Integer.parseInt(userIdText);
            User user = UserDAO.getUserById(userId);
            if (user == null) {
                statusLabel.setText("User not found!");
                return;
            }

            List<BorrowedBook> borrowedBooks = BorrowedBookDAO.getUserBorrowedBooks(userId);
            for (BorrowedBook book : borrowedBooks) {
                tableModel.addRow(new Object[]{
                        book.getId(),
                        book.getBookTitle(),
                        book.getUserName(),
                        new java.sql.Date(book.getBorrowDate().getTime()),
                        new java.sql.Date(book.getDueDate().getTime()),
                        book.getFine() != null ? "₹" + book.getFine() : "No Fine"
                });
            }
            statusLabel.setText("Found " + borrowedBooks.size() + " borrowed books for user: " + user.getName());
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid User ID. Must be a number.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading borrowed books: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private class ReturnBookAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedRow = borrowedBooksTable.getSelectedRow();
            if (selectedRow == -1) {
                statusLabel.setText("Please select a book to return.");
                return;
            }

            int borrowId = (int) tableModel.getValueAt(selectedRow, 0);
            String bookTitle = (String) tableModel.getValueAt(selectedRow, 1);
            String fineAmount = (String) tableModel.getValueAt(selectedRow, 5);
            BigDecimal fine = fineAmount.contains("₹") ?
                    new BigDecimal(fineAmount.replace("₹", "").trim()) : BigDecimal.ZERO;

            int confirm = JOptionPane.showConfirmDialog(null,
                    "Return '" + bookTitle + "'?" + (fine.compareTo(BigDecimal.ZERO) > 0 ?
                            "\nFine of " + fineAmount + " will be recorded." : ""),
                    "Confirm Return", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                // Get current date
                Date returnDate = new Date(System.currentTimeMillis());

                // Update the borrowed book record with return date
                boolean success = BorrowedBookDAO.returnBook(borrowId);

                if (success) {
                    JOptionPane.showMessageDialog(null,
                            "Book returned successfully on " + returnDate +
                                    (fine.compareTo(BigDecimal.ZERO) > 0 ?
                                            "\nFine of " + fineAmount + " recorded." : ""),
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadBorrowedBooks();
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Failed to return book.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}