package com.library.demo.controller;

import com.library.demo.model.BorrowRequest;
import com.library.demo.model.User;
import com.library.demo.repository.BorrowRequestRepository;
import com.library.demo.repository.UserRepository;
import com.library.demo.service.BookService;
import com.library.demo.service.IssueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.library.demo.model.IssuedBook;

import java.util.List;

@Controller
public class IssueController {

    @Autowired
    private IssueService issueService;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowRequestRepository borrowRequestRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/issue-books")
    public String issueBookForm(Model model) {
        model.addAttribute("books", bookService.getAllBooks());
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.USER)
                .toList();
        model.addAttribute("users", users);
        model.addAttribute("issuedBooks", issueService.getIssuedBooks());
        model.addAttribute("borrowRequests", borrowRequestRepository.findByStatus("PENDING"));
        return "issue-books";
    }

    @PostMapping("/issue-books")
    @SuppressWarnings("null")
    public String issueBook(@RequestParam Long bookId, @RequestParam Long userId,
                            @RequestParam(required = false) String dueDate,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            String adminEmail = authentication.getName();
            java.time.LocalDate due = null;
            if (dueDate != null && !dueDate.isBlank()) {
                due = java.time.LocalDate.parse(dueDate);
            } else {
                due = java.time.LocalDate.now().plusDays(14);
            }
            IssuedBook record = issueService.issueBook(bookId, userId, due, adminEmail);
            redirectAttributes.addFlashAttribute("success", "Book issued successfully!");
            // Send WebSocket notification to user
            messagingTemplate.convertAndSend("/topic/notifications-" + record.getMember().getEmail(), 
                java.util.Map.of("message", "📖 Book '" + record.getBook().getTitle() + "' was issued to you by the librarian.", "type", "info"));
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/issue-books";
    }

    @PostMapping("/issue-books/approve/{id}")
    @SuppressWarnings("null")
    public String approveRequest(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        BorrowRequest request = borrowRequestRepository.findById(id).orElse(null);
        if (request == null) {
            redirectAttributes.addFlashAttribute("error", "Borrow request not found.");
            return "redirect:/issue-books";
        }
        try {
            String adminEmail = authentication.getName();
            issueService.issueBook(request.getBook().getId(), request.getUser().getId(), java.time.LocalDate.now().plusDays(14), adminEmail);
            request.setStatus("APPROVED");
            borrowRequestRepository.save(request);
            redirectAttributes.addFlashAttribute("success", "Request approved and book issued successfully!");
            // Send WebSocket notification to user
            messagingTemplate.convertAndSend("/topic/notifications-" + request.getUser().getEmail(), 
                java.util.Map.of("message", "✅ APPROVED: Your request for '" + request.getBook().getTitle() + "' has been approved!", "type", "success"));
            // Also notify admin socket
            messagingTemplate.convertAndSend("/topic/admin", java.util.Map.of("message", "Approved request for " + request.getUser().getEmail(), "type", "sync"));
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Cannot approve: " + e.getMessage());
        }
        return "redirect:/issue-books";
    }

    @PostMapping("/issue-books/reject/{id}")
    @SuppressWarnings("null")
    public String rejectRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        BorrowRequest request = borrowRequestRepository.findById(id).orElse(null);
        if (request != null) {
            request.setStatus("REJECTED");
            borrowRequestRepository.save(request);
            redirectAttributes.addFlashAttribute("success", "Borrow request rejected.");
            // Send WebSocket notification to user
            messagingTemplate.convertAndSend("/topic/notifications-" + request.getUser().getEmail(), 
                java.util.Map.of("message", "❌ REJECTED: Your request for '" + request.getBook().getTitle() + "' was rejected.", "type", "error"));
            // Also notify admin socket
            messagingTemplate.convertAndSend("/topic/admin", java.util.Map.of("message", "Rejected request for " + request.getUser().getEmail(), "type", "sync"));
        }
        return "redirect:/issue-books";
    }

    @GetMapping("/return-books")
    public String returnBookPage(Model model) {
        model.addAttribute("issuedBooks", issueService.getIssuedBooks());
        return "return-books";
    }

    @PostMapping("/return-books")
    public String returnBook(@RequestParam Long issueId, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String adminEmail = authentication.getName();
            issueService.returnBook(issueId, adminEmail);
            redirectAttributes.addFlashAttribute("success", "Book returned successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/return-books";
    }
}
