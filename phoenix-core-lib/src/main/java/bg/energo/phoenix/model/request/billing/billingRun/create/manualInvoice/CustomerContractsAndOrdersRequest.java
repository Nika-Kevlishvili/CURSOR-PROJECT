package bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice;

import bg.energo.phoenix.model.enums.billing.billings.CustomerContractOrderType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerContractsAndOrdersRequest {

    @NotNull(message = "customerDetailId-Customer detail id shouldn't be null;")
    private Long customerDetailId;

    @NotNull(message = "page-shouldn't be null;")
    private Integer page;

    @NotNull(message = "size-shouldn't be null;")
    private Integer size;

    private String prompt;

    @NotNull(message = "type-Type shouldn't be null;")
    private CustomerContractOrderType type;

}
