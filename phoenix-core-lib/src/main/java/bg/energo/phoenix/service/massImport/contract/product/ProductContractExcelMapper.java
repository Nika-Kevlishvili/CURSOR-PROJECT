package bg.energo.phoenix.service.massImport.contract.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.CacheObjectForCustomerDetails;
import bg.energo.phoenix.model.CacheObjectForDetails;
import bg.energo.phoenix.model.CacheObjectForPod;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailType;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.product.ContractType;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.enums.product.term.terms.SupplyActivation;
import bg.energo.phoenix.model.enums.product.term.terms.WaitForOldContractTermToExpire;
import bg.energo.phoenix.model.request.contract.ProxyEditRequest;
import bg.energo.phoenix.model.request.contract.product.*;
import bg.energo.phoenix.model.request.contract.product.additionalParameters.ProductContractAdditionalParametersRequest;
import bg.energo.phoenix.model.request.contract.product.additionalParameters.ProductContractBankingDetails;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ContractVersionTypesRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ExternalIntermediaryRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.contract.product.ProductContractProductParametersService;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static java.time.LocalDate.from;

@Service
@RequiredArgsConstructor
public class ProductContractExcelMapper {

    private final ContractVersionTypesRepository contractVersionTypesRepository;
    private final CurrencyRepository currencyRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final BankRepository bankRepository;
    private final InterestRateRepository interestRateRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    private final ContractBillingGroupRepository contractBillingGroupRepository;
    private final ProductContractProductParametersService productParametersService;
    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;
    private final ExternalIntermediaryRepository externalIntermediaryRepository;
    private final ContractTemplateRepository contractTemplateRepository;

    /**
     * create product contract create request
     *
     * @param row
     * @param errorMessages
     * @return
     */
    public ProductContractCreateRequest productContractCreateRequest(Row row, List<String> errorMessages) {
        ProductContractCreateRequest request = new ProductContractCreateRequest();
        ProductContractAdditionalParametersRequest additionalParameters = getAdditionalParameters(row, errorMessages);
        request.setAdditionalParameters(additionalParameters);
        request.setProductContractPointOfDeliveries(getPodVersionIds(getStringValue(76, row), additionalParameters, errorMessages).stream().map(id -> new ProductContractPointOfDeliveryRequest(id, "")).toList());
        ProductContractBasicParametersCreateRequest basicParameters = new ProductContractBasicParametersCreateRequest();
        fillBasicParameters(basicParameters, row, errorMessages);
        request.setBasicParameters(basicParameters);
        request.setProductParameters(createProductParameters(basicParameters.getProductId(), basicParameters.getProductVersionId(), row, errorMessages));
        return request;
    }

    /**
     * Creates contract update request
     *
     * @param contractId
     * @param row
     * @param errorMessages
     * @return
     */
    public ProductContractUpdateRequest productContractUpdateRequest(Long contractId, Row row, List<String> errorMessages) {
        ProductContractUpdateRequest productContractUpdateRequest = new ProductContractUpdateRequest();
        ProductContractBasicParametersUpdateRequest basicParameters = createBasicParametersUpdate(row, errorMessages);
        productContractUpdateRequest.setBasicParameters(basicParameters);
        productContractUpdateRequest.setProductParameters(createProductParameters(basicParameters.getProductId(), basicParameters.getProductVersionId(), row, errorMessages));
        ProductContractAdditionalParametersRequest additionalParameters = getAdditionalParameters(row, errorMessages);
        productContractUpdateRequest.setAdditionalParameters(additionalParameters);
        Optional<Long> billingGroupOptional = contractBillingGroupRepository.findDefaultCacheObjectByContractId(contractId);
        if (billingGroupOptional.isEmpty()) {
            errorMessages.add("Default billing group do not exist!;");
            return null;
        }
        List<Long> podVersionIds = getPodVersionIds(getStringValue(76, row), additionalParameters, errorMessages);
        ArrayList<ContractPodRequest> podRequests = new ArrayList<>();
        podRequests
                .add(
                        new ContractPodRequest(
                                billingGroupOptional.get(),
                                podVersionIds
                                        .stream()
                                        .map(id -> new ProductContractPointOfDeliveryRequest(id, ""))
                                        .toList()
                        )
                );
        productContractUpdateRequest.setPodRequests(podRequests);

        return productContractUpdateRequest;
    }

    public String getContractNumber(Row row) {
        return getStringValue(0, row);
    }

