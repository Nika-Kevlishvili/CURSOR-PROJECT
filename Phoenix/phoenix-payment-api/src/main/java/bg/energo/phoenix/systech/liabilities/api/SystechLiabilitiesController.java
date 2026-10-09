package bg.energo.phoenix.systech.liabilities.api;

import bg.energo.phoenix.systech.liabilities.service.SystechLiabilitiesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("systech")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "systech.auth", name = "enabled", havingValue = "true")
public class SystechLiabilitiesController {

    private final SystechLiabilitiesService liabilitiesService;

    @GetMapping(path = "/obligations/{customerNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "systech-bearer-token"))
    public ResponseEntity<SystechLiabilitiesResponse> obligations(@PathVariable("customerNumber") String customerNumber) {
        return ResponseEntity.ok(liabilitiesService.obligations(customerNumber));
    }
}

