package com.veo.backend.repository;

import com.veo.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findUserById(Long id);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role.name IN :roleNames")
    List<User> findByRoleNames(@Param("roleNames") List<String> roleNames);
}