package com.library.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "issued_book_id")
    private IssuedBook issuedBook;

    private Double amount = 0.0;

    private String type; // "RENT", "FINE"

    private String paymentMethod; // "UPI", "CARD", "NET_BANKING", "CASH"

    private LocalDateTime paymentDate = LocalDateTime.now();

    public Payment() {}

    public Payment(User user, IssuedBook issuedBook, Double amount, String type, String paymentMethod) {
        this.user = user;
        this.issuedBook = issuedBook;
        this.amount = amount;
        this.type = type;
        this.paymentMethod = paymentMethod;
        this.paymentDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public IssuedBook getIssuedBook() { return issuedBook; }
    public void setIssuedBook(IssuedBook issuedBook) { this.issuedBook = issuedBook; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
}
