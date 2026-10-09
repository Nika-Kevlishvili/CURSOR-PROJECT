package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.response.contract.productContract.BasicParametersResponse;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractResponse;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalContractUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SalesPortalContractStatusValidatorTest {

    private SalesPortalContractStatusValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SalesPortalContractStatusValidator();
    }

    @Test
    void validate_shouldRejectEnteredIntoForceToActiveInTerm() {
        SalesPortalContractUpdateRequest request = requestWithStatus(ContractDetailsStatus.ACTIVE_IN_TERM);
        ProductContractResponse existingContract = contractWithStatus(ContractDetailsStatus.ENTERED_INTO_FORCE);
        List<String> errors = new ArrayList<>();

        validator.validate(request, existingContract, errors);

        assertThat(errors).containsExactly(
                "contract.contractStatus-Status can not be changed to ACTIVE_IN_TERM;"
        );
    }

    @Test
    void validate_shouldAllowEnteredIntoForceToTerminated() {
        SalesPortalContractUpdateRequest request = requestWithStatus(ContractDetailsStatus.TERMINATED);
        ProductContractResponse existingContract = contractWithStatus(ContractDetailsStatus.ENTERED_INTO_FORCE);
        List<String> errors = new ArrayList<>();

        validator.validate(request, existingContract, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldAllowUnchangedStatus() {
        SalesPortalContractUpdateRequest request = requestWithStatus(ContractDetailsStatus.ENTERED_INTO_FORCE);
        ProductContractResponse existingContract = contractWithStatus(ContractDetailsStatus.ENTERED_INTO_FORCE);
        List<String> errors = new ArrayList<>();

        validator.validate(request, existingContract, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldAllowSignedToEnteredIntoForce() {
        SalesPortalContractUpdateRequest request = requestWithStatus(ContractDetailsStatus.ENTERED_INTO_FORCE);
        ProductContractResponse existingContract = contractWithStatus(ContractDetailsStatus.SIGNED);
        List<String> errors = new ArrayList<>();

        validator.validate(request, existingContract, errors);

        assertThat(errors).isEmpty();
    }

    private static SalesPortalContractUpdateRequest requestWithStatus(ContractDetailsStatus status) {
        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setContractStatus(status);
        request.setContractSubStatus(ContractDetailsSubStatus.DELIVERY);
        return request;
    }

    private static ProductContractResponse contractWithStatus(ContractDetailsStatus status) {
        BasicParametersResponse basicParameters = new BasicParametersResponse();
        basicParameters.setStatus(status);

        ProductContractResponse response = new ProductContractResponse();
        response.setBasicParameters(basicParameters);
        return response;
    }
}
