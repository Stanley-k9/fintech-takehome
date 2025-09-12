package com.example.ledger.controller;

import com.example.ledger.model.Account;
import com.example.ledger.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerControllerTest {

    @Mock
    private LedgerService ledgerService;

    @InjectMocks
    private LedgerController ledgerController;

    @Test
    void createAccountCreated() {
        // Given
        final var account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("1000.00"));
        
        when(ledgerService.createAccount(any(BigDecimal.class))).thenReturn(account);

        // When
        LedgerController.CreateAccountRequest request = new LedgerController.CreateAccountRequest();
        request.setInitialBalance(new BigDecimal("1000.00"));
        final var response = ledgerController.createAccount(request);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(account, response.getBody());
    }

    @Test
    void getAccount() {
        // Given
        final var account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("1000.00"));
        
        when(ledgerService.getAccount(1L)).thenReturn(Optional.of(account));

        // When
        final var response = ledgerController.getAccount(1L);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(account, response.getBody());
    }

    @Test
    void getAccountNotFound() {
        // Given
        when(ledgerService.getAccount(999L)).thenReturn(Optional.empty());

        // When
        var response = ledgerController.getAccount(999L);

        // Then
        assertNotNull(response);
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void applyTransferValid() {
        // Given
        when(ledgerService.applyTransfer(any(), anyLong(), anyLong(), any(BigDecimal.class)))
                .thenReturn(true);

        // When
        LedgerController.TransferRequest request = new LedgerController.TransferRequest();
        request.setTransferId("transfer-123");
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        
        var response = ledgerController.applyTransfer(request);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Transfer completed successfully", response.getBody().getMessage());
    }

    @Test
    void applyTransferInvalid() {
        // Given
        when(ledgerService.applyTransfer(any(), anyLong(), anyLong(), any(BigDecimal.class)))
                .thenThrow(new IllegalArgumentException("Insufficient funds"));

        // When
        LedgerController.TransferRequest request = new LedgerController.TransferRequest();
        request.setTransferId("transfer-123");
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        
        var response = ledgerController.applyTransfer(request);

        // Then
        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Insufficient funds", response.getBody().getMessage());
    }
}
