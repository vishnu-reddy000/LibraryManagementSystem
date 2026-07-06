package com.library.demo.repository;

import com.library.demo.model.Book;
import com.library.demo.model.BorrowRequest;
import com.library.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {
    List<BorrowRequest> findByStatus(String status);
    List<BorrowRequest> findByUser(User user);
    List<BorrowRequest> findByBook(Book book);
}
