package com.example.moneytransferservice.repository;

import com.example.moneytransferservice.model.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TransactionRepository extends CrudRepository<Transaction, Long> {

    /**
     * Find all transactions involving a specific account (as sender or receiver)
     *
     * @param accountId The account ID
     * @return List of transactions
     */
    List<Transaction> findByFromAccountIdOrToAccountId(Long accountId, Long sameAccountId);
}