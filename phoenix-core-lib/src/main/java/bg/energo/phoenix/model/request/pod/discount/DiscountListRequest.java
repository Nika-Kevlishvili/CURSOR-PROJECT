package bg.energo.phoenix.model.request.pod.discount;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.pod.discount.DiscountParameterColumnName;
import bg.energo.phoenix.model.enums.pod.discount.DiscountParameterFilterField;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Set;

@Data
@PromptSymbolReplacer
public class DiscountListRequest {
    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFromBegin;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFromEnd;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateToBegin;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateToEnd;

    private DiscountParameterFilterField searchBy;

    private DiscountParameterColumnName sortBy;

    private Sort.Direction sortDirection;

    private Set<Boolean> invoiced;
}
