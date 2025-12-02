package bg.energo.phoenix.util.epb;

import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public abstract class EPBDecimalUtils {
    /**
     * Converts a BigDecimal number to currency scale.
     * If provided number is with bigger scale dimension, it will be saved
     *
     * @param number The BigDecimal number to convert.
     * @return The converted BigDecimal number with currency scale. example: 15.23
     * @throws RuntimeException if the provided number is null.
     */
    public static BigDecimal convertToCurrencyScale(BigDecimal number) {
        if (number == null) {
            throw new RuntimeException("Provided number is null");
        }

        return number.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the summary of a list of BigDecimal numbers.
     *
     * @param numbers The list of BigDecimal numbers to calculate the summary for.
     * @return The calculated summary as a BigDecimal number.
     */
    public static BigDecimal calculateSummary(List<BigDecimal> numbers) {
        BigDecimal result = new BigDecimal("0");
        List<BigDecimal> formattedNumbers = numbers
                .stream()
                .map(number -> Objects.requireNonNullElse(number, BigDecimal.ZERO))
                .toList();

        if (CollectionUtils.isNotEmpty(formattedNumbers)) {
            for (BigDecimal number : formattedNumbers) {
                result = result.add(number);
            }
        }

        return result;
    }

    /**
     * Rounds the provided BigDecimal value to two decimal places, rounding up.
     *
     * @param number The BigDecimal value to round.
     * @return The rounded BigDecimal value, or null if the provided value is null.
     */
    public static BigDecimal roundToTwoDecimalPlaces(BigDecimal number) {
        return number == null ? null : number.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Truncates the provided BigDecimal value to two decimal places, rounding down.
     *
     * @param value The BigDecimal value to truncate.
     * @return The truncated BigDecimal value, or null if the provided value is null.
     */
    public static BigDecimal truncateToTwoDecimals(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.DOWN);
    }
}
