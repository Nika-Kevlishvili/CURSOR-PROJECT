package bg.energo.migration.integration.phoenix.customer.model.request;

import bg.energo.migration.integration.phoenix.customer.model.enums.CustomerDetailStatus;
import bg.energo.migration.integration.phoenix.customer.model.enums.CustomerType;
import bg.energo.migration.integration.phoenix.customer.model.request.accountmanagers.CreateCustomerAccountManagerRequest;
import bg.energo.migration.integration.phoenix.customer.model.request.address.CustomerAddressRequest;
import bg.energo.migration.integration.phoenix.customer.model.request.bankingdetails.CustomerBankingDetails;
import bg.energo.migration.integration.phoenix.customer.model.request.businesscustomerdetails.BusinessCustomerDetails;
import bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.CreateCustomerCommunicationsRequest;
import bg.energo.migration.integration.phoenix.customer.model.request.managers.CreateManagerRequest;
import bg.energo.migration.integration.phoenix.customer.model.request.owner.CustomerOwnerRequest;
import bg.energo.migration.integration.phoenix.customer.model.request.privatecustomerdetails.PrivateCustomerDetails;
import bg.energo.migration.integration.phoenix.customer.model.request.relatedcustomers.CreateRelatedCustomerRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {
    private CustomerType customerType;
    private Boolean businessActivity;
    private String customerIdentifier;
    private Boolean foreign;
    private Boolean marketingConsent;
    private boolean preferCommunicationInEnglish;
    private String oldCustomerNumber;
    private String vatNumber;
    private CustomerDetailStatus customerDetailStatus;
    private BusinessCustomerDetails businessCustomerDetails;
    private PrivateCustomerDetails privateCustomerDetails;
    private Long ownershipFormId;
    private Long economicBranchId;
    private Long economicBranchNCEAId;
    private String mainSubjectOfActivity;
    private List<Long> segmentIds;
    private CustomerAddressRequest address;
    private CustomerBankingDetails bankingDetails;
    private List<CreateManagerRequest> managers;
    private List<CreateRelatedCustomerRequest> relatedCustomers;
    private List<CustomerOwnerRequest> owner;
    private List<CreateCustomerCommunicationsRequest> communicationData;
    private List<CreateCustomerAccountManagerRequest> accountManagers;
    private String customerAdditionalInformation;
}
