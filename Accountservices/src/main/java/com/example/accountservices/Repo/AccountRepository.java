package com.example.accountservices.Repo;

import com.example.accountservices.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findAccountByCustomerId(long customerId);
    Boolean existsAccountByCustomerId(long customerId);
    Boolean existsAccountByAccountNumber(String accountNumber);
    void deleteAccountByAccountNumber(String accountNumber);
    Account findAccountByAccountNumber(String accountNumber);

    // ── Real-world additions ─────────────────────────────────────────────────
    List<Account> findAllByAccountTypeAndStatus(String accountType, String status);
    List<Account> findAllByStatusAndMaturityDateLessThanEqual(String status, LocalDate date);
    List<Account> findAllByCustomerIdAndStatus(Long customerId, String status);

    @Modifying
    @Query("UPDATE Account a SET a.usedDailyAmount = 0, a.dailyResetDate = :today WHERE a.dailyResetDate < :today OR a.dailyResetDate IS NULL")
    int resetDailyUsage(@Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(a.usedDailyAmount), 0) FROM Account a WHERE a.accountNumber = :accountNumber")
    BigDecimal getDailyUsed(@Param("accountNumber") String accountNumber);
}
