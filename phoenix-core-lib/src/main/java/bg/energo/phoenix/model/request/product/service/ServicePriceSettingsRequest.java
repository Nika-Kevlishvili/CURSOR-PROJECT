package bg.energo.phoenix.model.request.product.service;

import bg.energo.phoenix.model.customAnotations.product.service.ValidServiceAmountValues;
import bg.energo.phoenix.model.customAnotations.product.service.ValidServiceInstallmentNumbers;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ValidServiceInstallmentNumbers
@ValidServiceAmountValues
public class ServicePriceSettingsRequest {

    @NotNull(message = "priceSettings.equalMonthlyInstallmentsActivation-[equalMonthlyInstallmentsActivation] field Cannot be null;")
    private Boolean equalMonthlyInstallmentsActivation;

    @Min(value = 1, message = "priceSettings.installmentNumber-Installment Number should be in range: [{1:9999}];")
    @Max(value = 9999, message = "priceSettings.installmentNumber-Installment Number should be in range: [{1:9999}];")
    private Short installmentNumber;

    @Min(value = 1, message = "priceSettings.installmentNumberFrom-Installment Number From should be in range: [{1:9999}];")
    @Max(value = 9999, message = "priceSettings.installmentNumberFrom-Installment Number From should be in range: [{1:9999}];")
    private Short installmentNumberFrom;

    @Min(value = 1, message = "priceSettings.installmentNumberTo-Installment Number To should be in range: [{1:9999}];")
    @Max(value = 9999, message = "priceSettings.installmentNumberTo-Installment Number To should be in range: [{1:9999}];")
    private Short installmentNumberTo;

    @DecimalMin(value = "0.01", message = "priceSettings.amount-Amount minimum value is [{value}];")
    @DecimalMax(value = "999999999999.99", message = "priceSettings.amount-Amount maximum value if [{value}];")
    @Digits(integer = 12, fraction = 2, message = "priceSettings.amount-Invalid Decimal fractions: max [{integer}] digits and [{fraction}] fractions;")
    private BigDecimal amount;

    @DecimalMin(value = "0.01", message = "priceSettings.amountFrom-Amount From minimum value is [{value}];")
    @DecimalMax(value = "999999999999.99", message = "priceSettings.amountFrom-Amount From maximum value if [{value}];")
    @Digits(integer = 12, fraction = 2, message = "priceSettings.amount-Invalid Decimal fractions: max [{integer}] digits and [{fraction}] fractions;")
    private BigDecimal amountFrom;

    @DecimalMin(value = "0.01", message = "priceSettings.amountTo-Amount To minimum value is [{value}];")
    @DecimalMax(value = "999999999999.99", message = "priceSettings.amountTo-Amount To maximum value if [{value}];")
    @Digits(integer = 12, fraction = 2, message = "priceSettings.amountTo-Invalid Decimal fractions: max [{integer}] digits and [{fraction}] fractions;")
    private BigDecimal amountTo;

    private Long currencyId;

}
