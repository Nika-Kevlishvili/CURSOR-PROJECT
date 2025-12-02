package bg.energo.phoenix.model.response.interimAdvancePayment;

import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.*;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.IapResponseFromNativeQuery;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.PriceComponentFormula;
import bg.energo.phoenix.model.response.interimAdvancePayment.periodical.DayOfWeekAndPeriodOfYearAndDateOfMonthResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentDetailedResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InterimAdvancePaymentResponse {

    private Long id;
    private String name;
    private ValueType valueType;
    private BigDecimal value;
    private BigDecimal valueFrom;
    private BigDecimal valueTo;
    private PriceComponentDetailedResponse priceComponent;
    private CurrencyResponse currency;
    private PaymentType paymentType;
    private DateOfIssueType dateOfIssueType;
    private Integer dateOfIssueValue;
    private Integer dateOfIssueValueFrom;
    private Integer dateOfIssueValueTo;
    private DayOfWeekAndPeriodOfYearAndDateOfMonthResponse dayOfWeekAndPeriodOfYearAndDateOfMonth;
    private IssuingForTheMonthToCurrent issuingForTheMonthToCurrent;
    private DeductionFrom deductionFrom;
    private Boolean matchesWithTermOfStandardInvoice;
    private Boolean noInterestInOverdueDebt;
    private InterimAdvancePaymentTermsResponse interimAdvancePaymentTerm;
    private InterimAdvancePaymentStatus status;
    private Long groupDetailsId;
    private PriceComponentFormula formula;
    private boolean fromGroup;
    private Boolean isLocked;
    public InterimAdvancePaymentResponse(
            InterimAdvancePayment interimAdvancePayment,
            DayOfWeekAndPeriodOfYearAndDateOfMonthResponse dayOfWeekAndPeriodOfYearAndDateOfMonth,
            InterimAdvancePaymentTermsResponse interimAdvancePaymentTerm
    ) {

        this.id = interimAdvancePayment.getId();
        this.name = interimAdvancePayment.getName();
        this.valueType = interimAdvancePayment.getValueType();
        this.value = interimAdvancePayment.getValue();
        this.valueFrom = interimAdvancePayment.getValueFrom();
        this.valueTo = interimAdvancePayment.getValueTo();
//        if(interimAdvancePayment.getPriceComponent() != null) this.priceComponentId = interimAdvancePayment.getPriceComponent().getId();
//        if(interimAdvancePayment.getCurrency() != null) this.currencyId = interimAdvancePayment.getCurrency().getId();
        this.paymentType = interimAdvancePayment.getPaymentType();
        this.dateOfIssueType = interimAdvancePayment.getDateOfIssueType();
        this.dateOfIssueValue = interimAdvancePayment.getDateOfIssueValue();
        this.dateOfIssueValueFrom = interimAdvancePayment.getDateOfIssueValueFrom();
        this.dateOfIssueValueTo = interimAdvancePayment.getDateOfIssueValueTo();
        this.dayOfWeekAndPeriodOfYearAndDateOfMonth = dayOfWeekAndPeriodOfYearAndDateOfMonth;
        this.issuingForTheMonthToCurrent = interimAdvancePayment.getIssuingForTheMonthToCurrent();
        this.deductionFrom = interimAdvancePayment.getDeductionFrom();
        this.matchesWithTermOfStandardInvoice = interimAdvancePayment.getMatchTermOfStandardInvoice();
        this.noInterestInOverdueDebt = interimAdvancePayment.getNoInterestOnOverdueDebts();
        this.interimAdvancePaymentTerm = interimAdvancePaymentTerm;
        this.status = interimAdvancePayment.getStatus();
        this.groupDetailsId = interimAdvancePayment.getGroupDetailId();
    }

    public InterimAdvancePaymentResponse(
            InterimAdvancePayment interimAdvancePayment,
            InterimAdvancePaymentTermsResponse interimAdvancePaymentTerm
    ) {
        this.id = interimAdvancePayment.getId();
        this.name = interimAdvancePayment.getName();
        this.valueType = interimAdvancePayment.getValueType();
        this.value = interimAdvancePayment.getValue();
        this.valueFrom = interimAdvancePayment.getValueFrom();
        this.valueTo = interimAdvancePayment.getValueTo();
        this.paymentType = interimAdvancePayment.getPaymentType();
        this.dateOfIssueType = interimAdvancePayment.getDateOfIssueType();
        this.dateOfIssueValue = interimAdvancePayment.getDateOfIssueValue();
        this.dateOfIssueValueFrom = interimAdvancePayment.getDateOfIssueValueFrom();
        this.dateOfIssueValueTo = interimAdvancePayment.getDateOfIssueValueTo();
        this.issuingForTheMonthToCurrent = interimAdvancePayment.getIssuingForTheMonthToCurrent();
        this.deductionFrom = interimAdvancePayment.getDeductionFrom();
        this.matchesWithTermOfStandardInvoice = interimAdvancePayment.getMatchTermOfStandardInvoice();
        this.noInterestInOverdueDebt = interimAdvancePayment.getNoInterestOnOverdueDebts();
        this.interimAdvancePaymentTerm = interimAdvancePaymentTerm;
        this.status = interimAdvancePayment.getStatus();
        PriceComponentFormula priceComponentFormula = new PriceComponentFormula();
        if(interimAdvancePayment.getPriceComponent()!=null){
            priceComponentFormula.setPriceComponentId(interimAdvancePayment.getPriceComponent().getId());
        }
        this.formula= priceComponentFormula;

        this.groupDetailsId = interimAdvancePayment.getGroupDetailId();
    }

    public InterimAdvancePaymentResponse(
            IapResponseFromNativeQuery interimAdvancePayment,
            InterimAdvancePaymentTermsResponse interimAdvancePaymentTerm
    ) {
        this.id = interimAdvancePayment.getId();
        this.name = interimAdvancePayment.getName();
        this.valueType = interimAdvancePayment.getValueType();
        this.value = interimAdvancePayment.getValue();
        this.valueFrom = interimAdvancePayment.getValueFrom();
        this.valueTo = interimAdvancePayment.getValueTo();
        this.paymentType = interimAdvancePayment.getPaymentType();
        this.dateOfIssueType = interimAdvancePayment.getDateOfIssueType();
        this.dateOfIssueValue = interimAdvancePayment.getDateOfIssueValue();
        this.dateOfIssueValueFrom = interimAdvancePayment.getDateOfIssueValueFrom();
        this.dateOfIssueValueTo = interimAdvancePayment.getDateOfIssueValueTo();
        this.issuingForTheMonthToCurrent = interimAdvancePayment.getIssuingForTheMonthToCurrent();
        this.deductionFrom = interimAdvancePayment.getDeductionFrom();
        this.matchesWithTermOfStandardInvoice = interimAdvancePayment.getMatchTermOfStandardInvoice();
        this.noInterestInOverdueDebt = interimAdvancePayment.getNoInterestOnOverdueDebts();
        this.interimAdvancePaymentTerm = interimAdvancePaymentTerm;
        this.status = interimAdvancePayment.getStatus();
        this.groupDetailsId = interimAdvancePayment.getGroupDetailId();
    }
}
