package com.openisle.config;

import com.openisle.model.Activity;
import com.openisle.model.ActivityType;
import com.openisle.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
    }
}
