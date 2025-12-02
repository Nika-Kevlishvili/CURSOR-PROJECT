package bg.energo.phoenix.model.request.receivable.disconnectionOfPowerSupply.listing;

import lombok.Getter;

public enum DisconnectionPowerSupplyListingListColumns {
    NUMBER("id"),
    DATE("createDate"),
    STATUS("disconnectionStatus"),
    REQUEST_FOR_DISCONNECTION_POWER_SUPPLY("requestForDisconnectionNumber"),
    NUMBER_OF_PODS("numberOfPods");

    @Getter
    private final String value;

    DisconnectionPowerSupplyListingListColumns(String disconnectionPowerSupplyListingColumn) {
        this.value = disconnectionPowerSupplyListingColumn;
    }
}
