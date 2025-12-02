package bg.energo.phoenix.model.enums.receivable.reconnectionOfThePowerSupply;

import lombok.Getter;

@Getter
public enum TableSearchBy {
    CUSTOMER_IDENTIFIER("CUSTOMER_IDENTIFIER"),
    CUSTOMER_NUMBER("CUSTOMER_NUMBER"),
    POD_IDENTIFIER("POD_IDENTIFIER"),
    DISCONNECTION_REQUEST_NUMBER("DISCONNECTION_REQUEST_NUMBER"),
    ALL("ALL");

    private final String value;

    private TableSearchBy(String value) {
        this.value=value;
    }
}
