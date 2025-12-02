package bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula;

import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.*;

import java.math.BigDecimal;

public interface IapResponseFromNativeQuery {
    Long getId();

    String getName();

    ValueType getValueType();

    BigDecimal getValue();

    BigDecimal getValueFrom();

    BigDecimal getValueTo();

    PaymentType getPaymentType();

    DateOfIssueType getDateOfIssueType();

    Integer getDateOfIssueValue();

    Integer getDateOfIssueValueFrom();

    Integer getDateOfIssueValueTo();

    IssuingForTheMonthToCurrent getIssuingForTheMonthToCurrent();

    DeductionFrom getDeductionFrom();

    Boolean getMatchTermOfStandardInvoice();

    Boolean getNoInterestOnOverdueDebts();

    InterimAdvancePaymentStatus getStatus();

    Long getGroupDetailId();

}
