package phoenix.core.customer.model.request;


import lombok.Data;
import org.hibernate.validator.constraints.Length;
import phoenix.core.customer.model.customAnotations.customer.DigitsCommaAndSpaceValidator;
import phoenix.core.customer.model.customAnotations.customer.MainSubjectActivityValidator;
import phoenix.core.customer.model.customAnotations.customer.withValidators.CustomerEditValidators.*;
import phoenix.core.customer.model.customAnotations.customer.withValidators.CustomerStatusWhileCreatingValidator;
import phoenix.core.customer.model.enums.customer.CustomerDetailStatus;
import phoenix.core.customer.model.enums.customer.CustomerType;
import phoenix.core.customer.model.request.communicationData.EditCommunicationDataRequest;
import phoenix.core.customer.model.request.manager.EditManagerRequest;
import phoenix.core.customer.model.request.relatedCustomer.EditRelatedCustomerRequest;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;


@Data
@EditCustomerStatusIsNotPotential
@EditCustomerIdentifierValidator
@EditCustomerVatNumberValidator
@EditValidationsByCustomerType
@EditCustomerManagerValidator
public class EditCustomerRequest {

    @NotNull(message = "UpdateExistingVersion is required")
    Boolean updateExistingVersion;

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

    @Valid List<EditManagerRequest> managers;

    @Valid
    private List<EditRelatedCustomerRequest> relatedCustomers;

    @Valid
    private List<CustomerOwnerEditRequest> owner;

    @Valid
    private List<EditCommunicationDataRequest> communicationData;

    @Valid
    private List<CustomerAccountManagerRequest> accountManagers;

}
