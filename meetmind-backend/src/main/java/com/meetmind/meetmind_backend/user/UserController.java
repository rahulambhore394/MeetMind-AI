package com.meetmind.meetmind_backend.user;

import com.meetmind.meetmind_backend.user.dto.UserProfileResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return new UserProfileResponse(user);
    }

    @PutMapping("/me")
    public UserProfileResponse updateCurrentUser(
            @RequestBody UserProfileResponse request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail());
        }
        User saved = userRepository.save(user);
        return new UserProfileResponse(saved);
    }
}