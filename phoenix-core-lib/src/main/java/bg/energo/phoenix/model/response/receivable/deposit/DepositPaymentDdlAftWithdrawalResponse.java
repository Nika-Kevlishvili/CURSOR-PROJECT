package bg.energo.phoenix.model.response.receivable.deposit;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.receivable.deposit.DepositPaymentDeadlineAfterWithdrawal;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import bg.energo.phoenix.model.enums.receivable.deposit.DepositPaymentDeadlineExclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepositPaymentDdlAftWithdrawalResponse {
    private Long id;
    private CalendarType calendarType;
    private Integer value;
    private Integer valueFrom;
    private Integer valueTo;
    private Long calendarId;
    private String calendarName;
    private List<DueDateChange> dueDateChangeList;
    private EntityStatus status;
    private List<DepositPaymentDeadlineExclude> depositPaymentDeadlineExcludes;

    public DepositPaymentDdlAftWithdrawalResponse(DepositPaymentDeadlineAfterWithdrawal withdrawal, Calendar calendar) {
        this.id = withdrawal.getId();
        this.calendarType = withdrawal.getCalendarType();
        this.value = withdrawal.getValue();
        this.calendarId = withdrawal.getCalendarId();
        this.status = withdrawal.getStatus();
        if(withdrawal.getValueFrom() != null){
            this.valueFrom = withdrawal.getValueFrom();
        }
        if(withdrawal.getValueTo() != null){
            this.valueTo = withdrawal.getValueTo();
        }
        if(withdrawal.getDueDateChange() != null){
            this.dueDateChangeList = withdrawal.getDueDateChange();
        }
        if(withdrawal.getDueDateChange() != null){
            this.depositPaymentDeadlineExcludes = withdrawal.getDepositPaymentDeadlineExcludes();
        }
        this.calendarName = calendar.getName();
    }
}
