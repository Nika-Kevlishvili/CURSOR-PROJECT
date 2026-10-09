package bg.energo.phoenix.salesportal.controller.salesPortal.nomenclature.address;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.model.response.nomenclature.address.*;
import bg.energo.phoenix.service.salesportal.nomenclature.address.SalesPortalAddressNomenclaturesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/nomenclature/address")
@Tag(name = "Nomenclatures - Address", description = "Address nomenclatures. (OAuth2 Client Credentials).")
public class SalesPortalAddressNomenclaturesController {

    private final SalesPortalAddressNomenclaturesService salesPortalAddressNomenclaturesService;

    @GetMapping("/country/{id}")
    @Operation(
            summary = "Get address country by id",
            description = """
                    Returns a single **address country** nomenclature record by id.

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
                            schema = @Schema(implementation = CountryResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<CountryResponse> viewCountry(@PathVariable("id") Long id,
                                                       @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalAddressNomenclaturesService.viewCountry(id, language), HttpStatus.OK);
    }

    @GetMapping("/district/{id}")
    @Operation(
            summary = "Get address district by id",
            description = """
                    Returns a single **address district** nomenclature record by id.

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
                            schema = @Schema(implementation = DistrictResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<DistrictResponse> viewDistrict(@PathVariable("id") Long id,
                                                         @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalAddressNomenclaturesService.viewDistrict(id, language), HttpStatus.OK);
    }

    @GetMapping("/municipality/{id}")
    @Operation(
            summary = "Get address municipality by id",
            description = """
                    Returns a single **address municipality** nomenclature record by id.

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
                            schema = @Schema(implementation = MunicipalityResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<MunicipalityResponse> viewMunicipality(@PathVariable("id") Long id,
                                                                 @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalAddressNomenclaturesService.viewMunicipality(id, language), HttpStatus.OK);
    }

    @GetMapping("/populated-place/{id}")
    @Operation(
            summary = "Get address populated place by id",
            description = """
                    Returns a single **address populated place** nomenclature record by id.

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
                            schema = @Schema(implementation = PopulatedPlaceResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<PopulatedPlaceResponse> viewPopulatedPlace(@PathVariable("id") Long id,
                                                                     @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalAddressNomenclaturesService.viewPopulatedPlace(id, language), HttpStatus.OK);
    }

    @GetMapping("/region/{id}")
    @Operation(
            summary = "Get address region by id",
            description = """
                    Returns a single **address region** nomenclature record by id.

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
                            schema = @Schema(implementation = RegionResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<RegionResponse> viewRegion(@PathVariable("id") Long id,
                                                     @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalAddressNomenclaturesService.viewRegion(id, language), HttpStatus.OK);
    }

    @GetMapping("/residential-area/{id}")
    @Operation(
            summary = "Get address residential area by id",
            description = """
                    Returns a single **address residential area** nomenclature record by id.

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
                            schema = @Schema(implementation = ResidentialAreaResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ResidentialAreaResponse> viewResidentialArea(@PathVariable("id") Long id,
                                                                       @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalAddressNomenclaturesService.viewResidentialArea(id, language), HttpStatus.OK);
    }

    @GetMapping("/street/{id}")
    @Operation(
            summary = "Get address street by id",
            description = """
                    Returns a single **address street** nomenclature record by id.

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
                            schema = @Schema(implementation = StreetsResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<StreetsResponse> viewStreet(@PathVariable("id") Long id,
                                                      @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalAddressNomenclaturesService.viewStreet(id, language), HttpStatus.OK);
    }

    @GetMapping("/zip-code/{id}")
    @Operation(
            summary = "Get address zip code by id",
            description = """
                    Returns a single **address zip code** nomenclature record by id.

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
                            schema = @Schema(implementation = ZipCodeResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ZipCodeResponse> viewZipCode(@PathVariable("id") Long id,
                                                       @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalAddressNomenclaturesService.viewZipCode(id, language), HttpStatus.OK);
    }
}
