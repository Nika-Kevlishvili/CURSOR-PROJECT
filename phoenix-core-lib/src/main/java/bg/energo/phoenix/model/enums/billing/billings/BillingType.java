package bg.energo.phoenix.model.enums.billing.billings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BillingType {
    STANDARD_BILLING,
    MANUAL_INVOICE,
    MANUAL_CREDIT_OR_DEBIT_NOTE,
    INVOICE_CORRECTION,
    MANUAL_INTERIM_AND_ADVANCE_PAYMENT,
    INVOICE_REVERSAL
}
