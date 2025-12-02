package bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.billing.billingRun.DetailedDataRowDateRangeValidator;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@DetailedDataRowDateRangeValidator
public class DetailedDataRowParameters {

    private Long id;

    @Size(min = 1, max = 1024, message = "priceComponent-[priceComponent] length should be between {min} and {max};")
    private String priceComponent;

    @Pattern(regexp = "^[A-Za-z0-9]{1,33}$", message = "pointOfDelivery-[pointOfDelivery] should be between 1 to 33 characters, allowed symbols are A-Z a-z 0-9;")
    private String pointOfDelivery;

    @DateRangeValidator(fieldPath = "detailedDataRowParameters.periodFrom", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate periodFrom;

    @DateRangeValidator(fieldPath = "detailedDataRowParameters.periodTo", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate periodTo;

    @Pattern(regexp = "^[A-Za-z0-9]{1,32}$", message = "meter-[meter] should be between 1 to 32 characters, allowed symbols are A-Z a-z 0-9;")
    private String meter;

    @DecimalMin(value = "0", message = "newMeterReading-[newMeterReading] minimum value is {value};")
    @DecimalMax(value = "999999999.99999", message = "newMeterReading-[newMeterReading] maximum value is {value};")
    @Digits(integer = 9, fraction = 5, message = "newMeterReading-newMeterReading Invalid format. newMeterReading should be in range 0 - 999999999.99999;")
    private BigDecimal newMeterReading;

    @DecimalMin(value = "0", message = "oldMeterReading-[oldMeterReading] minimum value is {value};")
    @DecimalMax(value = "999999999.99999", message = "oldMeterReading-[oldMeterReading] maximum value is {value};")
    @Digits(integer = 9, fraction = 5, message = "oldMeterReading-oldMeterReading Invalid format. oldMeterReading should be in range 0 - 999999999.99999;")
    private BigDecimal oldMeterReading;

    @DecimalMin(value = "0", message = "differences-[differences] minimum value is {value};")
    @DecimalMax(value = "999999999.99999", message = "differences-[differences] maximum value is {value};")
    @Digits(integer = 9, fraction = 5, message = "differences-differences Invalid format. differences should be in range 0 - 999999999.99999;")
    private BigDecimal differences;

    @DecimalMin(value = "0.01", message = "multiplier-[multiplier] minimum value is {value};")
    @DecimalMax(value = "9999999.99", message = "multiplier-[multiplier] maximum value is {value};")
    @Digits(integer = 7, fraction = 2, message = "multiplier-multiplier Invalid format. multiplier should be in range 0.01 - 9999999.99;")
    private BigDecimal multiplier;

    @DecimalMin(value = "0", message = "correction-[correction] minimum value is {value};")
    @DecimalMax(value = "999999999.99999", message = "correction-[correction] maximum value is {value};")
    @Digits(integer = 9, fraction = 5, message = "correction-correction Invalid format. correction should be in range 0 - 999999999.99999;")
    private BigDecimal correction;

    @DecimalMin(value = "0", message = "deducted-[deducted] minimum value is {value};")
    @DecimalMax(value = "999999999.99999", message = "deducted-[deducted] maximum value is {value};")
    @Digits(integer = 9, fraction = 5, message = "deducted-deducted Invalid format. deducted should be in range 0 - 999999999.99999;")
    private BigDecimal deducted;

    @DecimalMin(value = "-999999999.99999", message = "totalVolumes-[totalVolumes] minimum value is {value};")
    @DecimalMax(value = "999999999.99999", message = "totalVolumes-[totalVolumes] maximum value is {value};")
    @Digits(integer = 9, fraction = 5, message = "totalVolumes-totalVolumes Invalid format. totalVolumes should be in range -999999999.99999 - 999999999.99999;")
    private BigDecimal totalVolumes;

    @Size(min = 1, max = 512, message = "unitOfMeasureForTotalVolumes-[unitOfMeasureForTotalVolumes] length should be between {min} and {max};")
    private String unitOfMeasureForTotalVolumes;

    @DecimalMin(value = "-999999999.9999999999", message = "unitPrice-[unitPrice] minimum value is {value};")
    @DecimalMax(value = "999999999.9999999999", message = "unitPrice-[unitPrice] maximum value is {value};")
    @Digits(integer = 9, fraction = 10, message = "unitPrice-unitPrice Invalid format. unitPrice should be in range -999999999.9999999999 - 999999999.9999999999;")
    private BigDecimal unitPrice;

    @Size(min = 1, max = 512, message = "unitOfMeasureForUnitPrice-[unitOfMeasureForUnitPrice] length should be between {min} and {max};")
    private String unitOfMeasureForUnitPrice;

    @DecimalMin(value = "-999999999.9999999999", message = "currentValue-[currentValue] minimum value is {value};")
    @DecimalMax(value = "999999999.9999999999", message = "currentValue-[currentValue] maximum value is {value};")
    @Digits(integer = 9, fraction = 10, message = "currentValue-currentValue Invalid format. currentValue should be in range -999999999.9999999999 - 999999999.9999999999;")
    private BigDecimal currentValue;

    private Long valueCurrencyId;

    @Size(min = 1, max = 32, message = "incomeAccount-[incomeAccount] length should be between {min} and {max};")
    private String incomeAccount;

    @Size(min = 1, max = 32, message = "costCenter-[costCenter] length should be between {min} and {max};")
    private String costCenter;

    private Long vatRateId;

    private Boolean globalVatRate;

}
