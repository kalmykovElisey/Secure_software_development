package com.example.lab2.controller;

import java.net.URI;
import java.util.List;
import com.example.lab2.dto.Requests.BatchRequest;
import com.example.lab2.model.RecyclingBatch;
import com.example.lab2.service.RecyclingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recycling-batches")
public class RecyclingBatchController {

    private final RecyclingService service;

    public RecyclingBatchController(RecyclingService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecyclingBatch> list() {
        return service.listBatches();
    }

    @GetMapping("/{id}")
    public RecyclingBatch get(@PathVariable Long id) {
        return service.getBatch(id);
    }

    @PostMapping
    public ResponseEntity<RecyclingBatch> create(
            @RequestBody BatchRequest request
    ) {
        RecyclingBatch created = service.createBatch(request);

        return ResponseEntity
                .created(URI.create("/api/recycling-batches/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{id}")
    public RecyclingBatch update(
            @PathVariable Long id,
            @RequestBody BatchRequest request
    ) {
        return service.updateBatch(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteBatch(id);
        return ResponseEntity.noContent().build();
    }
}