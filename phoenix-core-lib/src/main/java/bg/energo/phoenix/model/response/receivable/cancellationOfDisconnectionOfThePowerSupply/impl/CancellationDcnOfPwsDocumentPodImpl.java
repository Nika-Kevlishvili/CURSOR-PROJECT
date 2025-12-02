package bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply.impl;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CancellationDcnOfPwsDocumentPodImpl {
    public String PODIdentifier;
    public String CustomerNumber;
    public String CustomerIdentifier;
    public String CustomerNameComb;
    public String CustomerName;
    public String CustomerMiddleName;
    public String CustomerSurname;
    public String PODAddressComb;
    public String MeasurementType;
    public String Reason;
    public String ExpressReconnectionYN;
    public String DisconnectionDate;
    public String ReconnectionDate;
    public String DisconnectionRequest;

    public void fillPodsInfo(CancellationDcnOfPwsDocumentInfoResponse response) {
        this.PODIdentifier = response.getPODIdentifier();
        this.CustomerNumber = response.getCustomerNumber();
        this.CustomerIdentifier = response.getCustomerIdentifier();
        this.CustomerNameComb = response.getCustomerNameComb();
        this.CustomerName = response.getCustomerName();
        this.CustomerMiddleName = response.getCustomerMiddleName();
        this.CustomerSurname = response.getCustomerSurname();
        this.PODAddressComb = response.getHeadquarterAddressComb();
        this.MeasurementType = response.getMeasurementType();
        this.Reason = response.getReason();
        this.DisconnectionDate = response.getDisconnectionDate();
        this.DisconnectionRequest = response.getDisconnectionRequest();
        this.ExpressReconnectionYN= " ";
        this.ReconnectionDate = " ";
    }

}
