package bg.energo.phoenix.model.enums.customer.list;

import lombok.Getter;

@Getter
public enum CustomerRelatedReceivableSortColumns {
    ID("id"),
    BILLING_GROUP("billingGroup"),
    ALTERNATIVE_RECIPIENT_OF_AN_INVOICE("alternativeRecipient"),
    INITIAL_AMOUNT("initialAmount"),
    CURRENT_AMOUNT("currentAmount");

    private final String value;

    private CustomerRelatedReceivableSortColumns(String value) {
        this.value=value;
    }
}
