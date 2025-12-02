package bg.energo.phoenix.model.request.customer.list;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.customer.list.ValidCustomerRelatedContractListRequest;
import bg.energo.phoenix.model.enums.customer.list.CustomerRelatedContractsSearchField;
import bg.energo.phoenix.model.enums.customer.list.CustomerRelatedContractsTableColumn;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@ValidCustomerRelatedContractListRequest
public class CustomerRelatedContractListRequest {
    private int page;
    private int size;
    private String prompt;
    private CustomerRelatedContractsTableColumn sortBy;
    private CustomerRelatedContractsSearchField searchBy;
    private Sort.Direction sortDirection;

    private List<String> contractTypes;

    @DuplicatedValuesValidator(fieldPath = "contractStatuses")
    private List<String> contractStatuses;

    @DuplicatedValuesValidator(fieldPath = "contractSubStatuses")
    private List<String> contractSubStatuses;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate signingDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate signingDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate activationDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate activationDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate contractTermEndDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate contractTermEndDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate entryIntoForceDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate entryIntoForceDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate creationDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate creationDateTo;
}
