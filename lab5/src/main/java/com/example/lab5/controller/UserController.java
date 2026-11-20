package com.example.lab5.controller;

import java.net.URI;
import java.util.List;
import com.example.lab5.dto.Requests.UserRequest;
import com.example.lab5.model.User;
import com.example.lab5.service.RecyclingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/users")
public class UserController {

    private final RecyclingService service;

    public UserController(RecyclingService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public List<User> list() {
        return service.listUsers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.readUser(#id, authentication)")
    public User get(@PathVariable Long id) {
        return service.getUser(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> create(@RequestBody UserRequest request) {
        User created = service.createUser(request);

        return ResponseEntity
                .created(URI.create("/api/users/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@access.editUser(#id, authentication)")
    public User update(
            @PathVariable Long id,
            @RequestBody UserRequest request
    ) {
        return service.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and !@access.ownUser(#id, authentication)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
