package bg.energo.phoenix.systech.search.api;

import bg.energo.phoenix.systech.search.service.SystechSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("systech")
@ConditionalOnProperty(prefix = "systech.auth", name = "enabled", havingValue = "true")
public class SystechSearchController {

    private final SystechSearchService searchService;

    @PostMapping(path = "/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "systech-bearer-token"))
    public ResponseEntity<SystechSearchResponse> search(@RequestBody SystechSearchRequest request) {
        return ResponseEntity.ok(searchService.search(request));
    }
}
