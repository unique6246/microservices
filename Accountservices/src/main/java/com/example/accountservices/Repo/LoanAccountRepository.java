package com.example.accountservices.Repo;

import com.example.accountservices.entity.LoanAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {
    List<LoanAccount> findAllByCustomerId(Long customerId);
    List<LoanAccount> findAllByStatus(String status);
    Optional<LoanAccount> findByAccountId(Long accountId);

    @Query("SELECT l FROM LoanAccount l WHERE l.nextEmiDate <= :today AND l.status = 'ACTIVE'")
    List<LoanAccount> findOverdueLoans(@Param("today") LocalDate today);

    boolean existsByCustomerIdAndStatus(Long customerId, String status);
}

