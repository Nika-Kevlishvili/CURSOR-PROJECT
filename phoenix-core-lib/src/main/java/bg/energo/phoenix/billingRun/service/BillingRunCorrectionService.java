package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.billingRun.model.BillingInvoicePaymentTerm;
import bg.energo.phoenix.billingRun.model.entity.BillingRunCorrectionPods;
import bg.energo.phoenix.billingRun.repository.BillingRunCorrectionInvoiceBaseRepository;
import bg.energo.phoenix.billingRun.repository.BillingRunCorrectionPodsRepository;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.invoice.*;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.enums.billing.billings.InvoiceDueDateType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStandardDetailType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.repository.billing.invoice.*;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceDetailedDataAmountModel;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceVatRateResponse;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class BillingRunCorrectionService {

    private final BillingRunRepository billingRunRepository;
    private final BillingRunCorrectionInvoiceBaseRepository billingRunCorrectionInvoiceBaseRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceStandardDetailedDataRepository invoiceStandardDetailedDataRepository;
    private final InvoiceStandardDetailedDataVatBaseRepository invoiceStandardDetailedDataVatBaseRepository;
    private final InvoicePaymentTermsRepository invoicePaymentTermsRepository;
    private final BillingPaymentTermDayCalculationService billingPaymentTermDayCalculationService;
    private final BillingRunCurrencyService billingRunCurrencyService;
    private final InvoiceNumberService invoiceNumberService;
    private final InvoiceVatRateValueRepository invoiceVatRateValueRepository;
    private final BillingRunCorrectionPodsRepository billingRunCorrectionPodsRepository;

    public void reverseInvoices(BillingRun billingRun, Invoice correctionBaseInvoice, Invoice correctionInvoice, List<InvoiceStandardDetailedData> correctionInvoiceDetails, List<InvoiceStandardDetailedDataVatBase> correctionInvoiceVatBaseDetails, ContractType contractType, HashMap<Long, Currency> currencyHashMap, Long mainCurrencyId, Long altCurrencyId, Currency mainCurrency, Long correctionRunId) {

        correctionInvoice.setParentInvoiceId(correctionBaseInvoice.getId());
        invoiceRepository.save(correctionInvoice);

        List<BillingRunCorrectionPods> correctionPodsList = new ArrayList<>();
        if(billingRun.getVolumeChange() && !billingRun.getPriceChange()) {
            correctionPodsList = billingRunCorrectionPodsRepository.findAllByCorrectionRunIdAndRunId(correctionRunId, billingRun.getId());
        }

        List<InvoiceStandardDetailedData> baseDetailedData = invoiceStandardDetailedDataRepository.findAllByInvoiceIdForCorrection(correctionBaseInvoice.getId());
        List<InvoiceStandardDetailedDataVatBase> baseIDetailedDataVatBase = invoiceStandardDetailedDataVatBaseRepository.findAllByInvoiceIdForCorrection(correctionBaseInvoice.getId());

        List<InvoiceStandardDetailedData> newReversalDetailedData = cloneDetails(baseDetailedData, mainCurrencyId, altCurrencyId, currencyHashMap,correctionPodsList,billingRun);
        List<InvoiceStandardDetailedDataVatBase> newReversalDetailedDataVatBase = cloneDetailsVatBase(baseIDetailedDataVatBase, mainCurrencyId, altCurrencyId, currencyHashMap,correctionPodsList,billingRun);
        if(newReversalDetailedData.isEmpty() && newReversalDetailedDataVatBase.isEmpty()) {
            return;
        }
        //START REVERSAL PROCESS FOR BASE/ORIGINAL INVOICE
        Invoice newReversalInvoice = new Invoice();

        newReversalInvoice.setInvoiceDate(billingRun.getInvoiceDate());
        newReversalInvoice.setInvoiceStatus(InvoiceStatus.DRAFT);
        newReversalInvoice.setInvoiceType(InvoiceType.CORRECTION);
        InvoiceDocumentType oldDocumentType = correctionBaseInvoice.getInvoiceDocumentType();
        newReversalInvoice.setInvoiceDocumentType(List.of(InvoiceDocumentType.INVOICE, InvoiceDocumentType.DEBIT_NOTE).contains(oldDocumentType) ? InvoiceDocumentType.CREDIT_NOTE : InvoiceDocumentType.DEBIT_NOTE);
        newReversalInvoice.setTaxEventDate(billingRun.getTaxEventDate());
        BillingInvoicePaymentTerm term;

        if (contractType.equals(ContractType.SERVICE_CONTRACT)) {
            term = invoicePaymentTermsRepository.findByServiceContractDetailId(correctionBaseInvoice.getServiceContractDetailId());
        } else {
            term = invoicePaymentTermsRepository.findByProductContractDetailId(correctionBaseInvoice.getProductContractDetailId());
        }
        if (billingRun.getInvoiceDueDateType().equals(InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT)) {
            if (term != null) {
                LocalDate paymentTermEndDate = billingPaymentTermDayCalculationService.calculateDeadline(term, billingRun.getInvoiceDate(), term.getValue());
                newReversalInvoice.setPaymentDeadline(paymentTermEndDate);
            }
        } else {
            newReversalInvoice.setPaymentDeadline(billingRun.getInvoiceDueDate());
        }
        newReversalInvoice.setParentInvoiceId(correctionBaseInvoice.getId());
        // Copy fields
        newReversalInvoice.setBillingId(billingRun.getId());
        newReversalInvoice.setAccountPeriodId(billingRun.getAccountingPeriodId());
        newReversalInvoice.setReversalCreatedFromId(correctionBaseInvoice.getId());

        newReversalInvoice.setContractType(correctionBaseInvoice.getContractType());
        newReversalInvoice.setMeterReadingPeriodFrom(correctionBaseInvoice.getMeterReadingPeriodFrom());
        newReversalInvoice.setMeterReadingPeriodTo(correctionBaseInvoice.getMeterReadingPeriodTo());
        newReversalInvoice.setBasisForIssuing(correctionBaseInvoice.getBasisForIssuing());
        newReversalInvoice.setIncomeAccountNumber(correctionBaseInvoice.getIncomeAccountNumber());
        newReversalInvoice.setCostCenterControllingOrder(correctionBaseInvoice.getCostCenterControllingOrder());
        newReversalInvoice.setInterestRateId(correctionBaseInvoice.getInterestRateId());
        newReversalInvoice.setDirectDebit(correctionBaseInvoice.getDirectDebit());
        newReversalInvoice.setBankId(correctionBaseInvoice.getBankId());
        newReversalInvoice.setIban(correctionBaseInvoice.getIban());
        newReversalInvoice.setCustomerDetailId(correctionBaseInvoice.getCustomerDetailId());
        newReversalInvoice.setCustomerId(correctionBaseInvoice.getCustomerId());
        newReversalInvoice.setServiceId(correctionBaseInvoice.getServiceId());
        newReversalInvoice.setPodId(correctionBaseInvoice.getPodId());
        newReversalInvoice.setProductId(correctionBaseInvoice.getProductId());
        newReversalInvoice.setCustomerCommunicationId(correctionBaseInvoice.getCustomerCommunicationId());
        newReversalInvoice.setContractCommunicationId(correctionBaseInvoice.getContractCommunicationId());
        newReversalInvoice.setContractBillingGroupId(correctionBaseInvoice.getContractBillingGroupId());
        newReversalInvoice.setProductContractId(correctionBaseInvoice.getProductContractId());
        newReversalInvoice.setProductContractDetailId(correctionBaseInvoice.getProductContractDetailId());
        newReversalInvoice.setServiceContractId(correctionBaseInvoice.getServiceContractId());
        newReversalInvoice.setServiceContractDetailId(correctionBaseInvoice.getServiceContractDetailId());
        newReversalInvoice.setInvoiceDocumentId(correctionBaseInvoice.getInvoiceDocumentId());
        newReversalInvoice.setCurrencyId(mainCurrencyId);
        newReversalInvoice.setCurrencyIdInOtherCurrency(altCurrencyId);
        newReversalInvoice.setProductDetailId(correctionBaseInvoice.getProductDetailId());
        newReversalInvoice.setServiceDetailId(correctionBaseInvoice.getServiceDetailId());
        newReversalInvoice.setAlternativeRecipientCustomerDetailId(correctionBaseInvoice.getAlternativeRecipientCustomerDetailId());
        newReversalInvoice.setNoInterestOnOverdueDebts(correctionBaseInvoice.getNoInterestOnOverdueDebts());
        newReversalInvoice.setInvoiceSlot(correctionBaseInvoice.getInvoiceSlot());
        newReversalInvoice.setTemplateDetailId(correctionBaseInvoice.getTemplateDetailId());
        newReversalInvoice.setHasOneTimeOnTimeInvoiceDetail(correctionBaseInvoice.getHasOneTimeOnTimeInvoiceDetail());
        newReversalInvoice.setHasWithElectricityInvoiceDetail(correctionBaseInvoice.getHasWithElectricityInvoiceDetail());
        newReversalInvoice.setPriceWasChanged(correctionBaseInvoice.getPriceWasChanged());


        List<InvoiceDetailedDataAmountModel> amountModels = newReversalDetailedData
                .stream()
                .map(model -> new InvoiceDetailedDataAmountModel(
                        model.getVatRatePercent(),
                        model.getMainCurrencyTotalAmountWithoutVat(),
                        Boolean.TRUE,
                        newReversalInvoice.getCurrencyExchangeRateOnInvoiceCreation()
                )).toList();
        List<InvoiceVatRateValue> uncommittedInvoiceVatRates = new ArrayList<>();
        List<InvoiceVatRateResponse> invoiceVatRateResponses = groupByVatRates(amountModels);

        if (!invoiceVatRateResponses.isEmpty()) {
            uncommittedInvoiceVatRates.addAll(mapVatRates(newReversalInvoice.getId(), invoiceVatRateResponses));
        }
        BigDecimal totalMainCurrencyWithVat = EPBDecimalUtils
                .calculateSummary(uncommittedInvoiceVatRates
                        .stream()
                        .map(InvoiceVatRateValue::getValueOfVat)
                        .toList()
                ).setScale(2, RoundingMode.HALF_UP)
                .add(EPBDecimalUtils.calculateSummary(uncommittedInvoiceVatRates
                        .stream()
                        .map(InvoiceVatRateValue::getAmountExcludingVat)
                        .toList()
                ).setScale(2, RoundingMode.HALF_UP));

        BigDecimal totalMainCurrencyWithoutVat = EPBDecimalUtils
                .calculateSummary(uncommittedInvoiceVatRates
                        .stream()
                        .map(InvoiceVatRateValue::getAmountExcludingVat)
                        .toList()
                ).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalAltCurrencyWithVat =
                billingRunCurrencyService.convertToCurrency(mainCurrencyId, altCurrencyId, EPBDecimalUtils
                                .calculateSummary(uncommittedInvoiceVatRates
                                        .stream()
                                        .map(InvoiceVatRateValue::getValueOfVat)
                                        .toList()
                                ).setScale(2, RoundingMode.HALF_UP).add(
                                        EPBDecimalUtils
                                                .calculateSummary(uncommittedInvoiceVatRates
                                                        .stream()
                                                        .map(InvoiceVatRateValue::getAmountExcludingVat)
                                                        .toList()
                                                ).setScale(2, RoundingMode.HALF_UP)
                                ), currencyHashMap)
                        .setScale(2, RoundingMode.HALF_UP);


        newReversalInvoice.setTotalAmountOfVat(totalMainCurrencyWithVat
                .subtract(totalMainCurrencyWithoutVat));

        newReversalInvoice.setTotalAmountExcludingVat(totalMainCurrencyWithoutVat);
        newReversalInvoice.setTotalAmountIncludingVat(totalMainCurrencyWithVat);
        newReversalInvoice.setCurrencyId(mainCurrencyId);
        newReversalInvoice.setCurrencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate());
        newReversalInvoice.setCurrencyIdInOtherCurrency(altCurrencyId);

        newReversalInvoice.setTotalAmountIncludingVatInOtherCurrency(totalAltCurrencyWithVat);

        invoiceRepository.saveAndFlush(newReversalInvoice);
        newReversalDetailedData.forEach(i -> i.setInvoice(newReversalInvoice));
        newReversalDetailedDataVatBase.forEach(v -> v.setInvoice(newReversalInvoice));

        uncommittedInvoiceVatRates.forEach(u -> u.setInvoiceId(newReversalInvoice.getId()));

        invoiceVatRateValueRepository.saveAll(uncommittedInvoiceVatRates);

        invoiceStandardDetailedDataRepository.saveAll(newReversalDetailedData);
        invoiceStandardDetailedDataVatBaseRepository.saveAll(newReversalDetailedDataVatBase);


        invoiceNumberService.fillInvoiceNumber(newReversalInvoice);

    }

    private List<InvoiceStandardDetailedData> cloneDetails( List<InvoiceStandardDetailedData> baseDetailedData, Long mainCurrencyId, Long altCurrencyId, HashMap<Long, Currency> currencyHashMap,
                                                            List<BillingRunCorrectionPods> correctionPodsList,
                                                            BillingRun billingRun) {
        List<InvoiceStandardDetailedData> clonedList = new ArrayList<>();
        for (InvoiceStandardDetailedData baseDetail : baseDetailedData) {
            if(billingRun.getVolumeChange() && !billingRun.getPriceChange()){
                if(correctionPodsList.stream().noneMatch(f->f.getFullReversalNeeded()
                        && f.getPodId().equals(baseDetail.getPodId())
                        && (
                        baseDetail.getDetailType().equals(InvoiceStandardDetailType.SCALE)
                        || baseDetail.getDetailType().equals(InvoiceStandardDetailType.SETTLEMENT)
                        ||  baseDetail.getDetailType().equals(InvoiceStandardDetailType.DISCOUNT)
                        )
                )) {
                    continue;
                }
            }

            InvoiceStandardDetailedData data = new InvoiceStandardDetailedData();
            // Copy fields
            data.setDetailType(baseDetail.getDetailType());
            data.setPcId(baseDetail.getPcId());
            data.setPcGroupDetailId(baseDetail.getPcGroupDetailId());
            data.setPodId(baseDetail.getPodId());
            data.setPodDetailId(baseDetail.getPodDetailId());
            data.setDateFrom(baseDetail.getDateFrom());
            data.setDateTo(baseDetail.getDateTo());
            data.setCustomerDetailId(baseDetail.getCustomerDetailId());
            data.setProductContractDetailId(baseDetail.getProductContractDetailId());
            data.setServiceContractDetailId(baseDetail.getServiceContractDetailId());
            data.setServiceDetailId(baseDetail.getServiceDetailId());
            data.setProductDetailId(baseDetail.getProductDetailId());
            data.setTotalVolumes(baseDetail.getTotalVolumes());
            data.setUnitPrice(baseDetail.getUnitPrice());
            data.setMainCurrencyTotalAmountWithoutVat(billingRunCurrencyService.convertToCurrency(baseDetail.getMainCurrencyId(),mainCurrencyId,baseDetail.getMainCurrencyTotalAmountWithoutVat(),currencyHashMap));
            data.setMainCurrencyTotalAmountWithVat(billingRunCurrencyService.convertToCurrency(baseDetail.getMainCurrencyId(),mainCurrencyId,baseDetail.getMainCurrencyTotalAmountWithVat(),currencyHashMap));
            data.setMainCurrencyTotalAmountVat(billingRunCurrencyService.convertToCurrency(baseDetail.getMainCurrencyId(),mainCurrencyId,baseDetail.getMainCurrencyTotalAmountVat(),currencyHashMap));
            data.setMainCurrencyId(mainCurrencyId);
            data.setAltCurrencyTotalAmountWithoutVat(billingRunCurrencyService.convertToCurrency(baseDetail.getMainCurrencyId(),altCurrencyId,baseDetail.getMainCurrencyTotalAmountWithoutVat(),currencyHashMap));
            data.setAltCurrencyTotalAmountWithVat(billingRunCurrencyService.convertToCurrency(baseDetail.getMainCurrencyId(),altCurrencyId,baseDetail.getMainCurrencyTotalAmountWithVat(),currencyHashMap));
            data.setAltCurrencyTotalAmountVat(billingRunCurrencyService.convertToCurrency(baseDetail.getMainCurrencyId(),altCurrencyId,baseDetail.getMainCurrencyTotalAmountVat(),currencyHashMap));
            data.setAltCurrencyId(altCurrencyId);
            data.setOriginalCurrencyTotalAmountWithoutVat(baseDetail.getOriginalCurrencyTotalAmountWithoutVat());
            data.setOriginalCurrencyTotalAmountWithVat(baseDetail.getOriginalCurrencyTotalAmountWithVat());
            data.setOriginalCurrencyTotalAmountVat(baseDetail.getOriginalCurrencyTotalAmountVat());
            data.setOriginalCurrencyId(baseDetail.getOriginalCurrencyId());
            data.setVatRateId(baseDetail.getVatRateId());
            data.setVatRatePercent(baseDetail.getVatRatePercent());
            data.setMeterId(baseDetail.getMeterId());
            data.setNewMeterReading(baseDetail.getNewMeterReading());
            data.setOldMeterReading(baseDetail.getOldMeterReading());
            data.setDifference(baseDetail.getDifference());
            data.setMultiplier(baseDetail.getMultiplier());
            data.setCorrection(baseDetail.getCorrection());
            data.setDeducted(baseDetail.getDeducted());
            data.setMeasuresUnitForTotalVolumes(baseDetail.getMeasuresUnitForTotalVolumes());
            data.setMeasureUnitForUnitPrice(baseDetail.getMeasureUnitForUnitPrice());
            data.setIncomeAccountNumber(baseDetail.getIncomeAccountNumber());
            data.setCostCenterControllingOrder(baseDetail.getCostCenterControllingOrder());
            data.setInterimDeductionInvoiceId(baseDetail.getInterimDeductionInvoiceId());
            data.setTariff(baseDetail.getTariff());
            data.setInterimId(baseDetail.getInterimId());
            data.setDiscountId(baseDetail.getDiscountId());
            data.setBillingDataProfileIds(baseDetail.getBillingDataProfileIds() != null ? baseDetail.getBillingDataProfileIds().clone() : null);
            data.setBillingDataScaleIds(baseDetail.getBillingDataScaleIds() != null ? baseDetail.getBillingDataScaleIds().clone() : null);
            data.setPriceComponentPriceTypeId(baseDetail.getPriceComponentPriceTypeId());
            data.setRestricted(baseDetail.getRestricted());
            data.setDiscounted(baseDetail.getDiscounted());
            data.setUnrecognizedPod(baseDetail.getUnrecognizedPod());
            data.setScaleId(baseDetail.getScaleId());
            // Add cloned object to list
            clonedList.add(data);
        }

        return clonedList;

     }
    private List<InvoiceStandardDetailedDataVatBase> cloneDetailsVatBase(List<InvoiceStandardDetailedDataVatBase> baseDetailedData, Long mainCurrencyId, Long altCurrencyId, HashMap<Long, Currency> currencyHashMap,
                                                                         List<BillingRunCorrectionPods> correctionPodsList,
                                                                         BillingRun billingRun) {
        List<InvoiceStandardDetailedDataVatBase> clonedList = new ArrayList<>();

        for (InvoiceStandardDetailedDataVatBase baseDetail : baseDetailedData) {
            if(billingRun.getVolumeChange() && !billingRun.getPriceChange()){
                if(correctionPodsList.stream().noneMatch(f->f.getFullReversalNeeded()
                                && f.getPodId().equals(baseDetail.getPodId())
                                && (
                                baseDetail.getDetailType().equals(InvoiceStandardDetailType.SCALE)
                                        || baseDetail.getDetailType().equals(InvoiceStandardDetailType.SETTLEMENT)
                                        ||  baseDetail.getDetailType().equals(InvoiceStandardDetailType.DISCOUNT)
                        )
                )) {
                    continue;
                }
            }

            InvoiceStandardDetailedDataVatBase data = new InvoiceStandardDetailedDataVatBase();
            // Copy fields
            data.setDetailType(baseDetail.getDetailType());
            data.setPcId(baseDetail.getPcId());
            data.setPcGroupDetailId(baseDetail.getPcGroupDetailId());
            data.setPodId(baseDetail.getPodId());
            data.setDateFrom(baseDetail.getDateFrom());
            data.setDateTo(baseDetail.getDateTo());
            data.setCustomerDetailId(baseDetail.getCustomerDetailId());
            data.setProductContractDetailId(baseDetail.getProductContractDetailId());
            data.setServiceContractDetailId(baseDetail.getServiceContractDetailId());
            data.setServiceDetailId(baseDetail.getServiceDetailId());
            data.setProductDetailId(baseDetail.getProductDetailId());
            data.setTotalVolumes(baseDetail.getTotalVolumes());
            data.setUnitPrice(baseDetail.getUnitPrice());
            data.setMainCurrencyTotalAmountWithoutVat(billingRunCurrencyService.convertToCurrency(baseDetail.getMainCurrencyId(),mainCurrencyId,baseDetail.getMainCurrencyTotalAmountWithoutVat(),currencyHashMap));
            data.setMainCurrencyTotalAmountWithVat(billingRunCurrencyService.convertToCurrency(baseDetail.getMainCurrencyId(),mainCurrencyId,baseDetail.getMainCurrencyTotalAmountWithVat(),currencyHashMap));
            data.setMainCurrencyTotalAmountVat(billingRunCurrencyService.convertToCurrency(baseDetail.getMainCurrencyId(),mainCurrencyId,baseDetail.getMainCurrencyTotalAmountVat(),currencyHashMap));
            data.setMainCurrencyId(mainCurrencyId);
            data.setAltCurrencyTotalAmountWithoutVat(billingRunCurrencyService.convertToCurrency(baseDetail.getMainCurrencyId(),altCurrencyId,baseDetail.getMainCurrencyTotalAmountWithoutVat(),currencyHashMap));
            data.setAltCurrencyTotalAmountWithVat(billingRunCurrencyService.convertToCurrency(baseDetail.getMainCurrencyId(),altCurrencyId,baseDetail.getMainCurrencyTotalAmountWithVat(),currencyHashMap));
            data.setAltCurrencyTotalAmountVat(billingRunCurrencyService.convertToCurrency(baseDetail.getMainCurrencyId(),altCurrencyId,baseDetail.getMainCurrencyTotalAmountVat(),currencyHashMap));
            data.setAltCurrencyId(altCurrencyId);
            data.setOriginalCurrencyTotalAmountWithoutVat(baseDetail.getOriginalCurrencyTotalAmountWithoutVat());
            data.setOriginalCurrencyTotalAmountWithVat(baseDetail.getOriginalCurrencyTotalAmountWithVat());
            data.setOriginalCurrencyTotalAmountVat(baseDetail.getOriginalCurrencyTotalAmountVat());
            data.setOriginalCurrencyId(baseDetail.getOriginalCurrencyId());
            data.setVatRateId(baseDetail.getVatRateId());
            data.setVatRatePercent(baseDetail.getVatRatePercent());
            data.setMeterId(baseDetail.getMeterId());
            data.setNewMeterReading(baseDetail.getNewMeterReading());
            data.setOldMeterReading(baseDetail.getOldMeterReading());
            data.setDifference(baseDetail.getDifference());
            data.setMultiplier(baseDetail.getMultiplier());
            data.setCorrection(baseDetail.getCorrection());
            data.setDeducted(baseDetail.getDeducted());
            data.setMeasuresUnitForTotalVolumes(baseDetail.getMeasuresUnitForTotalVolumes());
            data.setMeasureUnitForUnitPrice(baseDetail.getMeasureUnitForUnitPrice());
            data.setIncomeAccountNumber(baseDetail.getIncomeAccountNumber());
            data.setCostCenterControllingOrder(baseDetail.getCostCenterControllingOrder());
            data.setTariff(baseDetail.getTariff());
            data.setDiscountId(baseDetail.getDiscountId());
            data.setBillingDataProfileIds(baseDetail.getBillingDataProfileIds() != null ? baseDetail.getBillingDataProfileIds().clone() : null);
            data.setBillingDataScaleIds(baseDetail.getBillingDataScaleIds() != null ? baseDetail.getBillingDataScaleIds().clone() : null);
            data.setPriceComponentPriceTypeId(baseDetail.getPriceComponentPriceTypeId());
            data.setRestricted(baseDetail.getRestricted());
            data.setDiscounted(baseDetail.getDiscounted());
            data.setUnrecognizedPod(baseDetail.getUnrecognizedPod());
            data.setScaleId(baseDetail.getScaleId());
            // Add cloned object to list
            clonedList.add(data);
        }

        return clonedList;

    }

    private List<InvoiceVatRateValue> mapVatRates(Long invoiceId, List<InvoiceVatRateResponse> invoiceVatRateResponses) {
        return invoiceVatRateResponses
                .stream()
                .map(vat -> InvoiceVatRateValue
                        .builder()
                        .invoiceId(invoiceId)
                        .valueOfVat(vat.valueOfVat())
                        .amountExcludingVat(vat.amountExcludingVat())
                        .vatRatePercent(vat.vatRatePercent())
                        .build()).toList();
    }

    public List<InvoiceVatRateResponse> groupByVatRates(List<InvoiceDetailedDataAmountModel> models) {
        if (CollectionUtils.isEmpty(models)) {
            return new ArrayList<>();
        }

        List<InvoiceVatRateResponse> context = new ArrayList<>();

        Map<BigDecimal, List<InvoiceDetailedDataAmountModel>> groupedByVatRate = models
                .stream()
                .map(model -> new InvoiceDetailedDataAmountModel(Objects.requireNonNullElse(model.vatRatePercent(), BigDecimal.ZERO), model.pureAmount(), model.isMainCurrency(), model.alternativeCurrencyExchangeRate()))
                .collect(Collectors.groupingBy(InvoiceDetailedDataAmountModel::vatRatePercent));

        for (Map.Entry<BigDecimal, List<InvoiceDetailedDataAmountModel>> entry : groupedByVatRate.entrySet()) {
            BigDecimal vatRatePercent = entry.getKey();
            List<InvoiceDetailedDataAmountModel> amountModels = entry.getValue();
            BigDecimal totalAmount = EPBDecimalUtils
                    .calculateSummary(amountModels
                            .stream()
                            .map(InvoiceDetailedDataAmountModel::pureAmount)
                            .toList()
                    ).setScale(2, RoundingMode.HALF_UP);
            context.add(new InvoiceVatRateResponse(vatRatePercent, totalAmount));
        }

        return context;
    }
}
