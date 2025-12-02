package bg.energo.phoenix.model.response.receivable.rescheduling;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReschedulingInstallment {

    @JsonProperty("fee")
    private BigDecimal fee;

    @JsonProperty("due_date")
    private LocalDate dueDate;

    @JsonProperty("inst_num")
    private Long installmentNumber;

    @JsonProperty("inst_name")
    private String installmentName;

    @JsonProperty("inst_amount")
    private BigDecimal installmentAmount;

    @JsonProperty("interest_amount")
    private BigDecimal interestAmount;

    @JsonProperty("principal_amount")
    private BigDecimal principalAmount;

}
