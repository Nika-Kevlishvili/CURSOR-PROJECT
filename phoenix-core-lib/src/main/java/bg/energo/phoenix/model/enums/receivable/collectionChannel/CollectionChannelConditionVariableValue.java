package bg.energo.phoenix.model.enums.receivable.collectionChannel;

import lombok.Getter;

@Getter
public enum CollectionChannelConditionVariableValue {
    // reminder: don't include any "OR" or "AND" in the names, parser is getting confused
    //Contract
    PRODUCT_CONTRACT,
    SERVICE_CONTRACT,
    //Customer type
    LEGAL_ENTITY,
    PRIVATE_CUSTOMER,
    //Pod type
    CONSUMER,
    GENERATR,
    // contract type
    COMBINED,
    SUPPLY_BALANCING,
    SUPPLY_ONLY,
    WITHOUT_SUPPLY,
    // pod measurement type
    SETTLEMENT_PERIOD,
    SLP,
    // pod voltage level
    LOW,
    MEDIUM,
    MEDIUM_DIRECTLY_CONNECTED,
    HIGH,
    //PurposeOfConsumption pod
    HOUSEHOLD,
    NON_HOUSEHOLD,
    //InterimAdvancePayment
    YES,
    NO
}
