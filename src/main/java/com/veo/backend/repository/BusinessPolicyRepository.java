package com.veo.backend.repository;

import com.veo.backend.entity.BusinessPolicy;
import com.veo.backend.enums.BusinessPolicyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessPolicyRepository extends JpaRepository<BusinessPolicy, Long> {
    Optional<BusinessPolicy> findByType(BusinessPolicyType type);
}
