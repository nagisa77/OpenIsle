package com.openisle.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Daily count of AI markdown formatting usage for a user. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ai_format_usage",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "use_date"}))
public class AiFormatUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "use_date", nullable = false)
    private LocalDate useDate;

    @Column(nullable = false)
    private int count;
}
