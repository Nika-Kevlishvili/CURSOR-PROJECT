package bg.energo.phoenix.service.billing.billingRun.actions;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;

public interface BillingRunActionInvoker {
    void invoke(BillingRun billingRun);
}
