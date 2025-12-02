package phoenix.core.customer.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import phoenix.core.customer.model.customAnotations.customer.DigitsCommaAndSpaceValidator;
import phoenix.core.customer.model.customAnotations.customer.MainSubjectActivityValidator;
import phoenix.core.customer.model.customAnotations.customer.withValidators.*;
import phoenix.core.customer.model.enums.customer.CustomerDetailStatus;
import phoenix.core.customer.model.enums.customer.CustomerType;
import phoenix.core.customer.model.request.communicationData.CreateCommunicationDataRequest;
import phoenix.core.customer.model.request.manager.CreateManagerRequest;
import phoenix.core.customer.model.request.relatedCustomer.CreateRelatedCustomerRequest;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

import static phoenix.core.customer.model.request.communicationData.CreateCommunicationDataRequest.getCreateCommunicationDataRequest;
import static phoenix.core.customer.model.request.manager.CreateManagerRequest.getCreateManagersRequestList;
import static phoenix.core.customer.model.request.relatedCustomer.EditRelatedCustomerRequest.getRelatedCustomersAddRequest;

@Data
@CustomerStatusIsNotPotential
@CustomerIdentifierValidator
@VatNumberValidator
@ValidationsByCustomerType
@ManagerValidator
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {
    @NotNull(message = "Customer Type is required; ")
    private CustomerType customerType;

    @NotNull(message = "Customer Identifier is required; ")
    private String customerIdentifier;

    @NotNull(message = "Foreign is required; ")
    private Boolean foreign;

    @NotNull(message = "Marketing Consent is required; ")
    private Boolean marketingConsent;

    @DigitsCommaAndSpaceValidator
    @Length(min = 10, max = 512)
    private String oldCustomerNumber;

    private String vatNumber;

//    private String mainActivitySubject;

    @NotNull(message = "Customer Detail Status is required; ")
    @CustomerStatusWhileCreatingValidator
    private CustomerDetailStatus customerDetailStatus;

    @Valid
    private BusinessCustomerDetails businessCustomerDetails;

    @Valid
    private PrivateCustomerDetails privateCustomerDetails;

    private Long ownershipFormId;

    private Long economicBranchId;

    private Long economicBranchNCEAId;

    @MainSubjectActivityValidator(value = "Main Subject Of Activity")
    private String mainSubjectOfActivity;

    private List<Long> segmentIds;

    @Valid
    @NotNull(message = "Customer address is required; ")
    private CustomerAddressRequest address;

    @Valid
    private CustomerBankingDetails bankingDetails;
    @Valid
    private List<CreateManagerRequest> managers;

    @Valid
    private List<CreateRelatedCustomerRequest> relatedCustomers;

    @Valid
    private List<CustomerOwnerRequest> owner;

    @Valid
    private List<CreateCommunicationDataRequest> communicationData;

    @Valid
    private List<CustomerAccountManagerRequest> accountManagers;

    public CreateCustomerRequest(EditCustomerRequest request) {
        this.customerType = request.getCustomerType();
        this.customerIdentifier = request.getCustomerIdentifier();
        this.foreign = request.getForeign();
        this.marketingConsent = request.getMarketingConsent();
        this.oldCustomerNumber = request.getOldCustomerNumber();
        this.vatNumber = request.getVatNumber();
        this.customerDetailStatus = request.getCustomerDetailStatus();
        this.businessCustomerDetails = request.getBusinessCustomerDetails();
        this.privateCustomerDetails = request.getPrivateCustomerDetails();
        this.ownershipFormId = request.getOwnershipFormId();
        this.economicBranchId = request.getEconomicBranchId();
        this.economicBranchNCEAId = request.getEconomicBranchNCEAId();
        this.mainSubjectOfActivity = request.getMainSubjectOfActivity();
        this.segmentIds = request.getSegmentIds();
        this.address = request.getAddress();
        this.bankingDetails = request.getBankingDetails();
        this.managers = getCreateManagersRequestList(request.getManagers());
        this.relatedCustomers = getRelatedCustomersAddRequest(request.getRelatedCustomers());
        this.communicationData = getCreateCommunicationDataRequest(request.getCommunicationData());
        this.owner = CustomerOwnerEditRequest.getCustomerOwnerAddRequest(request.getOwner());
        this.communicationData = getCreateCommunicationDataRequest(request.getCommunicationData());
        this.accountManagers = request.getAccountManagers();
    }
}
