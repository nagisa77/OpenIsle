package com.openisle.config;

import com.openisle.model.Activity;
import com.openisle.model.ActivityType;
import com.openisle.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ActivityInitializer implements CommandLineRunner {
    private final ActivityRepository activityRepository;

    @Override
    public void run(String... args) {
        if (activityRepository.findByType(ActivityType.MILK_TEA) == null) {
            Activity a = new Activity();
            a.setTitle("🎡建站送奶茶活动");
            a.setType(ActivityType.MILK_TEA);
            a.setIcon("https://icons.veryicon.com/png/o/food--drinks/delicious-food-1/coffee-36.png");
            a.setContent("为了有利于建站推广以及激励发布内容，我们推出了建站送奶茶的活动，前50名达到level 1的用户，可以联系站长获取奶茶/咖啡一杯");
            activityRepository.save(a);
        }

        if (activityRepository.findByType(ActivityType.INVITE_POINTS) == null) {
            Activity a = new Activity();
            a.setTitle("🎁邀请码送积分活动");
            a.setType(ActivityType.INVITE_POINTS);
            a.setIcon("https://icons.veryicon.com/png/o/commerce-shopping/two-color-icon-library/gift-30.png");
            a.setContent("活动期间，邀请好友注册可获得积分奖励，快来参与吧！");
            a.setEndTime(LocalDateTime.of(2025, 10, 1, 0, 0));
            activityRepository.save(a);
        }
    }
}
