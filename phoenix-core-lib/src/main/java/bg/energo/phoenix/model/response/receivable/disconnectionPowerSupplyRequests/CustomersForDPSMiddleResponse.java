package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests;

import java.math.BigDecimal;

public interface CustomersForDPSMiddleResponse {
    String getCustomer();

    String getContracts();

    String getAltRecipientInvCustomer();

    String getBillingGroups();

    String getPodIdentifier();

    String getIsHighestConsumption();

    String getLiabilitiesInBillingGroup();

    String getLiabilitiesInPod();

    Long getPodId();

    Long getCustomerId();

    Long getGridOperatorId();

    BigDecimal getLiabilityAmountCustomer();

    String getExistingCustomerReceivables();

    Boolean getIsChecked();

    String getInvoiceNumber();

    String getCustomerNumber();

    Long getPodDetailId();
}