    public Integer getContractVersion(Row row) {
        return getIntegerValue(1, row);
    }

    public CreateEdit getCreateEdit(Row row) {
        return getEnum(2, row, CreateEdit.class);
    }

    public LocalDate getStartDate(Row row) {
        return getDateValue(3, row);
    }

    private ProductContractBasicParametersUpdateRequest createBasicParametersUpdate(Row row, List<String> errorMessages) {
        ProductContractBasicParametersUpdateRequest basicParametersUpdateRequest = new ProductContractBasicParametersUpdateRequest();
        fillBasicParameters(basicParametersUpdateRequest, row, errorMessages);
        basicParametersUpdateRequest.setTerminationDate(getDateValue(17, row));
        basicParametersUpdateRequest.setPerpetuityDate(getDateValue(18, row));
        return basicParametersUpdateRequest;
    }

    private ProductContractAdditionalParametersRequest getAdditionalParameters(Row row, List<String> errorMessages) {
        ProductContractAdditionalParametersRequest additionalParameters = new ProductContractAdditionalParametersRequest();
        ProductContractBankingDetails bankingDetails = new ProductContractBankingDetails();

        additionalParameters.setBankingDetails(bankingDetails);
        Boolean directDebit = getBoolean(getStringValue(46, row), errorMessages, "Direct_debit");
        bankingDetails.setDirectDebit(directDebit);
        if (Boolean.TRUE.equals(directDebit)) {
            bankingDetails.setBankId(getBank(row, errorMessages));
            bankingDetails.setIban(getStringValue(48, row));
        }
        additionalParameters.setInterestRateId(getInterestRate(row, errorMessages));
        additionalParameters.setEmployeeId(getEmployeeId(row, errorMessages));
        additionalParameters.setAssistingEmployees(getOptionalEmployeeId(row, 51));
        additionalParameters.setInternalIntermediaries(getOptionalEmployeeId(row, 52));
        additionalParameters.setExternalIntermediaries(getExternalIntermediaries(row, errorMessages));
        return additionalParameters;
    }

    public void fillBasicParameters(ProductContractBasicParametersCreateRequest basicParameters, Row row, List<String> errorMessages) {

        CacheObjectForCustomerDetails customerDetail = getCustomerDetail(row, errorMessages, basicParameters);
        getProductDetail(row, errorMessages, basicParameters);
        basicParameters.setStatus(getEnum(8, row, ContractDetailsStatus.class));
        basicParameters.setSubStatus(getEnum(9, row, ContractDetailsSubStatus.class));
        basicParameters.setType(getEnum(10, row, ContractDetailType.class));
        basicParameters.setVersionStatus(getEnum(11, row, ProductContractVersionStatus.class));
        Long versionType = getVersionType(row, errorMessages);
        if (versionType != null) {
            basicParameters.setVersionTypeIds(Set.of(versionType));
        }
        ContractEntryIntoForce entryIntoForce = getEnum(61, row, ContractEntryIntoForce.class);
        StartOfContractInitialTerm startOfContractInitialTerm = getEnum(63, row, StartOfContractInitialTerm.class);
        basicParameters.setSigningDate(getDateValue(13, row));
        if (entryIntoForce != null && entryIntoForce.equals(ContractEntryIntoForce.MANUAL)) {
            basicParameters.setEntryInForceDate(getDateValue(14, row));
        }
        if (startOfContractInitialTerm != null && startOfContractInitialTerm.equals(StartOfContractInitialTerm.MANUAL)) {
            basicParameters.setStartOfInitialTerm(getDateValue(15, row));
        }

        String untilAmount = getStringValue(19, row);
        if (untilAmount != null) {
            basicParameters.setUntilAmount(new BigDecimal(untilAmount));
            basicParameters.setHasUntilAmount(true);
            basicParameters.setUntilAmountCurrencyId(getCurrency(row, errorMessages, "Until amount currency name is empty", 20));
        }
        String untilVolume = getStringValue(21, row);
        if (untilVolume != null) {
            basicParameters.setHasUntilVolume(true);
            basicParameters.setUntilVolume(new BigDecimal(untilVolume));
        }
        basicParameters.setProcurementLaw(getBoolean(getStringValue(22, row), errorMessages, "contract_procurement_law"));
        ProxyEditRequest firstProxy = new ProxyEditRequest();
        firstProxy.setProxyName(getStringValue(23, row));
        firstProxy.setProxyForeignEntityPerson(getBoolean(getStringValue(24, row), errorMessages, "Contract_Proxy_foreign_entity"));
        firstProxy.setProxyCustomerIdentifier(getStringValue(25, row));
        firstProxy.setProxyEmail(getStringValue(26, row));
        firstProxy.setProxyPhone(getStringValue(27, row));
        firstProxy.setProxyPowerOfAttorneyNumber(getStringValue(28, row));
        firstProxy.setProxyData(getDateValue(29, row));
        firstProxy.setProxyValidTill(getDateValue(30, row));
        firstProxy.setNotaryPublic(getStringValue(31, row));
        firstProxy.setRegistrationNumber(getStringValue(32, row));
        firstProxy.setAreaOfOperation(getStringValue(33, row));
        firstProxy.setProxyAuthorizedByProxy(getStringValue(34, row));

        firstProxy.setAuthorizedProxyForeignEntityPerson(getBoolean(getStringValue(35, row), errorMessages, "contract_proxy_foreign_entity"));
        firstProxy.setAuthorizedProxyCustomerIdentifier(getStringValue(36, row));
        firstProxy.setAuthorizedProxyEmail(getStringValue(37, row));
        firstProxy.setAuthorizedProxyPhone(getStringValue(38, row));
        firstProxy.setAuthorizedProxyPowerOfAttorneyNumber(getStringValue(39, row));
        firstProxy.setAuthorizedProxyData(getDateValue(40, row));
        firstProxy.setAuthorizedProxyValidTill(getDateValue(41, row));
        firstProxy.setAuthorizedProxyNotaryPublic(getStringValue(42, row));
        firstProxy.setAuthorizedProxyRegistrationNumber(getStringValue(43, row));
        firstProxy.setAuthorizedProxyAreaOfOperation(getStringValue(44, row));

        if (firstProxy.getProxyName() != null && customerDetail != null) {
            if (CustomerType.LEGAL_ENTITY.equals(customerDetail.getCustomerType()) || Boolean.TRUE.equals(customerDetail.getIsBusiness())) {
                firstProxy.setManagerIds(accountManagerRepository.findManagersByCustomerDetailId(customerDetail.getDetailsId()).stream().map(CacheObject::getId).collect(Collectors.toSet()));
            }
            basicParameters.setProxy(List.of(firstProxy));
        }
    }

