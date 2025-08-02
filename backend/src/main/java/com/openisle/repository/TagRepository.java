package com.openisle.repository;

import com.openisle.model.Tag;
import com.openisle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByNameContainingIgnoreCase(String keyword);
    List<Tag> findByApproved(boolean approved);
    List<Tag> findByApprovedTrue();
    List<Tag> findByNameContainingIgnoreCaseAndApprovedTrue(String keyword);

    List<Tag> findByCreatorOrderByCreatedAtDesc(User creator, Pageable pageable);
    List<Tag> findByCreator(User creator);
}
