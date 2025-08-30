package com.example.todo.controller;

import com.example.todo.model.User;
import com.example.todo.security.AuthService;
import com.example.todo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    public record UserInfo (
            String userName,
            String email
    ) {}

    public record EditPwdRequest (
            String oldPassword,
            String newPassword
    ) {}

    @GetMapping
    public ResponseEntity<?> getMyUserInfo() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            User user = userService.getById(userId);
            if (user == null) throw new IllegalArgumentException("User not found");
            return ResponseEntity.ok(new UserInfo(user.getUserName(), user.getEmail()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<?> editPassword(@RequestBody EditPwdRequest request) throws ResponseStatusException {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            return ResponseEntity.ok(authService.editPassword(request.oldPassword, request.newPassword, userId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

}
