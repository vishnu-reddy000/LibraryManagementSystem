package com.library.demo.controller;

import com.library.demo.model.User;
import com.library.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
@SuppressWarnings("null")
public class UserManagementController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String listUsers(@RequestParam(required = false) String search, Model model) {
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            model.addAttribute("users", userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.Role.USER)
                    .filter(u -> u.getName().toLowerCase().contains(q)
                              || u.getEmail().toLowerCase().contains(q))
                    .toList());
            model.addAttribute("search", search);
        } else {
            model.addAttribute("users", userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.Role.USER)
                    .toList());
        }
        return "users";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
        return "redirect:/users";
    }

    @PostMapping("/role/{id}")
    public String changeRole(@PathVariable Long id,
                             @RequestParam String role,
                             RedirectAttributes redirectAttributes) {
        userRepository.findById(id).ifPresent(user -> {
            user.setRole(User.Role.valueOf(role.toUpperCase()));
            userRepository.save(user);
        });
        redirectAttributes.addFlashAttribute("success", "User role updated!");
        return "redirect:/users";
    }
}
