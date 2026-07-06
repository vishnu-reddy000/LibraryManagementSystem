package com.library.demo.controller;

import com.library.demo.model.Book;
import com.library.demo.model.BorrowRequest;
import com.library.demo.model.IssuedBook;
import com.library.demo.model.IssueStatus;
import com.library.demo.repository.BorrowRequestRepository;
import com.library.demo.repository.UserRepository;
import com.library.demo.service.BookService;
import com.library.demo.service.IssueService;
import com.library.demo.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.library.demo.model.Fine;
import com.library.demo.repository.FineRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.library.demo.service.FineService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@SuppressWarnings("null")
public class UserController {

    @Autowired
    private BookService bookService;

    @Autowired
    private IssueService issueService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowRequestRepository borrowRequestRepository;

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FineService fineService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/user")
    public String userDashboard(Authentication authentication, Model model) {
        String email = authentication.getName();
        com.library.demo.model.User user = userRepository.findByEmail(email).orElse(null);

        model.addAttribute("totalBooks", bookService.countBooks());
        model.addAttribute("availableBooks",
                bookService.getAllBooks().stream()
                        .filter(b -> b.getAvailableCopies() > 0).count());

        model.addAttribute("myActiveIssues", issueService.getActiveIssuesByEmail(email));
        
        if (user != null) {
            // Book Requests
            model.addAttribute("myRequests", borrowRequestRepository.findByUser(user));
            // Fines
            List<Fine> userFines = fineRepository.findByIssuedBookMemberEmail(email);
            double totalUnpaid = userFines.stream()
                    .filter(f -> !f.isPaid() && !f.isWaived())
                    .mapToDouble(Fine::getAmount)
                    .sum();
            model.addAttribute("myFines", userFines);
            model.addAttribute("totalUnpaidFine", totalUnpaid);
            
            // Notifications / Alerts
            model.addAttribute("dashboardNotices", getDashboardNotices(email));
        }

        populateCommonModelAttributes(email, model);
        return "user";
    }

    @GetMapping("/user/my-books")
    public String myBorrowedBooks(Authentication authentication,
                                  @RequestParam(required = false) String filter,
                                  Model model) {
        String email = authentication.getName();

        List<IssuedBook> allIssues = issueService.getIssuesByEmail(email);

        // Count each status
        long activeCount = allIssues.stream()
                .filter(i -> i.getStatus() == IssueStatus.ISSUED).count();
        long returnedCount = allIssues.stream()
                .filter(i -> i.getStatus() == IssueStatus.RETURNED).count();
        long overdueCount = allIssues.stream()
                .filter(i -> i.getStatus() == IssueStatus.OVERDUE).count();

        // Filter display list
        List<IssuedBook> displayIssues;
        if ("issued".equalsIgnoreCase(filter)) {
            displayIssues = allIssues.stream()
                    .filter(i -> i.getStatus() == IssueStatus.ISSUED)
                    .collect(Collectors.toList());
        } else if ("returned".equalsIgnoreCase(filter)) {
            displayIssues = allIssues.stream()
                    .filter(i -> i.getStatus() == IssueStatus.RETURNED)
                    .collect(Collectors.toList());
        } else if ("overdue".equalsIgnoreCase(filter)) {
            displayIssues = allIssues.stream()
                    .filter(i -> i.getStatus() == IssueStatus.OVERDUE)
                    .collect(Collectors.toList());
        } else {
            displayIssues = allIssues;
        }

        java.util.Map<String, String> colors = new java.util.HashMap<>();
        categoryRepository.findAll().forEach(c -> {
            if (c.getName() != null && c.getColor() != null) {
                colors.put(c.getName().trim().toLowerCase(), c.getColor());
            }
        });
        model.addAttribute("categoryColors", colors);

        model.addAttribute("allIssues", allIssues);
        model.addAttribute("displayIssues", displayIssues);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("returnedCount", returnedCount);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("filter", filter);
        populateCommonModelAttributes(email, model);

        return "my-books";
    }

