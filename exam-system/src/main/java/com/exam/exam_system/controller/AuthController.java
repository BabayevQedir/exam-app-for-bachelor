package com.exam.exam_system.controller;


import com.exam.exam_system.config.JwtService;
import com.exam.exam_system.model.User;
import com.exam.exam_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            User user = (User) authentication.getPrincipal();
            String token = jwtService.generateToken(user);
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", user.getUsername(),
                    "fullName", user.getFullName() != null ? user.getFullName() : "",
                    "role", user.getRole().name()
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "İstifadəçi adı və ya şifrə yanlışdır"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bu istifadəçi adı artıq mövcuddur"));
        }
        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(User.Role.valueOf(request.role().toUpperCase()))
                .enabled(true)
                .build();
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "İstifadəçi yaradıldı", "username", user.getUsername()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "role", user.getRole().name()
        ));
    }

    public record LoginRequest(String username, String password) {}
    public record RegisterRequest(String username, String password, String fullName, String role) {}
}