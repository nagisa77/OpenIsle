package com.openisle.service;

import com.openisle.model.AiFormatUsage;
import com.openisle.model.User;
import com.openisle.repository.AiFormatUsageRepository;
import com.openisle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AiUsageService {
    private final AiFormatUsageRepository usageRepository;
    private final UserRepository userRepository;

    @Value("${app.ai.format-limit:3}")
    private int formatLimit;

    public int getFormatLimit() {
        return formatLimit;
    }

    public void setFormatLimit(int formatLimit) {
        this.formatLimit = formatLimit;
    }

    public int incrementAndGetCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        LocalDate today = LocalDate.now();
        AiFormatUsage usage = usageRepository.findByUserAndUseDate(user, today)
                .orElseGet(() -> {
                    AiFormatUsage u = new AiFormatUsage();
                    u.setUser(user);
                    u.setUseDate(today);
                    u.setCount(0);
                    return u;
                });
        usage.setCount(usage.getCount() + 1);
        usageRepository.save(usage);
        return usage.getCount();
    }

    public int getCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        return usageRepository.findByUserAndUseDate(user, LocalDate.now())
                .map(AiFormatUsage::getCount)
                .orElse(0);
    }
}
