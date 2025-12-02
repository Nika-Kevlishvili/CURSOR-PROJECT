package bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.impl;

import bg.energo.phoenix.service.billing.model.impl.CompanyDetailedInformationModelImpl;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class ObjectionOfCbgDocumentImpl extends CompanyDetailedInformationModelImpl {
    public String Number;
    public String GridOperator;
    public LocalDateTime CreationDate;
    public LocalDate ChangeDate;
    public List<ObjectionOfCbgDocumentPodImpl> PODs;

    public void fillObjectionData(String number, String gridOperator, LocalDateTime creationDate, LocalDate changeDate, List<ObjectionOfCbgDocumentPodImpl> PODs) {
        this.Number = number;
        this.GridOperator = gridOperator;
        this.CreationDate = creationDate;
        this.ChangeDate = changeDate;
        this.PODs = PODs;
    }
}
