package bg.energo.phoenix.service.billing.billingRun.manualInvoice;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingDetailedData;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRunBillingGroup;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingSummaryData;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.enums.billing.billings.ContractOrderType;
import bg.energo.phoenix.model.enums.billing.billings.CustomerContractOrderType;
import bg.energo.phoenix.model.enums.billing.billings.ManualInvoiceType;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.billing.billingRun.create.BillingRunCreateRequest;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.*;
import bg.energo.phoenix.model.request.billing.billingRun.edit.manualInvoice.*;
import bg.energo.phoenix.model.request.billing.communicationData.BillingCommunicationDataListRequest;
import bg.energo.phoenix.model.response.billing.billingRun.DetailedDataRowParametersResponse;
import bg.energo.phoenix.model.response.billing.billingRun.ManualInvoiceTemplateContent;
import bg.energo.phoenix.model.response.billing.billingRun.SummaryDataRowParametersResponse;
import bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.ManualInvoiceBillingRunParametersResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.customer.CustomerContractOrderResponse;
import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.process.model.entity.Template;
import bg.energo.phoenix.process.repository.TemplateRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingDetailedDataRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunBillingGroupRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingSummaryDataRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import bg.energo.phoenix.service.billing.billingRun.BillingRunCommonService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.billing.BillingVatRateUtil;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import bg.energo.phoenix.util.epb.EPBExcelUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import bg.energo.phoenix.util.mi.ExcelMapperForMIFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.model.enums.billing.billings.ManualInvoiceType.DETAILED_INVOICE;
import static bg.energo.phoenix.model.enums.billing.billings.ManualInvoiceType.STANDARD_INVOICE;
import static bg.energo.phoenix.util.epb.EPBFinalFields.MANUAL_INVOICE_DETAILED_TEMPLATE_ID;
import static bg.energo.phoenix.util.epb.EPBFinalFields.MANUAL_INVOICE_STANDARD_TEMPLATE_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualInvoiceService {

    private final VatRateRepository vatRateRepository;

    private final InterestRateRepository interestRateRepository;

    private final BankRepository bankRepository;

    private final CurrencyRepository currencyRepository;

    private final CustomerDetailsRepository customerDetailsRepository;

    private final ProductContractRepository productContractRepository;

    private final ProductContractDetailsRepository productContractDetailsRepository;

    private final ServiceContractsRepository serviceContractsRepository;

    private final ServiceContractDetailsRepository serviceContractDetailsRepository;

    private final ContractBillingGroupRepository billingGroupRepository;

    private final BillingRunBillingGroupRepository billingRunBillingGroupRepository;

    private final GoodsOrderRepository goodsOrderRepository;

    private final ServiceOrderRepository serviceOrderRepository;

    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;

    private final BillingSummaryDataRepository billingSummaryDataRepository;

    private final BillingDetailedDataRepository billingDetailedDataRepository;

    private final ExcelMapperForMIFields excelMapper;

    private final TemplateRepository templateRepository;

    private final FileService fileService;

    private final CustomerRepository customerRepository;

    private final ManualInvoiceMapperService manualInvoiceMapperService;

    private final BillingRunCommonService commonService;

    private final BillingVatRateUtil billingVatRateUtil;

    /**
     * Maps the manual invoice parameters to the billing run.
     *
     * @param billingRunCreateRequest the request containing the manual invoice parameters
     * @param billingRun              the billing run to be updated
     * @param errorMessages           the list to store any error messages
     */
    @Transactional
    public void mapManualInvoiceParameters(BillingRunCreateRequest billingRunCreateRequest, BillingRun billingRun, List<String> errorMessages) {
        ManualInvoiceParameters manualInvoiceParameters = billingRunCreateRequest.getManualInvoiceParameters();
        mapManualInvoiceBasicDataParameters(manualInvoiceParameters.getManualInvoiceBasicDataParameters(), billingRun, errorMessages, false);
        mapManualInvoiceSummaryDataParameters(manualInvoiceParameters.getManualInvoiceSummaryDataParameters(), billingRun, errorMessages);
        if (manualInvoiceParameters.getManualInvoiceSummaryDataParameters().getManualInvoiceType().equals(DETAILED_INVOICE) &&
            manualInvoiceParameters.getManualInvoiceDetailedDataParameters() != null) {
            mapManualInvoiceDetailedDataParameters(manualInvoiceParameters.getManualInvoiceDetailedDataParameters(), billingRun, errorMessages);
        }
    }

    /**
     * Maps the basic data parameters of a manual invoice to the billing run.
     *
     * @param manualInvoiceBasicDataParameters the basic data parameters of the manual invoice
     * @param billingRun                       the billing run to be updated
     * @param errorMessages                    the list to store any error messages
     * @param isUpdate                         indicates whether this is an update operation
     */
    private void mapManualInvoiceBasicDataParameters(ManualInvoiceBasicDataParameters manualInvoiceBasicDataParameters, BillingRun billingRun, List<String> errorMessages, boolean isUpdate) {
        checkManualInvoiceBasicDataParameters(manualInvoiceBasicDataParameters, billingRun, errorMessages, isUpdate);
        billingRun.setBasisForIssuing(manualInvoiceBasicDataParameters.getBasisForIssuing());
        billingRun.setNumberOfIncomeAccount(manualInvoiceBasicDataParameters.getNumberOfIncomeAccount());
        billingRun.setCostCenterControllingOrder(manualInvoiceBasicDataParameters.getCostCenterControllingOrder());
        billingRun.setDirectDebit(!manualInvoiceBasicDataParameters.isDirectDebitManual() ? null : manualInvoiceBasicDataParameters.isDirectDebit());
        billingRun.setIban(manualInvoiceBasicDataParameters.getIban());
        billingRun.setPrefixType(manualInvoiceBasicDataParameters.getPrefixType());
    }

    /**
     * Maps the summary data parameters of a manual invoice to the billing run.
     *
     * @param manualInvoiceSummaryDataParameters the summary data parameters of the manual invoice
     * @param billingRun                         the billing run to be updated
     * @param errorMessages                      the list to store any error messages
     */
    private void mapManualInvoiceSummaryDataParameters(ManualInvoiceSummaryDataParameters manualInvoiceSummaryDataParameters, BillingRun billingRun, List<String> errorMessages) {
        billingRun.setManualInvoiceType(manualInvoiceSummaryDataParameters.getManualInvoiceType());
        if (!CollectionUtils.isEmpty(manualInvoiceSummaryDataParameters.getSummaryDataRowList())) {
            int index = 0;
            List<BillingSummaryData> summaryDataList = new ArrayList<>();
            for (SummaryDataRowParameters dataRowParameters : manualInvoiceSummaryDataParameters.getSummaryDataRowList()) {
                summaryDataList.add(createBillingSummaryDataParameters(billingRun, dataRowParameters, errorMessages, index));
                index++;
            }
            if (errorMessages.isEmpty()) {
                billingSummaryDataRepository.saveAll(summaryDataList);
            }
        }
    }

    /**
     * Creates a new BillingSummaryData object and sets its properties based on the provided SummaryDataRowParameters.
     *
     * @param billingRun        the BillingRun object associated with the BillingSummaryData
     * @param dataRowParameters the SummaryDataRowParameters object containing the data to be set
     * @param errorMessages     a list to store any error messages that occur during the process
     * @param index             the index of the current SummaryDataRowParameters in the list
     * @return the created BillingSummaryData object
     */
    private BillingSummaryData createBillingSummaryDataParameters(BillingRun billingRun, SummaryDataRowParameters dataRowParameters, List<String> errorMessages, int index) {
        BillingSummaryData billingSummaryData = new BillingSummaryData();
        billingSummaryData.setBillingId(billingRun.getId());
        setSummaryDataParameters(billingSummaryData, dataRowParameters);
        if (dataRowParameters.getValueCurrencyId() != null &&
            (checkCurrency(dataRowParameters.getValueCurrencyId(), errorMessages, "manualInvoiceSummaryDataParameters.summaryDataRowList[%s]".formatted(index)))) {
            billingSummaryData.setValueCurrencyId(dataRowParameters.getValueCurrencyId());
        }
        billingSummaryData.setMeasureUnitForUnitPrice(dataRowParameters.getUnitOfMeasureForUnitPrice());
        billingVatRateUtil.checkVatRateSummary(dataRowParameters.getGlobalVatRate(), dataRowParameters.getVatRateId(), errorMessages, billingSummaryData, "manualInvoiceSummaryDataParameters.summaryDataRowList[%s]".formatted(index));
        return billingSummaryData;
    }

    /**
     * Maps the detailed data parameters of a manual invoice to the billing run.
     *
     * @param manualInvoiceDetailedDataParameters the detailed data parameters of the manual invoice
     * @param billingRun                          the billing run to be updated
     * @param errorMessages                       the list to store any error messages
     */
    private void mapManualInvoiceDetailedDataParameters(ManualInvoiceDetailedDataParameters manualInvoiceDetailedDataParameters, BillingRun billingRun, List<String> errorMessages) {
        if (!CollectionUtils.isEmpty(manualInvoiceDetailedDataParameters.getDetailedDataRowParametersList())) {
            int index = 0;
            List<BillingDetailedData> detailedDataList = new ArrayList<>();
            for (DetailedDataRowParameters dataRowParameters : manualInvoiceDetailedDataParameters.getDetailedDataRowParametersList()) {
                detailedDataList.add(createBillingDetailedData(dataRowParameters, billingRun, errorMessages, index));
                index++;
            }
            if (errorMessages.isEmpty()) {
                billingDetailedDataRepository.saveAll(detailedDataList);
            }
        }
    }

    /**
     * Creates a new BillingDetailedData object and sets its properties based on the provided DetailedDataRowParameters.
     *
     * @param dataRowParameters the DetailedDataRowParameters object containing the data to be set
     * @param billingRun        the BillingRun object associated with the BillingDetailedData
     * @param errorMessages     a list to store any error messages that occur during the process
     * @param index             the index of the current DetailedDataRowParameters in the list
     * @return the created BillingDetailedData object
     */
    private BillingDetailedData createBillingDetailedData(DetailedDataRowParameters dataRowParameters, BillingRun billingRun, List<String> errorMessages, int index) {
        BillingDetailedData billingDetailedData = new BillingDetailedData();
        billingDetailedData.setBillingId(billingRun.getId());
        setDetailedDataParameters(billingDetailedData, dataRowParameters);
        if (dataRowParameters.getValueCurrencyId() != null) {
            if (checkCurrency(dataRowParameters.getValueCurrencyId(), errorMessages, "manualInvoiceParameters.manualInvoiceDetailedDataParameters.detailedDataRowParametersList[%s]".formatted(index))) {
                billingDetailedData.setValueCurrencyId(dataRowParameters.getValueCurrencyId());
            }
        }
        billingVatRateUtil.checkVatRateDetailed(dataRowParameters.getGlobalVatRate(), dataRowParameters.getVatRateId(), errorMessages, billingDetailedData, "manualInvoiceParameters.manualInvoiceDetailedDataParameters.detailedDataRowParametersList[%s]".formatted(index));

        return billingDetailedData;
    }

    /**
     * Checks and sets the basic data parameters for a manual invoice in a billing run.
     *
     * @param manualInvoiceBasicDataParameters the basic data parameters for the manual invoice
     * @param billingRun                       the billing run to be updated
     * @param errorMassages                    the list to store any error messages
     * @param isUpdate                         a flag indicating whether this is an update operation
     */
    private void checkManualInvoiceBasicDataParameters(ManualInvoiceBasicDataParameters manualInvoiceBasicDataParameters, BillingRun billingRun, List<String> errorMassages, boolean isUpdate) {

        String requestName = "manualInvoiceParameters.manualInvoiceBasicDataParameters";

        billingVatRateUtil.checkVatRateCommons(billingRun, manualInvoiceBasicDataParameters.isGlobalVatRate(), manualInvoiceBasicDataParameters.getVatRateId(), errorMassages, requestName);

        checkApplicableInterestRate(billingRun, manualInvoiceBasicDataParameters.getApplicableInterestRateId(), errorMassages, requestName);

        checkCustomerDetails(manualInvoiceBasicDataParameters.getCustomerDetailId(), errorMassages, billingRun, requestName);

        checkBank(billingRun, manualInvoiceBasicDataParameters.getBankId(), errorMassages, requestName);

        billingRun.setServiceOrderId(null);
        billingRun.setProductContractId(null);
        billingRun.setServiceContractId(null);
        billingRun.setGoodsOrderId(null);

        if (manualInvoiceBasicDataParameters.getContractOrderId() != null) {
            checkContractOrderForManualInvoice(billingRun, manualInvoiceBasicDataParameters.getContractOrderType(), manualInvoiceBasicDataParameters.getContractOrderId(), errorMassages);
        }

        checkBillingGroupForManualInvoice(billingRun, manualInvoiceBasicDataParameters.getBillingGroupId(), manualInvoiceBasicDataParameters.getContractOrderId(), manualInvoiceBasicDataParameters.getContractOrderType(), errorMassages, isUpdate);
        commonService.checkInvoiceCommunicationData(manualInvoiceBasicDataParameters.getInvoiceCommunicationDataId(), manualInvoiceBasicDataParameters.getContractOrderId(), manualInvoiceBasicDataParameters.getCustomerDetailId(), manualInvoiceBasicDataParameters.getContractOrderType(), billingRun, errorMassages, "manualInvoiceBasicDataParameters");
    }


    /**
     * Checks and sets the applicable interest rate for a billing run.
     *
     * @param billingRun               the billing run to be updated
     * @param applicableInterestRateId the ID of the applicable interest rate
     * @param errorMessages            the list to store any error messages
     * @param requestName              the name of the request
     */
    private void checkApplicableInterestRate(BillingRun billingRun, Long applicableInterestRateId, List<String> errorMessages, String requestName) {
        if (applicableInterestRateId != null) {
            if (interestRateRepository.existsByIdAndStatusIn(applicableInterestRateId, List.of(InterestRateStatus.ACTIVE))) {
                billingRun.setInterestRateId(applicableInterestRateId);
            } else {
                errorMessages.add(requestName + ".applicableInterestRateId-[applicableInterestRateId] interest rate not found");
            }
        } else {
            billingRun.setInterestRateId(null);
        }
    }

    /**
     * Sets the bank ID for the billing run to null if no bank ID is provided.
     *
     * @param billingRun    the billing run to be updated
     * @param bankId        the ID of the bank, or null if no bank is specified
     * @param errorMessages the list to store any error messages
     * @param requestName   the name of the request
     */
    private void checkBank(BillingRun billingRun, Long bankId, List<String> errorMessages, String requestName) {
        if (bankId != null) {
            if (bankRepository.existsByIdAndStatusIn(bankId, List.of(NomenclatureItemStatus.ACTIVE))) {
                billingRun.setBankId(bankId);
            } else {
                errorMessages.add(requestName + ".bankId-[bankId] bank not found");
            }
        } else {
            billingRun.setBankId(null);
        }
    }

    /**
     * Checks the customer details for a billing run and sets the customer detail ID on the billing run if the customer detail is found.
     *
     * @param customerDetailId the ID of the customer detail to check
     * @param errorMessages    the list to store any error messages
     * @param billingRun       the billing run to update
     * @param requestName      the name of the request
     */
    private void checkCustomerDetails(Long customerDetailId, List<String> errorMessages, BillingRun billingRun, String requestName) {
        if (!customerDetailsRepository.existsByDetailIdAndCustomerStatus(customerDetailId, List.of(CustomerStatus.ACTIVE))) {
            errorMessages.add(requestName + ".customerDetailId-[customerDetailId] customer detail not found");
        } else {
            billingRun.setCustomerDetailId(customerDetailId);
        }
    }

    /**
     * Checks if the specified currency ID exists and is active in the currency repository.
     *
     * @param currencyId    The ID of the currency to check.
     * @param errorMessages The list to store any error messages.
     * @param requestName   The name of the request.
     * @return True if the currency is found and active, false otherwise.
     */
    private boolean checkCurrency(Long currencyId, List<String> errorMessages, String requestName) {
        if (!currencyRepository.existsByIdAndStatus(currencyId, NomenclatureItemStatus.ACTIVE)) {
            errorMessages.add(requestName + ".currencyId-[currencyId] currency not found");
            return false;
        }
        return true;
    }

    /**
     * Adds an error message to the list of error messages when the specified billing group is not found.
     *
     * @param billingGroupId The ID of the billing group that was not found.
     * @param errorMessages  The list of error messages to add the new error message to.
     */
    private void checkBillingGroupForManualInvoice(BillingRun billingRun, Long billingGroupId, Long contractOrderId, ContractOrderType contractOrderType, List<String> errorMessages, boolean isUpdate) {
        if (isUpdate) {
            Optional<BillingRunBillingGroup> billingRunBillingGroupOptional = billingRunBillingGroupRepository.findByBillingRunIdAndStatus(billingRun.getId(), EntityStatus.ACTIVE);
            if (billingRunBillingGroupOptional.isPresent()) {
                BillingRunBillingGroup billingRunBillingGroup = billingRunBillingGroupOptional.get();
                billingRunBillingGroup.setStatus(EntityStatus.DELETED);
                billingRunBillingGroupRepository.save(billingRunBillingGroup);
            }
        }
        if (contractOrderId != null && contractOrderType.equals(ContractOrderType.PRODUCT_CONTRACT) && (billingGroupId != null)) {
            Optional<ContractBillingGroup> billingGroup = billingGroupRepository.findByIdAndStatusIn(billingGroupId, List.of(EntityStatus.ACTIVE));
            if (billingGroup.isPresent()) {
                if (!billingGroup.get().getContractId().equals(contractOrderId)) {
                    errorMessages.add(String.format("manualInvoiceBasicDataParameters.billingGroupId-[billingGroupId] billing group with id %s is not bound to product contract with id %s", billingGroupId, contractOrderId));
                } else {
                    BillingRunBillingGroup billingRunBillingGroup = createBillingRunBillingGroup(billingGroupId, billingRun.getId());
                    billingRunBillingGroupRepository.save(billingRunBillingGroup);
                }
            } else {
                errorMessages.add("manualInvoiceBasicDataParameters.billingGroupId-[billingGroupId] not found billing group with id %s".formatted(billingGroupId));
            }
        }
    }

    /**
     * Creates a new {@link BillingRunBillingGroup} instance with the specified billing group ID and billing run ID, and sets its status to {@link EntityStatus#ACTIVE}.
     *
     * @param billingGroupId The ID of the billing group to associate with the new billing run billing group.
     * @param billingRunId   The ID of the billing run to associate with the new billing run billing group.
     * @return A new {@link BillingRunBillingGroup} instance with the specified properties.
     */
    private BillingRunBillingGroup createBillingRunBillingGroup(Long billingGroupId, Long billingRunId) {
        BillingRunBillingGroup billingRunBillingGroup = new BillingRunBillingGroup();
        billingRunBillingGroup.setBillingGroupId(billingGroupId);
        billingRunBillingGroup.setBillingRunId(billingRunId);
        billingRunBillingGroup.setStatus(EntityStatus.ACTIVE);
        return billingRunBillingGroup;
    }

    /**
     * Checks the contract order type and associated contract order ID for a manual invoice.
     * Adds error messages to the provided list if the contract order is not found.
     * Updates the billing run with the appropriate contract order ID based on the contract order type.
     *
     * @param billingRun        The billing run to update with the contract order ID.
     * @param contractOrderType The type of contract order (e.g. service contract, product contract, goods order, service order).
     * @param contractOrderId   The ID of the contract order.
     * @param errorMessages     The list of error messages to add to if the contract order is not found.
     */
    private void checkContractOrderForManualInvoice(BillingRun billingRun, ContractOrderType contractOrderType, Long contractOrderId, List<String> errorMessages) {
        switch (contractOrderType) {
            case SERVICE_CONTRACT -> {
                if (!serviceContractsRepository.existsByIdAndStatusIn(contractOrderId, List.of(EntityStatus.ACTIVE))) {
                    errorMessages.add("manualInvoiceBasicDataParameters.contractOrderId-[contractOrderId] service contract detail not found");
                } else {
                    billingRun.setServiceContractId(contractOrderId);
                }
            }
            case PRODUCT_CONTRACT -> {
                if (!productContractRepository.existsByIdAndStatusIn(contractOrderId, List.of(ProductContractStatus.ACTIVE))) {
                    errorMessages.add("manualInvoiceBasicDataParameters.contractOrderId-[contractOrderId] product contract detail not found");
                } else {
                    billingRun.setProductContractId(contractOrderId);
                }
            }
            case GOODS_ORDER -> {
                if (!goodsOrderRepository.existsByIdAndStatusIn(contractOrderId, List.of(EntityStatus.ACTIVE))) {
                    errorMessages.add("manualInvoiceBasicDataParameters.contractOrderId-[contractOrderId] goods order not found");
                } else {
                    billingRun.setGoodsOrderId(contractOrderId);
                }
            }
            case SERVICE_ORDER -> {
                if (!serviceOrderRepository.existsByIdAndStatusIn(contractOrderId, List.of(EntityStatus.ACTIVE))) {
                    errorMessages.add("manualInvoiceBasicDataParameters.contractOrderId-[contractOrderId] service order not found");
                } else {
                    billingRun.setServiceOrderId(contractOrderId);
                }
            }
        }
    }

    /**
     * Validates the content of the provided file, maps the data to a response object, and returns the response.
     *
     * @param file The file to be validated and mapped.
     * @param type The type of manual invoice (standard or detailed).
     * @return The response object containing the mapped data from the file.
     */
    public ManualInvoiceImportResponse importFile(MultipartFile file, ManualInvoiceType type) {
        ManualInvoiceImportResponse response = new ManualInvoiceImportResponse();
        validateFileContentAndMapToResponse(file, type, response);
        return response;
    }

    /**
     * Validates the content of the provided file, maps the data to a response object, and returns the response.
     *
     * @param file     The file to be validated and mapped.
     * @param type     The type of manual invoice (standard or detailed).
     * @param response The response object to be populated with the mapped data.
     */
    private void validateFileContentAndMapToResponse(MultipartFile file, ManualInvoiceType type, ManualInvoiceImportResponse response) {
        excelMapper.validateFileFormat(file);
        var templatePath = templateRepository
                .findById(type.equals(ManualInvoiceType.DETAILED_INVOICE) ?
                        MANUAL_INVOICE_DETAILED_TEMPLATE_ID :
                        MANUAL_INVOICE_STANDARD_TEMPLATE_ID)
                .orElseThrow(() -> new DomainEntityNotFoundException("Template for manual invoice not found;"));

        log.info("template path is ->>>> :" + templatePath.getFileUrl());
        EPBExcelUtils.validateFileContent(file, fileService.downloadFile(templatePath.getFileUrl()).getByteArray(),
                type.equals(DETAILED_INVOICE) ? 2 : 1);
        if (type.equals(STANDARD_INVOICE)) {
            mapStandardParameters(file, response);
        } else {
            mapStandardParameters(file, response);
            mapDetailedParameters(file, response);
        }
    }

    /**
     * Maps the detailed parameters from the provided file to a response object.
     *
     * @param file The file containing the detailed parameters.
     * @param response The response object to be populated with the mapped data.
     */
    private void mapDetailedParameters(MultipartFile file, ManualInvoiceImportResponse response) {
        List<DetailedDataRowParametersResponse> detailedDataRowParametersList = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(1);

            Iterator<Row> iterator = sheet.iterator();

            if (iterator.hasNext()) iterator.next();

            while (iterator.hasNext()) {
                Row row = iterator.next();

                String priceComponent = EPBExcelUtils.getStringValue(0, row);
                String pod = EPBExcelUtils.getStringValue(1, row);
                LocalDate periodFrom = EPBExcelUtils.getLocalDateValue(2, row);
                LocalDate periodTo = EPBExcelUtils.getLocalDateValue(3, row);
                String meter = EPBExcelUtils.getStringValue(4, row);
                BigDecimal newMeterReading = EPBExcelUtils.getBigDecimalValue(5, row);
                BigDecimal oldMeterReading = EPBExcelUtils.getBigDecimalValue(6, row);
                BigDecimal differences = EPBExcelUtils.getBigDecimalValue(7, row);
                BigDecimal multiplier = EPBExcelUtils.getBigDecimalValue(8, row);
                BigDecimal correction = EPBExcelUtils.getBigDecimalValue(9, row);
                BigDecimal deducted = EPBExcelUtils.getBigDecimalValue(10, row);
                BigDecimal totalVolumes = EPBExcelUtils.getBigDecimalValue(11, row);
                String unitOfMeasuresForTotalVolumes = EPBExcelUtils.getStringValue(12, row);
                BigDecimal unitPrice = EPBExcelUtils.getBigDecimalValue(13, row);
                String unitOfMeasureForUnitPrice = EPBExcelUtils.getStringValue(14, row);
                BigDecimal value = EPBExcelUtils.getBigDecimalValue(15, row);
                String currencyName = EPBExcelUtils.getStringValue(16, row);
                Boolean isGlobalVatRate = null;
                CacheObject currency = null;
                VatRate vatRate = null;
                if (currencyName != null) {
                    currency = currencyRepository.getCacheObjectByNameAndStatus(currencyName, NomenclatureItemStatus.ACTIVE)
                            .orElseThrow(() -> new IllegalArgumentsProvidedException("detailedData[%s].unit_of_measures_for_value-currency with presented name: [%s] not found;".formatted(row.getRowNum(), currencyName)));
                }
                String incomeAccount = EPBExcelUtils.getStringValue(17, row);
                String costCenter = EPBExcelUtils.getStringValue(18, row);
                String vatRateName = EPBExcelUtils.getStringValue(19, row);
                if (vatRateName != null) {
                    vatRate = vatRateRepository.findByNameAndStatusIn(vatRateName, List.of(NomenclatureItemStatus.ACTIVE))
                            .orElseThrow(() -> new IllegalArgumentsProvidedException("detailedData[%s].vat_rate-vat rate with presented name: [%s] not found;".formatted(row.getRowNum(), vatRateName)));
                    isGlobalVatRate = vatRate.getGlobalVatRate();
                }
                DetailedDataRowParametersResponse rowParameters = new DetailedDataRowParametersResponse(
                        priceComponent,
                        pod,
                        periodFrom,
                        periodTo,
                        meter,
                        newMeterReading,
                        oldMeterReading,
                        differences,
                        multiplier,
                        correction,
                        deducted,
                        totalVolumes,
                        unitOfMeasuresForTotalVolumes,
                        unitPrice,
                        unitOfMeasureForUnitPrice,
                        value,
                        currency != null ? new ShortResponse(currency.getId(), currency.getName()) : null,
                        incomeAccount,
                        costCenter,
                        vatRate != null ? new VatRateResponse(vatRate) : null,
                        isGlobalVatRate
                );
                validateDetailedDataRowParameters(rowParameters, row.getRowNum() + 1);
                detailedDataRowParametersList.add(rowParameters);
            }
            response.setDetailedDataRowParametersList(detailedDataRowParametersList);
        } catch (IllegalArgumentsProvidedException e) {
            log.error("Illegal arguments provided in file", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception handled while trying to parse uploaded template;", e);
            throw new ClientException("Exception handled while trying to parse uploaded template;", APPLICATION_ERROR);
        }

    }

    /**
     * Maps the standard parameters from an uploaded file to a list of `SummaryDataRowParametersResponse` objects.
     *
     * This method reads an Excel file, iterates through its rows, and extracts various parameters such as price component, total volumes, unit price, value, currency, income account, cost center, and VAT rate. It then validates these parameters and adds them to the `summaryDataRowParametersList`.
     *
     * @param file The uploaded Excel file.
     * @param response The `ManualInvoiceImportResponse` object to which the summary data row parameters will be added.
     */
    private void mapStandardParameters(MultipartFile file, ManualInvoiceImportResponse response) {
        List<SummaryDataRowParametersResponse> summaryDataRowParametersList = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            Iterator<Row> iterator = sheet.iterator();

            if (iterator.hasNext()) iterator.next();

            while (iterator.hasNext()) {
                Row row = iterator.next();

                String priceComponentOrPriceComponentGroupOrItem = EPBExcelUtils.getStringValue(0, row);
                BigDecimal totalVolumes = EPBExcelUtils.getBigDecimalValue(1, row);
                String unitOfMeasuresForTotalVolumes = EPBExcelUtils.getStringValue(2, row);
                BigDecimal unitPrice = EPBExcelUtils.getBigDecimalValue(3, row);
                String unitOfMeasureForUnitPrice = EPBExcelUtils.getStringValue(4, row);
                BigDecimal value = EPBExcelUtils.getBigDecimalValue(5, row);
                String currencyName = EPBExcelUtils.getStringValue(6, row);
                CacheObject currency = null;
                Boolean isGlobalVatRate = null;
                VatRate vatRate = null;
                if (currencyName != null) {
                    currency = currencyRepository.getCacheObjectByNameAndStatus(currencyName, NomenclatureItemStatus.ACTIVE)
                            .orElseThrow(() -> new IllegalArgumentsProvidedException("summaryData[%s].unit_of_measures_for_value-currency with presented name: [%s] not found;".formatted(row.getRowNum(), currencyName)));
                }
                String incomeAccount = EPBExcelUtils.getStringValue(7, row);
                String costCenter = EPBExcelUtils.getStringValue(8, row);
                String vatRateName = EPBExcelUtils.getStringValue(9, row);
                if (vatRateName != null) {
                    vatRate = vatRateRepository.findByNameAndStatusIn(vatRateName, List.of(NomenclatureItemStatus.ACTIVE))
                            .orElseThrow(() -> new IllegalArgumentsProvidedException("summaryData[%s].vat_rate-vat rate with presented name: [%s] not found;".formatted(row.getRowNum(), vatRateName)));
                    isGlobalVatRate = vatRate.getGlobalVatRate();
                }

                SummaryDataRowParametersResponse rowParameters = new SummaryDataRowParametersResponse(
                        priceComponentOrPriceComponentGroupOrItem,
                        totalVolumes,
                        unitOfMeasuresForTotalVolumes,
                        unitPrice,
                        unitOfMeasureForUnitPrice,
                        value,
                        currency != null ? new ShortResponse(currency.getId(), currency.getName()) : null,
                        incomeAccount,
                        costCenter,
                        vatRate != null ? new VatRateResponse(vatRate) : null,
                        isGlobalVatRate
                );
                validateSummaryDataRowParameters(rowParameters, row.getRowNum() + 1);
                summaryDataRowParametersList.add(rowParameters);
            }
            response.setSummaryDataRowList(summaryDataRowParametersList);
        } catch (IllegalArgumentsProvidedException e) {
            log.error("Illegal arguments provided in file", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception handled while trying to parse uploaded template;", e);
            throw new ClientException("Exception handled while trying to parse uploaded template;", APPLICATION_ERROR);
        }
    }

    /**
     * Validates the parameters of a summary data row in the manual invoice process.
     *
     * @param rowParameters the parameters of the summary data row to validate
     * @param i the index of the summary data row
     * @throws IllegalArgumentsProvidedException if any of the parameters are invalid
     */
    private void validateSummaryDataRowParameters(SummaryDataRowParametersResponse rowParameters, int i) {
        double maxValForTotalVolumes = 999999999.99999999;


        double maxVal = 999999999.9999999999;
        BigDecimal minValue = BigDecimal.valueOf(-maxVal);
        BigDecimal maxValue = BigDecimal.valueOf(maxVal);

        if (rowParameters.getPriceComponentOrPriceComponentGroupOrItem() != null &&
            (notBetweenSizes(1, 1024, rowParameters.getPriceComponentOrPriceComponentGroupOrItem()))) {
            throw new IllegalArgumentsProvidedException("summaryData[%s].price_component_or_price_component_group_or_item- price component or price component group or item should be between size %s and %s;".formatted(i, 1, 1024));
        }
        if (rowParameters.getTotalVolumes() != null &&
            (notInRange(BigDecimal.valueOf(-maxValForTotalVolumes), BigDecimal.valueOf(maxValForTotalVolumes), rowParameters.getTotalVolumes()))) {
            throw new IllegalArgumentsProvidedException("summaryData[%s].total_volumes- total volumes should be in range %s and %s;".formatted(i, -maxValForTotalVolumes, maxValForTotalVolumes));
        }
        if (rowParameters.getUnitOfMeasuresForTotalVolumes() != null &&
            (notBetweenSizes(1, 512, rowParameters.getUnitOfMeasuresForTotalVolumes()))) {
            throw new IllegalArgumentsProvidedException("summaryData[%s].unit_of_measures_for_total_volumes- unit of measures for total volumes should be between size %s and %s;".formatted(i, 1, 512));
        }
        if (rowParameters.getUnitPrice() != null &&
            (notInRange(minValue, maxValue, rowParameters.getUnitPrice()))) {
            throw new IllegalArgumentsProvidedException("summaryData[%s].unit_price- unit price should be in range %s and %s;".formatted(i, -maxVal, maxVal));
        }
        if (rowParameters.getUnitOfMeasureForUnitPrice() != null &&
            (notBetweenSizes(1, 512, rowParameters.getUnitOfMeasureForUnitPrice()))) {
            throw new IllegalArgumentsProvidedException("summaryData[%s].unit_of_measures_for_unit_price- unit of measures for unit price should be between size %s and %s;".formatted(i, 1, 512));
        }
        if (rowParameters.getValue() != null &&
            (notInRange(minValue, maxValue, rowParameters.getValue()))) {
            throw new IllegalArgumentsProvidedException("summaryData[%s].value- value should be in range %s and %s;".formatted(i, -maxVal, maxVal));
        }
        if (rowParameters.getIncomeAccount() != null &&
            (notBetweenSizes(1, 32, rowParameters.getIncomeAccount()))) {
            throw new IllegalArgumentsProvidedException("summaryData[%s].income_account- income account should be between size %s and %s;".formatted(i, 1, 32));
        }
        if (rowParameters.getCostCenter() != null &&
            (notBetweenSizes(1, 32, rowParameters.getCostCenter()))) {
            throw new IllegalArgumentsProvidedException("summaryData[%s].cost_center- cost center should be between size %s and %s;".formatted(i, 1, 32));
        }
    }

    /**
     * Validates the parameters of a detailed data row in the manual invoice process.
     *
     * @param rowParameters the parameters of the detailed data row to validate
     * @param i the index of the detailed data row
     * @throws IllegalArgumentsProvidedException if any of the parameters are invalid
     */
    private void validateDetailedDataRowParameters(DetailedDataRowParametersResponse rowParameters, int i) {
        if (rowParameters.getPriceComponent() != null &&
            (notBetweenSizes(1, 1024, rowParameters.getPriceComponent()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].price_component- price component should be between size %s and %s;".formatted(i, 1, 1024));
        }
        if (rowParameters.getPointOfDelivery() != null &&
            (!rowParameters.getPointOfDelivery().matches("^[A-Za-z0-9]{1,33}$"))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].point_of_delivery- point of delivery should be between size %s and %s, allowed symbols are A-Z a-z 0-9;".formatted(i, 1, 33));
        }
        if (rowParameters.getPeriodFrom() != null && rowParameters.getPeriodTo() != null &&
            !(rowParameters.getPeriodFrom().isBefore(rowParameters.getPeriodTo()) ||
              rowParameters.getPeriodFrom().equals(rowParameters.getPeriodTo()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].period_from-period_to - period from should be before or equal to period to;".formatted(i));
        }
        if (rowParameters.getMeter() != null &&
            (!rowParameters.getMeter().matches("^[A-Za-z0-9]{1,32}$"))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].meter- meter should be between size %s and %s, allowed symbols are A-Z a-z 0-9;".formatted(i, 1, 32));
        }
        double maxVal = 999999999.99999;
        if (rowParameters.getNewMeterReading() != null &&
            (notInRange(BigDecimal.ZERO, BigDecimal.valueOf(maxVal), rowParameters.getNewMeterReading()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].new_meter_reading- new meter reading should be in range %s and %s;".formatted(i, 0, maxVal));
        }
        if (rowParameters.getOldMeterReading() != null &&
            (notInRange(BigDecimal.ZERO, BigDecimal.valueOf(maxVal), rowParameters.getOldMeterReading()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].old_meter_reading- old meter reading should be in range %s and %s;".formatted(i, 0, maxVal));
        }
        if (rowParameters.getDifferences() != null &&
            (notInRange(BigDecimal.ZERO, BigDecimal.valueOf(maxVal), rowParameters.getDifferences()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].differences- differences should be in range %s and %s;".formatted(i, 0, maxVal));
        }
        if (rowParameters.getMultiplier() != null &&
            (notInRange(BigDecimal.valueOf(0.01), BigDecimal.valueOf(999999999.99), rowParameters.getMultiplier()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].multiplier- multiplier should be in range %s and %s;".formatted(i, 0.01, 999999999.99));
        }
        if (rowParameters.getCorrection() != null &&
            (notInRange(BigDecimal.ZERO, BigDecimal.valueOf(maxVal), rowParameters.getCorrection()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].correction- correction should be in range %s and %s;".formatted(i, 0, maxVal));
        }
        if (rowParameters.getDeducted() != null &&
            (notInRange(BigDecimal.ZERO, BigDecimal.valueOf(maxVal), rowParameters.getDeducted()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].deducted- deducted should be in range %s and %s;".formatted(i, 0, maxVal));
        }
        if (rowParameters.getTotalVolumes() != null &&
            (notInRange(BigDecimal.valueOf(-maxVal), BigDecimal.valueOf(maxVal), rowParameters.getTotalVolumes()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].total_volumes- total volumes should be in range %s and %s;".formatted(i, -maxVal, maxVal));
        }
        if (rowParameters.getUnitOfMeasureForTotalVolumes() != null &&
            (notBetweenSizes(1, 512, rowParameters.getUnitOfMeasureForTotalVolumes()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].unit_of_measures_for_total_volumes- unit of measures for total volumes should be between size %s and %s;".formatted(i, 1, 512));
        }

        double max = 999999999.9999999999;
        BigDecimal minValue = BigDecimal.valueOf(-max);
        BigDecimal maxValue = BigDecimal.valueOf(max);

        if (rowParameters.getUnitPrice() != null &&
            (notInRange(minValue, maxValue, rowParameters.getUnitPrice()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].unit_price- unit price should be in range %s and %s;".formatted(i, -max, max));
        }
        if (rowParameters.getUnitOfMeasureForUnitPrice() != null &&
            (notBetweenSizes(1, 512, rowParameters.getUnitOfMeasureForUnitPrice()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].unit_of_measures_for_unit_price- unit of measures for unit price should be between size %s and %s;".formatted(i, 1, 512));
        }
        if (rowParameters.getCurrentValue() != null &&
            (notInRange(minValue, maxValue, rowParameters.getCurrentValue()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].value- value should be in range %s and %s;".formatted(i, minValue, maxValue));
        }
        if (rowParameters.getIncomeAccount() != null &&
            (notBetweenSizes(1, 32, rowParameters.getIncomeAccount()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].income_account- income account should be between size %s and %s;".formatted(i, 1, 32));
        }
        if (rowParameters.getCostCenter() != null &&
            (notBetweenSizes(1, 32, rowParameters.getCostCenter()))) {
            throw new IllegalArgumentsProvidedException("detailedData[%s].cost_center- cost center should be between size %s and %s;".formatted(i, 1, 32));
        }
    }

    /**
     * Checks if the given value is not within the specified range.
     *
     * @param min The minimum value of the range.
     * @param max The maximum value of the range.
     * @param value The value to check.
     * @return `true` if the value is less than the minimum or greater than the maximum, `false` otherwise.
     */
    private boolean notInRange(BigDecimal min, BigDecimal max, BigDecimal value) {
        return value.compareTo(min) < 0 || value.compareTo(max) > 0;
    }

    /**
     * Checks if the given string value is not between the specified minimum and maximum lengths.
     *
     * @param min The minimum length of the string.
     * @param max The maximum length of the string.
     * @param value The string value to check.
     * @return `true` if the length of the value is less than the minimum or greater than the maximum, `false` otherwise.
     */
    private boolean notBetweenSizes(int min, int max, String value) {
        return value.length() < min || value.length() > max;
    }

    /**
     * Downloads the template content for a manual invoice of the specified type.
     *
     * @param type The type of manual invoice to download the template for.
     * @return A {@link ManualInvoiceTemplateContent} object containing the template name and the downloaded file content.
     * @throws DomainEntityNotFoundException if the template for the specified manual invoice type is not found.
     */
    public ManualInvoiceTemplateContent downloadTemplate(ManualInvoiceType type) {
        Template template = templateRepository
                .findById(type.equals(STANDARD_INVOICE) ? MANUAL_INVOICE_STANDARD_TEMPLATE_ID : MANUAL_INVOICE_DETAILED_TEMPLATE_ID)
                .orElseThrow(() -> new DomainEntityNotFoundException("Template for manual invoice not found;"));

        return new ManualInvoiceTemplateContent(template.getTemplateName(), fileService.downloadFile(template.getFileUrl()));
    }

    /**
     * Retrieves the list of customer communication data based on the provided request.
     *
     * @param request The {@link BillingCommunicationDataListRequest} containing the details for the communication data retrieval.
     * @return A list of {@link CustomerCommunicationDataResponse} containing the customer communication data.
     */
    public List<CustomerCommunicationDataResponse> getCommunicationDataList(BillingCommunicationDataListRequest request) {
        if (request.getContractOrderId() == null) {
            return customerRepository.customerCommunicationDataList(
                    request.getCustomerDetailsId(),
                    communicationContactPurposeProperties.getBillingCommunicationId()
            );
        }
        List<CustomerCommunicationDataResponse> communicationDataList = new ArrayList<>();
        switch (request.getContractOrderType()) {
            case PRODUCT_CONTRACT -> {
                Optional<CustomerCommunicationDataResponse> communicationDataResponse =
                        productContractDetailsRepository.findCommunicationDataByContractIdAndCustomerDetailId(
                                request.getContractOrderId(), request.getCustomerDetailsId()
                        );
                communicationDataResponse.ifPresent(communicationDataList::add);
            }
            case SERVICE_CONTRACT -> {
                Optional<CustomerCommunicationDataResponse> communicationDataResponse =
                        serviceContractDetailsRepository.findCommunicationDataByContractIdAndCustomerDetailId(
                                request.getContractOrderId(), request.getCustomerDetailsId()
                        );
                communicationDataResponse.ifPresent(communicationDataList::add);
            }
            case GOODS_ORDER -> {
                Optional<CustomerCommunicationDataResponse> communicationDataResponse =
                        goodsOrderRepository.findCommunicationDataByOrderIdAndCustomerDetailId(
                                request.getContractOrderId(), request.getCustomerDetailsId()
                        );
                communicationDataResponse.ifPresent(communicationDataList::add);
            }
            case SERVICE_ORDER -> {
                Optional<CustomerCommunicationDataResponse> communicationDataResponse =
                        serviceOrderRepository.findCommunicationDataByOrderIdAndCustomerDetailId(
                                request.getContractOrderId(), request.getCustomerDetailsId()
                        );
                communicationDataResponse.ifPresent(communicationDataList::add);
            }
        }
        return communicationDataList;
    }

    /**
     * Retrieves a page of customer contract and order responses based on the provided request.
     *
     * @param request The {@link CustomerContractsAndOrdersRequest} containing the details for the contract and order retrieval.
     * @return A page of {@link CustomerContractOrderResponse} containing the customer contract and order data.
     * @throws DomainEntityNotFoundException if the customer detail with the provided ID is not found or is not active.
     */
    public Page<CustomerContractOrderResponse> getCustomerContractOrderList(CustomerContractsAndOrdersRequest request) {
        if (!customerDetailsRepository.existsByDetailIdAndCustomerStatus(request.getCustomerDetailId(), List.of(CustomerStatus.ACTIVE))) {
            throw new DomainEntityNotFoundException("customerDetailId-[customerDetailId] customer detail not found;");
        }
        return request.getType().equals(CustomerContractOrderType.CONTRACT) ?
                productContractRepository.findByCustomerDetailsIdAndPrompt(request.getCustomerDetailId(),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())) :
                goodsOrderRepository.findByCustomerDetailsIdAndPrompt(request.getCustomerDetailId(),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize()));
    }

    /**
     * Retrieves the manual invoice parameters for the specified billing run.
     *
     * @param billingRun The {@link BillingRun} for which to retrieve the manual invoice parameters.
     * @return A {@link ManualInvoiceBillingRunParametersResponse} containing the basic, summary, and detailed data parameters for the manual invoice.
     */
    public ManualInvoiceBillingRunParametersResponse getManualInvoiceParameters(BillingRun billingRun) {
        ManualInvoiceBillingRunParametersResponse response = new ManualInvoiceBillingRunParametersResponse();
        response.setManualInvoiceBasicDataParameters(manualInvoiceMapperService.mapBasicDataParameters(billingRun));
        response.setManualInvoiceSummaryDataParameters(manualInvoiceMapperService.mapSummaryDataParameters(billingRun));
        response.setManualInvoiceDetailedDataParameters(manualInvoiceMapperService.mapDetailedDataParameters(billingRun));
        return response;
    }

    /**
     * Updates the manual invoice parameters for the specified billing run based on the provided request.
     *
     * @param request The {@link ManualInvoiceEditParameters} containing the updated manual invoice parameters.
     * @param billingRun The {@link BillingRun} for which the manual invoice parameters are being updated.
     * @param errorMessages A list to store any error messages that occur during the update process.
     */
    @Transactional
    public void updateManualInvoiceParametersForEdit(ManualInvoiceEditParameters request, BillingRun billingRun, List<String> errorMessages) {
        mapManualInvoiceBasicDataParameters(request.getManualInvoiceBasicDataParameters(), billingRun, errorMessages, true);
        updateManualInvoiceSummaryDataParameters(request.getManualInvoiceSummaryDataParameters(), billingRun, errorMessages);
        updateManualInvoiceDetailedDataParameters(request.getManualInvoiceDetailedDataParameters(), billingRun, errorMessages);
    }


    /**
     * Updates the detailed data parameters for the manual invoice of the specified billing run.
     *
     * @param manualInvoiceDetailedDataParameters The {@link ManualInvoiceDetailedDataEditParameters} containing the updated detailed data parameters.
     * @param billingRun The {@link BillingRun} for which the manual invoice detailed data parameters are being updated.
     * @param errorMessages A list to store any error messages that occur during the update process.
     */
    private void updateManualInvoiceDetailedDataParameters(ManualInvoiceDetailedDataEditParameters manualInvoiceDetailedDataParameters, BillingRun billingRun, List<String> errorMessages) {
        Map<Long, BillingDetailedData> detailedData = billingDetailedDataRepository.findByBillingId(billingRun.getId())
                .stream().collect(Collectors.toMap(BillingDetailedData::getId, e -> e));

        if (manualInvoiceDetailedDataParameters != null && (!CollectionUtils.isEmpty(manualInvoiceDetailedDataParameters.getDetailedDataRowParametersList()))) {
            List<DetailedDataRowEditParameters> detailedDataRowParametersList = manualInvoiceDetailedDataParameters.getDetailedDataRowParametersList();
            int index = 0;
            List<BillingDetailedData> detailedDataListToSave = new ArrayList<>();
            for (DetailedDataRowEditParameters row : detailedDataRowParametersList) {
                if (row.getId() != null) {
                    BillingDetailedData billingDetailedData = detailedData.get(row.getId());
                    if (billingDetailedData == null) {
                        throw new DomainEntityNotFoundException("detailedDataRowList[%s]-billing detailed data not found by given id: %s;".formatted(index, row.getId()));
                    }
                    detailedDataListToSave.add(updateDetailedDataParameters(billingDetailedData, row, errorMessages, index));
                    detailedData.remove(billingDetailedData.getId());
                } else {
                    detailedDataListToSave.add(createBillingDetailedData(row, billingRun, errorMessages, index));
                }
                index++;
            }
            if (errorMessages.isEmpty()) {
                billingDetailedDataRepository.saveAll(detailedDataListToSave);
            }
        }
        if (!detailedData.isEmpty()) {
            billingDetailedDataRepository.deleteAll(detailedData.values());
        }
    }

    /**
     * Updates the detailed data parameters for a billing detailed data object based on the provided detailed data row edit parameters.
     *
     * @param billingDetailedData The {@link BillingDetailedData} object to update.
     * @param dataRowParameters The {@link DetailedDataRowEditParameters} containing the updated detailed data parameters.
     * @param errorMessages A list to store any error messages that occur during the update process.
     * @param index The index of the detailed data row in the list of detailed data rows.
     * @return The updated {@link BillingDetailedData} object.
     */
    private BillingDetailedData updateDetailedDataParameters(BillingDetailedData billingDetailedData, DetailedDataRowEditParameters dataRowParameters, List<String> errorMessages, int index) {
        setDetailedDataParameters(billingDetailedData, dataRowParameters);
        if (dataRowParameters.getValueCurrencyId() != null) {
            if (checkCurrency(dataRowParameters.getValueCurrencyId(), errorMessages, "manualInvoiceParameters.manualInvoiceDetailedDataEditParameters.detailedDataRowParametersList[%s]".formatted(index))) {
                billingDetailedData.setValueCurrencyId(dataRowParameters.getValueCurrencyId());
            }
        } else {
            billingDetailedData.setValueCurrencyId(null);
        }
        billingVatRateUtil.checkVatRateDetailed(dataRowParameters.getGlobalVatRate(), dataRowParameters.getVatRateId(), errorMessages, billingDetailedData, "manualInvoiceParameters.manualInvoiceDetailedDataEditParameters.detailedDataRowParametersList[%s]".formatted(index));
        return billingDetailedData;
    }

    /**
     * Sets the detailed data parameters for a billing detailed data object based on the provided detailed data row parameters.
     *
     * @param billingDetailedData The {@link BillingDetailedData} object to update.
     * @param row The {@link DetailedDataRowParameters} containing the updated detailed data parameters.
     */
    private void setDetailedDataParameters(BillingDetailedData billingDetailedData, DetailedDataRowParameters row) {
        billingDetailedData.setDifferences(row.getDifferences());
        billingDetailedData.setCorrection(row.getCorrection());
        billingDetailedData.setDeducted(row.getDeducted());
        billingDetailedData.setMeter(row.getMeter());
        billingDetailedData.setOldMeterReading(row.getOldMeterReading());
        billingDetailedData.setNewMeterReading(row.getNewMeterReading());
        billingDetailedData.setCostCenter(row.getCostCenter());
        billingDetailedData.setIncomeAccount(row.getIncomeAccount());
        billingDetailedData.setPod(row.getPointOfDelivery());
        billingDetailedData.setMultiplier(row.getMultiplier());
        billingDetailedData.setValue(row.getCurrentValue());
        billingDetailedData.setTotalVolumes(row.getTotalVolumes());
        billingDetailedData.setUnitPrice(row.getUnitPrice());
        billingDetailedData.setPeriodFrom(row.getPeriodFrom());
        billingDetailedData.setPeriodTo(row.getPeriodTo());
        billingDetailedData.setPriceComponent(row.getPriceComponent());
        billingDetailedData.setMeasuresUnitForTotalVolumes(row.getUnitOfMeasureForTotalVolumes());
        billingDetailedData.setMeasureUnitForUnitPrice(row.getUnitOfMeasureForUnitPrice());
    }

    /**
     * Updates the manual invoice summary data parameters for a billing run.
     *
     * @param manualInvoiceSummaryDataParameters The {@link ManualInvoiceSummaryDataEditParameters} containing the updated summary data parameters.
     * @param billingRun The {@link BillingRun} to update.
     * @param errorMessages A list to store any error messages that occur during the update process.
     */
    private void updateManualInvoiceSummaryDataParameters(ManualInvoiceSummaryDataEditParameters manualInvoiceSummaryDataParameters, BillingRun billingRun, List<String> errorMessages) {
        Map<Long, BillingSummaryData> billingSummaryDataMap = billingSummaryDataRepository.findByBillingId(billingRun.getId())
                .stream().collect(Collectors.toMap(BillingSummaryData::getId, e -> e));
        List<SummaryDataRowEditParameters> summaryDataRowList = manualInvoiceSummaryDataParameters.getSummaryDataRowList();
        billingRun.setManualInvoiceType(manualInvoiceSummaryDataParameters.getManualInvoiceType());
        if (!CollectionUtils.isEmpty(summaryDataRowList)) {
            List<BillingSummaryData> summaryDataListToSave = new ArrayList<>();
            int index = 0;
            for (SummaryDataRowEditParameters row : summaryDataRowList) {
                if (row.getId() != null) {
                    BillingSummaryData billingSummaryData = billingSummaryDataMap.get(row.getId());
                    if (billingSummaryData == null) {
                        throw new DomainEntityNotFoundException("summaryDataRowList[%s]-billing summary data not found by given id: %s;".formatted(index, row.getId()));
                    }

                    summaryDataListToSave.add(updateSummaryDataParameters(billingSummaryData, row, index, errorMessages));
                    billingSummaryDataMap.remove(billingSummaryData.getId());
                } else {
                    summaryDataListToSave.add(createBillingSummaryDataParameters(billingRun, row, errorMessages, index));
                }
                index++;
            }
            if (errorMessages.isEmpty()) {
                billingSummaryDataRepository.saveAll(summaryDataListToSave);
            }
        }
        if (!billingSummaryDataMap.isEmpty()) {
            billingSummaryDataRepository.deleteAll(billingSummaryDataMap.values());
        }
    }

    /**
     * Updates the summary data parameters for a billing summary data object.
     *
     * @param billingSummaryData The {@link BillingSummaryData} object to update.
     * @param dataRowParameters The {@link SummaryDataRowEditParameters} containing the updated parameters.
     * @param index The index of the summary data row in the list.
     * @param errorMessages A list to store any error messages that occur during the update process.
     * @return The updated {@link BillingSummaryData} object.
     */
    private BillingSummaryData updateSummaryDataParameters(BillingSummaryData billingSummaryData, SummaryDataRowEditParameters dataRowParameters, int index, List<String> errorMessages) {
        setSummaryDataParameters(billingSummaryData, dataRowParameters);
        if (dataRowParameters.getValueCurrencyId() != null) {
            if (checkCurrency(dataRowParameters.getValueCurrencyId(), errorMessages, "manualInvoiceSummaryDataEditParameters.summaryDataRowList[%s]".formatted(index))) {
                billingSummaryData.setValueCurrencyId(dataRowParameters.getValueCurrencyId());
            }
        } else {
            billingSummaryData.setValueCurrencyId(null);
        }
        billingSummaryData.setMeasureUnitForUnitPrice(dataRowParameters.getUnitOfMeasureForUnitPrice());
        billingVatRateUtil.checkVatRateSummary(dataRowParameters.getGlobalVatRate(), dataRowParameters.getVatRateId(), errorMessages, billingSummaryData, "manualInvoiceSummaryDataEditParameters.summaryDataRowList[%s]".formatted(index));
        return billingSummaryData;
    }

    /**
     * Sets the summary data parameters for a {@link BillingSummaryData} object.
     *
     * @param billingSummaryData The {@link BillingSummaryData} object to update.
     * @param row The {@link SummaryDataRowParameters} containing the updated parameters.
     */
    private void setSummaryDataParameters(BillingSummaryData billingSummaryData, SummaryDataRowParameters row) {
        billingSummaryData.setValue(row.getValue());
        billingSummaryData.setCostCenter(row.getCostCenter());
        billingSummaryData.setIncomeAccount(row.getIncomeAccount());
        billingSummaryData.setUnitPrice(row.getUnitPrice());
        billingSummaryData.setPriceComponentOrPriceComponentGroups(row.getPriceComponentOrPriceComponentGroupOrItem());
        billingSummaryData.setTotalVolumes(row.getTotalVolumes());
        billingSummaryData.setMeasuresUnitForTotalVolumes(row.getUnitOfMeasuresForTotalVolumes());
    }
}
