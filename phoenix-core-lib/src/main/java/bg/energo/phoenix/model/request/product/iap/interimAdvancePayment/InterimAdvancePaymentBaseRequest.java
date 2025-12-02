package bg.energo.phoenix.model.request.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.ValidationsByDateOfIssueValueType;
import bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.ValidationsByValueType;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ValidationsByValueType
@ValidationsByDateOfIssueValueType
public class InterimAdvancePaymentBaseRequest {

    @NotBlank(message = "name-Name is required;")
    @Length(min = 1, max = 1024, message = "name-Name length must be between 1_1024;")
    private String name;

    @NotNull(message = "valueType-Value type is required;")
    private ValueType valueType;

    @DecimalMin(value = "0.01", message = "value-[Value] should be more than 0.01;")
    @DecimalMax(value = "999999999.99", message = "value-[Value] should be less than 999999999.99;")
    private BigDecimal value;

    @DecimalMin(value = "0.01", message = "valueFrom-[valueFrom] should be more than 0.01;")
    @DecimalMax(value = "999999999.99", message = "valueFrom-[valueFrom] should be less than 999999999.99;")
    private BigDecimal valueFrom;

    @DecimalMin(value = "0.01", message = "valueTo-[valueTo] should be more than 0.01;")
    @DecimalMax(value = "999999999.99", message = "valueTo-[valueTo] should be less than 999999999.99;")
    private BigDecimal valueTo;

    private Long priceComponentId;

    private Long currencyId;

    private PaymentType paymentType;

    @NotNull(message = "dateOfIssueType-Date of issue type is required;")
    private DateOfIssueType dateOfIssueType;

    private Integer dateOfIssueValue;

    private Integer dateOfIssueValueFrom;

    private Integer dateOfIssueValueTo;

    @NotNull(message = "issuingForTheMonthToCurrent-is required;")
    private IssuingForTheMonthToCurrent issuingForTheMonthToCurrent;

    @NotNull(message = "deductionFrom-Deduction From is required;")
    private DeductionFrom deductionFrom;

    @NotNull(message = "matchesWithTermOfStandardInvoice-Matches with term of standard invoice is required;")
    private Boolean matchesWithTermOfStandardInvoice;

    @NotNull(message = "noInterestInOverdueDebt-No Interest in Overdue Debt is required;")
    private Boolean noInterestInOverdueDebt;

}
