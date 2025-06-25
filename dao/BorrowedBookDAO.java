package com.library.dao;

import com.library.model.BorrowedBook;
import com.library.utils.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.library.gui.admin.OverdueBooksPanel.FINE_PER_DAY;

public class BorrowedBookDAO {
    private static final int MAX_BORROW_LIMIT = 5;
    private static final BigDecimal DAILY_FINE_RATE = BigDecimal.valueOf(5);
    private static final Logger logger = Logger.getLogger(BorrowedBookDAO.class.getName());

    public static boolean borrowBook(int userId, int bookId, Timestamp borrowDate, Timestamp dueDate) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Validate user and book
            if (!doesUserExist(userId, conn)) {
                throw new SQLException("User does not exist");
            }
            if (!isBookAvailable(bookId, conn)) {
                throw new SQLException("Book is not available");
            }
            if (getUserBorrowCount(userId, conn) >= MAX_BORROW_LIMIT) {
                throw new SQLException("User has reached borrow limit of " + MAX_BORROW_LIMIT);
            }

            // Create borrow record
            String borrowQuery = "INSERT INTO borrowed_books (user_id, book_id, borrow_date, due_date, status) " +
                    "VALUES (?, ?, ?, ?, 'borrowed')";
            try (PreparedStatement pstmt = conn.prepareStatement(borrowQuery, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, bookId);
                pstmt.setTimestamp(3, borrowDate);
                pstmt.setTimestamp(4, dueDate);

                if (pstmt.executeUpdate() == 0) {
                    throw new SQLException("Failed to create borrow record");
                }
            }

