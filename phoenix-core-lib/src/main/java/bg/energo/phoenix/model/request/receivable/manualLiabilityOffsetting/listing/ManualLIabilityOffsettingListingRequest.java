package bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting.listing;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.enums.receivable.manualLiabilityOffsetting.Reversed;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class ManualLIabilityOffsettingListingRequest {

    @Size(min = 1, message = "prompt-Prompt length must be 1 or more")
    private String prompt;

    private ManualLiabilityOffsettingListColumns sortBy;

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private ManualLiabilityOffsettingSearchByEnums searchBy;

    @DateRangeValidator(fieldPath = "fromDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateRangeValidator(fieldPath = "toDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private List<Reversed> reversed;

    private Sort.Direction direction;
}
