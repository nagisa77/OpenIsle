package com.openisle.service;

import com.openisle.model.PushSubscription;
import com.openisle.model.User;
import com.openisle.repository.PushSubscriptionRepository;
import com.openisle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PushSubscriptionService {
    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveSubscription(String username, String endpoint, String p256dh, String auth) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        subscriptionRepository.deleteByUserAndEndpoint(user, endpoint);
        PushSubscription sub = new PushSubscription();
        sub.setUser(user);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        subscriptionRepository.save(sub);
    }

    public List<PushSubscription> listByUser(User user) {
        return subscriptionRepository.findByUser(user);
    }
}
