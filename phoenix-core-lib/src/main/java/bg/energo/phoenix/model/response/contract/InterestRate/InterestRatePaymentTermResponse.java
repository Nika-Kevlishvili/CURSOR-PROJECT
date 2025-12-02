package bg.energo.phoenix.model.response.contract.InterestRate;

import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRatePaymentTermsCalendarType;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateTermsDueDateChange;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateTermsExclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRatePaymentTermResponse {

    private Long id;

    private InterestRatePaymentTermsCalendarType type;

    private String name;

    private Integer value;

    private Integer valueFrom;

    private Integer valueTo;

    private Long calendarId;

    private String calendarName;

    private InterestRateTermsDueDateChange dueDateChange;

    private List<InterestRateTermsExclude> excludes;
}
