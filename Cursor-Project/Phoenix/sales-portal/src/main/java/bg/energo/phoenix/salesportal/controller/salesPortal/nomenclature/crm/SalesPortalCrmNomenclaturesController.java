package bg.energo.phoenix.salesportal.controller.salesPortal.nomenclature.crm;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import bg.energo.phoenix.salesportal.openapi.SalesPortalStandardApiResponses;
import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.model.response.nomenclature.crm.EmailMailboxesResponse;
import bg.energo.phoenix.model.response.nomenclature.crm.SmsSendingNumberResponse;
import bg.energo.phoenix.model.response.nomenclature.crm.TopicOfCommunicationResponse;
import bg.energo.phoenix.service.salesportal.nomenclature.crm.SalesPortalCrmNomenclaturesService;
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
@RequestMapping("/nomenclature/crm")
@Tag(name = "Nomenclatures - CRM", description = "CRM nomenclatures. (OAuth2 Client Credentials).")
public class SalesPortalCrmNomenclaturesController {

    private final SalesPortalCrmNomenclaturesService salesPortalCrmNomenclaturesService;

    @GetMapping("/email-mailboxes/{id}")
    @Operation(
            summary = "Get crm email mailboxes by id",
            description = """
                    Returns a single **crm email mailboxes** nomenclature record by id.

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
                            schema = @Schema(implementation = EmailMailboxesResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<EmailMailboxesResponse> viewEmailMailboxes(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalCrmNomenclaturesService.viewEmailMailboxes(id), HttpStatus.OK);
    }

    @GetMapping("/sms-sending-number/{id}")
    @Operation(
            summary = "Get crm sms sending number by id",
            description = """
                    Returns a single **crm sms sending number** nomenclature record by id.

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
                            schema = @Schema(implementation = SmsSendingNumberResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<SmsSendingNumberResponse> viewSmsSendingNumber(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalCrmNomenclaturesService.viewSmsSendingNumber(id), HttpStatus.OK);
    }

    @GetMapping("/topic-of-communication/{id}")
    @Operation(
            summary = "Get crm topic of communication by id",
            description = """
                    Returns a single **crm topic of communication** nomenclature record by id.

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
                            schema = @Schema(implementation = TopicOfCommunicationResponse.class)))})
    @SalesPortalStandardApiResponses
    public ResponseEntity<TopicOfCommunicationResponse> viewTopicOfCommunication(@PathVariable("id") Long id) {
        return new ResponseEntity<>(salesPortalCrmNomenclaturesService.viewTopicOfCommunication(id), HttpStatus.OK);
    }
}
