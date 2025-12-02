package bg.energo.phoenix.model.request.contract.express;

import bg.energo.phoenix.model.customAnotations.contract.express.ExpressContractCommunicationDataValidator;
import bg.energo.phoenix.model.customAnotations.contract.express.ExpressCustomerIdentifierValidator;
import bg.energo.phoenix.model.customAnotations.contract.express.ExpressCustomerTypeValidator;
import bg.energo.phoenix.model.customAnotations.contract.express.ExpressCustomerVatNumberValidator;
import bg.energo.phoenix.model.customAnotations.customer.BusinessActivityValidator;
import bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerAddressTypeValidator;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.customer.CustomerAddressRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@ExpressCustomerIdentifierValidator
@ExpressCustomerTypeValidator
@ExpressCustomerVatNumberValidator
@ExpressContractCommunicationDataValidator
public class ExpressContractCustomerRequest {
    @NotNull(message = "customer.identifier-customerIdentifier is mandatory;")
    private String identifier;

    @NotNull(message = "customer.customerType-Customer Type is required;")
    private CustomerType customerType;

    private boolean foreign;
    private boolean preferCommunicationInEnglish;
    private boolean consentToMarketingCommunication;
    private String vatNumber;

    private Long ownershipFormId;
    private Long economicBranchCiId;
    @BusinessActivityValidator(value = "customer.mainSubjectOfActivity")
    private String mainActivitySubject;
    private Boolean businessActivity;
    @Valid
    private ExpressContractBusinessCustomer businessCustomerDetails;
    @Valid
    private ExpressContractPrivateCustomer privateCustomerDetails;

    private String businessActivityName;
    private String businessActivityNameTransl;
    @NotNull(message = "customer.customerSegments-At least 1 segment should be provided;")
    @Size(min = 1, message = "customer.customerSegments-At least 1 segment should be provided;")
    private Set<Long> customerSegments;
    private List<@Valid ExpressContractManagerRequest> managerRequests;
    @NotNull(message = "customer.communications-At least 1 communications should be provided;")
    @Size(min = 1, message = "customer.communications-At least 1 communications should be provided;")
    private List<@Valid ExpressContractCommunicationsRequest> communications;
    @NotNull(message = "customer.address-address is required;")
    @Valid
    @CustomerAddressTypeValidator(message = "customer.address.foreign")
    private CustomerAddressRequest address;

    @JsonIgnore
    @AssertTrue(message = "businessCustomerDetails.legalFormId-legalFormId  is mandatory, when customer type is legal;")
    public boolean isValidLegalFormId() {
        if (this.customerType.equals(CustomerType.LEGAL_ENTITY) && this.businessCustomerDetails != null && this.businessCustomerDetails.getLegalFormId() == null) {
            return false;
        }
        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "businessCustomerDetails.legalFormTransId-legalFormTransId is mandatory, when customer type is legal;")
    public boolean isValidLegalFormTransId() {
        if (this.customerType.equals(CustomerType.LEGAL_ENTITY) && this.businessCustomerDetails != null && this.businessCustomerDetails.getLegalFormTransId() == null) {
            return false;
        }
        return true;
    }
}
