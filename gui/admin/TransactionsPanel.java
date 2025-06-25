package com.library.gui.admin;

import com.library.dao.BorrowedBookDAO;
import com.library.model.BorrowedBook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class TransactionsPanel extends JPanel {
    private JTable transactionsTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JComboBox<String> filterComboBox;

    public TransactionsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(240, 240, 240));

        // Title Panel
        JLabel titleLabel = new JLabel("📋 All Transactions", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(70, 130, 180));
        add(titleLabel, BorderLayout.NORTH);

        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        filterPanel.setBackground(new Color(230, 230, 230));

        filterComboBox = new JComboBox<>(new String[]{"All Transactions", "Borrowed", "Returned"});
        filterComboBox.addActionListener(e -> loadTransactions());

        refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> loadTransactions());
        refreshButton.setForeground(Color.BLACK);

        filterPanel.add(new JLabel("Filter:"));
        filterPanel.add(filterComboBox);
        filterPanel.add(refreshButton);

        add(filterPanel, BorderLayout.NORTH);

        // Table Setup
        tableModel = new DefaultTableModel(
                new String[]{"ID", "User", "Book", "Borrow Date", "Due Date", "Return Date", "Status", "Fine"},
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        transactionsTable = new JTable(tableModel);
        transactionsTable.setRowHeight(30);
        transactionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transactionsTable.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(transactionsTable);
        add(scrollPane, BorderLayout.CENTER);

        // Load initial data
        loadTransactions();
    }

    private void loadTransactions() {
        tableModel.setRowCount(0);
        String filter = (String) filterComboBox.getSelectedItem();

        try {
            List<BorrowedBook> transactions = BorrowedBookDAO.getAllTransactions(filter);
            for (BorrowedBook transaction : transactions) {
                tableModel.addRow(new Object[]{
                        transaction.getId(),
                        transaction.getUserName(),
                        transaction.getBookTitle(),
                        transaction.getBorrowDate(),
                        transaction.getDueDate(),
                        transaction.getReturnDate(),
                        transaction.getStatus(),
                        transaction.getFine() != null ? "₹" + transaction.getFine() : "No Fine"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading transactions: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}