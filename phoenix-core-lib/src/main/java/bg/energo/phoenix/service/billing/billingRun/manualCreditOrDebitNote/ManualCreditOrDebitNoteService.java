package bg.energo.phoenix.service.billing.billingRun.manualCreditOrDebitNote;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingDetailedData;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRunInvoices;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingSummaryData;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.enums.billing.billings.DocumentType;
import bg.energo.phoenix.model.enums.billing.billings.ManualInvoiceType;
import bg.energo.phoenix.model.enums.billing.billings.SendingAnInvoice;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.billing.billingRun.BillingRunCommonParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.BillingRunCreateRequest;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.DetailedDataRowParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceDetailedDataParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceSummaryDataParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.SummaryDataRowParameters;
import bg.energo.phoenix.model.request.billing.billingRun.edit.BillingRunEditRequest;
import bg.energo.phoenix.model.request.billing.billingRun.manualCreditOrDebitNote.BillingRunInvoiceInformation;
import bg.energo.phoenix.model.request.billing.billingRun.manualCreditOrDebitNote.ManualCreditOrDebitNoteBasicDataParameters;
import bg.energo.phoenix.model.request.billing.billingRun.manualCreditOrDebitNote.ManualCreditOrDebitNoteParameters;
import bg.energo.phoenix.model.response.billing.billingRun.manualCreditOrDebitNote.ManualCreditOrDebitNoteBillingRunParametersResponse;
import bg.energo.phoenix.repository.billing.billingRun.BillingDetailedDataRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunInvoicesRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingSummaryDataRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.service.billing.billingRun.manualInvoice.ManualInvoiceMapperService;
import bg.energo.phoenix.service.billing.invoice.enums.InvoiceObjectType;
import bg.energo.phoenix.util.billing.BillingVatRateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.billing.billings.InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualCreditOrDebitNoteService {

    private final InterestRateRepository interestRateRepository;

    private final BankRepository bankRepository;

    private final CurrencyRepository currencyRepository;

    private final BillingSummaryDataRepository billingSummaryDataRepository;

    private final BillingDetailedDataRepository billingDetailedDataRepository;

    private final BillingRunInvoicesRepository billingRunInvoicesRepository;

    private final InvoiceRepository invoiceRepository;

    private final ManualInvoiceMapperService manualInvoiceMapperService;

    private final ManualCreditOrDebitNoteMapperService manualCreditOrDebitNoteMapperService;
    private final BillingVatRateUtil billingVatRateUtil;

    @Transactional
    public void create(BillingRunCreateRequest billingRunCreateRequest, BillingRun billingRun, List<String> errorMessages) {
        validateDueDate(billingRunCreateRequest.getManualCreditOrDebitNoteParameters(), billingRunCreateRequest.getCommonParameters(), errorMessages);
        ManualCreditOrDebitNoteParameters manualCreditOrDebitNoteParameters = billingRunCreateRequest.getManualCreditOrDebitNoteParameters();
        ManualCreditOrDebitNoteBasicDataParameters basicParameters = manualCreditOrDebitNoteParameters.getManualCreditOrDebitNoteBasicDataParameters();

        createBillingInvoices(basicParameters, billingRun.getId(), billingRunCreateRequest.getCommonParameters(), errorMessages);

        mapManualCreditOrDebitNoteBasicDataParameters(basicParameters, billingRun, errorMessages);
        mapManualCreditOrDebitNoteSummaryDataParameters(manualCreditOrDebitNoteParameters.getManualCreditOrDebitNoteSummaryDataParameters(), billingRun, errorMessages);
        if (manualCreditOrDebitNoteParameters.getManualCreditOrDebitNoteSummaryDataParameters().getManualInvoiceType().equals(ManualInvoiceType.DETAILED_INVOICE) &&
            manualCreditOrDebitNoteParameters.getManualCreditOrDebitNoteDetailedDataParameters() != null) {
            mapManualCreditOrDebitNoteDetailedDataParameters(manualCreditOrDebitNoteParameters.getManualCreditOrDebitNoteDetailedDataParameters(), billingRun, errorMessages);
        }
    }

    @Transactional
    public void edit(BillingRunEditRequest billingRunCreateRequest, BillingRun billingRun, List<String> errorMessages) {
        validateDueDate(billingRunCreateRequest.getManualCreditOrDebitNoteParameters(), billingRunCreateRequest.getCommonParameters(), errorMessages);
        ManualCreditOrDebitNoteParameters manualCreditRequest = billingRunCreateRequest.getManualCreditOrDebitNoteParameters();
        ManualCreditOrDebitNoteBasicDataParameters basicDataParameters = manualCreditRequest.getManualCreditOrDebitNoteBasicDataParameters();
        mapManualCreditOrDebitNoteBasicDataParameters(basicDataParameters, billingRun, errorMessages);
        //billingInvoices
        editBillingInvoices(basicDataParameters, billingRun.getId(), billingRunCreateRequest.getCommonParameters(), errorMessages);

        ManualInvoiceSummaryDataParameters summaryData = manualCreditRequest.getManualCreditOrDebitNoteSummaryDataParameters();
        editSummaryData(summaryData, billingRun, errorMessages);

        editDetailedData(manualCreditRequest.getManualCreditOrDebitNoteDetailedDataParameters(), billingRun, errorMessages);

    }

    private void editBillingInvoices(ManualCreditOrDebitNoteBasicDataParameters basicDataParameters, Long billingRunId, BillingRunCommonParameters basicParameters, List<String> errorMessages) {
        List<BillingRunInvoiceInformation> billingRunInvoiceInformationList = basicDataParameters.getBillingRunInvoiceInformationList();
        List<Long> invoiceIds = billingRunInvoiceInformationList.stream().map(BillingRunInvoiceInformation::getInvoiceId).toList();
        List<BillingRunInvoices> billingRunInvoices = new ArrayList<>();
        if (!billingRunInvoicesRepository.invoicesHaveSameCustomer(invoiceIds)) {
            throw new ClientException("Invoice customers doesn't match", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        List<Long> invoiceIdsWithoutInvoiceType = billingRunInvoicesRepository.getInvoiceIdsWithoutInvoiceType(invoiceIds);
        if (!CollectionUtils.isEmpty(invoiceIdsWithoutInvoiceType)) {
            for (Long l : invoiceIdsWithoutInvoiceType) {
                errorMessages.add("Invoice with id %s doesn't have \"INVOICE\" as document type".formatted(l));
            }
            return;
        }

        Map<Long, BillingRunInvoices> invoicesMap = billingRunInvoicesRepository.findAllByBillingId(billingRunId).stream().collect(Collectors.toMap(BillingRunInvoices::getInvoiceId, j -> j));
        Long firstInvoiceId = invoiceIds.get(0);
        Invoice firstInvoice = invoiceRepository.findById(firstInvoiceId).orElseThrow(() -> new DomainEntityNotFoundException("First invoice was not found!;"));
        InvoiceObjectType invoiceObjectType = InvoiceObjectType.defineInvoiceObjectType(firstInvoice);
        if (invoiceObjectType.equals(InvoiceObjectType.SERVICE_ORDER) || invoiceObjectType.equals(InvoiceObjectType.GOODS_ORDER)) {
            if (basicParameters.getInvoiceDueDate() != null && ACCORDING_TO_THE_CONTRACT.equals(basicParameters.getInvoiceDueDate())) {
                errorMessages.add("commonParameters.invoiceDate-Invoice due date can not be according to the contract!;");
            }
            if (SendingAnInvoice.ACCORDING_TO_THE_CONTRACT.equals(basicParameters.getSendingAnInvoice())) {
                errorMessages.add("commonParameters.sendingAnInvoice-Sending an invoice can not be according to the contract!;");
            }
        }


        Set<Long> available = invoiceRepository.availableInvoicesForBilling(firstInvoiceId);
        int i = 0;
        for (Long invoiceId : invoiceIds) {
            if (!available.contains(invoiceId) && i!=0) {
                errorMessages.add("manualCreditOrDebitNoteBasicDataParameters.billingRunInvoiceInformationList.invoiceId[%s]-Invoice has different type then first!;".formatted(i));
            }
            BillingRunInvoices remove = invoicesMap.remove(invoiceId);
            if (remove == null) {
                BillingRunInvoices billingRunInvoice = new BillingRunInvoices();
                billingRunInvoice.setInvoiceId(invoiceId);
                billingRunInvoice.setBillingId(billingRunId);
                billingRunInvoice.setStatus(EntityStatus.ACTIVE);
                billingRunInvoices.add(billingRunInvoice);
            }
            i++;
        }
        if(InvoiceObjectType.ONLY_CUSTOMER.equals(invoiceObjectType)){
            validateDataParameters(basicDataParameters,errorMessages);
        }
        if (InvoiceObjectType.PRODUCT_CONTRACT.equals(invoiceObjectType) && !billingRunInvoicesRepository.invoicesHaveSameType(invoiceIds)) {
            throw new ClientException("Invoice billing groups doesn't match", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        Collection<BillingRunInvoices> values = invoicesMap.values();
        if (!CollectionUtils.isEmpty(values)) {
            for (BillingRunInvoices value : values) {
                value.setStatus(EntityStatus.DELETED);
                billingRunInvoices.add(value);
            }

        }
        billingRunInvoicesRepository.saveAll(billingRunInvoices);
    }

    private void editDetailedData(ManualInvoiceDetailedDataParameters dataParameters, BillingRun billingRun, List<String> errorMessages) {
        List<DetailedDataRowParameters> detailedRow;
        Map<Long, BillingDetailedData> dataMap = billingDetailedDataRepository.findByBillingId(billingRun.getId()).stream().collect(Collectors.toMap(BillingDetailedData::getId, j -> j));
        if (dataParameters == null || dataParameters.getDetailedDataRowParametersList() == null) {
            detailedRow = new ArrayList<>();
        } else {
            detailedRow = dataParameters.getDetailedDataRowParametersList();
        }
        List<BillingDetailedData> dataToSave = new ArrayList<>();

        for (int i = 0; i < detailedRow.size(); i++) {
            DetailedDataRowParameters row = detailedRow.get(i);
            Long id = row.getId();
            if (row.getId() == null) {
                BillingDetailedData billingDetailedData = new BillingDetailedData();
                fillDetailedData(row, billingRun, errorMessages, i, billingDetailedData);
                dataToSave.add(billingDetailedData);
                continue;
            }
            BillingDetailedData remove = dataMap.remove(id);
            if (remove == null) {
                errorMessages.add("manualCreditOrDebitNoteParameters.manualCreditOrDebitNoteDetailedDataParameters.detailedDataRowParametersList[%s].id-Wrong id provided!;".formatted(i));
                return;
            }
            fillDetailedData(row, billingRun, errorMessages, i, remove);
            dataToSave.add(remove);
        }
        Collection<BillingDetailedData> valuesToDelete = dataMap.values();
        if (!CollectionUtils.isEmpty(valuesToDelete)) {
            List<Long> list = valuesToDelete.stream().map(BillingDetailedData::getId).toList();
            billingDetailedDataRepository.deleteAllById(list);

        }
        billingDetailedDataRepository.saveAll(dataToSave);
    }

    private void editSummaryData(ManualInvoiceSummaryDataParameters summaryDataParameters, BillingRun billingRun, List<String> errorMessages) {
        billingRun.setManualInvoiceType(summaryDataParameters.getManualInvoiceType());
        Map<Long, BillingSummaryData> dataMap = billingSummaryDataRepository.findByBillingId(billingRun.getId()).stream().collect(Collectors.toMap(x -> x.getId(), j -> j));

        List<SummaryDataRowParameters> rowList = summaryDataParameters.getSummaryDataRowList();

        List<BillingSummaryData> dataToSave = new ArrayList<>();
        for (int i = 0; i < rowList.size(); i++) {
            SummaryDataRowParameters row = rowList.get(i);
            Long id = row.getId();
            if (id == null) {
                BillingSummaryData billingSummaryData = new BillingSummaryData();

                fillBillingSummaryData(billingRun, row, billingSummaryData, errorMessages, i);
                dataToSave.add(billingSummaryData);
                continue;
            }
            BillingSummaryData remove = dataMap.remove(id);
            if (remove == null) {
                errorMessages.add("manualCreditOrDebitNoteSummaryDataParameters.summaryDataRowList[%s].id-Wrong id provided!;".formatted(i));
                return;
            }
            dataToSave.add(remove);
            fillBillingSummaryData(billingRun, row, remove, errorMessages, i);
        }
        Collection<BillingSummaryData> valuesToDelete = dataMap.values();
        if (!CollectionUtils.isEmpty(valuesToDelete)) {
            List<Long> list = valuesToDelete.stream().map(BillingSummaryData::getId).toList();
            billingSummaryDataRepository.deleteAllById(list);
        }
        billingSummaryDataRepository.saveAll(dataToSave);
    }

    private void mapManualCreditOrDebitNoteBasicDataParameters(ManualCreditOrDebitNoteBasicDataParameters manualCreditOrDebitNoteBasicDataParameters, BillingRun billingRun, List<String> errorMessages) {
        checkManualCreditOrDebitNoteBasicDataParameters(manualCreditOrDebitNoteBasicDataParameters, billingRun, errorMessages);
        billingRun.setBasisForIssuing(manualCreditOrDebitNoteBasicDataParameters.getBasisForIssuing());
        billingRun.setNumberOfIncomeAccount(manualCreditOrDebitNoteBasicDataParameters.getNumberOfIncomeAccount());
        billingRun.setCostCenterControllingOrder(manualCreditOrDebitNoteBasicDataParameters.getCostCenterControllingOrder());
        billingRun.setDirectDebit(!manualCreditOrDebitNoteBasicDataParameters.isDirectDebitManual() ? null : manualCreditOrDebitNoteBasicDataParameters.isDirectDebit());
        billingRun.setIban(manualCreditOrDebitNoteBasicDataParameters.getIban());
        billingRun.setDocumentType(manualCreditOrDebitNoteBasicDataParameters.getDocumentType());
    }

    private void mapManualCreditOrDebitNoteSummaryDataParameters(ManualInvoiceSummaryDataParameters manualCreditOrDebitNoteSummaryDataParameters, BillingRun billingRun, List<String> errorMessages) {
        billingRun.setManualInvoiceType(manualCreditOrDebitNoteSummaryDataParameters.getManualInvoiceType());
        int index = 0;
        for (SummaryDataRowParameters dataRowParameters : manualCreditOrDebitNoteSummaryDataParameters.getSummaryDataRowList()) {
            createAndSaveBillingSummaryDataParameters(billingRun, dataRowParameters, errorMessages, index);
            index++;
        }
    }

    private void createAndSaveBillingSummaryDataParameters(BillingRun billingRun, SummaryDataRowParameters dataRowParameters, List<String> errorMessages, int index) {
        BillingSummaryData billingSummaryData = new BillingSummaryData();
        fillBillingSummaryData(billingRun, dataRowParameters, billingSummaryData, errorMessages, index);
        billingSummaryDataRepository.save(billingSummaryData);
    }

    private void fillBillingSummaryData(BillingRun billingRun, SummaryDataRowParameters dataRowParameters, BillingSummaryData billingSummaryData, List<String> errorMessages, int index) {
        billingSummaryData.setBillingId(billingRun.getId());
        billingSummaryData.setValue(dataRowParameters.getValue());
        billingSummaryData.setCostCenter(dataRowParameters.getCostCenter());
        billingSummaryData.setIncomeAccount(dataRowParameters.getIncomeAccount());
        billingSummaryData.setUnitPrice(dataRowParameters.getUnitPrice());
        billingSummaryData.setPriceComponentOrPriceComponentGroups(dataRowParameters.getPriceComponentOrPriceComponentGroupOrItem());
        billingSummaryData.setTotalVolumes(dataRowParameters.getTotalVolumes());
        billingSummaryData.setMeasuresUnitForTotalVolumes(dataRowParameters.getUnitOfMeasuresForTotalVolumes());
        if (dataRowParameters.getValueCurrencyId() != null) {
            if (checkCurrency(dataRowParameters.getValueCurrencyId(), errorMessages, "manualCreditOrDebitNoteSummaryDataParameters.summaryDataRowList[%s]".formatted(index))) {
                billingSummaryData.setValueCurrencyId(dataRowParameters.getValueCurrencyId());
            }
        }
        billingSummaryData.setMeasureUnitForUnitPrice(dataRowParameters.getUnitOfMeasureForUnitPrice());
        billingVatRateUtil.checkVatRateSummary(dataRowParameters.getGlobalVatRate(), dataRowParameters.getVatRateId(), errorMessages, billingSummaryData, "manualCreditOrDebitNoteSummaryDataParameters.summaryDataRowList[%s]".formatted(index));

    }

    private void mapManualCreditOrDebitNoteDetailedDataParameters(ManualInvoiceDetailedDataParameters manualCreditOrDebitNoteDetailedDataParameters, BillingRun billingRun, List<String> errorMessages) {
        int index = 0;
        for (DetailedDataRowParameters dataRowParameters : manualCreditOrDebitNoteDetailedDataParameters.getDetailedDataRowParametersList()) {
            createAndSaveBillingDetailedData(dataRowParameters, billingRun, errorMessages, index);
            index++;
        }
    }

    private void createAndSaveBillingDetailedData(DetailedDataRowParameters dataRowParameters, BillingRun billingRun, List<String> errorMessages, int index) {
        BillingDetailedData billingDetailedData = new BillingDetailedData();
        fillDetailedData(dataRowParameters, billingRun, errorMessages, index, billingDetailedData);

        billingDetailedDataRepository.save(billingDetailedData);
    }

    private void fillDetailedData(DetailedDataRowParameters dataRowParameters, BillingRun billingRun, List<String> errorMessages, int index, BillingDetailedData billingDetailedData) {
        billingDetailedData.setBillingId(billingRun.getId());
        billingDetailedData.setDifferences(dataRowParameters.getDifferences());
        billingDetailedData.setCorrection(dataRowParameters.getCorrection());
        billingDetailedData.setDeducted(dataRowParameters.getDeducted());
        billingDetailedData.setMeter(dataRowParameters.getMeter());
        billingDetailedData.setOldMeterReading(dataRowParameters.getOldMeterReading());
        billingDetailedData.setNewMeterReading(dataRowParameters.getNewMeterReading());
        billingDetailedData.setCostCenter(dataRowParameters.getCostCenter());
        billingDetailedData.setIncomeAccount(dataRowParameters.getIncomeAccount());
        billingDetailedData.setPod(dataRowParameters.getPointOfDelivery());
        billingDetailedData.setMultiplier(dataRowParameters.getMultiplier());
        billingDetailedData.setValue(dataRowParameters.getCurrentValue());
        billingDetailedData.setTotalVolumes(dataRowParameters.getTotalVolumes());
        billingDetailedData.setUnitPrice(dataRowParameters.getUnitPrice());
        billingDetailedData.setPeriodFrom(dataRowParameters.getPeriodFrom());
        billingDetailedData.setPeriodTo(dataRowParameters.getPeriodTo());
        billingDetailedData.setPriceComponent(dataRowParameters.getPriceComponent());
        billingDetailedData.setMeasuresUnitForTotalVolumes(dataRowParameters.getUnitOfMeasureForTotalVolumes());
        billingDetailedData.setMeasureUnitForUnitPrice(dataRowParameters.getUnitOfMeasureForUnitPrice());
        if (dataRowParameters.getValueCurrencyId() != null) {
            if (checkCurrency(dataRowParameters.getValueCurrencyId(), errorMessages, "manualCreditOrDebitNoteParameters.manualCreditOrDebitNoteDetailedDataParameters.detailedDataRowParametersList[%s]".formatted(index))) {
                billingDetailedData.setValueCurrencyId(dataRowParameters.getValueCurrencyId());
            }
        }
        billingVatRateUtil.checkVatRateDetailed(dataRowParameters.getGlobalVatRate(), dataRowParameters.getVatRateId(), errorMessages, billingDetailedData, "manualCreditOrDebitNoteParameters.manualCreditOrDebitNoteDetailedDataParameters.detailedDataRowParametersList[%s]".formatted(index));
    }

    private void checkManualCreditOrDebitNoteBasicDataParameters(ManualCreditOrDebitNoteBasicDataParameters manualCreditOrDebitNoteBasicDataParameters, BillingRun billingRun, List<String> errorMassages) {

        String requestName = "manualCreditOrDebitNoteParameters.manualCreditOrDebitNoteBasicDataParameters";

        billingVatRateUtil.checkVatRateCommons(billingRun, manualCreditOrDebitNoteBasicDataParameters.isGlobalVatRate(), manualCreditOrDebitNoteBasicDataParameters.getVatRateId(), errorMassages, requestName);

        checkApplicableInterestRate(billingRun, manualCreditOrDebitNoteBasicDataParameters.getApplicableInterestRateId(), errorMassages, requestName);

        checkBank(billingRun, manualCreditOrDebitNoteBasicDataParameters.getBankId(), errorMassages, requestName);
    }

    private void checkApplicableInterestRate(BillingRun billingRun, Long applicableInterestRateId, List<String> errorMessages, String requestName) {
        if (applicableInterestRateId != null) {
            if (!interestRateRepository.existsByIdAndStatusIn(applicableInterestRateId, List.of(InterestRateStatus.ACTIVE))) {

                errorMessages.add(requestName + ".applicableInterestRateId-[applicableInterestRateId] interest rate not found");
            }
        }
        billingRun.setInterestRateId(applicableInterestRateId);
    }

    private void checkBank(BillingRun billingRun, Long bankId, List<String> errorMessages, String requestName) {
        if (bankId != null) {
            if (!bankRepository.existsByIdAndStatusIn(bankId, List.of(NomenclatureItemStatus.ACTIVE))) {
                errorMessages.add(requestName + ".bankId-[bankId] bank not found");
            }
        }
        billingRun.setBankId(bankId);
    }

    private boolean checkCurrency(Long currencyId, List<String> errorMessages, String requestName) {
        if (!currencyRepository.existsByIdAndStatus(currencyId, NomenclatureItemStatus.ACTIVE)) {
            errorMessages.add(requestName + ".currencyId-[currencyId] currency not found");
            return false;
        }
        return true;
    }

    private void validateDueDate(ManualCreditOrDebitNoteParameters creditOrDebitNoteParameters, BillingRunCommonParameters commonParameters, List<String> errorMessages) {
        if (creditOrDebitNoteParameters.getManualCreditOrDebitNoteBasicDataParameters().getDocumentType().equals(DocumentType.CREDIT_NOTE) &&
            commonParameters.getDueDate() != null) {
            errorMessages.add("commonParameters.dueDate-[dueDate] should be empty when document type is credit note;");
        }
    }

    private void createBillingInvoices(ManualCreditOrDebitNoteBasicDataParameters basicDataParameters, Long billingRunId, BillingRunCommonParameters basicParameters, List<String> errorMessages) {
        List<BillingRunInvoiceInformation> billingRunInvoiceInformationList = basicDataParameters.getBillingRunInvoiceInformationList();

        List<BillingRunInvoices> billingRunInvoices = new ArrayList<>();
        List<Long> invoiceIds = billingRunInvoiceInformationList.stream().map(BillingRunInvoiceInformation::getInvoiceId).toList();
        if (invoiceIds.isEmpty()) {
            return;
        }
        if (!billingRunInvoicesRepository.invoicesHaveSameCustomer(invoiceIds)) {
            throw new ClientException("Invoice customers doesn't match", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long firstInvoiceId = invoiceIds.get(0);
        Set<Long> available = invoiceRepository.availableInvoicesForBilling(firstInvoiceId);
        InvoiceObjectType invoiceObjectType = null;
        int i = 0;
        for (BillingRunInvoiceInformation billingRunInvoiceInformation : billingRunInvoiceInformationList) {
            Long invoiceId = billingRunInvoiceInformation.getInvoiceId();

            Invoice invoice = invoiceRepository
                    .findById(invoiceId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Invoice not found with ID: %s".formatted(invoiceId)));
            if (!available.contains(invoiceId) && i!=0) {
                errorMessages.add("manualCreditOrDebitNoteBasicDataParameters.billingRunInvoiceInformationList.invoiceId[%s]-Invoice has different type then first!;".formatted(i));
            }
            InvoiceObjectType currentInvoiceObjectType = InvoiceObjectType.defineInvoiceObjectType(invoice);
            if (invoiceObjectType == null) {
                invoiceObjectType = currentInvoiceObjectType;


                if (currentInvoiceObjectType.equals(InvoiceObjectType.GOODS_ORDER) || currentInvoiceObjectType.equals(InvoiceObjectType.SERVICE_ORDER)) {

                    if (basicParameters.getInvoiceDueDate() != null && ACCORDING_TO_THE_CONTRACT.equals(basicParameters.getInvoiceDueDate())) {
                        errorMessages.add("commonParameters.invoiceDate-Invoice due date can not be according to the contract!;");
                    }
                    if (SendingAnInvoice.ACCORDING_TO_THE_CONTRACT.equals(basicParameters.getSendingAnInvoice())) {
                        errorMessages.add("commonParameters.sendingAnInvoice-Sending an invoice can not be according to the contract!;");
                    }

                }

            }

            if (invoice.getInvoiceDocumentType().equals(InvoiceDocumentType.INVOICE)) {
                BillingRunInvoices billingRunInvoice = new BillingRunInvoices();
                billingRunInvoice.setInvoiceId(billingRunInvoiceInformation.getInvoiceId());
                billingRunInvoice.setBillingId(billingRunId);
                billingRunInvoice.setStatus(EntityStatus.ACTIVE);

                billingRunInvoices.add(billingRunInvoice);
            } else {
                errorMessages.add("Invoice with id %s doesn't have \"INVOICE\" as document type".formatted(invoiceId));
            }
            i++;
        }
        if(InvoiceObjectType.ONLY_CUSTOMER.equals(invoiceObjectType)){
            validateDataParameters(basicDataParameters,errorMessages);
        }
        if (InvoiceObjectType.PRODUCT_CONTRACT.equals(invoiceObjectType) && !billingRunInvoicesRepository.invoicesHaveSameType(invoiceIds)) {
            throw new ClientException("Invoice billing groups doesn't match", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        billingRunInvoicesRepository.saveAll(billingRunInvoices);

    }

    private void validateDataParameters(ManualCreditOrDebitNoteBasicDataParameters basicDataParameters, List<String> errorMessages) {
        if(!basicDataParameters.isNumberOfIncomeAccountManual()){
            errorMessages.add("numberOfIncomeAccountManual-Income account number should be manually selected;");
        }
        if(!basicDataParameters.isCostCenterControllingOrderManual()){
            errorMessages.add("costCenterControllingOrderManual-Cost centering order should be manually selected;");
        }
        if(!basicDataParameters.isVatRateManual()){
            errorMessages.add("vatRateManual-Vat rate should be manually selected;");
        }
        if(!basicDataParameters.isApplicableInterestRateManual()){
            errorMessages.add("applicableInterestRateManual-Application interest rate should be manually selected;");
        }
    }

    public ManualCreditOrDebitNoteBillingRunParametersResponse getManualCreditOrDebitNoteParameters(BillingRun
                                                                                                            billingRun) {
        ManualCreditOrDebitNoteBillingRunParametersResponse response = new ManualCreditOrDebitNoteBillingRunParametersResponse();
        response.setManualCreditOrDebitNoteBasicDataParameters(manualCreditOrDebitNoteMapperService.mapBasicDataParameters(billingRun));
        response.setManualCreditOrDebitNoteSummaryDataParameters(manualInvoiceMapperService.mapSummaryDataParameters(billingRun));
        response.setManualCreditOrDebitNoteDetailedDataParameters(manualInvoiceMapperService.mapDetailedDataParameters(billingRun));
        return response;
    }

}
