package bg.energo.phoenix.model.enums.customer.list;

import lombok.Getter;

@Getter
public enum CustomerRelatedReceivableSearchField {
    ALL("ALL"),
    ID("ID"),
    BILLINGGROUP("BILLINGGROUP");

    private final String value;

    private CustomerRelatedReceivableSearchField(String value) {
        this.value = value;
    }
}
