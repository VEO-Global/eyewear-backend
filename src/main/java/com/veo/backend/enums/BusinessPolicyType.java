package com.veo.backend.enums;

public enum BusinessPolicyType {
    PURCHASE,
    RETURN,
    WARRANTY,
    SHIPPING,
    PRIVACY;

    public String getConfigKey() {
        return "business.policy." + name().toLowerCase();
    }
}
