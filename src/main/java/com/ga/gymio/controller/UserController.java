package com.ga.gymio.controller;

import com.ga.gymio.model.User;
import com.ga.gymio.model.request.LoginRequest;
import com.ga.gymio.model.response.LoginResponse;
import com.ga.gymio.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping(path = "/auth/users")
public class UserController {
    private UserService userService;

    @PostMapping("/register")
    public User createUser(@RequestBody User userObject){
        System.out.println("Calling createUser ==> ");
        return userService.createUser(userObject);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@RequestBody LoginRequest loginRequest) {
        System.out.println("Controller Calling Login() ==>");
        return ResponseEntity.ok(userService.loginUser(loginRequest));
    }
}