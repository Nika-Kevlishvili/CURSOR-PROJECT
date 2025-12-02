package bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply;

public interface PodsByGridOperatorResponse {

    String getCustomer();

    String getPodIdentifier();

    String getPsdrRequestNumber();

    Long getCustomerId();

    Long getPodId();

    Long getRequestForDisconnectionId();

    Long getGridOperatorId();

    boolean getIsChecked();

    boolean getUnableToUncheck();
}
