package bg.energo.phoenix.salesportal.controller.salesPortal;

import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.model.request.pod.pod.PodCreateRequest;
import bg.energo.phoenix.model.response.pod.pod.PodResponse;
import bg.energo.phoenix.service.pod.pod.PointOfDeliveryService;
import bg.energo.phoenix.service.salesportal.SalesPortalPodLastMeterReadingService;
import bg.energo.phoenix.service.salesportal.SalesPortalService;
import bg.energo.phoenix.service.salesportal.model.SalesPortalPodLastMeterReadingResponse;
import bg.energo.phoenix.service.salesportal.model.SalesPortalPodNameUpdateResponse;
import bg.energo.phoenix.service.salesportal.model.SalesPortalPodResponse;
import bg.energo.phoenix.service.salesportal.requests.PodByContractIdListRequest;
import bg.energo.phoenix.service.salesportal.requests.PodByCustomerIdListRequest;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalPodNameUpdateRequest;
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
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;


/**
 * Controller for Sales Portal provider integration (OAuth2 Client Credentials).
 *
 * <p>These endpoints are secured with JWT tokens obtained from
 * {@code POST /oauth2/token} using the OAuth2 Client Credentials grant.
 * The provider must first exchange {@code client_id} + {@code client_secret} for a JWT,
 * then include it as {@code Authorization: Bearer <token>} in subsequent requests.</p>
 *
 * <p>These endpoints do NOT use {@code @PermissionValidator} since the caller is an
 * external service, not a portal user.</p>
 *
 * @see SalesPortalTokenController for the token endpoint
 * @see bg.energo.phoenix.security.SalesPortalAuthenticationFilter
 * @see bg.energo.phoenix.config.SalesPortalSecurityConfig
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/pod")
@Tag(name = "POD", description = "Point of delivery operations (OAuth2 Client Credentials)")
public class SalesPortalPodController {

    private final SalesPortalService salesPortalService;
    private final SalesPortalPodLastMeterReadingService salesPortalPodLastMeterReadingService;
    private final PointOfDeliveryService pointOfDeliveryService;

    @PostMapping("/create")
    @Operation(
            summary = "Create a new POD",
            description = """
                    Creates a new point of delivery.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Body:** `PodCreateRequest`.

                    **Mandatory:** `identifier`, `gridOperatorId`, `systemSource`, `name`, `type`,
                    `estimatedMonthlyAvgConsumption`, `voltageLevel` and `addressRequest`.

                    `addressRequest` is required even though it looks structural: the service reads it on
                    every create. Inside it, `foreign` selects which half is used — `false` →
                    `localAddressData`, `true` → `foreignAddressData`.

                    For a local address, `localAddressData.countryId`, `populatedPlaceId` and `zipCodeId` are
                    always validated and therefore mandatory in practice. `districtId`, `residentialAreaId`
                    and `streetId` are optional, but each is checked **against `populatedPlaceId`** when sent —
                    an id that exists under a different populated place is rejected with
                    `... do not exists`. For a foreign address, `region`, `municipality`, `populatedPlace` and
                    `zipCode` are mandatory.

                    `slp` is optional and defaults to `false`. When `true`, `measurementTypeId` must be sent
                    and must belong to the same grid operator as `gridOperatorId`.

                    Address transliteration fields are optional: any `*Trsl` value left blank
                    is derived from the address itself (PHN-4067). Do not send an empty string — that is not
                    the same as omitting the field.

                    Returns **201** with `PodResponse` on success. Validation failures are **400** with
                    `field-Message;`.""",
            security = @SecurityRequirement(name = "bearer-token"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PodCreateRequest.class),
                            examples = @ExampleObject(
                                    name = "StructureOnly",
                                    summary = "Minimal body that succeeds — address included; ids are environment-specific",
                                    value = """
                                            {
                                              "identifier": "32Z0000000001234",
                                              "gridOperatorId": 1,
                                              "systemSource": "SALES_PORTAL",
                                              "name": "Office meter",
                                              "type": "CONSUMER",
                                              "estimatedMonthlyAvgConsumption": 500,
                                              "voltageLevel": "LOW",
                                              "slp": false,
                                              "addressRequest": {
                                                "foreign": false,
                                                "localAddressData": {
                                                  "countryId": 1027,
                                                  "populatedPlaceId": 2460,
                                                  "zipCodeId": 2439
                                                }
                                              }
                                            }"""))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "POD created.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PodResponse.class)))
    })
    @SalesPortalStandardApiResponses
    public ResponseEntity<PodResponse> create(
            @Validated(Default.class)
            @RequestBody PodCreateRequest request) {
        if (request.getSystemSource() == null) {
            throw new OperationNotAllowedException("systemSource-System source can not be null;");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(pointOfDeliveryService.create(request, new ArrayList<>()));
    }


    @PutMapping("/pod-name")
    @Operation(
            summary = "Update POD display name",
            description = """
                    Updates the display name on an active POD details version.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Body:** `podId`, `podIdentifier`, `detailId`, and `name` (max 1024 chars).
                    All four keys must match an active POD detail row.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when POD/detail not found or not active.""",
            security = @SecurityRequirement(name = "bearer-token"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalPodNameUpdateRequest.class),
                            examples = @ExampleObject(
                                    name = "RenamePod",
                                    value = """
                                            {
                                              "podId": 5001,
                                              "podIdentifier": "32Z0000000001234",
                                              "detailId": 7001,
                                              "name": "Office building main meter"
                                            }"""))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "POD name updated.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalPodNameUpdateResponse.class)))
    })
    @SalesPortalStandardApiResponses
    public ResponseEntity<SalesPortalPodNameUpdateResponse> updatePodName(
            @RequestBody @Valid SalesPortalPodNameUpdateRequest request) {
        SalesPortalPodNameUpdateResponse response = salesPortalService.updatePodName(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pod/{identifier}")
    @Operation(
            summary = "Get last meter reading for POD",
            description = """
                    Returns the latest active meter reading for an active POD.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `identifier` — POD number (trimmed, max 33 chars).

                    Returns **200** with `SalesPortalPodLastMeterReadingResponse` when a reading exists,
                    or **200** with empty JSON object `{}` when POD exists but has no qualifying reading.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when POD not found or identifier invalid.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Last meter reading or empty object when no reading.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(oneOf = {
                                    SalesPortalPodLastMeterReadingResponse.class,
                                    java.util.Map.class
                            })))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<?> getLastMeterReading(@PathVariable String identifier) {
        return salesPortalPodLastMeterReadingService.getLastMeterReading(identifier)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Collections.emptyMap()));
    }

    @GetMapping("/by-identifier")
    @Operation(
            summary = "Get POD by identifier",
            description = """
                    Returns full POD detail for a single active POD identifier.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Query:** `identifier` (max **16** chars), optional `language` (default `BULGARIAN`).

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when POD does not exist. Identifier > 16 chars → **400**.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "POD detail.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalPodResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<SalesPortalPodResponse> getPod(
            @RequestParam String identifier,
            @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language
    ) {
        return ResponseEntity.ok(salesPortalService.getPodByIdentifierByQuery(identifier, language));
    }

    @PostMapping("/by-customer-number")
    @Operation(
            summary = "List PODs by customer number",
            description = """
                    Returns a paginated list of PODs for an active customer.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Body:** `customerNumber` (10 digits), `page`, `size`, optional `sortBy`
                    (`POPULATED_PLACE` or `ACTIVATION_DATE`) and `direction`.

                    **Optional query:** `language` — default `BULGARIAN`.

                    Customer number must be exactly 10 digits. Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when customer not found.""",
            security = @SecurityRequirement(name = "bearer-token"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PodByCustomerIdListRequest.class),
                            examples = @ExampleObject(
                                    name = "FirstPage",
                                    value = """
                                            {
                                              "customerNumber": 6000123456,
                                              "page": 0,
                                              "size": 20,
                                              "sortBy": "POPULATED_PLACE",
                                              "direction": "DESC"
                                            }"""))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated POD list (Spring Page JSON).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalPodResponse.class)))
    })
    @SalesPortalStandardApiResponses
    public ResponseEntity<Page<SalesPortalPodResponse>> getPodByCustomerId(
            @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language,
            @RequestBody PodByCustomerIdListRequest request
    ) {
        return ResponseEntity.ok(salesPortalService.getPodByCustomerId(request, language));
    }

    @GetMapping("/by-contract-number")
    @Operation(
            summary = "List PODs by contract and customer number",
            description = """
                    Returns PODs linked to an active contract for an active customer.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Query params (bound from request object):** `contractNumber`, `customerNumber` (10 digits),
                    `page`, `size`, optional `language` (default `BULGARIAN`).

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when customer or contract not found.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated POD list (Spring Page JSON).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalPodResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<Page<SalesPortalPodResponse>> getPodByContractNumber(
            @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language,
            @ParameterObject @Valid PodByContractIdListRequest filter
    ) {
        return ResponseEntity.ok(salesPortalService.getPodByContractNumber(filter, language));
    }

}
