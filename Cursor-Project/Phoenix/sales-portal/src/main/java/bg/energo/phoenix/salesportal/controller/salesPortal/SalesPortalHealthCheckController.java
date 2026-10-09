package bg.energo.phoenix.salesportal.controller.salesPortal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "API", description = "Health and status")
public class SalesPortalHealthCheckController

{
    @GetMapping("/health")
    @Operation(
            summary = "Health check for Sales Portal integration",
            security = @SecurityRequirement(name = "bearer-token")
    )
    public ResponseEntity<Map<String, String>> healthCheck() {
        log.debug("Sales Portal health check called");
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

}
