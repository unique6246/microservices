package com.example.accountservices.controller;

import com.example.accountservices.Repo.AccountRepository;
import com.example.accountservices.dto.AccountDTO;
import com.example.accountservices.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping
    public ResponseEntity<List<AccountDTO>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getAccountById(@PathVariable Long id) {
        AccountDTO accountDTO = accountService.getAccountById(id);
        return accountDTO != null ? ResponseEntity.ok(accountDTO) : ResponseEntity.notFound().build();
    }

    //for transaction only
    @GetMapping("customer/{id}")
    public ResponseEntity<AccountDTO> getAccountByCustomerId(@PathVariable Long id)
    {
        return ResponseEntity.ok(accountService.getAccountByCustomerId(id));
    }

    @GetMapping("account/{accountNumber}")
    public ResponseEntity<AccountDTO> getAccountByAccountNumber(@PathVariable String accountNumber)
    {
        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }


    @PostMapping
    public ResponseEntity<String> createAccount(@RequestBody AccountDTO accountDTO) {
        if(accountRepository.existsAccountByAccountNumber(accountDTO.getAccountNumber())){
            return ResponseEntity.ok("Account already exists");
        }
        accountService.createAccount(accountDTO);
        return ResponseEntity.ok("Account created successfully\n");
    }

    //used during transaction
    @PutMapping("/update-balance")
    public void updateBalance(@RequestBody AccountDTO accountDTO) {
        accountService.updateBalance(accountDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok("Account deleted successfully");
    }
}
