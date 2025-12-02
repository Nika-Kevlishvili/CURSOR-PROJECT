package bg.energo.phoenix.service.billing.model.impl;

import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.ManualDebitOrCreditNoteInvoiceSummaryData;
import bg.energo.phoenix.service.billing.model.documentmodels.*;
import bg.energo.phoenix.service.billing.model.persistance.BillingRunDocumentModel;
import bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentCompensationDAO;
import bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentDetailedDataDAO;
import bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentSummeryDataDAO;
import bg.energo.phoenix.service.billing.model.persistance.projection.BillingRunDocumentVatBaseProjection;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.math.MathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.util.Pair;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class BillingRunDocumentModelImpl extends CompanyDetailedInformationModelImpl {
    public String CustomerNameComb;
    public String CustomerNameCombTrsl;
    public String CustomerIdentifer;
    public String CustomerVat;
    public String CustomerNumber;
    public String CustomerAddressComb;
    public String CustomerPopulatedPlace;
    public String CustomerZip;
    public String CustomerDistrict;
    public String CustomerQuarterRaType;
    public String CustomerQuarterRaName;
    public String CustomerStrBlvdType;
    public String CustomerStrBlvdName;
    public String CustomerStrBlvdNumber;
    public String CustomerBlock;
    public String CustomerEntrance;
    public String CustomerFloor;
    public String CustomerApartment;
    public String CustomerAdditionalInfo;
    public List<String> CustomerSegments = new ArrayList<>();
    public String BillingRunNumber;
    public LocalDateTime FileGenerationDate;
    public String BillingGroup;
    public String ContractNumber;
    public String DocumentType;
    public String DocumentNumber;
    public String DocumentPrefix;
    public LocalDate DocumentDate;
    public LocalDate TaxEventDate;
    public LocalDate MeterReadingFrom;
    public LocalDate MeterReadingTo;
    public String InvoicedMonth;
    public LocalDate PaymentDeadline;
    public String BasisForIssuing;
    public String CurrencyPrintName;
    public String CurrencyAbr;
    public String CurrencyFullName;
    public String OtherCurrencyPrintName;
    public String OtherCurrencyAbr;
    public String OtherCurrencyFullName;
    public BigDecimal TotalExclVat;
    public BigDecimal TotalVat;
    public BigDecimal TotalInclVat;
    public BigDecimal TotalInclVatOtherCurrency;
    public BigDecimal FinalLiabilityAmount;
    public String FinalLiabilityAmountWithWords;
    public List<BillingRunDocumentSummaryDataModel> SD = new ArrayList<>();
    public List<BillingRunExcludedPriceComponentSummaryModel> SDExcludedPC = new ArrayList<>();
    public List<BillingRunDocumentDetailedDataModel> DD = new ArrayList<>();
    public List<String> ConnectedInvoice = new ArrayList<>();
    public List<BillingRunDocumentSummaryDataCompensations> SDCompensations = new ArrayList<>();

    public void fillInvoiceData(BillingRunDocumentModel model) {
        this.CustomerNameComb = model.getCustomerNameComb();
        this.CustomerNameCombTrsl = model.getCustomerNameCombTrsl();
        this.CustomerIdentifer = model.getCustomerIdentifer();
        this.CustomerVat = model.getCustomerVat();
        this.CustomerNumber = model.getCustomerNumber();
        this.CustomerSegments = EPBListUtils.convertDBStringArrayIntoListString(model.getCustomerSegments());
        this.BillingRunNumber = model.getBillingNumber();
        this.FileGenerationDate = LocalDateTime.now();
        this.BillingGroup = model.getBillingGroup();
        this.ContractNumber = model.getContractNumber();
        this.DocumentType = model.getDocumentType();
        this.DocumentNumber = model.getDocumentNumber();
        this.DocumentPrefix = model.getDocumentPrefix();
        this.DocumentDate = model.getDocumentDate();
        this.TaxEventDate = model.getTaxEventDate();
        this.MeterReadingFrom = model.getMeterReadingFrom();
        this.MeterReadingTo = model.getMeterReadingTo();
        this.InvoicedMonth = model.getInvoicedMonth();
        this.PaymentDeadline = model.getPaymentDeadline();
        this.BasisForIssuing = model.getBasisForIssuing();
        this.CurrencyPrintName = model.getCurrencyPrintName();
        this.CurrencyAbr = model.getCurrencyAbr();
        this.CurrencyFullName = model.getCurrencyFullName();
        this.OtherCurrencyPrintName = model.getOtherCurrencyPrintName();
        this.OtherCurrencyAbr = model.getOtherCurrencyAbr();
        this.OtherCurrencyFullName = model.getOtherCurrencyFullName();
        this.TotalExclVat = EPBDecimalUtils.convertToCurrencyScale(model.getTotalExclVat());
        this.TotalVat = EPBDecimalUtils.convertToCurrencyScale(model.getTotalVat());
        this.TotalInclVat = EPBDecimalUtils.convertToCurrencyScale(model.getTotalInclVat());
        this.TotalInclVatOtherCurrency = EPBDecimalUtils.convertToCurrencyScale(model.getTotalInclVatOtherCurrency());
        this.CustomerAddressComb = model.getCustomerAddressComb();
        this.CustomerPopulatedPlace = model.getCustomerPopulatedPlace();
        this.CustomerZip = model.getCustomerZip();
        this.CustomerDistrict = model.getCustomerDistrict();
        this.CustomerQuarterRaType = model.getCustomerQuarterRaType();
        this.CustomerQuarterRaName = model.getCustomerQuarterRaName();
        this.CustomerStrBlvdType = model.getCustomerStrBlvdType();
        this.CustomerStrBlvdName = model.getCustomerStrBlvdName();
        this.CustomerStrBlvdNumber = model.getCustomerStrBlvdNumber();
        this.CustomerBlock = model.getCustomerBlock();
        this.CustomerEntrance = model.getCustomerEntrance();
        this.CustomerFloor = model.getCustomerFloor();
        this.CustomerApartment = model.getCustomerApartment();
        this.CustomerAdditionalInfo = model.getCustomerAdditionalInfo();

    }

    public void fillSummaryData(
            List<BillingRunDocumentSummeryDataDAO> models
    ) {

        Map<BigDecimal, BillingRunDocumentSummaryDataModel> summaryDataGroupedByVatRate = new HashMap<>();

        for (BillingRunDocumentSummeryDataDAO model : models) {
            switch (ObjectUtils.defaultIfNull(model.getDetailType(), "MANUAL")) {
                case "INTERIM_ADVANCE_PAYMENT",
                     "MANUAL_INTERIM_ADVANCE_PAYMENT",
                     "INTERIM_EXACT_AMOUNT",
                     "INTERIM_PRICE_COMPONENT",
                     "INTERIM_PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT" ->
                        processAdvancePayment(model, summaryDataGroupedByVatRate);
                case "INTERIM_DEDUCTION" -> processDeductedAdvancePayment(model, summaryDataGroupedByVatRate);
                default -> processPriceComponents(model, summaryDataGroupedByVatRate);
            }
            calculateDeductionValues(model, summaryDataGroupedByVatRate);
        }

        this.SD = summaryDataGroupedByVatRate.values()
                .stream()
                .sorted(Comparator.comparing(BillingRunDocumentSummaryDataModel::getVatRate)
                        .reversed())
                .peek(model -> {
                    model.DirectPC.sort(Comparator.comparing(
                                    BillingRunDocumentSummaryDataPriceComponentModel::getPriceUnit,
                                    Comparator.nullsFirst(Comparator.naturalOrder()))
                            .thenComparing(
                                    BillingRunDocumentSummaryDataPriceComponentModel::getPC));
                    model.IndirectPC.sort(Comparator.comparing(
                                    BillingRunDocumentSummaryDataPriceComponentModel::getPriceUnit,
                                    Comparator.nullsFirst(Comparator.naturalOrder()))
                            .thenComparing(
                                    BillingRunDocumentSummaryDataPriceComponentModel::getPC));
                    model.PCG.sort(Comparator.comparing(
                            BillingRunDocumentSummaryDataPriceComponentGroupModel::getPC));
                    model.AdvancePayments.sort(Comparator.comparing(
                            BillingRunDocumentSummaryDataAdvancePaymentModel::getPC));
                    model.DeductedAdvancePayments.sort(Comparator.comparing(
                            BillingRunDocumentSummaryDataAdvancePaymentModel::getPC));
                })
                .toList();
    }

    private void calculateDeductionValues(BillingRunDocumentSummeryDataDAO model, Map<BigDecimal, BillingRunDocumentSummaryDataModel> summaryDataMap) {
        BillingRunDocumentSummaryDataModel summaryModel = findOrCreateSummaryDataModel(model, summaryDataMap);
        BigDecimal totalAmount = model.getValue();

        summaryModel.TotalAfterInterimDeduction = StringUtils.equals("INTERIM_DEDUCTION", model.getDetailType()) ?
                summaryModel.TotalAfterInterimDeduction.subtract(totalAmount.abs()) : summaryModel.TotalAfterInterimDeduction.add(totalAmount);
        if (!StringUtils.equals("INTERIM_DEDUCTION", model.getDetailType())) {
            summaryModel.TotalBeforeInterimDeduction = summaryModel.TotalBeforeInterimDeduction.add(totalAmount);
        }
    }

    public void fillDetailedData(
            List<BillingRunDocumentDetailedDataDAO> models, List
            <BillingRunDocumentCompensationDAO> compensationDAOS,
            List<BillingRunDocumentVatBaseProjection> vatBaseProjections
    ) {
        Map<String, BillingRunDocumentDetailedDataModel> detailedDataGroupedByPointOfDelivery = new HashMap<>();

        for (BillingRunDocumentDetailedDataDAO model : models) {
            BillingRunDocumentDetailedDataModel billingRunDocumentDetailedDataModel = findOrCreateDetailedDataModel(
                    model,
                    detailedDataGroupedByPointOfDelivery
            );

            switch (model.getDetailType()) {
                case "SCALE" -> {
                    billingRunDocumentDetailedDataModel
                            .TableScalesData
                            .add(BillingRunDocumentDetailedDataScaleDataModel
                                    .builder()
                                    .ScaleType(model.getDetailType())
                                    .PeriodFrom(model.getPeriodFrom())
                                    .PeriodTo(model.getPeriodTo())
                                    .NewMR(model.getNewMeterReading())
                                    .OldMR(model.getOldMeterReading())
                                    .Difference(model.getDifference())
                                    .Multiplier(model.getMultiplier())
                                    .Correction(model.getCorrection())
                                    .Deducted(model.getDeducted())
                                    .Volumes(model.getTotalVolumes())
                                    .build());

                    BillingRunDocumentDetailedDataPriceComponentScale billingRunDocumentDetailedDataPriceComponentScale =
                            billingRunDocumentDetailedDataModel
                                    .TablePCScales
                                    .stream()
                                    .filter(scale -> existScaleModel(model, scale))
                                    .findFirst()
                                    .orElseGet(() -> createScalePriceComponentModel(model, billingRunDocumentDetailedDataModel));

                    addModelToScalesDataModel(model, billingRunDocumentDetailedDataPriceComponentScale);
                }
                case "SETTLEMENT", "FOR_MANUAL", "WITH_ELECTRICITY",
                     "OVER_TIME_PERIODICAL", "OVER_TIME_ONE_TIME", "SERVICE_ORDER" ->
                        billingRunDocumentDetailedDataModel
                                .TablePCProfiles
                                .add(
                                        BillingRunDocumentDetailedDataPriceComponentProfileModel
                                                .builder()
                                                .PC(model.getPriceComponent())
                                                .PeriodFrom(model.getPeriodFrom())
                                                .PeriodTo(model.getPeriodTo())
                                                .Value(model.getValue())
                                                .ValueUnit(model.getMeasureUnitOfValue())
                                                .Price(model.getPrice())
                                                .PriceOtherCurrency(model.getPriceInOtherCurrency())
                                                .PriceUnit(model.getMeasureUnitOfPrice())
                                                .PriceUnitOtherCurrency(model.getMeasureUnitOfPriceInOtherCurrency())
                                                .TotalVolumes(model.getTotalVolumes())
                                                .TotalVolumesUnit(model.getMeasureUnitForTotalVolumes())
                                                .build()
                                );
            }
        }

        finalizeScales(detailedDataGroupedByPointOfDelivery.values());
        Map<String, List<BillingRunDocumentCompensationDAO>> compensationMap = compensationDAOS.stream()
                .collect(Collectors.groupingBy(
                        BillingRunDocumentCompensationDAO::getPodId));
        Map<String, List<BillingRunDocumentVatBaseProjection>> vatBaseMap = vatBaseProjections.stream().collect(Collectors.groupingBy(x -> x.getPodId()));
        this.DD = detailedDataGroupedByPointOfDelivery.values()
                .stream()
                .sorted(Comparator.comparing(BillingRunDocumentDetailedDataModel::getPODID))
                .peek(x -> {
                    x.TableCompensations.addAll(!compensationMap.containsKey(x.PODID) ? new ArrayList<>() : compensationMap.get(x.PODID).stream().map(BillingRunDocumentDetailedDataCompensations::new).toList());
                    x.TableExcludedPC.addAll(!vatBaseMap.containsKey(x.PODID) ? new ArrayList<>() : vatBaseMap.get(x.PODID).stream().map(j -> new BillingRunExcludedPriceComponentSummaryModel(j, DocumentNumber, DocumentDate)).toList());
                })
                .toList();
    }

    private void finalizeScales(Collection<BillingRunDocumentDetailedDataModel> models) {
        for (BillingRunDocumentDetailedDataModel ddm : models) {
            long periodCounter = 1;
            List<BillingRunDocumentDetailedDataPriceComponentScale> tablePCScales = ddm.TablePCScales;
            tablePCScales.sort(Comparator.comparing(sc -> sc.PeriodFrom));
            for (BillingRunDocumentDetailedDataPriceComponentScale tablePCScale : tablePCScales) {
                tablePCScale.PeriodCounter = String.valueOf(periodCounter++);
            }
        }
    }

    private boolean existScaleModel(BillingRunDocumentDetailedDataDAO model, BillingRunDocumentDetailedDataPriceComponentScale scale) {
        boolean isPeriodFromSame = scale.PeriodFrom.equals(model.getPeriodFrom());
        boolean isPeriodToSame = scale.PeriodTo.equals(model.getPeriodTo());
        boolean isMeterSame = Objects.equals(scale.Meter, model.getMeter());
        boolean isProductSame = Objects.equals(scale.Product, model.getProductName());

        return Stream.of(isPeriodFromSame, isPeriodToSame, isMeterSame, isProductSame)
                .allMatch((sc) -> sc);
    }

    private BillingRunDocumentDetailedDataPriceComponentScale createScalePriceComponentModel(
            BillingRunDocumentDetailedDataDAO model,
            BillingRunDocumentDetailedDataModel billingRunDocumentDetailedDataModel
    ) {
        BillingRunDocumentDetailedDataPriceComponentScale scale = BillingRunDocumentDetailedDataPriceComponentScale
                .builder()
                .PeriodFrom(model.getPeriodFrom())
                .PeriodTo(model.getPeriodTo())
                .Meter(model.getMeter())
                .Product(model.getProductName())
                .ListPC(new ArrayList<>())
                .build();

        billingRunDocumentDetailedDataModel
                .TablePCScales
                .add(scale);
        return scale;
    }

    private void addModelToScalesDataModel(
            BillingRunDocumentDetailedDataDAO model,
            BillingRunDocumentDetailedDataPriceComponentScale billingRunDocumentDetailedDataPriceComponentScale
    ) {
        billingRunDocumentDetailedDataPriceComponentScale
                .ListPC
                .add(BillingRunDocumentDetailedDataPriceComponentModel
                        .builder()
                        .PC(model.getPriceComponent())
                        .NewMR(model.getNewMeterReading())
                        .OldMR(model.getOldMeterReading())
                        .DifferenceM(model.getDifference())
                        .Multiplier(model.getMultiplier())
                        .ScaleCode(model.getScaleCode())
                        .Correction(model.getCorrection())
                        .Deducted(model.getDeducted())
                        .Value(model.getValue())
                        .ValueUnit(model.getMeasureUnitOfValue())
                        .Price(model.getPrice())
                        .PriceOtherCurrency(model.getPriceInOtherCurrency())
                        .PriceUnit(model.getMeasureUnitOfPrice())
                        .PriceUnitOtherCurrency(model.getMeasureUnitOfPriceInOtherCurrency())
                        .TotalVolumes(model.getTotalVolumes())
                        .TotalVolumesUnit(model.getMeasureUnitForTotalVolumes())
                        .build());
    }

    private void processPriceComponents(
            BillingRunDocumentSummeryDataDAO model,
            Map<BigDecimal, BillingRunDocumentSummaryDataModel> summaryDataMap
    ) {
        BillingRunDocumentSummaryDataModel summaryModel = findOrCreateSummaryDataModel(model, summaryDataMap);

        switch (model.getPriceComponentConnectionType()) {
            case DIRECT -> {
                addDirectPriceComponentToSummaryDataModel(model, summaryModel);
            }
            case FROM_PC_GROUP -> {
                addIndirectPriceComponentToSummaryDataModel(model, summaryModel);

            }
            case GROUP -> addPCGToSummaryDataModel(model, summaryModel);
        }

        addToSummaryAmountInSummaryDataModel(model, summaryModel);
    }

    private void processAdvancePayment(
            BillingRunDocumentSummeryDataDAO model,
            Map<BigDecimal, BillingRunDocumentSummaryDataModel> summaryDataMap
    ) {
        BillingRunDocumentSummaryDataModel summaryModel = findOrCreateSummaryDataModel(model, summaryDataMap);

        summaryModel
                .AdvancePayments
                .add(
                        BillingRunDocumentSummaryDataAdvancePaymentModel
                                .builder()
                                .PC(model.getPriceComponent())
                                .Value(model.getValue())
                                .ValueUnit(model.getMeasureUnitOfValue())
                                .build()
                );

        addToSummaryAmountInSummaryDataModel(model, summaryModel);
    }

    private void processDeductedAdvancePayment(
            BillingRunDocumentSummeryDataDAO model,
            Map<BigDecimal, BillingRunDocumentSummaryDataModel> summaryDataMap
    ) {
        BillingRunDocumentSummaryDataModel summaryModel = findOrCreateSummaryDataModel(model, summaryDataMap);

        summaryModel
                .DeductedAdvancePayments
                .add(
                        BillingRunDocumentSummaryDataAdvancePaymentModel
                                .builder()
                                .PC(model.getPriceComponent())
                                .Value(model.getValue())
                                .ValueUnit(model.getMeasureUnitOfValue())
                                .build()
                );

        addToSummaryAmountInSummaryDataModel(model, summaryModel);
    }

    private BillingRunDocumentSummaryDataModel findOrCreateSummaryDataModel(
            BillingRunDocumentSummeryDataDAO model,
            Map<BigDecimal, BillingRunDocumentSummaryDataModel> summaryDataMap
    ) {
        if (summaryDataMap.containsKey(model.getVatRatePercent())) {
            return summaryDataMap.get(model.getVatRatePercent());
        } else {
            BillingRunDocumentSummaryDataModel summaryModel = new BillingRunDocumentSummaryDataModel();
            summaryModel.VatRate = model.getVatRatePercent();
            summaryModel.TotalExclVat = BigDecimal.ZERO;
            summaryModel.TotalVat = BigDecimal.ZERO;

            summaryDataMap.put(model.getVatRatePercent(), summaryModel);
            return summaryModel;
        }
    }

    private BillingRunDocumentDetailedDataModel findOrCreateDetailedDataModel(
            BillingRunDocumentDetailedDataDAO model,
            Map<String, BillingRunDocumentDetailedDataModel> detailedDataGroupedByPointOfDelivery
    ) {
        if (detailedDataGroupedByPointOfDelivery.containsKey(model.getPod())) {
            return detailedDataGroupedByPointOfDelivery.get(model.getPod());
        } else {
            BillingRunDocumentDetailedDataModel billingRunDocumentDetailedDataModel = new BillingRunDocumentDetailedDataModel();
            billingRunDocumentDetailedDataModel.PODID = model.getPod();
            billingRunDocumentDetailedDataModel.PODName = model.getPodName();
            billingRunDocumentDetailedDataModel.PODZip = model.getPodZip();
            billingRunDocumentDetailedDataModel.PODAdditionalID = model.getPodAdditionalID();
            billingRunDocumentDetailedDataModel.PODPlace = model.getPodPlace();
            billingRunDocumentDetailedDataModel.PODAddressComb = model.getPodAddressComb();
            billingRunDocumentDetailedDataModel.SLP = model.getMeasurementType();
            billingRunDocumentDetailedDataModel.Profile = model.getMeteringType();

            List<BillingRunDocumentDetailedDataPriceComponentScale> tablePCScales = new ArrayList<>();
            List<BillingRunDocumentDetailedDataPriceComponentProfileModel> tablePCProfiles = new ArrayList<>();
            billingRunDocumentDetailedDataModel.TablePCScales = tablePCScales;
            billingRunDocumentDetailedDataModel.TablePCProfiles = tablePCProfiles;

            detailedDataGroupedByPointOfDelivery.put(model.getPod(), billingRunDocumentDetailedDataModel);

            return billingRunDocumentDetailedDataModel;
        }
    }

    public BillingRunDocumentModelImpl fillSummaryDataForTaxInvoice(
            ManualDebitOrCreditNoteInvoiceSummaryData summary,
            BillingRunDocumentModel documentSpecificData
    ) {
        List<BillingRunDocumentSummaryDataModel> summaryData = new ArrayList<>();
        BillingRunDocumentSummaryDataModel model = new BillingRunDocumentSummaryDataModel();
        model.VatRate = summary.getVatRatePercent();
        model.TotalVat = documentSpecificData.getTotalVat();
        model.TotalExclVat = documentSpecificData.getTotalExclVat();
        List<BillingRunDocumentSummaryDataPriceComponentModel> directPC = new ArrayList<>();
        BillingRunDocumentSummaryDataPriceComponentModel pcModel = new BillingRunDocumentSummaryDataPriceComponentModel();
        pcModel.PC = summary.getPriceComponentOrPriceComponentGroups();
        pcModel.Value = summary.getValue();
        pcModel.ValueUnit = summary.getValueCurrencyName();
        directPC.add(pcModel);
        model.DirectPC = directPC;
        summaryData.add(model);
        this.SD = summaryData;
        return this;
    }

    public void fillSummaryDataForOrders(List<BillingRunDocumentSummeryDataDAO> models) {
        Map<BigDecimal, BillingRunDocumentSummaryDataModel> summaryDataGroupedByVatRate = new HashMap<>();

        for (BillingRunDocumentSummeryDataDAO model : models) {
            BillingRunDocumentSummaryDataModel summaryModel = findOrCreateSummaryDataModel(model, summaryDataGroupedByVatRate);

            BillingRunDocumentSummaryDataPriceComponentModel summaryDataPriceComponentModel = BillingRunDocumentSummaryDataPriceComponentModel
                    .builder()
                    .PC(model.getPriceComponent())
                    .TotalVolumes(EPBDecimalUtils.truncateToTwoDecimals(model.getTotalVolumes()))
                    .TotalVolumesUnit(model.getMeasureUnitForTotalVolumes())
                    .Price(EPBDecimalUtils.truncateToTwoDecimals(model.getPrice()))
                    .PriceOtherCurrency(EPBDecimalUtils.truncateToTwoDecimals(model.getPriceOtherCurrency()))
                    .PriceUnit(model.getMeasureUnitOfPrice())
                    .Value(EPBDecimalUtils.truncateToTwoDecimals(model.getValue()))
                    .ValueUnit(model.getMeasureUnitOfValue())
                    .build();

            if (StringUtils.isBlank(model.getPriceComponentGroup())) {
                summaryModel.DirectPC.add(summaryDataPriceComponentModel);
            } else {
                summaryModel.IndirectPC.add(summaryDataPriceComponentModel);

                summaryModel
                        .PCG
                        .stream()
                        .filter(pcg -> pcg.PC.equals(model.getPriceComponentGroup()))
                        .findFirst()
                        .ifPresentOrElse(
                                pcg -> pcg.Value = pcg.Value.add(EPBDecimalUtils.truncateToTwoDecimals(model.getPrice())),
                                () -> summaryModel.PCG.add(
                                        BillingRunDocumentSummaryDataPriceComponentGroupModel
                                                .builder()
                                                .PC(model.getPriceComponentGroup())
                                                .Value(EPBDecimalUtils.truncateToTwoDecimals(model.getValue()))
                                                .ValueUnit(model.getMeasureUnitOfValue())
                                                .build()
                                )
                        );
            }

            summaryModel.TotalExclVat = summaryModel.TotalExclVat.add(model.getValue());
            summaryModel.TotalVat = summaryModel.TotalVat.add(model
                    .getValue()
                    .multiply(model.getVatRatePercent())
                    .divide(BigDecimal.valueOf(100), MathContext.DECIMAL64));
        }

        this.SD = summaryDataGroupedByVatRate.values()
                .stream()
                .toList();
    }

    private void addToSummaryAmountInSummaryDataModel(
            BillingRunDocumentSummeryDataDAO model,
            BillingRunDocumentSummaryDataModel summaryModel
    ) {
        summaryModel.TotalExclVat = summaryModel.TotalExclVat.add(ObjectUtils.defaultIfNull(model.getValue(), BigDecimal.ZERO));

        if (model.getValue() != null && model.getVatRatePercent() != null) {
            BigDecimal vatRateValue = MathUtils.calculatePercentage(model.getValue(), model.getVatRatePercent());
            summaryModel.TotalVat = summaryModel.TotalVat.add(vatRateValue);
        }
    }

    private void addDirectPriceComponentToSummaryDataModel(
            BillingRunDocumentSummeryDataDAO model,
            BillingRunDocumentSummaryDataModel summaryModel
    ) {
        BillingRunDocumentSummaryDataPriceComponentModel priceComponentModel = BillingRunDocumentSummaryDataPriceComponentModel
                .builder()
                .PC(model.getPriceComponent())
                .TotalVolumes(model.getTotalVolumes())
                .TotalVolumesUnit(model.getMeasureUnitForTotalVolumes())
                .Price(model.getPrice())
                .PriceOtherCurrency(model.getPriceOtherCurrency())
                .PriceUnit(model.getMeasureUnitOfPrice())
                .Value(model.getValue())
                .ValueUnit(model.getMeasureUnitOfValue())
                .build();

        summaryModel.DirectPC.add(priceComponentModel);
    }


    private void addIndirectPriceComponentToSummaryDataModel(
            BillingRunDocumentSummeryDataDAO model,
            BillingRunDocumentSummaryDataModel summaryModel
    ) {
        BillingRunDocumentSummaryDataPriceComponentModel priceComponentModel = BillingRunDocumentSummaryDataPriceComponentModel
                .builder()
                .PC(model.getPriceComponent())
                .TotalVolumes(model.getTotalVolumes())
                .TotalVolumesUnit(model.getMeasureUnitForTotalVolumes())
                .Price(model.getPrice())
                .PriceOtherCurrency(model.getPriceOtherCurrency())
                .PriceUnit(model.getMeasureUnitOfPrice())
                .Value(model.getValue())
                .ValueUnit(model.getMeasureUnitOfValue())
                .build();

        summaryModel.IndirectPC.add(priceComponentModel);
    }

    private void addPCGToSummaryDataModel(BillingRunDocumentSummeryDataDAO model, BillingRunDocumentSummaryDataModel summaryModel) {
        summaryModel
                .PCG
                .stream()
                .filter(pcg -> Objects.equals(pcg.PC, model.getPriceComponent()))
                .findFirst()
                .ifPresentOrElse(
                        pcg -> pcg.Value = pcg.Value.add(model.getValue()),
                        () -> {
                            BillingRunDocumentSummaryDataPriceComponentGroupModel priceComponentGroupModel = BillingRunDocumentSummaryDataPriceComponentGroupModel
                                    .builder()
                                    .PC(model.getPriceComponent())
                                    .Value(model.getValue())
                                    .ValueUnit(model.getMeasureUnitOfValue())
                                    .build();

                            summaryModel.PCG.add(priceComponentGroupModel);
                        }
                );
    }

    public void fillConnectedInvoices(List<Invoice> invoices) {
        invoices.forEach(inv -> {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            this.ConnectedInvoice.add("%s/%s".formatted(
                    inv.getInvoiceNumber(),
                    inv.getInvoiceDate()
                            .format(dateTimeFormatter)
            ));
        });
    }

    public void fillCompensations(List<BillingRunDocumentCompensationDAO> compensations) {

        Map<DocumentCompensationKey, Pair<BigDecimal, BigDecimal>> map = new HashMap<>();
        for (BillingRunDocumentCompensationDAO doc : compensations) {
            DocumentCompensationKey key = new DocumentCompensationKey(
                    doc.getDocumentNumber(),
                    doc.getDocumentDate(),
                    doc.getPeriod(),
                    doc.getCurrency()
            );
            Pair<BigDecimal, BigDecimal> orDefault = map.getOrDefault(key, Pair.of(BigDecimal.ZERO, BigDecimal.ZERO));


            map.put(key, Pair.of(orDefault.getFirst().add(doc.getAmount()), orDefault.getSecond().add(doc.getVolumes())));
        }

        map.forEach((key, value) -> {
            this.SDCompensations.add(new BillingRunDocumentSummaryDataCompensations(key, value.getFirst(), value.getSecond()));
        });
    }

    public void fillVatBase(List<BillingRunDocumentVatBaseProjection> vatBaseProjections) {
        this.SDExcludedPC = vatBaseProjections.stream().map(x ->
                BillingRunExcludedPriceComponentSummaryModel.builder()
                        .PC(x.getPriceComponent())
                        .DocumentNumber(this.DocumentNumber)
                        .DocumentDate(this.DocumentDate)
                        .TotalVolumes(x.getTotalVolumes())
                        .TotalVolumesUnit(x.getMeasureUnitForTotalVolumes())
                        .Price(x.getPrice())
                        .PriceOtherCurrency(x.getPriceInOtherCurrency())
                        .PriceUnit(x.getMeasureUnitOfPrice())
                        .PriceUnitOtherCurrency(x.getMeasureUnitOfPriceInOtherCurrency())
                        .Value(x.getValue())
                        .ValueUnit(x.getMeasureUnitOfValue())
                        .build()
        ).toList();
    }
}
