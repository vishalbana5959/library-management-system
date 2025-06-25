package com.library.dao;

import com.library.model.Book;
import com.library.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BookDAO {

    private static final Logger logger = Logger.getLogger(BookDAO.class.getName());

    // Add a new book if it doesn't exist
    public static boolean addBook(Book book) {
        if (bookExists(book.getIsbn())) {
            System.out.println("⚠️ Book with ISBN '" + book.getIsbn() + "' already exists! Skipping insertion.");
            return false;
        }

        String query = "INSERT INTO books (id, title, author, genre, isbn, publisher, quantity, available, added_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, book.getBookId());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getAuthor());
            pstmt.setString(4, book.getGenre());
            pstmt.setString(5, book.getIsbn());
            pstmt.setString(6, book.getPublisher());
            pstmt.setInt(7, book.getQuantity());
            pstmt.setInt(8, book.getAvailable());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error adding book: " + e.getMessage());
            return false;
        }
    }

    // Fetch all books
    public static List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String query = "SELECT id, title, author, genre, isbn, publisher, quantity, available, added_at FROM books";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("genre"),
                        rs.getString("isbn"),
                        rs.getString("publisher"),
                        rs.getInt("quantity"),
                        rs.getInt("available"),
                        rs.getTimestamp("added_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching books: " + e.getMessage());
        }
        return books;
    }

    // ✅ **NEW: Get a book by ID**
    public static Book getBookById(int bookId) {
        String query = "SELECT id, title, author, genre, isbn, publisher, quantity, available, added_at FROM books WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, bookId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("genre"),
                        rs.getString("isbn"),
                        rs.getString("publisher"),
                        rs.getInt("quantity"),
                        rs.getInt("available"),
                        rs.getTimestamp("added_at")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching book by ID: " + e.getMessage());
        }
        return null;
    }

    // ✅ **NEW: Search books by Title, Author, or ISBN**
    public static List<Book> searchBooks(String query) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT id, title, author, genre, isbn, publisher, quantity, available, added_at FROM books " +
                "WHERE title LIKE ? OR author LIKE ? OR isbn LIKE ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + query + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("genre"),
                        rs.getString("isbn"),
                        rs.getString("publisher"),
                        rs.getInt("quantity"),
                        rs.getInt("available"),
                        rs.getTimestamp("added_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error searching books: " + e.getMessage());
        }
        return books;
    }

    // Update book details
    public static boolean updateBook(Book book) {
        String query = "UPDATE books SET title = ?, author = ?, genre = ?, isbn = ?, publisher = ?, quantity = ?, available = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, book.getTitle());
            pstmt.setString(2, book.getAuthor());
            pstmt.setString(3, book.getGenre());
            pstmt.setString(4, book.getIsbn());
            pstmt.setString(5, book.getPublisher());
            pstmt.setInt(6, book.getQuantity());
            pstmt.setInt(7, book.getAvailable());
            pstmt.setInt(8, book.getBookId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating book: " + e.getMessage());
            return false;
        }
    }

    // Delete a book by ID
    public static boolean deleteBook(int bookId) {
        String query = "DELETE FROM books WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, bookId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting book: " + e.getMessage());
            return false;
        }
    }

    // Check if a book with the given ISBN already exists
    public static boolean bookExists(String isbn) {
        String query = "SELECT COUNT(*) FROM books WHERE isbn = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, isbn);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Error checking book existence: " + e.getMessage());
            return false;
        }
    }

    public static boolean updateBookAvailability(int bookId, int change) {
        String query = "UPDATE books SET available = available + ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            conn.setAutoCommit(false);
            pstmt.setInt(1, change);
            pstmt.setInt(2, bookId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating book availability: " + e.getMessage(), e);
            return false;
        }
    }
}