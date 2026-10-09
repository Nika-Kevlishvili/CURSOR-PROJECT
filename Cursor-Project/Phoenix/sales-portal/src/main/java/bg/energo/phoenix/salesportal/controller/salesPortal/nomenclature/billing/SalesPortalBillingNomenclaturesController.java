package bg.energo.phoenix.salesportal.controller.salesPortal.nomenclature.billing;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.response.nomenclature.billing.IncomeAccountNameResponse;
import bg.energo.phoenix.model.response.nomenclature.billing.PrefixResponse;
import bg.energo.phoenix.model.response.nomenclature.billing.RiskAssessmentResponse;
import bg.energo.phoenix.model.response.nomenclature.billing.RpsNumberResponse;
import bg.energo.phoenix.service.salesportal.nomenclature.billing.SalesPortalBillingNomenclaturesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/nomenclature/billing")
@Tag(name = "Nomenclatures - Billing", description = "Billing nomenclatures. (OAuth2 Client Credentials).")
public class SalesPortalBillingNomenclaturesController {

    private final SalesPortalBillingNomenclaturesService salesPortalBillingNomenclaturesService;

    @GetMapping("/income-account-name/{id}")
    @Operation(
            summary = "Get billing income account name by id",
            description = """
                    Returns a single **billing income account name** nomenclature record by id.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `id` — nomenclature record id.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when no record exists for the id.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Nomenclature record.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = IncomeAccountNameResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<IncomeAccountNameResponse> viewIncomeAccountName(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalBillingNomenclaturesService.viewIncomeAccountName(id), HttpStatus.OK);
    }

    @GetMapping("/prefix/{id}")
    @Operation(
            summary = "Get billing prefix by id",
            description = """
                    Returns a single **billing prefix** nomenclature record by id.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `id` — nomenclature record id.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when no record exists for the id.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Nomenclature record.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PrefixResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<PrefixResponse> viewPrefix(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalBillingNomenclaturesService.viewPrefix(id), HttpStatus.OK);
    }

    @GetMapping("/risk-assessment/{id}")
    @Operation(
            summary = "Get billing risk assessment by id",
            description = """
                    Returns a single **billing risk assessment** nomenclature record by id.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `id` — nomenclature record id.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when no record exists for the id.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Nomenclature record.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RiskAssessmentResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<RiskAssessmentResponse> viewRiskAssessment(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalBillingNomenclaturesService.viewRiskAssessment(id), HttpStatus.OK);
    }

    @GetMapping("/rps-number/{id}")
    @Operation(
            summary = "Get billing rps number by id",
            description = """
                    Returns a single **billing rps number** nomenclature record by id.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `id` — nomenclature record id.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when no record exists for the id.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Nomenclature record.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RpsNumberResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<RpsNumberResponse> viewRpsNumber(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalBillingNomenclaturesService.viewRpsNumber(id), HttpStatus.OK);
    }
}
