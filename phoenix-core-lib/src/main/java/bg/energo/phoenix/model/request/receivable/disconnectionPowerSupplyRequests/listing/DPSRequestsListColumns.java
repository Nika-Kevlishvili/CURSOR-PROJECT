package bg.energo.phoenix.model.request.receivable.disconnectionPowerSupplyRequests.listing;

import lombok.Getter;

public enum DPSRequestsListColumns {
    NUMBER("id"),
    DATE("createdate"),
    STATUS("disconnectionrequeststatus"),
    TYPE("suppliertype"),
    GRID_OPERATOR("gridoperator"),
    DATE_OF_REGISTRATION_OF_THE_REQUEST_TO_THE_GRID_OPERATOR("gridoperatorrequestregistrationdate"),
    DATE_OD_REMINDER("customerreminderlettersentdate"),
    DATE_TO_PAY_THE_GRID_OPERATOR_DISCONNECTION_FEE("gridoperatordisconnectionfeepaydate"),
    DATE_OF_DISCONNECTION_OF_THE_POWER_SUPPLY("powersupplydisconnectiondate"),
    NUMBER_OF_PODS("numberofpods");

    @Getter
    private final String value;

    DPSRequestsListColumns(String disconnectionRequestColumn) {
        this.value = disconnectionRequestColumn;
    }
}
