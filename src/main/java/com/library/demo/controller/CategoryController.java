package com.library.demo.controller;

import com.library.demo.model.Category;
import com.library.demo.repository.BookRepository;
import com.library.demo.repository.CategoryRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/categories")
@SuppressWarnings("null")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BookRepository bookRepository;

    @GetMapping
    public String listCategories(Model model) {
        autoImportBookCategories();
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("category", new Category());
        populateCounts(model);
        return "categories";
    }

    @PostMapping("/add")
    public String addCategory(@Valid @ModelAttribute Category category,
                              BindingResult result, Model model,
                              RedirectAttributes ra) {
        autoImportBookCategories();
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAll());
            populateCounts(model);
            return "categories";
        }
        if (categoryRepository.existsByName(category.getName())) {
            ra.addFlashAttribute("error", "Category '" + category.getName() + "' already exists.");
            return "redirect:/categories";
        }
        categoryRepository.save(category);
        ra.addFlashAttribute("success", "Category added successfully!");
        return "redirect:/categories";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        categoryRepository.deleteById(id);
        ra.addFlashAttribute("success", "Category deleted!");
        return "redirect:/categories";
    }

    @PostMapping("/edit/{id}")
    public String editCategory(@PathVariable Long id,
                               @RequestParam String name,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String color,
                               RedirectAttributes ra) {
        categoryRepository.findById(id).ifPresent(cat -> {
            cat.setName(name);
            cat.setDescription(description);
            cat.setColor(color);
            categoryRepository.save(cat);
        });
        ra.addFlashAttribute("success", "Category updated!");
        return "redirect:/categories";
    }

    private void autoImportBookCategories() {
        var books = bookRepository.findAll();
        for (com.library.demo.model.Book book : books) {
            String catName = book.getCategory();
            if (catName != null && !catName.isBlank()) {
                final String cleanName = catName.trim();
                boolean exists = categoryRepository.findAll().stream()
                        .anyMatch(c -> c.getName().equalsIgnoreCase(cleanName));
                if (!exists) {
                    Category newCat = new Category();
                    newCat.setName(cleanName);
                    newCat.setDescription("Imported from existing books");
                    newCat.setColor("#3b82f6"); // Default blue
                    categoryRepository.save(newCat);
                }
            }
        }
    }

    private void populateCounts(Model model) {
        var books = bookRepository.findAll();
        Map<String, Long> counts = new HashMap<>();
        var categories = categoryRepository.findAll();
        for (com.library.demo.model.Book b : books) {
            if (b.getCategory() != null && !b.getCategory().isBlank()) {
                String catKey = b.getCategory().trim();
                String matchedName = categories.stream()
                        .filter(c -> c.getName().equalsIgnoreCase(catKey))
                        .map(Category::getName)
                        .findFirst()
                        .orElse(catKey);
                counts.put(matchedName, counts.getOrDefault(matchedName, 0L) + 1);
            }
        }
        model.addAttribute("categoryBookCounts", counts);
    }
}
