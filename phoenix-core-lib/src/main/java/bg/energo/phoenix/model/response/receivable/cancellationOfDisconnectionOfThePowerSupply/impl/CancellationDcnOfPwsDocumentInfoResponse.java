package bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply.impl;

public interface CancellationDcnOfPwsDocumentInfoResponse {
    String getPODIdentifier();
    String getCustomerNumber();
    String getCustomerIdentifier();
    String getCustomerNameComb();
    String getCustomerName();
    String getCustomerMiddleName();
    String getCustomerSurname();
    String getMeasurementType();
    String getReason();
    String getDisconnectionDate();
    String getDisconnectionRequest();
    String getHeadquarterAddressComb();
}
