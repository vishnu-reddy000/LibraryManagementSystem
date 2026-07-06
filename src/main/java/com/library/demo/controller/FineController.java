package com.library.demo.controller;

import com.library.demo.service.FineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/fines")
public class FineController {

    @Autowired
    private FineService fineService;

    @GetMapping
    public String finePage(Model model) {
        fineService.calculateFines(); // auto-calculate on page load
        model.addAttribute("allFines",      fineService.getAllFines());
        model.addAttribute("pendingFines",  fineService.getPendingFines());
        model.addAttribute("totalCollected", fineService.totalFinesCollected());
        model.addAttribute("pendingCount",  fineService.countPendingFines());
        return "fines";
    }

    @PostMapping("/collect/{id}")
    public String collectFine(@PathVariable Long id, RedirectAttributes ra) {
        fineService.collectFine(id);
        ra.addFlashAttribute("success", "Fine collected successfully!");
        return "redirect:/fines";
    }

    @PostMapping("/waive/{id}")
    public String waiveFine(@PathVariable Long id, RedirectAttributes ra) {
        fineService.waiveFine(id);
        ra.addFlashAttribute("success", "Fine waived successfully!");
        return "redirect:/fines";
    }
}
