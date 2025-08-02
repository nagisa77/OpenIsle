package com.openisle.controller;

import com.openisle.model.User;
import com.openisle.service.*;
import com.openisle.model.RegisterMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private EmailSender emailService;
    @MockBean
    private CaptchaService captchaService;
    @MockBean
    private GoogleAuthService googleAuthService;
    @MockBean
    private RegisterModeService registerModeService;

    @Test
    void registerSendsEmail() throws Exception {
        User user = new User();
        user.setEmail("a@b.com");
        user.setUsername("u");
        user.setVerificationCode("123456");
        Mockito.when(registerModeService.getRegisterMode()).thenReturn(RegisterMode.DIRECT);
        Mockito.when(userService.register(eq("u"), eq("a@b.com"), eq("p"), any(), eq(RegisterMode.DIRECT))).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"email\":\"a@b.com\",\"password\":\"p\",\"reason\":\"test reason more than twenty\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        Mockito.verify(emailService).sendEmail(eq("a@b.com"), any(), any());
    }

    @Test
    void verifyCodeEndpoint() throws Exception {
        Mockito.when(userService.verifyCode("u", "123")).thenReturn(true);

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"code\":\"123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verified"));
    }

    @Test
    void loginReturnsToken() throws Exception {
        User user = new User();
        user.setUsername("u");
        Mockito.when(userService.findByUsername("u")).thenReturn(Optional.of(user));
        Mockito.when(userService.matchesPassword(user, "p")).thenReturn(true);
        Mockito.when(jwtService.generateToken("u")).thenReturn("token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"password\":\"p\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"));
    }

    @Test
    void loginFails() throws Exception {
        Mockito.when(userService.findByUsername("u")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"password\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason_code").value("INVALID_CREDENTIALS"));
    }
}
