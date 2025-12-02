package bg.energo.phoenix.service.billing.runs.models;

import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationType;

import java.time.LocalDate;


public interface BillingDataPriceComponentGroup {
    
    Long getContractDetailId();

    Long getPriceComponentId();

    Long getPriceComponentGroupId();

    LocalDate getStartDate();

    LocalDate getEndDate();

    IssuedSeparateInvoice getIssueSeparateInvoice();

    ApplicationType getApplicationType();

    Long getApplicationModelId();
}
