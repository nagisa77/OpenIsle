package com.openisle.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Daily visit record for a user. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_visits",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "visit_date"}))
public class UserVisit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;
}
