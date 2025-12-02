package bg.energo.phoenix.model.response.receivable.rescheduling;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ReschedulingAddressResponse {
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
    String getCurrencyPrintName();
    String getCurrencyAbr();
    String getCurrencyFullName();
    BigDecimal getTotalInstallmentsAmount();
    BigDecimal getTotalInstallmentsPrinciple();
    BigDecimal getTotalInstallmentsInterests();
    LocalDateTime getLiabilitiesPeriodFrom();
    LocalDateTime getLiabilitiesPeriodTo();
}
