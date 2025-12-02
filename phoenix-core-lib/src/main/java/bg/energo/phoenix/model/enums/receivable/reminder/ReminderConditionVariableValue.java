package bg.energo.phoenix.model.enums.receivable.reminder;

public enum ReminderConditionVariableValue {
    //Contract
    PRODUCT_CONTRACT,
    SERVICE_CONTRACT,
    //Customer type
    LEGAL_ENTITY,
    PRIVATE_CUSTOMER,
    //Pod type
    CONSUMER,
    GENERATOR,
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
    NO,
}
