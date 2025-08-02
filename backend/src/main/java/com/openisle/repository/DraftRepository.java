package com.openisle.repository;

import com.openisle.model.Draft;
import com.openisle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DraftRepository extends JpaRepository<Draft, Long> {
    Optional<Draft> findByAuthor(User author);
    void deleteByAuthor(User author);
}
