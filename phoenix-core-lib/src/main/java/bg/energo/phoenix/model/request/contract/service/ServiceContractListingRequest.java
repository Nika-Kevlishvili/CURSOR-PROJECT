package bg.energo.phoenix.model.request.contract.service;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.contract.service.*;
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
public class ServiceContractListingRequest {
    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private List<ServiceContractContractType> types;

    private List<ServiceContractDetailStatus> contractDetailsStatuses;

    private List<ServiceContractDetailsSubStatus> contractDetailsSubStatuses;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfEntryIntoPerpetuityFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfEntryIntoPerpetuityTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfTerminationFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfTerminationTo;

    /*@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate contractTermEndDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate contractTermEndDateTo;*/

    @DuplicatedValuesValidator(fieldPath = "serviceIds")
    private List<Long> serviceIds;

    @DuplicatedValuesValidator(fieldPath = "accountManagerIds")
    private List<Long> accountManagerIds;

    private ServiceContractListingSearchFields searchBy;

    private ServiceContractListingSortFields sortBy;

    private Sort.Direction direction;
    private boolean excludeOldVersions;
    private boolean excludeFutureVersions;
}
