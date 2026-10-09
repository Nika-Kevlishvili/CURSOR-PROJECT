package bg.energo.phoenix.salesportal.controller.salesPortal;

import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.service.product.product.ProductService;
import bg.energo.phoenix.service.salesportal.SalesPortalContractAndCustomerUpdateService;
import bg.energo.phoenix.service.salesportal.SalesPortalService;
import bg.energo.phoenix.service.salesportal.model.SalesPortalContractVersionDetailResponse;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalContractAndCustomerUpdateRequest;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import bg.energo.phoenix.util.UrlEncodingUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static bg.energo.phoenix.permissions.PermissionEnum.INDIVIDUAL_PRODUCT_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.PRODUCT_VIEW_BASIC;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/product-contract")
@Tag(name = "Product contract", description = "Product/express contract creation, update, CSP documents, status, jobs and lookups (OAuth2 Client Credentials)")
public class SalesPortalProductContractController {

    private final ProductService productService;
    private final SalesPortalService salesPortalService;
    private final SalesPortalContractAndCustomerUpdateService salesPortalContractAndCustomerUpdateService;

    @PutMapping
    @Operation(
            summary = "Update product contract and customer",
            description = """
                    Atomic update of a product contract version and linked customer data.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Body:** `SalesPortalContractAndCustomerUpdateRequest` with mandatory `contract` and `customer` sections.

                    The handler is `@Valid`, and `contract` declares **24** mandatory fields — the example
                    below carries all of them. `customer` requires `customerSegmentIds` (non-empty) and
                    `address.isForeign`. Note the JSON key is `isForeign`, not `foreign`.

                    `contractType` is the **product** contract type (`COMBINED`, `SUPPLY_ONLY`,
                    `SUPPLY_BALANCING`, `WITHOUT_SUPPLY`) — not `PRODUCT_CONTRACT`/`SERVICE_CONTRACT`,
                    which is a different enum of the same name.

                    **Contract patch semantics:** mandatory spec fields on every update; omitted optional fields cleared;
                    `podIdentifiers` defines the full POD set (add/remove/replace). Status/sub-status/signing dates
                    and `directDebit` follow special merge rules.

                    **Customer patch semantics:** mandatory segments + address; type-specific name/nomenclature fields;
                    manager phones/emails not sent → existing contacts kept.

                    **HTTP status note:** validation and business-rule failures return **400** with `RestError`.
                    Message format: `field-Message;`.""",
            security = @SecurityRequirement(name = "bearer-token"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalContractAndCustomerUpdateRequest.class),
                            examples = @ExampleObject(
                                    name = "Minimal",
                                    summary = "Every mandatory field — the body is @Valid, so all 24 must be present. Ids are environment-specific.",
                                    value = """
                                            {
                                              "contract": {
                                                "contractId": 2741,
                                                "contractVersionId": 1,
                                                "contractStatus": "SIGNED",
                                                "contractSubStatus": "SIGNED_BY_CUSTOMER",
                                                "signingDate": "2026-08-21",
                                                "estimatedTotalConsumption": 12.000,
                                                "productId": 44,
                                                "productVersionId": 1,
                                                "contractType": "COMBINED",
                                                "contractTermId": 47,
                                                "invoicePaymentTermId": 77,
                                                "paymentGuarantee": "NO",
                                                "cashDepositAmount": 0.00,
                                                "cashDepositCurrencyId": 1001,
                                                "bankGuaranteeAmount": 0.00,
                                                "bankGuaranteeCurrencyId": 1001,
                                                "entryIntoForce": "SIGNING",
                                                "startOfInitialTerm": "SIGNING",
                                                "supplyActivationAfterContractResigning": "MANUAL",
                                                "waitForOldContractTermToExpire": "NO",
                                                "customerUic": "0847038794",
                                                "communicationDataContractId": 1609,
                                                "communicationDataBillingId": 1610,
                                                "podIdentifiers": ["32Z420101227152V"]
                                              },
                                              "customer": {
                                                "customerSegmentIds": [1013],
                                                "address": { "isForeign": false }
                                              }
                                            }"""))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Contract and customer updated successfully (empty body).")
    })
    @SalesPortalStandardApiResponses
    public ResponseEntity<Void> updateContractAndCustomer(
            @Valid @RequestBody SalesPortalContractAndCustomerUpdateRequest request
    ) {
        salesPortalContractAndCustomerUpdateService.updateContractAndCustomer(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/csp-document/generate")
    @Operation(
            summary = "Generate CSP product document (PDF)",
            description = """
                    Generates a CSP (product) document as PDF for the given product version.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Query:** `productId`, `versionId` — product and version to render.

                    **Body:** raw JSON string passed to the document template engine.

                    The `@PermissionValidator` on this method (`PRODUCT_VIEW_BASIC` /
                    `INDIVIDUAL_PRODUCT_VIEW_BASIC`) does **not** apply to this API:
                    `PermissionValidationAspect` returns early for a client-credentials principal, which is
                    every caller here. The OAuth2 token is the only gate, as on every other Sales Portal
                    endpoint.

                    The request body is an untyped JSON string forwarded to the document template engine —
                    which keys it consumes is not defined in code, so treat the example as illustrative.

                    Returns PDF bytes with Content-Disposition attachment header.""",
            security = @SecurityRequirement(name = "bearer-token"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Template data as raw JSON string.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "TemplatePayload",
                                    value = "{\"customerName\": \"Example Ltd\"}"))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Generated PDF document.",
                    content = @Content(
                            mediaType = "application/pdf",
                            schema = @Schema(type = "string", format = "binary")))
    })
    @SalesPortalStandardApiResponses
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.PRODUCTS,
                            permissions = {PRODUCT_VIEW_BASIC, INDIVIDUAL_PRODUCT_VIEW_BASIC})
            }
    )
    public HttpEntity<ByteArrayResource> generateDcaDocument(
            @RequestParam("productId")Long productId,
            @RequestParam("versionId") Long versionId,
            @RequestBody  String request) {
        FileContent content = productService.generateCSPDocument(productId,versionId,request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=%s".formatted(UrlEncodingUtil.encodeFileName(content.getFileName())));
        return new HttpEntity<>(new ByteArrayResource(content.getContent()), headers);
    }

    @GetMapping("/{id}/version/{versionID}")
    @Operation(
            summary = "Get contract version detail",
            description = """
                    Returns full detail for a product contract version: delivery/payment methods, product,
                    PODs, customer, business contacts, documents, external additional documents, and signing date.

                    `additionalDocuments` lists only additional documents marked External (`fileId`, `name`).
                    Additional documents marked Internal are omitted. Download an external additional document
                    with `GET /contract/additional-document/{fileId}`.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `id` — contract id, `versionID` — contract details version id (numeric strings).

                    **Optional query:** `language` — default `BULGARIAN` for translated labels.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when contract or version not found.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Contract version detail.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalContractVersionDetailResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<SalesPortalContractVersionDetailResponse> getContractVersionDetail(
            @PathVariable("id") String id,
            @PathVariable("versionID") String versionID,
            @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language
    ) {
        return ResponseEntity.ok(
                salesPortalService.getContractVersionDetail(Long.parseLong(id), Integer.parseInt(versionID), language));
    }
}
