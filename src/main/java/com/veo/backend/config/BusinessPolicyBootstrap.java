package com.veo.backend.config;

import com.veo.backend.entity.BusinessPolicy;
import com.veo.backend.enums.BusinessPolicyType;
import com.veo.backend.repository.BusinessPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessPolicyBootstrap {
    private final BusinessPolicyRepository businessPolicyRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaultPolicies() {
        boolean hasChanges = false;

        for (BusinessPolicyType type : BusinessPolicyType.values()) {
            if (businessPolicyRepository.findByType(type).isPresent()) {
                continue;
            }

            businessPolicyRepository.save(BusinessPolicy.builder()
                    .type(type)
                    .title(defaultTitle(type))
                    .content(defaultContent(type))
                    .isActive(true)
                    .updatedBy("system")
                    .build());
            hasChanges = true;
        }

        if (hasChanges) {
            log.info("Seeded default business policies for types: {}", Arrays.toString(BusinessPolicyType.values()));
        }
    }

    public static String defaultTitle(BusinessPolicyType type) {
        return switch (type) {
            case PURCHASE -> "Purchase Policy";
            case RETURN -> "Return Policy";
            case WARRANTY -> "Warranty Policy";
            case SHIPPING -> "Shipping Policy";
            case PRIVACY -> "Privacy Policy";
        };
    }

    public static String defaultContent(BusinessPolicyType type) {
        return switch (type) {
            case PURCHASE -> """
                    <p>Customers may purchase products through the official VEO channels.</p>
                    <p>Orders are confirmed after successful payment or staff verification.</p>
                    """;
            case RETURN -> """
                    <p>Customers can request returns within the allowed policy window for eligible products.</p>
                    <p>Returned items must be unused and include the original accessories.</p>
                    """;
            case WARRANTY -> """
                    <p>Warranty applies to manufacturing defects under the published warranty term.</p>
                    <p>Damage caused by misuse or accidental impact is not covered.</p>
                    """;
            case SHIPPING -> """
                    <p>Shipping fees and delivery time depend on destination and selected fulfillment method.</p>
                    <p>Free shipping promotions are applied according to active campaigns.</p>
                    """;
            case PRIVACY -> """
                    <p>Customer information is collected only for order fulfillment and service improvement.</p>
                    <p>Personal data is protected and not shared outside permitted operational purposes.</p>
                    """;
        };
    }
}
