package com.library.model;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.Objects;

import static com.library.dao.BookDAO.getBookById;
import static com.library.dao.UserDAO.getUserById;

public class BorrowedBook {
    private int id;
    private int userId;
    private int bookId;
    private Timestamp borrowDate;
    private Timestamp dueDate;
    private Timestamp returnDate; // Can be null
    private BigDecimal fine; // Default 0.00
    private Status status; // Enum for status

    // Enum for status (ensures type safety)
    public enum Status {
        BORROWED, RETURNED
    }

    // Constructor
    public BorrowedBook(int id, int userId, int bookId, Timestamp borrowDate, Timestamp dueDate, Timestamp returnDate, BigDecimal fine, Status status) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fine = (fine != null) ? fine : BigDecimal.ZERO; // Ensure fine is never null
        this.status = status;

    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public Timestamp getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(Timestamp borrowDate) {
        this.borrowDate = borrowDate;
    }

    public Timestamp getDueDate() {
        return dueDate;
    }

    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    public Timestamp getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(Timestamp returnDate) {
        this.returnDate = returnDate;
    }

    public BigDecimal getFine() {
        return fine;
    }

    public void setFine(BigDecimal fine) {
        this.fine = (fine != null) ? fine : BigDecimal.ZERO;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getUserName() {
        return Objects.requireNonNull(getUserById(userId)).getName();
    }
    public String getBookTitle() {
        return Objects.requireNonNull(getBookById(bookId)).getTitle();
    }
}
