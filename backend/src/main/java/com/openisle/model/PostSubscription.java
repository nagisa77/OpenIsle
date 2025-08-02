package com.openisle.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Subscription to a post for update notifications. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "post_subscriptions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}))
public class PostSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private Post post;
}