    public ProductContractProductParametersCreateRequest createProductParameters(Long productId, Long versionId, Row row, List<String> errorMessages) {
        ProductContractProductParametersCreateRequest productParameters = new ProductContractProductParametersCreateRequest();
        productParameters.setContractType(getEnum(55, row, ContractType.class));

        setPaymentGuarantee(row, productParameters, errorMessages);

        ContractEntryIntoForce entryIntoForce = getEnum(61, row, ContractEntryIntoForce.class);
        StartOfContractInitialTerm startOfContractInitialTerm = getEnum(63, row, StartOfContractInitialTerm.class);
        SupplyActivation supplyActivation = getEnum(65, row, SupplyActivation.class);
        if (entryIntoForce == null || startOfContractInitialTerm == null || supplyActivation == null) {
            throw new IllegalArgumentsProvidedException("EntryIntoForce, StartOfContractInitialTerm and supply activation can not be null");
        }

        LocalDate entryIntoForceValue = getDateValue(62, row);

        productParameters.setEntryIntoForce(entryIntoForce);
        if (entryIntoForce.equals(ContractEntryIntoForce.EXACT_DAY) && entryIntoForceValue == null) {
            errorMessages.add("Entry into force value can not be null!;");
        }
        productParameters.setEntryIntoForceValue(entryIntoForceValue);

        LocalDate startOfContractValue = getDateValue(64, row);
        productParameters.setStartOfContractInitialTerm(startOfContractInitialTerm);
        if (startOfContractInitialTerm.equals(StartOfContractInitialTerm.EXACT_DATE) && startOfContractValue == null) {
            errorMessages.add("Start of the initial term can not be null!;");
        }
        productParameters.setStartOfContractValue(startOfContractValue);

        LocalDate supplyActivationDateValue = getDateValue(66, row);
        productParameters.setSupplyActivation(supplyActivation);
        if (supplyActivation.equals(SupplyActivation.EXACT_DATE) && supplyActivationDateValue == null) {
            errorMessages.add("Supply activation value can not be null!;");
        }
        productParameters.setSupplyActivationValue(supplyActivationDateValue);
        productParameters.setProductContractWaitForOldContractTermToExpires(getEnum(67, row, WaitForOldContractTermToExpire.class));
        String installmentAmount = getStringValue(68, row);
        if (installmentAmount != null) {
            productParameters.setMonthlyInstallmentAmount(new BigDecimal(installmentAmount));
        }
        String installmentValue = getStringValue(69, row);
        if (installmentValue != null) {
            productParameters.setMonthlyInstallmentValue(Short.parseShort(installmentValue));
        }
        String marginalPrice = getStringValue(70, row);
        if (marginalPrice != null) {
            productParameters.setMarginalPrice(new BigDecimal(marginalPrice));
        }
        productParameters.setMarginalPriceValidity(getStringValue(71, row));
        String hourlyProfile = getStringValue(72, row);
        if (hourlyProfile != null) {
            productParameters.setHourlyLoadProfile(new BigDecimal(hourlyProfile));
        }

        String procurementPrice = getStringValue(73, row);
        if (procurementPrice != null) {
            productParameters.setProcurementPrice(new BigDecimal(procurementPrice));
        }
        String priceIncrease = getStringValue(74, row);
        if (priceIncrease != null) {
            productParameters.setImbalancePriceIncrease(new BigDecimal(priceIncrease));
        }
        String setMargin = getStringValue(75, row);
        if (setMargin != null) {
            productParameters.setSetMargin(new BigDecimal(setMargin));
        }
        productParametersService.thirdTabFieldsForMassImport(productParameters, productId, versionId, errorMessages);
        return productParameters;
    }

