package bg.energo.phoenix.model.request.receivable.rescheduling;

import bg.energo.phoenix.model.customAnotations.receivable.LocalDateDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReschedulingLiabilityRequest {

    @NotNull(message = "customerLiabilityId is mandatory")
    private Long customerLiabilityId;

    @NotNull(message = "initialAmount is mandatory")
    private BigDecimal initialAmount;

    @NotNull(message = "currentAmount is mandatory")
    private BigDecimal currentAmount;

    @NotNull(message = "dueDate is mandatory")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate dueDate;

    @NotNull(message = "interestsFromDate is mandatory")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate interestsFromDate;
}
