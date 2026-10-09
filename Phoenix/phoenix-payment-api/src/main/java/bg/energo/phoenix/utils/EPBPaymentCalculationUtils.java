package bg.energo.phoenix.utils;

import bg.energo.phoenix.model.CheckLiabilityDetails;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class EPBPaymentCalculationUtils {

    public static long computeScaledLiabilitiesAmount(Collection<CheckLiabilityDetails> liabilities) {
        BigDecimal totalAmount = liabilities
                .stream()
                .map(l -> detectTotalAmount(l.getTotalAmount(), l.getCurrentAmount()))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return convertToCoinAmount(totalAmount);
    }

    public static long convertToCoinAmount(BigDecimal amount) {
        return amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    public static BigDecimal convertToUnitAmount(Long amount) {
        return BigDecimal
                .valueOf(amount)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal calculateLatePaymentFineAmount(BigDecimal liabilityAmountWithLatePayment, BigDecimal liabilityAmount) {
        if (liabilityAmountWithLatePayment == null) {
            return null;
        }

        return liabilityAmountWithLatePayment.subtract(liabilityAmount);
    }

    public static BigDecimal detectTotalAmount(BigDecimal liabilityAmountWithLatePayment, BigDecimal liabilityAmount) {
        return Optional.ofNullable(liabilityAmountWithLatePayment).orElse(liabilityAmount);
    }

    public static <T> BigDecimal calculateTotalAmount(List<T> entities, Function<T, BigDecimal> amountExtractor) {
        return entities
                .stream()
                .map(amountExtractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static boolean isValidLiability(CheckLiabilityDetails liability) {
        BigDecimal currentAmount = liability.getCurrentAmount();
        BigDecimal amountToPay = liability.getTotalAmount();

        if (currentAmount == null && amountToPay == null) {
            return false; // If both currentAmount and amountToPay are null, the liability is invalid
        }

        // Only proceed if either the current amount or amount to pay is valid and greater than zero
        return EPBPaymentCalculationUtils.convertToCoinAmount(Objects.requireNonNullElse(amountToPay, currentAmount)) > 0;
    }

}
