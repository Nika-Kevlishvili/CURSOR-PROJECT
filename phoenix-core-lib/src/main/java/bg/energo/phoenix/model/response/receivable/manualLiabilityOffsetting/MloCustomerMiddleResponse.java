package bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface MloCustomerMiddleResponse {
    String getCustomerNameComb();
    String getCustomerNameCombTrsl();
    String getCustomerIdentifier();
    String getCustomerNumber();
    String getCustomerPopulatedPlace();
    String getCustomerZip();
    String getCustomerDistrict();
    String getCustomerStrBlvdType();
    String getCustomerStrBlvdName();
    String getCustomerBlock();
    String getCustomerEntrance();
    String getCustomerFloor();
    String getCustomerApartment();
    String getCustomerAdditionalInfo();
    String getCustomerQuarterRaName();
    String getCustomerQuarterRaType();
    String getCustomerStrBlvdNumber();
    String getCustomerSegments();
    String getCustomerAddressComb();
    LocalDate getOffsettingDate();
    BigDecimal getLiabilitiesAmountBefore();
    BigDecimal getLiabilitiesAmountAfter();
    BigDecimal getReceivablesAmountBefore();
    BigDecimal getReceivablesAmountAfter();
    String getCurrencyPrintName();
    String getCurrencyAbr();
    String getCurrencyFullName();
}
