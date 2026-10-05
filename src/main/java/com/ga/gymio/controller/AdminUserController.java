package com.ga.gymio.controller;

import com.ga.gymio.dto.request.AdminUserUpdateRequest;
import com.ga.gymio.model.User;
import com.ga.gymio.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{id}")
    public String updateUser(
            @PathVariable Long id,
            @RequestBody AdminUserUpdateRequest request) {

        userService.updateUser(id, request);

        return "User updated successfully";
    }
}