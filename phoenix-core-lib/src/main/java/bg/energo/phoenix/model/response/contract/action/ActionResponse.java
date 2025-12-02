package bg.energo.phoenix.model.response.contract.action;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import bg.energo.phoenix.model.enums.contract.action.ActionStatus;
import bg.energo.phoenix.model.enums.product.termination.terminations.TerminationNotificationChannelType;
import bg.energo.phoenix.model.response.contract.ContractFileResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActionResponse {
    private Long id;
    private EntityStatus status;
    private LocalDateTime createDate;
    private ActionStatus actionStatus;
    private Long actionTypeId;
    private String actionTypeName;
    private LocalDate noticeReceivingDate;
    private LocalDate executionDate;
    private BigDecimal calculatedPenaltyAmount;
    private Long calculatedPenaltyAmountCurrencyId;
    private String calculatedPenaltyAmountCurrencyName;
    private BigDecimal penaltyClaimAmount;
    private Long penaltyClaimAmountCurrencyId;
    private String penaltyClaimAmountCurrencyName;
    private ActionPenaltyPayer penaltyPayer;
    private Boolean dontAllowAutomaticPenaltyClaim;
    private String additionalInformation;
    private Long penaltyId;
    private String penaltyName;
    private Boolean withoutPenalty;
    private Long terminationId;
    private String terminationName;
    private Boolean withoutAutomaticTermination;
    private Long customerId;
    private String customerName; // format: "identification number (name)" 5502035521 (Иван Иванов Иван)
    private Long productContractId;
    private Long serviceContractId;
    private String contractName; // format: "prefix - contract number/creation date" ПКПС - 223456984/11.11.2022
    private boolean isLiabilityGenerated; // should be returned in preview to trigger "claim penalty" button visibility

    private List<TerminationNotificationChannelType> terminationNotificationChannels;
    private List<ActionPodResponse> pods; // format: pod identifier
    private List<ContractFileResponse> files;
    private ShortResponse liability;
    private ContractTemplateShortResponse emailTemplateResponse;
    private ContractTemplateShortResponse templateResponse;

    public ActionResponse(Action action,
                          String actionTypeName,
                          String contractName,
                          String calculatedPenaltyAmountCurrencyName,
                          String penaltyClaimAmountCurrencyName,
                          String penaltyName,
                          String terminationName,
                          Customer customer,
                          CustomerDetails customerDetails,
                          Boolean isLiabilityGenerated,
                          String legalFormName
    ) {
        this.id = action.getId();
        this.status = action.getStatus();
        this.createDate = action.getCreateDate();
        this.actionStatus = action.getActionStatus();
        this.actionTypeId = action.getActionTypeId();
        this.actionTypeName = actionTypeName;
        this.noticeReceivingDate = action.getNoticeReceivingDate();
        this.executionDate = action.getExecutionDate();
        this.calculatedPenaltyAmount = action.getCalculatedPenaltyAmount();
        this.calculatedPenaltyAmountCurrencyId = action.getCalculatedPenaltyCurrencyId();
        this.calculatedPenaltyAmountCurrencyName = calculatedPenaltyAmountCurrencyName;
        this.penaltyClaimAmount = action.getPenaltyClaimAmount();
        this.penaltyClaimAmountCurrencyId = action.getPenaltyClaimCurrencyId();
        this.penaltyClaimAmountCurrencyName = penaltyClaimAmountCurrencyName;
        this.penaltyPayer = action.getPenaltyPayer();
        this.dontAllowAutomaticPenaltyClaim = action.getDontAllowAutomaticPenaltyClaim();
        this.additionalInformation = action.getAdditionalInfo();
        this.penaltyId = action.getPenaltyId();
        this.penaltyName = penaltyName;
        this.withoutPenalty = action.getWithoutPenalty();
        this.terminationId = action.getTerminationId();
        this.terminationName = terminationName;
        this.withoutAutomaticTermination = action.getWithoutAutomaticTermination();
        this.customerId = action.getCustomerId();
        this.customerName = getFormattedCustomerName(customer, customerDetails, legalFormName);
        this.productContractId = action.getProductContractId();
        this.serviceContractId = action.getServiceContractId();
        this.contractName = contractName;
        this.isLiabilityGenerated = isLiabilityGenerated;
    }

    private static String getFormattedCustomerName(Customer customer, CustomerDetails customerDetails, String legalFormName) {
        switch (customer.getCustomerType()) {
            case PRIVATE_CUSTOMER -> {
                if (customerDetails.getMiddleName() == null) {
                    return "%s (%s %s)".formatted(
                            customer.getIdentifier(),
                            customerDetails.getName(),
                            customerDetails.getLastName()
                    );
                } else {
                    return "%s (%s %s %s)".formatted(
                            customer.getIdentifier(),
                            customerDetails.getName(),
                            customerDetails.getMiddleName(),
                            customerDetails.getLastName()
                    );
                }
            }
            case LEGAL_ENTITY -> {
                return "%s (%s %s)".formatted(
                        customer.getIdentifier(),
                        customerDetails.getName(),
                        legalFormName);
            }
            default -> throw new IllegalArgumentException("Unknown customer type: " + customer.getCustomerType());
        }
    }

}
