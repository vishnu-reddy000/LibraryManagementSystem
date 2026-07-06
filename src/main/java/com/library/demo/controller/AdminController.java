package com.library.demo.controller;

import com.library.demo.model.IssuedBook;
import com.library.demo.model.IssueStatus;
import com.library.demo.repository.UserRepository;
import com.library.demo.service.BookService;
import com.library.demo.service.FineService;
import com.library.demo.service.IssueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AdminController {

    @Autowired private BookService bookService;
    @Autowired private UserRepository userRepository;
    @Autowired private IssueService issueService;
    @Autowired private FineService fineService;

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        long totalBooks   = bookService.countBooks();
        long totalUsers   = userRepository.count();
        long issuedBooks  = issueService.countIssuedBooks();
        long overdueBooks = issueService.countOverdueBooks();
        long availableBooks = bookService.getAllBooks().stream()
                .mapToLong(b -> b.getAvailableCopies()).sum();

        fineService.calculateFines();

        List<IssuedBook> allIssues = issueService.getAllIssues();
        List<IssuedBook> recentIssues = allIssues.stream()
                .sorted((a, b) -> b.getIssueDate().compareTo(a.getIssueDate()))
                .limit(5).toList();
        List<IssuedBook> dueSoon = issueService.getIssuedBooks().stream()
                .sorted((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
                .limit(4).toList();
        var lowStock = bookService.getAllBooks().stream()
                .filter(b -> b.getAvailableCopies() <= 2).limit(4).toList();

        model.addAttribute("totalBooks",     totalBooks);
        model.addAttribute("totalUsers",     totalUsers);
        model.addAttribute("issuedBooks",    issuedBooks);
        model.addAttribute("overdueBooks",   overdueBooks);
        model.addAttribute("availableBooks", availableBooks);
        model.addAttribute("totalFines",     fineService.totalFinesCollected());
        model.addAttribute("recentIssues",   recentIssues);
        model.addAttribute("dueSoon",        dueSoon);
        model.addAttribute("lowStock",       lowStock);
        return "admin";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("totalBooks",   bookService.countBooks());
        model.addAttribute("totalUsers",   userRepository.count());
        model.addAttribute("issuedBooks",  issueService.countIssuedBooks());
        model.addAttribute("overdueBooks", issueService.countOverdueBooks());
        long returnedBooks = issueService.getAllIssues().stream()
                .filter(i -> i.getStatus() == IssueStatus.RETURNED)
                .count();
        model.addAttribute("returnedBooks", returnedBooks);
        model.addAttribute("totalFines",   fineService.totalFinesCollected());
        model.addAttribute("allIssues",    issueService.getAllIssues());
        return "reports";
    }
}
