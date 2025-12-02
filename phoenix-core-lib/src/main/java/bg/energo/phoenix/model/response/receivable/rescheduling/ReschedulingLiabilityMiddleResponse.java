package bg.energo.phoenix.model.response.receivable.rescheduling;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ReschedulingLiabilityMiddleResponse {

    Long getLiabilityId();

    String getLiability();

    BigDecimal getOriginalInitialAmount();

    LocalDate getDueDate();

    BigDecimal getOriginalCurrentAmount();

    LocalDate getInterestsFromDate();

    Long getOutgoingDocumentId();

    String getOutgoingDocumentType();

    BigDecimal getInitialLiabilityAmountInEuro();

    BigDecimal getInitialLiabilityAmountInLeva();

    BigDecimal getCurrentLiabilityAmountInLeva();

    BigDecimal getCurrentLiabilityAmountInEuro();

    Long getCurrencyId();

    String getCurrencyName();

}
