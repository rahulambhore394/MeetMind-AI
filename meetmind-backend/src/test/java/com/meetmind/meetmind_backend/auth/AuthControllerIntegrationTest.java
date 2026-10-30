package com.meetmind.meetmind_backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetmind.meetmind_backend.auth.dto.LoginRequest;
import com.meetmind.meetmind_backend.auth.dto.LoginResponse;
import com.meetmind.meetmind_backend.auth.dto.RegisterRequest;
import com.meetmind.meetmind_backend.chat.TestRedisConfig;
import com.meetmind.meetmind_backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.meetmind.meetmind_backend.event.KafkaEventPublisher kafkaEventPublisher;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void register_ValidRequest_ReturnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice Tester");
        request.setEmail("alice@meetmind.com");
        request.setPassword("StrongPassword123!");

        User mockUser = new User();
        mockUser.setId(101L);
        mockUser.setName("Alice Tester");
        mockUser.setEmail("alice@meetmind.com");

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockUser);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101L))
                .andExpect(jsonPath("$.name").value("Alice Tester"))
                .andExpect(jsonPath("$.email").value("alice@meetmind.com"));
    }

    @Test
    void register_InvalidEmail_ReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice Tester");
        request.setEmail("not-an-email");
        request.setPassword("123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ValidCredentials_ReturnsOk() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@meetmind.com");
        request.setPassword("StrongPassword123!");

        LoginResponse mockResponse = new LoginResponse("mock-access-token", 101L, "alice@meetmind.com", "Alice Tester");
        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.userId").value(101L))
                .andExpect(jsonPath("$.email").value("Alice Tester"));
    }

    @Test
    void login_EmptyPassword_ReturnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@meetmind.com");
        request.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