    @GetMapping("/user/books")
    public String browseBooks(Authentication authentication, @RequestParam(required = false) String search, Model model) {
        String email = authentication.getName();
        List<Book> books;
        if (search != null && !search.isBlank()) {
            books = bookService.searchBooks(search);
            model.addAttribute("search", search);
        } else {
            books = bookService.getAllBooks();
        }
        model.addAttribute("books", books);
        
        java.util.Map<String, String> colors = new java.util.HashMap<>();
        categoryRepository.findAll().forEach(c -> {
            if (c.getName() != null && c.getColor() != null) {
                colors.put(c.getName().trim().toLowerCase(), c.getColor());
            }
        });
        model.addAttribute("categoryColors", colors);

        // Map currently issued books for self-service returns
        List<IssuedBook> activeIssues = issueService.getActiveIssuesByEmail(email);
        java.util.Map<Long, Long> bookToIssueMap = new java.util.HashMap<>();
        for (IssuedBook issue : activeIssues) {
            bookToIssueMap.put(issue.getBook().getId(), issue.getId());
        }
        model.addAttribute("bookToIssueMap", bookToIssueMap);

        // Map book id -> pricePerDay for JS price calculation
        java.util.Map<Long, Double> bookPriceMap = new java.util.HashMap<>();
        for (Book book : books) {
            bookPriceMap.put(book.getId(), book.getPricePerDay() != null ? book.getPricePerDay() : 0.0);
        }
        model.addAttribute("bookPriceMap", bookPriceMap);
        
        populateCommonModelAttributes(email, model);
        return "user-books";
    }

    @PostMapping("/user/books/return/{issueId}")
    public String userReturnBookDirectly(@PathVariable Long issueId,
                                         @RequestHeader(value = "Referer", required = false) String referer,
                                         Authentication authentication,
                                         RedirectAttributes ra) {
        String email = authentication.getName();
        try {
            IssuedBook record = issueService.returnBook(issueId, "Self-Service");
            ra.addFlashAttribute("success", "Book returned successfully!");
            // Broadcast WebSocket alert to Admin
            messagingTemplate.convertAndSend("/topic/admin", 
                java.util.Map.of("message", "↩️ Book '" + record.getBook().getTitle() + "' was returned by user " + email, "type", "return"));
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        if (referer != null && referer.contains("/user/my-books")) {
            return "redirect:/user/my-books";
        }
        return "redirect:/user/books";
    }

    @PostMapping("/user/books/borrow/{id}")
    public String borrowBookDirectly(@PathVariable Long id, 
                                     @RequestParam(required = false) String dueDate,
                                     @RequestParam(required = false) Integer borrowDays,
                                     Authentication authentication, 
                                     RedirectAttributes ra) {
        String email = authentication.getName();
        com.library.demo.model.User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "User session not found.");
            return "redirect:/user/books";
        }
        try {
            int days = (borrowDays != null && borrowDays > 0) ? borrowDays : 14;
            java.time.LocalDate due = null;
            if (dueDate != null && !dueDate.isBlank()) {
                due = java.time.LocalDate.parse(dueDate);
            } else {
                due = java.time.LocalDate.now().plusDays(days);
            }
            IssuedBook record = issueService.issueBook(id, user.getId(), due, "Self-Service", days);
            ra.addFlashAttribute("success", "Book borrowed successfully!");
            // Broadcast WebSocket alert to Admin
            messagingTemplate.convertAndSend("/topic/admin", 
                java.util.Map.of("message", "📖 Book '" + record.getBook().getTitle() + "' borrowed directly by user " + email, "type", "borrow"));
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/user/books";
    }

