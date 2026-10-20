package com.ga.gymio.controller;

import com.ga.gymio.dto.request.AdminUserUpdateRequest;
import com.ga.gymio.model.User;
import com.ga.gymio.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Provides administrative endpoints for managing Gymio users.
 *
 * Access to all endpoints in this controller is restricted to users
 * with the ADMIN role.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    /**
     * Retrieves all users registered in the system.
     *
     * @return a list of all users
     */
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    /**
     * Updates a user's information using the supplied request data.
     *
     * @param id the ID of the user to update
     * @param request the updated user information
     * @return a success message confirming the update
     */
    @PutMapping("/{id}")
    public String updateUser(
            @PathVariable Long id,
            @RequestBody AdminUserUpdateRequest request) {

        userService.updateUser(id, request);

        return "User updated successfully";
    }
}