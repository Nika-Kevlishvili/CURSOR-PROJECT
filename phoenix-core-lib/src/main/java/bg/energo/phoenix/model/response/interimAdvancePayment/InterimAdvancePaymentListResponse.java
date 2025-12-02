package bg.energo.phoenix.model.response.interimAdvancePayment;

import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.DeductionFrom;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.ValueType;

import java.time.LocalDateTime;

public interface InterimAdvancePaymentListResponse {
    Long getId();

    String getName();

    ValueType getValueType();

    DeductionFrom getDeductionFrom();

    String getAvailable();

    LocalDateTime getCreateDate();

    InterimAdvancePaymentStatus getStatus();
}
