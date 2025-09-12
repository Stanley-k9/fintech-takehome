package com.example.transfer.controller;

import com.example.transfer.model.TransferRecord;
import com.example.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/transfers")
@Validated
@Slf4j
@AllArgsConstructor
@Tag(name = "Transfer", description = "Transfer management")
public class TransferController {

    private final TransferService transferService;


    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @Valid @RequestBody CreateTransferRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            HttpServletRequest httpRequest) {
        
        // Set request correlation ID
        var requestId = httpRequest.getHeader("X-Request-ID");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put("requestId", requestId);
        
        log.info("Creating transfer with idempotency key: {}", idempotencyKey);
        
        try {
            TransferRecord transferRecord = transferService.createTransfer(
                    idempotencyKey,
                    request.getFromAccountId(),
                    request.getToAccountId(),
                    request.getAmount()
            );
            
            return ResponseEntity.ok(new TransferResponse(
                    transferRecord.getTransferId(),
                    transferRecord.getStatus().toString(),
                    transferRecord.getErrorMessage()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new TransferResponse(null, "FAILED", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new TransferResponse(null, "FAILED", "Internal server error"));
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable String id) {

        final var transferRecord = transferService.getTransfer(id);
        return transferRecord.map(record -> ResponseEntity.ok(new TransferResponse(
                record.getTransferId(),
                record.getStatus().toString(),
                record.getErrorMessage()
        ))).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/batch")
    @Operation(summary = "Batch transfers", description = "Process up to 20 transfers concurrently")
    public ResponseEntity<BatchTransferResponse> processBatchTransfers(
            @Valid @RequestBody BatchTransferRequest request,
            HttpServletRequest httpRequest) {
        
        // Set request correlation ID
        String requestId = httpRequest.getHeader("X-Request-ID");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put("requestId", requestId);
        
        log.info("Processing batch of {} transfers", request.getTransfers().size());
        
        try {
            List<TransferRecord> transferRecords = transferService.processBatchTransfers(request.getTransfers());
            
            List<TransferResponse> responses = transferRecords.stream()
                    .map(record -> new TransferResponse(
                            record.getTransferId(),
                            record.getStatus().toString(),
                            record.getErrorMessage()
                    ))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(new BatchTransferResponse(responses));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid batch transfer request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new BatchTransferResponse(List.of()));
        } catch (Exception e) {
            log.error("Unexpected error processing batch transfers", e);
            return ResponseEntity.internalServerError()
                    .body(new BatchTransferResponse(List.of()));
        } finally {
            MDC.clear();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateTransferRequest {
        @NotNull
        private Long fromAccountId;
        
        @NotNull
        private Long toAccountId;
        
        @NotNull
        @Positive
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferResponse {
        private String transferId;
        private String status;
        private String errorMessage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchTransferRequest {
        @NotNull
        @Size(min = 1, max = 20, message = "Batch size must be between 1 and 20")
        private List<TransferService.BatchTransferRequest> transfers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchTransferResponse {
        private List<TransferResponse> transfers;
    }
}
