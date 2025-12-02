package bg.energo.phoenix.service.riskList;

import bg.energo.phoenix.service.riskList.model.RiskListBasicInfoResponse;
import bg.energo.phoenix.service.riskList.model.RiskListFullInfoResponse;
import bg.energo.phoenix.service.riskList.model.RiskListRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/risk-list")
@Validated
public class RiskListController {

    private final RiskListService riskListService;


    @GetMapping("/evaluate")
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public ResponseEntity<RiskListBasicInfoResponse> evaluateBasicCustomerRisk(RiskListRequest riskListRequest) {
        return new ResponseEntity<>(riskListService.evaluateBasicCustomerRisk(riskListRequest), HttpStatus.OK);
    }


    @GetMapping("/evaluate/{identifier}")
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public ResponseEntity<RiskListFullInfoResponse> evaluateFullCustomerRisk(@PathVariable String identifier) {
        return new ResponseEntity<>(riskListService.evaluateFullCustomerRisk(identifier), HttpStatus.OK);
    }

}
