package bg.energo.phoenix.systech.pay.api;

import bg.energo.phoenix.systech.pay.service.SystechPayService;
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
public class SystechPayController {

    private final SystechPayService payService;

    @GetMapping(path = "/pay/{TID}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "systech-bearer-token"))
    public ResponseEntity<SystechPayResponse> pay(@PathVariable("TID") String tid) {
        return ResponseEntity.ok(payService.pay(tid));
    }
}
