package bg.energo.phoenix.model.enums.customer.unwantedCustomer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum UnwantedCustomerSortField {
    ID("id"),
    IDENTIFIER("identifier"),
    NAME("name"),
    REASON("ucr.name"),
    DATE_OF_CREATION("createDate");

    @Getter
    private final String value;
}
