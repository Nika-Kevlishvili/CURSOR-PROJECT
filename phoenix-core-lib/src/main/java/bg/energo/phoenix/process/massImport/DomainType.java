package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DomainType {
    CUSTOMERS("customers"),
    UNWANTED_CUSTOMERS("unwanted-customers"),
    PRODUCTS("products"),
    PODS("pods"),
    METERS("meters"),
    SUPPLY_AUTOMATIC_ACTIVATIONS("supply-automatic-activations"),
    SUPPLY_AUTOMATIC_DEACTIVATIONS("supply-automatic-deactivations"),
    SUPPLY_ACTION_DEACTIVATIONS("supply-action-deactivations"),
    PRODUCT_CONTRACTS("product-contracts"),
    SERVICE_CONTRACT("service-contract"),
    CUSTOMER_LIABILITY("customer-liability"),
    CUSTOMER_RECEIVABLE("customer-receivable"),
    PAYMENT("payment"),
    GOVERNMENT_COMPENSATION("government-compensation");

    private final String value;

    public static DomainType fromValue(String value) {
        return Arrays
                .stream(DomainType.values())
                .filter(v -> v.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new ClientException(ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
    }
}
