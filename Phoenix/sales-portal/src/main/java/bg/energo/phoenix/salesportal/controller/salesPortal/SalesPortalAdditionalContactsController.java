package bg.energo.phoenix.salesportal.controller.salesPortal;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.response.customer.communicationData.additionalContacts.AdditionalContactResponse;
import bg.energo.phoenix.service.customer.customerCommunications.CustomerCommunicationsService;
import bg.energo.phoenix.service.salesportal.SalesPortalAdditionalContactsService;
import bg.energo.phoenix.service.salesportal.model.SalesPortalAdditionalContactUpsertResponse;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalAdditionalContactUpsertRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/additional-contacts")
@Tag(name = "Customer", description = "Customer lookup, update, search and additional contacts (OAuth2 Client Credentials)")
public class SalesPortalAdditionalContactsController {
    private final CustomerCommunicationsService customerCommunicationsService;
    private final SalesPortalAdditionalContactsService salesPortalAdditionalContactsService;

    @GetMapping("/{customerIdentifier}/{versionId}")
    @Operation(
            summary = "List additional contacts for a customer version",
            description = """
                    Returns a paginated list of additional contact persons for the given customer identifier and version.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Path:** `customerIdentifier` (UIC/customer number), `versionId` (customer details version, >= 1).

                    **Query:** `page` (default 0), `size` (default 20, max 100).

                    Returns **404** with empty body when customer/version is not found.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated additional contacts (Spring Page JSON).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AdditionalContactResponse.class))),
            @ApiResponse(responseCode = "404", description = "Customer or version not found.")
    })
    @SalesPortalStandardApiResponses
    public ResponseEntity<Page<AdditionalContactResponse>> getAdditionalContacts(
            @PathVariable
            @NotBlank(message = "customerUic-Customer UIC is mandatory;")
            String customerIdentifier,
            @PathVariable
            @NotNull(message = "versionId-Version ID is mandatory;")
            @Min(value = 1, message = "versionId-Version ID must be greater than 0;")
            Long versionId,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page-Page must be greater than or equal to 0;")
            Integer page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size-Size must be at least 1;")
            @Max(value = 100, message = "size-Size must be less than or equal to 100;")
            Integer size
    ) {
        try {
            return new ResponseEntity<>(
                    customerCommunicationsService.getAdditionalContacts(customerIdentifier, versionId, page, size),
                    HttpStatus.OK
            );
        } catch (DomainEntityNotFoundException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{customerIdentifier}/{versionId}")
    @Operation(
            summary = "Create or update an additional contact",
            description = """
                    Upserts an additional contact person under a customer version.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    **Create:** omit both `communicationDataId` and `contactPersonId` in the body.
                    **Update:** provide `communicationDataId` and/or `contactPersonId` for existing rows.

                    **Required body fields:** `source` (SALES_PORTAL or SELF_SERVICE_PORTAL only),
                    `titleId`, `name`, `surname`.

                    Phones and emails **replace** the active contact list on update (not append).

                    Returns created/updated `communicationDataId` and `contactPersonId` in the response.""",
            security = @SecurityRequirement(name = "bearer-token"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalAdditionalContactUpsertRequest.class),
                            examples = @ExampleObject(
                                    name = "NewAdditionalContact",
                                    value = """
                                            {
                                              "source": "SALES_PORTAL",
                                              "titleId": 22,
                                              "name": "ИВАН",
                                              "middleName": "ПЕТРОВ",
                                              "surname": "ГЕОРГИЕВ",
                                              "relationship": "Син",
                                              "birthDate": "2010-05-15",
                                              "contactIsOver18": false,
                                              "phones": ["+359888111111"],
                                              "emails": ["contact@example.com"]
                                            }"""))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Contact created or updated.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalAdditionalContactUpsertResponse.class),
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                                            {
                                              "communicationDataId": 555,
                                              "contactPersonId": 77
                                            }""")))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<SalesPortalAdditionalContactUpsertResponse> upsertAdditionalContact(
            @PathVariable
            @NotBlank(message = "customerUic-Customer UIC is mandatory;")
            String customerIdentifier,
            @PathVariable
            @NotNull(message = "versionId-Version ID is mandatory;")
            @Min(value = 1, message = "versionId-Version ID must be greater than 0;")
            Long versionId,
            @RequestBody @Valid SalesPortalAdditionalContactUpsertRequest request
    ) {
        return ResponseEntity.ok(salesPortalAdditionalContactsService.upsertAdditionalContact(customerIdentifier, versionId, request));
    }

    @DeleteMapping("/{customerIdentifier}/{versionId}/{communicationDataId}/{contactPersonId}")
    @Operation(
            summary = "Delete an additional contact",
            description = """
                    Soft-deletes an additional contact person under a customer version.

                    **Auth:** OAuth2 client-credentials Bearer JWT.

                    When the communication row has only one active contact person, the entire communication
                    row and its children are soft-deleted. Otherwise only the specified contact person is deleted.

                    Returns the ids of the affected communication data and contact person.""",
            security = @SecurityRequirement(name = "bearer-token"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Contact deleted (soft delete).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SalesPortalAdditionalContactUpsertResponse.class),
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                                            {
                                              "communicationDataId": 555,
                                              "contactPersonId": 77
                                            }""")))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<SalesPortalAdditionalContactUpsertResponse> deleteAdditionalContact(
            @PathVariable
            @NotBlank(message = "customerUic-Customer UIC is mandatory;")
            String customerIdentifier,
            @PathVariable
            @NotNull(message = "versionId-Version ID is mandatory;")
            @Min(value = 1, message = "versionId-Version ID must be greater than 0;")
            Long versionId,
            @PathVariable
            @NotNull(message = "communicationDataId-Communication data ID is mandatory;")
            @Min(value = 1, message = "communicationDataId-Communication data ID must be greater than 0;")
            Long communicationDataId,
            @PathVariable
            @NotNull(message = "contactPersonId-Contact person ID is mandatory;")
            @Min(value = 1, message = "contactPersonId-Contact person ID must be greater than 0;")
            Long contactPersonId
    ) {
        return ResponseEntity.ok(
                salesPortalAdditionalContactsService.deleteAdditionalContact(
                        customerIdentifier,
                        versionId,
                        communicationDataId,
                        contactPersonId
                )
        );
    }
}
