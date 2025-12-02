package bg.energo.phoenix.model.request.product.term.terms;

import bg.energo.phoenix.model.enums.product.term.terms.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TermsRequest {
    private String name;
    private Integer contractDeliveryActivationValue;
    private ContractDeliveryActivationType contractDeliveryActivationType;
    private Boolean contractDeliveryActivationAutoTermination;
    private Integer resigningDeadlineValue;
    private ResigningDeadlineType resigningDeadlineType;
    private Boolean supplyActivationFirstDayOfMonth;
    private Boolean supplyActivationFirstDayAfterExpiringContract;
    private Boolean supplyActivationExactDate;
    private Integer supplyActivationExactDateStartDay;
    private Boolean supplyActivationManual;
    private Integer generalNoticePeriodValue;
    private GeneralNoticePeriodType generalNoticePeriodType;
    private Integer noticeTermPeriodValue;
    private NoticeTermPeriodType noticeTermPeriodType;
    private Integer noticeTermDisconnectionPeriodValue;
    private NoticeTermDisconnectionPeriodType noticeTermDisconnectionPeriodType;
    private Boolean contractEntryIntoForceFromSigning;
    private Boolean contractEntryIntoForceFromExactDayOfMonth;
    private Integer contractEntryIntoForceFromExactDayOfMonthStartDay;
    private Boolean contractEntryIntoForceFromDateChangeOfCBG;
    private Boolean contractEntryIntoForceFromFirstDelivery;
    private Boolean contractEntryIntoForceManual;
    private Boolean noInterestOnOverdueDebts;
    private TermStatus status;
}
