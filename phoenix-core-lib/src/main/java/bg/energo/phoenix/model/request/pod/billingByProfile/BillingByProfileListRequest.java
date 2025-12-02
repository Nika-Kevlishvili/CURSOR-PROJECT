package bg.energo.phoenix.model.request.pod.billingByProfile;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.pod.billingByProfile.BillingByProfileSearchField;
import bg.energo.phoenix.model.enums.pod.billingByProfile.BillingByProfileTableColumn;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.request.pod.billingByScales.BillingByScaleListInvoiced;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@PromptSymbolReplacer
@AllArgsConstructor
@NoArgsConstructor
public class BillingByProfileListRequest {

    @NotNull(message = "page-Page shouldn't be null;")
    private Integer page;

    @NotNull(message = "size-Size shouldn't be null;")
    private Integer size;

    private String prompt;

    private BillingByProfileSearchField searchBy;

    private BillingByProfileTableColumn sortBy;

    Sort.Direction sortDirection;

    private List<Long> gridOperatorIds;

    private List<Long> profileIds;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFromStartingWith;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFromEndingWith;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateToStartingWith;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateToEndingWith;

    private PeriodType periodType;

    private BillingByScaleListInvoiced invoiced;

}
