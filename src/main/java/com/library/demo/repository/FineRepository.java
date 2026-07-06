package com.library.demo.repository;

import com.library.demo.model.Fine;
import com.library.demo.model.IssuedBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {
    Optional<Fine> findByIssuedBook(IssuedBook issuedBook);
    List<Fine> findByPaidFalseAndWaivedFalse();
    List<Fine> findByPaidTrue();
    List<Fine> findByIssuedBookMemberEmail(String email);
}
