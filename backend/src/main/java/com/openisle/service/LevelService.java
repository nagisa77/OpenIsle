package com.openisle.service;

import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import com.openisle.repository.ExperienceLogRepository;
import com.openisle.model.ExperienceLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LevelService {
    private final UserRepository userRepository;
    // repositories for experience-related entities
    private final ExperienceLogRepository experienceLogRepository;
    private final UserVisitService userVisitService;

    private static final int[] LEVEL_EXP = {100,200,300,600,1200,10000};

    private ExperienceLog getTodayLog(User user) {
        LocalDate today = LocalDate.now();
        return experienceLogRepository.findByUserAndLogDate(user, today)
                .orElseGet(() -> {
                    ExperienceLog log = new ExperienceLog();
                    log.setUser(user);
                    log.setLogDate(today);
                    log.setPostCount(0);
                    log.setCommentCount(0);
                    log.setReactionCount(0);
                    return experienceLogRepository.save(log);
                });
    }

    private int addExperience(User user, int amount) {
        user.setExperience(user.getExperience() + amount);
        userRepository.save(user);
        return amount;
    }

    public int awardForPost(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        ExperienceLog log = getTodayLog(user);
        if (log.getPostCount() > 1) return 0;
        log.setPostCount(log.getPostCount() + 1);
        experienceLogRepository.save(log);
        return addExperience(user,30);
    }

    public int awardForComment(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        ExperienceLog log = getTodayLog(user);
        if (log.getCommentCount() > 3) return 0;
        log.setCommentCount(log.getCommentCount() + 1);
        experienceLogRepository.save(log);
        return addExperience(user,10);
    }

    public int awardForReaction(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        ExperienceLog log = getTodayLog(user);
        if (log.getReactionCount() > 3) return 0;
        log.setReactionCount(log.getReactionCount() + 1);
        experienceLogRepository.save(log);
        return addExperience(user,5);
    }

    public int awardForSignin(String username) {
        boolean first = userVisitService.recordVisit(username);
        if (!first) return 0;
        User user = userRepository.findByUsername(username).orElseThrow();
        return addExperience(user,5);
    }

    public int getLevel(int exp) {
        int level = 0;
        for (int t : LEVEL_EXP) {
            if (exp >= t) level++; else break;
        }
        return level;
    }

    public int nextLevelExp(int exp) {
        for (int t : LEVEL_EXP) {
            if (exp < t) return t;
        }
        return LEVEL_EXP[LEVEL_EXP.length-1];
    }
}
