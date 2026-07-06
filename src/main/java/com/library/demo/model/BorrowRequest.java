package com.library.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "borrow_requests")
public class BorrowRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDate requestDate;

    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    private boolean adminRead = false;

    private boolean adminDeleted = false;

    public BorrowRequest() {}

    public BorrowRequest(Book book, User user) {
        this.book = book;
        this.user = user;
        this.requestDate = LocalDate.now();
    }

    // Getters
    public Long getId() { return id; }
    public Book getBook() { return book; }
    public User getUser() { return user; }
    public LocalDate getRequestDate() { return requestDate; }
    public String getStatus() { return status; }
    public boolean isAdminRead() { return adminRead; }
    public boolean isAdminDeleted() { return adminDeleted; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setBook(Book book) { this.book = book; }
    public void setUser(User user) { this.user = user; }
    public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }
    public void setStatus(String status) { this.status = status; }
    public void setAdminRead(boolean adminRead) { this.adminRead = adminRead; }
    public void setAdminDeleted(boolean adminDeleted) { this.adminDeleted = adminDeleted; }
}
