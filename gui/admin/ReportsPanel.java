package com.library.gui.admin;

import com.opencsv.CSVWriter;
import com.library.utils.DBConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportsPanel extends JPanel {
    private JTable reportsTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> reportTypeComboBox;
    private JButton generateReportButton;
    private JButton exportButton;
    private JLabel statusLabel;
    private JPanel chartPanel;
    private static final Logger logger = Logger.getLogger(ReportsPanel.class.getName());

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(45, 45, 45));

        // Title Panel
        JLabel titleLabel = new JLabel("📊 Reports & Analytics", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 204, 102));
        add(titleLabel, BorderLayout.NORTH);

        // Filter Panel
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        reportTypeComboBox = new JComboBox<>(new String[]{
                "Current Borrowings",
                "Recent Returns",
                "Overdue Books",
                "User Activity",
                "Popular Books",
                "Monthly Statistics"
        });

        generateReportButton = new JButton("📄 Generate Report");
        generateReportButton.setBackground(new Color(0, 153, 255));
        generateReportButton.setForeground(Color.BLACK);
        generateReportButton.setFont(new Font("Arial", Font.BOLD, 14));
        generateReportButton.addActionListener(e -> generateReport());

        exportButton = new JButton("💾 Export to CSV");
        exportButton.setBackground(new Color(76, 175, 80));
        exportButton.setForeground(Color.BLACK);
        exportButton.addActionListener(e -> exportToCSV());

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);

        // Add components to filter panel
        gbc.gridx = 0; gbc.gridy = 0;
        filterPanel.add(new JLabel("Report Type:"), gbc);
        gbc.gridx = 1;
        filterPanel.add(reportTypeComboBox, gbc);
        gbc.gridx = 2;
        filterPanel.add(generateReportButton, gbc);
        gbc.gridx = 3;
        filterPanel.add(exportButton, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4;
        filterPanel.add(statusLabel, gbc);

        add(filterPanel, BorderLayout.NORTH);

        // Table Setup
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        reportsTable = new JTable(tableModel);
        reportsTable.setBackground(new Color(240, 240, 240)); // 60, 60, 60
        reportsTable.setForeground(Color.BLACK);//white
        reportsTable.setSelectionBackground(new Color(0, 0, 0)); //100, 149, 237
        reportsTable.setAutoCreateRowSorter(true);
        reportsTable.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(reportsTable);
        add(scrollPane, BorderLayout.CENTER);

        // Initialize with default report
        generateReport();
    }

    private void generateReport() {
        String selectedReport = reportTypeComboBox.getSelectedItem().toString();
        statusLabel.setText("Generating " + selectedReport + " report...");
        generateReportButton.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                tableModel.setRowCount(0);

                String sql = "";
                String[] columnNames = {};

                switch (selectedReport) {
                    case "Current Borrowings":
                        sql = "SELECT bb.id, u.name AS user_name, b.title AS book_title, " +
                                "b.author, bb.borrow_date, bb.due_date " +
                                "FROM borrowed_books bb " +
                                "JOIN users u ON bb.user_id = u.id " +
                                "JOIN books b ON bb.book_id = b.id " +
                                "WHERE bb.status = 'borrowed' " +
                                "ORDER BY bb.due_date";
                        columnNames = new String[]{"Borrow ID", "User Name", "Book Title", "Author", "Borrow Date", "Due Date"};
                        break;

                    case "Recent Returns":
                        sql = "SELECT bb.id, u.name AS user_name, b.title AS book_title, " +
                                "b.author, bb.borrow_date, bb.return_date, bb.fine " +
                                "FROM borrowed_books bb " +
                                "JOIN users u ON bb.user_id = u.id " +
                                "JOIN books b ON bb.book_id = b.id " +
                                "WHERE bb.status = 'returned' AND bb.return_date >= ? " +
                                "ORDER BY bb.return_date DESC";
                        columnNames = new String[]{"Borrow ID", "User Name", "Book Title", "Author", "Borrow Date", "Return Date", "Fine (₹)"};
                        break;

                    case "Overdue Books":
                        sql = "SELECT bb.id, u.name AS user_name, b.title AS book_title, " +
                                "b.author, bb.due_date, DATEDIFF(CURRENT_DATE, bb.due_date) AS overdue_days, " +
                                "bb.fine AS current_fine " +
                                "FROM borrowed_books bb " +
                                "JOIN users u ON bb.user_id = u.id " +
                                "JOIN books b ON bb.book_id = b.id " +
                                "WHERE bb.status = 'borrowed' AND bb.due_date < CURRENT_DATE " +
                                "ORDER BY overdue_days DESC";
                        columnNames = new String[]{"Borrow ID", "User Name", "Book Title", "Author", "Due Date", "Days Overdue", "Fine (₹)"};
                        break;

                    case "User Activity":
                        sql = "SELECT u.id, u.name, " +
                                "COUNT(bb.id) AS total_borrowed, " +
                                "SUM(CASE WHEN bb.status = 'returned' THEN 1 ELSE 0 END) AS books_returned, " +
                                "SUM(CASE WHEN bb.due_date < CURRENT_DATE AND bb.status = 'borrowed' THEN 1 ELSE 0 END) AS overdue_books " +
                                "FROM users u " +
                                "LEFT JOIN borrowed_books bb ON u.id = bb.user_id " +
                                "GROUP BY u.id, u.name " +
                                "ORDER BY total_borrowed DESC";
                        columnNames = new String[]{"User ID", "Name", "Total Borrowed", "Books Returned", "Overdue Books"};
                        break;

                    case "Popular Books":
                        sql = "SELECT b.id, b.title, b.author, b.genre, " +
                                "COUNT(bb.id) AS times_borrowed, " +
                                "b.quantity - b.available AS currently_borrowed " +
                                "FROM books b " +
                                "LEFT JOIN borrowed_books bb ON b.id = bb.book_id " +
                                "GROUP BY b.id, b.title, b.author, b.genre, b.quantity, b.available " +
                                "ORDER BY times_borrowed DESC " +
                                "LIMIT 20";
                        columnNames = new String[]{"Book ID", "Title", "Author", "Genre", "Times Borrowed", "Currently Borrowed"};
                        break;

                    case "Monthly Statistics":
                        sql = "SELECT DATE_FORMAT(bb.borrow_date, '%Y-%m') AS month, " +
                                "COUNT(*) AS books_borrowed, " +
                                "SUM(CASE WHEN bb.status = 'returned' THEN 1 ELSE 0 END) AS books_returned, " +
                                "SUM(CASE WHEN bb.due_date < CURRENT_DATE AND bb.status = 'borrowed' THEN 1 ELSE 0 END) AS overdue_books, " +
                                "SUM(bb.fine) AS total_fines " +
                                "FROM borrowed_books bb " +
                                "WHERE bb.borrow_date >= DATE_SUB(CURRENT_DATE, INTERVAL 12 MONTH) " +
                                "GROUP BY DATE_FORMAT(bb.borrow_date, '%Y-%m') " +
                                "ORDER BY month DESC";
                        columnNames = new String[]{"Month", "Books Borrowed", "Books Returned", "Overdue Books", "Total Fines (₹)"};
                        break;
                }

                String[] finalColumnNames = columnNames;
                SwingUtilities.invokeLater(() -> {
                    tableModel.setColumnIdentifiers(finalColumnNames);
                });

                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {

                    // Set parameter for Recent Returns report
                    if ("Recent Returns".equals(selectedReport)) {
                        stmt.setDate(1, Date.valueOf(LocalDate.now().minusMonths(1)));
                    }

                    ResultSet rs = stmt.executeQuery();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next()) {
                        Object[] rowData = new Object[columnCount];
                        for (int i = 0; i < columnCount; i++) {
                            rowData[i] = rs.getObject(i + 1);
                        }
                        SwingUtilities.invokeLater(() -> {
                            tableModel.addRow(rowData);
                        });
                    }
                } catch (SQLException e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(ReportsPanel.this,
                                "Error generating report: " + e.getMessage(),
                                "Database Error",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setForeground(Color.WHITE);
                    statusLabel.setText("Report generated successfully");
                } catch (InterruptedException | ExecutionException e) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Error generating report");
                } finally {
                    generateReportButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void exportToCSV() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "No data to export. Please generate a report first.",
                    "Export Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Report as CSV");
        fileChooser.setSelectedFile(new File("library_report.csv"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();
        // Ensure the file has .csv extension
        if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
        }

        // Show progress dialog
        JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Exporting...", true);
        progressDialog.setLayout(new BorderLayout());
        progressDialog.add(new JLabel("Exporting report to CSV...", JLabel.CENTER), BorderLayout.CENTER);
        progressDialog.setSize(300, 100);
        progressDialog.setLocationRelativeTo(this);

        File finalFileToSave = fileToSave;
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try (CSVWriter writer = new CSVWriter(new FileWriter(finalFileToSave))) {
                    // Write header
                    int columnCount = tableModel.getColumnCount();
                    String[] header = new String[columnCount];
                    for (int i = 0; i < columnCount; i++) {
                        header[i] = tableModel.getColumnName(i);
                    }
                    writer.writeNext(header);

                    // Write data
                    for (int row = 0; row < tableModel.getRowCount(); row++) {
                        String[] rowData = new String[columnCount];
                        for (int col = 0; col < columnCount; col++) {
                            Object value = tableModel.getValueAt(row, col);
                            rowData[col] = (value != null) ? value.toString() : "";
                        }
                        writer.writeNext(rowData);
                    }

                    return true;
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error writing CSV file", e);
                    return false;
                }
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(ReportsPanel.this,
                                "Report successfully exported to:\n" + finalFileToSave.getAbsolutePath(),
                                "Export Successful",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(ReportsPanel.this,
                                "Failed to export report to CSV.",
                                "Export Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ReportsPanel.this,
                            "Error during export: " + e.getMessage(),
                            "Export Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }
}