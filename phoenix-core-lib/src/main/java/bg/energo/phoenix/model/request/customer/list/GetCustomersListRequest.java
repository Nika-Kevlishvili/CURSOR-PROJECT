package bg.energo.phoenix.model.request.customer.list;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.customer.filter.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@PromptSymbolReplacer
public class GetCustomersListRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private CustomerSearchFields searchFields;

    private List<CustomerFilterStatus> customerStatusFilter;

    private List<CustomerFilterType> customerTypeFilter;

    private List<Long> EconomicBranchCiIds;

    private List<Long> managerIds;

    private String populatedPlace;

    private UnwantedCustomerListingStatus unwantedCustomerStatus;

    private CustomerListColumns customerListColumns;

    private Sort.Direction columnDirection;

    private boolean excludePastVersion;

}
