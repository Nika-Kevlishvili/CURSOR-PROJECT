package bg.energo.phoenix.model.request.contract.product;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.contract.products.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@PromptSymbolReplacer
public class ProductContractListingRequest {
    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private List<ContractDetailType> types;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate activationFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate activationTo;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfEntryIntoPerpetuityFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfEntryIntoPerpetuityTo;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfTerminationFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfTerminationTo;

    private List<ContractDetailsStatus> contractDetailsStatuses;
    private List<ContractDetailsSubStatus> contractDetailsSubStatuses;

    @DuplicatedValuesValidator(fieldPath = "productIds")
    private List<Long> productIds;

    @DuplicatedValuesValidator(fieldPath = "accountManagerIds")
    private List<Long> accountManagerIds;

    private ProductContractListingSearchFields searchBy;

    private ProductContractListingSortFields sortBy;

    private Sort.Direction direction;
    private boolean excludeOldVersions;
    private boolean excludeFutureVersions;
}
