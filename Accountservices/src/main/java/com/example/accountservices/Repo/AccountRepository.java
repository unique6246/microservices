package com.example.accountservices.Repo;

import com.example.accountservices.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findAccountsByCustomerId(long customerId);
    Account findAccountByCustomerId(long customerId);
    Account findAccountByAccountNumber(String accountNumber);
    Boolean existsAccountByAccountNumber(String accountNumber);
}
