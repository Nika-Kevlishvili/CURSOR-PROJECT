package bg.energo.phoenix.bulgariapost.receipt.api;

import bg.energo.phoenix.bulgariapost.receipt.service.BulgariaPostReceiptService;
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
@RequestMapping("bulgarian-post")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bulgariapost.auth", name = "enabled", havingValue = "true")
public class BulgariaPostReceiptController {

    private final BulgariaPostReceiptService receiptService;

    @GetMapping(path = "/receipt", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BulgariaPostReceiptResponse> receipt(@ModelAttribute BulgariaPostReceiptRequest request) {
        return ResponseEntity.ok(receiptService.receipt(request));
    }
}

