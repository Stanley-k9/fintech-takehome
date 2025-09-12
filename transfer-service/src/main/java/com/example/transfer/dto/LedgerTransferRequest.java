package com.example.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerTransferRequest {
    private String transferId;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
}


