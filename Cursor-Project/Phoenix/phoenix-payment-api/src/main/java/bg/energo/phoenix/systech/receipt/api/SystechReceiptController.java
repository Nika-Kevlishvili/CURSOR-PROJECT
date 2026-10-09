package bg.energo.phoenix.systech.receipt.api;

import bg.energo.phoenix.systech.receipt.service.SystechReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("systech")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "systech.auth", name = "enabled", havingValue = "true")
public class SystechReceiptController {

    private final SystechReceiptService receiptService;

    @GetMapping(path = "/receipt", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "systech-bearer-token"))
    public ResponseEntity<SystechReceiptResponse> receipt(@ModelAttribute SystechReceiptRequest request) {
        return ResponseEntity.ok(receiptService.receipt(request));
    }
}

