package bg.energo.phoenix.model.response.receivable.rescheduling;

import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingPlans;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReschedulingPlansResponse {
    private String number;
    private String installment;
    private BigDecimal amount;
    private BigDecimal principalAmount;
    private BigDecimal interest;
    private BigDecimal fee;
    private LocalDate dueDate;

    public ReschedulingPlansResponse(ReschedulingPlans reschedulingPlans) {
        this.number=reschedulingPlans.getInstallmentNumber();
        this.installment="inst-" + reschedulingPlans.getInstallmentNumber();
        this.amount=reschedulingPlans.getAmount();
        this.principalAmount=reschedulingPlans.getPrincipalAmount();
        this.interest=reschedulingPlans.getInterestAmount();
        this.fee = reschedulingPlans.getFee();
        this.dueDate=reschedulingPlans.getDueDate();
    }
}
