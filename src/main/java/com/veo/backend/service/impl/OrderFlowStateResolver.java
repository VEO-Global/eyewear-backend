package com.veo.backend.service.impl;

import com.veo.backend.entity.Order;
import com.veo.backend.entity.Prescription;
import com.veo.backend.enums.OrderStatus;
import com.veo.backend.enums.PrescriptionOption;
import com.veo.backend.enums.PrescriptionReviewStatus;
import com.veo.backend.enums.StaffOrderPhase;

final class OrderFlowStateResolver {
    private OrderFlowStateResolver() {
    }

    static StaffOrderPhase resolvePhase(Order order, Prescription prescription) {
        if (order == null) {
            return StaffOrderPhase.PENDING_CONFIRMATION;
        }

        if (requiresPrescription(order, prescription) && !isPrescriptionApproved(order, prescription)) {
            return StaffOrderPhase.PRESCRIPTION_REVIEW;
        }

        if (order.getStatus() == null) {
            return StaffOrderPhase.PENDING_CONFIRMATION;
        }

        return switch (order.getStatus()) {
            case PENDING_PAYMENT, WAITING_FOR_STOCK -> StaffOrderPhase.PENDING_CONFIRMATION;
            case PENDING_VERIFICATION -> isReadyToDeliver(order, prescription)
                    ? StaffOrderPhase.READY_TO_DELIVER
                    : StaffOrderPhase.PENDING_CONFIRMATION;
            case MANUFACTURING, PACKING -> StaffOrderPhase.PROCESSING;
            case READY_TO_SHIP -> StaffOrderPhase.READY_TO_DELIVER;
            case SHIPPING -> StaffOrderPhase.SHIPPING;
            case COMPLETED -> StaffOrderPhase.COMPLETED;
            case CANCELLED -> StaffOrderPhase.CANCELED;
        };
    }

    static String resolvePhaseLabel(Order order, Prescription prescription) {
        return switch (resolvePhase(order, prescription)) {
            case PENDING_CONFIRMATION -> "Pending confirmation";
            case PRESCRIPTION_REVIEW -> "Prescription review";
            case PROCESSING -> "Processing";
            case READY_TO_DELIVER -> "Ready to deliver";
            case SHIPPING -> "Shipping";
            case COMPLETED -> "Completed";
            case CANCELED -> "Canceled";
            case RETURN_REFUND -> "Return / refund";
        };
    }

    static boolean requiresPrescription(Order order, Prescription prescription) {
        return order != null
                && (order.getPrescriptionOption() == PrescriptionOption.WITH_PRESCRIPTION || prescription != null);
    }

    static boolean isReadyToDeliver(Order order, Prescription prescription) {
        if (order == null || order.getStatus() != OrderStatus.PENDING_VERIFICATION) {
            return false;
        }

        if (!requiresPrescription(order, prescription)) {
            return true;
        }

        return isPrescriptionApproved(order, prescription);
    }

    static PrescriptionReviewStatus resolveEffectiveReviewStatus(Order order, Prescription prescription) {
        if (!requiresPrescription(order, prescription)) {
            return null;
        }

        if (prescription == null) {
            return PrescriptionReviewStatus.PENDING;
        }

        if (prescription.getReviewStatus() != null) {
            return prescription.getReviewStatus();
        }

        if (prescription.getVerifiedBy() != null || prescription.getVerifiedAt() != null) {
            return PrescriptionReviewStatus.APPROVED;
        }

        return PrescriptionReviewStatus.PENDING;
    }

    private static boolean isPrescriptionApproved(Order order, Prescription prescription) {
        return requiresPrescription(order, prescription)
                && resolveEffectiveReviewStatus(order, prescription) == PrescriptionReviewStatus.APPROVED;
    }
}
