package com.example.moneytransferservice.controller;

import com.example.moneytransferservice.model.Transaction;
import com.example.moneytransferservice.repository.TransactionRepository;
import com.example.moneytransferservice.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;
    private final TransactionRepository transactionRepository;

    @Autowired
    public TransferController(TransferService transferService, 
                             TransactionRepository transactionRepository) {
        this.transferService = transferService;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction createTransfer(@RequestBody Map<String, Object> transferRequest) {
        Long fromAccountId = Long.valueOf(transferRequest.get("fromAccountId").toString());
        Long toAccountId = Long.valueOf(transferRequest.get("toAccountId").toString());
        BigDecimal amount = new BigDecimal(transferRequest.get("amount").toString());
        
        return transferService.transferMoney(fromAccountId, toAccountId, amount);
    }

    @GetMapping
    public List<Transaction> getAllTransfers() {
        return (List<Transaction>) transactionRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransferById(@PathVariable Long id) {
        Optional<Transaction> transaction = transactionRepository.findById(id);
        return transaction.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}