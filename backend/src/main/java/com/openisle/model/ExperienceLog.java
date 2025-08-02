package com.openisle.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Daily experience gain counts for a user. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "experience_logs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "log_date"}))
public class ExperienceLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "post_count", nullable = false)
    private int postCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "reaction_count", nullable = false)
    private int reactionCount;
}
