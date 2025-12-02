package bg.energo.phoenix.model.enums.billing.billings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BillingCriteria {
    ALL_CUSTOMERS,
    CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS,
    LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS
}
