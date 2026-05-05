package com.example.transactionservice.Repo;

import com.example.transactionservice.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findTransactionsByTransactionType(String transactionType);
    List<Transaction> findByAccountNumber(String accountNumber);
    Page<Transaction> findAll(Pageable pageable);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
