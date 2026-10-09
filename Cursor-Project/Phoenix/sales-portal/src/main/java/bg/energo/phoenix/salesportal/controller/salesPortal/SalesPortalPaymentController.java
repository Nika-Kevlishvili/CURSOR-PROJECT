package bg.energo.phoenix.salesportal.controller.salesPortal;

import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.service.salesportal.SalesPortalService;
import bg.energo.phoenix.service.salesportal.enums.SalesPortalPaymentListColumns;
import bg.energo.phoenix.service.salesportal.model.SalesPortalPaymentDetailsResponse;
import bg.energo.phoenix.service.salesportal.model.SalesPortalPaymentListResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
@Tag(name = "payment", description = "Customer payment list and details (OAuth2 Client Credentials)")
public class SalesPortalPaymentController {
    private final SalesPortalService salesPortalService;

    @GetMapping("/list/customer/{identifier}")
    @Operation(
            summary = "List payments by customer identifier",
            description = """
                    Returns a paginated list of payments for an active customer.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `identifier` — customer UIC or customer number.

                    **Query:** `page` (≥ 0), `size` (≥ 1), optional `columns` sort field,
                    optional `direction` (`ASC`/`DESC`, default `DESC`), optional `language`
                    (accepted but not applied to list items — response contains ids only).

                    Default sort: `paymentId` descending.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when customer not found.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated payment list (Spring Page JSON).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalPaymentListResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<Page<SalesPortalPaymentListResponse>> getPaymentList(
            @PathVariable("identifier") String identifier,
            @RequestParam("page") Integer page,
            @RequestParam("size") Integer size,
            @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language,
            @RequestParam(value = "columns", required = false) SalesPortalPaymentListColumns columns,
            @RequestParam(value = "direction", required = false) Sort.Direction direction
    ) {
        return ResponseEntity.ok(
                salesPortalService.getPaymentListByCustomerIdentifier(identifier, page, size, columns, direction)
        );
    }

    @GetMapping("/details/customer/{identifier}/payment/{paymentId}")
    @Operation(
            summary = "Get payment details",
            description = """
                    Returns full payment detail for a customer-owned active payment.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `identifier` — customer UIC/number, `paymentId` — payment id (> 0).

                    **Optional query:** `language` — `BULGARIAN` (default) or `ENGLISH` for translated labels.

                    Payment must belong to the customer. Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when customer, payment,
                    or customer/payment mismatch not found.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment detail.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalPaymentDetailsResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<SalesPortalPaymentDetailsResponse> getPaymentDetails(
            @PathVariable("identifier") String identifier,
            @PathVariable("paymentId") Long paymentId,
            @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language
    ) {
        return ResponseEntity.ok(
                salesPortalService.getPaymentDetailsByCustomerIdentifierAndPaymentId(identifier, paymentId, language)
        );
    }
}
