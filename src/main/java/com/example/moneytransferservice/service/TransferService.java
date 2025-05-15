package com.example.moneytransferservice.service;

import com.example.moneytransferservice.model.Account;
import com.example.moneytransferservice.model.Transaction;
import com.example.moneytransferservice.repository.AccountRepository;
import com.example.moneytransferservice.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);


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
        log.info("Transfer initiated: {} from account {} to account {}",
                amount, fromAccountId, toAccountId);

        try {
            // Validate amount
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Transfer rejected: Invalid amount {}", amount);
                throw new IllegalArgumentException("Transfer amount must be positive");
            }

            // Lock accounts to prevent concurrent modifications
            log.debug("Locking source account {}", fromAccountId);
            Account fromAccount = accountRepository.findByIdWithLock(fromAccountId)
                    .orElseThrow(() -> {
                        log.error("Transfer failed: Source account {} not found", fromAccountId);
                        return new RuntimeException("Source account not found: " + fromAccountId);
                    });

            log.debug("Locking destination account {}", toAccountId);
            Account toAccount = accountRepository.findByIdWithLock(toAccountId)
                    .orElseThrow(() -> {
                        log.error("Transfer failed: Destination account {} not found", toAccountId);
                        return new RuntimeException("Destination account not found: " + toAccountId);
                    });

            // Validate currency match
            if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
                log.warn("Transfer rejected: Currency mismatch between accounts");
                throw new IllegalArgumentException("Currency mismatch between accounts");
            }

            // Perform the transfer
            log.debug("Updating balances for transfer: {} from account {} to account {}",
                    amount, fromAccountId, toAccountId);
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

            Transaction savedTransaction = transactionRepository.save(transaction);
            log.info("Transfer completed successfully: ID {}, {} from account {} to account {}",
                    savedTransaction.getId(), amount, fromAccountId, toAccountId);

            return savedTransaction;
        } catch (Exception e) {
            log.error("Transfer failed between accounts {} and {}: {}",
                    fromAccountId, toAccountId, e.getMessage());
            throw e;
        }

    }
}