    public List<Long> getPodVersionIds(String podIdentifiersString, ProductContractAdditionalParametersRequest additionalParameters, List<String> errorMessages) {
        if (podIdentifiersString == null) {
            errorMessages.add("Pod identifiers are not provided!;");
            return Collections.emptyList();
        }
        String[] split = podIdentifiersString.split(",");
        List<String> identifierStrings = Arrays.asList(split);
        List<CacheObjectForPod> byPodIdentifiers = pointOfDeliveryDetailsRepository.findByPodIdentifiers(identifierStrings);
        List<Long> podIds = new ArrayList<>();
        int sum = 0;
        for (CacheObjectForPod pod : byPodIdentifiers) {
            podIds.add(pod.getId());
            sum += pod.getEstimatedConsumption();
        }
        additionalParameters.setEstimatedTotalConsumptionUnderContractKwh(BigDecimal.valueOf(sum * 12L).divide(BigDecimal.valueOf(1000), MathContext.UNLIMITED));
        return podIds;
    }

    private void setPaymentGuarantee(Row row, ProductContractProductParametersCreateRequest productParameters, List<String> errorMessages) {
        PaymentGuarantee guarantee = getEnum(56, row, PaymentGuarantee.class);

        if (guarantee == null) {
            errorMessages.add("contract_payment_guarantee-Payment guarantee can not be null!;");
            return;
        }
        productParameters.setPaymentGuarantee(guarantee);
        if (guarantee.equals(PaymentGuarantee.CASH_DEPOSIT) || guarantee.equals(PaymentGuarantee.CASH_DEPOSIT_AND_BANK)) {
            productParameters.setCashDepositCurrencyId(getCurrency(row, errorMessages, "Cash_deposit_currency- can not be null!;", 58));
            String cashDeposit = getStringValue(57, row);
            if (cashDeposit == null) {
                errorMessages.add("cash_deposit_amount-Cash deposit amount can not be null!;");
                return;
            }
            Long cashDepositCurrency = getCurrency(row, errorMessages, "cash_deposit_currency", 58);
            productParameters.setCashDepositCurrencyId(cashDepositCurrency);
            productParameters.setCashDeposit(new BigDecimal(cashDeposit));
        }
        if (guarantee.equals(PaymentGuarantee.BANK) || guarantee.equals(PaymentGuarantee.CASH_DEPOSIT_AND_BANK)) {
            productParameters.setBankGuaranteeCurrencyId(getCurrency(row, errorMessages, "Cash_deposit_currency- can not be null!;", 60));
            String bankDeposit = getStringValue(59, row);
            if (bankDeposit == null) {
                errorMessages.add("bank_deposit_amount-Bank deposit amount can not be null!;");
                return;
            }
            Long bankDepositCurrency = getCurrency(row, errorMessages, "cash_deposit_currency", 60);
            productParameters.setBankGuaranteeCurrencyId(bankDepositCurrency);
            productParameters.setBankGuarantee(new BigDecimal(bankDeposit));
        }

    }

