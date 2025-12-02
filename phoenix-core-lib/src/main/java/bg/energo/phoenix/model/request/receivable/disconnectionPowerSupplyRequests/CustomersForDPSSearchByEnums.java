package bg.energo.phoenix.model.request.receivable.disconnectionPowerSupplyRequests;

import lombok.Getter;

public enum CustomersForDPSSearchByEnums {
    ALL("ALL"),
    CUSTOMER_IDENTIFIER("CUSTOMER_IDENTIFIER"),
    CUSTOMER_NUMBER("CUSTOMER_NUMBER"),
    CONTRACT_NUMBER("CONTRACT_NUMBER"),
    BILLING_GROUP_NUMBER("BILLING_GROUP_NUMBER"),
    POD_IDENTIFIER("POD_IDENTIFIER"),
    LIABILITY_NUMBER("LIABILITY_NUMBER"),
    OUTGOING_DOCUMENT_NUMBER("OUTGOING_DOCUMENT_NUMBER");

    @Getter
    private final String value;

    CustomersForDPSSearchByEnums(String value) {
        this.value = value;
    }
}
