package bg.energo.phoenix.model.response.priceComponent;

import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;

public interface PriceComponentForOvertimeResponse {
    Long getId();
    IssuedSeparateInvoice getIssuedSeparateInvoice();
    String getPriceFormula();
    Long getPriceComponentValueTypeId();
    Long getPriceComponentPriceTypeId();
    Long getVatRateId();
    String getIncomeAccountNumber();
    String getCostCenterControllingOrder();
    Long getContractDetailId();
    Long getServiceOrProductId();
    Double getVatRatePercent();
}
