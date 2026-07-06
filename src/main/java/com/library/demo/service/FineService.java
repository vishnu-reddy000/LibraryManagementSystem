package com.library.demo.service;

import com.library.demo.model.Fine;
import com.library.demo.model.IssuedBook;
import com.library.demo.model.IssueStatus;
import com.library.demo.repository.FineRepository;
import com.library.demo.repository.IssuedBookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@SuppressWarnings("null")
public class FineService {

    private static final double FINE_PER_DAY = 2.0;  // ₹2 per day

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private IssuedBookRepository issuedBookRepository;

    /** Calculate and create fines for all overdue issued books */
    public void calculateFines() {
        List<IssuedBook> issued = issuedBookRepository
                .findByStatus(IssueStatus.ISSUED);

        LocalDate today = LocalDate.now();
        for (IssuedBook record : issued) {
            if (record.getDueDate().isBefore(today)) {
                // Update status to OVERDUE
                record.setStatus(IssueStatus.OVERDUE);

                // Calculate fine amount
                long daysOverdue = ChronoUnit.DAYS.between(record.getDueDate(), today);
                double amount = daysOverdue * FINE_PER_DAY;
                record.setFineAmount(amount);
                issuedBookRepository.save(record);

                // Create fine if not already exists
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
        }
    }

    public List<Fine> getAllFines() {
        return fineRepository.findAll();
    }

    public List<Fine> getPendingFines() {
        return fineRepository.findByPaidFalseAndWaivedFalse();
    }

    public void collectFine(Long fineId) {
        fineRepository.findById(fineId).ifPresent(fine -> {
            fine.setPaid(true);
            fineRepository.save(fine);
        });
    }

    public void waiveFine(Long fineId) {
        fineRepository.findById(fineId).ifPresent(fine -> {
            fine.setWaived(true);
            fineRepository.save(fine);
        });
    }

    public double totalFinesCollected() {
        return fineRepository.findByPaidTrue().stream()
                .mapToDouble(Fine::getAmount).sum();
    }

    public long countPendingFines() {
        return fineRepository.findByPaidFalseAndWaivedFalse().size();
    }
}
