package bg.energo.phoenix.salesportal.controller.salesPortal;

import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.service.salesportal.SalesPortalValidationService;
import bg.energo.phoenix.service.salesportal.model.SalesPortalValidationServiceCheckIdentifierResponse;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalValidationServiceCheckIdentifierRequest;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/validation-service")
@Tag(name = "Validation service", description = "Customer identifier validation (OAuth2 Client Credentials)")
public class SalesPortalValidationServiceController {

    private final SalesPortalValidationService salesPortalValidationService;

    @GetMapping("/check-identifier")
    @Operation(
            summary = "Validate customer identifier (blacklist and risk list)",
            description = """
                    Checks whether a customer identifier exists and evaluates blacklist (unwanted customer)
                    and risk-list status for the given annual consumption.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Required query:** `customerIdentifier`, `consumption` (kWh, 0–99999999.999, up to 3 decimal places).

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when no customer matches the identifier.
                    Risk-list failures are logged and returned as `riskList: MISSING`.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Validation result.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalValidationServiceCheckIdentifierResponse.class),
                            examples = @ExampleObject(
                                    name = "ApprovedCustomer",
                                    value = """
                                            {
                                              "blacklistPresence": false,
                                              "blacklistReasonId": "",
                                              "riskList": "APPROVED"
                                            }""")))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<SalesPortalValidationServiceCheckIdentifierResponse> checkIdentifier(
            @ParameterObject @Valid SalesPortalValidationServiceCheckIdentifierRequest filter
    ) {
        return ResponseEntity.ok(salesPortalValidationService.checkIdentifier(filter));
    }
}
