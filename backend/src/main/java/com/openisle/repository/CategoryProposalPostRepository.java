package com.openisle.repository;

import com.openisle.model.CategoryProposalPost;
import com.openisle.model.CategoryProposalStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CategoryProposalPostRepository extends JpaRepository<CategoryProposalPost, Long> {
    List<CategoryProposalPost> findByEndTimeAfterAndProposalStatus(LocalDateTime now, CategoryProposalStatus status);
    List<CategoryProposalPost> findByEndTimeBeforeAndProposalStatus(LocalDateTime now, CategoryProposalStatus status);
    boolean existsByProposedSlug(String proposedSlug);
}



