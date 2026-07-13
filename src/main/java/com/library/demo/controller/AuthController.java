package com.library.demo.controller;

import com.library.demo.model.User;
import com.library.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            HttpServletRequest request,
                            Model model) {
        if (error != null) {
            HttpSession session = request.getSession(false);
            String errorMessage = "Invalid email or password.";
            if (session != null) {
                Exception ex = (Exception) session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
                if (ex != null && ex.getMessage() != null && !ex.getMessage().trim().isEmpty()) {
                    errorMessage = ex.getMessage();
                }
            }
            model.addAttribute("error", errorMessage);
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out.");
        }
        return "login";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signupSubmit(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               @RequestParam(defaultValue = "USER") String role,
                               Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "signup";
        }
        try {
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setRole(User.Role.valueOf(role.toUpperCase()));
            userService.register(user);
            return "redirect:/login?registered=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "signup";
        }
    }
}
