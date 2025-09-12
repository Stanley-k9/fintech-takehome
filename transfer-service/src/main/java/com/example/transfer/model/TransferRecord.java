package com.example.transfer.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "transfer_records", 
       uniqueConstraints = @UniqueConstraint(columnNames = "idempotencyKey"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String transferId;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long fromAccountId;

    @Column(nullable = false)
    private Long toAccountId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column
    private String errorMessage;

    public enum TransferStatus {
        PENDING, COMPLETED, FAILED
    }
}
