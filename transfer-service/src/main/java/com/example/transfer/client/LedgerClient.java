package com.example.transfer.client;

import com.example.transfer.dto.LedgerTransferRequest;
import com.example.transfer.dto.LedgerTransferResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ledger-service", url = "${ledger.service.url}")
public interface LedgerClient {
    
    @PostMapping("/ledger/transfer")
    LedgerTransferResponse transfer(@RequestBody LedgerTransferRequest request);
}


