package com.openisle.controller;

import com.openisle.model.User;
import com.openisle.service.EmailSender;
import com.openisle.service.JwtService;
import com.openisle.service.UserService;
import com.openisle.service.CaptchaService;
import com.openisle.service.GoogleAuthService;
import com.openisle.service.GithubAuthService;
import com.openisle.service.DiscordAuthService;
import com.openisle.service.TwitterAuthService;
import com.openisle.service.RegisterModeService;
import com.openisle.service.NotificationService;
import com.openisle.model.RegisterMode;
import com.openisle.repository.UserRepository;
import com.openisle.exception.FieldException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;
    private final EmailSender emailService;
    private final CaptchaService captchaService;
    private final GoogleAuthService googleAuthService;
    private final GithubAuthService githubAuthService;
    private final DiscordAuthService discordAuthService;
    private final TwitterAuthService twitterAuthService;
    private final RegisterModeService registerModeService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;


    @Value("${app.captcha.enabled:false}")
    private boolean captchaEnabled;

    @Value("${app.captcha.register-enabled:false}")
    private boolean registerCaptchaEnabled;

    @Value("${app.captcha.login-enabled:false}")
    private boolean loginCaptchaEnabled;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (captchaEnabled && registerCaptchaEnabled && !captchaService.verify(req.getCaptcha())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid captcha"));
        }
        User user = userService.register(
                req.getUsername(), req.getEmail(), req.getPassword(), "", registerModeService.getRegisterMode());
        emailService.sendEmail(user.getEmail(), "在网站填写验证码以验证", "您的验证码是 " + user.getVerificationCode());
        if (!user.isApproved()) {
            notificationService.createRegisterRequestNotifications(user, user.getRegisterReason());
        }
        return ResponseEntity.ok(Map.of("message", "Verification code sent"));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyRequest req) {
        boolean ok = userService.verifyCode(req.getUsername(), req.getCode());
        if (ok) {
            return ResponseEntity.ok(Map.of(
                    "message", "Verified",
                    "token", jwtService.generateReasonToken(req.getUsername())
            ));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid verification code"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (captchaEnabled && loginCaptchaEnabled && !captchaService.verify(req.getCaptcha())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid captcha"));
        }
        Optional<User> userOpt = userService.findByUsername(req.getUsername());
        if (userOpt.isEmpty()) {
            userOpt = userService.findByEmail(req.getUsername());
        }
        if (userOpt.isEmpty() || !userService.matchesPassword(userOpt.get(), req.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid credentials",
                    "reason_code", "INVALID_CREDENTIALS"));
        }
        User user = userOpt.get();
        if (!user.isVerified()) {
            user = userService.register(user.getUsername(), user.getEmail(), user.getPassword(), user.getRegisterReason(), registerModeService.getRegisterMode());
            emailService.sendEmail(user.getEmail(), "在网站填写验证码以验证", "您的验证码是 " + user.getVerificationCode());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "User not verified",
                    "reason_code", "NOT_VERIFIED",
                    "user_name", user.getUsername()));
        }
        if (RegisterMode.WHITELIST.equals(registerModeService.getRegisterMode()) && !user.isApproved()) {
            if (user.getRegisterReason() != null && !user.getRegisterReason().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Account awaiting approval",
                        "reason_code", "IS_APPROVING"
                ));
            }
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Register reason not approved",
                    "reason_code", "NOT_APPROVED",
                    "token", jwtService.generateReasonToken(user.getUsername())));
        }
        return ResponseEntity.ok(Map.of("token", jwtService.generateToken(user.getUsername())));
    }

    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody GoogleLoginRequest req) {
        Optional<User> user = googleAuthService.authenticate(req.getIdToken(), registerModeService.getRegisterMode());
        if (user.isPresent()) {
            if (RegisterMode.DIRECT.equals(registerModeService.getRegisterMode())) {
                return ResponseEntity.ok(Map.of("token", jwtService.generateToken(user.get().getUsername())));
            }
            if (!user.get().isApproved()) {
                if (user.get().getRegisterReason() != null && !user.get().getRegisterReason().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Account awaiting approval",
                            "reason_code", "IS_APPROVING",
                            "token", jwtService.generateReasonToken(user.get().getUsername())
                    ));
                }
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Account awaiting approval",
                        "reason_code", "NOT_APPROVED",
                        "token", jwtService.generateReasonToken(user.get().getUsername())
                ));
            }

            return ResponseEntity.ok(Map.of("token", jwtService.generateToken(user.get().getUsername())));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid google token",
                "reason_code", "INVALID_CREDENTIALS"
        ));
    }


    @PostMapping("/reason")
    public ResponseEntity<?> reason(@RequestBody MakeReasonRequest req) {
        String username = jwtService.validateAndGetSubjectForReason(req.getToken());
        Optional<User> userOpt = userService.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid token, Please re-login",
                    "reason_code", "INVALID_CREDENTIALS"
            ));
        }

        if (req.reason == null || req.reason.length() <= 20) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Reason's length must longer than 20",
                    "reason_code", "INVALID_CREDENTIALS"
            ));
        }

        User user = userOpt.get();
        if (user.isApproved() || registerModeService.getRegisterMode() == RegisterMode.DIRECT) {
            return ResponseEntity.ok().body(Map.of("valid", true));
        }

        user = userService.updateReason(user.getUsername(), req.getReason());
        notificationService.createRegisterRequestNotifications(user, req.getReason());
        return ResponseEntity.ok().body(Map.of("valid", true));
    }

    @PostMapping("/github")
    public ResponseEntity<?> loginWithGithub(@RequestBody GithubLoginRequest req) {
        Optional<User> user = githubAuthService.authenticate(req.getCode(), registerModeService.getRegisterMode(), req.getRedirectUri());
        if (user.isPresent()) {
            if (RegisterMode.DIRECT.equals(registerModeService.getRegisterMode())) {
                return ResponseEntity.ok(Map.of("token", jwtService.generateToken(user.get().getUsername())));
            }
            if (!user.get().isApproved()) {
                if (user.get().getRegisterReason() != null && !user.get().getRegisterReason().isEmpty()) {
                    // 已填写注册理由
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Account awaiting approval",
                            "reason_code", "IS_APPROVING",
                            "token", jwtService.generateReasonToken(user.get().getUsername())
                    ));
                }
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Account awaiting approval",
                        "reason_code", "NOT_APPROVED",
                        "token", jwtService.generateReasonToken(user.get().getUsername())
                ));
            }

            return ResponseEntity.ok(Map.of("token", jwtService.generateToken(user.get().getUsername())));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid github code",
                "reason_code", "INVALID_CREDENTIALS"
        ));
    }

    @PostMapping("/discord")
    public ResponseEntity<?> loginWithDiscord(@RequestBody DiscordLoginRequest req) {
        Optional<User> user = discordAuthService.authenticate(req.getCode(), registerModeService.getRegisterMode(), req.getRedirectUri());
        if (user.isPresent()) {
            if (RegisterMode.DIRECT.equals(registerModeService.getRegisterMode())) {
                return ResponseEntity.ok(Map.of("token", jwtService.generateToken(user.get().getUsername())));
            }
            if (!user.get().isApproved()) {
                if (user.get().getRegisterReason() != null && !user.get().getRegisterReason().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Account awaiting approval",
                            "reason_code", "IS_APPROVING",
                            "token", jwtService.generateReasonToken(user.get().getUsername())
                    ));
                }
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Account awaiting approval",
                        "reason_code", "NOT_APPROVED",
                        "token", jwtService.generateReasonToken(user.get().getUsername())
                ));
            }

            return ResponseEntity.ok(Map.of("token", jwtService.generateToken(user.get().getUsername())));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid discord code",
                "reason_code", "INVALID_CREDENTIALS"
        ));
    }
      
    @PostMapping("/twitter")
    public ResponseEntity<?> loginWithTwitter(@RequestBody TwitterLoginRequest req) {
        Optional<User> user = twitterAuthService.authenticate(
                req.getCode(),
                req.getCodeVerifier(),
                registerModeService.getRegisterMode(),
                req.getRedirectUri());
        if (user.isPresent()) {
            if (RegisterMode.DIRECT.equals(registerModeService.getRegisterMode())) {
                return ResponseEntity.ok(Map.of("token", jwtService.generateToken(user.get().getUsername())));
            }
            if (!user.get().isApproved()) {
                if (user.get().getRegisterReason() != null && !user.get().getRegisterReason().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Account awaiting approval",
                            "reason_code", "IS_APPROVING",
                            "token", jwtService.generateReasonToken(user.get().getUsername())
                    ));
                }
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Account awaiting approval",
                        "reason_code", "NOT_APPROVED",
                        "token", jwtService.generateReasonToken(user.get().getUsername())
                ));
            }

            return ResponseEntity.ok(Map.of("token", jwtService.generateToken(user.get().getUsername())));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid twitter code",
                "reason_code", "INVALID_CREDENTIALS"
        ));
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkToken() {
        return ResponseEntity.ok(Map.of("valid", true));
    }

    @PostMapping("/forgot/send")
    public ResponseEntity<?> sendReset(@RequestBody ForgotPasswordRequest req) {
        Optional<User> userOpt = userService.findByEmail(req.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        String code = userService.generatePasswordResetCode(req.getEmail());
        emailService.sendEmail(req.getEmail(), "请填写验证码以重置密码", "您的验证码是" + code);
        return ResponseEntity.ok(Map.of("message", "Verification code sent"));
    }

    @PostMapping("/forgot/verify")
    public ResponseEntity<?> verifyReset(@RequestBody VerifyForgotRequest req) {
        boolean ok = userService.verifyPasswordResetCode(req.getEmail(), req.getCode());
        if (ok) {
            String username = userService.findByEmail(req.getEmail()).get().getUsername();
            return ResponseEntity.ok(Map.of("token", jwtService.generateResetToken(username)));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid verification code"));
    }

    @PostMapping("/forgot/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        String username = jwtService.validateAndGetSubjectForReset(req.getToken());
        try {
            userService.updatePassword(username, req.getPassword());
            return ResponseEntity.ok(Map.of("message", "Password updated"));
        } catch (FieldException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "field", e.getField(),
                    "error", e.getMessage()
            ));
        }
    }

    @Data
    private static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String captcha;
    }

    @Data
    private static class LoginRequest {
        private String username;
        private String password;
        private String captcha;
    }

    @Data
    private static class GoogleLoginRequest {
        private String idToken;
    }

    @Data
    private static class GithubLoginRequest {
        private String code;
        private String redirectUri;
    }

    @Data
    private static class DiscordLoginRequest {
        private String code;
        private String redirectUri;
    }
  
    @Data
    private static class TwitterLoginRequest {
        private String code;
        private String redirectUri;
        private String codeVerifier;
    }

    @Data
    private static class VerifyRequest {
        private String username;
        private String code;
    }

    @Data
    private static class MakeReasonRequest {
        private String token;
        private String reason;
    }

    @Data
    private static class ForgotPasswordRequest {
        private String email;
    }

    @Data
    private static class VerifyForgotRequest {
        private String email;
        private String code;
    }

    @Data
    private static class ResetPasswordRequest {
        private String token;
        private String password;
    }
}
