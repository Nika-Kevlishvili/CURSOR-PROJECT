package bg.energo.phoenix.salesportal.controller.salesPortal.nomenclature;


import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureEnumResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.service.salesportal.nomenclature.SalesPortalNomenclatureMainService;
import io.jsonwebtoken.lang.Objects;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/nomenclature")
@Tag(name = "Nomenclatures", description = "Nomenclature list and filter (OAuth2 Client Credentials).")
@RequiredArgsConstructor
public class SalesPortalNomenclatureMainController {

    private final SalesPortalNomenclatureMainService salesPortalNomenclatureMainService;

    @GetMapping("/list")
    @Operation(
            summary = "List available nomenclature types",
            description = """
                    Returns all nomenclature types exposed by the Sales Portal API with localized labels.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Optional query:** `language` — `BULGARIAN` (default) or `ENGLISH`.

                    Response is a JSON array of `{ name, value }` where `name` is the `Nomenclature` enum
                    and `value` is the translated display label.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Nomenclature type catalog.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = NomenclatureEnumResponse.class)))
    })
    @SalesPortalStandardApiResponses
    public ResponseEntity<List<NomenclatureEnumResponse>> getNomenclatureList(
            @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language
    ) {
        return new ResponseEntity<>(
                salesPortalNomenclatureMainService.getNomenclatureList(Objects.isEmpty(language) ? Language.BULGARIAN : language),
                HttpStatus.OK
        );
    }

    @GetMapping("/{nomenclature}/filter")
    @Operation(
            summary = "Filter nomenclature items (paginated)",
            description = """
                    Returns a paginated list of nomenclature items for the given `nomenclature` type.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `nomenclature` — `Nomenclature` enum value (e.g. `SEGMENT`, `BANK`).

                    **Required query (bound from `NomenclatureItemsBaseFilterRequest`):**
                    `statuses` (list), `page`, `size`.

                    **Optional:** `prompt` (search text), `excludedItemId`, `includedItemIds`,
                    `exactMatch`, `lan` (default `BULGARIAN`).

                    Success HTTP status is **206 Partial Content** (Spring Page JSON).""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "206",
                    description = "Paginated nomenclature items.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = NomenclatureResponse.class)))
    })
    @SalesPortalStandardApiResponses
    public ResponseEntity<Page<NomenclatureResponse>> filterNomenclature(@PathVariable Nomenclature nomenclature,
                                                                         @ParameterObject @Valid NomenclatureItemsBaseFilterRequest filter) {
        return new ResponseEntity<>(
                salesPortalNomenclatureMainService.filterNomenclature(nomenclature, filter),
                HttpStatus.PARTIAL_CONTENT
        );
    }

}
