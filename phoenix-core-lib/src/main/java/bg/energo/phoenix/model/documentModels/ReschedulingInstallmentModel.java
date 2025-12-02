package bg.energo.phoenix.model.documentModels;

import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingPlans;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReschedulingInstallmentModel {
    public String Number;
    public LocalDate DueDate;
    public BigDecimal InstallmentAmount;
    public BigDecimal PrincipleAmount;
    public BigDecimal WithoutInterestsAmount;
    public BigDecimal InterestsAmount;

    public void from(ReschedulingPlans reschedulingPlans) {
        this.Number=reschedulingPlans.getInstallmentNumber();
        this.DueDate=reschedulingPlans.getDueDate();
        this.InstallmentAmount=reschedulingPlans.getAmount();
        this.PrincipleAmount = reschedulingPlans.getPrincipalAmount();
        this.InterestsAmount=reschedulingPlans.getInterestAmount();
        this.WithoutInterestsAmount = reschedulingPlans.getAmountWithoutInterests();
    }
}
