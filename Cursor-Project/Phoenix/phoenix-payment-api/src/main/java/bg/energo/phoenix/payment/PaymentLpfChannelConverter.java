package bg.energo.phoenix.payment;

import bg.energo.phoenix.model.process.latePaymentFIne.InterestCalculationResponseDTO;
import bg.energo.phoenix.model.process.latePaymentFIne.ResultItemDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Converts the {@code receivable.calculate_lfp_json} result into collection-channel currency so that
 * receipt/pay validation uses exactly the Interest shown by {@code GET /{channel}/obligations}.
 * <p>
 * {@code calculate_lfp} is always in the <b>main currency</b> (every period amount goes through
 * {@code convert_to_currency(.., liability.currency_id, 0)}, i.e. to the main currency), whatever the
 * liability currency. Obligations therefore computes Interest as
 * {@code convert_to_currency(calculate_lfp(liability, date), main_currency_id, channel_currency_id, 2)}
 * (no conversion when main = channel), the same as {@code receivable.online_payment_check} used by
 * EasyPay / CashTerminal / VirtualPos: it <b>rounds to 2 decimals first, then converts once</b>.
 * This class mirrors that order:
 * <ol>
 *     <li>Take the JSON top-level {@code calculated_interest}. It is produced by the same code as
 *     {@code calculate_lfp} and is already {@code round(total, 2)}, where the total includes the
 *     last period fee and the min/max interest caps. The per-period {@code results[]} items do
 *     not (they carry the raw 5-decimal period interest only), so their sum is used only as a
 *     fallback when the top-level value is absent, rounded to 2 decimals before conversion.</li>
 *     <li>Convert that 2-decimal amount from the main currency ({@code lpfCurrencyId}) to the channel
 *     currency once.</li>
 * </ol>
 * Converting the raw items and rounding afterwards (the previous behaviour) diverges by 0.01
 * (e.g. 50.005 EUR: 50.01 * 1.95583 = 97.81 vs 50.005 * 1.95583 = 97.80 лева, PHN-4460).
 * <p>
 * The item {@code currency_id} is the interest-rate period (fee) currency, not the currency of
 * {@code calculated_interest}: every period interest is computed in the main currency, so the
 * item currency is ignored here.
 */
public final class PaymentLpfChannelConverter {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    @FunctionalInterface
    public interface Convert {
        BigDecimal apply(BigDecimal amount, Long fromCurrencyId, Long toCurrencyId);
    }

    private PaymentLpfChannelConverter() {
    }

    /**
     * @param dto               parsed {@code calculate_lfp_json} result
     * @param lpfCurrencyId     currency of the {@code calculate_lfp} amount = the current main currency
     *                          (obligations SQL: {@code channel_cfg.main_currency_id}); not the liability currency
     * @param channelCurrencyId collection-channel currency
     * @param convert           conversion with 2-decimal rounding ({@code receivable.convert_to_currency(.., 2)})
     * @return interest in channel currency with scale 2, or {@code null} when nothing was calculated
     */
    public static BigDecimal toChannelCurrency(
            InterestCalculationResponseDTO dto,
            Long lpfCurrencyId,
            Long channelCurrencyId,
            Convert convert
    ) {
        if (dto == null) {
            return null;
        }
        BigDecimal lpf = calculateLfpAmount(dto);
        if (lpf == null) {
            return null;
        }
        return roundToObligationsScale(convertIfNeeded(lpf, lpfCurrencyId, channelCurrencyId, convert));
    }

    /**
     * The {@code calculate_lfp} value (2 decimals, source currency) carried by the JSON result.
     */
    static BigDecimal calculateLfpAmount(InterestCalculationResponseDTO dto) {
        if (dto.getCalculatedInterest() != null) {
            return roundToObligationsScale(dto.getCalculatedInterest());
        }
        if (dto.getResults() == null) {
            return null;
        }
        BigDecimal sum = null;
        for (ResultItemDTO item : dto.getResults()) {
            if (item == null || item.getCalculatedInterest() == null) {
                continue;
            }
            sum = sum == null ? item.getCalculatedInterest() : sum.add(item.getCalculatedInterest());
        }
        return roundToObligationsScale(sum);
    }

    static BigDecimal roundToObligationsScale(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private static BigDecimal convertIfNeeded(
            BigDecimal amount,
            Long fromCurrencyId,
            Long channelCurrencyId,
            Convert convert
    ) {
        if (fromCurrencyId == null || channelCurrencyId == null || Objects.equals(fromCurrencyId, channelCurrencyId)) {
            return amount;
        }
        BigDecimal converted = convert.apply(amount, fromCurrencyId, channelCurrencyId);
        return converted != null ? converted : amount;
    }
}
