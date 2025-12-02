package bg.energo.phoenix.service.billing.runs.models;

import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationType;
import bg.energo.phoenix.model.enums.time.PeriodType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BillingDataCalculatedPriceComponents extends BillingDataCalculatedModel {
    private LocalDate calculatedFrom;
    private LocalDate calculatedTo;
    private Long priceComponentId;
    private Long applicationModelId;
    private IssuedSeparateInvoice issuedSeparateInvoice;
    private ApplicationType applicationType;
    private PeriodType periodType;

    public BillingDataCalculatedPriceComponents(BillingDataCalculatedModel calculatedModel,BillingDataPriceComponents billingDataPriceComponents) {
        super(calculatedModel);
        this.calculatedFrom=calculatedModel.getBillingCalculatedFrom();
        this.calculatedTo=calculatedModel.getBillingCalculatedTo();
        this.priceComponentId=billingDataPriceComponents.getPriceComponentId();
        this.issuedSeparateInvoice=billingDataPriceComponents.getIssuedSeparateInvoice();
        this.applicationType=billingDataPriceComponents.getApplicationType();
        this.applicationModelId = billingDataPriceComponents.getApplicationModelId();
    }

    public BillingDataCalculatedPriceComponents(BillingDataCalculatedModel calculatedModel,LocalDate calculatedFrom,LocalDate calculatedTo,BillingDataPriceComponentGroup billingDataPriceComponents) {
        super(calculatedModel);
        this.calculatedFrom = calculatedFrom;
        this.calculatedTo = calculatedTo;
        this.priceComponentId = billingDataPriceComponents.getPriceComponentId();
        this.issuedSeparateInvoice=billingDataPriceComponents.getIssueSeparateInvoice();
        this.applicationType=billingDataPriceComponents.getApplicationType();
        this.applicationModelId=billingDataPriceComponents.getApplicationModelId();
    }

    public BillingDataCalculatedPriceComponents( BillingDataCalculatedPriceComponents calculatedPriceComponents) {
        super(calculatedPriceComponents);
        this.calculatedFrom = calculatedPriceComponents.getCalculatedFrom();
        this.calculatedTo = calculatedPriceComponents.getCalculatedTo();
        this.priceComponentId = calculatedPriceComponents.getPriceComponentId();
        this.issuedSeparateInvoice = calculatedPriceComponents.getIssuedSeparateInvoice();
        this.applicationType=calculatedPriceComponents.getApplicationType();
        this.applicationModelId=calculatedPriceComponents.getApplicationModelId();
        this.periodType=calculatedPriceComponents.getPeriodType();
    }

}
