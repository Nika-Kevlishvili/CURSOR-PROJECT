package bg.energo.phoenix.model.request.receivable.payment;

import bg.energo.phoenix.model.customAnotations.receivable.payment.PaymentCancelRequestValidator;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@PaymentCancelRequestValidator
public class PaymentCancelRequest {

    @NotNull(message = "Payment ID must not be null;")
    Long paymentId;

    @NotNull(message = "Customer ID must not be null;")
    Long customerId;

    Boolean blockedForOffsetting;

    LocalDate blockedForOffsettingFromDate;

    LocalDate blockedForOffsettingToDate;

    Long reasonId;
}
