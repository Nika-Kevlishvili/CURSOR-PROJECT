package bg.energo.phoenix.service.billing.runs.models;

import bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType;
import bg.energo.phoenix.model.enums.time.PeriodType;
import lombok.Data;

import java.time.LocalDate;

@Data

public class BillingRunSettlementCalculatedModel {

    private Integer invoiceNumber;
    private Long contractDetailId;
    private Long billingGroupId;
    private Boolean separateInvoice;
    private Long podDetailId;
    private Long podId;
    private PODMeasurementType measurementType;
    private Long billingByProfileId;
    private Long profileId;

    private LocalDate calculatedFrom;
    private LocalDate calculatedTo;

    private Long priceComponentId;
    private Long applicationModelId;
    private PeriodType periodType;

    public BillingRunSettlementCalculatedModel(BillingDataInvoiceModels models,LocalDate calculatedFrom,LocalDate calculatedTo) {
        this.invoiceNumber = models.getInvoiceNumber();

        this.contractDetailId = models.getContractDetailId();
        this.billingGroupId = models.getBillingGroupId();
        this.separateInvoice = models.getSeparateInvoice();
        this.podDetailId = models.getPodDetailId();
        this.podId = models.getPodId();
        this.measurementType = models.getMeasurementType();
        this.billingByProfileId = models.getBillingByProfileId();
        this.calculatedFrom = calculatedFrom;
        this.calculatedTo = calculatedTo;
        this.priceComponentId = models.getPriceComponentId();
        this.applicationModelId = models.getApplicationModelId();
        this.periodType=models.getPeriodType();
        this.profileId=models.getProfileId();
    }

    public BillingRunSettlementCalculatedModel(BillingRunSettlementCalculatedModel model) {
        this.invoiceNumber = model.invoiceNumber;
        this.contractDetailId = model.contractDetailId;
        this.billingGroupId = model.billingGroupId;
        this.separateInvoice = model.separateInvoice;
        this.podDetailId = model.podDetailId;
        this.podId = model.podId;
        this.measurementType = model.measurementType;
        this.billingByProfileId = model.billingByProfileId;
        this.calculatedFrom = model.calculatedFrom;
        this.calculatedTo = model.calculatedTo;
        this.priceComponentId = model.priceComponentId;
        this.applicationModelId = model.applicationModelId;
        this.periodType = model.periodType;
        this.profileId=model.profileId;
    }
}
