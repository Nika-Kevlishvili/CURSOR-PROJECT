package bg.energo.phoenix.util.math;

import java.math.BigDecimal;
import java.math.MathContext;

public abstract class MathUtils {
    public static BigDecimal calculatePercentage(BigDecimal value, BigDecimal percent) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }

        if (percent == null) {
            throw new IllegalArgumentException("percent is null");
        }

        return value.multiply(percent).divide(BigDecimal.valueOf(100), MathContext.DECIMAL64);
    }
}
