package com.openisle.controller;

import com.openisle.model.PasswordStrength;
import com.openisle.model.PublishMode;
import com.openisle.service.PasswordValidator;
import com.openisle.service.PostService;
import com.openisle.service.AiUsageService;
import com.openisle.service.RegisterModeService;
import com.openisle.model.RegisterMode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {
    private final PostService postService;
    private final PasswordValidator passwordValidator;
    private final AiUsageService aiUsageService;
    private final RegisterModeService registerModeService;

    @GetMapping
    public ConfigDto getConfig() {
        ConfigDto dto = new ConfigDto();
        dto.setPublishMode(postService.getPublishMode());
        dto.setPasswordStrength(passwordValidator.getStrength());
        dto.setAiFormatLimit(aiUsageService.getFormatLimit());
        dto.setRegisterMode(registerModeService.getRegisterMode());
        return dto;
    }

    @PostMapping
    public ConfigDto updateConfig(@RequestBody ConfigDto dto) {
        if (dto.getPublishMode() != null) {
            postService.setPublishMode(dto.getPublishMode());
        }
        if (dto.getPasswordStrength() != null) {
            passwordValidator.setStrength(dto.getPasswordStrength());
        }
        if (dto.getAiFormatLimit() != null) {
            aiUsageService.setFormatLimit(dto.getAiFormatLimit());
        }
        if (dto.getRegisterMode() != null) {
            registerModeService.setRegisterMode(dto.getRegisterMode());
        }
        return getConfig();
    }

    @Data
    public static class ConfigDto {
        private PublishMode publishMode;
        private PasswordStrength passwordStrength;
        private Integer aiFormatLimit;
        private RegisterMode registerMode;
    }
}
