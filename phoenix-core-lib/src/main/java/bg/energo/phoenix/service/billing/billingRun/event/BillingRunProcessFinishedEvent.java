package bg.energo.phoenix.service.billing.billingRun.event;

import bg.energo.phoenix.service.billing.billingRun.actions.AbstractBillingRunActionService;

public record BillingRunProcessFinishedEvent(
        AbstractBillingRunActionService billingRunActionService,
        boolean inNewThread,
        Long billingRunId,
        boolean mustCheckPermission
) {}
