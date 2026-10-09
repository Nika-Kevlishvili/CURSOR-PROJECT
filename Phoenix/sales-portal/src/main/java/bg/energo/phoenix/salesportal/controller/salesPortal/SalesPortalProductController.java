package bg.energo.phoenix.salesportal.controller.salesPortal;

import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.model.response.product.ProductListForSalesChannelProjection;
import bg.energo.phoenix.model.response.product.SalesPortalProductListProjection;
import bg.energo.phoenix.service.product.product.ProductService;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
@Tag(name = "product", description = "Product catalog listing (OAuth2 Client Credentials)")
public class SalesPortalProductController {

    private final ProductService productService;

    @GetMapping("/list")
    @Operation(
            summary = "List products for sales channel",
            description = """
                    Returns all active products eligible for the fixed sales channel (id = 1).

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Optional query:** `language` — `BULGARIAN` (default) or `ENGLISH` for translated labels.

                    Response is a JSON array (not paginated). Includes contract term fields and
                    `priceComponents` as embedded JSON. Products are filtered by complex eligibility
                    rules in the repository query (active status, payment guarantee, interim payments, etc.).""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Product catalog list.",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProductListForSalesChannelProjection.class))))
    })
    @SalesPortalStandardApiResponses
    public ResponseEntity<List<ProductListForSalesChannelProjection>> getProductListForSalesChannel(
            @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language
    ) {
        return ResponseEntity.ok(productService.getProductListForSalesChannel(language));
    }

    @GetMapping("/list-with-pod-and-customer")
    @Operation(
            summary = "List products filtered by POD and/or customer",
            description = """
                    Returns products matching POD parameters and/or customer context.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Query:** optional `podIdentifier`, optional `customerIdentifier` (UIC).
                    At least one filter is typically provided. Optional `language` (default `BULGARIAN`).

                    Each item includes `resultSource` indicating the matching rule:
                    `STANDARD_STRICT`, `STANDARD_POD`, `RESIGN_RELAXED`, or `STANDARD_CUSTOMER`.

                    `contractTerms` and `priceComponents` are embedded JSON strings.
                    Response is a JSON array (not paginated).""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Context-filtered product list.",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = SalesPortalProductListProjection.class))))
    })
    @SalesPortalStandardApiResponses
    public ResponseEntity<List<SalesPortalProductListProjection>> getSalesPortalProductListWithPodAndCustomer(
            @RequestParam(required = false) String podIdentifier,
            @RequestParam(required = false) String customerIdentifier,
            @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language
    ) {
        return ResponseEntity.ok(
                productService.getSalesPortalProductListWithPodAndCustomer(podIdentifier, customerIdentifier, language)
        );
    }
}
