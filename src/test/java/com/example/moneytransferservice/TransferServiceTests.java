package com.example.moneytransferservice;

import com.example.moneytransferservice.model.Account;
import com.example.moneytransferservice.model.Transaction;
import com.example.moneytransferservice.repository.AccountRepository;
import com.example.moneytransferservice.repository.TransactionRepository;
import com.example.moneytransferservice.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class TransferServiceTests {

    @Autowired
    private TransferService transferService;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private TransactionRepository transactionRepository;

    @Test
    public void testAccountCreation() {
        // Prepare
        Account newAccount = new Account("David", new BigDecimal("300.00"), "USD");
        when(accountRepository.save(any(Account.class))).thenReturn(newAccount);

        // Act
        Account savedAccount = accountRepository.save(newAccount);

        // Assert
        assertNotNull(savedAccount);
        assertEquals("David", savedAccount.getOwnerName());
        assertEquals(new BigDecimal("300.00"), savedAccount.getBalance());
        assertEquals("USD", savedAccount.getCurrency());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    public void testMoneyTransferSuccess() {
        // Prepare
        Account fromAccount = new Account("Alice", new BigDecimal("100.00"), "USD");
        fromAccount.setId(1L);
        Account toAccount = new Account("Bob", new BigDecimal("50.00"), "USD");
        toAccount.setId(2L);

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.updateBalance(eq(1L), any(BigDecimal.class))).thenReturn(1);
        when(accountRepository.updateBalance(eq(2L), any(BigDecimal.class))).thenReturn(1);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Transaction transaction = transferService.transferMoney(1L, 2L, new BigDecimal("25.00"));

        // Assert
        assertNotNull(transaction);
        assertEquals(1L, transaction.getFromAccountId());
        assertEquals(2L, transaction.getToAccountId());
        assertEquals(new BigDecimal("25.00"), transaction.getAmount());
        assertEquals("COMPLETED", transaction.getStatus());
        verify(accountRepository).updateBalance(eq(1L), eq(new BigDecimal("25.00").negate()));
        verify(accountRepository).updateBalance(eq(2L), eq(new BigDecimal("25.00")));
    }


    @Test
    public void testCurrencyMismatch() {
        // Prepare
        Account fromAccount = new Account("Alice", new BigDecimal("100.00"), "USD");
        fromAccount.setId(1L);
        Account toAccount = new Account("Carlos", new BigDecimal("50.00"), "EUR");
        toAccount.setId(3L);

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdWithLock(3L)).thenReturn(Optional.of(toAccount));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transferService.transferMoney(1L, 3L, new BigDecimal("25.00"));
        });

        assertTrue(exception.getMessage().contains("Currency mismatch"));
        verify(accountRepository, never()).updateBalance(any(Long.class), any(BigDecimal.class));
    }

    @Test
    public void testAccountNotFound() {
        // Prepare
        when(accountRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transferService.transferMoney(999L, 2L, new BigDecimal("25.00"));
        });

        assertTrue(exception.getMessage().contains("Source account not found"));
        verify(accountRepository, never()).updateBalance(any(Long.class), any(BigDecimal.class));
    }

    @Test
    public void testNegativeAmountTransfer() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transferService.transferMoney(1L, 2L, new BigDecimal("-25.00"));
        });

        assertTrue(exception.getMessage().contains("must be positive"));
        verify(accountRepository, never()).findByIdWithLock(any(Long.class));
        verify(accountRepository, never()).updateBalance(any(Long.class), any(BigDecimal.class));
    }

    @Test
    public void testZeroAmountTransfer() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transferService.transferMoney(1L, 2L, BigDecimal.ZERO);
        });

        assertTrue(exception.getMessage().contains("must be positive"));
        verify(accountRepository, never()).findByIdWithLock(any(Long.class));
        verify(accountRepository, never()).updateBalance(any(Long.class), any(BigDecimal.class));
    }

    @Test
    public void testTransactionRecording() {
        // Prepare
        Account fromAccount = new Account("Alice", new BigDecimal("100.00"), "USD");
        fromAccount.setId(1L);
        Account toAccount = new Account("Bob", new BigDecimal("50.00"), "USD");
        toAccount.setId(2L);

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.updateBalance(any(Long.class), any(BigDecimal.class))).thenReturn(1);

        Transaction expectedTransaction = new Transaction(1L, 2L, new BigDecimal("25.00"), "USD", "COMPLETED");
        expectedTransaction.setId(1L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(expectedTransaction);

        // Act
        Transaction transaction = transferService.transferMoney(1L, 2L, new BigDecimal("25.00"));

        // Assert
        assertNotNull(transaction);
        assertEquals(1L, transaction.getId());
        assertEquals(1L, transaction.getFromAccountId());
        assertEquals(2L, transaction.getToAccountId());
        assertEquals(new BigDecimal("25.00"), transaction.getAmount());
        assertEquals("USD", transaction.getCurrency());
        assertEquals("COMPLETED", transaction.getStatus());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    public void testConcurrentTransfers() throws InterruptedException {
        // Prepare
        int numberOfThreads = 5;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        Account account = new Account("Shared", new BigDecimal("1000.00"), "USD");
        account.setId(5L);

        when(accountRepository.findByIdWithLock(any(Long.class))).thenReturn(Optional.of(account));
        when(accountRepository.updateBalance(any(Long.class), any(BigDecimal.class))).thenReturn(1);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        for (int i = 0; i < numberOfThreads; i++) {
            service.execute(() -> {
                try {
                    transferService.transferMoney(5L, 6L, new BigDecimal("10.00"));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // Wait for all threads to complete
        service.shutdown();

        // Assert
        verify(accountRepository, times(numberOfThreads * 2)).findByIdWithLock(any(Long.class));
        verify(accountRepository, times(numberOfThreads)).updateBalance(eq(5L), eq(new BigDecimal("10.00").negate()));
        verify(accountRepository, times(numberOfThreads)).updateBalance(eq(6L), eq(new BigDecimal("10.00")));
        verify(transactionRepository, times(numberOfThreads)).save(any(Transaction.class));
    }

    @Test
    public void testAccountRetrieval() {
        // Prepare
        Account account = new Account("Alice", new BigDecimal("100.00"), "USD");
        account.setId(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        // Act
        Optional<Account> retrievedAccount = accountRepository.findById(1L);

        // Assert
        assertTrue(retrievedAccount.isPresent());
        assertEquals(1L, retrievedAccount.get().getId());
        assertEquals("Alice", retrievedAccount.get().getOwnerName());
        assertEquals(new BigDecimal("100.00"), retrievedAccount.get().getBalance());
        assertEquals("USD", retrievedAccount.get().getCurrency());
        verify(accountRepository).findById(1L);
    }
}


