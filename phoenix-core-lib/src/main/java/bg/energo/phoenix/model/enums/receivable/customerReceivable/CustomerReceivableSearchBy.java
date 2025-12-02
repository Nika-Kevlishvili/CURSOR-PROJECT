package bg.energo.phoenix.model.enums.receivable.customerReceivable;

import lombok.Getter;

@Getter
public enum CustomerReceivableSearchBy {
    ALL("ALL"),
    ID("ID"),
    CUSTOMER("CUSTOMER"),
    BILLINGGROUP("BILLINGGROUP");

    private final String value;

    private CustomerReceivableSearchBy(String value) {
        this.value = value;
    }
}
