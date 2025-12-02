package bg.energo.phoenix.model.request.contract.interestRate;

import bg.energo.phoenix.model.customAnotations.contract.interestRate.ValidInterestRatePaymentTermValues;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRatePaymentTermsCalendarType;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateTermsDueDateChange;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateTermsExclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidInterestRatePaymentTermValues
public class InterestRatePaymentTermBaseRequest {
    @NotNull(message = "paymentTerm.type-[Type] type must not be null;")
    private InterestRatePaymentTermsCalendarType type;

    @NotBlank(message = "paymentTerm.name-Name must not be null or blank;")
    private String name;

    private Integer value;

    private Integer valueFrom;

    private Integer valueTo;

    @NotNull(message = "paymentTerm.calendarId-Calendar must not be null;")
    private Long calendarId;

    private InterestRateTermsDueDateChange dueDateChange;

    private List<InterestRateTermsExclude> excludes;
}
