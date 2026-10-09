package bg.energo.phoenix.salesportal.controller.salesPortal.nomenclature.pod;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.model.response.nomenclature.pod.*;
import bg.energo.phoenix.service.salesportal.nomenclature.pod.SalesPortalPodNomenclaturesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/nomenclature/pod")
@Tag(name = "Nomenclatures - POD", description = "Point of delivery nomenclatures. (OAuth2 Client Credentials).")
public class SalesPortalPodNomenclaturesController {

    private final SalesPortalPodNomenclaturesService salesPortalPodNomenclaturesService;

    @GetMapping("/balancing-group-coordinators/{id}")
    @Operation(
            summary = "Get pod balancing group coordinators by id",
            description = """
                    Returns a single **pod balancing group coordinators** nomenclature record by id.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `id` — nomenclature record id.

                    **Optional query:** `language` — `BULGARIAN` (default) or `ENGLISH` for translated display values.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when no record exists for the id.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Nomenclature record.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BalancingGroupCoordinatorsResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<BalancingGroupCoordinatorsResponse> viewBalancingGroupCoordinators(@PathVariable("id") Long id,
                                                                                             @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalPodNomenclaturesService.viewBalancingGroupCoordinators(id, language), HttpStatus.OK);
    }

    @GetMapping("/measurement-type/{id}")
    @Operation(
            summary = "Get pod measurement type by id",
            description = """
                    Returns a single **pod measurement type** nomenclature record by id.

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
                            schema = @Schema(implementation = MeasurementTypeResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<MeasurementTypeResponse> viewMeasurementType(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalPodNomenclaturesService.viewMeasurementType(id), HttpStatus.OK);
    }

    @GetMapping("/pod-additional-parameters/{id}")
    @Operation(
            summary = "Get pod pod additional parameters by id",
            description = """
                    Returns a single **pod pod additional parameters** nomenclature record by id.

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
                            schema = @Schema(implementation = PodAdditionalParametersResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<PodAdditionalParametersResponse> viewPodAdditionalParameters(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalPodNomenclaturesService.viewPodAdditionalParameters(id), HttpStatus.OK);
    }

    @GetMapping("/profiles/{id}")
    @Operation(
            summary = "Get pod profiles by id",
            description = """
                    Returns a single **pod profiles** nomenclature record by id.

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
                            schema = @Schema(implementation = ProfilesResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ProfilesResponse> viewProfiles(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalPodNomenclaturesService.viewProfiles(id), HttpStatus.OK);
    }

    @GetMapping("/user-type/{id}")
    @Operation(
            summary = "Get pod user type by id",
            description = """
                    Returns a single **pod user type** nomenclature record by id.

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
                            schema = @Schema(implementation = UserTypeResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<UserTypeResponse> viewUserType(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalPodNomenclaturesService.viewUserType(id), HttpStatus.OK);
    }
}
