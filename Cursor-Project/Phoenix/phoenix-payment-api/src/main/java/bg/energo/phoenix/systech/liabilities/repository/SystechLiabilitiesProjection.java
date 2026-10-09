package bg.energo.phoenix.systech.liabilities.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface SystechLiabilitiesProjection {
    Long getLiabilityId();

    String getLiabilityNumber();

    LocalDate getOccurrenceDate();

    LocalDate getDueDate();

    BigDecimal getCurrentAmount();

    Long getCurrencyId();

    String getOutgoingDocumentFromExternalSystem();

    String getInvoiceNumber();

    String getOutgoingDocumentNumber();

    String getBasisForIssuing();

    LocalDate getMeterReadingPeriodFrom();

    LocalDate getMeterReadingPeriodTo();

    String getCustomerName();

    String getCustomerAddress();

    String getCustomerNumber();

    String getBillingGroup();

    String getCurrency();

    Long getInvoiceId();

    String getOutgoingDocumentType();

    Long getLatePaymentFineId();

    Long getActionId();

    Long getClaimedPenaltyId();

    Long getReschedulingId();

    Long getDepositId();
}

