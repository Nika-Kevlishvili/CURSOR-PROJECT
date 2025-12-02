package bg.energo.phoenix.service.billing.model.persistance;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface BillingRunDocumentModel {
    String getCustomerNameComb();

    String getCustomerNameCombTrsl();

    String getCustomerIdentifer();

    String getCustomerVat();

    String getCustomerNumber();

    String getCustomerSegments();

    String getBillingGroup();

    String getContractNumber();

    String getDocumentType();

    String getDocumentNumber();

    String getDocumentPrefix();

    LocalDate getDocumentDate();

    LocalDate getTaxEventDate();

    LocalDate getMeterReadingFrom();

    LocalDate getMeterReadingTo();

    String getInvoicedMonth();

    LocalDate getPaymentDeadline();

    String getBasisForIssuing();

    String getCurrencyPrintName();

    String getCurrencyAbr();

    String getCurrencyFullName();

    String getOtherCurrencyPrintName();

    String getOtherCurrencyAbr();

    String getOtherCurrencyFullName();

    BigDecimal getTotalExclVat();

    BigDecimal getTotalVat();

    BigDecimal getTotalInclVat();

    BigDecimal getTotalInclVatOtherCurrency();

    BigDecimal getFinalLiabilityAmount();

    String getCustomerAddressComb();

    String getCustomerPopulatedPlace();

    String getCustomerZip();

    String getCustomerDistrict();

    String getCustomerQuarterRaType();

    String getCustomerQuarterRaName();

    String getCustomerStrBlvdType();

    String getCustomerStrBlvdName();

    String getCustomerStrBlvdNumber();

    String getCustomerBlock();

    String getCustomerEntrance();

    String getCustomerFloor();

    String getCustomerApartment();

    String getCustomerAdditionalInfo();
    String getBillingNumber();
}
