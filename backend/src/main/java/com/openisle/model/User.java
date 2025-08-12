package com.openisle.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple user entity with basic fields and a role.
 */

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean verified = false;

    private String verificationCode;

    private String passwordResetCode;

    private String avatar;

    @Column(nullable = false)
    private int experience = 0;

    @Column(nullable = false)
    private int point = 0;

    @Column(length = 1000)
    private String introduction;

    @Column(length = 1000)
    private String registerReason;

    @Column(nullable = false)
    private boolean approved = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    private MedalType displayMedal;

    @ElementCollection(targetClass = NotificationType.class)
    @CollectionTable(name = "user_disabled_notification_types", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "notification_type")
    @Enumerated(EnumType.STRING)
    private Set<NotificationType> disabledNotificationTypes = EnumSet.of(
            NotificationType.POST_VIEWED,
            NotificationType.USER_ACTIVITY
    );

    @CreationTimestamp
    @Column(nullable = false, updatable = false,
            columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime createdAt;
}
