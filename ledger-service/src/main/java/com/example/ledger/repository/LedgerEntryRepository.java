package com.example.ledger.repository;

import com.example.ledger.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    
    Optional<LedgerEntry> findFirstByTransferId(String transferId);
    
    @Query("SELECT le FROM LedgerEntry le WHERE le.transferId = :transferId")
    List<LedgerEntry> findByTransferId(@Param("transferId") String transferId);
    
    boolean existsByTransferId(String transferId);
}
