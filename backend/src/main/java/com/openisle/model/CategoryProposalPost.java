package com.openisle.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A specialized post type used for proposing new categories.
 * It reuses poll mechanics (participants, votes, endTime) by extending PollPost.
 */
@Entity
@Table(name = "category_proposal_posts", indexes = {
        @Index(name = "idx_category_proposal_posts_status", columnList = "status"),
        @Index(name = "idx_category_proposal_posts_slug", columnList = "proposed_slug", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "post_id")
public class CategoryProposalPost extends PollPost {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CategoryProposalStatus proposalStatus = CategoryProposalStatus.PENDING;

    @Column(name = "proposed_name", nullable = false)
    private String proposedName;

    @Column(name = "proposed_slug", nullable = false, unique = true)
    private String proposedSlug;

    @Column(name = "description")
    private String description;

    // Approval threshold as percentage (0-100), default 60
    @Column(name = "approve_threshold", nullable = false)
    private int approveThreshold = 60;

    // Minimum number of participants required to meet quorum
    @Column(name = "quorum", nullable = false)
    private int quorum = 10;

    // Optional voting start time (end time inherited from PollPost)
    @Column(name = "start_at")
    private LocalDateTime startAt;

    // Snapshot of poll results at finalization (e.g., JSON)
    @Column(name = "result_snapshot", columnDefinition = "TEXT")
    private String resultSnapshot;

    // Reason when proposal is rejected
    @Column(name = "reject_reason")
    private String rejectReason;
}


