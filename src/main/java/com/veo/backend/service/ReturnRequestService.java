package com.veo.backend.service;

import com.veo.backend.dto.request.ReturnRequestCreateRequest;
import com.veo.backend.dto.response.PagedResponse;
import com.veo.backend.dto.response.ReturnRequestSummaryResponse;

public interface ReturnRequestService {
    ReturnRequestSummaryResponse createRequest(String email, ReturnRequestCreateRequest request);

    PagedResponse<ReturnRequestSummaryResponse> getMyRequests(String email, int page, int size);

    ReturnRequestSummaryResponse getMyRequestDetail(String email, Long id);
}
