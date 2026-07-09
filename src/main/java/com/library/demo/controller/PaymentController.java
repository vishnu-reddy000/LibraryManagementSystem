package com.library.demo.controller;

import com.library.demo.model.Fine;
import com.library.demo.model.IssuedBook;
import com.library.demo.model.Payment;
import com.library.demo.repository.FineRepository;
import com.library.demo.repository.IssuedBookRepository;
import com.library.demo.repository.PaymentRepository;
import com.library.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@SuppressWarnings("null")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private IssuedBookRepository issuedBookRepository;

    @PostMapping("/user/fines/pay/{fineId}")
    public String payFine(@PathVariable Long fineId, @RequestParam String paymentMethod, Authentication authentication, RedirectAttributes ra) {
        String email = authentication.getName();
        com.library.demo.model.User user = userRepository.findByEmail(email).orElse(null);
        Fine fine = fineRepository.findById(fineId).orElse(null);
        
        if (user != null && fine != null) {
            if (fine.isPaid()) {
                ra.addFlashAttribute("error", "This fine has already been paid.");
                return "redirect:/user/fines";
            }
            fine.setPaid(true);
            fineRepository.save(fine);

            // Create and save payment transaction record
            Payment payment = new Payment(user, fine.getIssuedBook(), fine.getAmount(), "FINE", paymentMethod);
            paymentRepository.save(payment);

            ra.addFlashAttribute("success", "Fine of ₹" + fine.getAmount() + " paid successfully using " + paymentMethod + "!");
        } else {
            ra.addFlashAttribute("error", "Fine record or user not found.");
        }
        return "redirect:/user/fines";
    }

    @PostMapping("/user/rent/pay/{issueId}")
    public String payRent(@PathVariable Long issueId, @RequestParam String paymentMethod, Authentication authentication, RedirectAttributes ra) {
        String email = authentication.getName();
        com.library.demo.model.User user = userRepository.findByEmail(email).orElse(null);
        IssuedBook record = issuedBookRepository.findById(issueId).orElse(null);
        
        if (user != null && record != null) {
            if (record.isRentPaid()) {
                ra.addFlashAttribute("error", "Rent has already been paid for this book.");
                return "redirect:/user/my-books";
            }
            record.setRentPaid(true);
            issuedBookRepository.save(record);

            // Create and save payment transaction record
            Payment payment = new Payment(user, record, record.getRentalCost(), "RENT", paymentMethod);
            paymentRepository.save(payment);

            ra.addFlashAttribute("success", "Rent of ₹" + record.getRentalCost() + " paid successfully using " + paymentMethod + "!");
        } else {
            ra.addFlashAttribute("error", "Borrow record or user not found.");
        }
        return "redirect:/user/my-books";
    }

    @GetMapping("/payments")
    public String viewPayments(Model model) {
        List<Payment> allPayments = paymentRepository.findAllByOrderByPaymentDateDesc();
        
        double totalCollected = allPayments.stream().mapToDouble(Payment::getAmount).sum();
        double rentCollected = allPayments.stream().filter(p -> "RENT".equalsIgnoreCase(p.getType())).mapToDouble(Payment::getAmount).sum();
        double finesCollected = allPayments.stream().filter(p -> "FINE".equalsIgnoreCase(p.getType())).mapToDouble(Payment::getAmount).sum();

        model.addAttribute("payments", allPayments);
        model.addAttribute("totalCollected", totalCollected);
        model.addAttribute("rentCollected", rentCollected);
        model.addAttribute("finesCollected", finesCollected);
        
        return "payments";
    }
}
