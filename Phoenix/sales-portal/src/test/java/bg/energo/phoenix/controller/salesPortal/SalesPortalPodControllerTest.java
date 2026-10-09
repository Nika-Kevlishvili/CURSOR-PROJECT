package bg.energo.phoenix.controller.salesPortal;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.model.request.pod.pod.PodCreateRequest;
import bg.energo.phoenix.model.response.pod.pod.PodResponse;
import bg.energo.phoenix.salesportal.controller.salesPortal.SalesPortalPodController;
import bg.energo.phoenix.service.pod.pod.PointOfDeliveryService;
import bg.energo.phoenix.service.salesportal.SalesPortalPodLastMeterReadingService;
import bg.energo.phoenix.service.salesportal.SalesPortalService;
import bg.energo.phoenix.service.salesportal.enums.SystemSoureEnum;
import bg.energo.phoenix.service.salesportal.model.SalesPortalPodLastMeterReadingResponse;
import bg.energo.phoenix.service.salesportal.model.SalesPortalPodNameUpdateResponse;
import bg.energo.phoenix.service.salesportal.model.SalesPortalPodResponse;
import bg.energo.phoenix.service.salesportal.requests.PodByContractIdListRequest;
import bg.energo.phoenix.service.salesportal.requests.PodByCustomerIdListRequest;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalPodNameUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesPortalPodControllerTest {

    @Mock
    private SalesPortalService salesPortalService;
    @Mock
    private SalesPortalPodLastMeterReadingService salesPortalPodLastMeterReadingService;
    @Mock
    private PointOfDeliveryService pointOfDeliveryService;

    private SalesPortalPodController salesPortalPodController;

    @BeforeEach
    void setUp() {
        this.salesPortalPodController = new SalesPortalPodController(
                salesPortalService,
                salesPortalPodLastMeterReadingService,
                pointOfDeliveryService
        );
    }

    @Test
    void getLastMeterReading_shouldReturnOkAndResponseBody_whenValidRequest() {
        String identifier = "32XGYKZDRAZPN1058";
        SalesPortalPodLastMeterReadingResponse expected = SalesPortalPodLastMeterReadingResponse.builder()
                .podIdentifier(identifier)
                .podId(1L)
                .build();

        when(salesPortalPodLastMeterReadingService.getLastMeterReading(identifier)).thenReturn(Optional.of(expected));

        ResponseEntity<?> actual = salesPortalPodController.getLastMeterReading(identifier);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalPodLastMeterReadingService).getLastMeterReading(identifier);
    }

    @Test
    void getLastMeterReading_shouldReturnEmptyObject_whenNoScaleData() {
        String identifier = "32XGYKZDRAZPN1058";
        when(salesPortalPodLastMeterReadingService.getLastMeterReading(identifier)).thenReturn(Optional.empty());

        ResponseEntity<?> actual = salesPortalPodController.getLastMeterReading(identifier);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isInstanceOf(Map.class);
        assertThat((Map<?, ?>) actual.getBody()).isEmpty();
    }

    @Test
    void getPod_shouldReturnOkAndResponseBody_whenValidRequest() {
        String identifier = "1234567890123456";
        Language language = Language.ENGLISH;
        SalesPortalPodResponse expected = new SalesPortalPodResponse();
        expected.setPodId(10L);
        expected.setPodDetailId(20L);
        expected.setPodIdentifier(identifier);
        expected.setPodStatus("ACTIVE");

        when(salesPortalService.getPodByIdentifierByQuery(identifier, language)).thenReturn(expected);

        ResponseEntity<SalesPortalPodResponse> actual = salesPortalPodController.getPod(identifier, language);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalService).getPodByIdentifierByQuery(identifier, language);
    }

    @Test
    void getPod_shouldDelegateWithBulgarian_whenLanOmittedUsesDefault() {
        String identifier = "1234567890123456";
        SalesPortalPodResponse expected = new SalesPortalPodResponse();

        when(salesPortalService.getPodByIdentifierByQuery(identifier, Language.BULGARIAN)).thenReturn(expected);

        ResponseEntity<SalesPortalPodResponse> actual =
                salesPortalPodController.getPod(identifier, Language.BULGARIAN);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalService).getPodByIdentifierByQuery(identifier, Language.BULGARIAN);
    }

    @Test
    void getPod_shouldPropagateException_whenServiceThrows() {
        String identifier = "unknown";
        Language language = Language.BULGARIAN;

        when(salesPortalService.getPodByIdentifierByQuery(identifier, language))
                .thenThrow(new DomainEntityNotFoundException("Pod not found!"));

        DomainEntityNotFoundException actual = assertThrows(
                DomainEntityNotFoundException.class,
                () -> salesPortalPodController.getPod(identifier, language)
        );

        assertThat(actual.getMessage()).isEqualTo("Pod not found!");
        verify(salesPortalService).getPodByIdentifierByQuery(identifier, language);
    }

    @Test
    void create_shouldReturnCreatedAndResponseBody() {
        PodCreateRequest request = createValidPodCreateRequest();
        PodResponse expected = new PodResponse();

        when(pointOfDeliveryService.create(request, List.of())).thenReturn(expected);

        ResponseEntity<PodResponse> actual = salesPortalPodController.create(request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(pointOfDeliveryService).create(request, List.of());
    }

    @Test
    void create_shouldReturnCreatedWithNullBody_whenServiceReturnsNull() {
        PodCreateRequest request = createValidPodCreateRequest();

        when(pointOfDeliveryService.create(request, List.of())).thenReturn(null);

        ResponseEntity<PodResponse> actual = salesPortalPodController.create(request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(actual.getBody()).isNull();
        verify(pointOfDeliveryService).create(request, List.of());
        verifyNoInteractions(salesPortalService);
    }

    @Test
    void create_shouldPropagateClientException_whenServiceThrows() {
        PodCreateRequest request = createValidPodCreateRequest();
        ClientException expected = new ClientException("Invalid request;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);

        when(pointOfDeliveryService.create(request, List.of())).thenThrow(expected);

        ClientException actual = assertThrows(
                ClientException.class,
                () -> salesPortalPodController.create(request)
        );

        assertThat(actual).isSameAs(expected);
        verify(pointOfDeliveryService).create(request, List.of());
        verifyNoInteractions(salesPortalService);
    }

    @Test
    void create_shouldPropagateUnexpectedException_whenServiceThrowsRuntimeException() {
        PodCreateRequest request = createValidPodCreateRequest();
        RuntimeException expected = new RuntimeException("Unexpected failure");

        when(pointOfDeliveryService.create(request, List.of())).thenThrow(expected);

        RuntimeException actual = assertThrows(
                RuntimeException.class,
                () -> salesPortalPodController.create(request)
        );

        assertThat(actual).isSameAs(expected);
        verify(pointOfDeliveryService).create(request, List.of());
        verifyNoInteractions(salesPortalService);
    }

    @Test
    void updatePodName_shouldReturnOkAndResponseBody() {
        SalesPortalPodNameUpdateRequest request = new SalesPortalPodNameUpdateRequest();
        SalesPortalPodNameUpdateResponse expected = new SalesPortalPodNameUpdateResponse();
        expected.setPodId(5L);
        expected.setPodDetailId(7L);

        when(salesPortalService.updatePodName(request)).thenReturn(expected);

        ResponseEntity<SalesPortalPodNameUpdateResponse> actual = salesPortalPodController.updatePodName(request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalService).updatePodName(request);
    }

    @Test
    void updatePodName_shouldPropagateNotFound_whenServiceThrowsDomainEntityNotFoundException() {
        SalesPortalPodNameUpdateRequest request = new SalesPortalPodNameUpdateRequest();

        when(salesPortalService.updatePodName(request))
                .thenThrow(new DomainEntityNotFoundException("Pod not found or not active"));

        DomainEntityNotFoundException actual = assertThrows(
                DomainEntityNotFoundException.class,
                () -> salesPortalPodController.updatePodName(request)
        );

        assertThat(actual.getMessage()).isEqualTo("Pod not found or not active");
        verify(salesPortalService).updatePodName(request);
    }

    @Test
    void updatePodName_shouldPropagateValidationError_whenServiceThrowsClientException() {
        SalesPortalPodNameUpdateRequest request = new SalesPortalPodNameUpdateRequest();
        ClientException expected = new ClientException("Invalid pod name;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);

        when(salesPortalService.updatePodName(request)).thenThrow(expected);

        ClientException actual = assertThrows(
                ClientException.class,
                () -> salesPortalPodController.updatePodName(request)
        );

        assertThat(actual).isSameAs(expected);
        verify(salesPortalService).updatePodName(request);
    }

    @Test
    void updatePodName_shouldReturnOkWithNullBody_whenServiceReturnsNull() {
        SalesPortalPodNameUpdateRequest request = new SalesPortalPodNameUpdateRequest();

        when(salesPortalService.updatePodName(request)).thenReturn(null);

        ResponseEntity<SalesPortalPodNameUpdateResponse> actual = salesPortalPodController.updatePodName(request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isNull();
        verify(salesPortalService).updatePodName(request);
    }

    @Test
    void updatePodName_shouldPropagateUnexpectedException_whenServiceThrowsRuntimeException() {
        SalesPortalPodNameUpdateRequest request = new SalesPortalPodNameUpdateRequest();
        RuntimeException expected = new RuntimeException("Unexpected failure");

        when(salesPortalService.updatePodName(request)).thenThrow(expected);

        RuntimeException actual = assertThrows(
                RuntimeException.class,
                () -> salesPortalPodController.updatePodName(request)
        );

        assertThat(actual).isSameAs(expected);
        verify(salesPortalService).updatePodName(request);
        verifyNoInteractions(pointOfDeliveryService);
    }

    @Test
    void getPodByCustomerId_shouldReturnOkAndPageBody() {
        PodByCustomerIdListRequest request = new PodByCustomerIdListRequest();
        Page<SalesPortalPodResponse> expected = new PageImpl<>(List.of(new SalesPortalPodResponse()));

        when(salesPortalService.getPodByCustomerId(request, Language.BULGARIAN)).thenReturn(expected);

        ResponseEntity<Page<SalesPortalPodResponse>> actual = salesPortalPodController.getPodByCustomerId(Language.BULGARIAN, request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalService).getPodByCustomerId(request, Language.BULGARIAN);
    }

    @Test
    void getPodByCustomerId_shouldReturnOkWithNullBody_whenServiceReturnsNull() {
        PodByCustomerIdListRequest request = new PodByCustomerIdListRequest();

        when(salesPortalService.getPodByCustomerId(request, Language.BULGARIAN)).thenReturn(null);

        ResponseEntity<Page<SalesPortalPodResponse>> actual = salesPortalPodController.getPodByCustomerId(Language.BULGARIAN, request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isNull();
        verify(salesPortalService).getPodByCustomerId(request, Language.BULGARIAN);
        verifyNoInteractions(pointOfDeliveryService);
    }

    @Test
    void getPodByCustomerId_shouldPropagateClientException_whenServiceThrows() {
        PodByCustomerIdListRequest request = new PodByCustomerIdListRequest();
        ClientException expected = new ClientException("Invalid customer number;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);

        when(salesPortalService.getPodByCustomerId(request, Language.BULGARIAN)).thenThrow(expected);

        ClientException actual = assertThrows(
                ClientException.class,
                () -> salesPortalPodController.getPodByCustomerId(Language.BULGARIAN, request)
        );

        assertThat(actual).isSameAs(expected);
        verify(salesPortalService).getPodByCustomerId(request, Language.BULGARIAN);
        verifyNoInteractions(pointOfDeliveryService);
    }

    @Test
    void getPodByCustomerId_shouldPropagateUnexpectedException_whenServiceThrowsRuntimeException() {
        PodByCustomerIdListRequest request = new PodByCustomerIdListRequest();
        RuntimeException expected = new RuntimeException("Unexpected failure");

        when(salesPortalService.getPodByCustomerId(request, Language.BULGARIAN)).thenThrow(expected);

        RuntimeException actual = assertThrows(
                RuntimeException.class,
                () -> salesPortalPodController.getPodByCustomerId(Language.BULGARIAN, request)
        );

        assertThat(actual).isSameAs(expected);
        verify(salesPortalService).getPodByCustomerId(request, Language.BULGARIAN);
        verifyNoInteractions(pointOfDeliveryService);
    }

    @Test
    void getPodByContractNumber_shouldReturnOkAndPageBody() {
        PodByContractIdListRequest request = new PodByContractIdListRequest();
        Page<SalesPortalPodResponse> expected = new PageImpl<>(List.of(new SalesPortalPodResponse()));

        when(salesPortalService.getPodByContractNumber(request, Language.BULGARIAN)).thenReturn(expected);

        ResponseEntity<Page<SalesPortalPodResponse>> actual = salesPortalPodController.getPodByContractNumber(Language.BULGARIAN, request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(expected);
        verify(salesPortalService).getPodByContractNumber(request, Language.BULGARIAN);
    }

    @Test
    void getPodByContractNumber_shouldReturnOkWithNullBody_whenServiceReturnsNull() {
        PodByContractIdListRequest request = new PodByContractIdListRequest();

        when(salesPortalService.getPodByContractNumber(request, Language.BULGARIAN)).thenReturn(null);

        ResponseEntity<Page<SalesPortalPodResponse>> actual = salesPortalPodController.getPodByContractNumber(Language.BULGARIAN, request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isNull();
        verify(salesPortalService).getPodByContractNumber(request, Language.BULGARIAN);
        verifyNoInteractions(pointOfDeliveryService);
    }

    @Test
    void getPodByContractNumber_shouldPropagateClientException_whenServiceThrows() {
        PodByContractIdListRequest request = new PodByContractIdListRequest();
        ClientException expected = new ClientException("Invalid contract number;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);

        when(salesPortalService.getPodByContractNumber(request, Language.BULGARIAN)).thenThrow(expected);

        ClientException actual = assertThrows(
                ClientException.class,
                () -> salesPortalPodController.getPodByContractNumber(Language.BULGARIAN, request)
        );

        assertThat(actual).isSameAs(expected);
        verify(salesPortalService).getPodByContractNumber(request, Language.BULGARIAN);
        verifyNoInteractions(pointOfDeliveryService);
    }

    @Test
    void getPodByContractNumber_shouldPropagateUnexpectedException_whenServiceThrowsRuntimeException() {
        PodByContractIdListRequest request = new PodByContractIdListRequest();
        RuntimeException expected = new RuntimeException("Unexpected failure");

        when(salesPortalService.getPodByContractNumber(request, Language.BULGARIAN)).thenThrow(expected);

        RuntimeException actual = assertThrows(
                RuntimeException.class,
                () -> salesPortalPodController.getPodByContractNumber(Language.BULGARIAN, request)
        );

        assertThat(actual).isSameAs(expected);
        verify(salesPortalService).getPodByContractNumber(request, Language.BULGARIAN);
        verifyNoInteractions(pointOfDeliveryService);
    }

    private PodCreateRequest createValidPodCreateRequest() {
        PodCreateRequest request = new PodCreateRequest();
        request.setSystemSource(SystemSoureEnum.SELF_SERVICE_PORTAL);
        return request;
    }
}
