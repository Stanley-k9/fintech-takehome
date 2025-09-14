package com.example.ledger.service;

import com.example.ledger.model.Account;
import com.example.ledger.model.LedgerEntry;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.LedgerEntryRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class LedgerService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public Account createAccount(BigDecimal initialBalance) {
        Account account = Account.builder()
                .balance(initialBalance)
                .build();
        return accountRepository.save(account);
    }

    public Optional<Account> getAccount(Long id) {
        log.info("Fetching account: {}", id);
        return accountRepository.findById(id);
    }

    @Transactional
    public boolean applyTransfer(String transferId, Long fromAccountId, Long toAccountId, BigDecimal amount) {

        // if ledger entries exist for this transferId, return success
        if (ledgerEntryRepository.existsByTransferId(transferId)) {
            return true;
        }

        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        // Lock accounts in order to prevent deadlocks
        final var accountIds = List.of(fromAccountId, toAccountId);
        final var accounts = accountRepository.findByIdsForUpdate(accountIds);
        
        if (accounts.size() != 2) {
            throw new IllegalArgumentException("One or both accounts not found");
        }

        final var fromAccount = accounts.stream()
                .filter(a -> a.getId().equals(fromAccountId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("From account not found"));

        final var toAccount = accounts.stream()
                .filter(a -> a.getId().equals(toAccountId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("To account not found"));

        // Check sufficient balance
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds in account " + fromAccountId);
        }

        // Update balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Create ledger entries (one DEBIT, one CREDIT)
        final var debitEntry = LedgerEntry.builder()
                .transferId(transferId)
                .accountId(fromAccountId)
                .amount(amount)
                .type(LedgerEntry.EntryType.DEBIT)
                .build();

        final var creditEntry = LedgerEntry.builder()
                .transferId(transferId)
                .accountId(toAccountId)
                .amount(amount)
                .type(LedgerEntry.EntryType.CREDIT)
                .build();

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        return true;
    }
}
