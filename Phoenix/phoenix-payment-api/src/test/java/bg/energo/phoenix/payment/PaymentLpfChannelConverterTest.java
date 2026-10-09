package bg.energo.phoenix.payment;

import bg.energo.phoenix.model.process.latePaymentFIne.InterestCalculationResponseDTO;
import bg.energo.phoenix.model.process.latePaymentFIne.ResultItemDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaymentLpfChannelConverterTest {

    private static final Long BGN = 1001L;
    private static final Long EUR = 1002L;

    @Test
    void toChannelCurrency_shouldIgnoreRatePeriodItemCurrency_whenMainCurrencyMatchesChannel() {
        InterestCalculationResponseDTO dto = new InterestCalculationResponseDTO();
        dto.setCalculatedInterest(new BigDecimal("3420"));
        ResultItemDTO item = new ResultItemDTO();
        item.setCurrencyId(BGN);
        item.setCalculatedInterest(new BigDecimal("3420"));
        dto.setResults(List.of(item));

        BigDecimal eur = PaymentLpfChannelConverter.toChannelCurrency(
                dto,
                EUR,
                EUR,
                this::bgnToEur
        );

        assertEquals(new BigDecimal("3420.00"), eur);
    }

    @Test
    void toChannelCurrency_shouldRoundBeforeConverting_matchingObligations() {
        InterestCalculationResponseDTO dto = new InterestCalculationResponseDTO();
        dto.setCalculatedInterest(new BigDecimal("50.01"));
        ResultItemDTO item = new ResultItemDTO();
        item.setCurrencyId(EUR);
        item.setCalculatedInterest(new BigDecimal("50.005"));
        dto.setResults(List.of(item));

        BigDecimal bgn = PaymentLpfChannelConverter.toChannelCurrency(
                dto,
                EUR,
                BGN,
                this::eurToBgn
        );

        assertEquals(new BigDecimal("97.81"), bgn);
    }

    @Test
    void toChannelCurrency_shouldConvertFromMainCurrency_whenLiabilityIsInChannelCurrency() {
        InterestCalculationResponseDTO dto = new InterestCalculationResponseDTO();
        dto.setCalculatedInterest(new BigDecimal("84.00"));
        ResultItemDTO item = new ResultItemDTO();
        item.setCurrencyId(EUR);
        item.setCalculatedInterest(new BigDecimal("84.00000"));
        dto.setResults(List.of(item));

        BigDecimal bgn = PaymentLpfChannelConverter.toChannelCurrency(
                dto,
                EUR,
                BGN,
                this::eurToBgn
        );

        assertEquals(new BigDecimal("164.29"), bgn);
    }

    @Test
    void toChannelCurrency_shouldRoundSummedItemsBeforeConverting_whenTopLevelMissing() {
        InterestCalculationResponseDTO dto = new InterestCalculationResponseDTO();
        ResultItemDTO first = new ResultItemDTO();
        first.setCurrencyId(EUR);
        first.setCalculatedInterest(new BigDecimal("25.0025"));
        ResultItemDTO second = new ResultItemDTO();
        second.setCurrencyId(EUR);
        second.setCalculatedInterest(new BigDecimal("25.0025"));
        dto.setResults(List.of(first, second));

        BigDecimal bgn = PaymentLpfChannelConverter.toChannelCurrency(
                dto,
                EUR,
                BGN,
                this::eurToBgn
        );

        assertEquals(new BigDecimal("97.81"), bgn);
    }

    @Test
    void toChannelCurrency_shouldUseTopLevelInterest_whenCappedBelowItemsSum() {
        InterestCalculationResponseDTO dto = new InterestCalculationResponseDTO();
        dto.setCalculatedInterest(new BigDecimal("4.00"));
        ResultItemDTO item = new ResultItemDTO();
        item.setCurrencyId(EUR);
        item.setCalculatedInterest(new BigDecimal("63.92800"));
        dto.setResults(List.of(item));

        BigDecimal eur = PaymentLpfChannelConverter.toChannelCurrency(
                dto,
                EUR,
                EUR,
                this::bgnToEur
        );

        assertEquals(new BigDecimal("4.00"), eur);
    }

    @Test
    void toChannelCurrency_shouldNotConvert_whenResultCurrencyMatchesChannel() {
        InterestCalculationResponseDTO dto = new InterestCalculationResponseDTO();
        ResultItemDTO item = new ResultItemDTO();
        item.setCurrencyId(EUR);
        item.setCalculatedInterest(new BigDecimal("1680.00"));
        dto.setResults(List.of(item));

        BigDecimal eur = PaymentLpfChannelConverter.toChannelCurrency(
                dto,
                EUR,
                EUR,
                this::bgnToEur
        );

        assertEquals(new BigDecimal("1680.00"), eur);
    }

    @Test
    void toChannelCurrency_shouldConvertTopLevelUsingFallback_whenResultsMissing() {
        InterestCalculationResponseDTO dto = new InterestCalculationResponseDTO();
        dto.setCalculatedInterest(new BigDecimal("13653.00"));

        BigDecimal eur = PaymentLpfChannelConverter.toChannelCurrency(
                dto,
                BGN,
                EUR,
                this::bgnToEur
        );

        assertEquals(new BigDecimal("6826.50"), eur);
    }

    @Test
    void toChannelCurrency_shouldRoundUnroundedJsonInterestToTwoDecimals_whenResultCurrencyMatchesChannel() {
        InterestCalculationResponseDTO dto = new InterestCalculationResponseDTO();
        ResultItemDTO item = new ResultItemDTO();
        item.setCurrencyId(EUR);
        item.setCalculatedInterest(new BigDecimal("9.11490"));
        dto.setResults(List.of(item));

        BigDecimal eur = PaymentLpfChannelConverter.toChannelCurrency(
                dto,
                EUR,
                EUR,
                this::bgnToEur
        );

        assertEquals(new BigDecimal("9.11"), eur);
    }

    @Test
    void toChannelCurrency_shouldRoundSummedJsonInterestToTwoDecimals_matchingCalculateLfp() {
        InterestCalculationResponseDTO dto = new InterestCalculationResponseDTO();
        ResultItemDTO item = new ResultItemDTO();
        item.setCurrencyId(EUR);
        item.setCalculatedInterest(new BigDecimal("175.56318"));
        dto.setResults(List.of(item));

        BigDecimal eur = PaymentLpfChannelConverter.toChannelCurrency(
                dto,
                EUR,
                EUR,
                this::bgnToEur
        );

        assertEquals(new BigDecimal("175.56"), eur);
    }

    @Test
    void roundToObligationsScale_shouldAcceptPaidSumEqualToObligationsPrincipalPlusInterest() {
        BigDecimal principal = new BigDecimal("23.00");
        BigDecimal jsonInterest = new BigDecimal("9.11490");
        BigDecimal paidSumFromObligations = new BigDecimal("32.11");

        BigDecimal expectedTotal = principal.add(
                PaymentLpfChannelConverter.roundToObligationsScale(jsonInterest)
        );

        assertEquals(0, paidSumFromObligations.compareTo(expectedTotal));
    }

    @Test
    void toChannelCurrency_shouldReturnNull_whenDtoMissing() {
        assertNull(PaymentLpfChannelConverter.toChannelCurrency(null, BGN, EUR, this::bgnToEur));
    }

    private BigDecimal eurToBgn(BigDecimal amount, Long fromId, Long toId) {
        if (EUR.equals(fromId) && BGN.equals(toId)) {
            return amount.multiply(new BigDecimal("1.95583")).setScale(2, RoundingMode.HALF_UP);
        }
        throw new IllegalStateException("Unexpected conversion " + fromId + " -> " + toId);
    }

    private BigDecimal bgnToEur(BigDecimal amount, Long fromId, Long toId) {
        if (BGN.equals(fromId) && EUR.equals(toId)) {
            return amount.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        }
        throw new IllegalStateException("Unexpected conversion " + fromId + " -> " + toId);
    }
}
