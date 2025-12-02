package bg.energo.phoenix.model.request.receivable.disconnectionPowerSupplyRequests;

import lombok.Getter;

public enum CustomersForDPSListColumns {
    CUSTOMER("customer"),
    CONTRACT("contracts"),
    ALTERNATIVE_RECIPIENT_INVOICE("alt_recipient_inv_customer"),
    BILLING_GROUP("billing_groups"),
    POD("podId"),
    POD_WITH_HIGHEST_CONSUMPTION("is_highest_consumption"),
    LIABILITY_IN_BILLING_GROUP("liabilities_in_billing_group"),
    LIABILITY_IN_POD("liabilities_in_pod"),
    EXISTING_CUSTOMER_RECEIVABLES("existingCustomerReceivables");

    @Getter
    private final String value;

    CustomersForDPSListColumns(String customersForDPSColumn) {
        this.value = customersForDPSColumn;
    }
}
