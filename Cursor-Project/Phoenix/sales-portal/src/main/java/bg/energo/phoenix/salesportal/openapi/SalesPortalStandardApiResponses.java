package bg.energo.phoenix.salesportal.openapi;

import bg.energo.phoenix.exception.RestError;
import bg.energo.phoenix.service.salesportal.model.SalesPortalUnauthorizedResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Standard Sales Portal error responses (400, 401, 403, 500, default).
 * Compose with method-specific {@code @ApiResponses} for HTTP 200 / 201.
 * <p>Not-found is <strong>not</strong> a 404 in this API — see the 400 description below.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = """
                        Invalid request, validation failure, business-rule violation, **or not-found**.

                        This API does not return 404 for a missing record. `DomainEntityNotFoundException`
                        extends `ClientException`, and the global handler maps every `ClientException` to
                        400 — so a record that does not exist comes back here, with
                        `errorCode: DOMAIN_ENTITY_NOT_FOUND`. Branch on `errorCode`, not on the status code.

                        `errorCode` values reaching this response:
                        - `ILLEGAL_ARGUMENTS_PROVIDED` — bean validation; `exceptionId` is a UUID
                        - `DOMAIN_ENTITY_NOT_FOUND` — the referenced record does not exist
                        - `OPERATION_NOT_ALLOWED` — a business rule rejected the request
                        - `APPLICATION_ERROR` — any other `ClientException`

                        Message format: `field-Message;`.""",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = RestError.class),
                        examples = {
                                @ExampleObject(
                                        name = "ValidationOrBusinessError",
                                        value = """
                                                {
                                                  "errorCode": "APPLICATION_ERROR",
                                                  "exceptionId": "ClientException",
                                                  "message": "fieldName-Error message;"
                                                }"""),
                                @ExampleObject(
                                        name = "NotFound",
                                        summary = "A missing record — returned as 400, not 404",
                                        value = """
                                                {
                                                  "errorCode": "DOMAIN_ENTITY_NOT_FOUND",
                                                  "exceptionId": "ClientException",
                                                  "message": "id-Country not found, ID: 999999;"
                                                }""")})),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized (missing, invalid, or expired Bearer token).",
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
public @interface SalesPortalStandardApiResponses {
}
