package bg.energo.phoenix.model.documentModels.disconnectionPowerSupplyRequests;

import java.time.LocalDate;

public interface DisconnectionPowerSupplyRequestsDocumentInfoResponse {
    String getPODIdentifier();
    String getCustomerNumber();
    String getCustomerIdentifier();
    String getCustomerNameComb();
    String getCustomerName();
    String getCustomerMiddleName();
    String getCustomerSurname();
    String getPODAddressComb();
    String getMeasurementType();
    String getReason();
    LocalDate getDisconnectionDate();
}
