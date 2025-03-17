package com.picpaybackend.controllers;

import com.picpaybackend.domain.transaction.Transaction;
import com.picpaybackend.domain.user.User;
import com.picpaybackend.dtos.TransactionDTO;
import com.picpaybackend.services.TransactionService;
import jakarta.persistence.EntityNotFoundException;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestBody TransactionDTO transactionDTO) throws Exception{
        Transaction transaction = transactionService.createTransaction(transactionDTO);
        return new ResponseEntity<>(transaction, HttpStatus.OK);
    }

    @PostMapping("/reversed/{id}")
    public ResponseEntity<Map<String, Object>> revertTransaction(@PathVariable UUID id) throws Exception{
        Map<String, Object> response = new HashMap<>();
        Transaction transaction = transactionService.findTransactionsById(id);
        transactionService.revertTransaction(transaction);
        response.put("status", "OK");
        response.put("message", "Transação revertida com sucesso.");
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransaction(){
        List<Transaction> transactions = this.transactionService.getAllTransactions();
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

}
