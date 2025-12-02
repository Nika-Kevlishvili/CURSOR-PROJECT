package bg.energo.phoenix.model.enums.billing.billings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BillingApplicationLevel {
    CUSTOMER,
    CONTRACT,
    POD
}
