package com.example.transfer.service;

import com.example.transfer.client.LedgerClient;
import com.example.transfer.dto.LedgerTransferRequest;
import com.example.transfer.dto.LedgerTransferResponse;
import com.example.transfer.model.TransferRecord;
import com.example.transfer.repository.TransferRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRecordRepository transferRecordRepository;

    @Mock
    private LedgerClient ledgerClient;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(transferRecordRepository, ledgerClient);
    }

    @Test
    void createTransfer() {
        // Given
        final var idempotencyKey = "test-key";
        final var fromAccountId = 1L;
        final var toAccountId = 2L;
        final var amount = new BigDecimal("100.00");

        when(transferRecordRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(transferRecordRepository.save(any(TransferRecord.class)))
                .thenAnswer(invocation -> {
                    TransferRecord record = invocation.getArgument(0);
                    record.setId(1L);
                    return record;
                });
        
        // Mock the LedgerClient response for the async processing
        LedgerTransferResponse response = new LedgerTransferResponse(true, "Success");
        when(ledgerClient.transfer(any(LedgerTransferRequest.class)))
                .thenReturn(response);

        // When
        final var result = transferService.createTransfer(idempotencyKey, fromAccountId, toAccountId, amount);

        // Then
        assertNotNull(result);
        assertEquals(fromAccountId, result.getFromAccountId());
        assertEquals(toAccountId, result.getToAccountId());
        assertEquals(amount, result.getAmount());
        
        // The status should be PENDING initially, but may change to COMPLETED due to async processing
        // We'll check that it's either PENDING or COMPLETED (both are valid)
        assertTrue(result.getStatus() == TransferRecord.TransferStatus.PENDING || 
                  result.getStatus() == TransferRecord.TransferStatus.COMPLETED,
                  "Status should be PENDING or COMPLETED, but was: " + result.getStatus());
        
        // Wait a bit for async processing to complete
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // After waiting, the status should be COMPLETED due to successful async processing
        assertEquals(TransferRecord.TransferStatus.COMPLETED, result.getStatus());
    }

    @Test
    void processTransfer() {
        // Given
        TransferRecord transferRecord = new TransferRecord();
        transferRecord.setTransferId("test-transfer");
        transferRecord.setFromAccountId(1L);
        transferRecord.setToAccountId(2L);
        transferRecord.setAmount(new BigDecimal("100.00"));

        final var response = new LedgerTransferResponse(true, "Success");
        when(ledgerClient.transfer(any(LedgerTransferRequest.class)))
                .thenReturn(response);
        when(transferRecordRepository.save(any(TransferRecord.class)))
                .thenReturn(transferRecord);

        // When
        transferService.processTransfer(transferRecord);

        // Then
        assertEquals(TransferRecord.TransferStatus.COMPLETED, transferRecord.getStatus());
    }
}
