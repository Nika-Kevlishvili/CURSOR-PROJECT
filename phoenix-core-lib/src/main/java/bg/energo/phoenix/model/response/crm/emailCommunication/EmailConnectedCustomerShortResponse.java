package bg.energo.phoenix.model.response.crm.emailCommunication;

public record EmailConnectedCustomerShortResponse(
        Long customerId,
        Long customerDetailId,
        Long customerDetailVersionId,
        String name
) {
}
