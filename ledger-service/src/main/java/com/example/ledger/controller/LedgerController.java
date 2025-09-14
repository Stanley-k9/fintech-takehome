package com.example.ledger.controller;

import com.example.ledger.model.Account;
import com.example.ledger.service.LedgerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping
@Validated
@Slf4j
@AllArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        final var account = ledgerService.createAccount(request.getInitialBalance());
        return ResponseEntity.ok(account);
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        final var account = ledgerService.getAccount(id);
        return account.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/ledger/transfer")
    public ResponseEntity<TransferResponse> applyTransfer(@Valid @RequestBody TransferRequest request) {
        try {
            boolean success = ledgerService.applyTransfer(
                    request.getTransferId(),
                    request.getFromAccountId(),
                    request.getToAccountId(),
                    request.getAmount()
            );
            return ResponseEntity.ok(new TransferResponse(success, "Transfer completed successfully"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Transfer failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new TransferResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during transfer", e);
            return ResponseEntity.internalServerError()
                    .body(new TransferResponse(false, "Internal server error"));
        }
    }

    // instead of using actuators we can utilize the custome one
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Ledger Service is healthy");
    }

    @Data
    public static class CreateAccountRequest {
        @NotNull
        @Positive
        private BigDecimal initialBalance;

        public BigDecimal getInitialBalance() {
            return initialBalance;
        }

        public void setInitialBalance(BigDecimal initialBalance) {
            this.initialBalance = initialBalance;
        }
    }

    //instead of creating generated DTOs we can hard code it below here
    public static class TransferRequest {
        @NotNull
        private String transferId;

        @NotNull
        private Long fromAccountId;

        @NotNull
        private Long toAccountId;

        @NotNull
        @Positive
        private BigDecimal amount;

        // Constructors
        public TransferRequest() {}

        public TransferRequest(String transferId, Long fromAccountId, Long toAccountId, BigDecimal amount) {
            this.transferId = transferId;
            this.fromAccountId = fromAccountId;
            this.toAccountId = toAccountId;
            this.amount = amount;
        }

        // Getters and Setters
        public String getTransferId() { return transferId; }
        public void setTransferId(String transferId) { this.transferId = transferId; }
        
        public Long getFromAccountId() { return fromAccountId; }
        public void setFromAccountId(Long fromAccountId) { this.fromAccountId = fromAccountId; }
        
        public Long getToAccountId() { return toAccountId; }
        public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    public static class TransferResponse {
        private boolean success;
        private String message;

        public TransferResponse() {}
        
        public TransferResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
