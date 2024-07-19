package com.example.transactionservice.controller;

import com.example.transactionservice.dto.TransferDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.transactionservice.dto.TransactionDTO;
import com.example.transactionservice.service.TransactionService;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    //all transaction
    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    //transaction by account number
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccountNumber(accountNumber));
    }

    //transaction by account type
    @GetMapping("/transaction/{transactionType}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByTransactionType(@PathVariable String transactionType) {
        return ResponseEntity.ok(transactionService.getTransactionsByTransactionType(transactionType));
    }

    //transaction between users
    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestBody TransferDTO transferDTO) {
        String message=transactionService.transfer(transferDTO);
        return ResponseEntity.ok(message);
    }
}
