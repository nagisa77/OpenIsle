package com.openisle.service;

import com.openisle.model.User;
import com.openisle.model.Role;
import com.openisle.service.PasswordValidator;
import com.openisle.service.UsernameValidator;
import com.openisle.service.AvatarGenerator;
import com.openisle.exception.FieldException;
import com.openisle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordValidator passwordValidator;
    private final UsernameValidator usernameValidator;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ImageUploader imageUploader;
    private final AvatarGenerator avatarGenerator;

    public User register(String username, String email, String password, String reason, com.openisle.model.RegisterMode mode) {
        usernameValidator.validate(username);
        passwordValidator.validate(password);
        // ── 先按用户名查 ──────────────────────────────────────────
        Optional<User> byUsername = userRepository.findByUsername(username);
        if (byUsername.isPresent()) {
            User u = byUsername.get();
            if (u.isVerified()) {                           // 已验证 → 直接拒绝
                throw new FieldException("username", "User name already exists");
            }
            // 未验证 → 允许“重注册”：覆盖必要字段并重新发验证码
            u.setEmail(email);                              // 若不允许改邮箱可去掉
            u.setPassword(passwordEncoder.encode(password));
            u.setVerificationCode(genCode());
            u.setRegisterReason(reason);
            u.setApproved(mode == com.openisle.model.RegisterMode.DIRECT);
            return userRepository.save(u);
        }

        // ── 再按邮箱查 ───────────────────────────────────────────
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User u = byEmail.get();
            if (u.isVerified()) {                           // 已验证 → 直接拒绝
                throw new FieldException("email", "User email already exists");
            }
            // 未验证 → 允许“重注册”
            u.setUsername(username);                        // 若不允许改用户名可去掉
            u.setPassword(passwordEncoder.encode(password));
            u.setVerificationCode(genCode());
            u.setRegisterReason(reason);
            u.setApproved(mode == com.openisle.model.RegisterMode.DIRECT);
            return userRepository.save(u);
        }

        // ── 完全新用户 ───────────────────────────────────────────
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.USER);
        user.setVerified(false);
        user.setVerificationCode(genCode());
        user.setAvatar(avatarGenerator.generate(username));
        user.setRegisterReason(reason);
        user.setApproved(mode == com.openisle.model.RegisterMode.DIRECT);
        return userRepository.save(user);
    }

    private String genCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    public boolean verifyCode(String username, String code) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && code.equals(userOpt.get().getVerificationCode())) {
            User user = userOpt.get();
            user.setVerified(true);
            user.setVerificationCode(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public Optional<User> authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(User::isVerified)
                .filter(User::isApproved)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()));
    }

    public boolean matchesPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByIdentifier(String identifier) {
        if (identifier.matches("\\d+")) {
            return userRepository.findById(Long.parseLong(identifier));
        }
        return userRepository.findByUsername(identifier);
    }

    public User updateAvatar(String username, String avatarUrl) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        String old = user.getAvatar();
        user.setAvatar(avatarUrl);
        User saved = userRepository.save(user);
        if (old != null && !old.equals(avatarUrl)) {
            imageUploader.removeReferences(java.util.Set.of(old));
        }
        if (avatarUrl != null) {
            imageUploader.addReferences(java.util.Set.of(avatarUrl));
        }
        return saved;
    }

    public User updateReason(String username, String reason) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        user.setRegisterReason(reason);
        return userRepository.save(user);
    }

    public User updateProfile(String currentUsername, String newUsername, String introduction) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        if (newUsername != null && !newUsername.equals(currentUsername)) {
            usernameValidator.validate(newUsername);
            userRepository.findByUsername(newUsername).ifPresent(u -> {
                throw new FieldException("username", "User name already exists");
            });
            user.setUsername(newUsername);
        }
        if (introduction != null) {
            user.setIntroduction(introduction);
        }
        return userRepository.save(user);
    }

    public String generatePasswordResetCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        String code = genCode();
        user.setPasswordResetCode(code);
        userRepository.save(user);
        return code;
    }

    public boolean verifyPasswordResetCode(String email, String code) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent() && code.equals(userOpt.get().getPasswordResetCode())) {
            User user = userOpt.get();
            user.setPasswordResetCode(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public User updatePassword(String username, String newPassword) {
        passwordValidator.validate(newPassword);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    /**
     * Get all administrator accounts.
     */
    public java.util.List<User> getAdmins() {
        return userRepository.findByRole(Role.ADMIN);
    }
}
