package com.library.dao;

import com.library.utils.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaymentDAO {
    private static final Logger logger = Logger.getLogger(PaymentDAO.class.getName());

    /**
     * Records a payment in the database
     * @param userId The user ID making the payment
     * @param amount The payment amount
     * @param method Payment method (Cash, Credit Card, etc.)
     * @param description Description of the payment
     * @param conn Optional connection for transaction handling (can be null)
     * @return true if payment was recorded successfully
     */
    public static boolean recordPayment(int userId, BigDecimal amount, String method,
                                        String description, Connection conn) {
        boolean shouldCloseConnection = false;
        if (conn == null) {
            try {
                conn = DBConnection.getConnection();
                shouldCloseConnection = true;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to get database connection", e);
                return false;
            }
        }

        String sql = "INSERT INTO payments (user_id, amount, payment_date, method, description) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, userId);
            pstmt.setBigDecimal(2, amount);
            pstmt.setString(3, method);
            pstmt.setString(4, description);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating payment failed, no rows affected");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    logger.log(Level.INFO, "Recorded payment ID: " + generatedKeys.getInt(1));
                }
            }
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error recording payment", e);
            return false;
        } finally {
            if (shouldCloseConnection && conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Error closing connection", e);
                }
            }
        }
    }

    /**
     * Gets the total payments made by a user
     * @param userId The user ID to check
     * @return Total payment amount or BigDecimal.ZERO if error occurs
     */
    public static BigDecimal getTotalPaymentsByUser(int userId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting total payments for user: " + userId, e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets payment history for a user
     * @param userId The user ID
     * @return ResultSet containing payment history (caller must close)
     * @throws SQLException if database error occurs
     */
    public static ResultSet getPaymentHistory(int userId) throws SQLException {
        Connection conn = DBConnection.getConnection();
        String sql = "SELECT id, amount, payment_date, method, description " +
                "FROM payments WHERE user_id = ? ORDER BY payment_date DESC";

        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, userId);
        return pstmt.executeQuery();
    }

    /**
     * Records a fine payment and updates the borrowed book record
     * @param borrowId The borrow record ID
     * @param userId The user ID paying the fine
     * @param amount The payment amount
     * @param method Payment method
     * @param description Payment description
     * @param conn Optional existing connection for transactions
     * @return true if both operations succeeded
     */
    public static boolean recordFinePayment(int borrowId, int userId, BigDecimal amount,
                                            String method, String description, Connection conn) {
        boolean shouldCloseConnection = false;
        if (conn == null) {
            try {
                conn = DBConnection.getConnection();
                conn.setAutoCommit(false);
                shouldCloseConnection = true;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to get database connection", e);
                return false;
            }
        }

        try {
            // 1. Record the payment
            boolean paymentRecorded = recordPayment(userId, amount, method, description, conn);

            // 2. Update the borrowed book record
            boolean fineUpdated = BorrowedBookDAO.clearFine(borrowId, conn);

            if (paymentRecorded && fineUpdated) {
                if (shouldCloseConnection) {
                    conn.commit();
                }
                return true;
            } else {
                if (shouldCloseConnection) {
                    conn.rollback();
                }
                return false;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error recording fine payment", e);
            if (shouldCloseConnection && conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.log(Level.WARNING, "Error during rollback", ex);
                }
            }
            return false;
        } finally {
            if (shouldCloseConnection && conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Error closing connection", e);
                }
            }
        }
    }
}