    @PostMapping("/user/books/request/{id}")
    public String requestBook(@PathVariable Long id, Authentication authentication, RedirectAttributes ra) {
        String email = authentication.getName();
        com.library.demo.model.User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "User session not found.");
            return "redirect:/user/books";
        }
        Book book = bookService.getBookById(id);
        
        boolean exists = borrowRequestRepository.findByUser(user).stream()
                .anyMatch(r -> r.getBook().getId().equals(id) && "PENDING".equalsIgnoreCase(r.getStatus()));
        if (exists) {
            ra.addFlashAttribute("error", "You already have a pending request for this book.");
            return "redirect:/user/books";
        }

        BorrowRequest request = new BorrowRequest(book, user);
        borrowRequestRepository.save(request);
        ra.addFlashAttribute("success", "Request submitted. Administrator will notify you when it's available!");
        // Broadcast WebSocket alert to Admin
        messagingTemplate.convertAndSend("/topic/admin", 
            java.util.Map.of("message", "🔔 New borrow request for '" + book.getTitle() + "' by user " + email, "type", "request"));
        return "redirect:/user/books";
    }

    private void populateCommonModelAttributes(String email, Model model) {
        model.addAttribute("userName", email);
        model.addAttribute("notifCount", getNotificationCount(email));
        userRepository.findByEmail(email).ifPresent(user -> model.addAttribute("member", user));
    }

    private long getNotificationCount(String email) {
        long count = 0;
        com.library.demo.model.User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            // 1. Overdue books
            List<IssuedBook> activeIssues = issueService.getActiveIssuesByEmail(email);
            for (IssuedBook issue : activeIssues) {
                if (issue.getDueDate().isBefore(java.time.LocalDate.now())) {
                    count++;
                }
            }
            // 2. Unpaid fines
            List<Fine> userFines = fineRepository.findByIssuedBookMemberEmail(email);
            for (Fine fine : userFines) {
                if (!fine.isPaid() && !fine.isWaived()) {
                    count++;
                }
            }
            // 3. Approved/Rejected borrow requests
            List<BorrowRequest> requests = borrowRequestRepository.findByUser(user);
            for (BorrowRequest req : requests) {
                if ("APPROVED".equalsIgnoreCase(req.getStatus()) || "REJECTED".equalsIgnoreCase(req.getStatus())) {
                    count++;
                }
            }
        }
        return count;
    }

    @GetMapping("/user/requests")
    public String userRequests(Authentication authentication, Model model) {
        String email = authentication.getName();
        com.library.demo.model.User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            List<BorrowRequest> requests = borrowRequestRepository.findByUser(user);
            model.addAttribute("requests", requests);
        }
        populateCommonModelAttributes(email, model);
        return "user-requests";
    }

    @GetMapping("/user/fines")
    public String userFines(Authentication authentication, Model model) {
        String email = authentication.getName();
        fineService.calculateFines(); // Make sure fines are up-to-date

        List<Fine> userFines = fineRepository.findByIssuedBookMemberEmail(email);
        double totalUnpaid = userFines.stream()
                .filter(f -> !f.isPaid() && !f.isWaived())
                .mapToDouble(Fine::getAmount)
                .sum();
        double totalPaid = userFines.stream()
                .filter(Fine::isPaid)
                .mapToDouble(Fine::getAmount)
                .sum();

        model.addAttribute("fines", userFines);
        model.addAttribute("totalUnpaid", totalUnpaid);
        model.addAttribute("totalPaid", totalPaid);
        populateCommonModelAttributes(email, model);
        return "user-fines";
    }

    @GetMapping("/user/notifications")
    public String userNotifications(Authentication authentication, Model model) {
        String email = authentication.getName();
        List<NotificationItem> notices = getDashboardNotices(email);
        
        // Add pending borrow requests as info notices (but not in dashboard to keep it cleaner)
        com.library.demo.model.User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            List<BorrowRequest> requests = borrowRequestRepository.findByUser(user);
            for (BorrowRequest req : requests) {
                if ("PENDING".equalsIgnoreCase(req.getStatus())) {
                    notices.add(new NotificationItem(
                        req.getId(),
                        "ℹ️ PENDING: Your borrow request for '" + req.getBook().getTitle() + "' is pending approval.",
                        "info",
                        false,
                        null
                    ));
                }
            }
        }

        model.addAttribute("notifications", notices);
        populateCommonModelAttributes(email, model);
        return "user-notifications";
    }

    @PostMapping("/user/notifications/dismiss/{id}")
    public String dismissNotification(@PathVariable Long id, Authentication authentication, RedirectAttributes ra) {
        String email = authentication.getName();
        com.library.demo.model.User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            borrowRequestRepository.findById(id).ifPresent(req -> {
                if (req.getUser().getId().equals(user.getId())) {
                    borrowRequestRepository.delete(req);
                }
            });
        }
        return "redirect:/user/notifications";
    }

    @GetMapping("/user/settings")
    public String userSettingsPage(Authentication authentication, Model model) {
        String email = authentication.getName();
        userRepository.findByEmail(email).ifPresent(u -> model.addAttribute("user", u));
        populateCommonModelAttributes(email, model);
        return "user-settings";
    }

    @PostMapping("/user/settings/change-password")
    public String changeUserPassword(Authentication authentication,
                                     @RequestParam String newPassword,
                                     @RequestParam String confirmPassword,
                                     RedirectAttributes ra) {
        String email = authentication.getName();
        com.library.demo.model.User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            ra.addFlashAttribute("error", "User not found.");
            return "redirect:/user/settings";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New passwords do not match.");
            return "redirect:/user/settings";
        }
        if (newPassword.length() < 6) {
            ra.addFlashAttribute("error", "Password must be at least 6 characters.");
            return "redirect:/user/settings";
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        ra.addFlashAttribute("success", "Password changed successfully!");
        return "redirect:/user/settings";
    }

    @PostMapping("/user/settings/update-profile")
    public String updateUserProfile(Authentication authentication,
                                    @RequestParam String name,
                                    RedirectAttributes ra) {
        String email = authentication.getName();
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setName(name);
            userRepository.save(user);
        });
        ra.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/user/settings";
    }

    @GetMapping("/user/current-email")
    @ResponseBody
    public java.util.Map<String, String> getCurrentUserEmail(Authentication authentication) {
        if (authentication != null) {
            return java.util.Map.of("email", authentication.getName());
        }
        return java.util.Map.of("email", "");
    }

    private List<NotificationItem> getDashboardNotices(String email) {
        com.library.demo.model.User user = userRepository.findByEmail(email).orElse(null);
        List<NotificationItem> notices = new java.util.ArrayList<>();
        if (user != null) {
            // 1. Check for overdue books
            List<IssuedBook> activeIssues = issueService.getActiveIssuesByEmail(email);
            for (IssuedBook issue : activeIssues) {
                if (issue.getDueDate().isBefore(java.time.LocalDate.now())) {
                    notices.add(new NotificationItem(
                        issue.getId(),
                        "⚠️ OVERDUE WARNING: The book '" + issue.getBook().getTitle() + 
                        "' was due on " + issue.getDueDate() + ". Please return it immediately.",
                        "error",
                        false,
                        null
                    ));
                }
            }
            // 2. Borrow requests
            List<BorrowRequest> requests = borrowRequestRepository.findByUser(user);
            for (BorrowRequest req : requests) {
                if ("APPROVED".equalsIgnoreCase(req.getStatus())) {
                    notices.add(new NotificationItem(
                        req.getId(),
                        "✅ APPROVED: Your request for '" + req.getBook().getTitle() + "' has been approved!",
                        "success",
                        true,
                        "/user/notifications/dismiss/" + req.getId()
                    ));
                } else if ("REJECTED".equalsIgnoreCase(req.getStatus())) {
                    notices.add(new NotificationItem(
                        req.getId(),
                        "❌ REJECTED: Your request for '" + req.getBook().getTitle() + "' was rejected.",
                        "error",
                        true,
                        "/user/notifications/dismiss/" + req.getId()
                    ));
                }
            }
            // 3. Unpaid fines
            List<Fine> userFines = fineRepository.findByIssuedBookMemberEmail(email);
            for (Fine fine : userFines) {
                if (!fine.isPaid() && !fine.isWaived()) {
                    notices.add(new NotificationItem(
                        fine.getId(),
                        "💸 UNPAID FINE: You have an outstanding fine of ₹" + fine.getAmount() + " for '" + fine.getIssuedBook().getBook().getTitle() + "'.",
                        "warning",
                        false,
                        null
                    ));
                }
            }
        }
        return notices;
    }

    public static class NotificationItem {
        private Long id;
        private String text;
        private String type; // error, success, warning, info
        private boolean dismissible;
        private String dismissUrl;

        public NotificationItem(Long id, String text, String type, boolean dismissible, String dismissUrl) {
            this.id = id;
            this.text = text;
            this.type = type;
            this.dismissible = dismissible;
            this.dismissUrl = dismissUrl;
        }

        public Long getId() { return id; }
        public String getText() { return text; }
        public String getType() { return type; }
        public boolean isDismissible() { return dismissible; }
        public String getDismissUrl() { return dismissUrl; }
    }
}
