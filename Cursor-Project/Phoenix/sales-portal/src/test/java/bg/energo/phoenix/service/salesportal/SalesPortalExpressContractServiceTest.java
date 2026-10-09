package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.enums.contract.express.ExpressContractType;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.request.contract.express.ExpressContractCustomerRequest;
import bg.energo.phoenix.model.request.contract.express.ExpressContractParameters;
import bg.energo.phoenix.model.request.contract.express.ExpressContractRequest;
import bg.energo.phoenix.service.contract.expressContract.ExpressContractService;
import bg.energo.phoenix.service.salesportal.enums.SystemSoureEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesPortalExpressContractServiceTest {

    @Mock private SalesPortalExpressContractRequestEnricher requestEnricher;
    @Mock private ExpressContractService expressContractService;
    @Mock private Validator validator;

    private SalesPortalExpressContractService service;

    @BeforeEach
    void setUp() {
        service = new SalesPortalExpressContractService(requestEnricher, expressContractService, validator);
        when(requestEnricher.enrich(any())).thenReturn(new ArrayList<>());
        when(validator.validate(any(ExpressContractRequest.class))).thenReturn(Set.of());
    }

    @ParameterizedTest
    @EnumSource(value = SystemSoureEnum.class, names = {"PHOENIX", "SALES_PORTAL", "SELF_SERVICE_PORTAL", "VCOK"})
    void create_acceptsAllowedSources(SystemSoureEnum portalSource) {
        ExpressContractRequest request = request();
        request.getCustomer().setSystemSourceId(portalSource);
        when(expressContractService.create(request)).thenReturn(4242L);

        assertThat(service.create(request)).isEqualTo(4242L);
        verify(requestEnricher).enrich(request);
    }

    @Test
    void create_rejectsMissingSystemSourceId() {
        ExpressContractRequest request = request();
        request.getCustomer().setSystemSourceId(null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentsProvidedException.class)
                .hasMessage("customer.systemSourceId-systemSourceId is mandatory;");

        verify(expressContractService, never()).create(any());
    }

    @Test
    void create_rejectsMissingSubStatus() {
        ExpressContractRequest request = request();
        request.getExpressContractParameters().setSubStatus(null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentsProvidedException.class)
                .hasMessage("expressContractParameters.subStatus-subStatus is mandatory;");

        verify(expressContractService, never()).create(any());
    }

    @Test
    void create_rejectsUnsupportedSubStatus() {
        ExpressContractRequest request = request();
        request.getExpressContractParameters().setSubStatus(ContractDetailsSubStatus.DRAFT);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentsProvidedException.class)
                .hasMessage("expressContractParameters.subStatus-Only SIGNED_BY_BOTH_SIDES and SIGNED_BY_CUSTOMER are allowed;");

        verify(expressContractService, never()).create(any());
    }

    @Test
    void create_acceptsSignedByCustomer() {
        ExpressContractRequest request = request();
        request.getExpressContractParameters().setSubStatus(ContractDetailsSubStatus.SIGNED_BY_CUSTOMER);
        when(expressContractService.create(request)).thenReturn(4242L);

        assertThat(service.create(request)).isEqualTo(4242L);
    }

    @Test
    void create_reportsEnrichmentAndConstraintErrorsTogether() {
        ExpressContractRequest request = request();
        when(requestEnricher.enrich(request)).thenReturn(new ArrayList<>(
                List.of("productParameters.contractType-contractType can not be null;")));
        Set<ConstraintViolation<ExpressContractRequest>> violations = Set.of(violation("customer.foreign-foreign is required;"));
        when(validator.validate(request)).thenReturn(violations);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentsProvidedException.class)
                .hasMessageContaining("productParameters.contractType-contractType can not be null;")
                .hasMessageContaining("customer.foreign-foreign is required;");

        verify(expressContractService, never()).create(any());
    }

    @Test
    void create_doesNotFailOnMissingCustomer_letsValidationReportIt() {
        ExpressContractRequest request = request();
        request.setCustomer(null);
        Set<ConstraintViolation<ExpressContractRequest>> violations = Set.of(violation("customer-Customer can not be null!;"));
        when(validator.validate(request)).thenReturn(violations);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentsProvidedException.class)
                .hasMessage("customer-Customer can not be null!;");
    }

    @SuppressWarnings("unchecked")
    private static ConstraintViolation<ExpressContractRequest> violation(String message) {
        ConstraintViolation<ExpressContractRequest> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn(message);
        return violation;
    }

    private static ExpressContractRequest request() {
        ExpressContractRequest request = new ExpressContractRequest();
        request.setExpressContractType(ExpressContractType.PRODUCT);
        ExpressContractParameters parameters = new ExpressContractParameters();
        parameters.setProductId(1001L);
        parameters.setProductVersionId(1L);
        parameters.setSubStatus(ContractDetailsSubStatus.SIGNED_BY_BOTH_SIDES);
        request.setExpressContractParameters(parameters);
        ExpressContractCustomerRequest customer = new ExpressContractCustomerRequest();
        customer.setIdentifier("19900101AB12");
        customer.setSystemSourceId(SystemSoureEnum.SALES_PORTAL);
        request.setCustomer(customer);
        request.setPodDetailIds(List.of(5001L));
        return request;
    }
}
