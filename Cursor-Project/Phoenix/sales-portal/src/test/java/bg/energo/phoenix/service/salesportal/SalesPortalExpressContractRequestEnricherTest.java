package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.model.enums.contract.express.ExpressContractType;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.product.product.ContractType;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.enums.product.term.terms.SupplyActivation;
import bg.energo.phoenix.model.request.contract.express.ExpressContractCustomerRequest;
import bg.energo.phoenix.model.request.contract.express.ExpressContractParameters;
import bg.energo.phoenix.model.request.contract.express.ExpressContractProductParametersRequest;
import bg.energo.phoenix.model.request.contract.express.ExpressContractRequest;
import bg.energo.phoenix.service.contract.expressContract.ExpressContractConsumptionService;
import bg.energo.phoenix.service.contract.expressContract.ExpressContractCustomerService;
import bg.energo.phoenix.service.contract.product.ProductContractProductParametersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesPortalExpressContractRequestEnricherTest {

    @Mock private ExpressContractCustomerService expressContractCustomerService;
    @Mock private ProductContractProductParametersService productParametersService;
    @Mock private ExpressContractConsumptionService expressContractConsumptionService;

    private SalesPortalExpressContractRequestEnricher enricher;

    @BeforeEach
    void setUp() {
        enricher = new SalesPortalExpressContractRequestEnricher(
                expressContractCustomerService,
                productParametersService,
                expressContractConsumptionService
        );
        lenient().when(productParametersService.fillMissingExpressProductParameters(any(), any(), any(), anyList()))
                .thenAnswer(invocation -> singleOptionProductParameters());
        lenient().when(expressContractConsumptionService.calculateFromPods(anyList()))
                .thenReturn(BigDecimal.valueOf(12));
    }

    @Test
    void enrich_setsSigningDateToday_whenMissing() {
        ExpressContractRequest request = minimalProductRequest();

        assertThat(enricher.enrich(request)).isEmpty();

        assertThat(request.getExpressContractParameters().getSigningDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void enrich_leavesSigningDateEmpty_whenSignedByCustomer() {
        ExpressContractRequest request = minimalProductRequest();
        request.getExpressContractParameters().setSubStatus(ContractDetailsSubStatus.SIGNED_BY_CUSTOMER);

        enricher.enrich(request);

        assertThat(request.getExpressContractParameters().getSigningDate()).isNull();
    }

    @Test
    void enrich_preservesProvidedSigningDate() {
        ExpressContractRequest request = minimalProductRequest();
        LocalDate provided = LocalDate.of(2026, 1, 15);
        request.getExpressContractParameters().setSigningDate(provided);

        enricher.enrich(request);

        assertThat(request.getExpressContractParameters().getSigningDate()).isEqualTo(provided);
    }

    @Test
    void enrich_calculatesEstimatedTotalConsumption_fromPods() {
        ExpressContractRequest request = minimalProductRequest();
        when(expressContractConsumptionService.calculateFromPods(List.of(5001L))).thenReturn(BigDecimal.valueOf(18));

        enricher.enrich(request);

        assertThat(request.getExpressContractParameters().getEstimatedTotalConsumption())
                .isEqualByComparingTo(BigDecimal.valueOf(18));
    }

    @Test
    void enrich_preservesProvidedEstimatedTotalConsumption() {
        ExpressContractRequest request = minimalProductRequest();
        request.getExpressContractParameters().setEstimatedTotalConsumption(BigDecimal.TEN);

        enricher.enrich(request);

        assertThat(request.getExpressContractParameters().getEstimatedTotalConsumption())
                .isEqualByComparingTo(BigDecimal.TEN);
        verify(expressContractConsumptionService, never()).calculateFromPods(anyList());
    }

    @Test
    void enrich_leavesConsumptionNull_whenNoConsumerPodData() {
        ExpressContractRequest request = minimalProductRequest();
        when(expressContractConsumptionService.calculateFromPods(List.of(5001L))).thenReturn(null);

        enricher.enrich(request);

        assertThat(request.getExpressContractParameters().getEstimatedTotalConsumption()).isNull();
    }

    @Test
    void enrich_defaultsProxyRequestToEmptyList() {
        ExpressContractRequest request = minimalProductRequest();
        request.setProxyRequest(null);

        enricher.enrich(request);

        assertThat(request.getProxyRequest()).isEmpty();
    }

    @Test
    void enrich_fillsCustomerProfile_forKnownCustomer() {
        ExpressContractRequest request = minimalProductRequest();
        when(expressContractCustomerService.fillProfileFromExistingCustomer(any(), anyList())).thenAnswer(invocation -> {
            ExpressContractCustomerRequest customer = invocation.getArgument(0);
            customer.setCustomerType(CustomerType.PRIVATE_CUSTOMER);
            customer.setForeign(false);
            return true;
        });

        enricher.enrich(request);

        assertThat(request.getCustomer().getCustomerType()).isEqualTo(CustomerType.PRIVATE_CUSTOMER);
        assertThat(request.getCustomer().getForeign()).isFalse();
        verify(expressContractCustomerService).fillProfileFromExistingCustomer(eq(request.getCustomer()), anyList());
    }

    @Test
    void enrich_collectsCustomerAndProductErrors_withoutThrowing() {
        ExpressContractRequest request = minimalProductRequest();
        when(expressContractCustomerService.fillProfileFromExistingCustomer(any(), anyList())).thenAnswer(invocation -> {
            List<String> errors = invocation.getArgument(1);
            errors.add("customer.privateCustomerDetails.kycExpirationDate-Stored KYC of the customer is expired;");
            return true;
        });
        when(productParametersService.fillMissingExpressProductParameters(isNull(), eq(1001L), eq(1L), anyList()))
                .thenAnswer(invocation -> {
                    List<String> errors = invocation.getArgument(3);
                    errors.add("productParameters.contractType-contractType can not be null;");
                    return new ExpressContractProductParametersRequest();
                });

        assertThat(enricher.enrich(request))
                .containsExactly(
                        "customer.privateCustomerDetails.kycExpirationDate-Stored KYC of the customer is expired;",
                        "productParameters.contractType-contractType can not be null;");
    }

    @Test
    void enrich_passesExistingProductParametersToFiller() {
        ExpressContractRequest request = minimalProductRequest();
        ExpressContractProductParametersRequest provided = singleOptionProductParameters();
        provided.setContractType(ContractType.COMBINED);
        request.setProductParameters(provided);
        when(productParametersService.fillMissingExpressProductParameters(eq(provided), eq(1001L), eq(1L), anyList()))
                .thenReturn(provided);

        enricher.enrich(request);

        ArgumentCaptor<ExpressContractProductParametersRequest> captor =
                ArgumentCaptor.forClass(ExpressContractProductParametersRequest.class);
        verify(productParametersService).fillMissingExpressProductParameters(captor.capture(), eq(1001L), eq(1L), anyList());
        assertThat(captor.getValue().getContractType()).isEqualTo(ContractType.COMBINED);
    }

    @Test
    void enrich_skipsProductParameters_forServiceContract() {
        ExpressContractRequest request = minimalProductRequest();
        request.setExpressContractType(ExpressContractType.SERVICE);

        enricher.enrich(request);

        verify(productParametersService, never()).fillMissingExpressProductParameters(any(), any(), any(), anyList());
        verify(expressContractConsumptionService, never()).calculateFromPods(anyList());
        assertThat(request.getExpressContractParameters().getSigningDate()).isEqualTo(LocalDate.now());
    }

    private static ExpressContractRequest minimalProductRequest() {
        ExpressContractRequest request = new ExpressContractRequest();
        request.setExpressContractType(ExpressContractType.PRODUCT);
        ExpressContractParameters parameters = new ExpressContractParameters();
        parameters.setProductId(1001L);
        parameters.setProductVersionId(1L);
        parameters.setSubStatus(ContractDetailsSubStatus.SIGNED_BY_BOTH_SIDES);
        request.setExpressContractParameters(parameters);
        ExpressContractCustomerRequest customer = new ExpressContractCustomerRequest();
        customer.setIdentifier("19900101AB12");
        request.setCustomer(customer);
        request.setPodDetailIds(List.of(5001L));
        return request;
    }

    private static ExpressContractProductParametersRequest singleOptionProductParameters() {
        ExpressContractProductParametersRequest params = new ExpressContractProductParametersRequest();
        params.setContractType(ContractType.COMBINED);
        params.setProductContractTermId(4001L);
        params.setPaymentGuarantee(PaymentGuarantee.NO);
        params.setInvoicePaymentTermId(4002L);
        params.setEntryIntoForce(ContractEntryIntoForce.SIGNING);
        params.setStartOfContractInitialTerm(StartOfContractInitialTerm.SIGNING);
        params.setSupplyActivation(SupplyActivation.FIRST_DAY_OF_MONTH);
        params.setContractFormulas(new ArrayList<>());
        params.setInterimAdvancePayments(new ArrayList<>());
        return params;
    }
}
