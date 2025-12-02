package bg.energo.phoenix.model.request.receivable.disconnectionOfPowerSupply;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DisconnectionOfPowerSupplySearchFields {

    ALL("ALL"),
    CUSTOMER_IDENTIFIER("CUSTOMER_IDENTIFIER"),
    CUSTOMER_NUMBER("CUSTOMER_NUMBER"),
    POD_IDENTIFIER("POD_IDENTIFIER");

    private final String value;
}
