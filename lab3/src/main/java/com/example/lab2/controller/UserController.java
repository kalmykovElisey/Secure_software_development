package com.example.lab2.controller;

import java.net.URI;
import java.util.List;
import com.example.lab2.dto.Requests.UserRequest;
import com.example.lab2.model.User;
import com.example.lab2.service.RecyclingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final RecyclingService service;

    public UserController(RecyclingService service) {
        this.service = service;
    }

    @GetMapping
    public List<User> list() {
        return service.listUsers();
    }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return service.getUser(id);
    }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody UserRequest request) {
        User created = service.createUser(request);

        return ResponseEntity
                .created(URI.create("/api/users/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{id}")
    public User update(
            @PathVariable Long id,
            @RequestBody UserRequest request
    ) {
        return service.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}