package bg.energo.phoenix.service.contract.product.termination;

import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationByTermsResponse;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationGenericModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ttest")
public class TestProductContractTerminationController {

    private final ProductContractTerminationWithPodsService productContractTerminationWithPodsService;
    private final ProductContractTerminationWithActionService productContractTerminationWithActionService;
    private final ProductContractTerminationByTermService productContractTerminationByTermService;
    private final ProductContractTermTerminationService productContractTermTerminationService;
    private final List<ProductContractTerminator> productContractTerminators;
    private final ProductContractTerminationManager terminatorManager;

    @GetMapping
    @Operation(
            security = @SecurityRequirement(name = "bearer-token")
    )
    public void start() throws InterruptedException {
        for (ProductContractTerminator productContractTerminator : productContractTerminators) {
            terminatorManager.processContractsForTermination(productContractTerminator);
        }
    }

    @GetMapping("/contracts-to-terminate-with-pods")
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public ResponseEntity<List<ProductContractTerminationGenericModel>> getProductContractsToTerminationWithPodsDeactivation(@RequestParam(name = "size") int size) {
        return ResponseEntity.ok().body(productContractTerminationWithPodsService.getContractData(size, 0));
    }

    @GetMapping("/contracts-to-terminate-with-action")
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public ResponseEntity<List<ProductContractTerminationGenericModel>> getEligibleProductContractsForTerminationWithAction(@RequestParam(name = "size") int size) {
        return ResponseEntity.ok().body(productContractTerminationWithActionService.getContractData(size, 0));
    }

    @GetMapping("/contract-to-terminate-by-terms")
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public ResponseEntity<List<ProductContractTerminationGenericModel>> getEligibleProductContractsForTerminationByTerms(@RequestParam(name = "size") int size) {
        return ResponseEntity.ok().body(productContractTerminationByTermService.getContractData(size, 0));
    }

    @PostMapping("/{contractId}/terminate-with-action")
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public ResponseEntity<String> terminateWithAction(@PathVariable Long contractId,
                                                      @RequestParam("persist") boolean persist,
                                                      @RequestParam("sizeToFilter") int sizeToFilter) {
        return ResponseEntity.ok().body(productContractTerminationWithActionService.terminateForTesting(contractId, sizeToFilter, persist));
    }

    @GetMapping("/term-termination")
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public void terminateByTerm(@RequestParam(name = "contractId") Long contractId) {
        Optional<ProductContractTerminationGenericModel> first = productContractTermTerminationService.getContractData(1000, 0).stream().filter(x -> x.getTermsResponses().getContractId().equals(contractId)).findFirst();
        first.ifPresent(productContractTermTerminationService::terminate);
    }

    @GetMapping("/pod-termination")
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public void terminateByPODS(@RequestParam(name = "contractId") Long contractId) {
        Optional<ProductContractTerminationGenericModel> first = productContractTerminationWithPodsService.getContractData(100, 0).stream().filter(x -> x.getPodsResponse().getId().equals(contractId)).findFirst();
        first.ifPresent(productContractTerminationWithPodsService::terminate);
    }

    @GetMapping("/term-termination/list")
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public List<ProductContractTerminationGenericModel> terminateByTermData() {
        return productContractTermTerminationService.getContractData(100, 0);
    }

    @PatchMapping("/execute-termination-by-terms")
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public void executeTerminationByTerms(@RequestParam(name = "contractId") Long contractId) {
        productContractTerminationByTermService
                .terminate(
                        new ProductContractTerminationGenericModel(
                                new ProductContractTerminationByTermsResponse(contractId)
                        )
                );
    }
}
