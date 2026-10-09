package bg.energo.phoenix.salesportal.controller.salesPortal.nomenclature.document;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.response.nomenclature.document.ExpirationPeriodResponse;
import bg.energo.phoenix.service.salesportal.nomenclature.document.SalesPortalDocumentNomenclaturesService;
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
@RequestMapping("/nomenclature/document")
@Tag(name = "Nomenclatures - Document", description = "Document nomenclatures. (OAuth2 Client Credentials).")
public class SalesPortalDocumentNomenclaturesController {

    private final SalesPortalDocumentNomenclaturesService salesPortalDocumentNomenclaturesService;

    @GetMapping("/expiration-period/{id}")
    @Operation(
            summary = "Get document expiration period by id",
            description = """
                    Returns a single **document expiration period** nomenclature record by id.

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
                            schema = @Schema(implementation = ExpirationPeriodResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<ExpirationPeriodResponse> viewExpirationPeriod(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalDocumentNomenclaturesService.viewExpirationPeriod(id), HttpStatus.OK);
    }
}
