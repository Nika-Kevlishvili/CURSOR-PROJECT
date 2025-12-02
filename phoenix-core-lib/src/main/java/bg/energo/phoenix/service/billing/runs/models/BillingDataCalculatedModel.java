package bg.energo.phoenix.service.billing.runs.models;

import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType;
import bg.energo.phoenix.model.enums.product.product.ContractType;
import bg.energo.phoenix.model.enums.time.PeriodType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class BillingDataCalculatedModel {

    private LocalDate billingCalculatedFrom;
    private LocalDate billingCalculatedTo;

    private Long contractDetailId;
    private ContractDetailsStatus status;
    private ContractType contractType;
    private LocalDate terminationDate;

    private Long billingGroupId;
    private String billingGroupNumber;
    private Boolean separateInvoice;
    private Long podDetailId;
    private Long podId;
    private PODMeasurementType measurementType;
    private String podIdentifier;
    private LocalDate podActivationDate;
    private LocalDate podDeactivationDate;


    private LocalDate billingFrom;
    private LocalDate billingTo;
    private Long billingByProfileId;
    private Long billingByScalesId;
    private Long profileId;
    private PeriodType periodType;
    private BillingDataType billingDataType;

    public BillingDataCalculatedModel(BillingDataShortModel shortModel, BillingRunForVolumesModel volumesModel, LocalDate calculatedFrom, LocalDate calculatedTo,Long profileDataId,Long profileId,Long scaleDataId,BillingDataType billingDataType,PeriodType periodType) {
        this.billingCalculatedFrom = calculatedFrom;
        this.billingCalculatedTo = calculatedTo;
        this.contractDetailId = volumesModel.getContractDetailId();
        this.status = volumesModel.getStatus();
        this.terminationDate = volumesModel.getTerminationDate();
        this.billingGroupId = volumesModel.getBillingGroupId();
        this.billingGroupNumber = volumesModel.getBillingGroupNumber();
        this.podDetailId = volumesModel.getPodDetailId();
        this.podIdentifier = volumesModel.getPodIdentifier();
        this.podActivationDate = volumesModel.getPodActivationDate();
        this.podDeactivationDate = volumesModel.getPodDeactivationDate();
        this.billingFrom = shortModel.getBillingTo();
        this.billingTo = shortModel.getBillingTo();
        this.billingByProfileId = profileDataId;
        this.billingByScalesId = scaleDataId;
        this.billingDataType = billingDataType;
        this.podId=volumesModel.getPodId();
        this.separateInvoice=volumesModel.getIssueSeparateInvoice();
        this.measurementType=volumesModel.getPodMeasurementType();
        this.profileId=profileId;
        this.contractType=volumesModel.getContractType();
        this.periodType=periodType;
    }

    public BillingDataCalculatedModel(BillingDataCalculatedModel calculatedModel){
        this.billingCalculatedFrom = calculatedModel.getBillingCalculatedFrom();
        this.billingCalculatedTo = calculatedModel.getBillingCalculatedTo();
        this.contractDetailId = calculatedModel.getContractDetailId();
        this.status = calculatedModel.getStatus();
        this.terminationDate = calculatedModel.getTerminationDate();
        this.billingGroupId = calculatedModel.getBillingGroupId();
        this.billingGroupNumber = calculatedModel.getBillingGroupNumber();
        this.podDetailId = calculatedModel.getPodDetailId();
        this.podIdentifier = calculatedModel.getPodIdentifier();
        this.podActivationDate = calculatedModel.getPodActivationDate();
        this.podDeactivationDate = calculatedModel.getPodDeactivationDate();
        this.billingFrom = calculatedModel.getBillingTo();
        this.billingTo = calculatedModel.getBillingTo();
        this.billingByProfileId = calculatedModel.getBillingByProfileId();
        this.billingByScalesId = calculatedModel.getBillingByScalesId();
        this.billingDataType = calculatedModel.getBillingDataType();
        this.separateInvoice=calculatedModel.getSeparateInvoice();
        this.podId=calculatedModel.getPodId();
        this.measurementType =calculatedModel.getMeasurementType();
        this.profileId=calculatedModel.getProfileId();
        this.contractType =calculatedModel.getContractType();
        this.periodType=calculatedModel.getPeriodType();
    }

}
