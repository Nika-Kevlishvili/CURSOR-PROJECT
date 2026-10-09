package bg.energo.phoenix.salesportal.controller.salesPortal.nomenclature.product;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.model.response.nomenclature.goods.GoodsGroupsResponse;
import bg.energo.phoenix.model.response.nomenclature.goods.GoodsSuppliersResponse;
import bg.energo.phoenix.model.response.nomenclature.goods.GoodsUnitsResponse;
import bg.energo.phoenix.model.response.nomenclature.priceComponent.PriceComponentPriceTypeResponse;
import bg.energo.phoenix.model.response.nomenclature.priceComponent.PriceComponentValueTypeResponse;
import bg.energo.phoenix.model.response.nomenclature.priceComponent.ScalesResponse;
import bg.energo.phoenix.model.response.nomenclature.product.*;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarResponse;
import bg.energo.phoenix.service.salesportal.nomenclature.product.SalesPortalProductNomenclaturesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/nomenclature/product")
@Tag(name = "Nomenclatures - Product", description = "Product nomenclatures. (OAuth2 Client Credentials).")
public class SalesPortalProductNomenclaturesController {

    private final SalesPortalProductNomenclaturesService salesPortalProductNomenclaturesService;

    @GetMapping("/calendar/{id}")
    @Operation(
            summary = "Get product calendar by id",
            description = """
                    Returns a single **product calendar** nomenclature record by id.

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
                            schema = @Schema(implementation = CalendarResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<CalendarResponse> viewCalendar(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewCalendar(id), HttpStatus.OK);
    }

    @GetMapping("/currency/{id}")
    @Operation(
            summary = "Get product currency by id",
            description = """
                    Returns a single **product currency** nomenclature record by id.

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
                            schema = @Schema(implementation = CurrencyResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<CurrencyResponse> viewCurrency(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewCurrency(id), HttpStatus.OK);
    }

    @GetMapping("/electricity-price-type/{id}")
    @Operation(
            summary = "Get product electricity price type by id",
            description = """
                    Returns a single **product electricity price type** nomenclature record by id.

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
                            schema = @Schema(implementation = ElectricityPriceTypeResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ElectricityPriceTypeResponse> viewElectricityPriceType(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewElectricityPriceType(id), HttpStatus.OK);
    }

    @GetMapping("/goods-groups/{id}")
    @Operation(
            summary = "Get product goods groups by id",
            description = """
                    Returns a single **product goods groups** nomenclature record by id.

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
                            schema = @Schema(implementation = GoodsGroupsResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<GoodsGroupsResponse> viewGoodsGroups(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewGoodsGroups(id), HttpStatus.OK);
    }

    @GetMapping("/goods-suppliers/{id}")
    @Operation(
            summary = "Get product goods suppliers by id",
            description = """
                    Returns a single **product goods suppliers** nomenclature record by id.

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
                            schema = @Schema(implementation = GoodsSuppliersResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<GoodsSuppliersResponse> viewGoodsSuppliers(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewGoodsSuppliers(id), HttpStatus.OK);
    }

    @GetMapping("/goods-units/{id}")
    @Operation(
            summary = "Get product goods units by id",
            description = """
                    Returns a single **product goods units** nomenclature record by id.

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
                            schema = @Schema(implementation = GoodsUnitsResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<GoodsUnitsResponse> viewGoodsUnits(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewGoodsUnits(id), HttpStatus.OK);
    }

    @GetMapping("/grid-operator/{id}")
    @Operation(
            summary = "Get product grid operator by id",
            description = """
                    Returns a single **product grid operator** nomenclature record by id.

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
                            schema = @Schema(implementation = GridOperatorResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<GridOperatorResponse> viewGridOperator(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewGridOperator(id), HttpStatus.OK);
    }

    @GetMapping("/price-component-price-type/{id}")
    @Operation(
            summary = "Get product price component price type by id",
            description = """
                    Returns a single **product price component price type** nomenclature record by id.

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
                            schema = @Schema(implementation = PriceComponentPriceTypeResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<PriceComponentPriceTypeResponse> viewPriceComponentPriceType(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewPriceComponentPriceType(id), HttpStatus.OK);
    }

    @GetMapping("/price-component-value-type/{id}")
    @Operation(
            summary = "Get product price component value type by id",
            description = """
                    Returns a single **product price component value type** nomenclature record by id.

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
                            schema = @Schema(implementation = PriceComponentValueTypeResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<PriceComponentValueTypeResponse> viewPriceComponentValueType(@PathVariable("id") Long id,
                                                                                       @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewPriceComponentValueType(id, language), HttpStatus.OK);
    }

    @GetMapping("/product-groups/{id}")
    @Operation(
            summary = "Get product product groups by id",
            description = """
                    Returns a single **product product groups** nomenclature record by id.

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
                            schema = @Schema(implementation = ProductGroupsResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ProductGroupsResponse> viewProductGroups(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewProductGroups(id), HttpStatus.OK);
    }

    @GetMapping("/product-types/{id}")
    @Operation(
            summary = "Get product product types by id",
            description = """
                    Returns a single **product product types** nomenclature record by id.

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
                            schema = @Schema(implementation = ProductTypesResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ProductTypesResponse> viewProductTypes(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewProductTypes(id), HttpStatus.OK);
    }

    @GetMapping("/sales-area/{id}")
    @Operation(
            summary = "Get product sales area by id",
            description = """
                    Returns a single **product sales area** nomenclature record by id.

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
                            schema = @Schema(implementation = SalesAreaResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<SalesAreaResponse> viewSalesArea(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewSalesArea(id), HttpStatus.OK);
    }

    @GetMapping("/sales-channel/{id}")
    @Operation(
            summary = "Get product sales channel by id",
            description = """
                    Returns a single **product sales channel** nomenclature record by id.

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
                            schema = @Schema(implementation = SalesChannelResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<SalesChannelResponse> viewSalesChannel(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewSalesChannel(id), HttpStatus.OK);
    }

    @GetMapping("/scales/{id}")
    @Operation(
            summary = "Get product scales by id",
            description = """
                    Returns a single **product scales** nomenclature record by id.

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
                            schema = @Schema(implementation = ScalesResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ScalesResponse> viewScales(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewScales(id), HttpStatus.OK);
    }

    @GetMapping("/service-groups/{id}")
    @Operation(
            summary = "Get product service groups by id",
            description = """
                    Returns a single **product service groups** nomenclature record by id.

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
                            schema = @Schema(implementation = ServiceGroupsResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ServiceGroupsResponse> viewServiceGroups(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewServiceGroups(id), HttpStatus.OK);
    }

    @GetMapping("/service-type/{id}")
    @Operation(
            summary = "Get product service type by id",
            description = """
                    Returns a single **product service type** nomenclature record by id.

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
                            schema = @Schema(implementation = ServiceTypeResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ServiceTypeResponse> viewServiceType(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewServiceType(id), HttpStatus.OK);
    }

    @GetMapping("/service-unit/{id}")
    @Operation(
            summary = "Get product service unit by id",
            description = """
                    Returns a single **product service unit** nomenclature record by id.

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
                            schema = @Schema(implementation = ServiceUnitResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ServiceUnitResponse> viewServiceUnit(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewServiceUnit(id), HttpStatus.OK);
    }

    @GetMapping("/vat-rate/{id}")
    @Operation(
            summary = "Get product vat rate by id",
            description = """
                    Returns a single **product vat rate** nomenclature record by id.

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
                            schema = @Schema(implementation = VatRateResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<VatRateResponse> viewVatRate(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalProductNomenclaturesService.viewVatRate(id), HttpStatus.OK);
    }
}
