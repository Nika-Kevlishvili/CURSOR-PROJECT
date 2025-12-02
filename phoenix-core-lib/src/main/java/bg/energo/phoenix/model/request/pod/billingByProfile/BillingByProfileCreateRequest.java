package bg.energo.phoenix.model.request.pod.billingByProfile;

import bg.energo.phoenix.model.customAnotations.pod.billingByProfile.ValidBillingByProfileDatesRange;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.request.pod.billingByProfile.data.BillingByProfileDataCreateRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidBillingByProfileDatesRange
public class BillingByProfileCreateRequest {

    @NotBlank(message = "identifier-Identifier must not blank;")
    @Size(max = 33, message = "identifier-Max length for identifier is {max};")
    @Pattern(regexp = "^[0-9A-Za-z]+$", message = "number-Allowed symbols in number are: A-Z a-z 0-9;")
    private String identifier;

    @NotNull(message = "periodType-Period type must not be null;")
    private PeriodType periodType;

    @NotNull(message = "profileId-Profile id must not be null;")
    private Long profileId;

    @NotNull(message = "periodFrom-Period from must not be null;")
    private LocalDateTime periodFrom;

    @NotNull(message = "periodTo-Period to must not be null;")
    private LocalDateTime periodTo;

    private List<@Valid BillingByProfileDataCreateRequest> entries;

    private boolean warningAcceptedByUser;

    @JsonIgnore
    @AssertTrue(message = "periodFrom-Period from must be first day of the month")
    public boolean isPeriodFromValid() {
        if (Objects.nonNull(periodFrom)) {
            return periodFrom.getDayOfMonth() == 1;
        }

        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "periodTo-Period to must be last day of the month")
    public boolean isPeriodToValid() {
        if (Objects.nonNull(periodTo)) {
            int dayOfMonth = periodTo.getDayOfMonth();

            LocalDateTime lastDayOfMonth = periodTo.with(TemporalAdjusters.lastDayOfMonth());

            return dayOfMonth == lastDayOfMonth.getDayOfMonth();
        }

        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "periodFrom-Period and period to must be in same year and same month")
    public boolean isPeriodFromAndToValid() {
        if (Objects.nonNull(periodFrom) && Objects.nonNull(periodTo)) {
            return periodFrom.getYear() == periodTo.getYear() && periodFrom.getMonth() == periodTo.getMonth();
        }

        return true;
    }
}
