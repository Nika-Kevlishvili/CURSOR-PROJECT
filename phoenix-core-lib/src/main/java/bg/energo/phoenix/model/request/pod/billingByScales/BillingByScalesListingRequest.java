package bg.energo.phoenix.model.request.pod.billingByScales;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@PromptSymbolReplacer
public class BillingByScalesListingRequest {
    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFromTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateToFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;
    

    private BillingByScaleListSearchBy searchBy;

    private BillingByScaleListSortBy sortBy;

    private BillingByScaleListInvoiced invoiced;

    private Sort.Direction direction;

}
