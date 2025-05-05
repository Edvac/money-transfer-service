package com.example.moneytransferservice.repository;

import com.example.moneytransferservice.model.Account;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends CrudRepository<Account, Long> {

    /**
     * Find all accounts with a specific currency
     *
     * @param currency The currency code to search for
     * @return List of matching accounts
     */
    List<Account> findByCurrency(String currency);

    /**
     * Find accounts with balance greater than or equal to specified amount
     *
     * @param minBalance The minimum balance threshold
     * @return List of accounts with sufficient balance
     */
    List<Account> findByBalanceGreaterThanEqual(BigDecimal minBalance);

    /**
     * Find accounts owned by a specific person (partial match)
     *
     * @param namePattern Pattern to match against owner names
     * @return List of matching accounts
     */
    @Query("SELECT * FROM accounts WHERE owner_name LIKE CONCAT('%', :namePattern, '%')")
    List<Account> findByOwnerNameContaining(@Param("namePattern") String namePattern);

    /**
     * Update account balance - for use in money transfer operations
     *
     * @param id The account ID
     * @param amount The amount to add (positive) or subtract (negative)
     * @return Number of rows affected
     */
    @Modifying
    @Query("UPDATE accounts SET balance = balance + :amount, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    int updateBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * Find accounts with negative balance (potential overdrafts)
     *
     * @return List of accounts with negative balance
     */
    @Query("SELECT * FROM accounts WHERE balance < 0")
    List<Account> findAccountsWithNegativeBalance();

    /**
     * Optional custom finder to get an account with pessimistic locking
     * for safe concurrent money transfers
     *
     * @param id The account ID
     * @return Optional containing the locked account or empty if not found
     */
    @Query("SELECT * FROM accounts WHERE id = :id FOR UPDATE")
    Optional<Account> findByIdWithLock(@Param("id") Long id);
}