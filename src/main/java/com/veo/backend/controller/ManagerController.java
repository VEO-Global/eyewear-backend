package com.veo.backend.controller;

import com.veo.backend.dto.request.ManagerUserStatusRequest;
import com.veo.backend.dto.request.ManagerUserUpdateRequest;
import com.veo.backend.dto.request.UserCreateRequest;
import com.veo.backend.dto.response.PagedResponse;
import com.veo.backend.dto.response.UserCreateResponse;
import com.veo.backend.dto.response.UserResponse;
import com.veo.backend.service.ManagerUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {
    private final ManagerUserService managerUserService;

    @GetMapping("/staff")
    public ResponseEntity<PagedResponse<UserResponse>> getStaffList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(managerUserService.getStaffList(keyword, role, active, page, size));
    }

    @GetMapping("/staff/{userId}")
    public ResponseEntity<UserResponse> getStaffById(@PathVariable Long userId) {
        return ResponseEntity.ok(managerUserService.getStaffById(userId));
    }

    @PostMapping("/staff")
    public ResponseEntity<UserCreateResponse> createStaff(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(managerUserService.createStaff(request));
    }

    @PutMapping("/staff/{userId}")
    public ResponseEntity<UserResponse> updateStaff(
            @PathVariable Long userId,
            @RequestBody ManagerUserUpdateRequest request) {
        return ResponseEntity.ok(managerUserService.updateStaff(userId, request));
    }

    @PatchMapping("/staff/{userId}/status")
    public ResponseEntity<String> toggleStatus(
            @PathVariable Long userId,
            @Valid @RequestBody ManagerUserStatusRequest request) {
        managerUserService.toggleUserStatus(userId, request);
        String message = Boolean.TRUE.equals(request.getIsActive())
                ? "Unlocked account successfully"
                : "Locked account successfully";
        return ResponseEntity.ok(message);
    }

    @GetMapping("/roles")
    public ResponseEntity<?> getManageableRoles() {
        return ResponseEntity.ok(managerUserService.getManageableRoles());
    }
}
