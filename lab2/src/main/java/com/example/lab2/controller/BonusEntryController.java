package com.example.lab2.controller;

import java.net.URI;
import java.util.List;
import com.example.lab2.dto.Requests.BonusRequest;
import com.example.lab2.model.BonusEntry;
import com.example.lab2.service.RecyclingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bonus-entries")
public class BonusEntryController {

    private final RecyclingService service;

    public BonusEntryController(RecyclingService service) {
        this.service = service;
    }

    @GetMapping
    public List<BonusEntry> list() {
        return service.listBonuses();
    }

    @GetMapping("/{id}")
    public BonusEntry get(@PathVariable Long id) {
        return service.getBonus(id);
    }

    @PostMapping
    public ResponseEntity<BonusEntry> create(
            @RequestBody BonusRequest request
    ) {
        BonusEntry created = service.createBonus(request);

        return ResponseEntity
                .created(URI.create("/api/bonus-entries/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{id}")
    public BonusEntry update(
            @PathVariable Long id,
            @RequestBody BonusRequest request
    ) {
        return service.updateBonus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteBonus(id);
        return ResponseEntity.noContent().build();
    }
}