            // Update book availability
            String updateQuery = "UPDATE books SET available = available - 1 WHERE id = ? AND available > 0";
            try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                pstmt.setInt(1, bookId);
                if (pstmt.executeUpdate() == 0) {
                    throw new SQLException("Failed to update book availability");
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            DBConnection.rollbackTransaction(conn);
            logger.log(Level.SEVERE, "Error borrowing book", e);
            return false;
        } finally {
            DBConnection.closeConnection(conn);
        }
    }

    public static boolean returnBook(int borrowId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);  // Start transaction

            // 1. Get current date as return date
            LocalDate currentDate = LocalDate.now();
            Date returnDate = Date.valueOf(currentDate);

            // 2. Get book details and validate borrow record
            int bookId = getBookIdFromBorrowId(borrowId, conn);
            if (bookId == -1) {
                throw new SQLException("Invalid borrow record ID: " + borrowId);
            }

            // 3. Calculate fine based on current date
            BigDecimal fine = calculateFine(borrowId, returnDate, conn);

            // 4. Update borrow record with return date and fine
            String updateBorrowSql = "UPDATE borrowed_books SET return_date = ?, fine = ?, status = 'returned' WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateBorrowSql)) {
                pstmt.setDate(1, returnDate);
                pstmt.setBigDecimal(2, fine);
                pstmt.setInt(3, borrowId);

                if (pstmt.executeUpdate() != 1) {
                    throw new SQLException("Failed to update borrow record");
                }
            }

            // 5. Update book availability
            String updateBookSql = "UPDATE books SET available = available + 1 WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateBookSql)) {
                pstmt.setInt(1, bookId);
                if (pstmt.executeUpdate() != 1) {
                    throw new SQLException("Failed to update book availability");
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "Error rolling back transaction", ex);
                }
            }
            logger.log(Level.SEVERE, "Error returning book with ID: " + borrowId, e);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);  // Reset auto-commit
                    conn.close();
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Error closing connection", e);
                }
            }
        }
    }

    public static List<BorrowedBook> getFinesWithDetails(String filter) throws SQLException {
        List<BorrowedBook> fines = new ArrayList<>();
        String sql = "SELECT bb.id, bb.user_id, bb.book_id, u.name AS user_name, b.title AS book_title, " +
                "bb.borrow_date, bb.due_date, bb.return_date, bb.fine, bb.status " +
                "FROM borrowed_books bb " +
                "JOIN users u ON bb.user_id = u.id " +
                "JOIN books b ON bb.book_id = b.id " +
                "WHERE (bb.fine > 0 OR bb.fine = 0 OR bb.fine IS NULL)";

        if ("Unpaid Fines".equals(filter)) {
            sql += " AND bb.fine > 0";
        } else if ("Paid Fines".equals(filter)) {
            sql += " AND (bb.fine = 0 OR bb.fine IS NULL)";
        }

        sql += " ORDER BY bb.due_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                fines.add(new BorrowedBook(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("book_id"),
                        rs.getTimestamp("borrow_date"),
                        rs.getTimestamp("due_date"),
                        rs.getTimestamp("return_date"),
                        rs.getBigDecimal("fine"),
                        BorrowedBook.Status.valueOf(rs.getString("status").toUpperCase())
                ));
            }
        }
        return fines;
    }

    public static boolean clearFine(int borrowId, Connection conn) throws SQLException {
        String query = "UPDATE borrowed_books SET fine = 0 WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, borrowId);
            return pstmt.executeUpdate() > 0;
        }
    }

    // Helper methods with connection parameter for transaction support
    private static boolean doesUserExist(int userId, Connection conn) throws SQLException {
        String query = "SELECT 1 FROM users WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean isBookAvailable(int bookId, Connection conn) throws SQLException {
        String query = "SELECT available FROM books WHERE id = ? AND available > 0";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int getUserBorrowCount(int userId, Connection conn) throws SQLException {
        String query = "SELECT COUNT(*) FROM borrowed_books WHERE user_id = ? AND status = 'borrowed'";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int getBookIdFromBorrowId(int borrowId, Connection conn) throws SQLException {
        String sql = "SELECT book_id FROM borrowed_books WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, borrowId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt("book_id") : -1;
            }
        }
    }

    private static BigDecimal calculateFine(int borrowId, Date returnDate, Connection conn) throws SQLException {
        String sql = "SELECT due_date FROM borrowed_books WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, borrowId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Date dueDate = rs.getDate("due_date");
                    long daysLate = ChronoUnit.DAYS.between(dueDate.toLocalDate(), returnDate.toLocalDate());
                    return daysLate > 0 ? BigDecimal.valueOf(daysLate * FINE_PER_DAY) : BigDecimal.ZERO;
                }
                throw new SQLException("Borrow record not found");
            }
        }
    }

    public static List<BorrowedBook> getUserBorrowedBooks(int userId) throws SQLException {
        List<BorrowedBook> books = new ArrayList<>();
        String query = "SELECT bb.id, bb.user_id, bb.book_id, b.title, " +
                "bb.borrow_date, bb.due_date, bb.return_date, bb.fine, bb.status " +
                "FROM borrowed_books bb JOIN books b ON bb.book_id = b.id " +
                "WHERE bb.user_id = ? AND bb.status = 'borrowed'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    books.add(new BorrowedBook(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getInt("book_id"),
                            rs.getTimestamp("borrow_date"),
                            rs.getTimestamp("due_date"),
                            rs.getTimestamp("return_date"),
                            rs.getBigDecimal("fine"),
                            BorrowedBook.Status.valueOf(rs.getString("status").toUpperCase())
                    ));
                }
            }
        }
        return books;
    }

    public static boolean hasUserBorrowedBook(int userId, int bookId) throws SQLException {
        String query = "SELECT 1 FROM borrowed_books " +
                "WHERE user_id = ? AND book_id = ? AND status = 'borrowed'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static List<BorrowedBook> getAllTransactions(String filter) throws SQLException {
        List<BorrowedBook> transactions = new ArrayList<>();
        String sql = "SELECT bb.id, bb.user_id, bb.book_id, u.name AS user_name, " +
                "b.title AS book_title, bb.borrow_date, bb.due_date, " +
                "bb.return_date, bb.fine, bb.status " +
                "FROM borrowed_books bb " +
                "JOIN users u ON bb.user_id = u.id " +
                "JOIN books b ON bb.book_id = b.id ";

        if ("Borrowed".equals(filter)) {
            sql += "WHERE bb.status = 'borrowed' ";
        } else if ("Returned".equals(filter)) {
            sql += "WHERE bb.status = 'returned' ";
        }

        sql += "ORDER BY bb.borrow_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                transactions.add(new BorrowedBook(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("book_id"),
                        rs.getTimestamp("borrow_date"),
                        rs.getTimestamp("due_date"),
                        rs.getTimestamp("return_date"),
                        rs.getBigDecimal("fine"),
                        BorrowedBook.Status.valueOf(rs.getString("status").toUpperCase())
                ));
            }
        }
        return transactions;
    }

    public static List<BorrowedBook> getOverdueBooks() throws SQLException {
        List<BorrowedBook> books = new ArrayList<>();
        String query = "SELECT bb.id, bb.user_id, bb.book_id, u.name AS user_name, " +
                "b.title AS book_title, bb.borrow_date, bb.due_date, " +
                "bb.return_date, bb.fine, bb.status " +
                "FROM borrowed_books bb " +
                "JOIN users u ON bb.user_id = u.id " +
                "JOIN books b ON bb.book_id = b.id " +
                "WHERE bb.status = 'borrowed' AND bb.due_date < CURRENT_DATE " +
                "ORDER BY bb.due_date";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                books.add(new BorrowedBook(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("book_id"),
                        rs.getTimestamp("borrow_date"),
                        rs.getTimestamp("due_date"),
                        rs.getTimestamp("return_date"),
                        rs.getBigDecimal("fine"),
                        BorrowedBook.Status.valueOf(rs.getString("status").toUpperCase()) // Ensure parentheses close properly here
                ));
            }
        }
        return books;
    }
}