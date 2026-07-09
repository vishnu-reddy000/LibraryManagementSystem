package com.library.demo.service;

import com.library.demo.model.Book;
import com.library.demo.model.IssuedBook;
import com.library.demo.model.IssueStatus;
import com.library.demo.model.User;
import com.library.demo.model.Fine;
import com.library.demo.repository.IssuedBookRepository;
import com.library.demo.repository.UserRepository;
import com.library.demo.repository.FineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Service
@SuppressWarnings("null")
public class IssueService {

    @Autowired
    private IssuedBookRepository issuedBookRepository;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FineRepository fineRepository;

    public List<IssuedBook> getAllIssues() {
        return issuedBookRepository.findAll();
    }

    public List<IssuedBook> getIssuedBooks() {
        return issuedBookRepository.findByStatus(IssueStatus.ISSUED);
    }

    /** All issue records for a given user email */
    public List<IssuedBook> getIssuesByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(issuedBookRepository::findByMember)
                .orElse(Collections.emptyList());
    }

    /** Currently issued (not returned) records for a user email */
    public List<IssuedBook> getActiveIssuesByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(u -> issuedBookRepository.findByMemberAndStatus(u, IssueStatus.ISSUED))
                .orElse(Collections.emptyList());
    }

    public IssuedBook issueBook(Long bookId, Long userId, LocalDate dueDate, String adminEmail) {
        return issueBook(bookId, userId, dueDate, adminEmail, null);
    }

    public IssuedBook issueBook(Long bookId, Long userId, LocalDate dueDate, String adminEmail, Integer borrowDays) {
        Book book = bookService.getBookById(bookId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Check if the user already has an active issue of this book
        boolean alreadyIssued = issuedBookRepository.findByMemberAndStatus(user, IssueStatus.ISSUED).stream()
                .anyMatch(issue -> issue.getBook().getId().equals(bookId));
        boolean alreadyOverdue = issuedBookRepository.findByMemberAndStatus(user, IssueStatus.OVERDUE).stream()
                .anyMatch(issue -> issue.getBook().getId().equals(bookId));
        if (alreadyIssued || alreadyOverdue) {
            throw new RuntimeException("You/User has already borrowed '" + book.getTitle() + "' and has not returned it yet.");
        }

        // Check if the user has any unpaid fines
        boolean hasUnpaidFines = fineRepository.findByIssuedBookMemberEmail(user.getEmail()).stream()
                .anyMatch(f -> !f.isPaid() && !f.isWaived());
        if (hasUnpaidFines) {
            throw new RuntimeException("Borrowing blocked: You/User has outstanding unpaid fines. Please clear all fines first.");
        }

        if (book.getAvailableCopies() <= 0) {
            throw new RuntimeException("No copies available for: " + book.getTitle());
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookService.saveBook(book);

        // Calculate rental cost
        int days = 14;
        if (borrowDays != null && borrowDays > 0) {
            days = borrowDays;
        } else if (dueDate != null) {
            days = (int) java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), dueDate);
            if (days <= 0) days = 14;
        }
        double pricePerDay = (book.getPricePerDay() != null) ? book.getPricePerDay() : 0.0;
        double rentalCost = pricePerDay * days;

        IssuedBook record = new IssuedBook();
        record.setBook(book);
        record.setMember(user);
        record.setIssueDate(LocalDate.now());
        record.setDueDate(dueDate != null ? dueDate : LocalDate.now().plusDays(days));
        record.setStatus(IssueStatus.ISSUED);
        record.setIssuedBy(adminEmail);
        record.setFineAmount(0.0);
        record.setBorrowDays(days);
        record.setRentalCost(rentalCost);

        return issuedBookRepository.save(record);
    }

    public IssuedBook returnBook(Long issueId, String adminEmail) {
        IssuedBook record = issuedBookRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue record not found"));

        LocalDate today = LocalDate.now();
        record.setReturnDate(today);
        record.setStatus(IssueStatus.RETURNED);
        record.setReturnedBy(adminEmail);

        // Check if overdue to lock in fineAmount
        if (record.getDueDate().isBefore(today)) {
            long daysOverdue = ChronoUnit.DAYS.between(record.getDueDate(), today);
            double amount = daysOverdue * 2.0; // ₹2 per day
            record.setFineAmount(amount);

            if (fineRepository.findByIssuedBook(record).isEmpty()) {
                fineRepository.save(new Fine(record, amount));
            } else {
                fineRepository.findByIssuedBook(record).ifPresent(f -> {
                    if (!f.isPaid() && !f.isWaived()) {
                        f.setAmount(amount);
                        fineRepository.save(f);
                    }
                });
            }
        }

        Book book = record.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookService.saveBook(book);

        return issuedBookRepository.save(record);
    }

    public long countIssuedBooks() {
        return issuedBookRepository.countByStatus(IssueStatus.ISSUED);
    }

    public long countOverdueBooks() {
        return issuedBookRepository.countByStatus(IssueStatus.OVERDUE);
    }
}
