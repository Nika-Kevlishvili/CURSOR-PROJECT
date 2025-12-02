package phoenix.core.customer.model.enums.customer.unwantedCustomer;

import lombok.Getter;

public enum UnwantedCustomerSortField {
    IDENTIFIER("identifier"), NAME("name"), REASON("ucr.name"), DATE_OF_CREATION("createDate");

    @Getter
    private String value;

    UnwantedCustomerSortField(String value) {
        this.value = value;
    }
}
