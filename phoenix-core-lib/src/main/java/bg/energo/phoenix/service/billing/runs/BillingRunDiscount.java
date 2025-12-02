package bg.energo.phoenix.service.billing.runs;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.pod.discount.Discount;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.repository.pod.discount.DiscountRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.service.billing.runs.models.BillingDataCalculatedDiscountModel;
import bg.energo.phoenix.service.billing.runs.models.BillingDataDiscountModel;
import bg.energo.phoenix.util.math.BillingRunRounding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingRunDiscount {
    private final PriceComponentRepository priceComponentRepository;
    private final DiscountRepository discountRepository;

    /**
     * Calculates the discounts for each sub-period within the given period based on the provided BillingDataDiscountModel.
     *
     * @param model The BillingDataDiscountModel object containing the necessary information.
     * @return A List of BillingDataCalculatedDiscountModel objects representing the calculated discounts for each sub-period.
     */
    public List<BillingDataCalculatedDiscountModel> calculateDiscounts(BillingDataDiscountModel model) {
        PriceComponent priceComponent = priceComponentRepository
                .findById(model.priceComponentId())
                .orElseThrow(() -> new DomainEntityNotFoundException("price component with id: [%s] not found".formatted(model.priceComponentId())));

        if (!Boolean.TRUE.equals(priceComponent.getDiscount())) {
            return new ArrayList<>();
        }

        List<BillingDataCalculatedDiscountModel> billingDataCalculatedDiscountModels = calculateDiscountsBySubPeriods(model);
        stripTrailingZeroes(billingDataCalculatedDiscountModels);

        return billingDataCalculatedDiscountModels;
    }

    /**
     * Calculates the discounts for each sub-period within the given period based on the provided BillingDataDiscountModel.
     *
     * @param model The BillingDataDiscountModel object containing the necessary information.
     * @return A List of BillingDataCalculatedDiscountModel objects representing the calculated discounts for each sub-period.
     */
    private List<BillingDataCalculatedDiscountModel> calculateDiscountsBySubPeriods(BillingDataDiscountModel model) {
        long modelIdSequence = 1;
        List<BillingDataCalculatedDiscountModel> discounts = new ArrayList<>();

        LocalDate periodFrom = model.periodFrom();
        LocalDate periodTo = model.periodTo();

        List<Discount> activeDiscountsByPointOfDelivery = discountRepository
                .findActiveDiscountByPointOfDeliveryIdAndCustomerId(model.pointOfDeliveryId(), model.customerId());

        List<Discount> discountIntersectionWithPeriod = activeDiscountsByPointOfDelivery
                .stream()
                .filter(discount ->
                        !periodFrom.isAfter(discount.getDateTo()) &&
                                !periodTo.isBefore(discount.getDateFrom()))
                .toList();

        if (discountIntersectionWithPeriod.isEmpty()) {
            return discounts;
        }

        long periodNumberOfDays = periodFrom.until(periodTo, ChronoUnit.DAYS) + 1;
        BigDecimal consumptionPerDay = calculateVolumePerConsumptionDay(model.totalVolume(), periodNumberOfDays);

        for (Discount discount : discountIntersectionWithPeriod) {
            LocalDate calculatedDiscountPeriodFrom = discount.getDateFrom().isBefore(periodFrom) ? periodFrom : discount.getDateFrom();
            LocalDate calculatedDiscountPeriodTo = discount.getDateTo().isAfter(periodTo) ? periodTo : discount.getDateTo();

            BigDecimal totalVolumeConsumptionBetweenDates = calculateTotalVolumeConsumptionBetweenDates(calculatedDiscountPeriodFrom, calculatedDiscountPeriodTo, consumptionPerDay);

            discounts.add(new BillingDataCalculatedDiscountModel(modelIdSequence++, totalVolumeConsumptionBetweenDates, calculatedDiscountPeriodFrom, calculatedDiscountPeriodTo, BigDecimal.ZERO, discount.getAmountInMoneyPerKWH()));
        }

        fillNonDiscountedDateRanges(modelIdSequence, periodFrom, periodTo, model.totalVolume(), consumptionPerDay, discounts);
        roundDiscounts(model, discounts);
        calculateDiscountedAmounts(discounts);

        return discounts;
    }

    private void calculateDiscountedAmounts(List<BillingDataCalculatedDiscountModel> models) {
        models
                .stream()
                .filter(model -> !model.getTotalVolume().equals(BigDecimal.ZERO))
                .filter(model -> !model.getDiscountPerKWH().equals(BigDecimal.ZERO))
                .forEach(model -> model.setDiscountAmount(model.getTotalVolume().multiply(model.getDiscountPerKWH())));
    }

    /**
     * Strips the trailing zeroes from the totalVolume and amount values of each BillingDataCalculatedDiscountModel
     * in the given list.
     *
     * @param models The List of BillingDataCalculatedDiscountModel objects.
     */
    private void stripTrailingZeroes(List<BillingDataCalculatedDiscountModel> models) {
        models.forEach(model -> {
            model.setTotalVolume(model.getTotalVolume().stripTrailingZeros());
            model.setDiscountAmount(model.getDiscountAmount().stripTrailingZeros());
        });
    }

    /**
     * Rounds the discounts for each BillingDataCalculatedDiscountModel in the given list.
     *
     * @param model     The BillingDataDiscountModel object.
     * @param discounts The List of BillingDataCalculatedDiscountModel objects.
     */
    private void roundDiscounts(BillingDataDiscountModel model, List<BillingDataCalculatedDiscountModel> discounts) {
        List<BillingRunRounding.BillingRunRoundingPayload> payloads = new ArrayList<>();

        for (BillingDataCalculatedDiscountModel discount : discounts) {
            payloads.add(new BillingRunRounding.BillingRunRoundingPayload(discount.getModelId(), discount.getTotalVolume(), discount.getPeriodFrom()));
        }

        BillingRunRounding.staticRounding(model.totalVolume(), payloads);

        for (BillingRunRounding.BillingRunRoundingPayload payload : payloads) {
            discounts
                    .stream()
                    .filter(discount -> discount.getModelId().equals(payload.getId()))
                    .forEach(discount -> discount.setTotalVolume(payload.getVolume()));
        }
    }

    /**
     * Calculates the volume per consumption day based on the total volume and number of days.
     *
     * @param totalVolume  The total volume
     * @param numberOfDays The number of days
     * @return The volume per consumption day
     */
    private BigDecimal calculateVolumePerConsumptionDay(BigDecimal totalVolume, Long numberOfDays) {
        return totalVolume.divide(new BigDecimal(numberOfDays), 10, RoundingMode.DOWN);
    }

    /**
     * Calculates the total volume consumption between two dates based on the average consumption per day.
     *
     * @param periodFrom        The start date of the period
     * @param periodTo          The end date of the period
     * @param consumptionPerDay The average consumption per day
     * @return The total volume consumption between the given dates
     */
    private BigDecimal calculateTotalVolumeConsumptionBetweenDates(LocalDate periodFrom, LocalDate periodTo, BigDecimal consumptionPerDay) {
        long periodNumberOfDays = periodFrom.until(periodTo, ChronoUnit.DAYS) + 1;
        return consumptionPerDay.multiply(new BigDecimal(periodNumberOfDays));
    }

    /**
     * Fills in the non-discounted date ranges in the given list of BillingDataCalculatedDiscountModel objects.
     *
     * @param modelIdSequence   The sequence for the model ID of the new BillingDataCalculatedDiscountModel objects
     * @param periodFrom        The start date of the entire period
     * @param periodTo          The end date of the entire period
     * @param totalVolume       The total volume to be distributed across the date ranges
     * @param consumptionPerDay The average consumption per day
     * @param discounts         The list of BillingDataCalculatedDiscountModel objects that already contains the discounted date ranges
     */
    private void fillNonDiscountedDateRanges(long modelIdSequence, LocalDate periodFrom, LocalDate periodTo, BigDecimal totalVolume, BigDecimal consumptionPerDay, List<BillingDataCalculatedDiscountModel> discounts) {
        discounts.sort(Comparator.comparing(BillingDataCalculatedDiscountModel::getPeriodFrom));

        if (discounts.isEmpty()) {
            discounts.add(new BillingDataCalculatedDiscountModel(modelIdSequence++, totalVolume, periodFrom, periodTo, BigDecimal.ZERO, BigDecimal.ZERO));
        } else {
            BillingDataCalculatedDiscountModel firstDiscount = discounts.get(0);
            if (!firstDiscount.getPeriodFrom().equals(periodFrom)) {
                BigDecimal totalVolumeConsumptionBetweenDates = calculateTotalVolumeConsumptionBetweenDates(periodFrom, firstDiscount.getPeriodFrom().minusDays(1), consumptionPerDay);
                discounts.add(new BillingDataCalculatedDiscountModel(modelIdSequence++, totalVolumeConsumptionBetweenDates, periodFrom, firstDiscount.getPeriodFrom().minusDays(1), BigDecimal.ZERO, BigDecimal.ZERO));
            }

            discounts.sort(Comparator.comparing(BillingDataCalculatedDiscountModel::getPeriodFrom));
            List<BillingDataCalculatedDiscountModel> newDiscountsContext = new ArrayList<>();
            for (int i = 0; i < discounts.size() - 1; i++) {
                BillingDataCalculatedDiscountModel currentDiscount = discounts.get(i);
                BillingDataCalculatedDiscountModel nextDiscount = discounts.get(i + 1);
                if (currentDiscount.getPeriodTo().until(nextDiscount.getPeriodFrom(), ChronoUnit.DAYS) > 1) {
                    BigDecimal totalVolumeConsumptionBetweenDates = calculateTotalVolumeConsumptionBetweenDates(currentDiscount.getPeriodTo().plusDays(1), nextDiscount.getPeriodFrom().minusDays(1), consumptionPerDay);
                    newDiscountsContext.add(new BillingDataCalculatedDiscountModel(modelIdSequence++, totalVolumeConsumptionBetweenDates, currentDiscount.getPeriodTo().plusDays(1), nextDiscount.getPeriodFrom().minusDays(1), BigDecimal.ZERO, BigDecimal.ZERO));
                }
            }
            discounts.addAll(newDiscountsContext);
            discounts.sort(Comparator.comparing(BillingDataCalculatedDiscountModel::getPeriodFrom));

            BillingDataCalculatedDiscountModel lastDiscount = discounts.get(discounts.size() - 1);
            if (!periodTo.equals(lastDiscount.getPeriodTo())) {
                BigDecimal totalVolumeConsumptionBetweenDates = calculateTotalVolumeConsumptionBetweenDates(lastDiscount.getPeriodTo().plusDays(1), periodTo, consumptionPerDay);
                discounts.add(new BillingDataCalculatedDiscountModel(modelIdSequence++, totalVolumeConsumptionBetweenDates, lastDiscount.getPeriodTo().plusDays(1), periodTo, BigDecimal.ZERO, BigDecimal.ZERO));
            }
        }
    }
}
