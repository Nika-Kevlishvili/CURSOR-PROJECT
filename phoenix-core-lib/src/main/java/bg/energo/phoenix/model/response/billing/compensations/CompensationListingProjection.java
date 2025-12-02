package bg.energo.phoenix.model.response.billing.compensations;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.compensation.CompensationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CompensationListingProjection {
    Long getId();

    String getNumber();

    LocalDate getDate();

    String getPeriod();

    BigDecimal getDocumentVolumes();

    String getCompensationReason();

    BigDecimal getPrice();

    BigDecimal getAmount();

    String getCurrency();

    String getCustomer();

    String getPod();

    String getRecipient();

    CompensationStatus getCompensationStatus();

    EntityStatus getStatus();

    String getIndex();

    LocalDate getInvoiceUsageDate();
}
