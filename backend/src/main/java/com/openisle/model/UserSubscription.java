package com.openisle.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Following relationship between users. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_subscriptions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"subscriber_id", "target_id"}))
public class UserSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscriber_id")
    private User subscriber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id")
    private User target;
}
