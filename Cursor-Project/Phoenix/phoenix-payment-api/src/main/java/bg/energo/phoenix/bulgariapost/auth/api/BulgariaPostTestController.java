package bg.energo.phoenix.bulgariapost.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Temporary test endpoint behind the Bulgaria Post token filter.
 * TODO: Remove before production deployment.
 */
@RestController
@RequestMapping("/bulgarian-post")
@ConditionalOnProperty(prefix = "bulgariapost.auth", name = "enabled", havingValue = "true")
public class BulgariaPostTestController {

    @GetMapping("/test")
    @Operation(
            security = @SecurityRequirement(name = "bearer-token"))
    public ResponseEntity<Map<String, Object>> test() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Token was valid and has been consumed",
                "timestamp", Instant.now().toString()
        ));
    }
}
