package com.openisle.service;

import com.openisle.model.PushSubscription;
import com.openisle.model.User;
import com.openisle.repository.PushSubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;

@Slf4j
@Service
public class PushNotificationService {
    private final PushSubscriptionRepository subscriptionRepository;
    private final PushService pushService;

    public PushNotificationService(PushSubscriptionRepository subscriptionRepository,
                                   @Value("${app.webpush.public-key}") String publicKey,
                                   @Value("${app.webpush.private-key}") String privateKey) throws GeneralSecurityException {
        this.subscriptionRepository = subscriptionRepository;
        Security.addProvider(new BouncyCastleProvider());
        this.pushService = new PushService(publicKey, privateKey);
    }

    public void sendNotification(User user, String payload) {
        List<PushSubscription> subs = subscriptionRepository.findByUser(user);
        for (PushSubscription sub : subs) {
            try {
                Notification notification = new Notification(sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), payload);
                pushService.send(notification);
            } catch (GeneralSecurityException | IOException | JoseException | InterruptedException | java.util.concurrent.ExecutionException e) {
                log.error(e.getMessage());
            }
        }
    }
}
