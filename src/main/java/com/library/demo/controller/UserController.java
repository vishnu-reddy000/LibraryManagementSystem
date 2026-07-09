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
import com.library.demo.model.DismissedNotification;
import com.library.demo.repository.FineRepository;
import com.library.demo.repository.DismissedNotificationRepository;
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
    private DismissedNotificationRepository dismissedNotificationRepository;

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
        return getDashboardNotices(email).size();
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
            List<String> dismissedKeys = dismissedNotificationRepository.findByUser(user).stream()
                    .map(DismissedNotification::getNotificationKey)
                    .collect(Collectors.toList());
            List<BorrowRequest> requests = borrowRequestRepository.findByUser(user);
            for (BorrowRequest req : requests) {
                String key = "borrow_" + req.getId();
                if ("PENDING".equalsIgnoreCase(req.getStatus()) && !dismissedKeys.contains(key)) {
                    notices.add(new NotificationItem(
                        key,
                        "ℹ️ PENDING: Your borrow request for '" + req.getBook().getTitle() + "' is pending approval.",
                        "info",
                        true,
                        "/user/notifications/dismiss/" + key
                    ));
                }
            }
        }

        model.addAttribute("notifications", notices);
        populateCommonModelAttributes(email, model);
        return "user-notifications";
    }

    @PostMapping("/user/notifications/dismiss/{key}")
    public String dismissNotification(@PathVariable String key, Authentication authentication) {
        String email = authentication.getName();
        com.library.demo.model.User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            if (!dismissedNotificationRepository.existsByUserAndNotificationKey(user, key)) {
                dismissedNotificationRepository.save(new DismissedNotification(user, key));
            }
            if (key.startsWith("borrow_")) {
                try {
                    Long id = Long.parseLong(key.substring(7));
                    borrowRequestRepository.findById(id).ifPresent(req -> {
                        if (req.getUser().getId().equals(user.getId())) {
                            if ("APPROVED".equalsIgnoreCase(req.getStatus()) || "REJECTED".equalsIgnoreCase(req.getStatus())) {
                                borrowRequestRepository.delete(req);
                            }
                        }
                    });
                } catch (Exception e) {
                    // Ignore parsing error
                }
            }
        }
        return "redirect:/user/notifications";
    }

    @PostMapping("/user/notifications/clear-all")
    public String clearAllNotifications(Authentication authentication) {
        String email = authentication.getName();
        com.library.demo.model.User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            List<NotificationItem> activeNotices = getDashboardNotices(email);
            // Also fetch pending ones since they are shown on the notifications page
            List<String> dismissedKeys = dismissedNotificationRepository.findByUser(user).stream()
                    .map(DismissedNotification::getNotificationKey)
                    .collect(Collectors.toList());
            List<BorrowRequest> requests = borrowRequestRepository.findByUser(user);
            for (BorrowRequest req : requests) {
                String key = "borrow_" + req.getId();
                if ("PENDING".equalsIgnoreCase(req.getStatus()) && !dismissedKeys.contains(key)) {
                    activeNotices.add(new NotificationItem(
                        key,
                        "",
                        "",
                        true,
                        ""
                    ));
                }
            }

            for (NotificationItem notif : activeNotices) {
                if (!dismissedNotificationRepository.existsByUserAndNotificationKey(user, notif.getKey())) {
                    dismissedNotificationRepository.save(new DismissedNotification(user, notif.getKey()));
                }
                if (notif.getKey().startsWith("borrow_")) {
                    try {
                        Long id = Long.parseLong(notif.getKey().substring(7));
                        borrowRequestRepository.findById(id).ifPresent(req -> {
                            if (req.getUser().getId().equals(user.getId())) {
                                if ("APPROVED".equalsIgnoreCase(req.getStatus()) || "REJECTED".equalsIgnoreCase(req.getStatus())) {
                                    borrowRequestRepository.delete(req);
                                }
                            }
                        });
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
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
            List<String> dismissedKeys = dismissedNotificationRepository.findByUser(user).stream()
                    .map(DismissedNotification::getNotificationKey)
                    .collect(Collectors.toList());

            // 1. Check for overdue books
            List<IssuedBook> activeIssues = issueService.getActiveIssuesByEmail(email);
            for (IssuedBook issue : activeIssues) {
                String key = "overdue_" + issue.getId();
                if (issue.getDueDate().isBefore(java.time.LocalDate.now()) && !dismissedKeys.contains(key)) {
                    notices.add(new NotificationItem(
                        key,
                        "⚠️ OVERDUE WARNING: The book '" + issue.getBook().getTitle() + 
                        "' was due on " + issue.getDueDate() + ". Please return it immediately.",
                        "error",
                        true,
                        "/user/notifications/dismiss/" + key
                    ));
                }
            }
            // 2. Borrow requests
            List<BorrowRequest> requests = borrowRequestRepository.findByUser(user);
            for (BorrowRequest req : requests) {
                String key = "borrow_" + req.getId();
                if (dismissedKeys.contains(key)) {
                    continue;
                }
                if ("APPROVED".equalsIgnoreCase(req.getStatus())) {
                    notices.add(new NotificationItem(
                        key,
                        "✅ APPROVED: Your request for '" + req.getBook().getTitle() + "' has been approved!",
                        "success",
                        true,
                        "/user/notifications/dismiss/" + key
                    ));
                } else if ("REJECTED".equalsIgnoreCase(req.getStatus())) {
                    notices.add(new NotificationItem(
                        key,
                        "❌ REJECTED: Your request for '" + req.getBook().getTitle() + "' was rejected.",
                        "error",
                        true,
                        "/user/notifications/dismiss/" + key
                    ));
                }
            }
            // 3. Unpaid fines
            List<Fine> userFines = fineRepository.findByIssuedBookMemberEmail(email);
            for (Fine fine : userFines) {
                String key = "fine_" + fine.getId();
                if (!fine.isPaid() && !fine.isWaived() && !dismissedKeys.contains(key)) {
                    notices.add(new NotificationItem(
                        key,
                        "💸 UNPAID FINE: You have an outstanding fine of ₹" + fine.getAmount() + " for '" + fine.getIssuedBook().getBook().getTitle() + "'.",
                        "warning",
                        true,
                        "/user/notifications/dismiss/" + key
                    ));
                }
            }
        }
        return notices;
    }

    public static class NotificationItem {
        private String key;
        private String text;
        private String type; // error, success, warning, info
        private boolean dismissible;
        private String dismissUrl;

        public NotificationItem(String key, String text, String type, boolean dismissible, String dismissUrl) {
            this.key = key;
            this.text = text;
            this.type = type;
            this.dismissible = dismissible;
            this.dismissUrl = dismissUrl;
        }

        public String getKey() { return key; }
        public String getText() { return text; }
        public String getType() { return type; }
        public boolean isDismissible() { return dismissible; }
        public String getDismissUrl() { return dismissUrl; }
    }
}
