package bg.energo.phoenix.model.enums.product.price.priceComponent;

public enum PriceComponentConditionVariableValue {
    // customer type
    LEGAL_ENTITY,
    PRIVATE_CUSTOMER,
    // pod voltage level
    LOW,
    MEDIUM,
    MEDIUM_DIRECT_CONNECTED,
    HIGH,
    // pod measurement type
    SETTLEMENT_PERIOD,
    SLP,
    // contract type
    COMBINED,
    SUPPLY_BALANCING,
    SUPPLY_ONLY,
    WITHOUT_SUPPLY,
    // general
    YES,
    NO,
    //PURPOSE_OF_CONSUMPTION pod
    HOUSEHOLD,
    NON_HOUSEHOLD
}
