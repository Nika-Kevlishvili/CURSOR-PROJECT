package bg.energo.phoenix.service.contract.product.termination.serviceContract;

import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationGenericModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
//@Profile({"dev","test"})
@RequiredArgsConstructor
@RequestMapping("/test-service-contract-termination")
//@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class TestServiceContractTerminationController {

//    private final ServiceContractTerminationScheduler serviceContractTerminationScheduler;
    private final ServiceContractTerminationWithActionService serviceContractTerminationWithActionService;
    private final ServiceContractTermTerminationService serviceContractTermTerminationService;
    private final ServiceContractTerminationByTermService serviceContractTerminationByTermService;

//    @PostMapping
//    @Operation(
//            security = @SecurityRequirement(name = "bearer-token")
//    )
//    public void start() throws InterruptedException {
//        serviceContractTerminationScheduler.start();
//    }


    @GetMapping("/contracts-to-terminate-with-action")
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public ResponseEntity<List<ServiceContractTerminationGenericModel>> getEligibleServiceContractsForTerminationWithAction(@RequestParam(name = "size") int size) {
        return ResponseEntity.ok().body(serviceContractTerminationWithActionService.getContractData(size, new ArrayList<>()));
    }


    @PostMapping("/{contractId}/terminate-with-action")
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public ResponseEntity<String> terminateWithAction(@PathVariable Long contractId,
                                                      @RequestParam("persist") boolean persist,
                                                      @RequestParam("sizeToFilter") int sizeToFilter) {
        return ResponseEntity.ok().body(serviceContractTerminationWithActionService.terminateForTesting(contractId, sizeToFilter, persist));
    }


    @GetMapping("/terms-termination")
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public ResponseEntity<List<ServiceContractTerminationGenericModel>> getEligibleServiceContractsTermsForTermination(@RequestParam(name = "size") int size) {
        return ResponseEntity.ok().body(serviceContractTermTerminationService.getContractData(size, new ArrayList<>()));
    }


    @GetMapping("/contract-to-terminate-by-terms")
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public ResponseEntity<List<ServiceContractTerminationGenericModel>> getEligibleServiceContractsForTerminationByTerms(@RequestParam(name = "size") int size) {
        return ResponseEntity.ok().body(serviceContractTerminationByTermService.getContractData(size, new ArrayList<>()));
    }

}
