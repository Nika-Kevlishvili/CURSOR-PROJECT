package bg.energo.phoenix.model.enums.billing.billings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BillingStatus {
    INITIAL,
    IN_PROGRESS_DRAFT,
    DRAFT,
    IN_PROGRESS_GENERATION,
    GENERATED,
    IN_PROGRESS_ACCOUNTING,
    COMPLETED,
    DELETED,
    PAUSED,
    CANCELLED
}
