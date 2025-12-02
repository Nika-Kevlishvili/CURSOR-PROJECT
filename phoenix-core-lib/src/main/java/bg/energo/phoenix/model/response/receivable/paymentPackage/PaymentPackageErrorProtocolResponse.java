package bg.energo.phoenix.model.response.receivable.paymentPackage;

public record PaymentPackageErrorProtocolResponse(
        Long paymentId,

        String initialAmount,

        String currentAmount,

        Long linkedReceivableId
) {

}
