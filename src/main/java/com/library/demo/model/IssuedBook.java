package com.library.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "issued_books")
public class IssuedBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    private LocalDate issueDate;

    private LocalDate dueDate;

    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    private IssueStatus status = IssueStatus.ISSUED;

    private Double fineAmount = 0.0;

    private Double rentalCost = 0.0;   // pre-agreed cost = pricePerDay * borrowDays

    private Integer borrowDays = 0;    // how many days the user chose to borrow

    private boolean rentPaid = false;

    private String issuedBy;

    private String returnedBy;

    public IssuedBook() {}

    // Getters
    public Long getId() { return id; }
    public Book getBook() { return book; }
    public User getMember() { return member; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public IssueStatus getStatus() { return status; }
    public Double getFineAmount() { return fineAmount; }
    public Double getRentalCost() { return rentalCost; }
    public Integer getBorrowDays() { return borrowDays; }
    public String getIssuedBy() { return issuedBy; }
    public String getReturnedBy() { return returnedBy; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setBook(Book book) { this.book = book; }
    public void setMember(User member) { this.member = member; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public void setStatus(IssueStatus status) { this.status = status; }
    public void setFineAmount(Double fineAmount) { this.fineAmount = fineAmount; }
    public void setRentalCost(Double rentalCost) { this.rentalCost = rentalCost; }
    public void setBorrowDays(Integer borrowDays) { this.borrowDays = borrowDays; }
    public boolean isRentPaid() { return rentPaid; }
    public void setRentPaid(boolean rentPaid) { this.rentPaid = rentPaid; }
    public void setIssuedBy(String issuedBy) { this.issuedBy = issuedBy; }
    public void setReturnedBy(String returnedBy) { this.returnedBy = returnedBy; }
}
