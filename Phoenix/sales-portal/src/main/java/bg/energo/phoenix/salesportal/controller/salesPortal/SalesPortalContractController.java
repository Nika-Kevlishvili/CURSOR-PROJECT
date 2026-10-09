package bg.energo.phoenix.salesportal.controller.salesPortal;

import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.service.salesportal.SalesPortalService;
import bg.energo.phoenix.service.salesportal.external.SalesPortalExternalIntegrationService;
import bg.energo.phoenix.service.salesportal.external.model.SalesPortalExternalContractStatusRequest;
import bg.energo.phoenix.service.salesportal.external.model.SalesPortalExternalContractStatusResponse;
import bg.energo.phoenix.service.salesportal.model.SalesPortalContractByCustomerResponse;
import bg.energo.phoenix.service.salesportal.model.SalesPortalContractByPodResponse;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/contract")
@Tag(name = "Product contract", description = "Product/express contract creation, update, CSP documents, status, jobs and lookups (OAuth2 Client Credentials)")
public class SalesPortalContractController {
    private final SalesPortalService salesPortalService;
    private final SalesPortalExternalIntegrationService salesPortalExternalIntegrationService;

    @GetMapping("/customer/{customerIdentifier}/{page}")
    @Operation(
            summary = "List contracts by customer identifier",
            description = """
                    Returns a paginated list of product contracts for an active customer.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `customerIdentifier` (UIC/customer number), `page` (0-based, page size fixed at 20).

                    **Optional query:** `language` — `BULGARIAN` (default) or `ENGLISH` for translated labels.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when the customer is not found.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated contracts (Spring Page JSON).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalContractByCustomerResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<Page<SalesPortalContractByCustomerResponse>> getContractsByCustomerId(
            @PathVariable("customerIdentifier") String customerIdentifier,
            @PathVariable("page") Integer page,
            @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language
    ) {
        return ResponseEntity.ok(salesPortalService.getContractsByCustomerIdentifier(
                customerIdentifier,
                page,
                language
        ));
    }

    @GetMapping("/pod/{podIdentifier}/{page}")
    @Operation(
            summary = "List contracts by POD identifier",
            description = """
                    Returns a paginated list of product contracts linked to an active POD.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `podIdentifier` (POD number), `page` (0-based, page size fixed at 20).

                    **Optional query:** `language` — default `BULGARIAN`.

                    Response includes `customerNumber` per contract. Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when POD is not found.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated contracts with customer number (Spring Page JSON).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalContractByPodResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<Page<SalesPortalContractByPodResponse>> getContractByPodId(
            @PathVariable("podIdentifier") String podIdentifier,
            @PathVariable("page") Integer page,
            @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language
    ) {
        return ResponseEntity.ok(salesPortalService.getContractByPodIdentifier(
                podIdentifier,
                page,
                language
        ));
    }

    @PostMapping("/status")
    @Operation(
            summary = "Send contract status to external CSP",
            description = """
                    Delivers a contract status change from Self-Service Portal to the external CSP integration.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Required body:** `contractId`, `contractVersionId`, `origin` (must be `Self_Service_Portal`),
                    `status`, `subStatus`, `eventDate`.

                    Contract id + version must exist in Phoenix. Retries failed deliveries automatically;
                    use `mockScenario` = `failed` for test scenarios.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when contract/version not found.""",
            security = @SecurityRequirement(name = "bearer-token"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalExternalContractStatusRequest.class),
                            examples = @ExampleObject(
                                    name = "StatusDelivery",
                                    value = """
                                            {
                                              "contractId": 10001,
                                              "contractVersionId": 3,
                                              "origin": "Self_Service_Portal",
                                              "status": "SIGNED",
                                              "subStatus": "SIGNED_BY_CUSTOMER",
                                              "activationDate": "2026-01-15",
                                              "eventDate": "2026-01-15T10:30:00"
                                            }"""))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "CSP acknowledged the status delivery.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalExternalContractStatusResponse.class),
                            examples = @ExampleObject(
                                    name = "Received",
                                    value = """
                                            {
                                              "contractId": 10001,
                                              "contractVersionId": 3,
                                              "status": "received",
                                              "responseDate": "2026-01-15T10:30:05"
                                            }""")))
    })
    @SalesPortalStandardApiResponses
    public ResponseEntity<SalesPortalExternalContractStatusResponse> sendContractStatus(
            @RequestBody @Valid SalesPortalExternalContractStatusRequest request
    ) {
        return ResponseEntity.ok(salesPortalExternalIntegrationService.sendContractStatus(request));
    }

    @PostMapping("/job")
    @Operation(
            summary = "Retry failed contract status deliveries",
            description = """
                    Triggers a manual retry of failed CSP contract-status deliveries (same logic as the scheduled job).

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    Returns a constant `1` on success (job started/completed). No request body.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Retry job executed.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "integer", example = "1")))
    })
    @SalesPortalStandardApiResponses
    public long job() {
        salesPortalExternalIntegrationService.retryFailedContractStatusDeliveries();
        return 1L;
    }

    @GetMapping("/document/{fileId}")
    @Operation(
            summary = "Download signed contract outgoing document",
            description = """
                    Downloads a fully signed contract outgoing document as PDF.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `fileId` — active document id linked to a product contract.

                    Document must be fully signed (electronic signers require `SIGNED` status;
                    paper flow requires contract status SIGNED/ACTIVE/TERMINATED).

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when document or contract link not found.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "PDF file bytes with Content-Disposition attachment header.",
                    content = @Content(mediaType = "application/pdf",
                            schema = @Schema(type = "string", format = "binary")))
    })
    @SalesPortalStandardApiResponses
    public HttpEntity<ByteArrayResource> getContractOutgoingDocument(
            @PathVariable("fileId") Long fileId
    ) {
        return salesPortalService.getContractOutgoingDocument(fileId);
    }

    @GetMapping("/additional-document/{fileId}")
    @Operation(
            summary = "Download external additional document",
            description = """
                    Downloads an additional document that is marked External and linked to a product contract.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `fileId` — additional document id from `additionalDocuments` on
                    `GET /product-contract/{id}/version/{versionID}`.

                    Documents marked Internal, deleted documents, and documents not linked to a contract
                    are not returned.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when the file cannot be downloaded.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "File bytes with Content-Disposition attachment header.",
                    content = @Content(mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary")))
    })
    @SalesPortalStandardApiResponses
    public HttpEntity<ByteArrayResource> getAdditionalDocument(
            @PathVariable("fileId") Long fileId
    ) {
        return salesPortalService.getAdditionalDocument(fileId);
    }

}
