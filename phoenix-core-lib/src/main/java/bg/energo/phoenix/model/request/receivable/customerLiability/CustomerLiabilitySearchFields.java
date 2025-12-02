package bg.energo.phoenix.model.request.receivable.customerLiability;

import lombok.Getter;

public enum CustomerLiabilitySearchFields {
    ALL("ALL"),
    ID("ID"),
    CUSTOMER("CUSTOMER"),
    BILLINGGROUP("BILLINGGROUP");

    @Getter
    private String value;

    CustomerLiabilitySearchFields(String value) {
        this.value = value;
    }
}
