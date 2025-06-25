package com.library.gui.admin;

import com.library.dao.BorrowedBookDAO;
import com.library.dao.PaymentDAO;
import com.library.model.BorrowedBook;
import com.library.utils.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FineManagementPanel extends JPanel {
    private JTable fineTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JButton refreshButton;
    private JComboBox<String> filterComboBox;

    public FineManagementPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(40, 50, 70));

        // Title Panel
        JLabel titleLabel = new JLabel("💰 Fine Management", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        add(titleLabel, BorderLayout.NORTH);

        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        filterPanel.setBackground(new Color(240, 240, 240));

        filterComboBox = new JComboBox<>(new String[]{"All Fines", "Unpaid Fines", "Paid Fines"});
        filterComboBox.addActionListener(e -> loadFineData());

        refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> loadFineData());

        filterPanel.add(new JLabel("Filter:"));
        filterPanel.add(filterComboBox);
        filterPanel.add(refreshButton);
        add(filterPanel, BorderLayout.NORTH);

        // Table Setup
        String[] columnNames = {"Borrow ID", "User Name", "Book Title", "Due Date", "Fine Amount", "Status", "Action"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only Action column is editable
            }
            @Override
            public Class<?> getColumnClass(int column) {
                return switch (column) {
                    case 0 -> Integer.class; // Borrow ID
                    case 4 -> BigDecimal.class; // Fine Amount
                    default -> String.class;
                };
            }
        };

        fineTable = new JTable(tableModel);
        fineTable.setRowHeight(30);
        fineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fineTable.setAutoCreateRowSorter(true);

        // Set column widths
        fineTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Borrow ID
        fineTable.getColumnModel().getColumn(1).setPreferredWidth(150); // User Name
        fineTable.getColumnModel().getColumn(2).setPreferredWidth(200); // Book Title
        fineTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Due Date
        fineTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Fine Amount
        fineTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Status
        fineTable.getColumnModel().getColumn(6).setPreferredWidth(120); // Action

        JScrollPane scrollPane = new JScrollPane(fineTable);
        add(scrollPane, BorderLayout.CENTER);

        // Status Label
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);
        add(statusLabel, BorderLayout.SOUTH);

        // Load initial data
        loadFineData();
    }

    private void loadFineData() {
        statusLabel.setText("Loading fine data...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                tableModel.setRowCount(0);
                try {
                    String filter = (String) filterComboBox.getSelectedItem();
                    List<BorrowedBook> fines = BorrowedBookDAO.getFinesWithDetails(filter);

                    for (BorrowedBook fine : fines) {
                        SwingUtilities.invokeLater(() -> {
                            JButton actionButton;
                            if ("Paid".equals(fine.getStatus())) {
                                actionButton = new JButton("View Receipt");
                                actionButton.addActionListener(new ViewReceiptAction(fine.getId()));
                                actionButton.setBackground(new Color(100, 149, 237));
                            } else {
                                actionButton = new JButton("Record Payment");
                                actionButton.addActionListener(new RecordPaymentAction(fine.getId()));
                                actionButton.setBackground(new Color(50, 205, 50));
                            }
                            actionButton.setForeground(Color.WHITE);
                            actionButton.setFocusPainted(false);

                            tableModel.addRow(new Object[]{
                                    fine.getId(),
                                    fine.getUserName(),
                                    fine.getBookTitle(),
                                    fine.getDueDate(),
                                    fine.getFine(),
                                    fine.getStatus(),
                                    actionButton
                            });
                        });
                    }
                } catch (SQLException e) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(FineManagementPanel.this,
                                    "Error loading fines: " + e.getMessage(),
                                    "Database Error",
                                    JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("");
                } catch (InterruptedException | ExecutionException e) {
                    statusLabel.setText("Error loading fines");
                }
            }
        };
        worker.execute();
    }

    private class RecordPaymentAction implements ActionListener {
        private final int borrowId;

        public RecordPaymentAction(int borrowId) {
            this.borrowId = borrowId;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedRow = fineTable.getSelectedRow();
            if (selectedRow == -1) return;

            BigDecimal fineAmount = (BigDecimal) tableModel.getValueAt(selectedRow, 4);
            String paymentMethod = (String) JOptionPane.showInputDialog(
                    null,
                    "Enter payment method:",
                    "Record Payment",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{"Cash", "Credit Card", "Online Transfer"},
                    "Cash");

            if (paymentMethod != null) {
                int confirm = JOptionPane.showConfirmDialog(
                        null,
                        "Record payment of ₹" + fineAmount + " via " + paymentMethod + "?",
                        "Confirm Payment",
                        JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                        @Override
                        protected Boolean doInBackground() throws Exception {
                            Connection conn = null;
                            try {
                                conn = DBConnection.getConnection();
                                conn.setAutoCommit(false);

                                // 1. Update borrowed_books
                                if (!BorrowedBookDAO.clearFine(borrowId, conn)) {
                                    return false;
                                }

                                // 2. Record payment
                                int userId = (int) tableModel.getValueAt(selectedRow, 0);
                                String bookTitle = (String) tableModel.getValueAt(selectedRow, 2);
                                String description = "Fine payment for: " + bookTitle;

                                if (!PaymentDAO.recordPayment(userId, fineAmount, paymentMethod, description, conn)) {
                                    return false;
                                }

                                conn.commit();
                                return true;
                            } catch (SQLException ex) {
                                if (conn != null) conn.rollback();
                                throw ex;
                            } finally {
                                if (conn != null) conn.close();
                            }
                        }

                        @Override
                        protected void done() {
                            try {
                                if (get()) {
                                    JOptionPane.showMessageDialog(null, "Payment recorded successfully!");
                                    loadFineData();
                                }
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(null,
                                        "Error recording payment: " + ex.getMessage(),
                                        "Error",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    };
                    worker.execute();
                }
            }
        }
    }

    private class ViewReceiptAction implements ActionListener {
        private final int borrowId;

        public ViewReceiptAction(int borrowId) {
            this.borrowId = borrowId;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Implement receipt viewing logic
            JOptionPane.showMessageDialog(null,
                    "Receipt viewing functionality would be implemented here\n" +
                            "for transaction ID: " + borrowId,
                    "View Receipt",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}