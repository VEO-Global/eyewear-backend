package com.veo.backend.service.impl;

import com.veo.backend.dto.request.PrescriptionReviewRequest;
import com.veo.backend.dto.response.LensSummaryResponse;
import com.veo.backend.dto.response.PrescriptionResponse;
import com.veo.backend.dto.response.PrescriptionReviewResponse;
import com.veo.backend.entity.LensProduct;
import com.veo.backend.entity.Order;
import com.veo.backend.entity.Prescription;
import com.veo.backend.entity.User;
import com.veo.backend.enums.OrderStatus;
import com.veo.backend.enums.PrescriptionReviewStatus;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.OrderRepository;
import com.veo.backend.repository.PrescriptionRepository;
import com.veo.backend.repository.UserRepository;
import com.veo.backend.service.PrescriptionReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionReviewServiceImpl implements PrescriptionReviewService {
    private final PrescriptionRepository prescriptionRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionReviewResponse> getPendingPrescriptions() {
        return prescriptionRepository.findPendingForStaff()
                .stream()
                .map(this::mapResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionReviewResponse getOrderPrescription(Long orderId) {
        Prescription prescription = prescriptionRepository.findFirstByOrderIdOrderByIdDesc(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Prescription not found for order"));
        return mapResponse(prescription);
    }

    @Override
    @Transactional
    public PrescriptionReviewResponse reviewPrescription(String reviewerEmail, Long prescriptionId, PrescriptionReviewRequest request) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Prescription not found"));
        User reviewer = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Reviewer not found"));

        prescription.setReviewStatus(request.getReviewStatus());
        prescription.setReviewNote(request.getReviewNote());
        prescription.setVerifiedBy(reviewer);
        prescription.setVerifiedAt(LocalDateTime.now());

        Order order = prescription.getOrder();
        if (order != null) {
            order.setStatus(request.getReviewStatus() == PrescriptionReviewStatus.APPROVED
                    ? OrderStatus.PENDING_VERIFICATION
                    : OrderStatus.PENDING_VERIFICATION);
            order.setUpdatedAt(LocalDateTime.now());
        }

        prescriptionRepository.save(prescription);
        return mapResponse(prescription);
    }

    private PrescriptionReviewResponse mapResponse(Prescription prescription) {
        Order order = prescription.getOrder();
        return PrescriptionReviewResponse.builder()
                .prescriptionId(prescription.getId())
                .orderId(order != null ? order.getId() : null)
                .orderCode(order != null ? order.getOrderCode() : null)
                .customerName(order != null && order.getUser() != null ? order.getUser().getFullName() : null)
                .customerEmail(order != null && order.getUser() != null ? order.getUser().getEmail() : null)
                .receiverName(order != null ? order.getReceiverName() : null)
                .phoneNumber(order != null ? order.getPhoneNumber() : null)
                .lens(mapLensResponse(prescription))
                .prescription(mapPrescriptionResponse(prescription))
                .reviewStatus(resolveEffectiveReviewStatus(prescription))
                .reviewNote(prescription.getReviewNote())
                .reviewedBy(prescription.getVerifiedBy() != null ? prescription.getVerifiedBy().getFullName() : null)
                .reviewedAt(prescription.getVerifiedAt())
                .createdAt(prescription.getCreatedAt())
                .build();
    }

    private LensSummaryResponse mapLensResponse(Prescription prescription) {
        LensProduct lens = prescription.getLensProduct();
        if (lens == null && prescription.getLensNameSnapshot() == null) {
            return null;
        }

        return LensSummaryResponse.builder()
                .id(lens != null ? lens.getId() : null)
                .name(prescription.getLensNameSnapshot() != null ? prescription.getLensNameSnapshot() : lens.getName())
                .price(prescription.getLensPriceSnapshot() != null ? prescription.getLensPriceSnapshot() : lens.getPrice())
                .description(prescription.getLensDescriptionSnapshot() != null ? prescription.getLensDescriptionSnapshot() : lens.getDescription())
                .build();
    }

    private PrescriptionResponse mapPrescriptionResponse(Prescription prescription) {
        return PrescriptionResponse.builder()
                .prescriptionImageUrl(prescription.getPrescriptionImageUrl())
                .sphereOd(prescription.getSphereOd())
                .sphereOs(prescription.getSphereOs())
                .cylinderOd(prescription.getCylinderOd())
                .cylinderOs(prescription.getCylinderOs())
                .axisOd(prescription.getAxisOd())
                .axisOs(prescription.getAxisOs())
                .pd(prescription.getPd())
                .reviewStatus(resolveEffectiveReviewStatus(prescription))
                .reviewNote(prescription.getReviewNote())
                .build();
    }

    private PrescriptionReviewStatus resolveEffectiveReviewStatus(Prescription prescription) {
        if (prescription == null) {
            return null;
        }

        if (prescription.getReviewStatus() != null) {
            return prescription.getReviewStatus();
        }

        if (prescription.getVerifiedBy() != null || prescription.getVerifiedAt() != null) {
            return PrescriptionReviewStatus.APPROVED;
        }

        return PrescriptionReviewStatus.PENDING;
    }
}
