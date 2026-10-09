package bg.energo.phoenix.salesportal.controller.salesPortal;

import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.request.contract.express.ExpressContractRequest;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.service.contract.product.ProductContractFilesService;
import bg.energo.phoenix.service.salesportal.SalesPortalExpressContractService;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/express-contract")
@Tag(name = "Product contract", description = "Product/express contract creation, update, CSP documents, status, jobs and lookups (OAuth2 Client Credentials)")
public class SalesPortalExpressContractController {

    private final SalesPortalExpressContractService salesPortalExpressContractService;
    private final ProductContractFilesService productContractFilesService;

    @PostMapping("/create")
    @Operation(
            summary = "Create express contract",
            description = """
                    Creates a new express product or service contract.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Body:** `ExpressContractRequest`. For a **known customer** and a **standard product**
                    with a single option per adjustable field, only these are required:
                    - `expressContractType`
                    - `expressContractParameters.productId` / `productVersionId`
                    - `expressContractParameters.subStatus` (`SIGNED_BY_BOTH_SIDES` or `SIGNED_BY_CUSTOMER`)
                    - `customer.identifier`
                    - `customer.systemSourceId`
                    - `podDetailIds`

                    `subStatus` = `SIGNED_BY_BOTH_SIDES` makes `signingDate` mandatory; an omitted date is
                    set to today. `subStatus` = `SIGNED_BY_CUSTOMER` does not use `signingDate`.

                    `expressContractParameters.contractNumber` may be omitted (the system generates
                    PREFIX + origin digit + YYMM + six-digit serial) or supplied in that same format.
                    Origin digit is 1 for `PHOENIX`, 2 for `SALES_PORTAL` and `SELF_SERVICE_PORTAL`,
                    and 3 for `VCOK`.

                    Omitted fields are auto-filled (estimatedTotalConsumption from
                    CONSUMER PODs, customer profile from DB, productParameters from single-option product
                    version, proxyRequest=[]). Values that **are** provided are validated and never overwritten.
                    A partial `customer.privateCustomerDetails` (e.g. only `kycPassed`) still gets omitted
                    name / DOB fields filled from the known customer.
                    `expressContractParameters.bankingDetails` may be omitted entirely — the contract is then
                    created without direct debit (`bankId` / `iban` are only required when `directDebit: true`).

                    `customer.systemSourceId` is **mandatory**. Allowed values are `PHOENIX`,
                    `SALES_PORTAL`, `SELF_SERVICE_PORTAL` and `VCOK`. The value is saved on the contract
                    and on the customer and communication data created from the request.

                    ### When `customer.identifier` is NOT yet in Phoenix

                    None of the auto-fill above can fire, so the caller must send the whole profile:
                    `customerType`, `foreign`, `customerSegments`, `address`, and the name fields of
                    `privateCustomerDetails` (or `businessCustomerDetails` for a legal entity).

                    On top of that, `customer.communications` must contain **both**
                    `CONTRACT_COMMUNICATION` and `INVOICE_ISSUANCE`, and `customer.address` must be present.
                    Missing either fails with `customer.communications-communications or address is not correct;`
                    — a single message that does not say which of the two conditions failed.
                    `PERMANENT_ADDRESS`, the third `ExpressCommunicationTypes` value, is accepted but not required.

                    ### `kycPassed` is mandatory

                    `privateCustomerDetails.kycPassed` must be sent whenever `privateCustomerDetails` is
                    present; omitting it fails with
                    `customer.privateCustomerDetails.kycPassed-KYC passed flag is mandatory;`. For a legal
                    entity the same applies to every entry of `customer.managerRequests`. The field carries no
                    bean constraint because for a **known** customer it is filled from the stored record before
                    validation runs — so in practice only the new-customer path has to send it.

                    `kycExpirationDate` may only be set when `kycPassed` is `true`, and must then be today or
                    later. If a known customer's stored KYC is flagged passed but has no date or an elapsed one,
                    the request fails on `customer.privateCustomerDetails.kycExpirationDate` and both fields have
                    to be sent explicitly.

                    ### Enum note

                    `productParameters.contractType` is the **product** contract type
                    (`COMBINED`, `SUPPLY_ONLY`, `SUPPLY_BALANCING`, `WITHOUT_SUPPLY`) — not
                    `PRODUCT_CONTRACT`/`SERVICE_CONTRACT`, which is a different enum of the same name.

                    **Returns:** the new contract id (number) on success.

                    ### How to create a contract from Swagger UI
                    1. **Authorize** - call `POST /oauth2/token` (`grant_type=client_credentials`), copy the
                       returned JWT, then click **Authorize** (top-right) and paste it.
                    2. **Look up ids** for your environment (`productId`/`productVersionId`, `podDetailIds`).
                    3. Pick the example that matches your case: *1-ExistingCustomerMinimal*,
                       *2-ExistingCustomerFull*, *3-NewCustomerMinimal* or *4-NewCustomerFull*.
                       The ids in them are environment-specific — replace them with values from step 2.
                    4. Click **Try it out**, then **Execute**.
                    5. **200** - the response body is the new **contract id**.
                    6. *(Optional)* upload the signed contract PDF via `POST /express-contract/upload-file`.""",
            security = @SecurityRequirement(name = "bearer-token"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ExpressContractRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "1-ExistingCustomerMinimal",
                                            summary = "Known customer, standard product — everything else is auto-filled",
                                            value = """
                                                    {
                                                      "expressContractType": "PRODUCT",
                                                      "expressContractParameters": {
                                                        "productId": 44,
                                                        "productVersionId": 1,
                                                        "subStatus": "SIGNED_BY_BOTH_SIDES"
                                                      },
                                                      "customer": {
                                                        "identifier": "7007178018",
                                                        "systemSourceId": "SALES_PORTAL"
                                                      },
                                                      "podDetailIds": [48]
                                                    }"""),
                                    @ExampleObject(
                                            name = "2-ExistingCustomerFull",
                                            summary = "Known customer with every field sent explicitly",
                                            value = """
                                                    {
                                                      "expressContractType": "PRODUCT",
                                                      "expressContractParameters": {
                                                        "productId": 44,
                                                        "productVersionId": 1,
                                                        "subStatus": "SIGNED_BY_BOTH_SIDES",
                                                        "procurementLaw": false,
                                                        "signingDate": "2026-08-21",
                                                        "estimatedTotalConsumption": 12.000,
                                                        "bankingDetails": { "directDebit": true, "bankId": 1000, "iban": "BG80BNBG96611020345678" }
                                                      },
                                                      "customer": {
                                                        "identifier": "7007178018",
                                                        "systemSourceId": "SALES_PORTAL",
                                                        "customerType": "PRIVATE_CUSTOMER",
                                                        "foreign": false,
                                                        "privateCustomerDetails": {
                                                          "firstName": "РОСИЦА",
                                                          "firstNameTranslated": "ROSITSA",
                                                          "lastName": "ЙОРДАНОВА",
                                                          "lastNameTranslated": "YORDANOVA",
                                                          "kycPassed": false
                                                        },
                                                        "customerSegments": [1013],
                                                        "address": {
                                                          "foreign": false,
                                                          "localAddressData": { "countryId": 1027, "populatedPlaceId": 2460, "zipCodeId": 2439 }
                                                        }
                                                      },
                                                      "productParameters": {
                                                        "contractType": "COMBINED",
                                                        "productContractTermId": 47,
                                                        "paymentGuarantee": "NO",
                                                        "invoicePaymentTermId": 77,
                                                        "entryIntoForce": "SIGNING",
                                                        "startOfContractInitialTerm": "SIGNING",
                                                        "supplyActivation": "MANUAL",
                                                        "contractFormulas": [],
                                                        "interimAdvancePayments": []
                                                      },
                                                      "podDetailIds": [48],
                                                      "proxyRequest": []
                                                    }"""),
                                    @ExampleObject(
                                            name = "3-NewCustomerMinimal",
                                            summary = "Identifier not yet in Phoenix — profile, segments, address and two communications are all required",
                                            value = """
                                                    {
                                                      "expressContractType": "PRODUCT",
                                                      "expressContractParameters": {
                                                        "productId": 44,
                                                        "productVersionId": 1,
                                                        "subStatus": "SIGNED_BY_BOTH_SIDES"
                                                      },
                                                      "customer": {
                                                        "identifier": "9505124512",
                                                        "systemSourceId": "SALES_PORTAL",
                                                        "customerType": "PRIVATE_CUSTOMER",
                                                        "foreign": false,
                                                        "customerSegments": [1013],
                                                        "privateCustomerDetails": {
                                                          "firstName": "МАРИЯ",
                                                          "firstNameTranslated": "MARIYA",
                                                          "lastName": "ДИМИТРОВА",
                                                          "lastNameTranslated": "DIMITROVA",
                                                          "kycPassed": false
                                                        },
                                                        "address": {
                                                          "foreign": false,
                                                          "localAddressData": { "countryId": 1027, "populatedPlaceId": 2460, "zipCodeId": 2439 }
                                                        },
                                                        "communications": [
                                                          {
                                                            "communicationTypes": "CONTRACT_COMMUNICATION",
                                                            "address": { "foreign": false, "localAddressData": { "countryId": 1027, "populatedPlaceId": 2460, "zipCodeId": 2439 } },
                                                            "contactRequests": [
                                                              { "contactType": "MOBILE_NUMBER", "contactValue": "+359888111111" },
                                                              { "contactType": "EMAIL", "contactValue": "mariya.dimitrova@example.com" }
                                                            ]
                                                          },
                                                          {
                                                            "communicationTypes": "INVOICE_ISSUANCE",
                                                            "address": { "foreign": false, "localAddressData": { "countryId": 1027, "populatedPlaceId": 2460, "zipCodeId": 2439 } },
                                                            "contactRequests": [
                                                              { "contactType": "EMAIL", "contactValue": "mariya.dimitrova@example.com" }
                                                            ]
                                                          }
                                                        ]
                                                      },
                                                      "podDetailIds": [48]
                                                    }"""),
                                    @ExampleObject(
                                            name = "4-NewCustomerFull",
                                            summary = "New customer with all three communication types, KYC, preferences and a proxy",
                                            value = """
                                                    {
                                                      "expressContractType": "PRODUCT",
                                                      "expressContractParameters": {
                                                        "productId": 44,
                                                        "productVersionId": 1,
                                                        "subStatus": "SIGNED_BY_BOTH_SIDES",
                                                        "signingDate": "2026-08-21",
                                                        "estimatedTotalConsumption": 12.000,
                                                        "bankingDetails": { "directDebit": true, "bankId": 1000, "iban": "BG80BNBG96611020345678" }
                                                      },
                                                      "customer": {
                                                        "identifier": "9505124512",
                                                        "systemSourceId": "SALES_PORTAL",
                                                        "customerType": "PRIVATE_CUSTOMER",
                                                        "foreign": false,
                                                        "preferCommunicationInEnglish": false,
                                                        "consentToMarketingCommunication": true,
                                                        "customerSegments": [1013],
                                                        "privateCustomerDetails": {
                                                          "firstName": "МАРИЯ",
                                                          "firstNameTranslated": "MARIYA",
                                                          "middleName": "ИВАНОВА",
                                                          "middleNameTranslated": "IVANOVA",
                                                          "lastName": "ДИМИТРОВА",
                                                          "lastNameTranslated": "DIMITROVA",
                                                          "birthDate": "1995-05-12",
                                                          "kycPassed": true,
                                                          "kycExpirationDate": "2027-12-31"
                                                        },
                                                        "address": {
                                                          "foreign": false,
                                                          "localAddressData": { "countryId": 1027, "populatedPlaceId": 2460, "zipCodeId": 2439 },
                                                          "number": "10"
                                                        },
                                                        "communications": [
                                                          { "communicationTypes": "PERMANENT_ADDRESS", "address": { "foreign": false, "localAddressData": { "countryId": 1027, "populatedPlaceId": 2460, "zipCodeId": 2439 } }, "contactRequests": [ { "contactType": "MOBILE_NUMBER", "contactValue": "+359888111111" } ] },
                                                          { "communicationTypes": "CONTRACT_COMMUNICATION", "address": { "foreign": false, "localAddressData": { "countryId": 1027, "populatedPlaceId": 2460, "zipCodeId": 2439 } }, "contactRequests": [ { "contactType": "EMAIL", "contactValue": "mariya.dimitrova@example.com" } ] },
                                                          { "communicationTypes": "INVOICE_ISSUANCE", "address": { "foreign": false, "localAddressData": { "countryId": 1027, "populatedPlaceId": 2460, "zipCodeId": 2439 } }, "contactRequests": [ { "contactType": "EMAIL", "contactValue": "invoices@example.com" } ] }
                                                        ]
                                                      },
                                                      "productParameters": {
                                                        "contractType": "COMBINED",
                                                        "productContractTermId": 47,
                                                        "paymentGuarantee": "NO",
                                                        "invoicePaymentTermId": 77,
                                                        "entryIntoForce": "SIGNING",
                                                        "startOfContractInitialTerm": "SIGNING",
                                                        "supplyActivation": "MANUAL",
                                                        "contractFormulas": [],
                                                        "interimAdvancePayments": []
                                                      },
                                                      "podDetailIds": [48],
                                                      "proxyRequest": []
                                                    }""")
                            })))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Express contract created. Response body is the new contract id.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "integer", format = "int64", example = "10234"),
                            examples = @ExampleObject(name = "CreatedContractId", summary = "The new contract id", value = "10234")))
    })
    @SalesPortalStandardApiResponses
    public ResponseEntity<Long> create(@RequestBody ExpressContractRequest request) {
        // Deliberately not @Valid: the minimal body is completed by the service before the bean
        // constraints are evaluated. See SalesPortalExpressContractService#create.
        return ResponseEntity.ok(salesPortalExpressContractService.create(request));
    }

    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload signed contract file",
            description = """
                    Uploads a contract attachment file (multipart). File is stored with `SIGNED` status
                    for use during express contract creation / signing flow.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Form field:** `multipartFile` — the file to upload.

                    Returns file metadata with statuses.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "File uploaded successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FileWithStatusesResponse.class)))
    })
    @SalesPortalStandardApiResponses
    public ResponseEntity<FileWithStatusesResponse> upload(@RequestParam MultipartFile multipartFile) {
        return ResponseEntity.ok(productContractFilesService.upload(multipartFile, List.of(DocumentFileStatus.SIGNED)));
    }

}
