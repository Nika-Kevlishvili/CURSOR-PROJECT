package bg.energo.phoenix.controller.salesPortal;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.model.response.customer.salesportal.SalesPortalCustomerResponse;
import bg.energo.phoenix.model.response.customer.search.CustomerSearchByIdentifierResponse;
import bg.energo.phoenix.salesportal.controller.salesPortal.SalesPortalCustomerController;
import bg.energo.phoenix.service.salesportal.SalesPortalCustomerUpdateService;
import bg.energo.phoenix.service.salesportal.SalesPortalService;
import bg.energo.phoenix.service.salesportal.customer.SalesPortalCustomerLookupService;
import bg.energo.phoenix.service.salesportal.customer.lookup.CustomerLookupCriteria;
import bg.energo.phoenix.service.salesportal.customer.lookup.LookupMethod;
import bg.energo.phoenix.service.salesportal.model.SalesPortalCustomerUpdateResponse;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalCustomerUpdateRequest;
import bg.energo.phoenix.service.salesportal.requests.CustomerListByCoordinatesRequest;
import bg.energo.phoenix.service.salesportal.model.CustomerListByCoordinatesProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesPortalCustomerControllerTest {

    @Mock
    private SalesPortalService salesPortalService;

    @Mock
    private SalesPortalCustomerUpdateService salesPortalCustomerUpdateService;

    @Mock
    private SalesPortalCustomerLookupService salesPortalCustomerLookupService;

    private SalesPortalCustomerController salesPortalCustomerController;

    @BeforeEach
    void setUp() {
        this.salesPortalCustomerController = new SalesPortalCustomerController(
                salesPortalService, salesPortalCustomerUpdateService, salesPortalCustomerLookupService);
    }

    @Test
    void searchCustomerByIdentifier_shouldReturnOkAndResponseBody_whenServiceReturnsCustomer() {
        String identifier = "CUST-12345";
        CustomerSearchByIdentifierResponse expected = new CustomerSearchByIdentifierResponse();

        when(salesPortalService.searchCustomerByIdentifier(identifier, null)).thenReturn(expected);

        ResponseEntity<CustomerSearchByIdentifierResponse> actual =
                salesPortalCustomerController.searchCustomerByIdentifier(identifier, null);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalService).searchCustomerByIdentifier(identifier, null);
    }

    @Test
    void searchCustomerByIdentifier_shouldReturnOkWithNullBody_whenServiceReturnsNull() {
        String identifier = "CUST-NULL";

        when(salesPortalService.searchCustomerByIdentifier(identifier, null)).thenReturn(null);

        ResponseEntity<CustomerSearchByIdentifierResponse> actual =
                salesPortalCustomerController.searchCustomerByIdentifier(identifier, null);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isNull();
        verify(salesPortalService).searchCustomerByIdentifier(identifier, null);
    }

    @Test
    void searchCustomerByIdentifier_shouldPropagateNotFound_whenServiceThrowsDomainEntityNotFoundException() {
        String identifier = "MISSING-CUSTOMER";

        when(salesPortalService.searchCustomerByIdentifier(identifier, null))
                .thenThrow(new DomainEntityNotFoundException("Customer not found"));

        DomainEntityNotFoundException actual = assertThrows(
                DomainEntityNotFoundException.class,
                () -> salesPortalCustomerController.searchCustomerByIdentifier(identifier, null)
        );

        assertThat(actual.getMessage()).isEqualTo("Customer not found");
        verify(salesPortalService).searchCustomerByIdentifier(identifier, null);
    }

    @Test
    void searchCustomerByIdentifier_shouldPropagateClientException_whenServiceThrowsValidationFailure() {
        String identifier = "INVALID";
        ClientException expected = new ClientException(
                "Invalid identifier;",
                ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED
        );

        when(salesPortalService.searchCustomerByIdentifier(identifier, null)).thenThrow(expected);

        ClientException actual = assertThrows(
                ClientException.class,
                () -> salesPortalCustomerController.searchCustomerByIdentifier(identifier, null)
        );

        assertThat(actual).isSameAs(expected);
        verify(salesPortalService).searchCustomerByIdentifier(identifier, null);
    }

    @Test
    void searchCustomerByIdentifier_shouldPropagateUnexpectedException_whenServiceThrowsRuntimeException() {
        String identifier = "CUST-EX";
        RuntimeException expected = new RuntimeException("Unexpected failure");

        when(salesPortalService.searchCustomerByIdentifier(identifier, null)).thenThrow(expected);

        RuntimeException actual = assertThrows(
                RuntimeException.class,
                () -> salesPortalCustomerController.searchCustomerByIdentifier(identifier, null)
        );

        assertThat(actual).isSameAs(expected);
        verify(salesPortalService).searchCustomerByIdentifier(identifier, null);
    }

    @Test
    void searchCustomerByIdentifier_shouldReturnOkWithEnglishLanguage_whenLanguageIsEnglish() {
        String identifier = "CUST-EN";
        CustomerSearchByIdentifierResponse expected = new CustomerSearchByIdentifierResponse();

        when(salesPortalService.searchCustomerByIdentifier(identifier, Language.ENGLISH)).thenReturn(expected);

        ResponseEntity<CustomerSearchByIdentifierResponse> actual =
                salesPortalCustomerController.searchCustomerByIdentifier(identifier, Language.ENGLISH);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalService).searchCustomerByIdentifier(identifier, Language.ENGLISH);
    }

    @Test
    void searchCustomerByIdentifier_shouldReturnOkWithBulgarianLanguage_whenLanguageIsBulgarian() {
        String identifier = "CUST-BG";
        CustomerSearchByIdentifierResponse expected = new CustomerSearchByIdentifierResponse();

        when(salesPortalService.searchCustomerByIdentifier(identifier, Language.BULGARIAN)).thenReturn(expected);

        ResponseEntity<CustomerSearchByIdentifierResponse> actual =
                salesPortalCustomerController.searchCustomerByIdentifier(identifier, Language.BULGARIAN);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalService).searchCustomerByIdentifier(identifier, Language.BULGARIAN);
    }

    // ─────────────────────── GET /customer/{uic} (PHN-2203) ───────────────────────

    @Test
    void getCustomerByUic_shouldReturnOkAndBody_whenServiceReturnsCustomer() {
        String uic = "131134023";
        SalesPortalCustomerResponse expected = new SalesPortalCustomerResponse();

        when(salesPortalCustomerLookupService.getSingleCustomer(
                LookupMethod.UIC, new CustomerLookupCriteria(uic, null), Language.BULGARIAN)).thenReturn(expected);

        ResponseEntity<SalesPortalCustomerResponse> actual =
                salesPortalCustomerController.getCustomerByUic(uic, Language.BULGARIAN);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalCustomerLookupService).getSingleCustomer(
                LookupMethod.UIC, new CustomerLookupCriteria(uic, null), Language.BULGARIAN);
    }

    @Test
    void getCustomerByUic_shouldPassEnglishLanguageThrough() {
        String uic = "131134023";
        SalesPortalCustomerResponse expected = new SalesPortalCustomerResponse();

        when(salesPortalCustomerLookupService.getSingleCustomer(
                LookupMethod.UIC, new CustomerLookupCriteria(uic, null), Language.ENGLISH)).thenReturn(expected);

        ResponseEntity<SalesPortalCustomerResponse> actual =
                salesPortalCustomerController.getCustomerByUic(uic, Language.ENGLISH);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalCustomerLookupService).getSingleCustomer(
                LookupMethod.UIC, new CustomerLookupCriteria(uic, null), Language.ENGLISH);
    }

    @Test
    void getCustomerByUic_shouldPropagateNotFound_whenNoCustomerMatches() {
        String uic = "000000000";

        when(salesPortalCustomerLookupService.getSingleCustomer(
                LookupMethod.UIC, new CustomerLookupCriteria(uic, null), Language.BULGARIAN))
                .thenThrow(new DomainEntityNotFoundException("No active customer found for UIC lookup"));

        DomainEntityNotFoundException actual = assertThrows(
                DomainEntityNotFoundException.class,
                () -> salesPortalCustomerController.getCustomerByUic(uic, Language.BULGARIAN)
        );

        assertThat(actual.getMessage()).isEqualTo("No active customer found for UIC lookup");
        verify(salesPortalCustomerLookupService).getSingleCustomer(
                LookupMethod.UIC, new CustomerLookupCriteria(uic, null), Language.BULGARIAN);
    }

    // ─────────────────────── GET /customer (unified, PHN-2202) ───────────────────────

    @Test
    void getCustomer_unified_shouldDelegateAndReturnOk() {
        SalesPortalCustomerResponse expected = new SalesPortalCustomerResponse();

        when(salesPortalCustomerLookupService.getSingleCustomer(
                "112233", null, null, null, null, null, null, null, null, Language.BULGARIAN))
                .thenReturn(expected);

        ResponseEntity<SalesPortalCustomerResponse> actual =
                salesPortalCustomerController.getCustomer(
                        "112233", null, null, null, null, null, null, null, null, Language.BULGARIAN);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalCustomerLookupService).getSingleCustomer(
                "112233", null, null, null, null, null, null, null, null, Language.BULGARIAN);
    }

    @Test
    void getCustomer_unified_shouldDelegateNameSearchWithPhone() {
        SalesPortalCustomerResponse expected = new SalesPortalCustomerResponse();

        when(salesPortalCustomerLookupService.getSingleCustomer(
                null, null, null, null, "Иван", "Иванов", "0888123456", null, null, Language.BULGARIAN))
                .thenReturn(expected);

        ResponseEntity<SalesPortalCustomerResponse> actual =
                salesPortalCustomerController.getCustomer(
                        null, null, null, null, "Иван", "Иванов", "0888123456", null, null, Language.BULGARIAN);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalCustomerLookupService).getSingleCustomer(
                null, null, null, null, "Иван", "Иванов", "0888123456", null, null, Language.BULGARIAN);
    }

    @Test
    void getCustomer_unified_shouldDelegateNameSearchWithEmailAndBirthDate() {
        SalesPortalCustomerResponse expected = new SalesPortalCustomerResponse();
        LocalDate birthDate = LocalDate.of(1985, 6, 15);

        when(salesPortalCustomerLookupService.getSingleCustomer(
                null, null, null, null, "Мария", null, null, "maria@example.com", birthDate, Language.ENGLISH))
                .thenReturn(expected);

        ResponseEntity<SalesPortalCustomerResponse> actual =
                salesPortalCustomerController.getCustomer(
                        null, null, null, null, "Мария", null, null, "maria@example.com", birthDate, Language.ENGLISH);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalCustomerLookupService).getSingleCustomer(
                null, null, null, null, "Мария", null, null, "maria@example.com", birthDate, Language.ENGLISH);
    }

    @Test
    void getCustomer_unified_shouldPropagateClientException_whenServiceThrows() {
        ClientException expected = new ClientException(
                "Provide one of: uic, customerNumber, contractNumber, invoiceNumber, name.",
                ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);

        when(salesPortalCustomerLookupService.getSingleCustomer(
                null, null, null, null, null, null, null, null, null, Language.BULGARIAN))
                .thenThrow(expected);

        ClientException actual = assertThrows(
                ClientException.class,
                () -> salesPortalCustomerController.getCustomer(
                        null, null, null, null, null, null, null, null, null, Language.BULGARIAN));

        assertThat(actual).isSameAs(expected);
    }

    // ─────────────────────── PUT /customer (PHN-2208) ───────────────────────

    @Test
    void updateCustomer_shouldDelegateAndReturnSuccess() {
        SalesPortalCustomerUpdateRequest request = new SalesPortalCustomerUpdateRequest();
        request.setCustomerId(12345L);
        request.setCustomerVersionId(47393L);
        request.setCustomerPnOrUic("8889101010");
        SalesPortalCustomerUpdateResponse expected = SalesPortalCustomerUpdateResponse.success();

        when(salesPortalCustomerUpdateService.updateCustomer(request)).thenReturn(expected);

        ResponseEntity<SalesPortalCustomerUpdateResponse> actual = salesPortalCustomerController.updateCustomer(request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalCustomerUpdateService).updateCustomer(request);
    }

    @Test
    void updateCustomer_shouldPropagateClientException() {
        SalesPortalCustomerUpdateRequest request = new SalesPortalCustomerUpdateRequest();
        request.setCustomerId(12345L);
        ClientException expected = new ClientException(
                "customerId-Customer not found, ID: 12345;",
                ErrorCode.DOMAIN_ENTITY_NOT_FOUND
        );

        when(salesPortalCustomerUpdateService.updateCustomer(request)).thenThrow(expected);

        ClientException actual = assertThrows(
                ClientException.class,
                () -> salesPortalCustomerController.updateCustomer(request)
        );

        assertThat(actual).isSameAs(expected);
        verify(salesPortalCustomerUpdateService).updateCustomer(request);
    }

    // ─────────────────────── POST /customer/search-by-coordinates ───────────────────────

    @Test
    void searchCustomersByCoordinates_shouldReturnOkAndPageBody() {
        CustomerListByCoordinatesRequest request = new CustomerListByCoordinatesRequest();
        Page<CustomerListByCoordinatesProjection> expected = new PageImpl<>(List.of());

        when(salesPortalService.getCustomerListByCoordinates(request)).thenReturn(expected);

        ResponseEntity<Page<CustomerListByCoordinatesProjection>> actual =
                salesPortalCustomerController.searchCustomersByCoordinates(request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalService).getCustomerListByCoordinates(request);
    }

    @Test
    void searchCustomersByCoordinates_shouldReturnOkWithNullBody_whenServiceReturnsNull() {
        CustomerListByCoordinatesRequest request = new CustomerListByCoordinatesRequest();

        when(salesPortalService.getCustomerListByCoordinates(request)).thenReturn(null);

        ResponseEntity<Page<CustomerListByCoordinatesProjection>> actual =
                salesPortalCustomerController.searchCustomersByCoordinates(request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isNull();
        verify(salesPortalService).getCustomerListByCoordinates(request);
    }

    @Test
    void searchCustomersByCoordinates_shouldPropagateClientException_whenServiceThrows() {
        CustomerListByCoordinatesRequest request = new CustomerListByCoordinatesRequest();
        ClientException expected = new ClientException("Invalid coordinates;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);

        when(salesPortalService.getCustomerListByCoordinates(request)).thenThrow(expected);

        ClientException actual = assertThrows(
                ClientException.class,
                () -> salesPortalCustomerController.searchCustomersByCoordinates(request)
        );

        assertThat(actual).isSameAs(expected);
        verify(salesPortalService).getCustomerListByCoordinates(request);
    }

    @Test
    void searchCustomersByCoordinates_shouldPropagateUnexpectedException_whenServiceThrowsRuntimeException() {
        CustomerListByCoordinatesRequest request = new CustomerListByCoordinatesRequest();
        RuntimeException expected = new RuntimeException("Unexpected failure");

        when(salesPortalService.getCustomerListByCoordinates(request)).thenThrow(expected);

        RuntimeException actual = assertThrows(
                RuntimeException.class,
                () -> salesPortalCustomerController.searchCustomersByCoordinates(request)
        );

        assertThat(actual).isSameAs(expected);
        verify(salesPortalService).getCustomerListByCoordinates(request);
    }
}
