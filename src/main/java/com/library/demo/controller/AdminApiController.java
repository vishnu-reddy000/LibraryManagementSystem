package com.library.demo.controller;

import com.library.demo.model.BorrowRequest;
import com.library.demo.repository.BorrowRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
public class AdminApiController {

    @Autowired
    private BorrowRequestRepository borrowRequestRepository;

    @GetMapping("/api/admin/pending-requests")
    public List<Map<String, Object>> getPendingRequests() {
        return borrowRequestRepository.findByStatus("PENDING").stream()
                .filter(req -> !req.isAdminDeleted())
                .map(req -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", req.getId());
                    map.put("bookTitle", req.getBook().getTitle());
                    map.put("memberName", req.getUser().getName());
                    map.put("memberEmail", req.getUser().getEmail());
                    map.put("requestDate", req.getRequestDate().toString());
                    map.put("adminRead", req.isAdminRead());
                    return map;
                }).collect(Collectors.toList());
    }

    @PostMapping("/api/admin/pending-requests/read/{id}")
    public Map<String, String> markAsRead(@PathVariable Long id) {
        borrowRequestRepository.findById(id).ifPresent(req -> {
            req.setAdminRead(true);
            borrowRequestRepository.save(req);
        });
        return Map.of("status", "success");
    }

    @PostMapping("/api/admin/pending-requests/read-all")
    public Map<String, String> markAllAsRead() {
        List<BorrowRequest> pending = borrowRequestRepository.findByStatus("PENDING");
        for (BorrowRequest req : pending) {
            if (!req.isAdminDeleted() && !req.isAdminRead()) {
                req.setAdminRead(true);
                borrowRequestRepository.save(req);
            }
        }
        return Map.of("status", "success");
    }

    @PostMapping("/api/admin/pending-requests/delete/{id}")
    public Map<String, String> markAsDeleted(@PathVariable Long id) {
        borrowRequestRepository.findById(id).ifPresent(req -> {
            req.setAdminDeleted(true);
            borrowRequestRepository.save(req);
        });
        return Map.of("status", "success");
    }
}
