package bg.energo.phoenix.model.response.receivable.payment;

public record PaymentToLiabilityOffsettingInfo(
        Long id,
        Long paymentId,
        Long liabilityId
) {
}
