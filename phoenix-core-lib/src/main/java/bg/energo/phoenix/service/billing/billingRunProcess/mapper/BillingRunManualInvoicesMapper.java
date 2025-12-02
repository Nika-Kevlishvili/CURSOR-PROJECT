package bg.energo.phoenix.service.billing.billingRunProcess.mapper;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingDetailedData;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingSummaryData;
import bg.energo.phoenix.model.entity.billing.invoice.*;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BillingRunManualInvoicesMapper {
    private final CurrencyRepository currencyRepository;
    private final VatRateRepository vatRateRepository;

    /**
     * Maps the given BillingSummaryData object to a ManualInvoiceSummaryData object.
     *
     * @param sd                    the BillingSummaryData object to map
     * @param globalVatRateOptional an optional global VAT rate
     * @param defaultVatRate        the default VAT rate
     * @param persistedInvoice      the persisted invoice object
     * @return the mapped ManualInvoiceSummaryData object
     */
    public ManualInvoiceSummaryData mapToManualInvoiceSummaryData(BillingSummaryData sd,
                                                                  Optional<VatRate> globalVatRateOptional,
                                                                  VatRate defaultVatRate,
                                                                  Invoice persistedInvoice) {
        Currency respectiveCurrency = findRespectiveCurrency(sd.getValueCurrencyId());
        VatRate respectiveVatRate = findRespectiveVatRate(sd.getGlobalVatRate(), sd.getVatRateId(), globalVatRateOptional, defaultVatRate);

        return ManualInvoiceSummaryData
                .builder()
                .invoiceId(persistedInvoice.getId())
                .priceComponentOrPriceComponentGroups(sd.getPriceComponentOrPriceComponentGroups())
                .totalVolumes(sd.getTotalVolumes())
                .measuresUnitForTotalVolumes(sd.getMeasuresUnitForTotalVolumes())
                .unitPrice(sd.getUnitPrice())
                .measureForUnitPrice(sd.getMeasureUnitForUnitPrice())
                .value(sd.getValue())
                .valueCurrencyId(respectiveCurrency.getId())
                .valueCurrencyName(respectiveCurrency.getName())
                .incomeAccountNumber(sd.getIncomeAccount())
                .costCenter(sd.getCostCenter())
                .vatRateId(respectiveVatRate.getId())
                .vatRateName(respectiveVatRate.getName())
                .vatRatePercent(respectiveVatRate.getValueInPercent())
                .valueCurrencyId(respectiveCurrency.getId())
                .valueCurrencyName(respectiveCurrency.getName())
                .valueCurrencyExchangeRate(respectiveCurrency.getAltCurrencyExchangeRate())
                .build();
    }

    /**
     * Maps the given BillingSummaryData object to a ManualDebitOrCreditNoteInvoiceSummaryData object.
     *
     * @param sd                    the BillingSummaryData object to map
     * @param globalVatRateOptional an optional global VAT rate
     * @param defaultVatRate        the default VAT rate
     * @param persistedInvoice      the persisted invoice object
     * @return the mapped ManualDebitOrCreditNoteInvoiceSummaryData object
     */
    public ManualDebitOrCreditNoteInvoiceSummaryData mapToManualDebitOrCreditNoteInvoiceSummaryData(BillingSummaryData sd,
                                                                                                    Optional<VatRate> globalVatRateOptional,
                                                                                                    VatRate defaultVatRate,
                                                                                                    Invoice persistedInvoice) {
        Currency respectiveCurrency = findRespectiveCurrency(sd.getValueCurrencyId());
        VatRate respectiveVatRate = findRespectiveVatRate(sd.getGlobalVatRate(), sd.getVatRateId(), globalVatRateOptional, defaultVatRate);

        return ManualDebitOrCreditNoteInvoiceSummaryData
                .builder()
                .invoiceId(persistedInvoice.getId())
                .priceComponentOrPriceComponentGroups(sd.getPriceComponentOrPriceComponentGroups())
                .totalVolumes(sd.getTotalVolumes())
                .measuresUnitForTotalVolumes(sd.getMeasuresUnitForTotalVolumes())
                .unitPrice(sd.getUnitPrice())
                .measureForUnitPrice(sd.getMeasureUnitForUnitPrice())
                .value(sd.getValue())
                .valueCurrencyId(respectiveCurrency.getId())
                .valueCurrencyName(respectiveCurrency.getName())
                .incomeAccountNumber(sd.getIncomeAccount())
                .costCenter(sd.getCostCenter())
                .vatRateId(respectiveVatRate.getId())
                .vatRateName(respectiveVatRate.getName())
                .vatRatePercent(respectiveVatRate.getValueInPercent())
                .valueCurrencyId(respectiveCurrency.getId())
                .valueCurrencyName(respectiveCurrency.getName())
                .valueCurrencyExchangeRate(respectiveCurrency.getAltCurrencyExchangeRate())
                .build();
    }

    /**
     * Maps the given BillingDetailedData object to a ManualInvoiceDetailedData object.
     *
     * @param dd                    the BillingDetailedData object to map
     * @param globalVatRateOptional an optional global VAT rate
     * @param persistedInvoice      the persisted invoice object
     * @return the mapped ManualInvoiceDetailedData object
     */
    public ManualInvoiceDetailedData mapToManualInvoiceDetailedData(BillingDetailedData dd,
                                                                    Optional<VatRate> globalVatRateOptional,
                                                                    Invoice persistedInvoice) {
        Optional<Currency> respectiveCurrencyOptional = Optional.ofNullable(dd.getValueCurrencyId()).isPresent() ? Optional.of(findRespectiveCurrency(dd.getValueCurrencyId())) : Optional.empty();
        Optional<VatRate> respectiveVatRate = findRespectiveVatRate(dd.getGlobalVatRate(), dd.getVatRateId(), globalVatRateOptional);

        return ManualInvoiceDetailedData
                .builder()
                .invoiceId(persistedInvoice.getId())
                .priceComponentOrPriceComponentGroups(dd.getPriceComponent())
                .pod(dd.getPod())
                .periodFrom(dd.getPeriodFrom())
                .periodTo(dd.getPeriodTo())
                .meter(dd.getMeter())
                .newMeterReading(dd.getNewMeterReading())
                .oldMeterReading(dd.getOldMeterReading())
                .differences(dd.getDifferences())
                .multiplier(dd.getMultiplier())
                .correction(dd.getCorrection())
                .deducted(dd.getDeducted())
                .totalVolumes(dd.getTotalVolumes())
                .measuresUnitForTotalVolumes(dd.getMeasuresUnitForTotalVolumes())
                .unitPrice(dd.getUnitPrice())
                .measureForUnitPrice(dd.getMeasureUnitForUnitPrice())
                .value(dd.getValue())
                .valueCurrencyId(respectiveCurrencyOptional.map(Currency::getId).orElse(null))
                .valueCurrencyName(respectiveCurrencyOptional.map(Currency::getName).orElse(null))
                .valueCurrencyExchangeRate(respectiveCurrencyOptional.map(Currency::getAltCurrencyExchangeRate).orElse(null))
                .incomeAccountNumber(dd.getIncomeAccount())
                .costCenter(dd.getCostCenter())
                .vatRateId(respectiveVatRate.map(VatRate::getId).orElse(null))
                .vatRateName(respectiveVatRate.map(VatRate::getName).orElse(null))
                .vatRatePercent(respectiveVatRate.map(VatRate::getValueInPercent).orElse(null))
                .build();
    }

    public ManualDebitOrCreditNoteInvoiceDetailedData mapToManualDebitOrCreditNoteInvoiceDetailedData(BillingDetailedData dd,
                                                                                                      Optional<VatRate> globalVatRateOptional,
                                                                                                      Invoice persistedInvoice) {
        Optional<Currency> respectiveCurrencyOptional = Optional.ofNullable(dd.getValueCurrencyId()).isPresent() ? Optional.of(findRespectiveCurrency(dd.getValueCurrencyId())) : Optional.empty();
        Optional<VatRate> respectiveVatRate = findRespectiveVatRate(dd.getGlobalVatRate(), dd.getVatRateId(), globalVatRateOptional);

        return ManualDebitOrCreditNoteInvoiceDetailedData
                .builder()
                .invoiceId(persistedInvoice.getId())
                .priceComponentOrPriceComponentGroups(dd.getPriceComponent())
                .pod(dd.getPod())
                .periodFrom(dd.getPeriodFrom())
                .periodTo(dd.getPeriodTo())
                .meter(dd.getMeter())
                .newMeterReading(dd.getNewMeterReading())
                .oldMeterReading(dd.getOldMeterReading())
                .differences(dd.getDifferences())
                .multiplier(dd.getMultiplier())
                .correction(dd.getCorrection())
                .deducted(dd.getDeducted())
                .totalVolumes(dd.getTotalVolumes())
                .measuresUnitForTotalVolumes(dd.getMeasuresUnitForTotalVolumes())
                .unitPrice(dd.getUnitPrice())
                .measureForUnitPrice(dd.getMeasureUnitForUnitPrice())
                .value(dd.getValue())
                .valueCurrencyId(respectiveCurrencyOptional.map(Currency::getId).orElse(null))
                .valueCurrencyName(respectiveCurrencyOptional.map(Currency::getName).orElse(null))
                .valueCurrencyExchangeRate(respectiveCurrencyOptional.map(Currency::getAltCurrencyExchangeRate).orElse(null))
                .incomeAccountNumber(dd.getIncomeAccount())
                .costCenter(dd.getCostCenter())
                .vatRateId(respectiveVatRate.map(VatRate::getId).orElse(null))
                .vatRateName(respectiveVatRate.map(VatRate::getName).orElse(null))
                .vatRatePercent(respectiveVatRate.map(VatRate::getValueInPercent).orElse(null))
                .build();
    }

    private Currency findRespectiveCurrency(Long valueCurrencyId) {
        return currencyRepository
                .findById(valueCurrencyId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Currency with id: [%s] not found;".formatted(valueCurrencyId)));
    }

    private VatRate findRespectiveVatRate(Boolean isGlobalVatRate, Long vatRateId, Optional<VatRate> globalVatRateOptional, VatRate defaultVatRate) {
        if (Boolean.TRUE.equals(isGlobalVatRate)) {
            return globalVatRateOptional
                    .orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
        } else {
            if (Objects.nonNull(vatRateId)) {
                return vatRateRepository.findById(vatRateId)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(vatRateId)));
            } else {
                return defaultVatRate;
            }
        }
    }

    private Optional<VatRate> findRespectiveVatRate(Boolean isGlobalVatRate, Long vatRateId, Optional<VatRate> globalVatRateOptional) {
        if (Objects.isNull(vatRateId)) {
            return Optional.empty();
        } else if (Boolean.TRUE.equals(isGlobalVatRate)) {
            if (globalVatRateOptional.isEmpty()) {
                throw new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;");
            }
            return globalVatRateOptional;
        } else {
            Optional<VatRate> vatRateOptional = vatRateRepository.findById(vatRateId);
            if (vatRateOptional.isEmpty()) {
                throw new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(vatRateId));
            }
            return vatRateOptional;
        }
    }
}
