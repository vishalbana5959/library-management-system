package com.library.gui.admin;

import com.library.utils.DBConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.concurrent.ExecutionException;

public class SearchBookPanel extends JPanel {
    private JTextField searchField;
    private JComboBox<String> searchCategory;
    private JComboBox<String> availabilityFilter;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JButton searchButton;

    public SearchBookPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 30));

        // Title Panel
        JLabel titleLabel = new JLabel("🔍 Search Books", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 153, 255));
        add(titleLabel, BorderLayout.NORTH);

        // Search Panel
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBackground(new Color(40, 40, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Search Components
        searchField = new JTextField(20);
        searchCategory = new JComboBox<>(new String[]{"Title", "Author", "Genre", "ISBN", "Publisher"});
        availabilityFilter = new JComboBox<>(new String[]{"All", "Available", "Unavailable"});
        searchButton = new JButton("Search");
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);

        // Configure search button
        searchButton.setBackground(new Color(52, 152, 219));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFont(new Font("Arial", Font.BOLD, 14));
        searchButton.addActionListener(new SearchAction());

        // Add components to search panel
        gbc.gridx = 0; gbc.gridy = 0;
        searchPanel.add(new JLabel("Search by:"), gbc);
        gbc.gridx = 1;
        searchPanel.add(searchCategory, gbc);
        gbc.gridx = 2;
        searchPanel.add(searchField, gbc);
        gbc.gridx = 3;
        searchPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 4;
        searchPanel.add(availabilityFilter, gbc);
        gbc.gridx = 5;
        searchPanel.add(searchButton, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 6;
        searchPanel.add(statusLabel, gbc);

        add(searchPanel, BorderLayout.NORTH);

        // Table Setup
        tableModel = new DefaultTableModel(
                new String[]{"ID", "Title", "Author", "Genre", "ISBN", "Publisher", "Status", "Qty"},
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };

        bookTable = new JTable(tableModel);
        bookTable.setBackground(new Color(50, 50, 50));
        bookTable.setForeground(Color.WHITE);
        bookTable.setSelectionBackground(new Color(70, 130, 180));
        bookTable.setAutoCreateRowSorter(true);
        bookTable.setFillsViewportHeight(true);

        // Set column widths
        bookTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Title
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Author
        bookTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Genre
        bookTable.getColumnModel().getColumn(4).setPreferredWidth(120); // ISBN
        bookTable.getColumnModel().getColumn(5).setPreferredWidth(150); // Publisher
        bookTable.getColumnModel().getColumn(6).setPreferredWidth(80);  // Status
        bookTable.getColumnModel().getColumn(7).setPreferredWidth(50);  // Qty

        JScrollPane scrollPane = new JScrollPane(bookTable);
        add(scrollPane, BorderLayout.CENTER);

        // Load initial data
        searchBooks("", "title", "All");
    }

    private class SearchAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String queryText = searchField.getText().trim();
            String category = searchCategory.getSelectedItem().toString().toLowerCase();
            String availability = availabilityFilter.getSelectedItem().toString();

            searchBooks(queryText, category, availability);
        }
    }

    private void searchBooks(String queryText, String category, String availability) {
        statusLabel.setText("Searching...");
        searchButton.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                tableModel.setRowCount(0);

                String sql = "SELECT b.id, b.title, b.author, b.genre, b.isbn, b.publisher, " +
                        "b.quantity, b.available, " +
                        "CASE WHEN b.available > 0 THEN 'Available' ELSE 'Unavailable' END as status " +
                        "FROM books b WHERE 1=1";

                // Add search condition
                if (!queryText.isEmpty()) {
                    sql += " AND LOWER(b." + category + ") LIKE ?";
                }

                // Add availability condition
                if (!"All".equals(availability)) {
                    sql += " AND b.available " + ("Available".equals(availability) ? "> 0" : "= 0");
                }

                sql += " ORDER BY b.title";

                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {

                    if (!queryText.isEmpty()) {
                        stmt.setString(1, "%" + queryText.toLowerCase() + "%");
                    }

                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        SwingUtilities.invokeLater(() -> {
                            try {
                                tableModel.addRow(new Object[]{
                                        rs.getInt("id"),
                                        rs.getString("title"),
                                        rs.getString("author"),
                                        rs.getString("genre"),
                                        rs.getString("isbn"),
                                        rs.getString("publisher"),
                                        rs.getString("status"),
                                        rs.getInt("available") + "/" + rs.getInt("quantity")
                                });
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                } catch (SQLException e) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(SearchBookPanel.this,
                                    "Error searching books: " + e.getMessage(),
                                    "Database Error",
                                    JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    statusLabel.setText("");
                } catch (InterruptedException | ExecutionException e) {
                    statusLabel.setText("Search failed");
                } finally {
                    searchButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }
}