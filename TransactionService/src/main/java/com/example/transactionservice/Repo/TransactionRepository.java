package com.example.transactionservice.Repo;

import com.example.transactionservice.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findTransactionsByTransactionType(String transactionType);
    List<Transaction> findByAccountNumber(String accountNumber);
    Page<Transaction> findAll(Pageable pageable);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    boolean existsByIdempotencyKey(String idempotencyKey);

    // ── Real-world additions ────────────────────────────────────────��────────
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    Page<Transaction> findByAccountNumberOrderByCreatedAtDesc(String accountNumber, Pageable pageable);

    List<Transaction> findByAccountNumberAndCreatedAtBetweenOrderByCreatedAtAsc(
            String accountNumber, LocalDateTime from, LocalDateTime to);

    List<Transaction> findByAccountNumberAndChannelAndCreatedAtBetween(
            String accountNumber, String channel, LocalDateTime from, LocalDateTime to);

    Page<Transaction> findByFraudFlagTrue(Pageable pageable);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.accountNumber = :account AND t.createdAt >= :since")
    long countRecentTransactions(@Param("account") String accountNumber,
                                 @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.accountNumber = :account AND t.transactionType = 'DEBIT' " +
           "AND t.createdAt >= :since AND t.status = 'COMPLETED'")
    BigDecimal sumDebitsAfter(@Param("account") String accountNumber,
                              @Param("since") LocalDateTime since);
}
