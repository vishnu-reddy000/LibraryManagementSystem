package com.library.demo.service;

import com.library.demo.model.Book;
import com.library.demo.model.BorrowRequest;
import com.library.demo.model.IssuedBook;
import com.library.demo.repository.BookRepository;
import com.library.demo.repository.BorrowRequestRepository;
import com.library.demo.repository.FineRepository;
import com.library.demo.repository.IssuedBookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@SuppressWarnings("null")
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private IssuedBookRepository issuedBookRepository;

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private BorrowRequestRepository borrowRequestRepository;

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
    }

    public Book saveBook(Book book) {
        if (book.getId() == null && book.getAvailableCopies() == 0 && book.getTotalCopies() > 0) {
            book.setAvailableCopies(book.getTotalCopies());
        }
        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(Long id) {
        bookRepository.findById(id).ifPresent(book -> {
            List<BorrowRequest> requests = borrowRequestRepository.findByBook(book);
            borrowRequestRepository.deleteAll(requests);

            List<IssuedBook> records = issuedBookRepository.findByBook(book);
            for (IssuedBook record : records) {
                fineRepository.findByIssuedBook(record).ifPresent(fineRepository::delete);
                issuedBookRepository.delete(record);
            }
            bookRepository.delete(book);
        });
    }

    public List<Book> searchBooks(String query) {
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(query, query);
    }

    public long countBooks() {
        return bookRepository.count();
    }
}
