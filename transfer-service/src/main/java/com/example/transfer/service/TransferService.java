package com.example.transfer.service;

import com.example.transfer.client.LedgerClient;
import com.example.transfer.dto.LedgerTransferRequest;
import com.example.transfer.dto.LedgerTransferResponse;
import com.example.transfer.model.TransferRecord;
import com.example.transfer.repository.TransferRecordRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class TransferService {

    private final TransferRecordRepository transferRecordRepository;
    private final LedgerClient ledgerClient;
    private final Executor executor = Executors.newFixedThreadPool(10);

    @Transactional
    public TransferRecord createTransfer(String idempotencyKey, Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // Check for existing transfer with same idempotency key
        final var existingTransfer = transferRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTransfer.isPresent()) {
            return existingTransfer.get();
        }

        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        // Generate transfer ID
        final var transferId = UUID.randomUUID().toString();

        // Create transfer record
        var transferRecord = TransferRecord.builder()
                .transferId(transferId)
                .idempotencyKey(idempotencyKey)
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .status(TransferRecord.TransferStatus.PENDING)
                .build();

        transferRecord = transferRecordRepository.save(transferRecord);

        // Process transfer asynchronously
        processTransferAsync(transferRecord);

        return transferRecord;
    }

    private void processTransferAsync(TransferRecord transferRecord) {
        CompletableFuture.runAsync(() -> {
            try {
                processTransfer(transferRecord);
            } catch (Exception e) {
                log.error("Error processing transfer {} asynchronously", transferRecord.getTransferId(), e);
            }
        }, executor);
    }

    @CircuitBreaker(name = "ledger-service", fallbackMethod = "fallbackTransfer")
    @Retry(name = "ledger-service")
    @Transactional
    public void processTransfer(TransferRecord transferRecord) {

        try {
            LedgerTransferRequest request = new LedgerTransferRequest(
                    transferRecord.getTransferId(),
                    transferRecord.getFromAccountId(),
                    transferRecord.getToAccountId(),
                    transferRecord.getAmount()
            );

            final var response = ledgerClient.transfer(request);

            if (response.isSuccess()) {
                transferRecord.setStatus(TransferRecord.TransferStatus.COMPLETED);
                log.info("Transfer {} completed successfully", transferRecord.getTransferId());
            } else {
                transferRecord.setStatus(TransferRecord.TransferStatus.FAILED);
                transferRecord.setErrorMessage(response.getMessage());
                log.warn("Transfer {} failed: {}", transferRecord.getTransferId(), response.getMessage());
            }
        } catch (Exception e) {
            transferRecord.setStatus(TransferRecord.TransferStatus.FAILED);
            transferRecord.setErrorMessage(e.getMessage());
            log.error("Transfer {} failed with exception", transferRecord.getTransferId(), e);
        }

        transferRecordRepository.save(transferRecord);
    }

    public void fallbackTransfer(TransferRecord transferRecord, Exception ex) {
        log.error("Circuit breaker opened for transfer {}", transferRecord.getTransferId(), ex);
        transferRecord.setStatus(TransferRecord.TransferStatus.FAILED);
        transferRecord.setErrorMessage("Ledger service unavailable");
        transferRecordRepository.save(transferRecord);
    }

    public Optional<TransferRecord> getTransfer(String transferId) {
        log.info("Fetching transfer: {}", transferId);
        return transferRecordRepository.findByTransferId(transferId);
    }

    @Transactional
    public List<TransferRecord> processBatchTransfers(List<BatchTransferRequest> requests) {
        log.info("Processing batch of {} transfers", requests.size());
        
        if (requests.size() > 20) {
            throw new IllegalArgumentException("Batch size cannot exceed 20 transfers");
        }

        final var futures = requests.stream()
                .map(request -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return createTransfer(
                                request.getIdempotencyKey(),
                                request.getFromAccountId(),
                                request.getToAccountId(),
                                request.getAmount()
                        );
                    } catch (Exception e) {
                        log.error("Error creating transfer in batch", e);
                        return null;
                    }
                }, executor))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(transferRecord -> transferRecord != null)
                .map(transferRecord -> (TransferRecord) transferRecord)
                .collect(Collectors.toList());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchTransferRequest {
        private String idempotencyKey;
        private Long fromAccountId;
        private Long toAccountId;
        private BigDecimal amount;
    }
}
