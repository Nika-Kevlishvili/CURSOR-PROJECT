package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequests;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CustomerConditionType;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.DisconnectionRequestsStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.SupplierType;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DPSRequestsResponse {

    private Long id;

    private LocalDate createDate;

    private String requestNumber;

    private SupplierType supplierType;

    private DisconnectionRequestsStatus disconnectionRequestsStatus;

    private ShortResponse gridOperator;

    private ShortResponse disconnectionReason;

    private LocalDate gridOpRequestRegDate;

    private LocalDateTime customerReminderLetterSentDate;

    private LocalDate gridOpDisconnectionFeePayDate;

    private LocalDate powerSupplyDisconnectionDate;

    private BigDecimal liabilityAmountFrom;

    private BigDecimal liabilityAmountTo;

    private ShortResponse currency;

    private CustomerConditionType customerConditionType;

    private String condition;

    private List<ConditionInfoShortResponse> conditionsInfo;

    private String listOfCustomers;

    private EntityStatus status;

    private ShortResponse reminderForDisconnection;

    private List<TaskShortResponse> taskShortResponse;
    private List<ContractTemplateShortResponse> templateResponses;

    private List<DisconnectionRequestsFileResponse> files;

    private Boolean isAllSelected;
    private Boolean podWithHighestConsumption;
    private List<Long> excludePodIds;

    public DPSRequestsResponse(DisconnectionPowerSupplyRequests disconnectionPowerSupplyRequests) {
        this.id = disconnectionPowerSupplyRequests.getId();
        this.createDate = LocalDate.from(disconnectionPowerSupplyRequests.getCreateDate());
        this.requestNumber = disconnectionPowerSupplyRequests.getRequestNumber();
        this.supplierType = disconnectionPowerSupplyRequests.getSupplierType();
        this.disconnectionRequestsStatus = disconnectionPowerSupplyRequests.getDisconnectionRequestsStatus();
        this.gridOpRequestRegDate = disconnectionPowerSupplyRequests.getGridOpRequestRegDate();
        this.customerReminderLetterSentDate = disconnectionPowerSupplyRequests.getCustomerReminderLetterSentDate();
        this.gridOpDisconnectionFeePayDate = disconnectionPowerSupplyRequests.getGridOpDisconnectionFeePayDate();
        this.powerSupplyDisconnectionDate = disconnectionPowerSupplyRequests.getPowerSupplyDisconnectionDate();
        this.liabilityAmountFrom = disconnectionPowerSupplyRequests.getLiabilityAmountFrom();
        this.liabilityAmountTo = disconnectionPowerSupplyRequests.getLiabilityAmountTo();
        this.customerConditionType = disconnectionPowerSupplyRequests.getCustomerConditionType();
        this.listOfCustomers = disconnectionPowerSupplyRequests.getListOfCustomers();
        this.status = disconnectionPowerSupplyRequests.getStatus();
        this.isAllSelected = disconnectionPowerSupplyRequests.getIsAllSelected();
        this.podWithHighestConsumption = disconnectionPowerSupplyRequests.getPodWithHighestConsumption();
    }

}
