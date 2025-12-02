package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.billingRun.model.BillingInvoicePaymentTerm;
import bg.energo.phoenix.billingRun.model.StandardInvoiceForInterim;
import bg.energo.phoenix.billingRun.model.entity.BillingRunInterimData;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceStandardDetailedData;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceVatRateValue;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentTerms;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.enums.billing.billings.InvoiceDueDateType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStandardDetailType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.product.product.EntityType;
import bg.energo.phoenix.model.response.billing.billingRun.BillingRunStandardProcessModel;
import bg.energo.phoenix.model.response.priceParameter.PriceParameterForCalculationResponse;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceStandardDetailedDataRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceVatRateValueRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentTermsRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentFormulaVariableRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterRepository;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceDetailedDataAmountModel;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceVatRateResponse;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ognl.OgnlException;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class BillingRunInterimProcessingService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceStandardDetailedDataRepository invoiceStandardDetailedDataRepository;
    private final BillingRunCurrencyService billingRunCurrencyService;
    private final VatRateRepository vatRateRepository;
    private final PriceParameterRepository priceParameterRepository;
    private final PriceComponentRepository priceComponentRepository;
    private final BillingRunPriceComponentEvaluationService priceComponentEvaluationService;
    private final PriceComponentFormulaVariableRepository priceComponentFormulaVariableRepository;

    private final InterimAdvancePaymentTermsRepository interimAdvancePaymentTermsRepository;
    private final BillingPaymentTermDayCalculationService billingPaymentTermDayCalculationService;
    private final InvoicePaymentTermsRepository invoicePaymentTermsRepository;
    private final InvoiceVatRateValueRepository invoiceVatRateValueRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final InvoiceNumberService invoiceNumberService;

    @Transactional
    public Long process(BillingRunInterimData interimData, Currency mainCurrency,
                        VatRate globalVatRate, HashMap<Long, VatRate> vatRateMap,
                        HashMap<Long, Currency> currencyHashMap, Long mainCurrencyId,
                        Long altCurrencyId, LocalDate invoiceIssueDate,
                        BillingRunStandardProcessModel billingRunModel) throws OgnlException {

        VatRate vatRate;
        if (interimData.getGlobalVatRate()) {
            vatRate = globalVatRate;
        } else {
            vatRate = vatRateMap.get(interimData.getVatRateId());
            if (vatRate == null) {
                vatRate = vatRateRepository.findById(interimData.getVatRateId()).get();
                vatRateMap.put(vatRate.getId(), vatRate);
            }
        }

        List<InvoiceStandardDetailedData> standardInvoiceDetails = new ArrayList<>();
        Invoice invoice = new Invoice();
        invoice.setContractType(interimData.getContractType());
        invoice.setProcessorRowId(interimData.getId());
        invoice.setInvoiceDate(billingRunModel.getInvoiceDate());
        invoice.setTaxEventDate(billingRunModel.getTaxEventDate());
        invoice.setInterestRateId(Boolean.TRUE.equals(interimData.getNoInterestOnOverdueDebt()) ? null : interimData.getInterestRateId());
        invoice.setBasisForIssuing("Междинно/авансово плащане за периода"); // hardcoded value for interim advance payment invoices
        invoice.setAccountPeriodId(billingRunModel.getAccountPeriodId());
        invoice.setDirectDebit(interimData.getDirectDebit());
        invoice.setBankId(interimData.getBankId());
        invoice.setCustomerId(interimData.getCustomerId());
        invoice.setIban(interimData.getIban());
        invoice.setCustomerDetailId(interimData.getCustomerDetailId());
        invoice.setCostCenterControllingOrder(interimData.getCostCenterControllingOrder());
        invoice.setIncomeAccountNumber(interimData.getNumberOfIncomeAccount());
        invoice.setAlternativeRecipientCustomerDetailId(interimData.getReceiptOfAnInvoiceNumber());
        invoice.setCustomerCommunicationId(interimData.getCustomerCommunicationId());
        invoice.setContractCommunicationId(interimData.getContractCommunicationId());
        invoice.setProductContractId(interimData.getContractType().equals(ContractType.PRODUCT_CONTRACT) ? interimData.getContractId() : null);
        invoice.setProductContractDetailId(interimData.getContractType().equals(ContractType.PRODUCT_CONTRACT) ? interimData.getContractDetailId() : null);
        invoice.setServiceContractId(interimData.getContractType().equals(ContractType.SERVICE_CONTRACT) ? interimData.getContractId() : null);
        invoice.setServiceContractDetailId(interimData.getContractType().equals(ContractType.SERVICE_CONTRACT) ? interimData.getContractDetailId() : null);
        invoice.setServiceDetailId(interimData.getContractType().equals(ContractType.SERVICE_CONTRACT) ? interimData.getProductDetailId() : null);
        invoice.setProductDetailId(interimData.getContractType().equals(ContractType.PRODUCT_CONTRACT) ? interimData.getProductDetailId() : null);
        invoice.setInvoiceStatus(InvoiceStatus.DRAFT);
        invoice.setInvoiceType(InvoiceType.INTERIM_AND_ADVANCE_PAYMENT);
        invoice.setInvoiceDocumentType(InvoiceDocumentType.INVOICE);

        invoice.setServiceId(interimData.getContractType().equals(ContractType.SERVICE_CONTRACT) ? interimData.getProductId() : null);
        invoice.setProductId(interimData.getContractType().equals(ContractType.PRODUCT_CONTRACT) ? interimData.getProductId() : null);
        invoice.setProductDetailId(interimData.getContractType().equals(ContractType.PRODUCT_CONTRACT) ? interimData.getProductDetailId() : null);
        invoice.setServiceDetailId(interimData.getContractType().equals(ContractType.SERVICE_CONTRACT) ? interimData.getProductDetailId() : null);
        invoice.setContractBillingGroupId(interimData.getBillingGroupId());
        invoice.setPodId(interimData.getPodId());
        invoice.setProductId(interimData.getProductId());
        invoice.setInvoiceSlot(interimData.getIssuedSeparateInvoice());
        invoice.setBillingId(interimData.getRunId());
        invoice.setIsDeducted(false);

        Long templateDetailId = contractTemplateRepository
                .findTemplateDetailForBilling(
                        interimData.getContractType().equals(ContractType.SERVICE_CONTRACT) ? EntityType.SERVICE.name() : EntityType.PRODUCT.name(), interimData.getProductDetailId(), interimData.getRunId())
                .orElse(null);

        invoice.setTemplateDetailId(templateDetailId);
        BigDecimal totalMainCurrencyWithoutVat = BigDecimal.ZERO;
        BigDecimal totalMainCurrencyWithVat = BigDecimal.ZERO;

        BigDecimal totalAltCurrencyWithoutVat = BigDecimal.ZERO;
        BigDecimal totalAltCurrencyWithVat = BigDecimal.ZERO;

        if (interimData.getValueType().equals("EXACT_AMOUNT")) {
            totalMainCurrencyWithoutVat = billingRunCurrencyService.convertToCurrency(interimData.getCurrencyId(), mainCurrencyId, interimData.getCalculationValue(), currencyHashMap);
            totalAltCurrencyWithoutVat = billingRunCurrencyService.convertToCurrency(interimData.getCurrencyId(), altCurrencyId, interimData.getCalculationValue(), currencyHashMap);
            BigDecimal totalOriginalCurrencyWithoutVat = interimData.getCalculationValue();
            BigDecimal vatOriginalCurrency = totalOriginalCurrencyWithoutVat
                    .multiply(vatRate.getValueInPercent())
                    .setScale(12, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(100L), 12, RoundingMode.HALF_UP);

            BigDecimal vatMainCurrency = totalMainCurrencyWithoutVat
                    .multiply(vatRate.getValueInPercent())
                    .setScale(12, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(100L), 12, RoundingMode.HALF_UP);

            BigDecimal vatAltCurrency = totalAltCurrencyWithoutVat
                    .multiply(vatRate.getValueInPercent())
                    .setScale(12, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(100L), 12, RoundingMode.HALF_UP);

            totalMainCurrencyWithVat = totalMainCurrencyWithoutVat.add(vatMainCurrency);
            totalAltCurrencyWithVat = totalAltCurrencyWithoutVat.add(vatAltCurrency);

            InvoiceStandardDetailedData detailedData = new InvoiceStandardDetailedData();
            detailedData.setInvoice(invoice);
            detailedData.setAltCurrencyId(altCurrencyId);
            detailedData.setMainCurrencyId(mainCurrencyId);
            detailedData.setMainCurrencyTotalAmountVat(vatMainCurrency);
            detailedData.setAltCurrencyTotalAmountVat(vatAltCurrency);
            detailedData.setAltCurrencyTotalAmountWithoutVat(totalAltCurrencyWithoutVat);
            detailedData.setMainCurrencyTotalAmountWithoutVat(totalMainCurrencyWithoutVat);
            detailedData.setMainCurrencyTotalAmountWithVat(totalMainCurrencyWithVat);
            detailedData.setAltCurrencyTotalAmountWithVat(totalAltCurrencyWithVat);
            detailedData.setProductContractDetailId(interimData.getContractType().equals(ContractType.PRODUCT_CONTRACT) ? interimData.getContractDetailId() : null);
            detailedData.setServiceContractDetailId(interimData.getContractType().equals(ContractType.SERVICE_CONTRACT) ? interimData.getContractDetailId() : null);
            detailedData.setProductDetailId(interimData.getContractType().equals(ContractType.PRODUCT_CONTRACT) ? interimData.getProductDetailId() : null);
            detailedData.setServiceDetailId(interimData.getContractType().equals(ContractType.SERVICE_CONTRACT) ? interimData.getProductDetailId() : null);
            detailedData.setOriginalCurrencyId(interimData.getCurrencyId());
            detailedData.setOriginalCurrencyTotalAmountWithVat(totalOriginalCurrencyWithoutVat.add(vatOriginalCurrency));
            detailedData.setOriginalCurrencyTotalAmountVat(vatOriginalCurrency);
            detailedData.setOriginalCurrencyTotalAmountWithoutVat(totalOriginalCurrencyWithoutVat);
            detailedData.setVatRateId(vatRate.getId());
            detailedData.setVatRatePercent(vatRate.getValueInPercent());
            detailedData.setDetailType(InvoiceStandardDetailType.INTERIM_EXACT_AMOUNT);
            detailedData.setCostCenterControllingOrder(interimData.getCostCenterControllingOrder());
            detailedData.setIncomeAccountNumber(interimData.getNumberOfIncomeAccount());
            detailedData.setInterimId(interimData.getInterimId());
            standardInvoiceDetails.add(detailedData);
        } else if (interimData.getValueType().equals("PRICE_COMPONENT")) {

            if (interimData.getPriceFormula().contains("$PRICE_PROFILE$")) {
                throw new RuntimeException("PRICE_PROFILE was used in price component cannot calculate");
            }

            String regex = "\\$(\\d+)\\$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(interimData.getPriceFormula());
            List<Long> matches = new ArrayList<>();
            while (matcher.find()) {
                matches.add(Long.valueOf(matcher.group(1)));
            }

            Map<String, Object> variablesContext = new HashMap<>();
            if (matches.size() > 0) {
                List<PriceParameterForCalculationResponse> priceParamsFilled = priceParameterRepository.isPreviousThirtyDaysFilled(matches);

                if (priceParamsFilled.stream().anyMatch(p -> !p.getPreviousThirtyDaysFilled())) {
                    throw new RuntimeException("Price wasn't calculated because of missing data");
                }

                for (PriceParameterForCalculationResponse price : priceParamsFilled) {
                    variablesContext.put("$" + price.getId() + "$", price.getTotalPrice()
                            .divide(
                                    BigDecimal.valueOf(price.getTotalPeriodsWithinRange()), 12, RoundingMode.HALF_UP));
                }
            }
            List<PriceComponentFormulaVariable> priceComponentFormulaVariables = priceComponentFormulaVariableRepository.findAllByPriceComponentId(interimData.getPriceComponentId());
            if (priceComponentFormulaVariables.stream().anyMatch(p -> p.getValue() == null || p.getValueFrom() != null || p.getValueTo() != null)) {
                throw new RuntimeException("Invalid price component x1-x10 configuration");
            }

            for (PriceComponentFormulaVariable x : priceComponentFormulaVariables) {
                variablesContext.put("$" + x.getVariable().name() + "$", x.getValue());
            }

            PriceComponent priceComponent = priceComponentRepository.findById(interimData.getPriceComponentId()).get();

            BigDecimal price = priceComponentEvaluationService.evaluateExpression(interimData.getPriceFormula(), variablesContext);

            price = billingRunCurrencyService.convertToCurrency(priceComponent.getCurrencyId(), mainCurrencyId, price, currencyHashMap);

            totalMainCurrencyWithoutVat = billingRunCurrencyService.convertToCurrency(mainCurrencyId, mainCurrencyId, price, currencyHashMap);
            totalAltCurrencyWithoutVat = billingRunCurrencyService.convertToCurrency(mainCurrencyId, altCurrencyId, price, currencyHashMap);
            BigDecimal totalOriginalCurrencyWithoutVat = price;
            BigDecimal vatOriginalCurrency = totalOriginalCurrencyWithoutVat
                    .multiply(vatRate.getValueInPercent())
                    .setScale(12, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(100L), 12, RoundingMode.HALF_UP);

            BigDecimal vatMainCurrency = totalMainCurrencyWithoutVat
                    .multiply(vatRate.getValueInPercent())
                    .setScale(12, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(100L), 12, RoundingMode.HALF_UP);
            BigDecimal vatAltCurrency = totalAltCurrencyWithoutVat
                    .multiply(vatRate.getValueInPercent())
                    .setScale(12, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(100L), 12, RoundingMode.HALF_UP);

            totalMainCurrencyWithVat = totalMainCurrencyWithoutVat.add(vatMainCurrency);
            totalAltCurrencyWithVat = totalAltCurrencyWithoutVat.add(vatAltCurrency);

            InvoiceStandardDetailedData detailedData = new InvoiceStandardDetailedData();
            detailedData.setPcId(interimData.getPriceComponentId());
            detailedData.setInvoice(invoice);
            detailedData.setInterimId(interimData.getInterimId());
            detailedData.setAltCurrencyId(altCurrencyId);
            detailedData.setMainCurrencyId(mainCurrencyId);
            detailedData.setMainCurrencyTotalAmountVat(vatMainCurrency);
            detailedData.setAltCurrencyTotalAmountVat(vatAltCurrency);
            detailedData.setAltCurrencyTotalAmountWithoutVat(totalAltCurrencyWithoutVat);
            detailedData.setMainCurrencyTotalAmountWithoutVat(totalMainCurrencyWithoutVat);
            detailedData.setMainCurrencyTotalAmountWithVat(totalMainCurrencyWithVat);
            detailedData.setAltCurrencyTotalAmountWithVat(totalAltCurrencyWithVat);
            detailedData.setProductContractDetailId(interimData.getContractType().equals(ContractType.PRODUCT_CONTRACT) ? interimData.getContractDetailId() : null);
            detailedData.setServiceContractDetailId(interimData.getContractType().equals(ContractType.SERVICE_CONTRACT) ? interimData.getContractDetailId() : null);
            detailedData.setProductDetailId(interimData.getContractType().equals(ContractType.PRODUCT_CONTRACT) ? interimData.getProductDetailId() : null);
            detailedData.setServiceDetailId(interimData.getContractType().equals(ContractType.SERVICE_CONTRACT) ? interimData.getProductDetailId() : null);
            detailedData.setOriginalCurrencyId(mainCurrencyId);
            detailedData.setOriginalCurrencyTotalAmountWithVat(totalOriginalCurrencyWithoutVat
                    .add(vatOriginalCurrency));
            detailedData.setOriginalCurrencyTotalAmountVat(vatOriginalCurrency);
            detailedData.setOriginalCurrencyTotalAmountWithoutVat(totalOriginalCurrencyWithoutVat);
            detailedData.setVatRateId(vatRate.getId());
            detailedData.setVatRatePercent(vatRate.getValueInPercent());
            detailedData.setDetailType(InvoiceStandardDetailType.INTERIM_PRICE_COMPONENT);
            detailedData.setCostCenterControllingOrder(interimData.getCostCenterControllingOrder());
            detailedData.setIncomeAccountNumber(interimData.getNumberOfIncomeAccount());
            standardInvoiceDetails.add(detailedData);
        } else if (interimData.getValueType().equals("PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT")) {
            invoice.setInterimCalculatedFromInvoiceId(interimData.getPrevInvoiceId());

            List<StandardInvoiceForInterim> invoiceDetails = invoiceStandardDetailedDataRepository.findAllStandardDetailsByInvoiceId(interimData.getPrevInvoiceId());
            for (StandardInvoiceForInterim sd : invoiceDetails) {
                BigDecimal totalOriginalCurrencyWithoutVat = sd.getPrice()
                        .multiply(interimData.getCalculationValue())
                        .setScale(12, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(100L), 12, RoundingMode.HALF_UP);

                BigDecimal _totalMainCurrencyWithoutVat = billingRunCurrencyService.convertToCurrency(sd.getMainCurrencyId(), mainCurrencyId, totalOriginalCurrencyWithoutVat, currencyHashMap);
                ;
                BigDecimal _totalAltCurrencyWithoutVat = billingRunCurrencyService.convertToCurrency(mainCurrencyId, altCurrencyId, _totalMainCurrencyWithoutVat, currencyHashMap);

                totalMainCurrencyWithoutVat = totalMainCurrencyWithoutVat.add(_totalMainCurrencyWithoutVat);
                totalAltCurrencyWithoutVat = totalAltCurrencyWithoutVat.add(_totalAltCurrencyWithoutVat);

                BigDecimal vatOriginalCurrency = totalOriginalCurrencyWithoutVat
                        .multiply(sd.getPercent())
                        .setScale(12, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(100L), 12, RoundingMode.HALF_UP);

                BigDecimal vatMainCurrency = _totalMainCurrencyWithoutVat
                        .multiply(sd.getPercent())
                        .setScale(12, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(100L), 12, RoundingMode.HALF_UP);

                BigDecimal vatAltCurrency = _totalAltCurrencyWithoutVat
                        .multiply(sd.getPercent())
                        .setScale(12, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(100L), 12, RoundingMode.HALF_UP);

                BigDecimal _totalMainCurrencyWithVat = _totalMainCurrencyWithoutVat.add(vatMainCurrency);
                BigDecimal _totalAltCurrencyWithVat = _totalAltCurrencyWithoutVat.add(vatAltCurrency);
                totalMainCurrencyWithVat = totalMainCurrencyWithVat.add(_totalMainCurrencyWithVat);
                totalAltCurrencyWithVat = totalAltCurrencyWithVat.add(_totalAltCurrencyWithVat);

                InvoiceStandardDetailedData detailedData = new InvoiceStandardDetailedData();
                detailedData.setInvoice(invoice);
                detailedData.setInterimId(interimData.getInterimId());
                detailedData.setVatRatePercent(sd.getPercent());
                detailedData.setAltCurrencyId(altCurrencyId);
                detailedData.setMainCurrencyId(mainCurrencyId);
                detailedData.setMainCurrencyTotalAmountVat(vatMainCurrency);
                detailedData.setAltCurrencyTotalAmountVat(vatAltCurrency);
                detailedData.setMainCurrencyTotalAmountWithoutVat(_totalMainCurrencyWithoutVat);
                detailedData.setMainCurrencyTotalAmountWithVat(_totalMainCurrencyWithVat);
                detailedData.setAltCurrencyTotalAmountWithoutVat(_totalAltCurrencyWithoutVat);
                detailedData.setAltCurrencyTotalAmountWithVat(_totalAltCurrencyWithVat);
                detailedData.setProductContractDetailId(interimData.getContractType().equals(ContractType.PRODUCT_CONTRACT) ? interimData.getContractDetailId() : null);
                detailedData.setServiceContractDetailId(interimData.getContractType().equals(ContractType.SERVICE_CONTRACT) ? interimData.getContractDetailId() : null);
                detailedData.setProductDetailId(interimData.getContractType().equals(ContractType.PRODUCT_CONTRACT) ? interimData.getProductDetailId() : null);
                detailedData.setServiceDetailId(interimData.getContractType().equals(ContractType.SERVICE_CONTRACT) ? interimData.getProductDetailId() : null);
                detailedData.setOriginalCurrencyId(sd.getMainCurrencyId());
                detailedData.setOriginalCurrencyTotalAmountWithVat(totalOriginalCurrencyWithoutVat.add(vatOriginalCurrency));
                detailedData.setOriginalCurrencyTotalAmountVat(vatOriginalCurrency);
                detailedData.setOriginalCurrencyTotalAmountWithoutVat(totalOriginalCurrencyWithoutVat);
                //detailedData.setVatRateId(sd.getVatRateId());
                detailedData.setVatRatePercent(sd.getPercent());
                detailedData.setDetailType(InvoiceStandardDetailType.INTERIM_PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT);
                detailedData.setCostCenterControllingOrder(interimData.getCostCenterControllingOrder());
                detailedData.setIncomeAccountNumber(interimData.getNumberOfIncomeAccount());
                standardInvoiceDetails.add(detailedData);
            }

        } else {
            throw new RuntimeException("undefined ValueType");
        }

        List<InvoiceDetailedDataAmountModel> amountModels = standardInvoiceDetails
                .stream()
                .map(model -> new InvoiceDetailedDataAmountModel(
                        model.getVatRatePercent(),
                        model.getMainCurrencyTotalAmountWithoutVat(),
                        Boolean.TRUE,
                        invoice.getCurrencyExchangeRateOnInvoiceCreation()
                )).toList();
        List<InvoiceVatRateValue> uncommittedInvoiceVatRates = new ArrayList<>();
        List<InvoiceVatRateResponse> invoiceVatRateResponses = groupByVatRates(amountModels);

        if (!invoiceVatRateResponses.isEmpty()) {
            uncommittedInvoiceVatRates.addAll(mapVatRates(invoice.getId(), invoiceVatRateResponses));
        }
        totalMainCurrencyWithVat = EPBDecimalUtils
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

        totalMainCurrencyWithoutVat = EPBDecimalUtils
                .calculateSummary(uncommittedInvoiceVatRates
                        .stream()
                        .map(InvoiceVatRateValue::getAmountExcludingVat)
                        .toList()
                ).setScale(2, RoundingMode.HALF_UP);

        totalAltCurrencyWithVat =
                billingRunCurrencyService.convertToCurrency(mainCurrencyId, altCurrencyId, EPBDecimalUtils
                                .calculateSummary(uncommittedInvoiceVatRates
                                        .stream()
                                        .map(InvoiceVatRateValue::getValueOfVat)
                                        .toList()
                                ).setScale(2, RoundingMode.HALF_UP), currencyHashMap)
                        .setScale(2, RoundingMode.HALF_UP);


        invoice.setTotalAmountOfVat(totalMainCurrencyWithVat
                .subtract(totalMainCurrencyWithoutVat));
        invoice.setTotalAmountExcludingVat(totalMainCurrencyWithoutVat);
        invoice.setTotalAmountIncludingVat(totalMainCurrencyWithVat);
        invoice.setCurrencyId(mainCurrencyId);
        invoice.setCurrencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate());
        invoice.setCurrencyIdInOtherCurrency(altCurrencyId);


        invoice.setTotalAmountIncludingVatInOtherCurrency(totalAltCurrencyWithVat);

        LocalDate issuingForTheMonth = invoiceIssueDate;
        switch (interimData.getIssuingForTheMonthToCurrent()) {
            case MINUS_ONE -> issuingForTheMonth = issuingForTheMonth.minusMonths(1);
            case PLUS_ONE -> issuingForTheMonth = issuingForTheMonth.plusMonths(1);
            case PLUS_TWO -> issuingForTheMonth = issuingForTheMonth.plusMonths(2);
            case PLUS_TWELVE -> issuingForTheMonth = issuingForTheMonth.plusMonths(12);
        }

        LocalDate paymentTermEndDate = null;

        if (interimData.getPaymentTermId() != null) {
            if (billingRunModel.getInvoiceDueDateType().equals(InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT)) {
                InterimAdvancePaymentTerms term = interimAdvancePaymentTermsRepository.findById(interimData.getPaymentTermId()).get();
                paymentTermEndDate = billingPaymentTermDayCalculationService.calculateDeadline(term, billingRunModel.getInvoiceDate(), interimData.getPaymentTermValue());
            }else {
                paymentTermEndDate = billingRunModel.getInvoiceDueDate();
            }
        }
        if (Boolean.TRUE.equals(interimData.getMatchTermOfStandardInvoice())) {
            invoice.setIsInterimMatchInvoice(true);
            BillingInvoicePaymentTerm term;
            if (billingRunModel.getInvoiceDueDateType().equals(InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT)) {
                if (interimData.getContractType().equals(ContractType.PRODUCT_CONTRACT)) {
                    term = invoicePaymentTermsRepository.findByProductContractDetailId(interimData.getContractDetailId());
                } else {
                    term = invoicePaymentTermsRepository.findByServiceContractDetailId(interimData.getContractDetailId());
                }
                if (term != null) {
                    paymentTermEndDate = billingPaymentTermDayCalculationService.calculateDeadline(term, billingRunModel.getInvoiceDate(), term.getValue());
                }
            } else {
                paymentTermEndDate = billingRunModel.getInvoiceDueDate();
            }
        }

        invoice.setIssuingForTheMonth(issuingForTheMonth.withDayOfMonth(1));
        invoice.setIssuingForPaymentTermDate(paymentTermEndDate);
        invoice.setPaymentDeadline(paymentTermEndDate);
        invoice.setDeductionFromType(interimData.getDeductionFrom());
        invoice.setNoInterestOnOverdueDebts(interimData.getNoInterestOnOverdueDebt());
        invoiceRepository.saveAndFlush(invoice);

        standardInvoiceDetails.forEach(i -> i.setInvoiceId(invoice.getId()));

        uncommittedInvoiceVatRates.forEach(u -> u.setInvoiceId(invoice.getId()));

        invoiceVatRateValueRepository.saveAll(uncommittedInvoiceVatRates);

        invoiceStandardDetailedDataRepository.saveAll(standardInvoiceDetails);
        invoiceNumberService.fillInvoiceNumber(invoice);
        return invoice.getId();
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

    private List<InvoiceVatRateResponse> groupByVatRates(List<InvoiceDetailedDataAmountModel> models) {
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
