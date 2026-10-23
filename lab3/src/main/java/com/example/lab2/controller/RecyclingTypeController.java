package com.example.lab2.controller;

import java.net.URI;
import java.util.List;
import com.example.lab2.dto.Requests.TypeRequest;
import com.example.lab2.model.RecyclingType;
import com.example.lab2.service.RecyclingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recycling-types")
public class RecyclingTypeController {

    private final RecyclingService service;

    public RecyclingTypeController(RecyclingService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecyclingType> list() {
        return service.listTypes();
    }

    @GetMapping("/{id}")
    public RecyclingType get(@PathVariable Long id) {
        return service.getType(id);
    }

    @PostMapping
    public ResponseEntity<RecyclingType> create(
            @RequestBody TypeRequest request
    ) {
        RecyclingType created = service.createType(request);

        return ResponseEntity
                .created(URI.create("/api/recycling-types/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{id}")
    public RecyclingType update(
            @PathVariable Long id,
            @RequestBody TypeRequest request
    ) {
        return service.updateType(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteType(id);
        return ResponseEntity.noContent().build();
    }
}