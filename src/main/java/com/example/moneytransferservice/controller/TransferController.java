package com.example.moneytransferservice.controller;

import com.example.moneytransferservice.model.Transaction;
import com.example.moneytransferservice.repository.TransactionRepository;
import com.example.moneytransferservice.service.TransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private static final Logger log = LoggerFactory.getLogger(TransferController.class);

    private final TransferService transferService;
    private final TransactionRepository transactionRepository;

    @Autowired
    public TransferController(TransferService transferService,
                              TransactionRepository transactionRepository) {
        this.transferService = transferService;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping
    public ResponseEntity<?> createTransfer(@RequestBody Map<String, Object> transferRequest) {
        try {
            // Check if required fields exist
            if (!transferRequest.containsKey("fromAccountId") ||
                    !transferRequest.containsKey("toAccountId") ||
                    !transferRequest.containsKey("amount")) {

                return createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Missing required fields. Please provide fromAccountId, toAccountId, and amount."
                );
            }

            // Try to parse the values
            Long fromAccountId;
            Long toAccountId;
            BigDecimal amount;

            try {
                fromAccountId = Long.valueOf(transferRequest.get("fromAccountId").toString());
                toAccountId = Long.valueOf(transferRequest.get("toAccountId").toString());
                amount = new BigDecimal(transferRequest.get("amount").toString());
            } catch (NumberFormatException e) {
                log.warn("Invalid number format in transfer request: {}", e.getMessage());
                return createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Invalid number format. Please ensure all values are in the correct format."
                );
            }

            // Perform the transfer
            log.debug("Processing transfer request: {} from account {} to account {}",
                    amount, fromAccountId, toAccountId);

            Transaction transaction = transferService.transferMoney(fromAccountId, toAccountId, amount);

            log.info("Transfer successfully processed with ID: {}", transaction.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);

        } catch (IllegalArgumentException e) {
            // Handle validation errors (negative amount, currency mismatch)
            log.warn("Transfer validation failed: {}", e.getMessage());
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (RuntimeException e) {
            // Check if this is an account not found error
            if (e.getMessage() != null &&
                    (e.getMessage().contains("Source account not found") ||
                            e.getMessage().contains("Destination account not found"))) {

                log.warn("Transfer failed due to missing account: {}", e.getMessage());
                return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
            }

            // For all other runtime errors
            log.error("Unexpected error during transfer: {}", e.getMessage(), e);
            return createErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred while processing your transfer. Please try again later."
            );
        }
    }

    @GetMapping
    public List<Transaction> getAllTransfers(@RequestParam(required = false) Long accountId) {
        if (accountId != null) {
            log.debug("Getting transfers for account ID: {}", accountId);
            return transactionRepository.findByFromAccountIdOrToAccountId(accountId, accountId);
        }

        log.debug("Getting all transfers");
        List<Transaction> transactions = new ArrayList<>();
        transactionRepository.findAll().forEach(transactions::add);
        return transactions;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransferById(@PathVariable Long id) {
        log.debug("Getting transfer with ID: {}", id);
        Optional<Transaction> transaction = transactionRepository.findById(id);
        return transaction.map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Transfer not found with ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Helper method to create consistent error responses
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("timestamp", new Date());
        return ResponseEntity.status(status).body(errorResponse);
    }
}
