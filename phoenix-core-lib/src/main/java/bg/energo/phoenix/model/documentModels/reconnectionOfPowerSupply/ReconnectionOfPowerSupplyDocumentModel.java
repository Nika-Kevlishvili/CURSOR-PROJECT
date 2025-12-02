package bg.energo.phoenix.model.documentModels.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.documentModels.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsDocumentPodImpl;
import bg.energo.phoenix.service.billing.model.impl.CompanyDetailedInformationModelImpl;

import java.time.LocalDateTime;
import java.util.List;

public class ReconnectionOfPowerSupplyDocumentModel extends CompanyDetailedInformationModelImpl {
    public String DocumentNumber;
    public LocalDateTime DocumentDate;
    public String GridOperator;
    public String GridOperatorFullName;
    public List<DisconnectionPowerSupplyRequestsDocumentPodImpl> PODs;

    public void fillDocumentData(String documentNumber, LocalDateTime documentDate, String gridOperator, String gridOperatorFullName, List<DisconnectionPowerSupplyRequestsDocumentPodImpl> PODs) {
        this.DocumentNumber = documentNumber;
        this.DocumentDate = documentDate;
        this.GridOperator = gridOperator;
        this.GridOperatorFullName = gridOperatorFullName;
        this.PODs = PODs;
    }
}
