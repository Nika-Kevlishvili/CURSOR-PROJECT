package bg.energo.phoenix.model.response.receivable.latePaymentFine;

import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineReversed;
import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface LatePaymentFineListingMiddleResponse {
    String getNumber();
    LocalDate getCreateDate();
    LocalDate getDueDate();
    LatePaymentFineType getType();
    String getCustomerIdentifier();
    LatePaymentFineReversed getReversed();
    String getBillingGroup();
    BigDecimal getTotalAmount();
    String getCurrency();
    Long getId();
    LocalDate getLogicalDate();
    String getCustomer();
}
