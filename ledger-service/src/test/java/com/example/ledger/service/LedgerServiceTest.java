package com.example.ledger.service;

import com.example.ledger.model.Account;
import com.example.ledger.model.LedgerEntry;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private LedgerService ledgerService;

    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setBalance(new BigDecimal("1000.00"));
        fromAccount.setVersion(1L);

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setBalance(new BigDecimal("500.00"));
        toAccount.setVersion(1L);
    }

    @Test
    void createAccountWithInitialBalance() {
        // Given
        final var initialBalance = new BigDecimal("1000.00");
        when(accountRepository.save(any(Account.class))).thenReturn(fromAccount);

        // When
        final var result = ledgerService.createAccount(initialBalance);

        // Then
        assertNotNull(result);
        assertEquals(initialBalance, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void getAccount() {
        // Given
        Long accountId = 1L;
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(fromAccount));

        // When
        Optional<Account> result = ledgerService.getAccount(accountId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(fromAccount, result.get());
    }

    @Test
    void getAccount_ShouldReturnEmptyWhenNotExists() {
        // Given
        Long accountId = 999L;
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When
        Optional<Account> result = ledgerService.getAccount(accountId);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void applyTransfer_ShouldProcessTransferSuccessfully() {
        // Given
        String transferId = "transfer-123";
        BigDecimal amount = new BigDecimal("100.00");
        
        when(ledgerEntryRepository.existsByTransferId(transferId)).thenReturn(false);
        when(accountRepository.findByIdsForUpdate(List.of(1L, 2L))).thenReturn(List.of(fromAccount, toAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(fromAccount, toAccount);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(new LedgerEntry());

        // When
        boolean result = ledgerService.applyTransfer(transferId, 1L, 2L, amount);

        // Then
        assertTrue(result);
        verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void applyTransfer_ShouldReturnTrueForDuplicateTransferId() {
        // Given
        String transferId = "transfer-123";
        when(ledgerEntryRepository.existsByTransferId(transferId)).thenReturn(true);

        // When
        boolean result = ledgerService.applyTransfer(transferId, 1L, 2L, new BigDecimal("100.00"));

        // Then
        assertTrue(result);
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void applyTransferExceptionInsufficientFunds() {
        // Given
        final var transferId = "transfer-123";
        final var fromAccount = Account.builder().id(1L).balance(new BigDecimal("100.00")).build();
        final var toAccount = Account.builder().id(2L).balance(new BigDecimal("500.00")).build();
        final var amount = new BigDecimal("2000.00");
        
        when(ledgerEntryRepository.existsByTransferId(transferId)).thenReturn(false);
        when(accountRepository.findByIdsForUpdate(List.of(1L, 2L))).thenReturn(List.of(fromAccount, toAccount));

        // When & Then
        assertThrows(IllegalStateException.class, () ->
            ledgerService.applyTransfer(transferId, 1L, 2L, amount));
    }

    @Test
    void applyTransferExceptionInvalidAmount() {
        // Given
        String transferId = "transfer-123";
        BigDecimal amount = new BigDecimal("-100.00");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ledgerService.applyTransfer(transferId, 1L, 2L, amount));
    }
}
