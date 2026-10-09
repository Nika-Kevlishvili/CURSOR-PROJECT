package bg.energo.phoenix.salesportal.controller.salesPortal;

import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.model.response.customer.salesportal.SalesPortalCustomerResponse;
import bg.energo.phoenix.model.response.customer.search.CustomerSearchByIdentifierResponse;
import bg.energo.phoenix.service.salesportal.SalesPortalCustomerUpdateService;
import bg.energo.phoenix.service.salesportal.SalesPortalService;
import bg.energo.phoenix.service.salesportal.customer.SalesPortalCustomerLookupService;
import bg.energo.phoenix.service.salesportal.customer.lookup.CustomerLookupCriteria;
import bg.energo.phoenix.service.salesportal.customer.lookup.LookupMethod;
import bg.energo.phoenix.service.salesportal.model.CustomerListByCoordinatesProjection;
import bg.energo.phoenix.service.salesportal.requests.CustomerListByCoordinatesRequest;
import bg.energo.phoenix.service.salesportal.model.SalesPortalCustomerUpdateResponse;
import bg.energo.phoenix.service.salesportal.model.SalesPortalUnauthorizedResponse;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalCustomerUpdateRequest;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/customer")
@Tag(name = "Customer", description = "Customer lookup, update, search and additional contacts (OAuth2 Client Credentials)")
public class SalesPortalCustomerController {
    private final SalesPortalService salesPortalService;
    private final SalesPortalCustomerUpdateService salesPortalCustomerUpdateService;
    private final SalesPortalCustomerLookupService salesPortalCustomerLookupService;


