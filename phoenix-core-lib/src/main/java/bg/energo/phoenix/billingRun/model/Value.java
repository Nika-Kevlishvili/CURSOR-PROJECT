package bg.energo.phoenix.billingRun.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public abstract class Value {
    private BigDecimal value;
    private BigDecimal valueBefore9DigitRounding;
    private BigDecimal valueDeduction;
    private BigDecimal valueDeductionBefore9DigitRounding;
    private BigDecimal valueCorrection;
    private BigDecimal valueCorrectionBefore9DigitRounding;


    protected LocalDate dateFrom;

    public Integer getValueNinthDigitAfterDot() {
        if (valueBefore9DigitRounding == null)
            return null;
        String numberStr = valueBefore9DigitRounding.toPlainString();
        int indexOfDot = numberStr.indexOf('.');
        if (indexOfDot == 0) {
            return null;
        }
        return (int) numberStr.charAt(numberStr.indexOf('.') + 9);
    }

    public Integer getDeductionNinthDigitAfterDot() {
        if (valueDeductionBefore9DigitRounding == null)
            return null;
        String numberStr = valueDeductionBefore9DigitRounding.toPlainString();
        int indexOfDot = numberStr.indexOf('.');
        if (indexOfDot == 0) {
            return null;
        }
        return (int) numberStr.charAt(numberStr.indexOf('.') + 9);
    }

    public Integer getCorrectionNinthDigitAfterDot() {
        if (valueCorrectionBefore9DigitRounding == null)
            return null;
        String numberStr = valueCorrectionBefore9DigitRounding.toPlainString();
        int indexOfDot = numberStr.indexOf('.');
        if (indexOfDot == 0) {
            return null;
        }
        return (int) numberStr.charAt(numberStr.indexOf('.') + 9);
    }
}
