package com.openisle.controller;

import com.openisle.model.Activity;
import com.openisle.model.ActivityType;
import com.openisle.model.User;
import com.openisle.service.ActivityService;
import com.openisle.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {
    private final ActivityService activityService;
    private final UserService userService;

    @GetMapping
    public List<Activity> list() {
        return activityService.list();
    }

    @GetMapping("/milk-tea")
    public MilkTeaInfo milkTea() {
        Activity a = activityService.getByType(ActivityType.MILK_TEA);
        long count = activityService.countParticipants(a);
        if (!a.isEnded() && count >= 50) {
            activityService.end(a);
        }
        MilkTeaInfo info = new MilkTeaInfo();
        info.setRedeemCount(count);
        info.setEnded(a.isEnded());
        return info;
    }

    @PostMapping("/milk-tea/redeem")
    public java.util.Map<String, String> redeemMilkTea(@RequestBody RedeemRequest req, Authentication auth) {
        User user = userService.findByIdentifier(auth.getName()).orElseThrow();
        Activity a = activityService.getByType(ActivityType.MILK_TEA);
        boolean first = activityService.redeem(a, user, req.getContact());
        if (first) {
            return java.util.Map.of("message", "redeemed");
        }
        return java.util.Map.of("message", "updated");
    }

    @Data
    private static class MilkTeaInfo {
        private long redeemCount;
        private boolean ended;
    }

    @Data
    private static class RedeemRequest {
        private String contact;
    }
}
