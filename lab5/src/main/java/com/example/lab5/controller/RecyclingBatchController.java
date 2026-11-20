package com.example.lab5.controller;

import java.net.URI;
import java.util.List;
import com.example.lab5.dto.Requests.BatchRequest;
import com.example.lab5.model.RecyclingBatch;
import com.example.lab5.service.RecyclingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.example.lab5.security.AccessRules;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/recycling-batches")
public class RecyclingBatchController {

    private final RecyclingService service;
    private final AccessRules access;

    public RecyclingBatchController(RecyclingService service, AccessRules access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public List<RecyclingBatch> list(Authentication auth) {
        return service.listBatches().stream()
                .filter(batch -> access.readUser(batch.userId(), auth)).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.readBatch(#id, authentication)")
    public RecyclingBatch get(@PathVariable Long id) {
        return service.getBatch(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<RecyclingBatch> create(
            @RequestBody BatchRequest request
    ) {
        RecyclingBatch created = service.createBatch(request);

        return ResponseEntity
                .created(URI.create("/api/recycling-batches/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public RecyclingBatch update(
            @PathVariable Long id,
            @RequestBody BatchRequest request
    ) {
        return service.updateBatch(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteBatch(id);
        return ResponseEntity.noContent().build();
    }
}
