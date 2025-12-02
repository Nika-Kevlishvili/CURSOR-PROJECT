package bg.energo.phoenix.model.request.receivable.rescheduling;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.enums.receivable.rescheduling.ReschedulingInterestType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ReschedulingCalculateRequest {

    @NotNull(message = "reschedulingInterestType-reschedulingInterestType is mandatory;")
    private ReschedulingInterestType reschedulingInterestType;

    @DuplicatedValuesValidator(fieldPath = "files")
    @NotNull(message = "liabilityIds-liabilityIds is mandatory;")
    private List<Long> liabilityIds;

    private BigDecimal installmentAmount;

    @Min(value = 2, message = "installmentCount-installmentCount minimum value is 2;")
    private Integer installmentCount;

    @NotNull(message = "installmentCurrencyId-installmentCurrencyId is mandatory;")
    private Integer installmentCurrencyId;

    @DateRangeValidator(fieldPath = "installmentDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "installmentDate-installmentDate is mandatory;")
    private LocalDate installmentDate;

    private Long replaceInstallmentRateId;

}
