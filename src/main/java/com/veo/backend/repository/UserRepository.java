package com.veo.backend.repository;

import com.veo.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findUserById(Long id);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role.name IN :roleNames")
    List<User> findByRoleNames(@Param("roleNames") List<String> roleNames);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.role.name IN :roleNames
              AND (:role IS NULL OR u.role.name = :role)
              AND (:active IS NULL OR u.isActive = :active)
              AND (
                    :keyword IS NULL
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            """)
    Page<User> searchUsersByRoles(
            @Param("roleNames") List<String> roleNames,
            @Param("role") String role,
            @Param("active") Boolean active,
            @Param("keyword") String keyword,
            Pageable pageable);
}
