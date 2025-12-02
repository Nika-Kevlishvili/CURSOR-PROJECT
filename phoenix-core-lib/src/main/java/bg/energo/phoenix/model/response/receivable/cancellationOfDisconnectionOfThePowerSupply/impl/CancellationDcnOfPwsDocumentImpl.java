package bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply.impl;

import bg.energo.phoenix.service.billing.model.impl.CompanyDetailedInformationModelImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class CancellationDcnOfPwsDocumentImpl extends CompanyDetailedInformationModelImpl {
    public String DocumentNumber;
    public String DocumentDate;
    public String GridOperator;
    public String GridOperatorFullName;
    public String SupplierType;
    public List<CancellationDcnOfPwsDocumentPodImpl> PODs;

    public void fillCancellationData(String documentNumber, String documentDate, String gridOperator, String gridOperatorFullName, String supplierType, List<CancellationDcnOfPwsDocumentPodImpl> PODs) {
        this.DocumentNumber = documentNumber;
        this.DocumentDate = documentDate;
        this.GridOperator = gridOperator;
        this.GridOperatorFullName = gridOperatorFullName;
        this.SupplierType = supplierType;
        this.PODs = PODs;
    }
}
