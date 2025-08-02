package com.openisle.controller;

import com.openisle.model.User;
import com.openisle.service.EmailSender;
import com.openisle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserRepository userRepository;
    private final EmailSender emailSender;
    @Value("${app.website-url}")
    private String websiteUrl;

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setApproved(true);
        userRepository.save(user);
        emailSender.sendEmail(user.getEmail(), "您的注册已审核通过",
                "🎉您的注册已经审核通过, 点击以访问网站: " + websiteUrl);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setApproved(false);
        userRepository.save(user);
        emailSender.sendEmail(user.getEmail(), "您的注册已被管理员拒绝",
                "您的注册被管理员拒绝, 点击链接可以重新填写理由申请: " + websiteUrl);
        return ResponseEntity.ok().build();
    }
}
