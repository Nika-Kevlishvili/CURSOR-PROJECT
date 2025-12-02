package bg.energo.phoenix.util.math;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

public abstract class BillingRunRounding {
    private static final BigDecimal INCREMENT = new BigDecimal("0.0001");

    /**
     * Round total volumes of intersection and total volumes of out of intersection depend on
     * <h2>Rounding general rule</h2>
     * <li>Round after decimal point 4 digits.</li>
     * <li>If we have kw hours more after sum rounded values, than we should start remove one
     * by one this kw hours from rounded values. Start removing 0.0001 kw hours from
     * rounded values for which before rounding the part after 4th digit after decimal point was smallest.</li>
     * <p>
     * <li>If we have kw hours less after sum rounded values, than we should start add one
     * by one this kw hours in  rounded values. Start
     * adding 0.0001 kw hours in rounded values for which before rounding the part after
     * 4th digit after decimal point was biggest.</li>
     * <p>
     * <li>if all rounded values part after 4th digit after decimal point have same. We should add/remove
     * 0.0001kwh from rounded values which have the earliest date range.</li>
     *
     * @param totalVolume The total volume to be rounded.
     * @param payload     The list of payload items to be rounded.
     * @return The list of rounded payload items.
     */
    public static List<BillingRunRoundingPayload> round(BigDecimal totalVolume, List<BillingRunRoundingPayload> payload) {
        // round total volume
        totalVolume = totalVolume.setScale(4, RoundingMode.HALF_UP);

        List<BillingRunRoundingPayload> roundedPayloadContext = new ArrayList<>();

        // round each payload and calculate total
        BigDecimal totalRoundedVolume = BigDecimal.ZERO;
        for (BillingRunRoundingPayload p : payload) {
            BigDecimal rounded = p.getVolume().setScale(4, RoundingMode.HALF_UP);
            roundedPayloadContext.add(new BillingRunRoundingPayload(p.getId(), rounded, p.periodFrom));
            totalRoundedVolume = totalRoundedVolume.add(rounded);
        }

        BigDecimal diff = totalVolume.subtract(totalRoundedVolume);

        BillingRunRoundingValueComparator billingRunRoundingValueComparator = new BillingRunRoundingValueComparator();
        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            roundVolumesWhenTotalVolumesIsMoreThenRoundedVolumes(payload, roundedPayloadContext, diff, billingRunRoundingValueComparator);
        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
            roundVolumesWhenTotalVolumesIsLessThenRoundedVolumes(payload, roundedPayloadContext, diff, billingRunRoundingValueComparator);
        }

        stripTrailingZeroes(payload, roundedPayloadContext);

