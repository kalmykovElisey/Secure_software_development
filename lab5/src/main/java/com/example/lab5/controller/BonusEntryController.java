package com.example.lab5.controller;

import java.net.URI;
import java.util.List;
import com.example.lab5.dto.Requests.BonusRequest;
import com.example.lab5.model.BonusEntry;
import com.example.lab5.service.RecyclingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.example.lab5.security.AccessRules;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/bonus-entries")
public class BonusEntryController {

    private final RecyclingService service;
    private final AccessRules access;

    public BonusEntryController(RecyclingService service, AccessRules access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public List<BonusEntry> list(Authentication auth) {
        return service.listBonuses().stream()
                .filter(bonus -> access.readBonus(bonus.id(), auth)).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.readBonus(#id, authentication)")
    public BonusEntry get(@PathVariable Long id) {
        return service.getBonus(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BonusEntry> create(
            @RequestBody BonusRequest request
    ) {
        BonusEntry created = service.createBonus(request);

        return ResponseEntity
                .created(URI.create("/api/bonus-entries/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BonusEntry update(
            @PathVariable Long id,
            @RequestBody BonusRequest request
    ) {
        return service.updateBonus(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteBonus(id);
        return ResponseEntity.noContent().build();
    }
}
