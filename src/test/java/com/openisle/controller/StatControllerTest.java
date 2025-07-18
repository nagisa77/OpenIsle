package com.openisle.controller;

import com.openisle.config.CustomAccessDeniedHandler;
import com.openisle.config.SecurityConfig;
import com.openisle.service.JwtService;
import com.openisle.repository.UserRepository;
import com.openisle.service.UserVisitService;
import com.openisle.model.Role;
import com.openisle.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, CustomAccessDeniedHandler.class})
class StatControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private UserVisitService userVisitService;

    @Test
    void dauReturnsCount() throws Exception {
        Mockito.when(jwtService.validateAndGetSubject("token")).thenReturn("user");
        User user = new User();
        user.setUsername("user");
        user.setPassword("p");
        user.setEmail("u@example.com");
        user.setRole(Role.USER);
        Mockito.when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        Mockito.when(userVisitService.countDau(Mockito.any())).thenReturn(3L);

        mockMvc.perform(get("/api/stats/dau").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dau").value(3));
    }

    @Test
    void dauRangeReturnsSeries() throws Exception {
        Mockito.when(jwtService.validateAndGetSubject("token")).thenReturn("user");
        User user = new User();
        user.setUsername("user");
        user.setPassword("p");
        user.setEmail("u@example.com");
        user.setRole(Role.USER);
        Mockito.when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        java.util.Map<java.time.LocalDate, Long> map = new java.util.LinkedHashMap<>();
        map.put(java.time.LocalDate.now().minusDays(1), 1L);
        map.put(java.time.LocalDate.now(), 2L);
        Mockito.when(userVisitService.countDauRange(Mockito.any(), Mockito.any())).thenReturn(map);

        mockMvc.perform(get("/api/stats/dau-range").param("days", "2").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value(1))
                .andExpect(jsonPath("$[1].value").value(2));
    }
}
