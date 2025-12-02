package bg.energo.phoenix.model.response.interimAdvancePayment.copy;

import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.*;
import bg.energo.phoenix.model.response.interimAdvancePayment.periodical.DayOfWeekAndPeriodOfYearAndDateOfMonthResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentResponse;
import bg.energo.phoenix.model.response.terms.copy.InvoicePaymentTermsCopyResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InterimAdvancePaymentCopyResponse {
    private Long id;
    private String name;
    private ValueType valueType;
    private BigDecimal value;
    private BigDecimal valueFrom;
    private BigDecimal valueTo;
    private PriceComponentResponse priceComponent;
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
    private InvoicePaymentTermsCopyResponse interimAdvancePaymentTerm;
    private InterimAdvancePaymentStatus status;
    private Long groupDetailsId;


    public InterimAdvancePaymentCopyResponse(InterimAdvancePayment source,
                                             DayOfWeekAndPeriodOfYearAndDateOfMonthResponse periodConfigurationResponse,
                                             InvoicePaymentTermsCopyResponse interimAdvancePaymentTerm,
                                             PriceComponent copiedPriceComponent) {
        this.name = source.getName();
        this.valueType = source.getValueType();
        this.value = source.getValue();
        this.valueFrom = source.getValueFrom();
        this.valueTo = source.getValueTo();
        this.priceComponent = getPriceComponentCopyResponse(copiedPriceComponent);
        this.currency = getCurrencyNomenclature(source.getCurrency());
        this.paymentType = source.getPaymentType();
        this.dateOfIssueType = source.getDateOfIssueType();
        this.dateOfIssueValue = source.getDateOfIssueValue();
        this.dateOfIssueValueFrom = source.getDateOfIssueValueFrom();
        this.dateOfIssueValueTo = source.getDateOfIssueValueTo();
        this.dayOfWeekAndPeriodOfYearAndDateOfMonth = periodConfigurationResponse;
        this.issuingForTheMonthToCurrent = source.getIssuingForTheMonthToCurrent();
        this.deductionFrom = source.getDeductionFrom();
        this.matchesWithTermOfStandardInvoice = source.getMatchTermOfStandardInvoice();
        this.noInterestInOverdueDebt = source.getNoInterestOnOverdueDebts();
        this.interimAdvancePaymentTerm = interimAdvancePaymentTerm;
        this.status = source.getStatus();
    }


    private PriceComponentResponse getPriceComponentCopyResponse(PriceComponent priceComponent) {
        if (priceComponent != null) {
            return new PriceComponentResponse(priceComponent);
        }
        else return null;
    }


    private CurrencyResponse getCurrencyNomenclature(Currency currency) {
        if (currency == null || !currency.getStatus().equals(NomenclatureItemStatus.ACTIVE)) {
            return null;
        }
        return new CurrencyResponse(currency);
    }
}
