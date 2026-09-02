package com.meetmind.meetmind_backend.auth;




import com.meetmind.meetmind_backend.auth.dto.LoginRequest;
import com.meetmind.meetmind_backend.auth.dto.LoginResponse;
import com.meetmind.meetmind_backend.auth.dto.RegisterRequest;
import com.meetmind.meetmind_backend.auth.dto.RegisterResponse;
import com.meetmind.meetmind_backend.user.User;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        User user =
                authService.register(request);

        RegisterResponse response =
                new RegisterResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        LoginResponse response =
                authService.login(request);

        return ResponseEntity.ok(response);
    }
}