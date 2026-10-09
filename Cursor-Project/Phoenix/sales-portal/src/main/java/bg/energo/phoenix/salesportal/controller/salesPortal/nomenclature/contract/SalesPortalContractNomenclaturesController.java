package bg.energo.phoenix.salesportal.controller.salesPortal.nomenclature.contract;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.response.nomenclature.contract.*;
import bg.energo.phoenix.service.salesportal.nomenclature.contract.SalesPortalContractNomenclaturesService;
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
@RequestMapping("/nomenclature/contract")
@Tag(name = "Nomenclatures - Contract", description = "Contract nomenclatures. (OAuth2 Client Credentials).")
public class SalesPortalContractNomenclaturesController {

    private final SalesPortalContractNomenclaturesService salesPortalContractNomenclaturesService;

    @GetMapping("/action-type/{id}")
    @Operation(
            summary = "Get contract action type by id",
            description = """
                    Returns a single **contract action type** nomenclature record by id.

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
                            schema = @Schema(implementation = ActionTypeResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ActionTypeResponse> viewActionType(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalContractNomenclaturesService.viewActionType(id), HttpStatus.OK);
    }

    @GetMapping("/activity/{id}")
    @Operation(
            summary = "Get contract activity by id",
            description = """
                    Returns a single **contract activity** nomenclature record by id.

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
                            schema = @Schema(implementation = ActivityResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ActivityResponse> viewActivity(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalContractNomenclaturesService.viewActivity(id), HttpStatus.OK);
    }

    @GetMapping("/base-interest-rate/{id}")
    @Operation(
            summary = "Get contract base interest rate by id",
            description = """
                    Returns a single **contract base interest rate** nomenclature record by id.

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
                            schema = @Schema(implementation = BaseInterestRateResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<BaseInterestRateResponse> viewBaseInterestRate(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalContractNomenclaturesService.viewBaseInterestRate(id), HttpStatus.OK);
    }

    @GetMapping("/campaign/{id}")
    @Operation(
            summary = "Get contract campaign by id",
            description = """
                    Returns a single **contract campaign** nomenclature record by id.

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
                            schema = @Schema(implementation = CampaignResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<CampaignResponse> viewCampaign(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalContractNomenclaturesService.viewCampaign(id), HttpStatus.OK);
    }

    @GetMapping("/contract-version-types/{id}")
    @Operation(
            summary = "Get contract contract version types by id",
            description = """
                    Returns a single **contract contract version types** nomenclature record by id.

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
                            schema = @Schema(implementation = ContractVersionTypesResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ContractVersionTypesResponse> viewContractVersionTypes(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalContractNomenclaturesService.viewContractVersionTypes(id), HttpStatus.OK);
    }

    @GetMapping("/deactivation-purpose/{id}")
    @Operation(
            summary = "Get contract deactivation purpose by id",
            description = """
                    Returns a single **contract deactivation purpose** nomenclature record by id.

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
                            schema = @Schema(implementation = DeactivationPurposeResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<DeactivationPurposeResponse> viewDeactivationPurpose(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalContractNomenclaturesService.viewDeactivationPurpose(id), HttpStatus.OK);
    }

    @GetMapping("/external-intermediary/{id}")
    @Operation(
            summary = "Get contract external intermediary by id",
            description = """
                    Returns a single **contract external intermediary** nomenclature record by id.

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
                            schema = @Schema(implementation = ExternalIntermediaryResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ExternalIntermediaryResponse> viewExternalIntermediary(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalContractNomenclaturesService.viewExternalIntermediary(id), HttpStatus.OK);
    }

    @GetMapping("/sub-activity/{id}")
    @Operation(
            summary = "Get contract sub activity by id",
            description = """
                    Returns a single **contract sub activity** nomenclature record by id.

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
                            schema = @Schema(implementation = SubActivityResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<SubActivityResponse> viewSubActivity(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalContractNomenclaturesService.viewSubActivity(id), HttpStatus.OK);
    }

    @GetMapping("/task-type/{id}")
    @Operation(
            summary = "Get contract task type by id",
            description = """
                    Returns a single **contract task type** nomenclature record by id.

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
                            schema = @Schema(implementation = TaskTypeResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<TaskTypeResponse> viewTaskType(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalContractNomenclaturesService.viewTaskType(id), HttpStatus.OK);
    }
}
