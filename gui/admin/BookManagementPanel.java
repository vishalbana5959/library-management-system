package com.library.gui.admin;

import com.library.dao.BookDAO;
import com.library.model.Book;
import com.library.utils.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BookManagementPanel extends JPanel {
    private JTable bookTable;
    private JTextField searchField;
    private JButton addButton, editButton, deleteButton, refreshButton;
    private DefaultTableModel model;
    private JLabel statusLabel;

    public BookManagementPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE); // Changed to white to match dashboard content area
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // Added padding

        // Title Panel
        JLabel titleLabel = new JLabel("📚 Book Management", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24)); // Consistent font
        titleLabel.setForeground(new Color(50, 65, 90)); // Matching sidebar color
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0)); // Added spacing
        add(titleLabel, BorderLayout.NORTH);

        // Search Panel
        JPanel searchPanel = new JPanel(new BorderLayout(10, 10));
        searchPanel.setBackground(new Color(230, 230, 230));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0)); // Added spacing

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setToolTipText("Search by Title, Author, ISBN, Genre or Publisher");
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)), // Light gray border
                BorderFactory.createEmptyBorder(8, 8, 8, 8))); // Inner padding
        searchField.addActionListener(e -> searchBooks());

        JButton searchButton = createStyledButton("🔍 Search", new Color(70, 90, 120)); // Matching sidebar button color
        searchButton.setPreferredSize(new Dimension(120, 40)); // Consistent button size
        searchButton.addActionListener(e -> searchBooks());

        JPanel searchFieldPanel = new JPanel(new BorderLayout());
        searchFieldPanel.add(searchField, BorderLayout.CENTER);
        searchFieldPanel.add(searchButton, BorderLayout.EAST);

        searchPanel.add(searchFieldPanel, BorderLayout.CENTER);
        add(searchPanel, BorderLayout.NORTH);

        // Table Setup
        String[] columns = {"ID", "Title", "Author", "Genre", "ISBN", "Publisher", "Total Qty", "Available", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0, 6, 7 -> Integer.class;
                    default -> String.class;
                };
            }
        };

        bookTable = new JTable(model);
        bookTable.setRowHeight(35); // Slightly taller rows
        bookTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookTable.setAutoCreateRowSorter(true);
        bookTable.setFillsViewportHeight(true);
        bookTable.setGridColor(new Color(230, 230, 230)); // Light grid lines
        bookTable.setSelectionBackground(new Color(220, 235, 250)); // Light blue selection
        bookTable.setSelectionForeground(Color.BLACK);
        bookTable.setIntercellSpacing(new Dimension(0, 1)); // Spacing between rows
        bookTable.setShowGrid(false); // Hide grid lines for cleaner look

        // Header styling
        bookTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        bookTable.getTableHeader().setBackground(new Color(70, 90, 120)); // Matching sidebar
        bookTable.getTableHeader().setForeground(Color.BLACK);//WHITE
        bookTable.getTableHeader().setPreferredSize(new Dimension(0, 35)); // Taller header

        // Set column widths
        bookTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(250); // Wider for titles
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        bookTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        bookTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        bookTable.getColumnModel().getColumn(5).setPreferredWidth(180);
        bookTable.getColumnModel().getColumn(6).setPreferredWidth(80);
        bookTable.getColumnModel().getColumn(7).setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(bookTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200))); // Light border
        scrollPane.setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);

        addButton = createStyledButton("➕ Add Book", new Color(0, 153, 76));
        editButton = createStyledButton("✏️ Edit", new Color(255, 204, 0));
        deleteButton = createStyledButton("🗑️ Delete", new Color(204, 0, 0));
        refreshButton = createStyledButton("🔄 Refresh", new Color(0, 102, 204));

        // Consistent button sizes
        Dimension buttonSize = new Dimension(140, 40);
        addButton.setPreferredSize(buttonSize);
        editButton.setPreferredSize(buttonSize);
        deleteButton.setPreferredSize(buttonSize);
        refreshButton.setPreferredSize(buttonSize);

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);

        // Status Label
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(new Color(100, 100, 100)); // Dark gray
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(245, 78, 22));//WHITE
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0)); // Added spacing
        add(bottomPanel, BorderLayout.SOUTH);

        // Button actions (unchanged)
        addButton.addActionListener(e -> addBook());
        editButton.addActionListener(e -> editBook());
        deleteButton.addActionListener(e -> deleteBook());
        refreshButton.addActionListener(e -> loadBooks());

        // Load initial data
        loadBooks();
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.BLACK);//WHITE
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect to match dashboard
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(
                        Math.min(bgColor.getRed() + 30, 255),
                        Math.min(bgColor.getGreen() + 30, 255),
                        Math.min(bgColor.getBlue() + 30, 255)
                ));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    public void loadBooks() {
        statusLabel.setText("Loading books...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                model.setRowCount(0);
                List<Book> books = BookDAO.getAllBooks();
                for (Book book : books) {
                    SwingUtilities.invokeLater(() -> {
                        model.addRow(new Object[]{
                                book.getBookId(),
                                book.getTitle(),
                                book.getAuthor(),
                                book.getGenre(),
                                book.getIsbn(),
                                book.getPublisher(),
                                book.getQuantity(),
                                book.getAvailable(),
                                getStatusText(book.getQuantity(), book.getAvailable())
                        });
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("");
                } catch (InterruptedException | ExecutionException e) {
                    statusLabel.setText("Error loading books");
                }
            }
        };
        worker.execute();
    }

    private String getStatusText(int quantity, int available) {
        if (quantity == 0) return "Out of Stock";
        if (available == 0) return "All Borrowed";
        if (available == quantity) return "All Available";
        return "Partially Available";
    }

    private void addBook() {
        new BookFormDialog(null, this).setVisible(true);
    }

    private void editBook() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a book to edit.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = bookTable.convertRowIndexToModel(selectedRow);
        int bookId = (int) model.getValueAt(modelRow, 0);

        Book book = BookDAO.getBookById(bookId);
        new BookFormDialog(book, this).setVisible(true);
    }

    private void deleteBook() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a book to delete.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = bookTable.convertRowIndexToModel(selectedRow);
        int bookId = (int) model.getValueAt(modelRow, 0);
        String bookTitle = (String) model.getValueAt(modelRow, 1);

        // Check if book is currently borrowed
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM borrowed_books WHERE book_id = ? AND status = 'borrowed'")) {
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this,
                        "Cannot delete book. There are " + rs.getInt(1) + " copies currently borrowed.",
                        "Delete Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error checking book status: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete book '" + bookTitle + "'? This action cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return BookDAO.deleteBook(bookId);
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(BookManagementPanel.this,
                                    "Book deleted successfully.",
                                    "Success",
                                    JOptionPane.INFORMATION_MESSAGE);
                            loadBooks();
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(BookManagementPanel.this,
                                "Error deleting book: " + e.getMessage(),
                                "Database Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void searchBooks() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            loadBooks();
            return;
        }

        statusLabel.setText("Searching books...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                model.setRowCount(0);
                List<Book> books = BookDAO.searchBooks(query);
                for (Book book : books) {
                    SwingUtilities.invokeLater(() -> {
                        model.addRow(new Object[]{
                                book.getBookId(),
                                book.getTitle(),
                                book.getAuthor(),
                                book.getGenre(),
                                book.getIsbn(),
                                book.getPublisher(),
                                book.getQuantity(),
                                book.getAvailable(),
                                getStatusText(book.getQuantity(), book.getAvailable())
                        });
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("");
                } catch (InterruptedException | ExecutionException e) {
                    statusLabel.setText("Error searching books");
                }
            }
        };
        worker.execute();
    }
}