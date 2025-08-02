package com.openisle.repository;

import com.openisle.model.User;
import com.openisle.model.UserVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UserVisitRepository extends JpaRepository<UserVisit, Long> {
    Optional<UserVisit> findByUserAndVisitDate(User user, LocalDate visitDate);
    long countByUser(User user);
    long countByVisitDate(LocalDate visitDate);

    @Query("SELECT uv.visitDate AS d, COUNT(uv) AS c FROM UserVisit uv WHERE uv.visitDate BETWEEN :start AND :end GROUP BY uv.visitDate ORDER BY uv.visitDate")
    java.util.List<Object[]> countRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
