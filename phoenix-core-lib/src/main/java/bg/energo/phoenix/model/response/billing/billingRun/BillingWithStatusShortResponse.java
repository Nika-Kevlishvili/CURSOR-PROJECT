package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;

public interface BillingWithStatusShortResponse {
    Long getId();

    BillingStatus getStatus();
}
