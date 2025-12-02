package bg.energo.phoenix.model.enums.product.service.list;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ServiceSearchField {
    ALL("ALL"),
    NAME("NAME"),
    SERVICE_GROUP_NAME("SERVICE_GROUP_NAME"),
    PENALTIES_GROUP_NAME("PENALTIES_GROUP_NAME"),
    PENALTY_NAME("PENALTY_NAME"),
    TERMINATIONS_GROUP_NAME("TERMINATIONS_GROUP_NAME"),
    TERMINATION_NAME("TERMINATION_NAME"),
    TERMS_GROUP_NAME("TERMS_GROUP_NAME"),
    TERMS_NAME("TERMS_NAME"),
    IAP_GROUP_NAME("IAP_GROUP_NAME"),
    IAP_NAME("IAP_NAME"),
    PRICE_COMPONENTS_GROUP_NAME("PRICE_COMPONENTS_GROUP_NAME"),
    PRICE_COMPONENT_NAME("PRICE_COMPONENT_NAME");

    private final String value;
}