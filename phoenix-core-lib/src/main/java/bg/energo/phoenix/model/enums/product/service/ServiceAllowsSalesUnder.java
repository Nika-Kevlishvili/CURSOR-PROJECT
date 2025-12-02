package bg.energo.phoenix.model.enums.product.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ServiceAllowsSalesUnder {
    CONCLUDED_CONTRACT("Concluded Contract"),
    ACTIVATED_CONTRACT("Activated Contract");

    private final String description;
}
