package com.example.lab5.controller;

import com.example.lab5.dto.Operations.*;
import com.example.lab5.dto.Requests.BatchRequest;
import com.example.lab5.service.RecyclingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/operations")
public class OperationsController {

    private final RecyclingService service;

    public OperationsController(RecyclingService service) {
        this.service = service;
    }

    @PostMapping("/accept")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<BatchWithBonus> accept(
            @RequestBody BatchRequest request
    ) {
        BatchWithBonus result = service.acceptWithBonus(request);

        return ResponseEntity
                .created(URI.create(
                        "/api/recycling-batches/" + result.batch().id()
                ))
                .body(result);
    }

    @PostMapping("/batches/{batchId}/correct-weight")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public BatchWithBonus correctWeight(
            @PathVariable("batchId") Long batchId,
            @RequestBody WeightCorrectionRequest request
    ) {
        return service.correctWeight(batchId, request);
    }

    @PostMapping("/users/{userId}/accrue-bonuses")
    @PreAuthorize("hasRole('ADMIN')")
    public AccrualResult accrueBonuses(
            @PathVariable("userId") Long userId,
            @RequestBody AccrualRequest request
    ) {
        return service.accrueBonuses(userId, request);
    }

    @GetMapping("/available-points")
    public List<AvailablePoint> availablePoints(
            @RequestParam("typeId") Long typeId,
            @RequestParam("weightKg") BigDecimal weightKg
    ) {
        return service.findAvailablePoints(typeId, weightKg);
    }

    @GetMapping("/users/{userId}/summary")
    @PreAuthorize("@access.readUser(#userId, authentication)")
    public UserSummary summary(
            @PathVariable("userId") Long userId,
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return service.summarizeUser(userId, from, to);
    }
}
