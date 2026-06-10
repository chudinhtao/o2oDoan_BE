package com.fnb.auth.repository;

import com.fnb.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    java.util.List<User> findAllByRoleIn(java.util.List<String> roles);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.role IN :roles AND (cast(:keyword as string) IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', cast(:keyword as string), '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', cast(:keyword as string), '%')))")
    org.springframework.data.domain.Page<User> searchStaff(
            @org.springframework.data.repository.query.Param("roles") java.util.List<String> roles,
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable);
}
