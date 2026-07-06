package com.library.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "fines")
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "issued_book_id", nullable = false)
    private IssuedBook issuedBook;

    private double amount;       // calculated fine in ₹

    private boolean paid = false;

    private boolean waived = false;

    private LocalDate createdDate;

    public Fine() {}

    public Fine(IssuedBook issuedBook, double amount) {
        this.issuedBook  = issuedBook;
        this.amount       = amount;
        this.createdDate  = LocalDate.now();
    }

    // Getters
    public Long getId()                  { return id; }
    public IssuedBook getIssuedBook()  { return issuedBook; }
    public double getAmount()            { return amount; }
    public boolean isPaid()              { return paid; }
    public boolean isWaived()            { return waived; }
    public LocalDate getCreatedDate()    { return createdDate; }

    // Setters
    public void setId(Long id)                         { this.id = id; }
    public void setIssuedBook(IssuedBook r)          { this.issuedBook = r; }
    public void setAmount(double amount)               { this.amount = amount; }
    public void setPaid(boolean paid)                  { this.paid = paid; }
    public void setWaived(boolean waived)              { this.waived = waived; }
    public void setCreatedDate(LocalDate createdDate)  { this.createdDate = createdDate; }
}
