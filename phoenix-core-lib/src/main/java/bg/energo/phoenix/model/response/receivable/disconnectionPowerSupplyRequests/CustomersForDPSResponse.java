package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Data
@NoArgsConstructor
public class CustomersForDPSResponse {
    private String customers;
    private String contracts;
    private String altRecipientInvCustomers;
    private String billingGroups;
    private String podIdentifier;
    private Boolean isHighestConsumption;
    private String liabilitiesInBillingGroup;
    private String liabilitiesInPod;
    private Long podId;
    private Long customerId;
    private Long gridOperatorId;
    private BigDecimal liabilityAmountCustomer;
    private Boolean existingCustomerReceivables;
    private String invoiceNumber;
    private String customerNumber;

    private Long podDetailId;
    private Boolean isChecked = false;

    public CustomersForDPSResponse(CustomersForDPSMiddleResponse middleResponse) {
        this.customers = middleResponse.getCustomer();
        this.contracts = middleResponse.getContracts();
        this.altRecipientInvCustomers = middleResponse.getAltRecipientInvCustomer();
        this.billingGroups = middleResponse.getBillingGroups();
        this.podIdentifier = middleResponse.getPodIdentifier();
        this.isHighestConsumption = "true".equals(middleResponse.getIsHighestConsumption());
        this.liabilitiesInBillingGroup = middleResponse.getLiabilitiesInBillingGroup();
        this.liabilitiesInPod = middleResponse.getLiabilitiesInPod();
        this.podId = middleResponse.getPodId();
        this.customerId = middleResponse.getCustomerId();
        this.gridOperatorId = middleResponse.getGridOperatorId();
        this.liabilityAmountCustomer = middleResponse.getLiabilityAmountCustomer();
        this.existingCustomerReceivables = "YES".equals(middleResponse.getExistingCustomerReceivables());
        this.invoiceNumber = middleResponse.getInvoiceNumber();
        this.customerNumber = middleResponse.getCustomerNumber();
        this.podDetailId = middleResponse.getPodDetailId();
        if (Objects.nonNull(middleResponse.getIsChecked())) {
            this.isChecked = middleResponse.getIsChecked();
        }
    }

}
