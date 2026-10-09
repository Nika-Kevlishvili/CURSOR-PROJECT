package bg.energo.phoenix.salesportal.controller.salesPortal.nomenclature.customer;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.model.response.nomenclature.customer.*;
import bg.energo.phoenix.service.salesportal.nomenclature.customer.SalesPortalCustomerNomenclaturesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/nomenclature/customer")
@Tag(name = "Nomenclatures - Customer", description = "Customer nomenclatures. (OAuth2 Client Credentials).")
public class SalesPortalCustomerNomenclaturesController {

    private final SalesPortalCustomerNomenclaturesService salesPortalCustomerNomenclaturesService;

    @GetMapping("/account-manager-type/{id}")
    @Operation(
            summary = "Get customer account manager type by id",
            description = """
                    Returns a single **customer account manager type** nomenclature record by id.

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
                            schema = @Schema(implementation = AccountManagerTypeResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<AccountManagerTypeResponse> viewAccountManagerType(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewAccountManagerType(id), HttpStatus.OK);
    }

    @GetMapping("/bank/{id}")
    @Operation(
            summary = "Get customer bank by id",
            description = """
                    Returns a single **customer bank** nomenclature record by id.

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
                            schema = @Schema(implementation = BankResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<BankResponse> viewBank(@PathVariable("id") Long id,
                                                 @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewBank(id, language), HttpStatus.OK);
    }

    @GetMapping("/belonging-capital-owner/{id}")
    @Operation(
            summary = "Get customer belonging capital owner by id",
            description = """
                    Returns a single **customer belonging capital owner** nomenclature record by id.

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
                            schema = @Schema(implementation = BelongingCapitalOwnerResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<BelongingCapitalOwnerResponse> viewBelongingCapitalOwner(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewBelongingCapitalOwner(id), HttpStatus.OK);
    }

    @GetMapping("/ci-connection-type/{id}")
    @Operation(
            summary = "Get customer ci connection type by id",
            description = """
                    Returns a single **customer ci connection type** nomenclature record by id.

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
                            schema = @Schema(implementation = CiConnectionTypeResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<CiConnectionTypeResponse> viewCiConnectionType(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewCiConnectionType(id), HttpStatus.OK);
    }

    @GetMapping("/contact-purpose/{id}")
    @Operation(
            summary = "Get customer contact purpose by id",
            description = """
                    Returns a single **customer contact purpose** nomenclature record by id.

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
                            schema = @Schema(implementation = ContactPurposeResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ContactPurposeResponse> viewContactPurpose(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewContactPurpose(id), HttpStatus.OK);
    }

    @GetMapping("/credit-rating/{id}")
    @Operation(
            summary = "Get customer credit rating by id",
            description = """
                    Returns a single **customer credit rating** nomenclature record by id.

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
                            schema = @Schema(implementation = CreditRatingResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<CreditRatingResponse> viewCreditRating(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewCreditRating(id), HttpStatus.OK);
    }

    @GetMapping("/economic-branch-ci/{id}")
    @Operation(
            summary = "Get customer economic branch ci by id",
            description = """
                    Returns a single **customer economic branch ci** nomenclature record by id.

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
                            schema = @Schema(implementation = EconomicBranchCIResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<EconomicBranchCIResponse> viewEconomicBranchCI(@PathVariable("id") Long id,
                                                                         @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewEconomicBranchCI(id, language), HttpStatus.OK);
    }

    @GetMapping("/economic-branch-ncea/{id}")
    @Operation(
            summary = "Get customer economic branch ncea by id",
            description = """
                    Returns a single **customer economic branch ncea** nomenclature record by id.

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
                            schema = @Schema(implementation = EconomicBranchNCEAResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<EconomicBranchNCEAResponse> viewEconomicBranchNCEA(@PathVariable("id") Long id,
                                                                             @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewEconomicBranchNCEA(id, language), HttpStatus.OK);
    }

    @GetMapping("/gcc-connection-type/{id}")
    @Operation(
            summary = "Get customer gcc connection type by id",
            description = """
                    Returns a single **customer gcc connection type** nomenclature record by id.

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
                            schema = @Schema(implementation = GccConnectionTypeResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<GccConnectionTypeResponse> viewGccConnectionType(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewGccConnectionType(id), HttpStatus.OK);
    }

    @GetMapping("/legal-form/{id}")
    @Operation(
            summary = "Get customer legal form by id",
            description = """
                    Returns a single **customer legal form** nomenclature record by id.

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
                            schema = @Schema(implementation = LegalFormResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<LegalFormResponse> viewLegalForm(@PathVariable("id") Long id,
                                                           @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewLegalForm(id, language), HttpStatus.OK);
    }

    @GetMapping("/missing-customer/{id}")
    @Operation(
            summary = "Get customer missing customer by id",
            description = """
                    Returns a single **customer missing customer** nomenclature record by id.

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
                            schema = @Schema(implementation = MissingCustomerResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<MissingCustomerResponse> viewMissingCustomer(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewMissingCustomer(id), HttpStatus.OK);
    }

    @GetMapping("/ownership-form/{id}")
    @Operation(
            summary = "Get customer ownership form by id",
            description = """
                    Returns a single **customer ownership form** nomenclature record by id.

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
                            schema = @Schema(implementation = OwnershipFormResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<OwnershipFormResponse> viewOwnershipForm(@PathVariable("id") Long id,
                                                                   @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewOwnershipForm(id, language), HttpStatus.OK);
    }

    @GetMapping("/platform/{id}")
    @Operation(
            summary = "Get customer platform by id",
            description = """
                    Returns a single **customer platform** nomenclature record by id.

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
                            schema = @Schema(implementation = PlatformResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<PlatformResponse> viewPlatform(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewPlatform(id), HttpStatus.OK);
    }

    @GetMapping("/preferences/{id}")
    @Operation(
            summary = "Get customer preferences by id",
            description = """
                    Returns a single **customer preferences** nomenclature record by id.

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
                            schema = @Schema(implementation = PreferencesResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<PreferencesResponse> viewPreferences(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewPreferences(id), HttpStatus.OK);
    }

    @GetMapping("/representation-method/{id}")
    @Operation(
            summary = "Get customer representation method by id",
            description = """
                    Returns a single **customer representation method** nomenclature record by id.

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
                            schema = @Schema(implementation = RepresentationMethodResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<RepresentationMethodResponse> viewRepresentationMethod(@PathVariable("id") Long id,
                                                                                 @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewRepresentationMethod(id, language), HttpStatus.OK);
    }

    @GetMapping("/segment/{id}")
    @Operation(
            summary = "Get customer segment by id",
            description = """
                    Returns a single **customer segment** nomenclature record by id.

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
                            schema = @Schema(implementation = SegmentResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<SegmentResponse> viewSegment(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewSegment(id), HttpStatus.OK);
    }

    @GetMapping("/title/{id}")
    @Operation(
            summary = "Get customer title by id",
            description = """
                    Returns a single **customer title** nomenclature record by id.

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
                            schema = @Schema(implementation = TitleResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<TitleResponse> viewTitle(@PathVariable("id") Long id,
                                                   @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewTitle(id, language), HttpStatus.OK);
    }

    @GetMapping("/unwanted-customer-reason/{id}")
    @Operation(
            summary = "Get customer unwanted customer reason by id",
            description = """
                    Returns a single **customer unwanted customer reason** nomenclature record by id.

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
                            schema = @Schema(implementation = UnwantedCustomerReasonResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<UnwantedCustomerReasonResponse> viewUnwantedCustomerReason(@PathVariable("id") Long id,
                                                                                    @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalCustomerNomenclaturesService.viewUnwantedCustomerReason(id, language), HttpStatus.OK);
    }
}
