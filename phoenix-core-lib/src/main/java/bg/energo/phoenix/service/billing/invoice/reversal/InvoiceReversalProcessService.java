package bg.energo.phoenix.service.billing.invoice.reversal;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.invoice.*;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceReversalStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceReconnectionDto;
import bg.energo.phoenix.repository.billing.billingRun.BillingInvoicesFileRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.repository.billing.invoice.*;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.customerReceivables.CustomerReceivableRepository;
import bg.energo.phoenix.service.billing.billingRun.errors.BillingRunErrorService;
import bg.energo.phoenix.service.billing.billingRun.errors.InvoiceErrorShortObject;
import bg.energo.phoenix.service.billing.invoice.InvoiceEventPublisher;
import bg.energo.phoenix.service.billing.invoice.InvoiceService;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityService;
import bg.energo.phoenix.service.receivable.customerReceivables.CustomerReceivableService;
import bg.energo.phoenix.util.epb.EPBExcelUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static bg.energo.phoenix.util.epb.EPBDecimalUtils.convertToCurrencyScale;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceReversalProcessService {

    private final InvoiceRepository invoiceRepository;
    private final BillingInvoicesFileRepository billingInvoicesFileRepository;
    private final FileService fileService;

    private final InvoiceReversalInvoicesRepository invoiceReversalInvoicesRepository;
    private final TransactionTemplate transactionTemplate;
    private final BillingRunRepository billingRunRepository;
    private final InvoiceEventPublisher invoiceEventPublisher;
    private final InvoiceVatRateValueRepository invoiceVatRateValueRepository;
    private final BillingRunErrorService billingRunErrorService;

    private final ManualInvoiceSummaryDataRepository summaryDataRepository;
    private final ManualInvoiceDetailedDataRepository detailedDataRepository;
    private final InvoiceStandardDetailedDataRepository standardDetailedDataRepository;
    private final InvoiceDetailedDataRepository invoiceDetailedDataRepository;
    private final ManualDebitOrCreditNoteInvoiceSummaryDataRepository manualDebitOrCreditNoteInvoiceSummaryDataRepository;
    private final ManualDebitOrCreditNoteInvoiceDetailedDataRepository manualDebitOrCreditNoteInvoiceDetailedDataRepository;
    private final InvoiceNumberService invoiceNumberService;
    private final InvoiceService invoiceService;
    private final CustomerReceivableRepository customerReceivableRepository;
    private final CustomerLiabilityService customerLiabilityService;
    private final CustomerReceivableService customerReceivableService;
    private final CurrencyRepository currencyRepository;
    @Value("${product-contract.termination-with-expiration-term.number-of-threads}")
    private Integer numberOfThreads;

    @Value("${product-contract.termination-with-expiration-term.batch-size}")
    private Integer batchSize;

    @Value("${product-contract.termination-with-expiration-term.query-batch-size}")
    private Integer queryBatchSize;


    @Transactional
    public void saveData(BillingRun billingRun) {
        Set<String> invoiceNumbers = billingRun.getListOfInvoices() == null ? new HashSet<>() : Arrays.stream(billingRun.getListOfInvoices().split(",")).collect(Collectors.toSet());
        List<InvoiceErrorShortObject> errorMessages = new ArrayList<>();
        Map<String, String> numberWithDateMap = new HashMap<>();

        List<InvoiceReversalModel> invoices = new ArrayList<>();
        billingInvoicesFileRepository.findByBillingIdAndStatus(billingRun.getId(), EntityStatus.ACTIVE)
                .ifPresent(x -> {
                    try {
                        Workbook workbook = new XSSFWorkbook(fileService.downloadFile(x.getFileUrl()).getInputStream());
                        Sheet sheet = workbook.getSheetAt(0);
                        for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                            Row cells = sheet.getRow(i);
                            if (cells == null) {
                                break;
                            }
                            String fileInvoiceNumber = EPBExcelUtils.getStringValue(0, cells);
                            if (fileInvoiceNumber == null) {
                                continue;
                            }
                            if (invoiceNumbers.contains(fileInvoiceNumber)) {
                                errorMessages.add(new InvoiceErrorShortObject(fileInvoiceNumber, "Invoice Number: %s is already provided in text block. Row: %s".formatted(fileInvoiceNumber, cells.getRowNum())));
                                continue;
                            }
                            LocalDate localDateValue = EPBExcelUtils.getLocalDateValue(1, cells);
                            if (localDateValue == null) {
                                invoiceNumbers.add(fileInvoiceNumber);
                            } else {
                                if (numberWithDateMap.containsKey(fileInvoiceNumber)) {
                                    errorMessages.add(new InvoiceErrorShortObject(fileInvoiceNumber, "Duplicate value found in file for invoice: %s, row: %s".formatted(fileInvoiceNumber, cells.getRowNum())));
                                } else {
                                    Optional<InvoiceReversalModel> invoiceNumberAndDate = invoiceRepository.findAllByInvoiceNumberAndDate(fileInvoiceNumber, localDateValue);
                                    if (invoiceNumberAndDate.isEmpty()) {
                                        errorMessages.add(new InvoiceErrorShortObject(fileInvoiceNumber, "Invoice with number: %s not found or is reversed!!;".formatted(fileInvoiceNumber)));
                                        continue;
                                    }
                                    invoices.add(invoiceNumberAndDate.get());
                                    numberWithDateMap.put(fileInvoiceNumber, localDateValue.toString());
                                }
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });


        invoices.addAll(invoiceRepository.findAllByInvoiceNumberAndNewest(invoiceNumbers));

        for (InvoiceReversalModel invoice : invoices) {
            numberWithDateMap.remove(invoice.getInvoiceNumber());
            invoiceNumbers.remove(invoice.getInvoiceNumber());
        }
        invoiceNumbers.addAll(numberWithDateMap.keySet());
        for (String invoiceNumber : invoiceNumbers) {
            errorMessages.add(new InvoiceErrorShortObject(invoiceNumber, "Invoice with number: %s not found or is reversed!;".formatted(invoiceNumber)));
        }


        if (!errorMessages.isEmpty()) {
            billingRunErrorService.publishBillingErrors(errorMessages, billingRun.getId(), billingRun.getStatus());
        }

        invoiceReversalInvoicesRepository.saveAll(invoices.stream().map(x -> new InvoiceReversalInvoice(x, billingRun.getId())).toList());
    }

    @SneakyThrows
    public void startProcessing(BillingRun billingRun) {
        if (!billingRun.getStatus().equals(BillingStatus.INITIAL) && !billingRun.getStatus().equals(BillingStatus.IN_PROGRESS_DRAFT)) {
            return;
        }
        List<InvoiceReversalModelExtended> extendedModels = invoiceReversalInvoicesRepository.findReversalModels(billingRun.getId());

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        Map<InvoiceReversalModelExtended, List<InvoiceReversalModelExtended>> reversalModelMap = new HashMap<>();
        Map<Long, InvoiceReversalModelExtended> interimReversalModels = new HashMap<>();

        for (InvoiceReversalModelExtended invoice : extendedModels) {
            if (invoice.getInvoiceType().equals(InvoiceType.INTERIM_AND_ADVANCE_PAYMENT)) {
                if (invoice.getStandardId() != null) {
                    InvoiceReversalModelExtended reversalModel = interimReversalModels.get(invoice.getStandardId());
                    if (reversalModel == null) {
                        interimReversalModels.put(invoice.getStandardId(), invoice);
                        reversalModelMap.put(invoice, new ArrayList<>());
                    } else {
                        List<InvoiceReversalModelExtended> reversalModels = reversalModelMap.get(reversalModel);
                        reversalModels.add(invoice);
                    }
                } else {
                    reversalModelMap.put(invoice, null);
                }
            } else {
                reversalModelMap.put(invoice, null);
            }
        }

        ArrayList<Map.Entry<InvoiceReversalModelExtended, List<InvoiceReversalModelExtended>>> entries = new ArrayList<>(reversalModelMap.entrySet());
        List<List<Map.Entry<InvoiceReversalModelExtended, List<InvoiceReversalModelExtended>>>> partitions = ListUtils.partition(entries, batchSize);

        List<Callable<Boolean>> reversalTasks = new ArrayList<>();
        int callableCount = 0;
        for (List<Map.Entry<InvoiceReversalModelExtended, List<InvoiceReversalModelExtended>>> partition : partitions) {
            Callable<Boolean> reversalTask = () -> {
                List<InvoiceReversalInvoice> invoiceReversalInvoices = new ArrayList<>();
                List<InvoiceErrorShortObject> errorMessages = new ArrayList<>();
                for (Map.Entry<InvoiceReversalModelExtended, List<InvoiceReversalModelExtended>> curr : partition) {
                    try {
                        processInvoice(curr.getKey(), curr.getValue(), billingRun.getId());
                        setReversalStatus(invoiceReversalInvoices, curr, InvoiceReversalStatus.REVERSED);
                    } catch (Exception e) {
                        setReversalStatus(invoiceReversalInvoices, curr, InvoiceReversalStatus.FAILED);
                        errorMessages.add(new InvoiceErrorShortObject(curr.getKey().getInvoiceNumber(), e.getMessage()));
                    }
                }
                if (!errorMessages.isEmpty()) {
                    billingRunErrorService.publishBillingErrors(errorMessages, billingRun.getId(), billingRun.getStatus());
                }
                invoiceReversalInvoicesRepository.saveAll(invoiceReversalInvoices);
                return true;
            };
            reversalTasks.add(reversalTask);
            callableCount++;
            if (callableCount >= queryBatchSize) {
                executorService.invokeAll(reversalTasks);
                reversalTasks.clear();
                if (billingRunRepository.isBillingRunPaused(billingRun.getId())) {
                    break;
                }
            }
        }
        if (!reversalTasks.isEmpty()) {
            executorService.invokeAll(reversalTasks);
        }

        billingRun.setStatus(BillingStatus.DRAFT);
        billingRunRepository.save(billingRun);
    }

    private void setReversalStatus(List<InvoiceReversalInvoice> invoiceReversalInvoices, Map.Entry<InvoiceReversalModelExtended, List<InvoiceReversalModelExtended>> curr, InvoiceReversalStatus status) {
        InvoiceReversalInvoice keyReversal = curr.getKey().getInvoiceReversalInvoice();
        keyReversal.setReversalStatus(status);
        invoiceReversalInvoices.add(keyReversal);
        List<InvoiceReversalModelExtended> value = curr.getValue();
        if (CollectionUtils.isNotEmpty(value)) {
            for (InvoiceReversalModelExtended invoiceReversalModelExtended : curr.getValue()) {
                InvoiceReversalInvoice invoiceReversalInvoice = invoiceReversalModelExtended.getInvoiceReversalInvoice();
                invoiceReversalInvoice.setReversalStatus(status);
                invoiceReversalInvoices.add(invoiceReversalInvoice);
            }
        }
    }


    private void processInvoice(InvoiceReversalModel base, List<InvoiceReversalModelExtended> additionalModels, Long billingRunId) {
        transactionTemplate.executeWithoutResult(j -> {
            if (base.getInvoiceType().equals(InvoiceType.MANUAL)) {
                processManual(base, billingRunId);
            } else if (base.getInvoiceType().equals(InvoiceType.STANDARD)) {
                processStandard(base, billingRunId);
            } else if (base.getInvoiceType().equals(InvoiceType.INTERIM_AND_ADVANCE_PAYMENT)) {
                processInterim(base, additionalModels, billingRunId);
            }
        });
    }

    private void processInterim(InvoiceReversalModel base, List<InvoiceReversalModelExtended> additionalModels, Long billingRunId) {
//დავადედაქტო ბეის ინვოისი
        //შევქმნა რევერსალი ყველა ინტერიმისთვის.
        //შევქმნა რევერსალი სტანდარტისთვის


        if(base.getStandardId()!=null){
            //Check all interim is provided;
            //reverse all interim
            StringBuilder stringBuilder = new StringBuilder();

            List<Invoice> additionalInvoices = invoiceRepository.findAllInterimByStandardId(base.getStandardId());
            Set<Long> collect = additionalModels.stream().map(InvoiceReversalModel::getInvoiceId).collect(Collectors.toSet());
            collect.add(base.getInvoiceId());
            for (Invoice additionalInvoice : additionalInvoices) {
                if (additionalInvoice.getId().equals(base.getInvoiceId())) {
                    continue;
                }
                if (!collect.contains(additionalInvoice.getId())) {
                    if (!stringBuilder.isEmpty()) {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(additionalInvoice.getInvoiceNumber());
                    continue;
                }
                Invoice reversedAdditionalInterim = invoiceRepository.save(invoiceReversal(additionalInvoice, billingRunId, null));
                //save additional data

                reverseVatRates(reversedAdditionalInterim.getReversalCreatedFromId(), reversedAdditionalInterim.getId());
                reverseManualAndSummaryData(reversedAdditionalInterim.getReversalCreatedFromId(), reversedAdditionalInterim.getId());
                reverseStandardDetailedData(reversedAdditionalInterim.getReversalCreatedFromId(), reversedAdditionalInterim);
                reverseDetailedData(reversedAdditionalInterim.getReversalCreatedFromId(), reversedAdditionalInterim.getId());
            }
            if (!stringBuilder.isEmpty()) {
                throw new DomainEntityNotFoundException("Invoices with number: %s. are missed from reversal list".formatted(stringBuilder.toString()));
            }
            Invoice baseStandardInvoice = invoiceRepository.findById(base.getStandardId()).orElseThrow();
            Invoice baseStandardReversed = invoiceRepository.save(invoiceReversal(baseStandardInvoice, billingRunId, null));
            reverseVatRates(baseStandardReversed.getReversalCreatedFromId(), baseStandardReversed.getId());
            reverseManualAndSummaryData(baseStandardReversed.getReversalCreatedFromId(), baseStandardReversed.getId());
            reverseStandardDetailedData(baseStandardReversed.getReversalCreatedFromId(), baseStandardReversed);
            reverseDetailedData(baseStandardReversed.getReversalCreatedFromId(), baseStandardReversed.getId());
        }

        Invoice baseInterimInvoice = invoiceRepository.findById(base.getInvoiceId()).orElseThrow();
        Invoice baseInterimSaved = invoiceRepository.save(invoiceReversal(baseInterimInvoice, billingRunId, null));
        baseInterimInvoice.setIsDeducted(true);
        invoiceRepository.save(baseInterimInvoice);
        reverseVatRates(baseInterimSaved.getReversalCreatedFromId(), baseInterimSaved.getId());
        reverseManualAndSummaryData(baseInterimSaved.getReversalCreatedFromId(), baseInterimSaved.getId());
        reverseStandardDetailedData(baseInterimSaved.getReversalCreatedFromId(), baseInterimSaved);
        reverseDetailedData(baseInterimSaved.getReversalCreatedFromId(), baseInterimSaved.getId());


        //reverse base
        //make base deducted

        /*
        List<Invoice> invoicesToSave = new ArrayList<>();
        List<Invoice> oldInvoicesToSave = new ArrayList<>();
        Invoice baseInvoice = invoiceRepository.
                findById(base.getInvoiceId()).orElseThrow(() -> new DomainEntityNotFoundException("Invoice with id: %s not found!;".formatted(base.getInvoiceId())));
        baseInvoice.setIsDeducted(true);
        oldInvoicesToSave.add(baseInvoice);
        invoicesToSave.add(invoiceReversal(baseInvoice,
                billingRunId, null));
        if (base.getStandardId() != null) {
            StringBuilder stringBuilder = new StringBuilder();
            List<Invoice> additionalInvoices = invoiceRepository.findAllInterimByStandardId(base.getStandardId());
            Set<Long> collect = additionalModels.stream().map(InvoiceReversalModel::getInvoiceId).collect(Collectors.toSet());

            for (Invoice additionalInvoice : additionalInvoices) {
                if (additionalInvoice.getId().equals(base.getInvoiceId())) {
                    continue;
                }
                if (!collect.contains(additionalInvoice.getId())) {
                    if (!stringBuilder.isEmpty()) {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(additionalInvoice.getInvoiceNumber());
                }
                invoicesToSave.add(invoiceReversal(additionalInvoice, billingRunId, null));
            }
            if (!stringBuilder.isEmpty()) {
                throw new DomainEntityNotFoundException("Invoices with number: %s. are missed from reversal list".formatted(stringBuilder.toString()));
            }
        }
*/


/*        List<Invoice> allForInterim = invoiceRepository.findAllForInterim(base.getStandardId());
        if (!allForInterim.isEmpty()) {
            for (Invoice invoice : allForInterim) {
                invoice.setIsDeducted(true);
                oldInvoicesToSave.add(invoice);
                invoicesToSave.add(invoiceReversal(invoice, billingRunId, null));
            }
        }

        List<Invoice> saved = invoiceRepository.saveAll(invoicesToSave);
        invoiceRepository.saveAll(oldInvoicesToSave);
        Set<Long> ids = new HashSet<>();
        for (Invoice invoice : saved) {
            ids.add(invoice.getId());
            reverseVatRates(invoice.getReversalCreatedFromId(), invoice.getId());
            reverseManualAndSummaryData(invoice.getReversalCreatedFromId(), invoice.getId());
            reverseStandardDetailedData(invoice.getReversalCreatedFromId(), invoice);
            reverseDetailedData(invoice.getReversalCreatedFromId(), invoice.getId());
        }

        invoiceEventPublisher.publishInvoiceEvent(ids);*/
    }

    private void processStandard(InvoiceReversalModel base, Long billingRunId) {
        List<Invoice> invoicesToSave = new ArrayList<>();

        invoicesToSave.add(invoiceReversal(invoiceRepository.
                        findById(base
                                .getInvoiceId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Invoice with id: %s not found!;".formatted(base.getInvoiceId()))),
                billingRunId,
                null
        ));
        List<Invoice> correctionsForStandard = invoiceRepository.findCorrectionsForStandard(base.getInvoiceId());
        if (!correctionsForStandard.isEmpty()) {
            for (Invoice invoice : correctionsForStandard) {
                invoicesToSave.add(invoiceReversal(invoice, billingRunId, null));
            }
        }
        List<Invoice> saved = invoiceRepository.saveAll(invoicesToSave);
        Set<Invoice> invoices = new HashSet<>();
        for (Invoice invoice : saved) {
            invoices.add(invoice);
            reverseVatRates(invoice.getReversalCreatedFromId(), invoice.getId());
            reverseManualAndSummaryData(invoice.getReversalCreatedFromId(), invoice.getId());
            reverseStandardDetailedData(invoice.getReversalCreatedFromId(), invoice);
            reverseDetailedData(invoice.getReversalCreatedFromId(), invoice.getId());
        }
        invoiceNumberService.fillInvoiceNumber(invoices);
    }

    private void processManual(InvoiceReversalModel base, Long billingRunId) {
        Invoice invoiceId = invoiceRepository.findById(base.getInvoiceId()).orElseThrow(() -> new DomainEntityNotFoundException("Invoice with id: %s not found!;".formatted(base.getInvoiceId())));
        Invoice save = invoiceRepository.save(invoiceReversal(invoiceId, billingRunId, null));

        reverseVatRates(invoiceId.getId(), save.getId());
        reverseManualAndSummaryData(invoiceId.getId(), save.getId());

        reverseDetailedData(invoiceId.getId(), save.getId());
        invoiceNumberService.fillInvoiceNumber(save);
    }

    public void processReconnection(Long invoiceId, InvoiceReconnectionDto invoiceReconnectionDto) {
        Invoice invoice = invoiceReversal(invoiceRepository.findById(invoiceId)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Invoice with id: %s not found!;".formatted(invoiceId))),
                null,
                invoiceReconnectionDto);
        Invoice saved = invoiceRepository.save(invoice);


        reverseVatRatesForReconnection(saved.getReversalCreatedFromId(), saved.getId(), invoiceReconnectionDto);
        reverseManualAndSummaryData(saved.getReversalCreatedFromId(), saved.getId());
        reverseDetailedData(saved.getReversalCreatedFromId(), saved.getId());

        invoiceNumberService.fillInvoiceNumber(saved);
        customerLiabilityService.createReceivableForReversal(invoice);
    }

    private void reverseDetailedData(Long oldId, Long newId) {
        List<InvoiceDetailedData> allByInvoiceId = invoiceDetailedDataRepository.findAllByInvoiceId(oldId);
        invoiceDetailedDataRepository.saveAll(allByInvoiceId.stream().map(x -> x.cloneForReversal(newId)).toList());
    }

    private void reverseVatRates(Long oldId, Long newId) {
        List<InvoiceVatRateValue> allByInvoiceIdIn = invoiceVatRateValueRepository.findAllByInvoiceId(oldId);
        invoiceVatRateValueRepository
                .saveAll(allByInvoiceIdIn.stream().map(x -> new InvoiceVatRateValue(null, x.getVatRatePercent(), x.getAmountExcludingVat(), x.getValueOfVat(), newId)).toList());
    }

    private void reverseVatRatesForReconnection(Long oldId, Long newId, InvoiceReconnectionDto invoiceReconnectionDto) {
        List<InvoiceVatRateValue> allByInvoiceIdIn = invoiceVatRateValueRepository.findAllByInvoiceId(oldId);
        invoiceVatRateValueRepository
                .saveAll(allByInvoiceIdIn.stream().map(x -> new InvoiceVatRateValue(null, invoiceReconnectionDto.vatRatePercent(), invoiceReconnectionDto.taxValue(), invoiceReconnectionDto.valueOfVat(), newId)).toList());
    }

    public void reverseManualAndSummaryData(Long oldId, Long newId) {

        List<ManualInvoiceSummaryData> summaryDataList = summaryDataRepository.findAllByInvoiceId(oldId);
        summaryDataRepository.saveAll(summaryDataList.stream().map(x -> x.cloneForReversal(newId)).toList());

        List<ManualInvoiceDetailedData> detailedData = detailedDataRepository.findAllByInvoiceId(oldId);
        detailedDataRepository.saveAll(detailedData.stream().map(x -> x.cloneForReversal(newId)).toList());


        List<ManualDebitOrCreditNoteInvoiceSummaryData> manualDebitOrCreditNoteInvoiceSummaryData = manualDebitOrCreditNoteInvoiceSummaryDataRepository.findAllByInvoiceId(oldId);
        manualDebitOrCreditNoteInvoiceSummaryDataRepository.saveAll(manualDebitOrCreditNoteInvoiceSummaryData.stream().map(x -> x.cloneForReversal(newId)).toList());

        List<ManualDebitOrCreditNoteInvoiceDetailedData> manualDebitOrCreditNoteInvoiceDetailedData = manualDebitOrCreditNoteInvoiceDetailedDataRepository.findAllByInvoiceId(oldId);
        manualDebitOrCreditNoteInvoiceDetailedDataRepository.saveAll(manualDebitOrCreditNoteInvoiceDetailedData.stream().map(x -> x.cloneForReversal(newId)).toList());
    }

    public void reverseStandardDetailedData(Long oldId, Invoice newInvoice) {

        List<InvoiceStandardDetailedData> standardDetailedData = standardDetailedDataRepository.findAllByInvoiceId(oldId);
        standardDetailedDataRepository.saveAll(standardDetailedData.stream().map(x -> x.cloneForReversal(newInvoice)).toList());
    }

    private Invoice invoiceReversal(Invoice oldInvoice, Long billingId, InvoiceReconnectionDto invoiceReconnectionDto) {
        Invoice newInvoice = new Invoice();

        newInvoice.setInvoiceDate(oldInvoice.getInvoiceDate());
        newInvoice.setInvoiceStatus(invoiceReconnectionDto == null ? InvoiceStatus.DRAFT : InvoiceStatus.REAL);
        newInvoice.setInvoiceType(InvoiceType.REVERSAL);
        InvoiceDocumentType oldDocumentType = oldInvoice.getInvoiceDocumentType();
        newInvoice.setInvoiceDocumentType(List.of(InvoiceDocumentType.INVOICE, InvoiceDocumentType.DEBIT_NOTE).contains(oldDocumentType) ? InvoiceDocumentType.CREDIT_NOTE : InvoiceDocumentType.DEBIT_NOTE);
        newInvoice.setTaxEventDate(oldInvoice.getTaxEventDate());
        newInvoice.setPaymentDeadline(oldInvoice.getPaymentDeadline());
        newInvoice.setMeterReadingPeriodFrom(oldInvoice.getMeterReadingPeriodFrom());
        newInvoice.setMeterReadingPeriodTo(oldInvoice.getMeterReadingPeriodTo());

        newInvoice.setBasisForIssuing(oldInvoice.getBasisForIssuing());
        newInvoice.setIncomeAccountNumber(oldInvoice.getIncomeAccountNumber());
        newInvoice.setCostCenterControllingOrder(oldInvoice.getCostCenterControllingOrder());
        newInvoice.setInterestRateId(oldInvoice.getInterestRateId());
        newInvoice.setDirectDebit(oldInvoice.getDirectDebit());
        newInvoice.setBankId(oldInvoice.getBankId());

        newInvoice.setIban(oldInvoice.getIban());
        newInvoice.setCustomerDetailId(oldInvoice.getCustomerDetailId());
        newInvoice.setCustomerId(oldInvoice.getCustomerId());
        newInvoice.setCustomerCommunicationId(oldInvoice.getCustomerCommunicationId());
        newInvoice.setContractCommunicationId(oldInvoice.getContractCommunicationId());
        newInvoice.setGoodsOrderId(oldInvoice.getGoodsOrderId());
        newInvoice.setServiceOrderId(oldInvoice.getServiceOrderId());
        newInvoice.setContractBillingGroupId(oldInvoice.getContractBillingGroupId());

        newInvoice.setProductContractId(oldInvoice.getProductContractId());
        newInvoice.setProductContractDetailId(oldInvoice.getProductContractDetailId());
        newInvoice.setServiceContractId(oldInvoice.getServiceContractId());
        newInvoice.setServiceContractDetailId(oldInvoice.getServiceContractDetailId());
        newInvoice.setBillingId(billingId);
        newInvoice.setInvoiceCancellationId(oldInvoice.getInvoiceCancellationId());
        newInvoice.setCurrencyId(oldInvoice.getCurrencyId());

        newInvoice.setCurrencyExchangeRateOnInvoiceCreation(oldInvoice.getCurrencyExchangeRateOnInvoiceCreation());
        newInvoice.setProductDetailId(oldInvoice.getProductDetailId());
        newInvoice.setServiceDetailId(oldInvoice.getServiceDetailId());
        newInvoice.setAlternativeRecipientCustomerDetailId(oldInvoice.getAlternativeRecipientCustomerDetailId());
        newInvoice.setAccountPeriodId(oldInvoice.getAccountPeriodId());

        setAmounts(newInvoice, oldInvoice, invoiceReconnectionDto);
        newInvoice.setNoInterestOnOverdueDebts(oldInvoice.getNoInterestOnOverdueDebts());
        newInvoice.setTemplateDetailId(oldInvoice.getTemplateDetailId());
        newInvoice.setReversalCreatedFromId(oldInvoice.getId());
        return newInvoice;
    }

    public List<Invoice> validateRealGeneration(BillingRun billingRun) {
        List<ReversalValidationObject> objectForDraft = invoiceRepository.findAllValidObjectForReal(billingRun.getId());
        List<InvoiceErrorShortObject> errorMessages = new ArrayList<>();
        List<Long> invalidInvoices = new ArrayList<>();
        invalidInvoices.add(-1L);
        for (ReversalValidationObject reversalValidationObject : objectForDraft) {
            if (reversalValidationObject.getReversalId() == null) {
                errorMessages.add(new InvoiceErrorShortObject(reversalValidationObject.getInvoiceNumber(), "Invoice with number %s should be marked for real generation!;".formatted(reversalValidationObject.getInvoiceNumber())));
                invalidInvoices.add(reversalValidationObject.getInvoiceId());
            }
        }

        List<Invoice> allValidInvoicesForAccounting = invoiceRepository.findAllValidInvoicesForReversalAccounting(billingRun.getId(), invalidInvoices);
        allValidInvoicesForAccounting.forEach((inv) -> inv.setInvoiceStatus(InvoiceStatus.REAL));
        allValidInvoicesForAccounting.stream().collect(Collectors.groupingBy(Invoice::getInvoiceDocumentType))
                .forEach((key, value) -> {
                    for (Invoice invoice : value) {
                        if (key.equals(InvoiceDocumentType.DEBIT_NOTE)) {
                            customerLiabilityService.createLiabilityForReversal(invoice);
                        } else if (key.equals(InvoiceDocumentType.CREDIT_NOTE)) {
                            customerLiabilityService.createReceivableForReversal(invoice);
                        }
                    }
                });

        if (!errorMessages.isEmpty()) {
            billingRunErrorService.publishBillingErrors(errorMessages, billingRun.getId(), billingRun.getStatus());
        }

        return allValidInvoicesForAccounting;
    }

    private void setAmounts(Invoice newInvoice, Invoice oldInvoice, InvoiceReconnectionDto invoiceReconnectionDto) {
        if (invoiceReconnectionDto == null) {
            newInvoice.setTotalAmountExcludingVat(oldInvoice.getTotalAmountExcludingVat());
            newInvoice.setTotalAmountOfVat(oldInvoice.getTotalAmountOfVat());
            newInvoice.setTotalAmountIncludingVat(oldInvoice.getTotalAmountIncludingVat());
            newInvoice.setTotalAmountIncludingVatInOtherCurrency(oldInvoice.getTotalAmountIncludingVatInOtherCurrency());
        } else {
            Currency currency = currencyRepository.findById(oldInvoice.getCurrencyId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found with ID: " + oldInvoice.getCurrencyId()));
            BigDecimal taxValue = invoiceReconnectionDto.taxValue();
            BigDecimal valueOfVat = invoiceReconnectionDto.valueOfVat();
            BigDecimal totalAmountIncludingVat = invoiceReconnectionDto.totalAmountIncludingVat();

            newInvoice.setTotalAmountOfVat(valueOfVat);
            newInvoice.setTotalAmountExcludingVat(taxValue);
            newInvoice.setTotalAmountIncludingVat(totalAmountIncludingVat);
            newInvoice.setTotalAmountOfVatInOtherCurrency(convertToCurrencyScale(valueOfVat.multiply(taxValue)));
            newInvoice.setTotalAmountIncludingVatInOtherCurrency(convertToCurrencyScale(totalAmountIncludingVat.multiply(currency.getAltCurrencyExchangeRate())));
            newInvoice.setTotalAmountExcludingVatInOtherCurrency(convertToCurrencyScale(taxValue.multiply(currency.getAltCurrencyExchangeRate())));
        }
    }
}
