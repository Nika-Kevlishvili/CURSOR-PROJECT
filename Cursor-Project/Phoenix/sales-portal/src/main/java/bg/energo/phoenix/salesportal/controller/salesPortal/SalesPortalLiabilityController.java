package bg.energo.phoenix.salesportal.controller.salesPortal;

import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.service.salesportal.SalesPortalService;
import bg.energo.phoenix.service.salesportal.enums.SalesPortalLiabilityDocumentType;
import bg.energo.phoenix.service.salesportal.enums.SalesPortalLiabilityListColumns;
import bg.energo.phoenix.service.salesportal.model.SalesPortalLiabilityDetailsResponse;
import bg.energo.phoenix.service.salesportal.model.SalesPortalLiabilityListResponse;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/liability")
@Tag(name = "liability", description = "Customer liability list, details, and documents (OAuth2 Client Credentials)")
public class SalesPortalLiabilityController {
    private final SalesPortalService salesPortalService;

    @GetMapping("/list/{customerIdentifier}")
    @Operation(
            summary = "List liabilities by customer identifier",
            description = """
                    Returns a paginated list of liabilities for an active customer.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `customerIdentifier` — customer UIC or customer number.

                    **Query:** `page` (≥ 0), `size` (> 0), optional `columns` sort field,
                    optional `direction` (`ASC`/`DESC`, default `DESC`), optional `language`
                    (default `BULGARIAN`) for translated type/status labels.

                    Default sort: `id` descending.

                    Each item includes `amountWithInterest` — current amount plus calculated
                    late-payment interest for today. Equals `currentAmount` when no interest
                    applies (rate missing, not overdue, or interest calculation blocked).

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when customer not found.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated liability list (Spring Page JSON).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalLiabilityListResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<Page<SalesPortalLiabilityListResponse>> getLiabilityList(
            @PathVariable("customerIdentifier") String customerIdentifier,
            @RequestParam("page") Integer page,
            @RequestParam("size") Integer size,
            @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language,
            @RequestParam(value = "columns", required = false) SalesPortalLiabilityListColumns columns,
            @RequestParam(value = "direction", required = false) Sort.Direction direction
    ) {
        return ResponseEntity.ok(
                salesPortalService.getLiabilityListByCustomerIdentifier(customerIdentifier, page, size, language, columns, direction)
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get liability details by id",
            description = """
                    Returns full liability detail including POD, billing, collection, and document file ids.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `id` — liability id (> 0).

                    **Optional query:** `language` — default `BULGARIAN`.

                    Response includes `amountWithInterest` — current amount plus calculated
                    late-payment interest for today. Equals `currentAmount` when no interest
                    applies (rate missing, not overdue, or interest calculation blocked).

                    Use `outgoingDocumentFileIds` with `GET /liability/document/{fileId}/{type}` to download documents.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when liability not found.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Liability detail.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalLiabilityDetailsResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<SalesPortalLiabilityDetailsResponse> getLiabilityById(
            @PathVariable("id") Long id,
            @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language
    ) {
        return ResponseEntity.ok(salesPortalService.getLiabilityById(id, language));
    }

    @GetMapping("/document/{fileId}/{type}")
    @Operation(
            summary = "Download liability outgoing document",
            description = """
                    Downloads an outgoing document file linked to a liability.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `fileId` — document id (> 0).

                    **Path:** `type` — document category
                    (`INVOICE`, `LATE_PAYMENT_FINE`, `DEPOSIT`, `RESCHEDULING`, `ACTION`, `CLAIMED_PENALTY`).
                    ⚠ `type` does **not** affect the result: the document is resolved from `fileId` alone and
                    the value is never read. It is a required path segment for URL-shape compatibility only,
                    and passing the "wrong" category still returns the same file. Any of the six values works;
                    pass the category you got the `fileId` from. Do not rely on `type` to filter or to assert
                    what kind of document you are downloading.

                    File is served from the signed file URL, or from the EDMS archive when the document is
                    archived. Returns binary with `Content-Disposition: attachment`.

                    A `fileId` that is null or ≤ 0 fails with `fileId-File ID is mandatory;`. An id that
                    matches no document returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` and
                    `fileId-Document not found with given id;` — not a 404.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Document file bytes (force-download).",
                    content = @Content(
                            mediaType = "application/force-download",
                            schema = @Schema(type = "string", format = "binary")))
    })
    @SalesPortalStandardApiResponses
    public HttpEntity<ByteArrayResource> getLiabilityOutgoingDocument(
            @PathVariable("fileId") Long fileId,
            @Parameter(
                    description = "Document category. Accepted but not used — see the operation description.",
                    example = "INVOICE")
            @PathVariable("type") SalesPortalLiabilityDocumentType type
    ) {
        return salesPortalService.getLiabilityOutgoingDocument(fileId, type);
    }
}
