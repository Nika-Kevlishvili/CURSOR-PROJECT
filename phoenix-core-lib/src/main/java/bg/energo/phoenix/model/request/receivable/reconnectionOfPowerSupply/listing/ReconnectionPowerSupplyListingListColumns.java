package bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply.listing;

import lombok.Getter;

public enum ReconnectionPowerSupplyListingListColumns {
    NUMBER("id"),
    DATE("createDate"),
    STATUS("reconnectionStatus"),
    GRID_OPERATOR("gridOperator"),
    REQUEST_FOR_DISCONNECTION_POWER_SUPPLY("powerSupplyDisconnectionRequestNumber"),
    NUMBER_OF_PODS("numberOfPods");

    @Getter
    private final String value;

    ReconnectionPowerSupplyListingListColumns(String reconnectionPowerSupplyListingColumn) {
        this.value = reconnectionPowerSupplyListingColumn;
    }
}
