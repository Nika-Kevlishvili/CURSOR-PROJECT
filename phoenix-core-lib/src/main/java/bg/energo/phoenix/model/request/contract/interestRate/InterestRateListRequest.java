package bg.energo.phoenix.model.request.contract.interestRate;

import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateCharging;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterestRateListRequest {
    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private List<InterestRateType> type;

    private List<InterestRateCharging> charging;

    private InterestRateGrouping grouping;

    private InterestRateListColumns sortBy;

    private InterestRateSearchFields searchBy;

    private Sort.Direction direction;
}