    private Long getInterestRate(Row row, List<String> errorMessages) {
        String interestRateName = getStringValue(49, row);
        if (!StringUtils.isEmpty(interestRateName)) {
            Optional<CacheObject> interestRate = interestRateRepository.findCacheObjectByName(interestRateName, InterestRateStatus.ACTIVE);
            if (interestRate.isPresent()) {
                return interestRate.get().getId();
            }
        } else {
            errorMessages.add("Interest rate name is empty;");
            return null;
        }
        return null;
    }

    private Long getEmployeeId(Row row, List<String> errorMessages) {
        String employeeIdentifier = getStringValue(50, row);
        if (!StringUtils.isEmpty(employeeIdentifier)) {
            Optional<CacheObject> accountManager = accountManagerRepository.findCacheObjectByName(employeeIdentifier);
            if (accountManager.isPresent()) {
                return accountManager.get().getId();
            }
        } else {
            return null;
        }
        errorMessages.add("Invalid employee id provided;");

        return null;
    }

    private List<Long> getOptionalEmployeeId(Row row, Integer rowId) {
        return getAccountManagerId(rowId, row);
    }


    private List<Long> getAccountManagerId(Integer rowNumber, Row row) {
        String employeeIdentifier = getStringValue(rowNumber, row);
        if (!StringUtils.isEmpty(employeeIdentifier)) {
            List<String> list = Arrays.asList(employeeIdentifier.split(","));
            return accountManagerRepository.findCacheObjectByNameIn(list).stream().map(CacheObject::getId).toList();
        }
        return null;
    }

