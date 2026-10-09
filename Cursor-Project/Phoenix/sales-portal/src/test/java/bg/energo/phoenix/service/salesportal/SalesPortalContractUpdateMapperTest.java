package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.Manager;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.request.contract.product.ProductContractUpdateRequest;
import bg.energo.phoenix.model.response.contract.productContract.BasicParametersResponse;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractResponse;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractVersionWithStatusResponse;
import bg.energo.phoenix.model.response.proxy.ProxyManagersResponse;
import bg.energo.phoenix.model.response.proxy.ProxyResponse;
import bg.energo.phoenix.repository.contract.proxy.ProxyManagersRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.ManagerRepository;
import bg.energo.phoenix.repository.product.product.ProductAdditionalParamsRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.service.customer.CustomerMapperService;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalContractUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesPortalContractUpdateMapperTest {

    private static final String OLD_CUSTOMER_UIC = "1111111111";
    private static final String NEW_CUSTOMER_UIC = "9808282755";
    private static final Long OLD_CUSTOMER_ID = 100L;
    private static final Long NEW_CUSTOMER_ID = 200L;
    private static final Long OLD_CUSTOMER_DETAIL_ID = 1000L;
    private static final Long NEW_CUSTOMER_DETAIL_ID = 2000L;
    private static final Long OLD_MANAGER_ID = 28442L;
    private static final Long NEW_MANAGER_ID = 30001L;

    @Mock private CustomerMapperService customerMapperService;
    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerDetailsRepository customerDetailsRepository;
    @Mock private ManagerRepository managerRepository;
    @Mock private ProductDetailsRepository productDetailsRepository;
    @Mock private ProductAdditionalParamsRepository productAdditionalParamsRepository;
    @Mock private InvoicePaymentTermsRepository invoicePaymentTermsRepository;
    @Mock private ProxyManagersRepository proxyManagersRepository;
    @Mock private SalesPortalContractPodService salesPortalContractPodService;

    private SalesPortalContractUpdateMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SalesPortalContractUpdateMapper(
                customerMapperService,
                customerRepository,
                customerDetailsRepository,
                managerRepository,
                productDetailsRepository,
                productAdditionalParamsRepository,
                invoicePaymentTermsRepository,
                proxyManagersRepository,
                salesPortalContractPodService
        );
    }

    @Test
    void toProductContractUpdateRequest_shouldUseSelectedVersionStartDateNotContractCreationDate() {
        LocalDate creationDate = LocalDate.of(2026, 6, 10);
        LocalDate version2StartDate = LocalDate.of(2026, 6, 26);
        long version2DetailId = 5002L;

        BasicParametersResponse basic = new BasicParametersResponse();
        basic.setCreationDate(creationDate);
        basic.setVersionId(2);

        ProductContractVersionWithStatusResponse version1 = new ProductContractVersionWithStatusResponse();
        version1.setId(5001L);
        version1.setVersionId(1);
        version1.setStartDate(creationDate);

        ProductContractVersionWithStatusResponse version2 = new ProductContractVersionWithStatusResponse();
        version2.setId(version2DetailId);
        version2.setVersionId(2);
        version2.setStartDate(version2StartDate);

        ProductContractResponse existingContract = new ProductContractResponse();
        existingContract.setBasicParameters(basic);
        existingContract.setContractDetailId(version2DetailId);
        existingContract.setVersions(List.of(version1, version2));

        ProductContractUpdateRequest mappedRequest = new ProductContractUpdateRequest();
        when(customerMapperService.mapProductContractUpdateRequest(eq(existingContract), any(), isNull()))
                .thenReturn(mappedRequest);

        List<String> errors = new ArrayList<>();
        ProductContractUpdateRequest result = mapper.toProductContractUpdateRequest(
                new SalesPortalContractUpdateRequest(),
                existingContract,
                errors
        );

        ArgumentCaptor<LocalDate> startDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        assertThat(errors).isEmpty();
        assertThat(result).isSameAs(mappedRequest);
        verify(customerMapperService).mapProductContractUpdateRequest(
                eq(existingContract),
                startDateCaptor.capture(),
                isNull()
        );
        assertThat(startDateCaptor.getValue()).isEqualTo(version2StartDate);
    }

    @Test
    void validateProxyManagersForRequest_shouldRejectOldCustomerManagerWhenCustomerIsSwapped() {
        ProductContractResponse existingContract = contractForCustomer(OLD_CUSTOMER_ID, OLD_CUSTOMER_DETAIL_ID);
        SalesPortalContractUpdateRequest request = contractUpdateWithProxyManager(NEW_CUSTOMER_UIC, OLD_MANAGER_ID);

        stubNewLegalEntityCustomer();
        when(managerRepository.findByIdAndStatus(OLD_MANAGER_ID, Status.ACTIVE))
                .thenReturn(Optional.of(manager(OLD_MANAGER_ID, OLD_CUSTOMER_DETAIL_ID)));

        List<String> errors = new ArrayList<>();
        mapper.validateProxyManagersForRequest(request, existingContract, errors);

        assertThat(errors).anyMatch(error -> error.contains("does not belong to customer with UIC " + NEW_CUSTOMER_UIC));
    }

    @Test
    void validateProxyManagersForRequest_shouldAcceptManagerFromRequestCustomer() {
        ProductContractResponse existingContract = contractForCustomer(OLD_CUSTOMER_ID, OLD_CUSTOMER_DETAIL_ID);
        SalesPortalContractUpdateRequest request = contractUpdateWithProxyManager(NEW_CUSTOMER_UIC, NEW_MANAGER_ID);

        stubNewLegalEntityCustomer();
        when(managerRepository.findByIdAndStatus(NEW_MANAGER_ID, Status.ACTIVE))
                .thenReturn(Optional.of(manager(NEW_MANAGER_ID, NEW_CUSTOMER_DETAIL_ID)));

        List<String> errors = new ArrayList<>();
        mapper.validateProxyManagersForRequest(request, existingContract, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validateProxyManagersForRequest_shouldRejectPreservedManagersWhenCustomerIsSwappedWithoutProxyBlock() {
        ProductContractResponse existingContract = contractForCustomer(OLD_CUSTOMER_ID, OLD_CUSTOMER_DETAIL_ID);
        existingContract.getBasicParameters().setProxy(List.of(proxyWithManager(OLD_MANAGER_ID)));

        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setCustomerUic(NEW_CUSTOMER_UIC);

        stubNewLegalEntityCustomer();
        when(managerRepository.findByIdAndStatus(OLD_MANAGER_ID, Status.ACTIVE))
                .thenReturn(Optional.of(manager(OLD_MANAGER_ID, OLD_CUSTOMER_DETAIL_ID)));

        List<String> errors = new ArrayList<>();
        mapper.validateProxyManagersForRequest(request, existingContract, errors);

        assertThat(errors).anyMatch(error -> error.contains("does not belong to customer with UIC " + NEW_CUSTOMER_UIC));
    }

    private void stubNewLegalEntityCustomer() {
        Customer newCustomer = new Customer();
        newCustomer.setId(NEW_CUSTOMER_ID);
        newCustomer.setCustomerType(CustomerType.LEGAL_ENTITY);
        when(customerRepository.findByIdentifierAndStatus(NEW_CUSTOMER_UIC, CustomerStatus.ACTIVE))
                .thenReturn(Optional.of(newCustomer));

        CustomerDetails newDetails = new CustomerDetails();
        newDetails.setId(NEW_CUSTOMER_DETAIL_ID);
        newDetails.setCustomerId(NEW_CUSTOMER_ID);
        when(customerDetailsRepository.findTopByCustomerIdOrderByIdDesc(NEW_CUSTOMER_ID))
                .thenReturn(Optional.of(newDetails));
    }

    private static ProductContractResponse contractForCustomer(Long customerId, Long customerDetailId) {
        BasicParametersResponse basic = new BasicParametersResponse();
        basic.setCustomerId(customerId);
        basic.setCustomerDetailId(customerDetailId);

        ProductContractResponse response = new ProductContractResponse();
        response.setBasicParameters(basic);
        return response;
    }

    private static SalesPortalContractUpdateRequest contractUpdateWithProxyManager(String customerUic, Long managerId) {
        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setCustomerUic(customerUic);

        SalesPortalContractUpdateRequest.ProxyUpdate proxy = new SalesPortalContractUpdateRequest.ProxyUpdate();
        proxy.setProxyId(1237L);
        proxy.setProxyName("Proxy");
        proxy.setManagerIds(Set.of(managerId));
        request.setProxy(proxy);
        return request;
    }

    private static ProxyResponse proxyWithManager(Long managerId) {
        ProxyManagersResponse proxyManager = new ProxyManagersResponse();
        proxyManager.setCustomerManagerId(managerId);

        ProxyResponse proxy = new ProxyResponse();
        proxy.setId(1237L);
        proxy.setProxyManagers(List.of(proxyManager));
        return proxy;
    }

    private static Manager manager(Long id, Long customerDetailId) {
        Manager manager = new Manager();
        manager.setId(id);
        manager.setCustomerDetailId(customerDetailId);
        return manager;
    }
}
