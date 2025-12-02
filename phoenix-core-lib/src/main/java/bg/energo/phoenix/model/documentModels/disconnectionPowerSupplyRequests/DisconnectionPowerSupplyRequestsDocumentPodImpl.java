package bg.energo.phoenix.model.documentModels.disconnectionPowerSupplyRequests;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@RequiredArgsConstructor
public class  DisconnectionPowerSupplyRequestsDocumentPodImpl {
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
    public LocalDate DisconnectionDate;
    public String ReconnectionDate;
    public String DisconnectionRequest;

    public void fillPodsInfo(DisconnectionPowerSupplyRequestsDocumentInfoResponse response) {
        this.PODIdentifier = response.getPODIdentifier();
        this.CustomerNumber = response.getCustomerNumber();
        this.CustomerIdentifier = response.getCustomerIdentifier();
        this.CustomerNameComb = response.getCustomerNameComb();
        this.CustomerName = response.getCustomerName();
        this.CustomerMiddleName = response.getCustomerMiddleName();
        this.CustomerSurname = response.getCustomerSurname();
        this.PODAddressComb = response.getPODAddressComb();
        this.MeasurementType = response.getMeasurementType();
        this.Reason = response.getReason();
        this.ExpressReconnectionYN = "";
        this.DisconnectionDate = response.getDisconnectionDate();
        this.ReconnectionDate = "";
        this.DisconnectionRequest = "";
    }
}
