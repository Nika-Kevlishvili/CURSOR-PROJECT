package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.DisconnectionRequestsStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.SupplierType;

import java.time.LocalDate;

public interface DisconnectionRequestListingMiddleResponse {

    Long getId();

    Long getGridOperatorId();

    String getRequestNumber();

    LocalDate getCreateDate();

    DisconnectionRequestsStatus getDisconnectionRequestStatus();

    SupplierType getSupplierType();

    String getGridOperator();

    LocalDate getGridOperatorRequestRegistrationDate();

    LocalDate getCustomerReminderLetterSentDate();

    LocalDate getGridOperatorDisconnectionFeePayDate();

    LocalDate getPowerSupplyDisconnectionDate();

    Long getNumberOfPods();

    EntityStatus getStatus();

}
