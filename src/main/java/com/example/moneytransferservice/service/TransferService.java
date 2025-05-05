package com.example.moneytransferservice.service;

import com.example.moneytransferservice.model.Account;
import com.example.moneytransferservice.model.Transaction;
import com.example.moneytransferservice.repository.AccountRepository;
import com.example.moneytransferservice.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransferService {
    
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    
    @Autowired
    public TransferService(AccountRepository accountRepository, 
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }
    
    /**
     * Transfer money between two accounts
     *
     * @param fromAccountId Source account ID
     * @param toAccountId Destination account ID
     * @param amount Amount to transfer
     * @return The created transaction record
     * @throws IllegalArgumentException If validation fails
     * @throws RuntimeException If accounts not found or other errors
     */
    @Transactional
    public Transaction transferMoney(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        
        // Lock accounts to prevent concurrent modifications
        Account fromAccount = accountRepository.findByIdWithLock(fromAccountId)
            .orElseThrow(() -> new RuntimeException("Source account not found: " + fromAccountId));
            
        Account toAccount = accountRepository.findByIdWithLock(toAccountId)
            .orElseThrow(() -> new RuntimeException("Destination account not found: " + toAccountId));
            
        // Validate currency match
        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new IllegalArgumentException("Currency mismatch between accounts");
        }
        
        // Perform the transfer
        accountRepository.updateBalance(fromAccountId, amount.negate());
        accountRepository.updateBalance(toAccountId, amount);
        
        // Record the transaction
        Transaction transaction = new Transaction(
            fromAccountId, 
            toAccountId, 
            amount, 
            fromAccount.getCurrency(),
            "COMPLETED"
        );
        
        return transactionRepository.save(transaction);
    }
}