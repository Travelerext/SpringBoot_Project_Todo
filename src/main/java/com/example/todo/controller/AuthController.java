package com.example.todo.controller;

import com.example.todo.security.AuthService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    public record RegisterRequest(
            @Email(message = "Invalid email format")
            String email,
            String userName,
            String password,
            @Pattern(regexp = "^[0-9]{6}$", message = "Invalid verification code")
            String inputCode
    ) {}

    public record ResetPasswordRequest(
            @Email(message = "Invalid email format")
            String email,
            String password,
            @Pattern(regexp = "^[0-9]{6}$", message = "Invalid verification code")
            String inputCode
    ) {}

    public record LoginRequest(
            String email,
            String password
    ) {}

    public record RefreshTokenRequest(String refreshToken) {}

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestParam String email) {
        try {
            authService.sendVerifyCode(email);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getReason());
        } catch (MailException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("发送邮件失败");
        }
    }

    @PostMapping("/send-reset-code")
    public ResponseEntity<?> sendResetPwdCode(@RequestParam String email) {
        try {
            authService.sendResetPwdVerifyCode(email);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getReason());
        } catch (MailException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("发送邮件失败");
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPwd(request.inputCode, request.password, request.email);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getReason());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(
                    authService.register(request.userName, request.email, request.password, request.inputCode)
            );
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getReason());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(
                    authService.login(request.email, request.password)
            );
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getReason());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        try {
            return ResponseEntity.ok(
                    authService.refresh(request.refreshToken)
            );
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getReason());
        }
    }

}
