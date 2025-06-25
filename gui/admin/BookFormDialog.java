package com.library.gui.admin;

import com.library.dao.BookDAO;
import com.library.model.Book;

import javax.swing.*;
import java.awt.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class BookFormDialog extends JDialog {
    private JTextField titleField, authorField, genreField, isbnField, publisherField, quantityField;
    private JButton saveButton, cancelButton;
    private Book book;
    private BookManagementPanel parentPanel;

    public BookFormDialog(Book book, BookManagementPanel parentPanel) {
        this.book = book;
        this.parentPanel = parentPanel;

        setTitle(book == null ? "➕ Add Book" : "✏️ Edit Book");
        setSize(500, 400);
        setModal(true);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        // Labels and Fields
        gbc.gridy = 0;
        addField("📖 Title:", titleField = new JTextField(), gbc);
        gbc.gridy++;
        addField("✍️ Author:", authorField = new JTextField(), gbc);
        gbc.gridy++;
        addField("📚 Genre:", genreField = new JTextField(), gbc);
        gbc.gridy++;
        addField("📌 ISBN:", isbnField = new JTextField(), gbc);
        gbc.gridy++;
        addField("🏢 Publisher:", publisherField = new JTextField(), gbc);
        gbc.gridy++;
        addField("📦 Quantity:", quantityField = new JTextField(), gbc);

        // Buttons
        JPanel buttonPanel = new JPanel();
        saveButton = new JButton("💾 Save");
        cancelButton = new JButton("❌ Cancel");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        gbc.gridy++;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        // If editing, fill fields with book details
        if (book != null) {
            titleField.setText(book.getTitle());
            authorField.setText(book.getAuthor());
            genreField.setText(book.getGenre());
            isbnField.setText(book.getIsbn());
            publisherField.setText(book.getPublisher());
            quantityField.setText(String.valueOf(book.getQuantity()));
        }

        // Button Actions
        saveButton.addActionListener(e -> saveBook());
        cancelButton.addActionListener(e -> dispose());

        setVisible(true);
    }

    private void addField(String label, JTextField field, GridBagConstraints gbc) {
        gbc.gridx = 0;
        add(new JLabel(label), gbc);
        gbc.gridx = 1;
        add(field, gbc);
    }

    private void saveBook() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String genre = genreField.getText().trim();
        String isbn = isbnField.getText().trim();
        String publisher = publisherField.getText().trim();
        int quantity;

        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "❌ Quantity must be a positive number.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (title.isEmpty() || author.isEmpty() || genre.isEmpty() || isbn.isEmpty() || publisher.isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠️ All fields must be filled.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Timestamp addedAt = book == null ? Timestamp.valueOf(LocalDateTime.now()) : book.getAddedAt();

        if (book == null) {
            // Add new book
            Book newBook = new Book(0, title, author, genre, isbn, publisher, quantity, quantity, addedAt);
            BookDAO.addBook(newBook);
        } else {
            // Update existing book
            book.setTitle(title);
            book.setAuthor(author);
            book.setGenre(genre);
            book.setIsbn(isbn);
            book.setPublisher(publisher);
            book.setQuantity(quantity);
            book.setAvailable(quantity); // Reset availability to full quantity
            BookDAO.updateBook(book);
        }

        parentPanel.loadBooks(); // Ensure this method exists in BookManagementPanel
        dispose();
    }
}
