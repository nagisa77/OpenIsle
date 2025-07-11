package com.openisle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.openisle.model.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    java.util.List<User> findByUsernameContainingIgnoreCase(String keyword);
    java.util.List<User> findByRole(com.openisle.model.Role role);
}
