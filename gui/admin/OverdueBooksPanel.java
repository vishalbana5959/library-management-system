package com.library.gui.admin;

import com.library.utils.DBConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;

public class OverdueBooksPanel extends JPanel {
    private JTable overdueTable;
    private DefaultTableModel tableModel;
    private JButton markPaidButton;
    private JButton refreshButton;
    private JLabel statusLabel;
    public static final int FINE_PER_DAY = 5; // ₹5 per day fine

    public OverdueBooksPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(240, 240, 240));

        // Title Panel
        JLabel titleLabel = new JLabel("📌 Overdue Books & Fines", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(200, 0, 0));
        add(titleLabel, BorderLayout.NORTH);

        // Table Setup
        tableModel = new DefaultTableModel(
                new String[]{"Borrow ID", "User Name", "Book Title", "Author", "Due Date", "Days Overdue", "Fine (₹)"},
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0, 5 -> Integer.class; // Borrow ID, Days Overdue
                    case 6 -> BigDecimal.class; // Fine
                    default -> String.class;
                };
            }
        };

        overdueTable = new JTable(tableModel);
        overdueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        overdueTable.setAutoCreateRowSorter(true);
        overdueTable.setFillsViewportHeight(true);

        // Set column widths
        overdueTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Borrow ID
        overdueTable.getColumnModel().getColumn(1).setPreferredWidth(150); // User Name
        overdueTable.getColumnModel().getColumn(2).setPreferredWidth(200); // Book Title
        overdueTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Author
        overdueTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Due Date
        overdueTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // Days Overdue
        overdueTable.getColumnModel().getColumn(6).setPreferredWidth(80);  // Fine

        JScrollPane scrollPane = new JScrollPane(overdueTable);
        add(scrollPane, BorderLayout.CENTER);

        // Button Panel
        markPaidButton = new JButton("✅ Mark Fine as Paid");
        markPaidButton.setBackground(new Color(34, 177, 76));
        markPaidButton.setForeground(Color.BLACK);
        markPaidButton.setFont(new Font("Arial", Font.BOLD, 14));
        markPaidButton.setFocusPainted(false);
        markPaidButton.addActionListener(new MarkFinePaidAction());

        refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> loadOverdueBooks());

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.add(markPaidButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(statusLabel);
        add(buttonPanel, BorderLayout.SOUTH);

        loadOverdueBooks();
    }

    private void loadOverdueBooks() {
        tableModel.setRowCount(0);
        statusLabel.setText("Loading overdue books...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "SELECT bb.id, u.name, b.title, b.author, bb.due_date, bb.fine, bb.return_date " +
                                     "FROM borrowed_books bb " +
                                     "JOIN users u ON bb.user_id = u.id " +
                                     "JOIN books b ON bb.book_id = b.id " +
                                     "WHERE bb.status = 'borrowed' AND bb.due_date < CURRENT_DATE " +
                                     "ORDER BY bb.due_date")) {

                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        int borrowId = rs.getInt("id");
                        String userName = rs.getString("name");
                        String bookTitle = rs.getString("title");
                        String author = rs.getString("author");
                        Date dueDate = rs.getDate("due_date");
                        BigDecimal fine = rs.getBigDecimal("fine");

                        // Calculate days overdue and current fine
                        LocalDate due = dueDate.toLocalDate();
                        long daysOverdue = ChronoUnit.DAYS.between(due, LocalDate.now());
                        BigDecimal currentFine = (fine != null) ? fine :
                                BigDecimal.valueOf(daysOverdue * FINE_PER_DAY);

                        SwingUtilities.invokeLater(() -> {
                            tableModel.addRow(new Object[]{
                                    borrowId,
                                    userName,
                                    bookTitle,
                                    author,
                                    dueDate,
                                    daysOverdue,
                                    currentFine
                            });
                        });
                    }
                } catch (SQLException e) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(OverdueBooksPanel.this,
                                    "Error loading overdue books: " + e.getMessage(),
                                    "Database Error",
                                    JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }

            @Override
            protected void done() {
                statusLabel.setText("");
            }
        };
        worker.execute();
    }

    private class MarkFinePaidAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedRow = overdueTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(null,
                        "Please select a fine to mark as paid.",
                        "No Selection",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int borrowId = (int) tableModel.getValueAt(selectedRow, 0);
            String userName = (String) tableModel.getValueAt(selectedRow, 1);
            BigDecimal fineAmount = (BigDecimal) tableModel.getValueAt(selectedRow, 6);

            int confirm = JOptionPane.showConfirmDialog(null,
                    "Mark fine of ₹" + fineAmount + " for " + userName + " as paid?",
                    "Confirm Payment",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                markFineAsPaid(borrowId, fineAmount);
            }
        }
    }

    private void markFineAsPaid(int borrowId, BigDecimal fineAmount) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                Connection conn = null;
                try {
                    conn = DBConnection.getConnection();
                    conn.setAutoCommit(false);

                    // 1. Update borrowed_books record
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "UPDATE borrowed_books SET fine = 0 WHERE id = ?")) {
                        stmt.setInt(1, borrowId);
                        if (stmt.executeUpdate() == 0) {
                            throw new SQLException("Failed to update fine status");
                        }
                    }

                    // 2. Record payment in payments table
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO payments (user_id, amount, payment_date, description) " +
                                    "VALUES ((SELECT user_id FROM borrowed_books WHERE id = ?), ?, CURRENT_DATE, ?)")) {
                        stmt.setInt(1, borrowId);
                        stmt.setBigDecimal(2, fineAmount);
                        stmt.setString(3, "Late return fine payment");
                        stmt.executeUpdate();
                    }

                    conn.commit();
                    return true;
                } catch (SQLException e) {
                    if (conn != null) conn.rollback();
                    throw e;
                } finally {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        conn.close();
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(null,
                                "Fine marked as paid successfully.",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        loadOverdueBooks();
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(OverdueBooksPanel.this,
                            "Error updating fine: " + e.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}