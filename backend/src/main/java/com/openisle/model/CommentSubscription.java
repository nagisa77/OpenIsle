package com.openisle.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Subscription to a comment for replies notifications. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "comment_subscriptions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "comment_id"}))
public class CommentSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id")
    private Comment comment;
}
