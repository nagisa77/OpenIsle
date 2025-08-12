package com.openisle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.openisle.model.User;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    java.util.List<User> findByUsernameContainingIgnoreCase(String keyword);
    java.util.List<User> findByRole(com.openisle.model.Role role);
    long countByExperienceGreaterThanEqual(int experience);
    long countByCreatedAtBefore(LocalDateTime createdAt);

    @Query("SELECT FUNCTION('date', u.createdAt) AS d, COUNT(u) AS c FROM User u " +
           "WHERE u.createdAt >= :start AND u.createdAt < :end GROUP BY d ORDER BY d")
    java.util.List<Object[]> countDailyRange(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);
}
