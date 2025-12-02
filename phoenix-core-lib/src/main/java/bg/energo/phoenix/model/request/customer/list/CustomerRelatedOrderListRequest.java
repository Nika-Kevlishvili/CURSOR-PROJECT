package bg.energo.phoenix.model.request.customer.list;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.customer.list.ValidCustomerRelatedOrderListRequest;
import bg.energo.phoenix.model.enums.customer.list.CustomerRelatedOrdersSearchField;
import bg.energo.phoenix.model.enums.customer.list.CustomerRelatedOrdersTableColumn;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@Builder
@ValidCustomerRelatedOrderListRequest
public class CustomerRelatedOrderListRequest {
    private int page;
    private int size;
    private String prompt;
    private CustomerRelatedOrdersTableColumn sortBy;
    private CustomerRelatedOrdersSearchField searchBy;
    private Sort.Direction sortDirection;

    private List<String> orderTypes;

    @DuplicatedValuesValidator(fieldPath = "orderStatuses")
    private List<String> orderStatuses;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceMaturityDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceMaturityDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate creationDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate creationDateTo;

    private Set<Boolean> invoicePaid;
}
