package bg.energo.phoenix.systech.auth.api;

import java.util.Objects;

import bg.energo.phoenix.systech.auth.service.SystechAuthService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("systech")
@ConditionalOnProperty(prefix = "systech.auth", name = "enabled", havingValue = "true")
public class SystechAuthController {

    private final SystechAuthService authService;

    public SystechAuthController(SystechAuthService authService) {
        this.authService = Objects.requireNonNull(authService, "authService");
    }

    @PostMapping(path = "/auth", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SystechAuthResponse> authenticate(@RequestBody SystechAuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }
}
