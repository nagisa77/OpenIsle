package com.openisle.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.openisle.config.CustomAccessDeniedHandler;
import com.openisle.config.SecurityConfig;
import com.openisle.service.JwtService;
import com.openisle.repository.UserRepository;
import com.openisle.service.UserVisitService;
import com.openisle.model.Role;
import com.openisle.model.User;
import java.util.Optional;
import org.mockito.Mockito;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, CustomAccessDeniedHandler.class})
class AdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private UserVisitService userVisitService;

    @Test
    void adminHelloReturnsMessage() throws Exception {
        Mockito.when(jwtService.validateAndGetSubject("adminToken")).thenReturn("admin");
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword("p");
        admin.setEmail("a@b.com");
        admin.setRole(Role.ADMIN);
        Mockito.when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        mockMvc.perform(get("/api/admin/hello").header("Authorization", "Bearer adminToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, Admin User"));
    }

    @Test
    void adminHelloMissingToken() throws Exception {
        mockMvc.perform(get("/api/admin/hello"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Missing token"));
    }

    @Test
    void adminHelloInvalidToken() throws Exception {
        Mockito.when(jwtService.validateAndGetSubject("bad")).thenThrow(new RuntimeException());

        mockMvc.perform(get("/api/admin/hello").header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid or expired token"));
    }

    @Test
    void adminHelloNotAdmin() throws Exception {
        Mockito.when(jwtService.validateAndGetSubject("userToken")).thenReturn("user");
        User user = new User();
        user.setUsername("user");
        user.setPassword("p");
        user.setEmail("u@example.com");
        user.setRole(Role.USER);
        Mockito.when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/admin/hello").header("Authorization", "Bearer userToken"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
