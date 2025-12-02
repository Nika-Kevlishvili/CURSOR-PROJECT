package bg.energo.phoenix.model.response.receivable.payment;

import bg.energo.phoenix.model.entity.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PaymentListMiddleResponse {
    String getPaymentNumber();
    String getCustomer();
    String getBillingGroup();
    String getOutgoingDocumentType();
    String getPaymentChannel();
    Long getPaymentPackage();
    LocalDate getPaymentDate();
    BigDecimal getInitialAmount();
    BigDecimal getCurrentAmount();
    Long getCustomerPaymentId();
    EntityStatus getStatus();
}
