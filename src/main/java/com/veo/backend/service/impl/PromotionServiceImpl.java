package com.veo.backend.service.impl;

import com.veo.backend.dto.request.PromotionRequest;
import com.veo.backend.dto.response.PromotionResponse;
import com.veo.backend.entity.Voucher;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.VoucherRepository;
import com.veo.backend.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {
    private final VoucherRepository voucherRepository;

    @Override
    public List<PromotionResponse> getAllPromotions() {
        return voucherRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public PromotionResponse getPromotionById(Long id) {
        return mapToResponse(findVoucher(id));
    }

    @Override
    @Transactional
    public PromotionResponse createPromotion(PromotionRequest request) {
        voucherRepository.findByCodeIgnoreCase(request.getCode().trim())
                .ifPresent(voucher -> {
                    throw new AppException(ErrorCode.CONFLICT, "Promotion code already exists");
                });

        Voucher voucher = new Voucher();
        applyRequest(voucher, request);
        return mapToResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotion(Long id, PromotionRequest request) {
        Voucher voucher = findVoucher(id);

        voucherRepository.findByCodeIgnoreCase(request.getCode().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.CONFLICT, "Promotion code already exists");
                });

        applyRequest(voucher, request);
        return mapToResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public void deletePromotion(Long id) {
        Voucher voucher = findVoucher(id);
        voucher.setActive(false);
        voucherRepository.save(voucher);
    }

    @Override
    public PromotionResponse validateCode(String code) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_ERROR, "Promotion code is invalid"));

        if (!isValidNow(voucher)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Promotion code is expired or inactive");
        }

        return mapToResponse(voucher);
    }

    private Voucher findVoucher(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_ERROR, "Promotion not found"));
    }

    private void applyRequest(Voucher voucher, PromotionRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "End date must be after start date");
        }

        voucher.setCode(request.getCode().trim().toUpperCase());
        voucher.setDiscountPercent(request.getDiscountPercent());
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setQuantity(request.getQuantity());
        voucher.setActive(request.getIsActive() != null ? request.getIsActive() : true);
    }

    private PromotionResponse mapToResponse(Voucher voucher) {
        return PromotionResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .discountPercent(voucher.getDiscountPercent())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .quantity(voucher.getQuantity())
                .isActive(voucher.isActive())
                .validNow(isValidNow(voucher))
                .build();
    }

    private boolean isValidNow(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        return voucher.isActive()
                && voucher.getQuantity() != null
                && voucher.getQuantity() > 0
                && voucher.getStartDate() != null
                && voucher.getEndDate() != null
                && !now.isBefore(voucher.getStartDate())
                && !now.isAfter(voucher.getEndDate());
    }
}
