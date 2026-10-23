package com.example.lab2.controller;

import java.net.URI;
import java.util.List;
import com.example.lab2.dto.Requests.PointRequest;
import com.example.lab2.model.CollectionPoint;
import com.example.lab2.service.RecyclingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/collection-points")
public class CollectionPointController {

    private final RecyclingService service;

    public CollectionPointController(RecyclingService service) {
        this.service = service;
    }

    @GetMapping
    public List<CollectionPoint> list() {
        return service.listPoints();
    }

    @GetMapping("/{id}")
    public CollectionPoint get(@PathVariable Long id) {
        return service.getPoint(id);
    }

    @PostMapping
    public ResponseEntity<CollectionPoint> create(
            @RequestBody PointRequest request
    ) {
        CollectionPoint created = service.createPoint(request);

        return ResponseEntity
                .created(URI.create("/api/collection-points/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{id}")
    public CollectionPoint update(
            @PathVariable Long id,
            @RequestBody PointRequest request
    ) {
        return service.updatePoint(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deletePoint(id);
        return ResponseEntity.noContent().build();
    }
}