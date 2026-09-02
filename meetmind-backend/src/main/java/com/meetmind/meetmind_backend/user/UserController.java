package com.meetmind.meetmind_backend.user;


import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public String getCurrentUser(
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        return "Logged in as: " + user.getEmail();
    }
}