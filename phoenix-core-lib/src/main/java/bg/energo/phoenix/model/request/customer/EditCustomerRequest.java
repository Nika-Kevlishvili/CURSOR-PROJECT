package bg.energo.phoenix.model.request.customer;


import bg.energo.phoenix.model.customAnotations.customer.BusinessActivityValidator;
import bg.energo.phoenix.model.customAnotations.customer.DigitsCommaAndSpaceValidator;
import bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerAccountManagerValidator;
import bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerAdditionalInfoValidator;
import bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerAddressValidatorInEdit;
import bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerEditValidators.*;
import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.customer.communicationData.EditCustomerCommunicationsRequest;
import bg.energo.phoenix.model.request.customer.customerAccountManager.EditCustomerAccountManagerRequest;
import bg.energo.phoenix.model.request.customer.manager.EditManagerRequest;
import bg.energo.phoenix.model.request.customer.relatedCustomer.EditRelatedCustomerRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;


/**
 * <h1>EditCustomerRequest</h1>
 * {@link #customerDetailsVersion} the version of the customer details to be used in edit process
 * {@link #updateExistingVersion} boolean value to change actual version or create a new version based on #customerDetailsVersion
 * {@link #customerType} Type of customer
 * {@link #businessActivity} boolean for business activity
 * {@link #customerIdentifier} Identifier of customer
 * {@link #foreign} boolean for customer foreign status
 * {@link #marketingConsent} boolean for marketing consent of customer
 * {@link #oldCustomerNumber} old customer number
 * {@link #vatNumber} vat number number
 * {@link #customerDetailStatus} customer detail status
 * {@link #businessCustomerDetails} business customer details
 * {@link #privateCustomerDetails} private customer details
 * {@link #ownershipFormId} nomenclature ownershipFormId
 * {@link #economicBranchId} nomenclature economicBranchId
 * {@link #economicBranchNCEAId} nomenclature economicBranchNCEAId
 * {@link #mainSubjectOfActivity} String main subject of activity
 * {@link #segmentIds} array of segment db ids
 * {@link #address} object for detailed address info
 * {@link #bankingDetails} object for customer detailed banking details
 * {@link #managers} array for customer managers details
 * {@link #relatedCustomers} array for related customers details
 * {@link #owner} array for customers owner details
 * {@link #communicationData} array for customers communication Data
 * {@link #accountManagers} array for customers account managers
 */
@Data
@EditCustomerStatusIsNotPotential
@CustomerAddressValidatorInEdit
@ValidCustomerIdentifierOnEdit
@EditCustomerVatNumberValidator
@EditValidationsByCustomerType
@EditCustomerManagerValidator
@EditCustomerLegalFormValidator
public class EditCustomerRequest {

    @NotNull(message = "customerDetailsVersion-customer detail version is required;")
    Long customerDetailsVersion;
    @NotNull(message = "updateExistingVersion-UpdateExistingVersion is required;")
    Boolean updateExistingVersion;

    @NotNull(message = "customerType-Customer Type is required;")
    private CustomerType customerType;

    private Boolean businessActivity;

    @Size(min = 1, max = 17, message = "customerIdentifier-Customer Identifier length should be in range [{min}:{max}] and not be blank;")
    @NotNull(message = "customerIdentifier-Customer Identifier is required;")
    private String customerIdentifier;

    @NotNull(message = "foreign-Foreign is required;")
    private Boolean foreign;

    @NotNull(message = "marketingConsent-Marketing Consent is required;")
    private Boolean marketingConsent;

    private boolean preferCommunicationInEnglish;

    @DigitsCommaAndSpaceValidator
    @Length(min = 10, max = 512, message = "oldCustomerNumber-Old Customer Number length must be between 10 and 512;")
    private String oldCustomerNumber;

    private String vatNumber;

    @NotNull(message = "customerDetailStatus-Customer Detail Status is required;")
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

    private List<CustomerEditContractRequest> customerEditContractRequests;

    @Valid
    @EditManagerPersonalNumberValidator
    private List<EditManagerRequest> managers;

    @Valid
    private List<EditRelatedCustomerRequest> relatedCustomers;

    @Valid
    private List<CustomerOwnerEditRequest> owner;

    @Valid
    private List<EditCustomerCommunicationsRequest> communicationData;

    @CustomerAccountManagerValidator
    private List<EditCustomerAccountManagerRequest> accountManagers;

    @CustomerAdditionalInfoValidator(value = "customerAdditionalInformation")
    @Length(min = 1, max = 4096, message = "customerAdditionalInformation-customer additional information length must be between 1 and 4096;")
    private String customerAdditionalInformation;

}
