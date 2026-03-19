package com.veo.backend.controller;

import com.veo.backend.dto.request.UserCreateRequest;
import com.veo.backend.dto.response.UserCreateResponse;
import com.veo.backend.service.AdminUserService;
import com.veo.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserCreateResponse createUser(@RequestBody UserCreateRequest request) {
        return adminUserService.createUser(request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }
}
