package com.example.transfer.controller;

import com.example.transfer.model.TransferRecord;
import com.example.transfer.service.TransferService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock
    private TransferService transferService;

    @InjectMocks
    private TransferController transferController;

    @Test
    void createTransfer() {
        // Given
        TransferRecord transferRecord = new TransferRecord();
        transferRecord.setTransferId("transfer-123");
        transferRecord.setStatus(TransferRecord.TransferStatus.COMPLETED);
        
        when(transferService.createTransfer(anyString(), any(), any(), any(BigDecimal.class)))
                .thenReturn(transferRecord);

        // When
        TransferController.CreateTransferRequest request = new TransferController.CreateTransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        
        var httpRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(httpRequest.getHeader("X-Request-ID")).thenReturn("test-request-123");
        
        var response = transferController.createTransfer(request, "idempotency-key-123", httpRequest);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("transfer-123", response.getBody().getTransferId());
        assertEquals("COMPLETED", response.getBody().getStatus());
    }

    @Test
    void getTransfer() {
        // Given
        TransferRecord transferRecord = new TransferRecord();
        transferRecord.setTransferId("transfer-456");
        transferRecord.setStatus(TransferRecord.TransferStatus.PENDING);
        
        when(transferService.getTransfer("transfer-456"))
                .thenReturn(java.util.Optional.of(transferRecord));

        // When
        var response = transferController.getTransfer("transfer-456");

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("transfer-456", response.getBody().getTransferId());
        assertEquals("PENDING", response.getBody().getStatus());
    }
}