        return roundedPayloadContext;
    }

    /**
     * This method performs static rounding on the totalVolume and payload items.
     *
     * @param totalVolume The total volume to be rounded.
     * @param payload     The list of payload items to be rounded.
     */
    public static void staticRounding(BigDecimal totalVolume, List<BillingRunRoundingPayload> payload) {
        BigDecimal roundedTotalVolume = totalVolume.setScale(4, RoundingMode.HALF_UP);

        List<BillingRunRoundingPayload> naturalPayloadContext = new ArrayList<>();
        payload.forEach(p -> naturalPayloadContext.add(new BillingRunRoundingPayload(p.id, p.volume, p.periodFrom)));

        BigDecimal totalRoundedVolume = BigDecimal.ZERO;
        for (BillingRunRoundingPayload p : payload) {
            p.volume = p.getVolume().setScale(4, RoundingMode.HALF_UP);
            totalRoundedVolume = totalRoundedVolume.add(p.volume);
        }

        BigDecimal diff = roundedTotalVolume.subtract(totalRoundedVolume);

        BillingRunRoundingValueComparator billingRunRoundingValueComparator = new BillingRunRoundingValueComparator();

        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            roundVolumesWhenTotalVolumesIsMoreThenRoundedVolumes(naturalPayloadContext, payload, diff, billingRunRoundingValueComparator);
        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
            roundVolumesWhenTotalVolumesIsLessThenRoundedVolumes(naturalPayloadContext, payload, diff, billingRunRoundingValueComparator);
        }

        stripTrailingZeroes(payload, naturalPayloadContext);
    }

    private static void roundVolumesWhenTotalVolumesIsLessThenRoundedVolumes(List<BillingRunRoundingPayload> payload, List<BillingRunRoundingPayload> roundedPayloadContext, BigDecimal diff, BillingRunRoundingValueComparator billingRunRoundingValueComparator) {
        while (diff.compareTo(BigDecimal.ZERO) < 0) {
            List<BillingRunRoundingPayload> payloadItems = new ArrayList<>(payload);

            while (!payloadItems.isEmpty()) {
                BillingRunRoundingPayload naturalPayloadThatShouldBeDecreased = payloadItems
                        .stream()
                        .min(billingRunRoundingValueComparator.thenComparing(BillingRunRoundingPayload::getPeriodFrom).thenComparing(BillingRunRoundingPayload::getId, Comparator.reverseOrder()))
                        .orElseThrow(() -> new ClientException(ErrorCode.APPLICATION_ERROR));

                payloadItems.remove(naturalPayloadThatShouldBeDecreased);

                BillingRunRoundingPayload roundedPayload = roundedPayloadContext
                        .stream()
                        .filter(p -> p.getId().equals(naturalPayloadThatShouldBeDecreased.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ClientException(ErrorCode.APPLICATION_ERROR));

                roundedPayload.setVolume(roundedPayload.getVolume().subtract(INCREMENT));

                diff = diff.add(INCREMENT);
                if (diff.compareTo(BigDecimal.ZERO) >= 0) {
                    return;
                }
            }
        }
    }

    private static void roundVolumesWhenTotalVolumesIsMoreThenRoundedVolumes(List<BillingRunRoundingPayload> payload, List<BillingRunRoundingPayload> roundedPayloadContext, BigDecimal diff, BillingRunRoundingValueComparator billingRunRoundingValueComparator) {
        while (diff.compareTo(BigDecimal.ZERO) > 0) {
            List<BillingRunRoundingPayload> payloadItems = new ArrayList<>(payload);

            while (!payloadItems.isEmpty()) {
                BillingRunRoundingPayload naturalPayloadThatShouldBeIncreased = payloadItems
                        .stream()
                        .max(billingRunRoundingValueComparator.thenComparing(BillingRunRoundingPayload::getPeriodFrom, Comparator.reverseOrder()).thenComparing(BillingRunRoundingPayload::getId, Comparator.reverseOrder()))
                        .orElseThrow(() -> new ClientException(ErrorCode.APPLICATION_ERROR));

                payloadItems.remove(naturalPayloadThatShouldBeIncreased);

                BillingRunRoundingPayload roundedPayload = roundedPayloadContext
                        .stream()
                        .filter(p -> p.getId().equals(naturalPayloadThatShouldBeIncreased.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ClientException(ErrorCode.APPLICATION_ERROR));

                roundedPayload.setVolume(roundedPayload.getVolume().add(INCREMENT));

                diff = diff.subtract(INCREMENT);
                if (diff.compareTo(BigDecimal.ZERO) <= 0) {
                    return;
                }
            }
        }
    }

    @SafeVarargs
    public static void stripTrailingZeroes(List<BillingRunRoundingPayload>... payloads) {
        Arrays
                .stream(payloads)
                .flatMap(Collection::parallelStream)
                .forEach(payload -> payload.setVolume(payload.getVolume().stripTrailingZeros()));
    }

    @Data
    @AllArgsConstructor
    public static class BillingRunRoundingPayload {
        private Long id;
        private BigDecimal volume;
        private LocalDate periodFrom;
    }

    private static class BillingRunRoundingValueComparator implements Comparator<BillingRunRoundingPayload> {
        @Override
        public int compare(BillingRunRoundingPayload o1NaturalVolume, BillingRunRoundingPayload o2NaturalVolume) {
            BigDecimal o1VolumeFifthPositionAfterPointVolume = o1NaturalVolume.volume.setScale(5, RoundingMode.DOWN).subtract(o1NaturalVolume.volume.setScale(4, RoundingMode.DOWN));
            BigDecimal o2VolumeFifthPositionAfterPointVolume = o2NaturalVolume.volume.setScale(5, RoundingMode.DOWN).subtract(o2NaturalVolume.volume.setScale(4, RoundingMode.DOWN));
            return o1VolumeFifthPositionAfterPointVolume.compareTo(o2VolumeFifthPositionAfterPointVolume);
        }
    }
}