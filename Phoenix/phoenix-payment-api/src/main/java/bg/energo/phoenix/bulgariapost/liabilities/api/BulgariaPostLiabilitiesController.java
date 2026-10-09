package bg.energo.phoenix.bulgariapost.liabilities.api;

import bg.energo.phoenix.bulgariapost.liabilities.service.BulgariaPostLiabilitiesService;
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
public class BulgariaPostLiabilitiesController {

    private final BulgariaPostLiabilitiesService liabilitiesService;

    @GetMapping(path = "/obligations/{customerNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BulgariaPostLiabilitiesResponse> obligations(@PathVariable("customerNumber") String customerNumber) {
        return ResponseEntity.ok(liabilitiesService.obligations(customerNumber));
    }
}

