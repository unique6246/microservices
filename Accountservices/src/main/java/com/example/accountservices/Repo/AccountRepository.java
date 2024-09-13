package com.example.accountservices.Repo;

import com.example.accountservices.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findAccountByCustomerId(long customerId);
    Account findAccountById(long id);
    Account findAccountByAccountNumber(String accountNumber);
    Boolean existsAccountByAccountNumber(String accountNumber);
}
