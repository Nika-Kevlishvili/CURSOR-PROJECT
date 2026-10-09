package bg.energo.phoenix.salesportal.controller.salesPortal;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.security.jwt.JwtVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * OAuth2 Token endpoint for Sales Portal provider.
 * <p>
 * Implements the OAuth2 Client Credentials grant type. The external provider sends
 * {@code client_id}, {@code client_secret}, and {@code grant_type=client_credentials}
 * to obtain a JWT access token that can be used for subsequent API calls.
 * </p>
 *
 * <p>Example request:</p>
 * <pre>
 * POST /oauth2/token
 * Content-Type: application/x-www-form-urlencoded
 *
 * grant_type=client_credentials&amp;client_id=sp-xxx&amp;client_secret=yyy
 * </pre>
 *
 * <p>Example response:</p>
 * <pre>
 * {
 *   "access_token": "eyJhbGciOi...",
 *   "token_type": "Bearer",
 *   "expires_in": 3600
 * }
 * </pre>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth2")
@Tag(name = "Auth", description = "OAuth2 Client Credentials token endpoint")
public class SalesPortalTokenController {

    private final JwtVerifier jwtVerifier;

    @Value("${sales-portal.client.id:}")
    private String configuredClientId;

    @Value("${sales-portal.client.secret:}")
    private String configuredClientSecret;

    @PostMapping(value = "/token")
    @Operation(summary = "Exchange client credentials for an access token (OAuth2 Client Credentials grant)")
    public ResponseEntity<Map<String, Object>> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret
    ) {
        // Validate grant type
        if (!"client_credentials".equals(grantType)) {
            log.warn("Sales Portal token request with unsupported grant_type: {}", grantType);
            throw new ClientException(
                    "Unsupported grant_type. Only 'client_credentials' is supported.",
                    ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED
            );
        }

        // Validate client credentials
        if (!configuredClientId.equals(clientId) || !configuredClientSecret.equals(clientSecret)) {
            log.warn("Sales Portal token request with invalid credentials. client_id: {}", clientId);
            throw new ClientException(
                    "Invalid client_id or client_secret.",
                    ErrorCode.ACCESS_DENIED
            );
        }

        // Issue JWT token
        String accessToken = jwtVerifier.issueClientCredentialsToken(clientId);
        long expiresIn = jwtVerifier.getClientCredentialsExpirationSeconds();

        log.info("Sales Portal token issued for client: {}", clientId);

        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "token_type", "Bearer",
                "expires_in", expiresIn
        ));
    }
}
