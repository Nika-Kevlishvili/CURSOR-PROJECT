package bg.energo.phoenix.util.epb;

import bg.energo.phoenix.model.entity.pod.billingByProfile.BillingDataByProfile;
import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameterDetailInfo;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.enums.time.TimeZone;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeMap;

public class EPBExportUtil {
    public static LocalDateTime adjustStep(LocalDateTime current, Duration step) {
        LocalDateTime next = current.plus(step);

        if (current.getDayOfMonth() == 1 && step.toDays() >= 28) {
            if (current.plusMonths(1).getMonth() != next.getMonth()) {
                return current.plusMonths(1).withDayOfMonth(1);
            }
        }
        return next;
    }

    public static Duration getIntervalDuration(PeriodType periodType) {
        return switch (periodType) {
            case FIFTEEN_MINUTES -> Duration.ofMinutes(15);
            case ONE_HOUR -> Duration.ofHours(1);
            case ONE_DAY -> Duration.ofDays(1);
            case ONE_MONTH -> Duration.ofDays(30);
        };
    }


    public static <T> void fillCompleteDataList(
            TreeMap<LocalDateTime, T> mappedData,
            List<T> completeDataList,
            LocalDateTime current,
            EmptyDetailSupplier<T> emptyDetailSupplier
    ) {
        if (mappedData.containsKey(current)) {
            completeDataList.add(mappedData.get(current));
        } else {
            completeDataList.add(emptyDetailSupplier.getEmptyDetail());
        }
    }

    public static boolean canBeShiftedHour(LocalDateTime current, TimeZone timeZone) {
        if (timeZone == null) {
            return false;
        }
        return switch (timeZone) {
            case CET -> current.getHour() == 3;
            case EET -> current.getHour() == 4;
        };
    }

    @FunctionalInterface
    public interface EmptyDetailSupplier<T> {
        T getEmptyDetail();
    }
}
