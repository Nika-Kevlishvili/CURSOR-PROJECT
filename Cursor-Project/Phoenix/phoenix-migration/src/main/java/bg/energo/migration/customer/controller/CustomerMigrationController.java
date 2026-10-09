package bg.energo.migration.customer.controller;

import bg.energo.migration.customer.service.ProcessCustomerDataService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/migrate-customers")
@RequiredArgsConstructor
public class CustomerMigrationController {
    private final ProcessCustomerDataService processCustomerDataService;

    @Operation
    @GetMapping
    public void migrateCustomers() {
        processCustomerDataService.processCustomers();
    }

    @Operation
    @GetMapping("/v2")
    public void migrateCustomers2() {
        processCustomerDataService.processCustomersV2();
    }
}
