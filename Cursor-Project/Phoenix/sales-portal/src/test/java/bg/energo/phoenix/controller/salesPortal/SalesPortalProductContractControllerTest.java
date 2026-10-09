package bg.energo.phoenix.controller.salesPortal;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.salesportal.controller.salesPortal.SalesPortalProductContractController;
import bg.energo.phoenix.service.product.product.ProductService;
import bg.energo.phoenix.service.salesportal.SalesPortalContractAndCustomerUpdateService;
import bg.energo.phoenix.service.salesportal.SalesPortalService;
import bg.energo.phoenix.util.UrlEncodingUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesPortalProductContractControllerTest {

    @Mock
    private ProductService productService;
    @Mock
    private SalesPortalService salesPortalService;
    @Mock
    private SalesPortalContractAndCustomerUpdateService salesPortalContractAndCustomerUpdateService;

    private SalesPortalProductContractController salesPortalProductContractController;

    @BeforeEach
    void setUp() {
        this.salesPortalProductContractController = new SalesPortalProductContractController(
                productService,
                salesPortalService,
                salesPortalContractAndCustomerUpdateService
        );
    }

    @Test
    void generateDcaDocument_shouldReturnPdfAttachmentAndBody_whenServiceReturnsContent() {
        Long productId = 1L;
        Long versionId = 2L;
        String request = "{\"foo\":\"bar\"}";
        String fileName = "contract document.pdf";
        byte[] contentBytes = new byte[]{1, 2, 3};
        FileContent fileContent = new FileContent(fileName, contentBytes);

        when(productService.generateCSPDocument(productId, versionId, request)).thenReturn(fileContent);

        HttpEntity<ByteArrayResource> actual =
                salesPortalProductContractController.generateDcaDocument(productId, versionId, request);

        assertThat(actual.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(actual.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=%s".formatted(UrlEncodingUtil.encodeFileName(fileName)));
        assertThat(actual.getBody()).isNotNull();
        assertThat(actual.getBody().getByteArray()).isEqualTo(contentBytes);
        verify(productService).generateCSPDocument(productId, versionId, request);
    }

    @Test
    void generateDcaDocument_shouldPropagateClientException_whenServiceThrowsValidationFailure() {
        Long productId = 11L;
        Long versionId = 22L;
        String request = "{}";
        ClientException expected = new ClientException(
                "Invalid request;",
                ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED
        );

        when(productService.generateCSPDocument(productId, versionId, request)).thenThrow(expected);

        ClientException actual = assertThrows(
                ClientException.class,
                () -> salesPortalProductContractController.generateDcaDocument(productId, versionId, request)
        );

        assertThat(actual).isSameAs(expected);
        verify(productService).generateCSPDocument(productId, versionId, request);
    }

    @Test
    void generateDcaDocument_shouldPropagateNotFound_whenServiceThrowsDomainEntityNotFoundException() {
        Long productId = 100L;
        Long versionId = 200L;
        String request = "{\"x\":1}";

        when(productService.generateCSPDocument(productId, versionId, request))
                .thenThrow(new DomainEntityNotFoundException("Product contract not found"));

        DomainEntityNotFoundException actual = assertThrows(
                DomainEntityNotFoundException.class,
                () -> salesPortalProductContractController.generateDcaDocument(productId, versionId, request)
        );

        assertThat(actual.getMessage()).isEqualTo("Product contract not found");
        verify(productService).generateCSPDocument(productId, versionId, request);
    }

    @Test
    void generateDcaDocument_shouldPropagateUnexpectedException_whenServiceThrowsRuntimeException() {
        Long productId = 8L;
        Long versionId = 9L;
        String request = "{\"test\":true}";
        RuntimeException expected = new RuntimeException("Unexpected failure");

        when(productService.generateCSPDocument(productId, versionId, request)).thenThrow(expected);

        RuntimeException actual = assertThrows(
                RuntimeException.class,
                () -> salesPortalProductContractController.generateDcaDocument(productId, versionId, request)
        );

        assertThat(actual).isSameAs(expected);
        verify(productService).generateCSPDocument(productId, versionId, request);
    }

    @Test
    void generateDcaDocument_shouldThrowNullPointerException_whenServiceReturnsNullContent() {
        Long productId = 15L;
        Long versionId = 16L;
        String request = "{\"id\":15}";

        when(productService.generateCSPDocument(productId, versionId, request)).thenReturn(null);

        NullPointerException actual = assertThrows(
                NullPointerException.class,
                () -> salesPortalProductContractController.generateDcaDocument(productId, versionId, request)
        );

        assertThat(actual).isNotNull();
        verify(productService).generateCSPDocument(productId, versionId, request);
    }
}
