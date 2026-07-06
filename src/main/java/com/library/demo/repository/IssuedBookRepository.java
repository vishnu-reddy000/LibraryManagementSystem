package com.library.demo.repository;

import com.library.demo.model.Book;
import com.library.demo.model.IssuedBook;
import com.library.demo.model.IssueStatus;
import com.library.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssuedBookRepository extends JpaRepository<IssuedBook, Long> {
    List<IssuedBook> findByStatus(IssueStatus status);
    long countByStatus(IssueStatus status);
    List<IssuedBook> findByMember(User member);
    List<IssuedBook> findByMemberAndStatus(User member, IssueStatus status);
    List<IssuedBook> findByBook(Book book);
}
