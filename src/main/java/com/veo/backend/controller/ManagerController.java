package com.veo.backend.controller;

import com.veo.backend.dto.response.UserResponse;
import com.veo.backend.service.ManagerUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {
    private final ManagerUserService managerUserService;

    @GetMapping("/staff")
    public ResponseEntity<List<UserResponse>> getStaffList() {
        return ResponseEntity.ok(managerUserService.getStaffList());
    }

    @PatchMapping("/{userId}/{status}")
    public ResponseEntity<String> toggleStatus(
            @PathVariable Long userId,
            @PathVariable boolean status) {

        managerUserService.toggleUserStatus(userId, status);
        String message = status ? "Unlocked account successfully" : "Locked account successfully";

        return ResponseEntity.ok(message);
    }
}
