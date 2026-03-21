package com.veo.backend.repository;

import com.veo.backend.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    Optional<UserAddress> findFirstByUserIdOrderByIsDefaultDescIdAsc(Long userId);
}
