package bg.energo.phoenix.bulgariapost.auth.api;

import java.util.Objects;

import bg.energo.phoenix.bulgariapost.auth.service.BulgariaPostAuthService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("bulgarian-post")
@ConditionalOnProperty(prefix = "bulgariapost.auth", name = "enabled", havingValue = "true")
public class BulgariaPostAuthController {

    private final BulgariaPostAuthService authService;

    public BulgariaPostAuthController(BulgariaPostAuthService authService) {
        this.authService = Objects.requireNonNull(authService, "authService");
    }

    @PostMapping(path = "/auth", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BulgariaPostAuthResponse> authenticate(@RequestBody BulgariaPostAuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }
}

