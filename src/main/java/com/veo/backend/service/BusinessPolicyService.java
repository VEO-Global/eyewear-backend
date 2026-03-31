package com.veo.backend.service;

import com.veo.backend.dto.request.BusinessPolicyUpdateRequest;
import com.veo.backend.dto.response.BusinessPolicyResponse;
import com.veo.backend.enums.BusinessPolicyType;

import java.util.List;

public interface BusinessPolicyService {
    List<BusinessPolicyResponse> getAllPolicies();

    BusinessPolicyResponse getPolicyByType(BusinessPolicyType type);

    BusinessPolicyResponse updatePolicy(BusinessPolicyType type, BusinessPolicyUpdateRequest request, String updatedBy);
}
