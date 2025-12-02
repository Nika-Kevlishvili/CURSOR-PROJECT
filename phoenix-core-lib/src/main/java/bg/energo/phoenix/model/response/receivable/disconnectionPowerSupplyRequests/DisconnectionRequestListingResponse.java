package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.DisconnectionRequestsStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.SupplierType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DisconnectionRequestListingResponse {

    private Long id;

    private Long gridOperatorId;

    private String requestNumber;

    private LocalDate createDate;

    private DisconnectionRequestsStatus disconnectionRequestStatus;

    private SupplierType supplierType;

    private String gridOperator;

    private LocalDate gridOperatorRequestRegistrationDate;

    private LocalDate customerReminderLetterSentDate;

    private LocalDate gridOperatorDisconnectionFeePayDate;

    private LocalDate powerSupplyDisconnectionDate;

    private Long numberOfPods;

    private EntityStatus status;

    public DisconnectionRequestListingResponse(DisconnectionRequestListingMiddleResponse middleResponse) {
        this.id = middleResponse.getId();
        this.gridOperatorId = middleResponse.getGridOperatorId();
        this.requestNumber = middleResponse.getRequestNumber();
        this.createDate = middleResponse.getCreateDate();
        this.disconnectionRequestStatus = middleResponse.getDisconnectionRequestStatus();
        this.supplierType = middleResponse.getSupplierType();
        this.gridOperator = middleResponse.getGridOperator();
        this.gridOperatorRequestRegistrationDate = middleResponse.getGridOperatorRequestRegistrationDate();
        this.customerReminderLetterSentDate = middleResponse.getCustomerReminderLetterSentDate();
        this.gridOperatorDisconnectionFeePayDate = middleResponse.getGridOperatorDisconnectionFeePayDate();
        this.powerSupplyDisconnectionDate = middleResponse.getPowerSupplyDisconnectionDate();
        this.numberOfPods = middleResponse.getNumberOfPods();
        this.status = middleResponse.getStatus();
    }

}
