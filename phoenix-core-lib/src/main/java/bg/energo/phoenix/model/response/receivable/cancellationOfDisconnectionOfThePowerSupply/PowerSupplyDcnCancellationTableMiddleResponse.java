package bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply;

public interface PowerSupplyDcnCancellationTableMiddleResponse {
    String getCustomer();

    String getPodIdentifier();

    Long getCustomerId();

    Long getPodId();

    String getCancellationReasonName();
    Long getCancellationReasonId();

    Long getRequestForDisconnectionId();


    boolean getIsChecked();

    boolean getUnableToUncheck();
}
