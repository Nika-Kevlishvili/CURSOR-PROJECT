package bg.energo.phoenix.salesportal.controller.salesPortal.nomenclature.receivable;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.response.nomenclature.receivable.*;
import bg.energo.phoenix.service.salesportal.nomenclature.receivable.SalesPortalReceivableNomenclaturesService;
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
@RequestMapping("/nomenclature/receivable")
@Tag(name = "Nomenclatures - Receivable", description = "Receivable nomenclatures. (OAuth2 Client Credentials).")
public class SalesPortalReceivableNomenclaturesController {

    private final SalesPortalReceivableNomenclaturesService salesPortalReceivableNomenclaturesService;

    @GetMapping("/additional-condition/{id}")
    @Operation(
            summary = "Get receivable additional condition by id",
            description = """
                    Returns a single **receivable additional condition** nomenclature record by id.

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
                            schema = @Schema(implementation = AdditionalConditionResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<AdditionalConditionResponse> viewAdditionalCondition(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalReceivableNomenclaturesService.viewAdditionalCondition(id), HttpStatus.OK);
    }

    @GetMapping("/balancing-group-coordinator-ground/{id}")
    @Operation(
            summary = "Get receivable balancing group coordinator ground by id",
            description = """
                    Returns a single **receivable balancing group coordinator ground** nomenclature record by id.

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
                            schema = @Schema(implementation = BalancingGroupCoordinatorGroundResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<BalancingGroupCoordinatorGroundResponse> viewBalancingGroupCoordinatorGround(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalReceivableNomenclaturesService.viewBalancingGroupCoordinatorGround(id), HttpStatus.OK);
    }

    @GetMapping("/blocking-reason/{id}")
    @Operation(
            summary = "Get receivable blocking reason by id",
            description = """
                    Returns a single **receivable blocking reason** nomenclature record by id.

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
                            schema = @Schema(implementation = BlockingReasonResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<BlockingReasonResponse> viewBlockingReason(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalReceivableNomenclaturesService.viewBlockingReason(id), HttpStatus.OK);
    }

    @GetMapping("/collection-partner/{id}")
    @Operation(
            summary = "Get receivable collection partner by id",
            description = """
                    Returns a single **receivable collection partner** nomenclature record by id.

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
                            schema = @Schema(implementation = CollectionPartnerResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<CollectionPartnerResponse> viewCollectionPartner(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalReceivableNomenclaturesService.viewCollectionPartner(id), HttpStatus.OK);
    }

    @GetMapping("/customer-assessment-criteria/{id}")
    @Operation(
            summary = "Get receivable customer assessment criteria by id",
            description = """
                    Returns a single **receivable customer assessment criteria** nomenclature record by id.

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
                            schema = @Schema(implementation = CustomerAssessmentCriteriaFullResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<CustomerAssessmentCriteriaFullResponse> viewCustomerAssessmentCriteria(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalReceivableNomenclaturesService.viewCustomerAssessmentCriteria(id), HttpStatus.OK);
    }

    @GetMapping("/ground-for-objection-withdrawal-to-change-of-acbg/{id}")
    @Operation(
            summary = "Get receivable ground for objection withdrawal to change of acbg by id",
            description = """
                    Returns a single **receivable ground for objection withdrawal to change of acbg** nomenclature record by id.

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
                            schema = @Schema(implementation = GroundForObjectionWithdrawalToChangeOfACbgResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<GroundForObjectionWithdrawalToChangeOfACbgResponse> viewGroundForObjectionWithdrawalToChangeOfACbg(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalReceivableNomenclaturesService.viewGroundForObjectionWithdrawalToChangeOfACbg(id), HttpStatus.OK);
    }

    @GetMapping("/reason-for-cancellation/{id}")
    @Operation(
            summary = "Get receivable reason for cancellation by id",
            description = """
                    Returns a single **receivable reason for cancellation** nomenclature record by id.

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
                            schema = @Schema(implementation = ReasonForCancellationResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ReasonForCancellationResponse> viewReasonForCancellation(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalReceivableNomenclaturesService.viewReasonForCancellation(id), HttpStatus.OK);
    }

    @GetMapping("/reason-for-disconnection/{id}")
    @Operation(
            summary = "Get receivable reason for disconnection by id",
            description = """
                    Returns a single **receivable reason for disconnection** nomenclature record by id.

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
                            schema = @Schema(implementation = ReasonForDisconnectionResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ReasonForDisconnectionResponse> viewReasonForDisconnection(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalReceivableNomenclaturesService.viewReasonForDisconnection(id), HttpStatus.OK);
    }

    @GetMapping("/tax-for-the-grid-operator/{id}")
    @Operation(
            summary = "Get receivable tax for the grid operator by id",
            description = """
                    Returns a single **receivable tax for the grid operator** nomenclature record by id.

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
                            schema = @Schema(implementation = TaxForTheGridOperatorViewResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<TaxForTheGridOperatorViewResponse> viewTaxForTheGridOperator(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalReceivableNomenclaturesService.viewTaxForTheGridOperator(id), HttpStatus.OK);
    }
}
