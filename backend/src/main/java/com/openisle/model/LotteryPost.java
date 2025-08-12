package com.openisle.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "lottery_posts")
@Getter
@Setter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "post_id")
public class LotteryPost extends Post {

    @Column
    private String prizeDescription;

    @Column
    private String prizeIcon;

    @Column(nullable = false)
    private int prizeCount;

    @Column
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @ManyToMany
    @JoinTable(name = "lottery_participants",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> participants = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "lottery_winners",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> winners = new HashSet<>();
}
