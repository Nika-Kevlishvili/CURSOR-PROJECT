package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.contract.express.ExpressContractCustomerRequest;
import bg.energo.phoenix.model.request.contract.express.ExpressContractManagerRequest;
import bg.energo.phoenix.model.request.contract.express.ExpressContractPrivateCustomer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SalesPortalExpressContractKycValidatorTest {

    private SalesPortalExpressContractKycValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SalesPortalExpressContractKycValidator();
    }

    @Test
    void privateCustomer_rejectsOmittedKycPassed() {
        ExpressContractCustomerRequest request = privateCustomerRequest();
        request.getPrivateCustomerDetails().setKycPassed(null);

        List<String> errors = new ArrayList<>();
        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "customer.privateCustomerDetails.kycPassed-KYC passed flag is mandatory;"
        );
    }

    @Test
    void privateCustomer_acceptsExplicitFalseWithoutExpirationDate() {
        ExpressContractCustomerRequest request = privateCustomerRequest();
        request.getPrivateCustomerDetails().setKycPassed(false);
        request.getPrivateCustomerDetails().setKycExpirationDate(null);

        List<String> errors = new ArrayList<>();
        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void privateCustomer_acceptsExplicitTrueWithExpirationDate() {
        ExpressContractCustomerRequest request = privateCustomerRequest();
        request.getPrivateCustomerDetails().setKycPassed(true);
        request.getPrivateCustomerDetails().setKycExpirationDate(java.time.LocalDate.now().plusDays(1));

        List<String> errors = new ArrayList<>();
        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void legalEntity_rejectsManagerWithNullKycPassed() {
        ExpressContractCustomerRequest request = legalEntityRequest();
        request.getManagerRequests().get(0).setKycPassed(null);

        List<String> errors = new ArrayList<>();
        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "customer.managerRequests[0].kycPassed-KYC passed flag is mandatory;"
        );
    }

    @Test
    void legalEntity_acceptsExplicitManagerKycValues() {
        ExpressContractCustomerRequest request = legalEntityRequest();
        request.getManagerRequests().get(0).setKycPassed(false);
        request.getManagerRequests().get(0).setKycExpirationDate(null);

        List<String> errors = new ArrayList<>();
        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    private static ExpressContractCustomerRequest privateCustomerRequest() {
        ExpressContractPrivateCustomer privateCustomer = new ExpressContractPrivateCustomer();
        privateCustomer.setKycPassed(false);

        ExpressContractCustomerRequest request = new ExpressContractCustomerRequest();
        request.setCustomerType(CustomerType.PRIVATE_CUSTOMER);
        request.setPrivateCustomerDetails(privateCustomer);
        return request;
    }

    private static ExpressContractCustomerRequest legalEntityRequest() {
        ExpressContractManagerRequest manager = new ExpressContractManagerRequest();
        manager.setKycPassed(false);

        ExpressContractCustomerRequest request = new ExpressContractCustomerRequest();
        request.setCustomerType(CustomerType.LEGAL_ENTITY);
        request.setManagerRequests(List.of(manager));
        return request;
    }
}
