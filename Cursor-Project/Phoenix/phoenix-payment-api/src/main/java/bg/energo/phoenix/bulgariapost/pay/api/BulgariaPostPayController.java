package bg.energo.phoenix.bulgariapost.pay.api;

import bg.energo.phoenix.bulgariapost.pay.service.BulgariaPostPayService;
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
@RequestMapping("bulgarian-post")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bulgariapost.auth", name = "enabled", havingValue = "true")
public class BulgariaPostPayController {

    private final BulgariaPostPayService payService;

    @GetMapping(path = "/pay/{TID}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BulgariaPostPayResponse> pay(@PathVariable("TID") String tid) {
        return ResponseEntity.ok(payService.pay(tid));
    }
}