    @GetMapping("/search-by-identifier")
    @Operation(
            summary = "Search customer by identifier (legacy lookup)",
            description = """
                    Returns customer parameters, address, and managers for a single identifier match.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Required query:** `customerIdentifier` (EGN, UIC, or customer number).

                    **Optional query:** `language` — `BULGARIAN` (default) or `ENGLISH` for translated display values.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when no customer matches the identifier.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer found.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomerSearchByIdentifierResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<CustomerSearchByIdentifierResponse> searchCustomerByIdentifier(
            @RequestParam("customerIdentifier") String customerIdentifier,
            @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language
    ) {
        return ResponseEntity.ok(salesPortalService.searchCustomerByIdentifier(customerIdentifier, language));
    }

    /**
     * Single "get customer" endpoint (PHN-2202): accepts every supported lookup key and resolves the
     * customer by priority among the provided keys (UIC &gt; customer number &gt; contract number &gt;
     * invoice number — first non-blank wins). Returns one customer. Name/phone-email search
     * (multi-result) is intentionally not part of this endpoint. A key whose lookup is not yet
     * implemented returns a clear not-yet-available error.
     */
    @GetMapping
    @Operation(
            summary = "Get single customer by lookup key",
            description = """
                    Single customer lookup (PHN-2202). Provide **one** lookup key; priority when multiple are sent:
                    `uic` > `customerNumber` > `contractNumber` > `invoiceNumber` > name-based search.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Name search:** requires `name` plus at least one of `phone` or `email`.
                    Optional: `surname`, `birthDate` (ISO date).

                    **Optional query:** `language` — default `BULGARIAN`.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when no active customer matches. Unsupported lookup keys return **400**.
                    Multi-result name search is not supported on this endpoint.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Single customer resolved.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalCustomerResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<SalesPortalCustomerResponse> getCustomer(
            @Parameter(description = "Customer identifier — EGN (private) or UIC (legal entity).") @RequestParam(value = "uic", required = false) String uic,
            @Parameter(description = "Public 10-digit customer number.") @RequestParam(value = "customerNumber", required = false) String customerNumber,
            @Parameter(description = "Active product contract number.") @RequestParam(value = "contractNumber", required = false) String contractNumber,
            @Parameter(description = "Invoice number linked to the customer.") @RequestParam(value = "invoiceNumber", required = false) String invoiceNumber,
            @Parameter(description = "Given name (name-based search).") @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "Family name (name-based search).") @RequestParam(value = "surname", required = false) String surname,
            @Parameter(description = "Contact phone number.") @RequestParam(value = "phone", required = false) String phone,
            @Parameter(description = "Contact e-mail address.") @RequestParam(value = "email", required = false) String email,
            @Parameter(description = "Date of birth, ISO-8601 (private customers).") @RequestParam(value = "birthDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
            @Parameter(description = "Response language for resolved display values.") @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language
    ) {
        return ResponseEntity.ok(salesPortalCustomerLookupService.getSingleCustomer(
                uic, customerNumber, contractNumber, invoiceNumber, name, surname, phone, email, birthDate, language));
    }

    /**
     * Returns a single customer resolved by EGN/UIC (PHN-2203), selecting the customer version by
     * contract-status priority (LC-* rules) and resolving display values for the requested
     * {@code language}. Responds 400 with {@code DOMAIN_ENTITY_NOT_FOUND} when no active customer
     * matches the UIC.
     */
    @GetMapping("/{uic}")
    @Operation(
            summary = "Get customer by UIC (path)",
            description = """
                    Returns a single customer by EGN/UIC path parameter (PHN-2203).
                    Customer version is selected by contract-status priority (LC-* rules).
                    Display values are resolved for the requested `language` (default `BULGARIAN`).

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    Returns **400** with `errorCode: DOMAIN_ENTITY_NOT_FOUND` when no active customer matches the UIC.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer found.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalCustomerResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<SalesPortalCustomerResponse> getCustomerByUic(
            @Parameter(description = "Customer EGN/UIC to look up.") @PathVariable("uic") String uic,
            @Parameter(description = "Response language for resolved display values.") @RequestParam(value = "language", required = false, defaultValue = "BULGARIAN") Language language
    ) {
        return ResponseEntity.ok(salesPortalCustomerLookupService.getSingleCustomer(
                LookupMethod.UIC, new CustomerLookupCriteria(uic,null), language));
    }

    /**
     * Updates customer data for an existing customer version (PHN-2208).
     * <p>
     * Generic customer-data update covering both individual and legal-entity customers.
     * Behavior differs from the existing {@code PUT /customer/{id}} flow in four ways:
     * <ul>
     *   <li>Updates apply in place to the version indicated by {@code customerVersionId} —
     *       no new customer version is created. Mutations propagate to any contract
     *       referencing this customer-details row.</li>
     *   <li>Manager and communication-data phones/emails use APPEND with duplicates allowed.</li>
     *   <li>Cross-type fields (individual fields sent for a legal entity, or vice versa) are
     *       silently ignored — only an INFO log records them.</li>
     *   <li>No optimistic-lock guard and no GDPR-sentinel handling — Sales Portal is an
     *       M2M client with full visibility.</li>
     * </ul>
     */
    @PutMapping
    @Operation(
            summary = "Update customer version in place",
            description = """
                    Updates an existing customer version.

                    **Auth:** OAuth2 client-credentials Bearer JWT (`bearer-token`).

                    **Customer type:** Resolved from the database. Only fields for that type are applied;
                    cross-type fields are silently ignored (INFO log).

                    **Always required:** `customerId`, `customerVersionId`, `customerPnOrUic`
                    (`customerPnOrUic` must equal `customer.identifier`).

                    **Individual (additional):** `name`, `surname` mandatory; `middleName` optional (null/omit clears);
                    `registered` + `address` for main address; `kycPassed` / `kycExpirationDate` (expiration required when KYC true, today or future).

                    **Legal entity (additional):** at least one of each `communicationData`, `managers`, `owners`
                    (entries update existing rows by id — append phones/emails, do not replace contact lists);
                    `directDebit` null/absent = leave unchanged; when `true`, `bankId` / `bic` / `iban` required.

                    **Optional-field clear rule:** omitted/null optional fields clear to null, except `directDebit`.

                    **HTTP status note:** business "not found" is thrown as `ClientException` and currently returns
                    **400** (not 404) with body `ErrorResponse` where `errorCode` is typically `APPLICATION_ERROR`
                    and `exceptionId` is `ClientException`. Bean-validation failures return **400** with
                    `errorCode` `ILLEGAL_ARGUMENTS_PROVIDED` and a UUID `exceptionId`.

                    **Message format:** `field-Message;` (semicolon-separated when multiple).""",
            security = @SecurityRequirement(name = "bearer-token"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Customer update payload. Required identity keys plus type-specific fields.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalCustomerUpdateRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "IndividualCustomer",
                                            summary = "Update individual customer (registered/nomenclature address)",
                                            value = """
                                                    {
                                                      "customerId": 12345,
                                                      "customerVersionId": 47393,
                                                      "customerPnOrUic": "8889101010",
                                                      "name": "ИВАН",
                                                      "middleName": "ПЕТРОВ",
                                                      "surname": "ГЕОРГИЕВ",
                                                      "registered": true,
                                                      "address": {
                                                        "countryId": 1,
                                                        "populatedPlaceId": 101,
                                                        "zipCodeId": 110,
                                                        "streetId": 200,
                                                        "streetNumber": "12",
                                                        "block": "А",
                                                        "entrance": "1",
                                                        "floor": "3",
                                                        "apartment": "8"
                                                      },
                                                      "kycPassed": true,
                                                      "kycExpirationDate": "2027-12-31"
                                                    }"""),
                                    @ExampleObject(
                                            name = "LegalEntityCustomer",
                                            summary = "Update legal-entity customer (append contacts, set direct debit)",
                                            value = """
                                                    {
                                                      "customerId": 12345,
                                                      "customerVersionId": 47393,
                                                      "customerPnOrUic": "175174082",
                                                      "additionalComment": "Hold company note",
                                                      "economicBranchCiId": 50,
                                                      "mainSubjectOfActivity": "Energy distribution and supply",
                                                      "communicationData": [
                                                        {
                                                          "communicationDataId": 555,
                                                          "registered": true,
                                                          "address": {
                                                            "countryId": 1,
                                                            "populatedPlaceId": 101,
                                                            "zipCodeId": 110,
                                                            "streetNumber": "1"
                                                          },
                                                          "phones": ["+359888111111"],
                                                          "emails": ["office@example.com"]
                                                        }
                                                      ],
                                                      "managers": [
                                                        {
                                                          "managerId": 77,
                                                          "name": "ГЕОРГИ",
                                                          "surname": "ИВАНОВ",
                                                          "jobPosition": "Управител",
                                                          "representationMethodId": 11,
                                                          "titleId": 22,
                                                          "mobileNumbers": ["+359888222222"],
                                                          "emails": ["manager@example.com"],
                                                          "kycPassed": true,
                                                          "kycExpirationDate": "2027-06-30"
                                                        }
                                                      ],
                                                      "owners": [
                                                        {
                                                          "ownerId": 33,
                                                          "ownerIdentifier": "987654321",
                                                          "ownerName": "ХОЛДИНГ АД",
                                                          "belongingCapitalOwnerId": 5
                                                        }
                                                      ],
                                                      "directDebit": true,
                                                      "bankId": 7,
                                                      "bic": "UBBSBGSF",
                                                      "iban": "BG80BNBG96611020345678"
                                                    }""")
                            })))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer version updated successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalCustomerUpdateResponse.class),
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = "{\"status\": \"success\"}"))),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Invalid request (bad data), validation failure, business-rule violation, or not-found.
                            Not-found cases (unknown customer/version) currently return **400**, not 404.
                            Bean validation → `errorCode` usually `ILLEGAL_ARGUMENTS_PROVIDED`, UUID `exceptionId`.
                            `ClientException` → `errorCode` usually `APPLICATION_ERROR`, `exceptionId` = `ClientException`.""",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RestError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "MandatoryFieldMissing",
                                            summary = "Individual name missing",
                                            value = """
                                                    {
                                                      "errorCode": "APPLICATION_ERROR",
                                                      "exceptionId": "ClientException",
                                                      "message": "name-Name is mandatory;"
                                                    }"""),
                                    @ExampleObject(
                                            name = "IdentifierMismatch",
                                            summary = "customerPnOrUic does not match customer.identifier",
                                            value = """
                                                    {
                                                      "errorCode": "APPLICATION_ERROR",
                                                      "exceptionId": "ClientException",
                                                      "message": "customerPnOrUic-Customer identifier does not match customerId;"
                                                    }"""),
                                    @ExampleObject(
                                            name = "CustomerNotFound",
                                            summary = "Unknown customerId (HTTP 400, not 404)",
                                            value = """
                                                    {
                                                      "errorCode": "APPLICATION_ERROR",
                                                      "exceptionId": "ClientException",
                                                      "message": "customerId-Customer not found, ID: 12345;"
                                                    }"""),
                                    @ExampleObject(
                                            name = "BeanValidation",
                                            summary = "Jakarta @Valid failure",
                                            value = """
                                                    {
                                                      "errorCode": "ILLEGAL_ARGUMENTS_PROVIDED",
                                                      "exceptionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                      "message": "customerId-Customer ID is mandatory;"
                                                    }""")
                            })),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access (missing, invalid, or expired Bearer token).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalUnauthorizedResponse.class),
                            examples = @ExampleObject(
                                    name = "InvalidToken",
                                    value = """
                                            {
                                              "error": "Unauthorized",
                                              "message": "Invalid or expired token"
                                            }"""))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated but not allowed (ACCESS_DENIED).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RestError.class),
                            examples = @ExampleObject(
                                    name = "AccessDenied",
                                    value = """
                                            {
                                              "errorCode": "APPLICATION_ERROR",
                                              "exceptionId": "ClientException",
                                              "message": "Access denied"
                                            }"""))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RestError.class),
                            examples = @ExampleObject(
                                    name = "InternalError",
                                    value = """
                                            {
                                              "errorCode": "APPLICATION_ERROR",
                                              "exceptionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                              "message": "Internal server error"
                                            }"""))),
            @ApiResponse(
                    responseCode = "default",
                    description = "General error (any non-success status not listed above).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RestError.class)))
    })
    public ResponseEntity<SalesPortalCustomerUpdateResponse> updateCustomer(
            @Valid @RequestBody SalesPortalCustomerUpdateRequest request) {
        return ResponseEntity.ok(salesPortalCustomerUpdateService.updateCustomer(request));
    }

    @PostMapping("/search-by-coordinates")
    @Operation(
            summary = "Search customers by geographic coordinates",
            description = """
                    Returns a paginated list of customers whose communication addresses fall within
                    `radiusMeters` of the given latitude/longitude.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Required body:** `latitude`, `longitude`, `radiusMeters`, `page`, `size`.
                    **Optional:** `language` — `BG` (default) or `EN`.""",
            security = @SecurityRequirement(name = "bearer-token"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomerListByCoordinatesRequest.class),
                            examples = @ExampleObject(
                                    name = "SearchNearSofia",
                                    value = """
                                            {
                                              "latitude": 42.6977,
                                              "longitude": 23.3219,
                                              "radiusMeters": 500.0,
                                              "page": 0,
                                              "size": 20,
                                              "language": "BG"
                                            }"""))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated customer list (Spring Page JSON).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomerListByCoordinatesProjection.class)))
    })
    @SalesPortalStandardApiResponses
    public ResponseEntity<Page<CustomerListByCoordinatesProjection>> searchCustomersByCoordinates(
            @RequestBody @Valid CustomerListByCoordinatesRequest request) {
        return ResponseEntity.ok(salesPortalService.getCustomerListByCoordinates(request));
    }
}
