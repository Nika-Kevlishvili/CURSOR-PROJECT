package bg.energo.phoenix.model.request.customer;

import bg.energo.phoenix.model.customAnotations.customer.BusinessActivityValidator;
import bg.energo.phoenix.model.customAnotations.customer.DigitsCommaAndSpaceValidator;
import bg.energo.phoenix.model.customAnotations.customer.manager.ManagerPersonalNumberValidator;
import bg.energo.phoenix.model.customAnotations.customer.withValidators.*;
import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.customer.communicationData.CreateCustomerCommunicationsRequest;
import bg.energo.phoenix.model.request.customer.customerAccountManager.CreateCustomerAccountManagerRequest;
import bg.energo.phoenix.model.request.customer.manager.CreateManagerRequest;
import bg.energo.phoenix.model.request.customer.relatedCustomer.CreateRelatedCustomerRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.util.List;

import static bg.energo.phoenix.model.request.customer.communicationData.CreateCustomerCommunicationsRequest.getCreateCommunicationDataRequest;
import static bg.energo.phoenix.model.request.customer.customerAccountManager.CreateCustomerAccountManagerRequest.getCreateCustomerAccountManagerRequests;
import static bg.energo.phoenix.model.request.customer.manager.CreateManagerRequest.getCreateManagersRequestList;
import static bg.energo.phoenix.model.request.customer.relatedCustomer.EditRelatedCustomerRequest.getRelatedCustomersAddRequest;

@Data
@CustomerStatusIsNotPotential
@CustomerAddressValidator
@ValidCustomerIdentifierOnCreate
@VatNumberValidator
@ValidationsByCustomerType
@ManagerValidator
@NoArgsConstructor
@AllArgsConstructor
@CreateCustomerLegalFormValidator
public class CreateCustomerRequest {
    @NotNull(message = "customerType-Customer Type is required;")
    private CustomerType customerType;

    private Boolean businessActivity;

    @Size(min = 1, max = 17, message = "customerIdentifier-Customer Identifier length should be in range [{min}:{max}] and not be blank;")
    @NotBlank(message = "customerIdentifier-Customer Identifier is required;")
    private String customerIdentifier;

    @NotNull(message = "foreign-Foreign is required;")
    private Boolean foreign;

    @NotNull(message = "marketingConsent-Marketing Consent is required;")
    private Boolean marketingConsent;

    private boolean preferCommunicationInEnglish;

    @DigitsCommaAndSpaceValidator
    @Length(min = 10, max = 512, message = "oldCustomerNumber-Old customer number length must be between 10 and 512;")
    private String oldCustomerNumber;

    private String vatNumber;

    @NotNull(message = "customerDetailStatus-Customer Detail Status is required;")
    @CustomerStatusWhileCreatingValidator
    private CustomerDetailStatus customerDetailStatus;

    @Valid
    private BusinessCustomerDetails businessCustomerDetails;

    @Valid
    private PrivateCustomerDetails privateCustomerDetails;

    private Long ownershipFormId;

    private Long economicBranchId;

    private Long economicBranchNCEAId;

    @BusinessActivityValidator(value = "mainSubjectOfActivity")
    private String mainSubjectOfActivity;

    private List<Long> segmentIds;

    @Valid
    private CustomerAddressRequest address;

    @Valid
    private CustomerBankingDetails bankingDetails;
    @Valid
    @ManagerPersonalNumberValidator
    private List<CreateManagerRequest> managers;

    @Valid
    private List<CreateRelatedCustomerRequest> relatedCustomers;

    @Valid
    private List<CustomerOwnerRequest> owner;

    @Valid
    private List<CreateCustomerCommunicationsRequest> communicationData;

    @CustomerAccountManagerValidator
    private List<CreateCustomerAccountManagerRequest> accountManagers;

    @CustomerAdditionalInfoValidator(value = "customerAdditionalInformation")
    @Length(min = 1, max = 4096, message = "customerAdditionalInformation-customer additional information length must be between 1 and 4096;")
    private String customerAdditionalInformation;

    public CreateCustomerRequest(EditCustomerRequest request) {
        this.customerType = request.getCustomerType();
        this.businessActivity = request.getBusinessActivity();
        this.customerIdentifier = request.getCustomerIdentifier();
        this.foreign = request.getForeign();
        this.setPreferCommunicationInEnglish(request.isPreferCommunicationInEnglish());
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
        this.owner = CustomerOwnerEditRequest.getCustomerOwnerAddRequest(request.getOwner());
        this.communicationData = getCreateCommunicationDataRequest(request.getCommunicationData());
        this.accountManagers = getCreateCustomerAccountManagerRequests(request.getAccountManagers());
    }
}
