package com.veo.backend.service.impl;

import com.veo.backend.dto.request.BusinessPolicyUpdateRequest;
import com.veo.backend.dto.response.BusinessPolicyResponse;
import com.veo.backend.config.BusinessPolicyBootstrap;
import com.veo.backend.entity.BusinessPolicy;
import com.veo.backend.enums.BusinessPolicyType;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.BusinessPolicyRepository;
import com.veo.backend.service.BusinessPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessPolicyServiceImpl implements BusinessPolicyService {
    private final BusinessPolicyRepository businessPolicyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BusinessPolicyResponse> getAllPolicies() {
        return Arrays.stream(BusinessPolicyType.values())
                .map(this::getPolicyByType)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessPolicyResponse getPolicyByType(BusinessPolicyType type) {
        BusinessPolicy policy = businessPolicyRepository.findByType(type)
                .orElseGet(() -> createDefaultPolicy(type));
        return mapToResponse(policy);
    }

    @Override
    @Transactional
    public BusinessPolicyResponse updatePolicy(BusinessPolicyType type, BusinessPolicyUpdateRequest request, String updatedBy) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Policy content must not be empty");
        }

        BusinessPolicy policy = businessPolicyRepository.findByType(type)
                .orElseGet(() -> createDefaultPolicy(type));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            policy.setTitle(request.getTitle().trim());
        }
        policy.setContent(request.getContent().trim());
        policy.setIsActive(true);
        policy.setUpdatedBy(trimToNull(updatedBy));

        return mapToResponse(businessPolicyRepository.save(policy));
    }

    private BusinessPolicyResponse mapToResponse(BusinessPolicy policy) {
        return BusinessPolicyResponse.builder()
                .type(policy.getType())
                .key(policy.getType().getConfigKey())
                .title(policy.getTitle())
                .content(policy.getContent())
                .description(policy.getTitle())
                .isActive(policy.getIsActive())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .updatedBy(policy.getUpdatedBy())
                .build();
    }

    private BusinessPolicy createDefaultPolicy(BusinessPolicyType type) {
        BusinessPolicy policy = BusinessPolicy.builder()
                .type(type)
                .title(BusinessPolicyBootstrap.defaultTitle(type))
                .content(BusinessPolicyBootstrap.defaultContent(type))
                .isActive(true)
                .updatedBy("system")
                .build();
        return businessPolicyRepository.save(policy);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
