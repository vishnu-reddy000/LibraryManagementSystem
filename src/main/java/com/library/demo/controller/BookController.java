package com.library.demo.controller;

import com.library.demo.model.Book;
import com.library.demo.service.BookService;
import com.library.demo.repository.CategoryRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

@Controller
@RequestMapping("/books")
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public String listBooks(@RequestParam(required = false) String search, Model model) {
        if (search != null && !search.isBlank()) {
            model.addAttribute("books", bookService.searchBooks(search));
            model.addAttribute("search", search);
        } else {
            model.addAttribute("books", bookService.getAllBooks());
        }
        
        java.util.Map<String, String> colors = new java.util.HashMap<>();
        categoryRepository.findAll().forEach(c -> {
            if (c.getName() != null && c.getColor() != null) {
                colors.put(c.getName().trim().toLowerCase(), c.getColor());
            }
        });
        model.addAttribute("categoryColors", colors);
        
        return "books";
    }

    @GetMapping("/add")
    public String addBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("pageTitle", "Add Book");
        model.addAttribute("categories", getMergedCategoryNames());
        return "book-form";
    }

    @PostMapping("/add")
    public String saveBook(@Valid @ModelAttribute Book book, BindingResult result,
                           @RequestParam("imageFile") MultipartFile imageFile,
                           Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Add Book");
            model.addAttribute("categories", getMergedCategoryNames());
            return "book-form";
        }
        handleImageUpload(book, imageFile);
        bookService.saveBook(book);
        redirectAttributes.addFlashAttribute("success", "Book added successfully!");
        return "redirect:/books";
    }

    @GetMapping("/edit/{id}")
    public String editBookForm(@PathVariable Long id, Model model) {
        model.addAttribute("book", bookService.getBookById(id));
        model.addAttribute("pageTitle", "Edit Book");
        model.addAttribute("categories", getMergedCategoryNames());
        return "book-form";
    }

    @PostMapping("/edit/{id}")
    public String updateBook(@PathVariable Long id, @Valid @ModelAttribute Book book, BindingResult result,
                             @RequestParam("imageFile") MultipartFile imageFile,
                             Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Edit Book");
            model.addAttribute("categories", getMergedCategoryNames());
            return "book-form";
        }
        handleImageUpload(book, imageFile);
        book.setId(id);
        bookService.saveBook(book);
        redirectAttributes.addFlashAttribute("success", "Book updated successfully!");
        return "redirect:/books";
    }

    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteBook(id);
            redirectAttributes.addFlashAttribute("success", "Book deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete book: it is currently borrowed or referenced by other records.");
        }
        return "redirect:/books";
    }

    private List<String> getMergedCategoryNames() {
        Set<String> catNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        // 1. From database categories
        categoryRepository.findAll().forEach(c -> {
            if (c.getName() != null && !c.getName().isBlank()) {
                catNames.add(c.getName());
            }
        });
        // 2. From existing books (in case they have category names not in categories database yet)
        bookService.getAllBooks().forEach(b -> {
            if (b.getCategory() != null && !b.getCategory().isBlank()) {
                catNames.add(b.getCategory());
            }
        });
        return new ArrayList<>(catNames);
    }

    private void handleImageUpload(Book book, MultipartFile imageFile) {
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String uploadsDir = "uploads";
                Path uploadsPath = Paths.get(uploadsDir);
                if (!Files.exists(uploadsPath)) {
                    Files.createDirectories(uploadsPath);
                }
                String originalFilename = imageFile.getOriginalFilename();
                String cleanFilename = System.currentTimeMillis() + "_" + (originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_") : "cover.jpg");
                Path filePath = uploadsPath.resolve(cleanFilename);
                Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                book.setImageUrl("/uploads/" + cleanFilename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
