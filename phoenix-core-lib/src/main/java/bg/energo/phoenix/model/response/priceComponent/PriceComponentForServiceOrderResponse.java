package bg.energo.phoenix.model.response.priceComponent;

import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import bg.energo.phoenix.model.enums.product.price.priceComponent.NumberType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationLevel;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationType;

public interface PriceComponentForServiceOrderResponse {
    Long getId();

    IssuedSeparateInvoice getIssuedSeparateInvoice();

    String getPriceFormula();

    Long getPriceComponentValueTypeId();

    Long getPriceComponentPriceTypeId();

    String getIncomeAccountNumber();

    String getCostCenterControllingOrder();

    Long getServiceDetailId();

    Long getVatRateId();

    Double getVatRatePercent();

    Long getApplicationModelId();

    ApplicationModelType getApplicationModelType();

    ApplicationType getApplicationType();

    String getConditions();

    Long getPcGroupDetailId();

    Long getServiceUnitId();

    String getPerPieceRanges();

    ApplicationLevel getApplicationLevel();

    NumberType getNumberType();

    Boolean getNoPodCondition();

    Long getCurrencyId();

    Long getValueTypeId();
}
