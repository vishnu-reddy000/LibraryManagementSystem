package com.library.demo.controller;

import com.library.demo.model.User;
import com.library.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String settingsPage(Authentication authentication, Model model) {
        String email = authentication.getName();
        userRepository.findByEmail(email).ifPresent(u -> model.addAttribute("adminUser", u));
        model.addAttribute("finePerDay", 5.0);
        model.addAttribute("maxIssueDays", 14);
        return "settings";
    }

    @PostMapping("/change-password")
    public String changePassword(Authentication authentication,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes ra) {
        String email = authentication.getName();
        User admin = userRepository.findByEmail(email).orElse(null);

        if (admin == null) {
            ra.addFlashAttribute("error", "User not found.");
            return "redirect:/settings";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New passwords do not match.");
            return "redirect:/settings";
        }
        if (newPassword.length() < 6) {
            ra.addFlashAttribute("error", "Password must be at least 6 characters.");
            return "redirect:/settings";
        }
        admin.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(admin);
        ra.addFlashAttribute("success", "Password changed successfully!");
        return "redirect:/settings";
    }

    @PostMapping("/update-profile")
    public String updateProfile(Authentication authentication,
                                @RequestParam String name,
                                RedirectAttributes ra) {
        String email = authentication.getName();
        userRepository.findByEmail(email).ifPresent(admin -> {
            admin.setName(name);
            userRepository.save(admin);
        });
        ra.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/settings";
    }
}