    private Long getVersionType(Row row, List<String> errorMessages) {
        String interestRateName = getStringValue(12, row);
        if (!StringUtils.isEmpty(interestRateName)) {
            Optional<CacheObject> versionType = contractVersionTypesRepository.findCacheObjectByName(interestRateName, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
            if (versionType.isPresent()) {
                return versionType.get().getId();
            }
        } else {
            errorMessages.add("Version Type name is empty");
            return null;
        }
        return null;
    }

    private Long getBank(Row row, List<String> errorMessages) {
        String bankName = getStringValue(47, row);
        if (!StringUtils.isEmpty(bankName)) {
            Optional<CacheObject> bank = bankRepository.findCacheObjectByName(bankName, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
            if (bank.isPresent()) {
                return bank.get().getId();
            }
        } else {
            errorMessages.add("Bank name is empty");
            return null;
        }
        return null;
    }

    private CacheObjectForCustomerDetails getCustomerDetail(Row row, List<String> errorMessages, ProductContractBasicParametersCreateRequest request) {
        String customerIdentifier = getStringValue(4, row);
        Long customerVersion = getLongValue(5, row);
        if (customerIdentifier != null && customerVersion != null) {
            Optional<CacheObjectForCustomerDetails> versionType = customerDetailsRepository.findCacheObjectByIdentifier(customerIdentifier, customerVersion);
            if (versionType.isPresent()) {
                CacheObjectForCustomerDetails cacheObjectForDetails = versionType.get();
                request.setCustomerId(cacheObjectForDetails.getId());
                request.setCustomerVersionId(cacheObjectForDetails.getVersionId());
                List<CacheObject> billingGroupComms = customerDetailsRepository.customerCommunicationDataCacheObjectList(cacheObjectForDetails.getDetailsId(), communicationContactPurposeProperties.getBillingCommunicationId());
                if (billingGroupComms.size() == 0) {
                    errorMessages.add("customer do not have billing group communication data!;");
                    return cacheObjectForDetails;
                }
                request.setCommunicationDataBillingId(billingGroupComms.get(0).getId());
                List<CacheObject> contractComms = customerDetailsRepository.customerCommunicationDataCacheObjectList(cacheObjectForDetails.getDetailsId(), communicationContactPurposeProperties.getContractCommunicationId());
                if (contractComms.size() == 0) {
                    errorMessages.add("customer do not have Contract communication data!;");
                    return cacheObjectForDetails;
                }
                request.setCommunicationDataContractId(contractComms.get(0).getId());

                return cacheObjectForDetails;
            }
        } else {
            errorMessages.add("Customer detail is empty;");
            return null;
        }
        errorMessages.add("Customer do not exists!;");
        return null;
    }

    private void getProductDetail(Row row, List<String> errorMessages, ProductContractBasicParametersCreateRequest request) {
        Long productId = getLongValue(6, row);
        Long productVersion = getLongValue(7, row);
        if (productId != null && productVersion != null) {
            Optional<CacheObjectForDetails> versionType = productDetailsRepository.findCacheObjectByProductId(productId, productVersion);
            if (versionType.isPresent()) {
                CacheObjectForDetails cacheObjectForDetails = versionType.get();
                request.setProductId(cacheObjectForDetails.getId());
                request.setProductVersionId(cacheObjectForDetails.getVersionId());
                return;
            }
        } else {
            errorMessages.add("Product detail is empty;");
            return;
        }
        errorMessages.add("Product do not exist!;");
    }

    private Long getCurrency(Row row, List<String> errorMessages, String message, Integer rowNumber) {
        String currencyName = getStringValue(rowNumber, row);
        if (!StringUtils.isEmpty(currencyName)) {
            Optional<CacheObject> currency = currencyRepository.getCacheObjectByNameAndStatusIn(currencyName, List.of(NomenclatureItemStatus.ACTIVE));
            if (currency.isPresent()) {
                return currency.get().getId();
            }
        } else {
            errorMessages.add(message);
            return null;
        }
        return null;
    }

    private <T extends Enum<T>> T getEnum(int columnNumber, Row row, Class<T> type) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.STRING);
            String stringCellValue = row.getCell(columnNumber).getStringCellValue();
            return Enum.valueOf(type, stringCellValue);
        }
        return null;
    }

    private Long getLongValue(int columnNumber, Row row) {

        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {

            Long result;
            if (row.getCell(columnNumber).getCellType() == CellType.STRING) {
                result = Long.parseLong(row.getCell(columnNumber).getStringCellValue());
            } else if (row.getCell(columnNumber).getCellType() == CellType.NUMERIC) {
                result = Double.valueOf(row.getCell(columnNumber).getNumericCellValue()).longValue();
            } else {
                throw new ClientException("providedPower-Invalid cell type for provided power in row " + columnNumber + ";", ILLEGAL_ARGUMENTS_PROVIDED);
            }
            return result;
        } else {
            return null;
        }
    }

    private Integer getIntegerValue(int columnNumber, Row row) {

        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {

            Integer result;
            if (row.getCell(columnNumber).getCellType() == CellType.STRING) {
                result = Integer.parseInt(row.getCell(columnNumber).getStringCellValue());
            } else if (row.getCell(columnNumber).getCellType() == CellType.NUMERIC) {
                result = Double.valueOf(row.getCell(columnNumber).getNumericCellValue()).intValue();
            } else {
                throw new ClientException("providedPower-Invalid cell type for provided power in row " + columnNumber + ";", ILLEGAL_ARGUMENTS_PROVIDED);
            }
            return result;
        } else {
            return null;
        }
    }

    private String getStringValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.STRING);
            return row.getCell(columnNumber).getStringCellValue();
        }
        return null;
    }

    private Boolean getBoolean(String value, List<String> errorMessages, String fieldName) {
        if (value != null) {
            if (value.equalsIgnoreCase("YES")) return true;
            if (value.equalsIgnoreCase("NO")) return false;
            errorMessages.add(fieldName + "-Must be provided only YES or NO;");
        }
        return false;
    }


    private LocalDate getDateValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.NUMERIC);
            if (row.getCell(columnNumber).getDateCellValue() != null) {
                return from(
                        LocalDate.ofInstant(
                                row.getCell(columnNumber).getDateCellValue().toInstant(), ZoneId.systemDefault()));
            }
        }
        return null;
    }

    private List<Long> getExternalIntermediaries(Row row, List<String> errorMessages) {
        String externalIntermediaries = getStringValue(53, row);
        if (!StringUtils.isEmpty(externalIntermediaries)) {
            List<String> list = Arrays.asList(externalIntermediaries.split(","));

            List<CacheObject> intermediary = externalIntermediaryRepository.findCacheObjectByNames(list);
            Set<String> inters = intermediary.stream().map(CacheObject::getName).collect(Collectors.toSet());

            for (String s : list) {
                if (!inters.contains(s)) {
                    errorMessages.add("External Intermediary with name %s not found!;".formatted(s));
                }
            }
            return intermediary.stream().map(CacheObject::getId).toList();
        } else {
            return null;
        }

    }
}
