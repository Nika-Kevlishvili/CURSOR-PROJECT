package bg.energo.phoenix.service.massImport.contract;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRate;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.service.*;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.Manager;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommContactPurposes;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.nomenclature.contract.ContractVersionType;
import bg.energo.phoenix.model.entity.nomenclature.contract.ExternalIntermediary;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.service.*;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.contract.service.ContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractContractType;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.DateOfIssueType;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.PaymentType;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.ValueType;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.PaymentTermStatus;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.enums.product.term.terms.TermStatus;
import bg.energo.phoenix.model.request.contract.ProxyEditRequest;
import bg.energo.phoenix.model.request.contract.product.PriceComponentContractFormula;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityRequest;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityType;
import bg.energo.phoenix.model.request.contract.service.*;
import bg.energo.phoenix.model.request.contract.service.edit.*;
import bg.energo.phoenix.model.response.contract.productContract.ContractPriceComponentResponse;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractIAPResponse;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractSubObjectShortResponse;
import bg.energo.phoenix.model.response.contract.serviceContract.SubObjectContractResponse;
import bg.energo.phoenix.model.response.terms.InvoicePaymentTermsResponse;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.*;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.ManagerRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommContactPurposesRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ContractVersionTypesRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ExternalIntermediaryRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentFormulaVariableRepository;
import bg.energo.phoenix.repository.product.service.ServiceAdditionalParamsRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.product.service.ServiceRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServiceContractTermRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServiceInterimAndAdvancePaymentRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServicePriceComponentRepository;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupTermsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.contract.relatedEntities.RelatedContractsAndOrdersService;
import bg.energo.phoenix.service.product.iap.interimAdvancePayment.InterimAdvancePaymentTermsService;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static java.time.LocalDate.from;

@Service
@RequiredArgsConstructor
public class ServiceContractExcelMapper {
    private final CurrencyRepository currencyRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final ContractVersionTypesRepository contractVersionTypesRepository;
    private final BankRepository bankRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final ProductContractRepository productContractRepository;
    private final ServiceContractsRepository serviceContractRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final GoodsOrderRepository goodsOrderRepository;
    private final InterestRateRepository interestRateRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final InvoicePaymentTermsRepository invoicePaymentTermsRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final ServiceContractContractVersionTypesRepository serviceContractContractVersionTypesRepository;
    private final ServiceContractProxyRepository serviceContractProxyRepository;
    private final ServiceContractFilesRepository serviceContractFilesRepository;
    private final ServiceContractAdditionalDocumentsRepository serviceContractAdditionalDocumentsRepository;
    private final RelatedContractsAndOrdersService relatedContractsAndOrdersService;
    private final ServiceContractAssistingEmployeeRepository serviceContractAssistingEmployeeRepository;
    private final ServiceContractInternalIntermediaryRepository serviceContractInternalIntermediaryRepository;
    private final ServiceContractExternalIntermediaryRepository serviceContractExternalIntermediaryRepository;
    private final ContractLinkedServiceContractRepository contractLinkedServiceContractRepository;
    private final ContractLinkedProductContractRepository contractLinkedProductContractRepository;
    private final ServiceContractPodsRepository serviceContractPodsRepository;
    private final ServiceUnrecognizedPodsRepository serviceUnrecognizedPodsRepository;
    private final ServiceContractPriceComponentsRepository serviceContractPriceComponentsRepository;
    private final PriceComponentFormulaVariableRepository priceComponentFormulaVariableRepository;
    private final ServiceContractInterimAdvancePaymentsRepository contractInterimAdvancePaymentsRepository;
    private final InterimAdvancePaymentRepository advancePaymentRepository;
    private final InterimAdvancePaymentTermsService interimAdvancePaymentTermsService;
    private final ServiceContractTermRepository serviceContractTermRepository;
    private final ServicePriceComponentRepository servicePriceComponentRepository;
    private final ServiceInterimAndAdvancePaymentRepository serviceInterimAndAdvancePaymentRepository;
    private final CustomerCommunicationsRepository communicationsRepository;
    private final ExternalIntermediaryRepository externalIntermediaryRepository;
    private final CustomerCommunicationContactsRepository communicationContactsRepository;
    private final CustomerCommContactPurposesRepository commContactPurposesRepository;
    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;
    private final ManagerRepository managerRepository;
    private final TermsGroupTermsRepository termsGroupTermsRepository;
    private final TermsRepository termsRepository;
    private final ServiceAdditionalParamsRepository serviceAdditionalParamsRepository;
    private final ContractTemplateRepository contractTemplateRepository;

    public ServiceContractCreateRequest toCreateServiceContractRequest(Row row, List<String> errorMessages, boolean checkStartDate) {
        ServiceContractCreateRequest request = new ServiceContractCreateRequest();
        ServiceContractBasicParametersCreateRequest basicParameters = new ServiceContractBasicParametersCreateRequest();
        ServiceContractAdditionalParametersRequest additionalParameters = new ServiceContractAdditionalParametersRequest();
        ServiceContractServiceParametersCreateRequest serviceParameters = new ServiceContractServiceParametersCreateRequest();

        String contractNumber = getStringValue(0, row);
        Long contractVersion = getLongValue(1, row);
        String contractCreateEdit = getStringValue(2, row); //C or E
        LocalDate contractStartDate = getDateValue(3, row);
        if (checkStartDate) {
            if (contractStartDate == null) {
                errorMessages.add("Start Date can't be null");
                return null;
            }
        }
        Set<Long> managerForProxy = new HashSet<>();
        //basicParameters.setContractStatusModifyDate(getDateValue(0, row)); //statusModifyDate
        String detailsSubStatus = getStringValue(8, row);
        if (detailsSubStatus != null) {
            basicParameters.setDetailsSubStatus(ServiceContractDetailsSubStatus.valueOf(detailsSubStatus));
        }
        basicParameters.setContractTermUntilAmountIsReached(getDecimalValue(0, row));
        basicParameters.setContractTermUntilAmountIsReachedCheckbox(Boolean.TRUE.equals(getBoolean(getStringValue(0, row), errorMessages, "contractTermUntilAmountIsReachedCheckbox")));
        // Long CurrencyId = getCurrencyId(getStringValue(54, row), errorMessages);
        //basicParameters.setCurrencyId(CurrencyId);
        String customerIdentifier = getStringValue(4, row);
        Customer customer = getCustomer(customerIdentifier, errorMessages);
        if (customer != null) {
            basicParameters.setCustomerId(customer.getId());
            Long customerVersionId = getLongValue(5, row);
            CustomerDetails customerDetails = checkCustomerVersion(customer, customerVersionId, errorMessages);
            if (customerDetails != null) {
                basicParameters.setCustomerVersionId(customerDetails.getVersionId());
                basicParameters.setCommunicationDataForBilling(getLatestCommunications(customerDetails.getId(), errorMessages, true));
                basicParameters.setCommunicationDataForContract(getLatestCommunications(customerDetails.getId(), errorMessages, false));
                managerForProxy = fillManagerForProxy(customerDetails.getId());
            }
        }
        basicParameters.setContractStatusModifyDate(LocalDate.now());
        /*Long communicationDataForBilling = getLongValue(0, row);
        basicParameters.setCommunicationDataForBilling(communicationDataForBilling); //TODO ADD COMMUNICATION DATA FOR BILLING CHECK*/
        Long serviceId = getLongValue(6, row);
        Long serviceDetailsId = getLongValue(7, row);
        if (serviceId != null) {
            EPService service = getEpService(serviceId, errorMessages);
            if (service != null) {
                basicParameters.setServiceId(service.getId());
                if (serviceDetailsId != null) {
                    ServiceDetails serviceDetails = getServiceDetails(service, serviceDetailsId, errorMessages);
                    if (serviceDetails != null) {
                        basicParameters.setServiceVersionId(serviceDetailsId);
                        checkServiceVersionAdditionalParams(serviceDetails, errorMessages);
                    }
                }
            }
        }

        String contractStatus = getStringValue(8, row);
        if (contractStatus != null) {
            basicParameters.setContractStatus(ServiceContractDetailStatus.valueOf(contractStatus));
        }
        String contractSubStatus = getStringValue(9, row);
        if (contractSubStatus != null) {
            basicParameters.setDetailsSubStatus(ServiceContractDetailsSubStatus.valueOf(contractSubStatus));
        }
        String contractType = getStringValue(10, row);
        if (contractType != null) {
            basicParameters.setContractType(ServiceContractContractType.valueOf(contractType));
        }
        String contractVersionStatus = getStringValue(11, row);
        if (contractVersionStatus != null) {
            basicParameters.setContractVersionStatus(ContractVersionStatus.valueOf(contractVersionStatus));
        }
        String contractVersionTypes = getStringValue(12, row);
        if (contractVersionTypes != null) {
            List<Long> contractVersionTypeIds = getContractVersionTypes(contractVersionTypes, errorMessages);
            if (!CollectionUtils.isEmpty(contractVersionTypeIds)) {
                basicParameters.setContractVersionTypes(contractVersionTypeIds);
            } else {
                errorMessages.add("ContractVersionTypes is empty;");
            }
        }

        basicParameters.setSignInDate(getDateValue(13, row)); //signInDate

        basicParameters.setEntryIntoForceDate(getDateValue(14, row)); //entryIntoForceDate

        basicParameters.setStartOfTheInitialTermOfTheContract(getDateValue(15, row));

        basicParameters.setContractTermEndDate(getDateValue(16, row));

        //basicParameters.setTerminationDate(getDateValue(17,row));//TODO In editRequest
        //basicParameters.setPerpetuityDate(getDateValue(18,row));//TODO In editRequest
        BigDecimal unitlAmountIsReached = getDecimalValue(19, row);
        if (unitlAmountIsReached != null) {
            basicParameters.setContractTermUntilAmountIsReached(unitlAmountIsReached);
            basicParameters.setContractTermUntilAmountIsReachedCheckbox(true);
            basicParameters.setCurrencyId(getCurrencyId(getStringValue(20, row), errorMessages));
        }

        basicParameters.setProxy(getProxyParameters(row, errorMessages, managerForProxy));

        List<RelatedEntityRequest> relatedEntityRequestList = getRelatedEntities(row, errorMessages);
        if (!CollectionUtils.isEmpty(relatedEntityRequestList)) {
            basicParameters.setRelatedEntities(relatedEntityRequestList);
        }
        request.setBasicParameters(basicParameters);

        additionalParameters.setBankingDetails(getBankingDetails(row, errorMessages));

        additionalParameters.setInterestRateId(getInterestRateId(row, errorMessages));
        additionalParameters.setEmployeeId(getEmployeeId(row, errorMessages));
        additionalParameters.setAssistingEmployees(getAssistingEmployee(getStringValue(49, row), errorMessages));
        additionalParameters.setInternalIntermediaries(getAssistingEmployee(getStringValue(50, row), errorMessages));
        additionalParameters.setExternalIntermediaries(parseExternalIntermediaries(getStringValue(51, row), errorMessages));
        //TODO find out whats Contract_template - 52 item
        request.setAdditionalParameters(additionalParameters);
        serviceParameters.setPaymentGuarantee(getPaymentGuarantee(row, errorMessages));
        serviceParameters.setCashDepositAmount(getCashDepositAmount(row, errorMessages));
        serviceParameters.setCashDepositCurrencyId(getCurrencyId(getStringValue(55, row), errorMessages));
        serviceParameters.setBankGuaranteeAmount(getBankGuaranteeAmount(row, errorMessages));
        serviceParameters.setBankGuaranteeCurrencyId(getCurrencyId(getStringValue(57, row), errorMessages));
        serviceParameters.setEntryIntoForce(getEntryIntoForce(row, errorMessages));
        serviceParameters.setEntryIntoForceDate(getDateValue(59, row));
        serviceParameters.setStartOfContractInitialTerm(getStartOfTerm(row, errorMessages));
        serviceParameters.setStartOfContractInitialTermDate(getDateValue(61, row));
        serviceParameters.setMonthlyInstallmentAmount(getDecimalValue(62, row));
        serviceParameters.setMonthlyInstallmentNumber(getShortValue(63, row));
        serviceParameters.setPodIds(getPods(row, errorMessages));
        serviceParameters.setUnrecognizedPods(getUnrecognizedPods(row, errorMessages));
        serviceParameters.setContractNumbers(getContractNumbers(row, errorMessages));
        serviceParameters.setGuaranteeContract(false);
        serviceParameters.setContractTermEndDate(getDateValue(16, row));
        serviceParameters.setQuantity(getDecimalValue(67, row));
        checkAndFillServiceParameters(serviceParameters, serviceId, serviceDetailsId, errorMessages);
        request.setServiceParameters(serviceParameters);
        return request;
    }

    private void checkServiceVersionAdditionalParams(ServiceDetails serviceDetails, List<String> errorMessages) {
        List<ServiceAdditionalParams> params = serviceAdditionalParamsRepository.findServiceAdditionalParamsByServiceDetailId(serviceDetails.getId());
        for (ServiceAdditionalParams param : params) {
            if (param.getValue() == null && StringUtils.isNotEmpty(param.getLabel())) {
                errorMessages.add("all values in service additional params must be filled;");
                return;
            }
        }
    }

    private Set<Long> fillManagerForProxy(Long id) {
        List<Manager> managersOptional = managerRepository.findByCustomerDetailIdAndStatusOrderByCreateDateAsc(id, Status.ACTIVE);
        if (!CollectionUtils.isEmpty(managersOptional)) {
            return Set.of(managersOptional.get(0).getId());
        } else return null;
    }

    private List<Long> parseExternalIntermediaries(String names, List<String> errorMessages) {
        List<Long> returnList = new ArrayList<>();
        if (!StringUtils.isEmpty(names)) {
            List<String> splitNames = splitStringToList(names, ",");
            for (String name : splitNames) {
                Optional<ExternalIntermediary> externalIntermediaryOptional = externalIntermediaryRepository.findByNameAndStatus(name, NomenclatureItemStatus.ACTIVE);
                externalIntermediaryOptional.ifPresent(externalIntermediary -> returnList.add(externalIntermediary.getId()));
            }
            return returnList;
        } else {
            //errorMessages.add("ExternalIntermediary name is empty;");
            return null;
        }
    }

    public Long getLatestCommunications(Long customerDetailId, List<String> errorMessage, boolean isBilling) {
        List<CustomerCommunications> contractCommunicationsList =
                communicationsRepository.findByCustomerDetailIdAndStatuses(customerDetailId, List.of(Status.ACTIVE));
        if (!CollectionUtils.isEmpty(contractCommunicationsList)) {
            for (CustomerCommunications item : contractCommunicationsList) {
                if (checkServiceContractCommunicationData(customerDetailId, item.getId(), errorMessage, isBilling)) {
                    return item.getId();
                }
            }
        }
        errorMessage.add("Can't find correct communication data for the customer;");
        return null;
    }

    private boolean checkServiceContractCommunicationData(Long customerDetailsId, Long id, List<String> errorMessages, boolean isBilling) {
        Optional<CustomerCommunications> contractCommunicationsOptional = communicationsRepository.findByIdAndStatuses(id, List.of(Status.ACTIVE));
        if (contractCommunicationsOptional.isEmpty()) {
            //errorMessages.add("communicationDataContractId-Communication data for contract not found!;");
            return false;
        }
        CustomerCommunications contractCommunications = contractCommunicationsOptional.get();
        if (!customerDetailsId.equals(contractCommunications.getCustomerDetailsId())) {
            //errorMessages.add("communicationDataContractId-Contract communications is invalid;");
            return false;
        }
        List<CustomerCommContactPurposes> contractPurposes = commContactPurposesRepository.findByCustomerCommId(contractCommunications.getId(), List.of(Status.ACTIVE));
        Long contractCommunicationId;
        if (isBilling) {
            contractCommunicationId = communicationContactPurposeProperties.getBillingCommunicationId();
        } else {
            contractCommunicationId = communicationContactPurposeProperties.getContractCommunicationId();
        }
        boolean contains = false;
        for (CustomerCommContactPurposes contractPurpose : contractPurposes) {
            if (contractPurpose.getContactPurposeId().equals(contractCommunicationId)) {
                contains = true;
                break;
            }
        }
        if (!contains) {
            //errorMessages.add("communicationDataContractId-Contract communications is invalid;");
            return false;
        }
        return checkForEmailAndNumber(id, errorMessages, "basicParameters.communicationDataContractId-Contract communication should have Email and Mobile number contact types!;");
    }

    private boolean checkForEmailAndNumber(Long id, List<String> errorMessages, String message) {
        List<CustomerCommunicationContacts> contactsList = communicationContactsRepository.findByCustomerCommIdAndStatuses(id, List.of(Status.ACTIVE));

        Set<CustomerCommContactTypes> contactTypes = new HashSet<>();
        contactTypes.add(CustomerCommContactTypes.MOBILE_NUMBER);
        contactTypes.add(CustomerCommContactTypes.EMAIL);

        for (CustomerCommunicationContacts contacts : contactsList) {
            contactTypes.remove(contacts.getContactType());
        }
        //errorMessages.add(message);
        return contactTypes.isEmpty();
    }

    private void checkAndFillServiceParameters(ServiceContractServiceParametersCreateRequest serviceParameters, Long serviceId, Long serviceDetailsId, List<String> errorMessages) {
        checkForServiceStatus(serviceParameters, serviceId, serviceDetailsId, errorMessages);
    }

    private void checkAndFillServiceParametersForEdit(ServiceContractServiceParametersEditRequest serviceParameters, Long serviceId, Long serviceDetailsId, List<String> errorMessages) {
        LocalDateTime now = LocalDateTime.now();
        EPService service = getEpService(serviceId, errorMessages);
        if (service != null) {
            ServiceDetails serviceDetails = getServiceDetails(service, serviceDetailsId, errorMessages);
            if (serviceDetails != null) {
                if (!service.getStatus().equals(ServiceStatus.ACTIVE)) {
                    errorMessages.add("service is not active;");
                }
                if (serviceDetails.getAvailableForSale()) {
                    LocalDateTime availableFrom = serviceDetails.getAvailableFrom();
                    LocalDateTime availableTo = serviceDetails.getAvailableTo();
                    if (availableFrom != null && availableTo != null) {
                        if (now.isBefore(availableFrom) || now.isAfter(availableTo)) {
                            errorMessages.add("current time should be between availableFrom and availableTo;");
                        }
                    } /*else {
                        errorMessages.add("both availableFrom and availableTo should not be null;");
                    }*/
                    if (availableFrom != null && availableTo == null) {
                        if (now.isBefore(availableFrom)) {
                            errorMessages.add("current period can't be before availableFrom;");
                        }
                    }
                    if (availableFrom == null && availableTo != null) {
                        if (now.isAfter(availableTo)) {
                            errorMessages.add("current period can't be after availableTo;");
                        }
                    }
                }
                if (individualServiceCheck(service, serviceDetails.getId(), errorMessages)) {
                    errorMessages.add("Service is individual service;");
                }
                Long id = checkContractTerm(service, serviceDetails, errorMessages);
                if (id != null) {
                    serviceParameters.setContractTermId(id);
                }/* else {
                    errorMessages.add("Invalid contractTerm;");
                }*/

                InvoicePaymentTermsResponse invoicePaymentTermsResponse = invoicePaymentTermCheck(service, serviceDetails);
                if (invoicePaymentTermsResponse != null) {
                    serviceParameters.setInvoicePaymentTermId(invoicePaymentTermsResponse.getId());
                    serviceParameters.setInvoicePaymentTerm(invoicePaymentTermsResponse.getValue());
                } //else errorMessages.add("invalid invoice payment term;");

                List<PriceComponentContractFormula> formulas = priceComponentCheck(serviceDetailsId, serviceDetails, errorMessages);
                if (!CollectionUtils.isEmpty(formulas)) {
                    serviceParameters.setContractFormulas(formulas);
                } //else errorMessages.add("invalid price component;");

                List<ServiceContractInterimAdvancePaymentsRequest> serviceContractInterimAdvancePaymentsRequests =
                        checkInterimAdvancePayments(serviceDetailsId, serviceDetails, errorMessages);
                if (!CollectionUtils.isEmpty(serviceContractInterimAdvancePaymentsRequests)) {
                    serviceParameters.setInterimAdvancePaymentsRequests(serviceContractInterimAdvancePaymentsRequests);
                } /*else {
                    errorMessages.add("invalid price component;");
                }*/
            }
        }
    }

    private void checkForServiceStatus(ServiceContractServiceParametersCreateRequest serviceParameters, Long serviceId, Long serviceDetailsId, List<String> errorMessages) {
        LocalDateTime now = LocalDateTime.now();
        EPService service = getEpService(serviceId, errorMessages);
        if (service != null) {
            ServiceDetails serviceDetails = getServiceDetails(service, serviceDetailsId, errorMessages);
            if (serviceDetails != null) {
                if (!service.getStatus().equals(ServiceStatus.ACTIVE)) {
                    errorMessages.add("service is not active;");
                }
                if (serviceDetails.getAvailableForSale()) {
                    LocalDateTime availableFrom = serviceDetails.getAvailableFrom();
                    LocalDateTime availableTo = serviceDetails.getAvailableTo();
                    if (availableFrom != null && availableTo != null) {
                        if (now.isBefore(availableFrom) || now.isAfter(availableTo)) {
                            errorMessages.add("current time should be between availableFrom and availableTo;");
                        }
                    }
                    if (availableFrom != null && availableTo == null) {
                        if (now.isBefore(availableFrom)) {
                            errorMessages.add("current period can't be before availableFrom;");
                        }
                    }
                    if (availableFrom == null && availableTo != null) {
                        if (now.isAfter(availableTo)) {
                            errorMessages.add("current period can't be after availableTo;");
                        }
                    }
                }
                if (individualServiceCheck(service, serviceDetails.getId(), errorMessages)) {
                    errorMessages.add("Service is individual service;");
                }
                Long id = checkContractTerm(service, serviceDetails, errorMessages);
                if (id != null) {
                    serviceParameters.setContractTermId(id);
                }/* else {
                    errorMessages.add("Invalid contractTerm;");
                }*/

                InvoicePaymentTermsResponse invoicePaymentTermsResponse = invoicePaymentTermCheck(service, serviceDetails);
                if (invoicePaymentTermsResponse != null) {
                    serviceParameters.setInvoicePaymentTermId(invoicePaymentTermsResponse.getId());
                    serviceParameters.setInvoicePaymentTerm(invoicePaymentTermsResponse.getValue());
                } //else errorMessages.add("invalid invoice payment term;");

                List<PriceComponentContractFormula> formulas = priceComponentCheck(serviceDetailsId, serviceDetails, errorMessages);
                if (!CollectionUtils.isEmpty(formulas)) {
                    serviceParameters.setContractFormulas(formulas);
                } //else errorMessages.add("invalid price component;");

                List<ServiceContractInterimAdvancePaymentsRequest> serviceContractInterimAdvancePaymentsRequests =
                        checkInterimAdvancePayments(serviceDetailsId, serviceDetails, errorMessages);
                if (!CollectionUtils.isEmpty(serviceContractInterimAdvancePaymentsRequests)) {
                    serviceParameters.setInterimAdvancePaymentsRequests(serviceContractInterimAdvancePaymentsRequests);
                } /*else {
                    errorMessages.add("invalid price component;");
                }*/
            }
        }
    }

    private List<ServiceContractInterimAdvancePaymentsRequest> checkInterimAdvancePayments(Long serviceDetailsId, ServiceDetails serviceDetails,/* ServiceContractServiceParametersCreateRequest serviceParameters,*/ List<String> errorMessages) {
        List<String> localErrorMessages = new ArrayList<>();
        //List<ServiceInterimAndAdvancePayment> interimAndAdvancePayments = serviceDetails.getInterimAndAdvancePayments();
        List<ServiceInterimAndAdvancePayment> interimAndAdvancePayments =
                serviceInterimAndAdvancePaymentRepository.findByServiceDetailsIdAndStatusIn(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));
        List<ServiceContractInterimAdvancePaymentsRequest> interimAdvancePaymentsRequests = new ArrayList<>();
        if (!CollectionUtils.isEmpty(interimAndAdvancePayments)) {
            for (ServiceInterimAndAdvancePayment item : interimAndAdvancePayments) {
                Boolean shouldAdd = true;
                ServiceContractInterimAdvancePaymentsRequest serviceContractInterimAdvancePaymentsRequest = new ServiceContractInterimAdvancePaymentsRequest();
                //InterimAdvancePayment interimAndAdvancePayment = item.getInterimAndAdvancePayment();
                InterimAdvancePayment interimAndAdvancePayment = null;
                Optional<InterimAdvancePayment> interimAndAdvancePaymentOptional = advancePaymentRepository.findByIdAndStatus(item.getInterimAndAdvancePayment().getId(), InterimAdvancePaymentStatus.ACTIVE);
                if (interimAndAdvancePaymentOptional.isPresent()) {
                    interimAndAdvancePayment = interimAndAdvancePaymentOptional.get();
                    if (Objects.equals(interimAndAdvancePayment.getPaymentType(), PaymentType.OBLIGATORY)) {
                        if (interimAndAdvancePayment.getValueType().equals(ValueType.PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT) ||
                            interimAndAdvancePayment.getValueType().equals(ValueType.EXACT_AMOUNT)) {
                            if (interimAndAdvancePayment.getValue() == null) {
                                localErrorMessages.add("When Interim advance payment value typs is OBLIGATORY or PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT value should be present");
                                shouldAdd = false;
                            }
                        }
                        if (interimAndAdvancePayment.getValueType().equals(ValueType.PRICE_COMPONENT)) {
                            if (interimAndAdvancePayment.getValue() == null) {
                                localErrorMessages.add("When Interim advance payment value type is PRICE_COMPONENT value should be present");
                                shouldAdd = false;
                            }
                        }
                        if (interimAndAdvancePayment.getDateOfIssueType().equals(DateOfIssueType.MATCH_THE_INVOICE_DATE)) {
                            if (shouldAdd) {
                                serviceContractInterimAdvancePaymentsRequest.setInterimAdvancePaymentId(interimAndAdvancePayment.getId());
                                interimAdvancePaymentsRequests.add(serviceContractInterimAdvancePaymentsRequest);
                            }
                        } else if (interimAndAdvancePayment.getDateOfIssueType().equals(DateOfIssueType.WORKING_DAYS_AFTER_INVOICE_DATE)) {
                            if (interimAndAdvancePayment.getDateOfIssueValue() == null) {
                                localErrorMessages.add("interim and advance payment with date of issue type DAYS AFTER INVOICE DATE must have dateOfIssueValue filled");
                                shouldAdd = false;
                            }
                            if (interimAndAdvancePayment.getDateOfIssueValueFrom() != null) {
                                localErrorMessages.add("interim and advance payment with date of issue type DAYS AFTER INVOICE DATE must not have dateOfIssueValueFrom filled");
                                shouldAdd = false;
                            }
                            if (interimAndAdvancePayment.getDateOfIssueValueTo() != null) {
                                localErrorMessages.add("interim and advance payment with date of issue type DAYS AFTER INVOICE DATE must not have dateOfIssueValueTo filled");
                                shouldAdd = false;
                            }
                        }
                    }
                } else {
                    localErrorMessages.add("interimAndAdvancePayment shouldn't be null;");
                }
                if (shouldAdd) {
                    if (interimAndAdvancePayment != null) {
                        serviceContractInterimAdvancePaymentsRequest.setInterimAdvancePaymentId(interimAndAdvancePayment.getId());
                        interimAdvancePaymentsRequests.add(serviceContractInterimAdvancePaymentsRequest);
                    }
                }
            }
        } else return null;
        errorMessages.addAll(localErrorMessages);
        return interimAdvancePaymentsRequests;
    }

    private Long checkContractTerm(EPService service,
                                   ServiceDetails serviceDetails,
            /*ServiceContractServiceParametersCreateRequest serviceParameters,*/
                                   List<String> errorMessages) {
        // List<ServiceContractTerm> contractTerms = serviceDetails.getContractTerms();
        //serviceContractTermRepository.findByServiceDetailsIdAndStatusInOrderByCreateDate(details.getId(), List.of(ServiceSubobjectStatus.ACTIVE))
        List<ServiceContractTerm> contractTerms =
                serviceContractTermRepository.findByServiceDetailsIdAndStatusInOrderByCreateDate(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));
        if (!CollectionUtils.isEmpty(contractTerms)) {
            if (contractTerms.size() > 1) {
                errorMessages.add("Contract Terms can't be more than 1;");
            } else {
                ServiceContractTerm contractTerm = contractTerms.get(0);
                return contractTerm.getId();
                //serviceParameters.setContractTermEndDate(); //TODO SHOULD BE FILLED IN
            }
        } else return null;
        return null;
    }

    /*private Boolean validServiceParameters(Long serviceId, Long serviceVersion, List<String> errorMessages) {
        LocalDateTime now = LocalDateTime.now();
        EPService service = getEpService(serviceId, errorMessages);
        if (service != null) {
            ServiceDetails serviceDetails = getServiceDetails(service, serviceVersion, errorMessages);
            if (serviceDetails != null) {

                if (!service.getStatus().equals(ServiceStatus.ACTIVE)) {
                    errorMessages.add("service is not active;");
                    return false;
                }
                if (serviceDetails.getAvailableForSale()) {
                    LocalDateTime availableFrom = serviceDetails.getAvailableFrom();
                    LocalDateTime availableTo = serviceDetails.getAvailableTo();
                    if (availableFrom != null && availableTo != null) {
                        if (now.isBefore(availableFrom) || now.isAfter(availableTo)) {
                            errorMessages.add("current time should be between availableFrom and availableTo;");
                            return false;
                        }
                    } else {
                        errorMessages.add("availableFrom and/or availableTo should not be null;");
                        return false;
                    }
                    //TODO CONTINUE LOGIC
                    return false;
                }
                if (individualServiceCheck(service, serviceDetails.getId(), errorMessages)) {
                    errorMessages.add("Service is individual service;");
                    return false;
                }
                //TODO CONTRACT TERM LOGIC
               *//* if (invoicePaymentTermCheck(service, serviceDetails)) {
                    errorMessages.add("invalid invoice payment term;");
                    return false;
                }
                if (priceComponentCheck(serviceVersion, serviceDetails)) {
                    errorMessages.add("invalid price component;");
                    return false;
                }*//*
            }
        }
        return true;
    }*/

    private List<PriceComponentContractFormula> priceComponentCheck(Long serviceVersion, ServiceDetails serviceDetails, /*ServiceContractServiceParametersCreateRequest serviceParameters,*/ List<String> errorMessages) {
        //List<ServicePriceComponent> priceComponents = serviceDetails.getPriceComponents();
        List<ServicePriceComponent> priceComponents =
                servicePriceComponentRepository.findByServiceDetailsIdAndStatusIn(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));
        List<PriceComponentContractFormula> contractFormulas = new ArrayList<>();
        if (!CollectionUtils.isEmpty(priceComponents)) {
            for (ServicePriceComponent item : priceComponents) {
                //List<PriceComponentFormulaVariable> formulaVariables = item.getPriceComponent().getFormulaVariables();
                List<PriceComponentFormulaVariable> formulaVariables = priceComponentFormulaVariableRepository.findByPriceComponent(item.getPriceComponent());
                if (!CollectionUtils.isEmpty(formulaVariables)) {
                    for (PriceComponentFormulaVariable formulaVariable : formulaVariables) {
                        if (formulaVariable.getValue() != null) {
                            PriceComponentContractFormula formula = new PriceComponentContractFormula();
                            formula.setFormulaVariableId(formulaVariable.getId());
                            formula.setValue(formulaVariable.getValue());
                            contractFormulas.add(formula);
                        }
                    }
                }
            }
        }
        if (!CollectionUtils.isEmpty(contractFormulas)) {
            return contractFormulas;
        } else {
            return null;
        }
    }

    private InvoicePaymentTermsResponse invoicePaymentTermCheck(EPService service, ServiceDetails serviceDetails/*, ServiceContractServiceParametersCreateRequest serviceParameters*/) {
        Terms term = null;
        TermsGroups termsGroups = serviceDetails.getTermsGroups();
        if (serviceDetails.getTerms() != null) {
            term = serviceDetails.getTerms();
        } else if (termsGroups != null) {
            Long termId = termsRepository.getTermIdFromCurrentTermGroup(termsGroups.getId());
            if (termId != null) {
                Optional<Terms> termsOptional = termsRepository.findByIdAndStatusIn(termId, List.of(TermStatus.ACTIVE));
                if (termsOptional.isPresent()) {
                    term = termsOptional.get();
                }
            }
        }
        if (term != null) {
            InvoicePaymentTermsResponse paymentTermsResponse = null;
            List<InvoicePaymentTermsResponse> paymentTermsResponses =
                    invoicePaymentTermsRepository.findDetailedByTermIdAndStatusIn(term.getId(), List.of(PaymentTermStatus.ACTIVE));
            if (paymentTermsResponses.size() > 1) {
                return null;
            } else {
                paymentTermsResponse = paymentTermsResponses.get(0);
            }
            /*if (paymentTermsResponse.getId() != null) {
                    serviceParameters.setInvoicePaymentTermId(paymentTermsResponse.getId());
                } else return true;
                if (paymentTermsResponse.getValue() != null) {
                    serviceParameters.setInvoicePaymentTerm(paymentTermsResponse.getValue());
                } else return true;*/
            return paymentTermsResponse;
        }
        return null;
    }

    private Boolean individualServiceCheck(EPService service, Long serviceContractDetailsId, List<String> errorMessages) {
        if (service.getCustomerIdentifier() == null) {
            return false;
        }
        return serviceContractsRepository.existsByServiceIdAndContractIdNotEquals(service.getId(), serviceContractDetailsId);
    }

    private List<String> getContractNumbers(Row row, List<String> errorMessages) {
        String contractNumbersString = getStringValue(66, row);
        List<String> contractNumbers = new ArrayList<>();
        if (!StringUtils.isEmpty(contractNumbersString)) {
            contractNumbers = splitStringToList(contractNumbersString, ",");
            if (!CollectionUtils.isEmpty(contractNumbers)) {
                return contractNumbers;
            }
        }
        return null;
    }

    private List<String> getUnrecognizedPods(Row row, List<String> errorMessages) {
        String unrecognizedPodsString = getStringValue(65, row);
        List<String> unrecognizedPods = new ArrayList<>();
        if (!StringUtils.isEmpty(unrecognizedPodsString)) {
            unrecognizedPods = splitStringToList(unrecognizedPodsString, ",");
            if (!CollectionUtils.isEmpty(unrecognizedPods)) {
                return unrecognizedPods;
            }
        }
        return null;
    }

    private List<Long> getPods(Row row, List<String> errorMessages) {
        List<Long> podIds = new ArrayList<>();
        String pods = getStringValue(64, row);
        if (!StringUtils.isEmpty(pods)) {
            List<String> podNumbers = splitStringToList(pods, ",");
            if (!CollectionUtils.isEmpty(podNumbers)) {
                for (String podNumber : podNumbers) {
                    podIds.add(getPodId(podNumber, errorMessages));
                }
            }
        }
        return podIds;
    }

    private Long getPodId(String podNumber, List<String> errorMessages) {
        Optional<PointOfDelivery> podOptional = pointOfDeliveryRepository.findByIdentifierAndStatusIn(podNumber, List.of(PodStatus.ACTIVE));
        if (podOptional.isPresent()) {
            return podOptional.get().getId();
        } else {
            // errorMessages.add("Can't find pod with identifier:%s;".formatted(podNumber));
            return null;
        }
    }

    private StartOfContractInitialTerm getStartOfTerm(Row row, List<String> errorMessages) {
        String startOfInitialTerm = getStringValue(60, row);
        if (!StringUtils.isEmpty(startOfInitialTerm)) {
            return StartOfContractInitialTerm.valueOf(startOfInitialTerm);
        } else {
            //errorMessages.add("Can't parse startOfTerm;");
            return null;
        }
    }

    private ContractEntryIntoForce getEntryIntoForce(Row row, List<String> errorMessages) {
        String entryIntoForce = getStringValue(58, row);
        if (!StringUtils.isEmpty(entryIntoForce)) {
            return ContractEntryIntoForce.valueOf(entryIntoForce);
        } else {
            //errorMessages.add("Can't parse entryIntoForce;");
            return null;
        }
    }

    private BigDecimal getBankGuaranteeAmount(Row row, List<String> errorMessages) {
        BigDecimal bankGuaranteeAmount = getDecimalValue(56, row);
        //errorMessages.add("Can't parse BankGuaranteeAmount;");
        return bankGuaranteeAmount;
    }

    private BigDecimal getCashDepositAmount(Row row, List<String> errorMessages) {
        BigDecimal cashDepositAmount = getDecimalValue(54, row);
        // errorMessages.add("Can't parse cashDepositAmount;");
        return cashDepositAmount;
    }

    private PaymentGuarantee getPaymentGuarantee(Row row, List<String> errorMessages) {
        String paymentGuarantee = getStringValue(53, row);
        if (!StringUtils.isEmpty(paymentGuarantee)) {
            return PaymentGuarantee.valueOf(paymentGuarantee);
        } else {
            //errorMessages.add("Can't parse paymentGuarantee;");
            return null;
        }
    }

    private List<Long> getAssistingEmployee(String assistingEmployees, List<String> errorMessages) {
        List<Long> userIds = new ArrayList<>();
        if (!StringUtils.isEmpty(assistingEmployees)) {
            List<String> usernames = splitStringToList(assistingEmployees, ",");
            for (String userName : usernames) {
                AccountManager accountManager = getAccountManagerByUserName(userName, errorMessages);
                if (accountManager != null) {
                    userIds.add(accountManager.getId());
                } else {
                    //errorMessages.add("Can't find employee with userName:%s;".formatted(userName));
                }
            }
        }
        return userIds;
    }

    private Long getEmployeeId(Row row, List<String> errorMessages) {
        String userName = getStringValue(48, row);
        if (!StringUtils.isEmpty(userName)) {
            AccountManager accountManager = getAccountManagerByUserName(userName, errorMessages);
            if (accountManager != null) {
                return accountManager.getId();
            }
        } else {
            //  errorMessages.add("Can't parse employee username;");
        }
        return null;
    }

    private AccountManager getAccountManagerByUserName(String userName, List<String> errorMessages) {
        Optional<AccountManager> employeeOptional = accountManagerRepository.findByUserNameAndStatusIn(userName, List.of(Status.ACTIVE));
        if (employeeOptional.isPresent()) {
            return employeeOptional.get();
        } else {
            errorMessages.add("Unable to find employee with username %s;".formatted(userName));
            return null;
        }
    }

    private Long getInterestRateId(Row row, List<String> errorMessages) {
        String interestRateName = getStringValue(47, row);
        if (!StringUtils.isEmpty(interestRateName)) {
            Optional<InterestRate> interestRateOptional = interestRateRepository.findByNameAndStatusIn(interestRateName, List.of(InterestRateStatus.ACTIVE));
            if (interestRateOptional.isPresent()) {
                return interestRateOptional.get().getId();
            }
        }
        return null;
    }

    private List<RelatedEntityRequest> getRelatedEntities(Row row, List<String> errorMessages) {
        List<RelatedEntityRequest> relatedEntityRequests = new ArrayList<>();
        String contractNumberString = getStringValue(43, row);
        if (!StringUtils.isEmpty(contractNumberString)) {

            List<String> contractNumbers = splitStringToList(contractNumberString, ",");
            if (!CollectionUtils.isEmpty(contractNumbers)) {
                for (String contractNumber : contractNumbers) {
                    Boolean found = false;
                    Optional<ServiceContracts> serviceContractsOptional = serviceContractsRepository.findByContractNumberAndStatus(contractNumber, EntityStatus.ACTIVE);
                    if (serviceContractsOptional.isPresent()) {
                        found = true;
                        ServiceContracts serviceContracts = serviceContractsOptional.get();
                        relatedEntityRequests.add(relatedEntityEntity(serviceContracts.getId(), RelatedEntityType.SERVICE_CONTRACT));
                    }
                    Optional<ProductContract> productContractOptional = productContractRepository.findByContractNumberAndStatus(contractNumber, ProductContractStatus.ACTIVE);
                    if (productContractOptional.isPresent()) {
                        found = true;
                        ProductContract productContract = productContractOptional.get();
                        relatedEntityRequests.add(relatedEntityEntity(productContract.getId(), RelatedEntityType.PRODUCT_CONTRACT));
                    }
                    Optional<ServiceOrder> serviceOrderOptional = serviceOrderRepository.findByOrderNumberAndStatus(contractNumber, EntityStatus.ACTIVE);
                    if (serviceOrderOptional.isPresent()) {
                        found = true;
                        ServiceOrder serviceOrder = serviceOrderOptional.get();
                        relatedEntityRequests.add(relatedEntityEntity(serviceOrder.getId(), RelatedEntityType.SERVICE_ORDER));
                    }
                    Optional<GoodsOrder> goodsOrderOptional = goodsOrderRepository.findByOrderNumberAndStatus(contractNumber, EntityStatus.ACTIVE);
                    if (goodsOrderOptional.isPresent()) {
                        found = true;
                        GoodsOrder goodsOrder = goodsOrderOptional.get();
                        relatedEntityRequests.add(relatedEntityEntity(goodsOrder.getId(), RelatedEntityType.GOODS_ORDER));
                    }
                    if (!found) {
                        errorMessages.add("Can't find Contract with contract number:%s;".formatted(contractNumber));
                    }
                }
            }
            return relatedEntityRequests;
        } else {
            //errorMessages.add("Contract numbers shouldn't be null");
            return null;
        }
    }

    private RelatedEntityRequest relatedEntityEntity(Long id, RelatedEntityType relatedEntityType) {
        RelatedEntityRequest request = new RelatedEntityRequest();
        request.setRelatedEntityId(id);
        request.setRelatedEntityType(relatedEntityType);
        return request;
    }

    private ServiceContractBankingDetails getBankingDetails(Row row, List<String> errorMessages) {
        ServiceContractBankingDetails bankingDetails = new ServiceContractBankingDetails();
        bankingDetails.setDirectDebit(Boolean.TRUE.equals(getBoolean(getStringValue(44, row), errorMessages, "Contract_direct_debit")));
        Bank bank = getBank(row, errorMessages);
        if (bank != null) {
            bankingDetails.setBankId(bank.getId());
        }
        bankingDetails.setIban(getStringValue(46, row));
        return bankingDetails;
    }

    private Bank getBank(Row row, List<String> errorMessages) {
        String bankName = getStringValue(45, row);
        if (!StringUtils.isEmpty(bankName)) {
            Optional<Bank> bankOptional = bankRepository.findByNameAndStatusIn(bankName, List.of(NomenclatureItemStatus.ACTIVE));
            if (bankOptional.isPresent()) {
                return bankOptional.get();
            } else {
                errorMessages.add("Can't find bank by name:%s;".formatted(bankName));
                return null;
            }
        }
        return null;
    }

    private List<ProxyEditRequest> getProxyParameters(Row row, List<String> errorMessages, Set<Long> managerIds) {
        List<ProxyEditRequest> proxyAddRequests = new ArrayList<>();
        ProxyEditRequest proxyAddRequest = new ProxyEditRequest();
        proxyAddRequest.setProxyName(getStringValue(21, row));
        Boolean entityOrPerson = getBoolean(getStringValue(22, row), errorMessages, "Contract_proxy_Foreign_Entity_or_Person");
        proxyAddRequest.setProxyForeignEntityPerson(entityOrPerson);
        String proxyCustomerIdentifier = getStringValue(23, row);
        proxyAddRequest.setProxyCustomerIdentifier(proxyCustomerIdentifier == null ? "" : proxyCustomerIdentifier);
        proxyAddRequest.setProxyEmail(getStringValue(24, row));
        proxyAddRequest.setProxyPhone(getStringValue(25, row));
        proxyAddRequest.setProxyPowerOfAttorneyNumber(getStringValue(26, row));
        proxyAddRequest.setProxyData(getDateValue(27, row));
        proxyAddRequest.setProxyValidTill(getDateValue(28, row));
        proxyAddRequest.setNotaryPublic(getStringValue(29, row));
        proxyAddRequest.setRegistrationNumber(getStringValue(30, row));
        proxyAddRequest.setAreaOfOperation(getStringValue(31, row));
        proxyAddRequest.setProxyAuthorizedByProxy(getStringValue(32, row));
        Boolean AuthProxyEntityOrPerson = getBoolean(getStringValue(33, row), errorMessages, "Contract_proxy_authorized_Foreign_Entity_or_Person");
        proxyAddRequest.setAuthorizedProxyForeignEntityPerson(AuthProxyEntityOrPerson);
        proxyAddRequest.setAuthorizedProxyCustomerIdentifier(getStringValue(34, row));
        proxyAddRequest.setAuthorizedProxyEmail(getStringValue(35, row));
        proxyAddRequest.setAuthorizedProxyPhone(getStringValue(36, row));
        proxyAddRequest.setAuthorizedProxyPowerOfAttorneyNumber(getStringValue(37, row));
        proxyAddRequest.setAuthorizedProxyData(getDateValue(38, row));
        proxyAddRequest.setAuthorizedProxyValidTill(getDateValue(39, row));
        proxyAddRequest.setAuthorizedProxyNotaryPublic(getStringValue(40, row));
        proxyAddRequest.setAuthorizedProxyRegistrationNumber(getStringValue(41, row));
        proxyAddRequest.setAuthorizedProxyAreaOfOperation(getStringValue(42, row));
        //proxyAddRequest.setProxyCustomerType(CustomerType.valueOf(getStringValue(66,row)));
        //proxyAddRequest.setAuthorizedProxyCustomerType(CustomerType.valueOf(getStringValue(67,row)));
        if (proxyAddRequest.getProxyForeignEntityPerson() != null ||
            proxyAddRequest.getAuthorizedProxyForeignEntityPerson() != null) {
            proxyAddRequests.add(proxyAddRequest);
        }
        proxyAddRequest.setManagerIds(managerIds);
        return CollectionUtils.isEmpty(proxyAddRequests) ? null : proxyAddRequests;
    }

    private List<ProxyEditRequest> getProxyEditParameters(Row row, List<String> errorMessages, Set<Long> managerIds) {
        /*List<ProxyEditRequest> newProxyAddRequest = getProxyParameters(row, errorMessages);
        ProxyEditRequest proxyEditRequest = ProxyAddRequestToProxyEditRequest(newProxyAddRequest);
        proxyEditRequest.setId(null);*/
        return getProxyParameters(row, errorMessages, managerIds);
    }

    private ProxyEditRequest ProxyAddRequestToProxyEditRequest(List<ProxyEditRequest> proxyAddRequest) {
        ProxyEditRequest proxyEditRequest = new ProxyEditRequest();
        for (ProxyEditRequest proxy : proxyAddRequest) {
            proxyEditRequest.setProxyForeignEntityPerson(proxy.getProxyForeignEntityPerson());
            proxyEditRequest.setProxyName(proxy.getProxyName());
            proxyEditRequest.setProxyCustomerIdentifier(proxy.getProxyCustomerIdentifier());
            //proxyEditRequest.setProxyCustomerType(proxy.getProxyCustomerType());
            proxyEditRequest.setProxyEmail(proxy.getProxyEmail());
            proxyEditRequest.setProxyPhone(proxy.getProxyPhone());
            proxyEditRequest.setProxyPowerOfAttorneyNumber(proxy.getProxyPowerOfAttorneyNumber());
            proxyEditRequest.setProxyData(proxy.getProxyData());
            proxyEditRequest.setProxyValidTill(proxy.getProxyValidTill());
            proxyEditRequest.setNotaryPublic(proxy.getNotaryPublic());
            proxyEditRequest.setRegistrationNumber(proxy.getRegistrationNumber());
            proxyEditRequest.setAreaOfOperation(proxy.getAreaOfOperation());
            proxyEditRequest.setAuthorizedProxyForeignEntityPerson(proxy.getAuthorizedProxyForeignEntityPerson());
            proxyEditRequest.setProxyAuthorizedByProxy(proxy.getProxyAuthorizedByProxy());
            proxyEditRequest.setAuthorizedProxyCustomerIdentifier(proxy.getAuthorizedProxyCustomerIdentifier());
            //proxyEditRequest.setAuthorizedProxyCustomerType(proxy.getAuthorizedProxyCustomerType());
            proxyEditRequest.setAuthorizedProxyEmail(proxy.getAuthorizedProxyEmail());
            proxyEditRequest.setAuthorizedProxyPhone(proxy.getAuthorizedProxyPhone());
            proxyEditRequest.setAuthorizedProxyPowerOfAttorneyNumber(proxy.getAuthorizedProxyPowerOfAttorneyNumber());
            proxyEditRequest.setAuthorizedProxyData(proxy.getAuthorizedProxyData());
            proxyEditRequest.setAuthorizedProxyValidTill(proxy.getAuthorizedProxyValidTill());
            proxyEditRequest.setAuthorizedProxyNotaryPublic(proxy.getAuthorizedProxyNotaryPublic());
            proxyEditRequest.setAuthorizedProxyRegistrationNumber(proxy.getAuthorizedProxyRegistrationNumber());
            proxyEditRequest.setAuthorizedProxyAreaOfOperation(proxy.getAuthorizedProxyAreaOfOperation());
            proxyEditRequest.setManagerIds(proxy.getManagerIds());
        }
        return proxyEditRequest;
    }

    public List<Long> getContractVersionTypesWithContract(Long id) {
        List<Long> contractVersionTypeIds = new ArrayList<>();
        List<ServiceContractContractVersionTypes> contractVersionTypes = serviceContractContractVersionTypesRepository.findByContractDetailIdAndStatusIn(id, List.of(ContractSubObjectStatus.ACTIVE));
        if (!CollectionUtils.isEmpty(contractVersionTypes)) {
            contractVersionTypeIds = contractVersionTypes.stream()
                    .map(ServiceContractContractVersionTypes::getContractVersionTypeId)
                    .toList();
        }
        if (!CollectionUtils.isEmpty(contractVersionTypeIds)) {
            return contractVersionTypeIds;
        } else return null;
    }

    private List<Long> getContractVersionTypes(String contractVersionTypes, List<String> errorMessages) {
        List<Long> nomenclatureIds = new ArrayList<>();
        List<String> nomenclatureNames = splitStringToList(contractVersionTypes, ",");
        if (!CollectionUtils.isEmpty(nomenclatureNames)) {
            for (String name : nomenclatureNames) {
                ContractVersionType contractVersionType = checkContractVersionTypeNomenclature(name, errorMessages);
                if (contractVersionType != null) {
                    nomenclatureIds.add(contractVersionType.getId());
                }
            }
        } else {
            errorMessages.add("ContractVersionTypes is empty;");
        }
        return nomenclatureIds;
    }

    private ContractVersionType checkContractVersionTypeNomenclature(String name, List<String> errorMessages) {
        Optional<ContractVersionType> contractVersionTypeOptional = contractVersionTypesRepository.findByNameAndStatusIn(name, List.of(NomenclatureItemStatus.ACTIVE));
        if (contractVersionTypeOptional.isPresent()) {
            return contractVersionTypeOptional.get();
        } else {
            errorMessages.add("Can't find contractVersionType with name:%s;".formatted(name));
            return null;
        }
    }

    private List<String> splitStringToList(String contractVersionTypes, String delimiter) {
        String[] chunks = contractVersionTypes.split(delimiter);
        return List.of(chunks);
    }

    private ServiceDetails getServiceDetails(EPService service, Long serviceDetailsId, List<String> errorMessages) {
        Optional<ServiceDetails> serviceDetails =
                serviceDetailsRepository.findByServiceIdAndVersionAndStatusIn(service.getId(), serviceDetailsId, List.of(ServiceDetailStatus.ACTIVE));
        if (serviceDetails.isPresent()) {
            return serviceDetails.get();
        } else {
            errorMessages.add("Can't find Service version with serviceId: %s and datailsId:%s ;".formatted(service.getId(), serviceDetailsId));
            return null;
        }
    }

    private EPService getEpService(Long serviceId, List<String> errorMessages) {
        Optional<EPService> service = serviceRepository.findByIdAndStatusIn(serviceId, List.of(ServiceStatus.ACTIVE));
        if (service.isPresent()) {
            return service.get();
        } else {
            errorMessages.add("Can't find Service with id: %s;".formatted(serviceId));
            return null;
        }
    }

    private CustomerDetails checkCustomerVersion(Customer customer, Long customerVersionId, List<String> errorMessages) {
        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findByCustomerIdAndVersionId(customer.getId(), customerVersionId);
        if (customerDetailsOptional.isPresent()) {
            return customerDetailsOptional.get();
        } else {
            errorMessages.add("Can't find customerVersion with customerId: %s and customerVersionId: %s;".formatted(customer.getId(), customerVersionId));
            return null;
        }

    }

    public Customer getCustomer(String customerIdentifier, List<String> errorMessages) {
        Optional<Customer> customer = customerRepository.findByIdentifierAndStatus(customerIdentifier, CustomerStatus.ACTIVE);
        if (customer.isPresent()) {
            return customer.get();
        } else {
            errorMessages.add("Can't find customer with identifier:%s;".formatted(customerIdentifier));
            return null;
        }
    }

    public Customer getCustomerWithDetailsId(Long detailsId, List<String> errorMessages) {
        Optional<Customer> customer = customerRepository.findByCustomerDetailIdAndStatusIn(detailsId, List.of(CustomerStatus.ACTIVE));
        if (customer.isPresent()) {
            return customer.get();
        } else {
            errorMessages.add("Can't find customer with detailsId:%s;".formatted(detailsId));
            return null;
        }
    }

    private Long getCurrencyId(String currencyName, List<String> errorMessages) {
        Long result = null;
        if (currencyName != null) {
            Optional<CacheObject> currencyOptional = currencyRepository
                    .getCacheObjectByNameAndStatus(currencyName, NomenclatureItemStatus.ACTIVE);
            if (currencyOptional.isPresent()) {
                result = currencyOptional.get().getId();
            } else {
                errorMessages.add("currencyId-Not found currency with name: " + currencyName + ";");
            }
        }
        return result;
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
            if (row.getCell(columnNumber).getCellType() == CellType.NUMERIC) {
                result = Integer.valueOf((int) row.getCell(columnNumber).getNumericCellValue());
            } else {
                throw new ClientException("providedPower-Invalid cell type for provided power in row " + columnNumber + ";", ILLEGAL_ARGUMENTS_PROVIDED);
            }
            return result;
        } else {
            return null;
        }
    }

    private Short getShortValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            Short result;
            if (row.getCell(columnNumber).getCellType() == CellType.NUMERIC) {
                result = Short.valueOf((short) row.getCell(columnNumber).getNumericCellValue());
            } else {
                throw new ClientException("providedPower-Invalid cell type for provided power in row " + columnNumber + ";", ILLEGAL_ARGUMENTS_PROVIDED);
            }
            return result;
        } else {
            return null;
        }
    }

    private BigDecimal getDecimalValue(int columnNumber, Row row) {

        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {

            BigDecimal result;
            if (row.getCell(columnNumber).getCellType() == CellType.STRING) {
                result = BigDecimal.valueOf(Long.parseLong(row.getCell(columnNumber).getStringCellValue()));
            } else if (row.getCell(columnNumber).getCellType() == CellType.NUMERIC) {
                result = BigDecimal.valueOf(row.getCell(columnNumber).getNumericCellValue());
            } else {
                throw new ClientException("providedPower-Invalid cell type for provided power in row " + columnNumber + ";", ILLEGAL_ARGUMENTS_PROVIDED);
            }
            return result;
        } else {
            return null;
        }
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

    public ServiceContracts getServiceContractWithNumber(String contractNumber, List<String> errorMessages) {
        Optional<ServiceContracts> serviceContractsOptional = serviceContractsRepository.findByContractNumberAndStatus(contractNumber, EntityStatus.ACTIVE);
        if (serviceContractsOptional.isPresent()) {
            return serviceContractsOptional.get();
        } else {
            errorMessages.add("Can't find Service contract with number:%s;".formatted(contractNumber));
            return null;
        }
    }

    public ServiceContractDetails getServiceContractDetails(Long serviceContractId, Long contractVersion, List<String> errorMessages) {
        if (serviceContractId != null && contractVersion != null) {
            Optional<ServiceContractDetails> serviceContractDetailsOptional =
                    serviceContractDetailsRepository.findByContractIdAndVersionId(serviceContractId, contractVersion);
            if (serviceContractDetailsOptional.isPresent()) {
                return serviceContractDetailsOptional.get();
            } else {
                errorMessages.add("Can't find service contract version;");
                return null;
            }
        } else {
            errorMessages.add("ServiceContract Id or contractVersion is null;");
            return null;
        }
    }

    public ServiceContractDetails getMaxServiceContractDetailIdWithContractId(Long id, List<String> errorMessages) {
        Long serviceContractDetailId = serviceContractDetailsRepository.findByContractIdAndVersionIdMax(id);
        if (serviceContractDetailId != null) {
            Optional<ServiceContractDetails> serviceContractDetailsOptional = serviceContractDetailsRepository.findByContractIdAndVersionId(id, serviceContractDetailId);
            return serviceContractDetailsOptional.orElse(null);
        } else {
            return null;
        }
    }

    public ServiceContractEditRequest toEditServiceContractRequest(Row row, List<String> errorMessages, String contractNumber, Long contractVersion) {
        ServiceContracts serviceContract = null;
        ServiceContractDetails serviceContractDetails = null;
        if (contractNumber != null && contractVersion != null) {
            Optional<ServiceContracts> serviceContractsOptional = serviceContractsRepository.findByContractNumberAndStatus(contractNumber, EntityStatus.ACTIVE);
            if (serviceContractsOptional.isPresent()) {
                serviceContract = serviceContractsOptional.get();
                Optional<ServiceContractDetails> serviceContractDetailsOptional =
                        serviceContractDetailsRepository.findByContractIdAndVersionId(serviceContract.getId(), contractVersion);
                if (serviceContractDetailsOptional.isPresent()) {
                    serviceContractDetails = serviceContractDetailsOptional.get();
                } else {
                    errorMessages.add("Can't find active serviceContract with number:%s and version:%s;".formatted(contractNumber, contractVersion));
                }
            } else {
                errorMessages.add("Can't find active serviceContract with number:%s;".formatted(contractNumber));
            }
        } else {
            errorMessages.add("contractNumber or contractVersion shouldn't be null;");
        }
        if (serviceContract != null && serviceContractDetails != null) {
            ServiceContractEditRequest request = getEditServiceContractRequestFromExcel(row, errorMessages);
            LocalDate contractStartDate = getDateValue(4, row);
            Boolean existsWithStartDate = checkContractVersionWithStartDate(serviceContract, contractStartDate, errorMessages);
            if (existsWithStartDate != null) {
                if (existsWithStartDate) {
                    errorMessages.add("Version with this Start Date already exists;");
                    return null;
                }
            }
            request.setSavingAsNewVersion(true);
            request.getBasicParameters().setContractStatusModifyDate(LocalDate.now()); //TODO should be added
            return fillInEditRequestFields(serviceContract, serviceContractDetails, request, errorMessages);
        } else {
            errorMessages.add("Can't find active serviceContract with number:%s and version:%s;".formatted(contractNumber, contractVersion));
        }
        return null;
    }

    public ServiceContractEditRequest toEditServiceContractRequestOnlyBasedOnExcel(Row row, List<String> errorMessages, String contractNumber, Long contractVersion) {
        ServiceContracts serviceContract = null;
        ServiceContractDetails serviceContractDetails = null;
        if (contractNumber != null && contractVersion != null) {
            Optional<ServiceContracts> serviceContractsOptional = serviceContractsRepository.findByContractNumberAndStatus(contractNumber, EntityStatus.ACTIVE);
            if (serviceContractsOptional.isPresent()) {
                serviceContract = serviceContractsOptional.get();
                Optional<ServiceContractDetails> serviceContractDetailsOptional =
                        serviceContractDetailsRepository.findByContractIdAndVersionId(serviceContract.getId(), contractVersion);
                if (serviceContractDetailsOptional.isPresent()) {
                    serviceContractDetails = serviceContractDetailsOptional.get();
                } else {
                    errorMessages.add("Can't find active serviceContract with number:%s and version:%s;".formatted(contractNumber, contractVersion));
                }
            } else {
                errorMessages.add("Can't find active serviceContract with number:%s;".formatted(contractNumber));
            }
        } else {
            errorMessages.add("contractNumber or contractVersion shouldn't be null;");
        }
        if (serviceContract != null && serviceContractDetails != null) {
            ServiceContractEditRequest request = getEditServiceContractRequestFromExcel(row, errorMessages);
            LocalDate contractStartDate = getDateValue(3, row);
            /*Boolean existsWithStartDate = checkContractVersionWithStartDate(serviceContract, contractStartDate, errorMessages);
            if (existsWithStartDate != null) {
                if (existsWithStartDate) {
                    errorMessages.add("Version with this Start Date already exists;");
                    return null;
                }
            }*/
            request.setSavingAsNewVersion(true);
            request.getBasicParameters().setContractStatusModifyDate(LocalDate.now()); //TODO should be added
            return request;
        } else {
            errorMessages.add("Can't find active serviceContract with number:%s and version:%s;".formatted(contractNumber, contractVersion));
        }
        return null;
    }

    private ServiceContractEditRequest fillInEditRequestFields(ServiceContracts serviceContract, ServiceContractDetails serviceContractDetails, ServiceContractEditRequest request, List<String> errorMessages) {
        ServiceContractEditRequest serviceContractEditRequest = new ServiceContractEditRequest();
        ServiceContractBasicParametersEditRequest finalBasicParameters = new ServiceContractBasicParametersEditRequest();
        ServiceContractAdditionalParametersRequest finalAdditionalParameters = new ServiceContractAdditionalParametersRequest();
        ServiceContractServiceParametersEditRequest finalServiceParameters = new ServiceContractServiceParametersEditRequest();

        ServiceContractBasicParametersEditRequest basicParameters = request.getBasicParameters();
        ServiceContractAdditionalParametersRequest additionalParameters = request.getAdditionalParameters();
        ServiceContractServiceParametersEditRequest serviceParameters = request.getServiceParameters();

        if (basicParameters.getServiceId() == null) {
            EPService service = getServiceByServiceDetails(serviceContractDetails.getServiceDetailId());
            finalBasicParameters.setServiceId(service.getId());
        } else {
            finalBasicParameters.setServiceId(basicParameters.getServiceId());
        }
        if (basicParameters.getServiceVersionId() == null) {
            finalBasicParameters.setServiceVersionId(serviceContractDetails.getServiceDetailId());
        } else {
            finalBasicParameters.setServiceVersionId(basicParameters.getServiceVersionId());
        }
        if (basicParameters.getContractStatus() == null) {
            finalBasicParameters.setContractStatus(serviceContract.getContractStatus());
        } else {
            finalBasicParameters.setContractStatus(basicParameters.getContractStatus());
        }
        //TODO CONTRACT STATUS MODIFY DATE IS NOT IN EXCEL
        finalBasicParameters.setContractStatusModifyDate(LocalDate.now());
        if (basicParameters.getContractType() == null) {
            finalBasicParameters.setContractType(basicParameters.getContractType());
        } else {
            finalBasicParameters.setContractType(serviceContractDetails.getType());
        }
        if (basicParameters.getDetailsSubStatus() == null) {
            finalBasicParameters.setDetailsSubStatus(serviceContract.getSubStatus());
        } else {
            finalBasicParameters.setDetailsSubStatus(basicParameters.getDetailsSubStatus());
        }
        if (basicParameters.getSignInDate() == null) {
            finalBasicParameters.setSignInDate(serviceContract.getSigningDate());
        } else {
            finalBasicParameters.setSignInDate(basicParameters.getSignInDate());
        }
        if (basicParameters.getEntryIntoForceDate() == null) {
            finalBasicParameters.setEntryIntoForceDate(serviceContract.getEntryIntoForceDate());
        }
        if (basicParameters.getContractTermUntilAmountIsReached() == null) {
            finalBasicParameters.setContractTermUntilAmountIsReached(serviceContractDetails.getContractTermUntilTheAmountValue());
        } else {
            finalBasicParameters.setContractTermUntilAmountIsReached(basicParameters.getContractTermUntilAmountIsReached());
        }
        if (basicParameters.getContractTermUntilAmountIsReachedCheckbox() == null) {
            finalBasicParameters.setContractTermUntilAmountIsReachedCheckbox(serviceContractDetails.getContractTermUntilTheAmount());
        } else {
            finalBasicParameters.setContractTermUntilAmountIsReachedCheckbox(basicParameters.getContractTermUntilAmountIsReachedCheckbox());
        }
        if (basicParameters.getCurrencyId() == null) {
            finalBasicParameters.setCurrencyId(serviceContractDetails.getCurrencyId());
        } else {
            finalBasicParameters.setCurrencyId(basicParameters.getCurrencyId());
        }
        if (basicParameters.getCustomerId() == null) {
            Customer customer = getCustomerWithDetailsId(serviceContractDetails.getCustomerDetailId(), errorMessages);
            if (customer != null) {
                finalBasicParameters.setCustomerId(customer.getId());
            }
        } else {
            finalBasicParameters.setCustomerId(basicParameters.getCustomerId());
        }
        if (basicParameters.getCustomerVersionId() == null) {
            finalBasicParameters.setCustomerVersionId(serviceContractDetails.getCustomerDetailId());
        } else {
            finalBasicParameters.setCustomerVersionId(basicParameters.getCustomerVersionId());
        }
        if (basicParameters.getCommunicationDataForBilling() == null) {
            finalBasicParameters.setCommunicationDataForBilling(serviceContractDetails.getCustomerCommunicationIdForBilling());
        } else {
            finalBasicParameters.setCommunicationDataForBilling(basicParameters.getCommunicationDataForBilling());
        }
        if (basicParameters.getCommunicationDataForContract() == null) {
            finalBasicParameters.setCommunicationDataForContract(serviceContractDetails.getCustomerCommunicationIdForContract());
        } else {
            finalBasicParameters.setCommunicationDataForContract(basicParameters.getCommunicationDataForContract());
        }
        if (basicParameters.getContractVersionStatus() == null) {
            finalBasicParameters.setContractVersionStatus(serviceContractDetails.getContractVersionStatus());
        } else {
            finalBasicParameters.setContractVersionStatus(basicParameters.getContractVersionStatus());
        }
        if (basicParameters.getContractVersionTypes() == null) {
            List<ServiceContractContractVersionTypes> contractVersionTypes =
                    serviceContractContractVersionTypesRepository.findByContractDetailIdAndStatusIn(serviceContractDetails.getId(), List.of(ContractSubObjectStatus.ACTIVE));
            finalBasicParameters.setContractVersionTypes(contractVersionTypes.stream().map(ServiceContractContractVersionTypes::getContractVersionTypeId).collect(Collectors.toList()));
        } else {
            finalBasicParameters.setContractVersionTypes(basicParameters.getContractVersionTypes());
        }
        if (basicParameters.getStartOfTheInitialTermOfTheContract() == null) {
            finalBasicParameters.setStartOfTheInitialTermOfTheContract(serviceContractDetails.getInitialTermStartValue());
        } else {
            finalBasicParameters.setStartOfTheInitialTermOfTheContract(basicParameters.getStartOfTheInitialTermOfTheContract());
        }
        if (basicParameters.getTerminationDate() == null) {
            finalBasicParameters.setTerminationDate(serviceContract.getTerminationDate());
        } else {
            finalBasicParameters.setTerminationDate(basicParameters.getTerminationDate());
        }
        if (basicParameters.getPerpetuityDate() == null) {
            finalBasicParameters.setPerpetuityDate(serviceContract.getPerpetuityDate());
        } else {
            finalBasicParameters.setPerpetuityDate(basicParameters.getPerpetuityDate());
        }
        if (basicParameters.getContractTermEndDate() == null) {
            finalBasicParameters.setContractTermEndDate(serviceContract.getContractTermEndDate());
        } else {
            finalBasicParameters.setContractTermEndDate(basicParameters.getContractTermEndDate());
        }
        if (basicParameters.getStartDate() == null) {
            finalBasicParameters.setStartDate(serviceContractDetails.getStartDate());
        } else {
            finalBasicParameters.setStartDate(basicParameters.getStartDate());
        }
        if (basicParameters.getProxy() == null) {
            finalBasicParameters.setProxy(getProxyFromDb(serviceContractDetails.getId()));
        } else {
            List<ProxyEditRequest> combinedProxies = Stream.of(basicParameters.getProxy(), getProxyFromDb(serviceContractDetails.getId()))
                    .flatMap(Collection::stream)
                    .toList();
            //basicParameters.getProxy().addAll(getProxyFromDb(serviceContractDetails.getId()));
            finalBasicParameters.setProxy(combinedProxies);
        }
        finalBasicParameters.setFiles(getFiles(serviceContractDetails));
        finalBasicParameters.setDocuments(getDocuments(serviceContractDetails));
        finalBasicParameters.setRelatedEntities(getDbRelatedEntities(serviceContractDetails));
        serviceContractEditRequest.setBasicParameters(finalBasicParameters);

        ServiceContractBankingDetails bankingDetails = additionalParameters.getBankingDetails();
        ServiceContractBankingDetails finalBankingDetails = new ServiceContractBankingDetails();
        if (bankingDetails.getDirectDebit() == null) {
            finalBankingDetails.setDirectDebit(serviceContractDetails.getDirectDebit());
        } else {
            finalBankingDetails.setDirectDebit(bankingDetails.getDirectDebit());
        }
        if (bankingDetails.getBankId() == null) {
            finalBankingDetails.setBankId(serviceContractDetails.getBankId());
        } else {
            finalBankingDetails.setBankId(bankingDetails.getBankId());
        }
        if (StringUtils.isEmpty(serviceContractDetails.getIban())) {
            finalBankingDetails.setIban(serviceContractDetails.getIban());
        } else {
            finalBankingDetails.setIban(bankingDetails.getIban());
        }
        finalAdditionalParameters.setBankingDetails(finalBankingDetails);
        if (additionalParameters.getInterestRateId() == null) {
            finalAdditionalParameters.setInterestRateId(serviceContractDetails.getApplicableInterestRate());
        } else {
            finalAdditionalParameters.setInterestRateId(additionalParameters.getInterestRateId());
        }
        if (additionalParameters.getCampaignId() == null) {
            finalAdditionalParameters.setCampaignId(serviceContractDetails.getCampaignId());
        } else {
            finalAdditionalParameters.setCampaignId(additionalParameters.getCampaignId());
        }
        if (additionalParameters.getAssistingEmployees() == null) {
            finalAdditionalParameters.setAssistingEmployees(getDbAssistantEmployees(serviceContractDetails));
        } else {
            List<Long> dbAssistingEmployees = getDbAssistantEmployees(serviceContractDetails);
            if (!CollectionUtils.isEmpty(dbAssistingEmployees)) {
                /*finalAdditionalParameters.setAssistingEmployees(
                        Stream.of(additionalParameters.getAssistingEmployees(), dbAssistingEmployees).flatMap(Collection::stream)
                                .toList());*/
                finalAdditionalParameters.setAssistingEmployees(
                        Stream.concat(
                                        additionalParameters.getAssistingEmployees().stream(),
                                        dbAssistingEmployees.stream()
                                )
                                .distinct() // Remove duplicates
                                .collect(Collectors.toList())
                );
            } else {
                finalAdditionalParameters.setAssistingEmployees(additionalParameters.getAssistingEmployees());
            }
        }
        if (additionalParameters.getInternalIntermediaries() == null) {
            finalAdditionalParameters.setInternalIntermediaries(getInternalIntermediaries(serviceContractDetails));
        } else {
            List<Long> dbInternalIntermediaries = getInternalIntermediaries(serviceContractDetails);
            if (!CollectionUtils.isEmpty(dbInternalIntermediaries)) {
                /*finalAdditionalParameters.setInternalIntermediaries(
                        Stream.of(additionalParameters.getInternalIntermediaries(), dbInternalIntermediaries).flatMap(Collection::stream).toList());*/
                finalAdditionalParameters.setInternalIntermediaries(
                        Stream.concat(
                                        additionalParameters.getInternalIntermediaries().stream(),
                                        dbInternalIntermediaries.stream()
                                )
                                .distinct() // Remove duplicates
                                .collect(Collectors.toList())
                );
            } else {
                finalAdditionalParameters.setInternalIntermediaries(additionalParameters.getInternalIntermediaries());
            }
        }
        if (additionalParameters.getExternalIntermediaries() == null) {
            finalAdditionalParameters.setExternalIntermediaries(getExternalIntermediaries(serviceContractDetails));
        } else {
            List<Long> dbExternalIntermediaries = getExternalIntermediaries(serviceContractDetails);
            if (!CollectionUtils.isEmpty(dbExternalIntermediaries)) {
               /* finalAdditionalParameters.setExternalIntermediaries(
                        Stream.of(additionalParameters.getExternalIntermediaries(), dbExternalIntermediaries).flatMap(Collection::stream).toList());*/
                finalAdditionalParameters.setExternalIntermediaries(
                        Stream.concat(
                                        additionalParameters.getExternalIntermediaries().stream(),
                                        dbExternalIntermediaries.stream()
                                )
                                .distinct() // Remove duplicates
                                .collect(Collectors.toList())
                );
            } else {
                finalAdditionalParameters.setExternalIntermediaries(additionalParameters.getExternalIntermediaries());
            }
        }
        if (additionalParameters.getEmployeeId() == null) {
            finalAdditionalParameters.setEmployeeId(serviceContractDetails.getEmployeeId());
        } else {
            finalAdditionalParameters.setEmployeeId(additionalParameters.getEmployeeId());
        }
        serviceContractEditRequest.setAdditionalParameters(finalAdditionalParameters);


        if (serviceParameters.getContractNumbers() == null) {
            finalServiceParameters.setContractNumbersEditList(getDbContractNumbers(serviceContractDetails));
        } else {
            List<ServiceContractContractNumbersEditRequest> dbContractNumbers = getDbContractNumbers(serviceContractDetails);
            List<ServiceContractContractNumbersEditRequest> contractNumbers = new ArrayList<>();
            if (!CollectionUtils.isEmpty(dbContractNumbers)) {
                for (String number : finalServiceParameters.getContractNumbers()) {
                    ServiceContractContractNumbersEditRequest contractContractNumbersEditRequest = new ServiceContractContractNumbersEditRequest();
                    contractContractNumbersEditRequest.setContractNumber(number);
                    contractNumbers.add(contractContractNumbersEditRequest);
                }
                finalServiceParameters.setContractNumbersEditList(Stream.of(contractNumbers, dbContractNumbers).flatMap(Collection::stream).toList());
            } else {
                List<ServiceContractContractNumbersEditRequest> excelContractNumbers = new ArrayList<>();
                List<String> finalContractNumbers = finalServiceParameters.getContractNumbers();
                if (!CollectionUtils.isEmpty(finalContractNumbers)) {
                    for (String number : finalContractNumbers) {
                        ServiceContractContractNumbersEditRequest contractContractNumbersEditRequest = new ServiceContractContractNumbersEditRequest();
                        contractContractNumbersEditRequest.setContractNumber(number);
                        excelContractNumbers.add(contractContractNumbersEditRequest);
                    }
                    finalServiceParameters.setContractNumbersEditList(excelContractNumbers);
                }
            }
        }
        if (serviceParameters.getPodIds() == null) {
            finalServiceParameters.setPodsEditList(getDbPodIds(serviceContractDetails));
        } else {
            List<ServiceContractPodsEditRequest> dbPods = getDbPodIds(serviceContractDetails);
            List<ServiceContractPodsEditRequest> newPods = new ArrayList<>();
            if (!CollectionUtils.isEmpty(dbPods)) {
                for (Long id : serviceParameters.getPodIds()) {
                    ServiceContractPodsEditRequest editRequest = new ServiceContractPodsEditRequest();
                    editRequest.setPodId(id);
                    newPods.add(editRequest);
                }
                finalServiceParameters.setPodsEditList(Stream.of(dbPods, newPods).flatMap(Collection::stream).collect(Collectors.toList()));
            } else {
                for (Long id : serviceParameters.getPodIds()) {
                    ServiceContractPodsEditRequest editRequest = new ServiceContractPodsEditRequest();
                    editRequest.setPodId(id);
                    newPods.add(editRequest);
                }
                finalServiceParameters.setPodsEditList(newPods);
            }
        }
        if (serviceParameters.getUnrecognizedPods() == null) {
            finalServiceParameters.setUnrecognizedPodsEditList(getDbUnrecognizedPods(serviceContract));
        } else {
            List<ServiceContractUnrecognizedPodsEditRequest> dbPods = getDbUnrecognizedPods(serviceContract);
            List<ServiceContractUnrecognizedPodsEditRequest> newPods = new ArrayList<>();
            for (String name : serviceParameters.getUnrecognizedPods()) {
                ServiceContractUnrecognizedPodsEditRequest serviceContractUnrecognizedPodsEditRequest = new ServiceContractUnrecognizedPodsEditRequest();
                serviceContractUnrecognizedPodsEditRequest.setPodName(name);
                newPods.add(serviceContractUnrecognizedPodsEditRequest);
            }
            if (!CollectionUtils.isEmpty(dbPods)) {
                finalServiceParameters.setUnrecognizedPodsEditList(Stream.of(dbPods, newPods).flatMap(Collection::stream).toList());
            } else {
                finalServiceParameters.setUnrecognizedPodsEditList(newPods);
            }
        }

        if (serviceParameters.getContractTermId() == null) {
            finalServiceParameters.setContractTermId(serviceContractDetails.getServiceContractTermId());
        } else {
            finalServiceParameters.setContractTermId(serviceParameters.getContractTermId());
        }

        if (serviceParameters.getContractTermEndDate() == null) {
            finalServiceParameters.setContractTermEndDate(serviceContractDetails.getContractTermEndDate());
        } else {
            finalServiceParameters.setContractTermEndDate(serviceParameters.getContractTermEndDate());
        }

        if (serviceParameters.getEntryIntoForce() == null) {
            finalServiceParameters.setEntryIntoForce(serviceContractDetails.getEntryIntoForce());
        } else {
            finalServiceParameters.setEntryIntoForce(serviceParameters.getEntryIntoForce());
        }

        if (serviceParameters.getEntryIntoForceDate() == null) {
            finalServiceParameters.setEntryIntoForceDate(serviceContractDetails.getEntryIntoForceValue());
        } else {
            finalServiceParameters.setEntryIntoForceDate(serviceParameters.getEntryIntoForceDate());
        }

        if (serviceParameters.getStartOfContractInitialTerm() == null) {
            // finalServiceParameters.setStartOfContractInitialTerm(serviceContract.getStartOfContractInitialTerm()); //TODO NEED IN DATABASE
        } else {
            finalServiceParameters.setStartOfContractInitialTerm(serviceParameters.getStartOfContractInitialTerm());
        }

        if (serviceParameters.getStartOfContractInitialTermDate() == null) {
            finalServiceParameters.setStartOfContractInitialTermDate(serviceContract.getContractInitialTermStartDate());
        } else {
            finalServiceParameters.setStartOfContractInitialTermDate(serviceParameters.getStartOfContractInitialTermDate());
        }

        if (serviceParameters.getInvoicePaymentTermId() == null) {
            finalServiceParameters.setInvoicePaymentTermId(serviceContractDetails.getInvoicePaymentTermId());
        } else {
            finalServiceParameters.setInvoicePaymentTermId(serviceParameters.getInvoicePaymentTermId());
        }

        if (serviceParameters.getInvoicePaymentTerm() == null) {
            finalServiceParameters.setInvoicePaymentTerm(serviceContractDetails.getInvoicePaymentTermValue());
        } else {
            finalServiceParameters.setInvoicePaymentTerm(serviceParameters.getInvoicePaymentTerm());
        }

        if (serviceParameters.getPaymentGuarantee() == null) {
            finalServiceParameters.setPaymentGuarantee(serviceContractDetails.getPaymentGuarantee());
        } else {
            finalServiceParameters.setPaymentGuarantee(serviceParameters.getPaymentGuarantee());
        }

        if (serviceParameters.getCashDepositAmount() == null) {
            finalServiceParameters.setCashDepositAmount(serviceContractDetails.getCashDepositAmount());
        } else {
            finalServiceParameters.setCashDepositAmount(serviceParameters.getCashDepositAmount());
        }

        if (serviceParameters.getCashDepositCurrencyId() == null) {
            finalServiceParameters.setCashDepositCurrencyId(serviceContractDetails.getCashDepositCurrency());
        } else {
            finalServiceParameters.setCashDepositCurrencyId(serviceParameters.getCashDepositCurrencyId());
        }

        if (serviceParameters.getBankGuaranteeAmount() == null) {
            finalServiceParameters.setBankGuaranteeAmount(serviceContractDetails.getBankGuaranteeAmount());
        } else {
            finalServiceParameters.setBankGuaranteeAmount(serviceParameters.getBankGuaranteeAmount());
        }

        if (serviceParameters.getBankGuaranteeCurrencyId() == null) {
            finalServiceParameters.setBankGuaranteeCurrencyId(serviceContractDetails.getBankGuaranteeCurrencyId());
        } else {
            finalServiceParameters.setBankGuaranteeCurrencyId(serviceParameters.getBankGuaranteeCurrencyId());
        }

       /* if (serviceParameters.isGuaranteeContract() == null) {
            finalServiceParameters.setGuaranteeContract(serviceContractDetails.getGuaranteeContract());
        } else {
            finalServiceParameters.setGuaranteeContract(serviceParameters.isGuaranteeContract());
        }*/
        finalServiceParameters.setGuaranteeContract(serviceParameters.isGuaranteeContract());

        if (serviceParameters.getGuaranteeContractInfo() == null) {
            finalServiceParameters.setGuaranteeContractInfo(serviceContractDetails.getGuaranteeContractInfo());
        } else {
            finalServiceParameters.setGuaranteeContractInfo(serviceParameters.getGuaranteeContractInfo());
        }
        finalServiceParameters.setContractFormulas(getContractFormulas(serviceContractDetails));
        if (serviceParameters.getQuantity() == null) {
            finalServiceParameters.setQuantity(BigDecimal.valueOf(serviceContractDetails.getQuantity()));
        } else {
            finalServiceParameters.setQuantity(serviceParameters.getQuantity());
        }
        finalServiceParameters.setInterimAdvancePaymentsRequests(getInterimAdvancePayments(serviceContractDetails));

        if (serviceParameters.getMonthlyInstallmentNumber() == null) {
            finalServiceParameters.setMonthlyInstallmentNumber(serviceContractDetails.getEqualMonthlyInstallmentNumber());
        } else {
            finalServiceParameters.setMonthlyInstallmentNumber(serviceParameters.getMonthlyInstallmentNumber());
        }

        if (serviceParameters.getMonthlyInstallmentAmount() == null) {
            finalServiceParameters.setMonthlyInstallmentAmount(serviceContractDetails.getEqualMonthlyInstallmentAmount());
        } else {
            finalServiceParameters.setMonthlyInstallmentAmount(serviceParameters.getMonthlyInstallmentAmount());
        }
        serviceContractEditRequest.setServiceParameters(finalServiceParameters);
        return serviceContractEditRequest;
    }

    private List<ServiceContractInterimAdvancePaymentsRequest> getInterimAdvancePayments(ServiceContractDetails serviceContractDetails) {
        List<ServiceContractInterimAdvancePaymentsRequest> returnList = new ArrayList<>();
        Map<Long, ServiceContractInterimAdvancePayments> collect = contractInterimAdvancePaymentsRepository.findAllByContractDetailIdAndStatusIn(serviceContractDetails.getId(), List.of(ContractSubObjectStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ServiceContractInterimAdvancePayments::getInterimAdvancePaymentId, x -> x));
        List<InterimAdvancePayment> iaps = advancePaymentRepository.findAllByIdIn(collect.keySet());
        List<ServiceContractIAPResponse> interimAdvancePayments = new ArrayList<>();
        for (InterimAdvancePayment iap : iaps) {
            ServiceContractInterimAdvancePayments contractInterimAdvancePayments = collect.get(iap.getId());
            ServiceContractInterimAdvancePaymentsRequest serviceContractInterimAdvancePaymentsRequest = new ServiceContractInterimAdvancePaymentsRequest();
            serviceContractInterimAdvancePaymentsRequest.setIssueDate(contractInterimAdvancePayments.getIssueDate());
            serviceContractInterimAdvancePaymentsRequest.setValue(contractInterimAdvancePayments.getValue());
            serviceContractInterimAdvancePaymentsRequest.setInterimAdvancePaymentId(contractInterimAdvancePayments.getInterimAdvancePaymentId());
            returnList.add(serviceContractInterimAdvancePaymentsRequest);
        }
        return returnList;
    }

    private List<PriceComponentContractFormula> getContractFormulas(ServiceContractDetails serviceContractDetails) {
        List<PriceComponentContractFormula> returnList = new ArrayList<>();
        Map<Long, ServiceContractPriceComponents> collect = serviceContractPriceComponentsRepository.findByContractDetailIdAndStatusIn(serviceContractDetails.getId(), List.of(ContractSubObjectStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ServiceContractPriceComponents::getPriceComponentFormulaVariableId, x -> x));
        List<ContractPriceComponentResponse> priceComponentResponses = new ArrayList<>();
        List<PriceComponentFormulaVariable> priceComponentFormulas = priceComponentFormulaVariableRepository.findAllByIdIn(collect.keySet());
        for (PriceComponentFormulaVariable priceComponentFormula : priceComponentFormulas) {
           /* ServiceContractPriceComponents contractPriceComponents = collect.get(priceComponentFormula.getId());
            ContractPriceComponentResponse response = new ContractPriceComponentResponse();
            response.setValue(contractPriceComponents.getValue());
            response.setFormulaVariableId(priceComponentFormula.getId());
            response.setVariableDescription(priceComponentFormula.getDescription());
            priceComponentResponses.add(response);*/
            PriceComponentContractFormula priceComponentContractFormula = new PriceComponentContractFormula();
            priceComponentContractFormula.setFormulaVariableId(priceComponentFormula.getId());
            priceComponentContractFormula.setValue(priceComponentFormula.getValue());
            returnList.add(priceComponentContractFormula);
        }
        return returnList;
    }

    private List<ServiceContractUnrecognizedPodsEditRequest> getDbUnrecognizedPods(ServiceContracts serviceContracts) {
        List<ServiceContractUnrecognizedPodsEditRequest> returnList = new ArrayList<>();
        List<ServiceUnrecognizedPods> unrecognizedPods = serviceUnrecognizedPodsRepository.findByContractDetailsId(serviceContracts.getId());
        if (!CollectionUtils.isEmpty(unrecognizedPods)) {
            for (ServiceUnrecognizedPods item : unrecognizedPods) {
                ServiceContractUnrecognizedPodsEditRequest serviceContractUnrecognizedPodsEditRequest = new ServiceContractUnrecognizedPodsEditRequest();
                serviceContractUnrecognizedPodsEditRequest.setId(item.getId());
                serviceContractUnrecognizedPodsEditRequest.setPodName(item.getPodIdentifier());
                returnList.add(serviceContractUnrecognizedPodsEditRequest);
            }
            return returnList;
        } else return null;
    }

    private List<ServiceContractPodsEditRequest> getDbPodIds(ServiceContractDetails serviceContractDetails) {
        List<ServiceContractPodsEditRequest> returnList = new ArrayList<>();
        List<ServiceContractPods> serviceContractPods = serviceContractPodsRepository.findByContractDetailIdAndStatus(serviceContractDetails.getId(), ContractSubObjectStatus.ACTIVE);
        if (!CollectionUtils.isEmpty(serviceContractPods)) {
            for (ServiceContractPods item : serviceContractPods) {
                ServiceContractPodsEditRequest serviceContractPodsEditRequest = new ServiceContractPodsEditRequest();
                serviceContractPodsEditRequest.setId(item.getId());
                serviceContractPodsEditRequest.setPodId(item.getPodId());
                returnList.add(serviceContractPodsEditRequest);
            }
            return returnList;
        } else return null;
    }

    private List<ServiceContractContractNumbersEditRequest> getDbContractNumbers(ServiceContractDetails serviceContractDetails) {
        List<ServiceContractContractNumbersEditRequest> returnList = new ArrayList<>();
        List<SubObjectContractResponse> contractResponseList = new ArrayList<>();
        List<ContractLinkedProductContract> dbProductLinkedContracts =
                contractLinkedProductContractRepository.findAllByContractIdAndStatus(serviceContractDetails.getId(), ContractSubObjectStatus.ACTIVE);
        List<ContractLinkedServiceContract> dbServiceLinkedContracts =
                contractLinkedServiceContractRepository.findAllByContractIdAndStatus(serviceContractDetails.getId(), ContractSubObjectStatus.ACTIVE);
        if (!CollectionUtils.isEmpty(dbProductLinkedContracts)) {
            for (ContractLinkedProductContract item : dbProductLinkedContracts) {
                SubObjectContractResponse subObjectContractResponse = new SubObjectContractResponse();
                subObjectContractResponse.setId(item.getId());
                subObjectContractResponse.setContractNumber(getProductContractWithId(item.getLinkedProductContractId()));
                contractResponseList.add(subObjectContractResponse);
            }
        }
        if (!CollectionUtils.isEmpty(dbServiceLinkedContracts)) {
            for (ContractLinkedServiceContract item : dbServiceLinkedContracts) {
                SubObjectContractResponse subObjectContractResponse = new SubObjectContractResponse();
                subObjectContractResponse.setId(item.getId());
                subObjectContractResponse.setContractNumber(getServiceContractWithId(item.getLinkedServiceContractId()));
                contractResponseList.add(subObjectContractResponse);
            }
        }

        if (!CollectionUtils.isEmpty(contractResponseList)) {
            for (SubObjectContractResponse item : contractResponseList) {
                ServiceContractContractNumbersEditRequest serviceContractContractNumbersEditRequest = new ServiceContractContractNumbersEditRequest();
                serviceContractContractNumbersEditRequest.setContractNumber(item.getContractNumber());
                returnList.add(serviceContractContractNumbersEditRequest);
            }
            return returnList;
        } else return null;
    }

    private String getProductContractWithId(Long linkedProductContractId) {
        Optional<ProductContract> productContractOptional = productContractRepository.findByIdAndStatusIn(linkedProductContractId, List.of(ProductContractStatus.ACTIVE));
        if (productContractOptional.isPresent()) {
            return productContractOptional.get().getContractNumber();
        } else {
            throw new DomainEntityNotFoundException("Can't find active productContract with linked product contract id:%s;".formatted(linkedProductContractId));
        }
    }

    private String getServiceContractWithId(Long linkedServiceContractId) {
        Optional<ServiceContracts> serviceContractOptional = serviceContractRepository.findByIdAndStatusIn(linkedServiceContractId, List.of(EntityStatus.ACTIVE));
        if (serviceContractOptional.isPresent()) {
            return serviceContractOptional.get().getContractNumber();
        } else {
            throw new DomainEntityNotFoundException("Can't find active serviceContract with linked product contract id:%s;".formatted(linkedServiceContractId));
        }
    }

    private List<Long> getInternalIntermediaries(ServiceContractDetails serviceContractDetails) {
        List<ServiceContractSubObjectShortResponse> dbInternalIntermediaries = serviceContractInternalIntermediaryRepository
                .getShortResponseByContractDetailIdAndStatusInWithInternalIntermediaryId(
                        serviceContractDetails.getId(),
                        List.of(EntityStatus.ACTIVE)
                );
        if (!CollectionUtils.isEmpty(dbInternalIntermediaries)) {
            return dbInternalIntermediaries.stream().map(ServiceContractSubObjectShortResponse::getId).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    private List<Long> getExternalIntermediaries(ServiceContractDetails serviceContractDetails) {
        List<ServiceContractSubObjectShortResponse> dbExternalIntermediaries = serviceContractExternalIntermediaryRepository
                .getShortResponseByContractDetailIdAndStatusInWithExternalIntermediaryId(
                        serviceContractDetails.getId(),
                        List.of(EntityStatus.ACTIVE)
                );
        if (!CollectionUtils.isEmpty(dbExternalIntermediaries)) {
            return dbExternalIntermediaries.stream().map(ServiceContractSubObjectShortResponse::getId).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    private List<Long> getDbAssistantEmployees(ServiceContractDetails serviceContractDetails) {
        List<ServiceContractSubObjectShortResponse> assistingEmployees = serviceContractAssistingEmployeeRepository
                .getShortResponseByContractDetailIdAndStatusInWithAndAssistingEmployeeId(
                        serviceContractDetails.getId(),
                        List.of(EntityStatus.ACTIVE));
        if (!CollectionUtils.isEmpty(assistingEmployees)) {
            return assistingEmployees.stream().map(ServiceContractSubObjectShortResponse::getId).collect(Collectors.toList());
        } else
            return null;
    }

    private List<RelatedEntityRequest> getDbRelatedEntities(ServiceContractDetails serviceContractDetails) {
        List<RelatedEntityRequest> relatedEntityRequests = new ArrayList<>();
        List<RelatedEntityResponse> relatedEntities =
                relatedContractsAndOrdersService.getRelatedEntities(serviceContractDetails.getId(), RelatedEntityType.SERVICE_CONTRACT);
        if (!CollectionUtils.isEmpty(relatedEntities)) {
            for (RelatedEntityResponse item : relatedEntities) {
                RelatedEntityRequest request = new RelatedEntityRequest();
                request.setId(item.getId());
                request.setEntityId(item.getEntityId());
                request.setEntityType(item.getEntityType());
                request.setRelatedEntityId(item.getRelatedEntityId());
                request.setRelatedEntityType(item.getRelatedEntityType());
                relatedEntityRequests.add(request);
            }
        }
        if (CollectionUtils.isEmpty(relatedEntityRequests)) {
            return null;
        }
        return relatedEntityRequests;
    }

    private LinkedHashSet<Long> getDocuments(ServiceContractDetails serviceContractDetails) {
        List<ServiceContractAdditionalDocuments> contractFiles =
                serviceContractAdditionalDocumentsRepository.findServiceContractFilesByContractDetailIdAndStatusIn(serviceContractDetails.getId(), List.of(EntityStatus.ACTIVE));
        if (!CollectionUtils.isEmpty(contractFiles)) {
            return contractFiles.stream().map(ServiceContractAdditionalDocuments::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        } else return null;
    }

    private LinkedHashSet<Long> getFiles(ServiceContractDetails serviceContractDetails) {
        List<ServiceContractFiles> contractFiles =
                serviceContractFilesRepository.findServiceContractFilesByContractDetailIdAndStatusIn(serviceContractDetails.getId(), List.of(EntityStatus.ACTIVE));
        if (!CollectionUtils.isEmpty(contractFiles)) {
            return contractFiles.stream().map(ServiceContractFiles::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        } else return null;
    }

    private List<ProxyEditRequest> getProxyFromDb(Long id) {
        List<ServiceContractProxy> dbProxies = serviceContractProxyRepository.findByContractDetailIdAndStatusIn(id, List.of(ContractSubObjectStatus.ACTIVE));
        List<ProxyEditRequest> proxyEditRequestsList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dbProxies)) {
            for (ServiceContractProxy item : dbProxies) {
                proxyEditRequestsList.add(getProxyEditRequest(item));
            }
        }
        return proxyEditRequestsList;
    }

    private ProxyEditRequest getProxyEditRequest(ServiceContractProxy item) {
        ProxyEditRequest proxyEditRequest = new ProxyEditRequest();
        proxyEditRequest.setId(item.getId());
        proxyEditRequest.setProxyName(item.getProxyName());
        proxyEditRequest.setProxyForeignEntityPerson(item.getProxyForeignEntityPerson());
        proxyEditRequest.setProxyCustomerIdentifier(item.getProxyPersonalIdentifier());
        proxyEditRequest.setProxyEmail(item.getProxyEmail());
        proxyEditRequest.setProxyPhone(item.getProxyMobilePhone());
        proxyEditRequest.setProxyPowerOfAttorneyNumber(item.getProxyAttorneyPowerNumber());
        proxyEditRequest.setProxyData(item.getProxyDate());
        proxyEditRequest.setProxyValidTill(item.getProxyValidTill());
        proxyEditRequest.setNotaryPublic(item.getProxyNotaryPublic());
        proxyEditRequest.setRegistrationNumber(item.getProxyRegistrationNumber());
        proxyEditRequest.setAreaOfOperation(item.getProxyOperationArea());
        proxyEditRequest.setProxyForeignEntityPerson(item.getProxyForeignEntityPerson());
        proxyEditRequest.setAuthorizedProxyForeignEntityPerson(item.getProxyByProxyForeignEntityPerson());
        proxyEditRequest.setProxyAuthorizedByProxy(item.getProxyByProxyName());
        proxyEditRequest.setAuthorizedProxyEmail(item.getProxyByProxyEmail());
        proxyEditRequest.setAuthorizedProxyPhone(item.getProxyByProxyMobilePhone());
        proxyEditRequest.setAuthorizedProxyValidTill(item.getProxyByProxyValidTill());
        proxyEditRequest.setAuthorizedProxyNotaryPublic(item.getProxyByProxyNotaryPublic());
        proxyEditRequest.setAuthorizedProxyRegistrationNumber(item.getProxyByProxyRegistrationNumber());
        proxyEditRequest.setAuthorizedProxyAreaOfOperation(item.getProxyByProxyOperationArea());
        proxyEditRequest.setAuthorizedProxyPowerOfAttorneyNumber(item.getProxyByProxyAttorneyPowerNumber());
        proxyEditRequest.setAuthorizedProxyCustomerIdentifier(item.getProxyByProxyPersonalIdentifier());
        proxyEditRequest.setAuthorizedProxyData(item.getProxyByProxyDate());
        return proxyEditRequest;
    }

    public EPService getServiceByServiceDetails(Long detailsId) {
        Optional<ServiceDetails> serviceDetails = serviceDetailsRepository.findByIdAndStatus(detailsId, ServiceDetailStatus.ACTIVE);
        return serviceDetails.map(ServiceDetails::getService).orElse(null);
    }

    public ServiceContractEditRequest getEditServiceContractRequestFromExcel(Row row, List<String> errorMessages) {

        ServiceContractEditRequest request = new ServiceContractEditRequest();
        ServiceContractBasicParametersEditRequest basicParameters = new ServiceContractBasicParametersEditRequest();
        ServiceContractAdditionalParametersRequest additionalParameters = new ServiceContractAdditionalParametersRequest();
        ServiceContractServiceParametersEditRequest serviceParameters = new ServiceContractServiceParametersEditRequest();

        String contractNumber = getStringValue(0, row);
        Long contractVersion = getLongValue(1, row);
        String contractCreateEdit = getStringValue(2, row); //C or E
        LocalDate contractStartDate = getDateValue(3, row);
        //basicParameters.setContractStatusModifyDate(getDateValue(0, row)); //statusModifyDate
        Set<Long> managerIds = new HashSet<>();
        Cell detailsSubStatus = row.getCell(9);
        if (detailsSubStatus != null) {
            basicParameters.setDetailsSubStatus(ServiceContractDetailsSubStatus.valueOf(detailsSubStatus.getStringCellValue()));
        }
        BigDecimal contracTermUntilAmountIsReachedValue = getDecimalValue(19, row);
        basicParameters.setContractTermUntilAmountIsReached(contracTermUntilAmountIsReachedValue);
        basicParameters.setContractTermUntilAmountIsReachedCheckbox(contracTermUntilAmountIsReachedValue != null);
       /* Long CurrencyId = getCurrencyId(getStringValue(54, row), errorMessages);
        basicParameters.setCurrencyId(CurrencyId);*/
        String customerIdentifier = getStringValue(4, row);
        Customer customer = getCustomer(customerIdentifier, errorMessages);
        if (customer != null) {
            basicParameters.setCustomerId(customer.getId());
            Long customerVersionId = getLongValue(5, row);
            CustomerDetails customerDetails = checkCustomerVersion(customer, customerVersionId, errorMessages);
            if (customerDetails != null) {
                basicParameters.setCustomerVersionId(customerVersionId);
                basicParameters.setCommunicationDataForBilling(getLatestCommunications(customerDetails.getId(), errorMessages, true));
                basicParameters.setCommunicationDataForContract(getLatestCommunications(customerDetails.getId(), errorMessages, false));
                managerIds = fillManagerForProxy(customerDetails.getId());
            }
        }
        Long serviceId = getLongValue(6, row);
        Long serviceDetailsId = getLongValue(7, row);
        if (serviceId != null) {
            EPService service = getEpService(serviceId, errorMessages);
            if (service != null) {
                basicParameters.setServiceId(service.getId());
                if (serviceDetailsId != null) {
                    ServiceDetails serviceDetails = getServiceDetails(service, serviceDetailsId, errorMessages);
                    if (serviceDetails != null) {
                        basicParameters.setServiceVersionId(serviceDetailsId);
                    }
                }
            }
        }
        String contractStatus = getStringValue(8, row);
        if (contractStatus != null) {
            basicParameters.setContractStatus(ServiceContractDetailStatus.valueOf(contractStatus));
        }
        String contractSubStatus = getStringValue(9, row);
        if (contractSubStatus != null) {
            basicParameters.setDetailsSubStatus(ServiceContractDetailsSubStatus.valueOf(contractSubStatus));
        }
        String contractType = getStringValue(10, row);
        if (contractType != null) {
            basicParameters.setContractType(ServiceContractContractType.valueOf(contractType));
        }
        String contractVersionStatus = getStringValue(11, row);
        if (contractVersionStatus != null) {
            basicParameters.setContractVersionStatus(ContractVersionStatus.valueOf(contractVersionStatus));
        }
        String contractVersionTypes = getStringValue(12, row);
        if (contractVersionTypes != null) {
            List<Long> contractVersionTypeIds = getContractVersionTypes(contractVersionTypes, errorMessages);
            if (!CollectionUtils.isEmpty(contractVersionTypeIds)) {
                basicParameters.setContractVersionTypes(contractVersionTypeIds);
            } else {
                errorMessages.add("ContractVersionTypes is empty;");
            }
        }


        basicParameters.setSignInDate(getDateValue(13, row)); //signInDate

        basicParameters.setEntryIntoForceDate(getDateValue(14, row)); //entryIntoForceDate

        basicParameters.setStartOfTheInitialTermOfTheContract(getDateValue(15, row));

        basicParameters.setContractTermEndDate(getDateValue(16, row));

        basicParameters.setTerminationDate(getDateValue(17, row));
        basicParameters.setPerpetuityDate(getDateValue(18, row));
        BigDecimal unitlAmountIsReached = getDecimalValue(19, row);
        if (unitlAmountIsReached != null) {
            basicParameters.setContractTermUntilAmountIsReached(unitlAmountIsReached);
            basicParameters.setContractTermUntilAmountIsReachedCheckbox(true);
            basicParameters.setCurrencyId(getCurrencyId(getStringValue(20, row), errorMessages));
        }
        basicParameters.setStartDate(contractStartDate);
        basicParameters.setProxy(getProxyEditParameters(row, errorMessages, managerIds));

        List<RelatedEntityRequest> relatedEntityRequestList = getRelatedEntities(row, errorMessages);
        if (!CollectionUtils.isEmpty(relatedEntityRequestList)) {
            basicParameters.setRelatedEntities(relatedEntityRequestList);
        }
        request.setBasicParameters(basicParameters);

        additionalParameters.setBankingDetails(getBankingDetails(row, errorMessages));

        additionalParameters.setInterestRateId(getInterestRateId(row, errorMessages));
        additionalParameters.setEmployeeId(getEmployeeId(row, errorMessages));
        additionalParameters.setAssistingEmployees(getAssistingEmployee(getStringValue(49, row), errorMessages));
        additionalParameters.setInternalIntermediaries(getAssistingEmployee(getStringValue(50, row), errorMessages));
        //additionalParameters.setExternalIntermediaries(getAssistingEmployee(getStringValue(50, row), errorMessages));
        additionalParameters.setExternalIntermediaries(parseExternalIntermediaries(getStringValue(51, row), errorMessages));
        request.setAdditionalParameters(additionalParameters);
        serviceParameters.setPaymentGuarantee(getPaymentGuarantee(row, errorMessages));
        serviceParameters.setCashDepositAmount(getCashDepositAmount(row, errorMessages));
        serviceParameters.setCashDepositCurrencyId(getCurrencyId(getStringValue(55, row), errorMessages));
        serviceParameters.setBankGuaranteeAmount(getBankGuaranteeAmount(row, errorMessages));
        serviceParameters.setBankGuaranteeCurrencyId(getCurrencyId(getStringValue(57, row), errorMessages));
        serviceParameters.setEntryIntoForce(getEntryIntoForce(row, errorMessages));
        serviceParameters.setEntryIntoForceDate(getDateValue(59, row));
        serviceParameters.setStartOfContractInitialTerm(getStartOfTerm(row, errorMessages));
        serviceParameters.setStartOfContractInitialTermDate(getDateValue(61, row));
        serviceParameters.setMonthlyInstallmentAmount(getDecimalValue(62, row));
        serviceParameters.setMonthlyInstallmentNumber(getShortValue(63, row));
        serviceParameters.setPodIds(getPods(row, errorMessages));
        serviceParameters.setUnrecognizedPods(getUnrecognizedPods(row, errorMessages));
        serviceParameters.setContractNumbers(getContractNumbers(row, errorMessages));
        serviceParameters.setUnrecognizedPodsEditList(getUnrecognizedPodsEditList(serviceParameters.getUnrecognizedPods()));
        serviceParameters.setPodsEditList(getPodsForEditList(serviceParameters.getPodIds()));
        serviceParameters.setContractNumbersEditList(getContractNumbersEditList(serviceParameters.getContractNumbers()));
        serviceParameters.setQuantity(getDecimalValue(67, row));
        request.setServiceParameters(serviceParameters);
        serviceParameters.setContractTermEndDate(getDateValue(16, row));
        serviceParameters.setGuaranteeContract(false);
        checkAndFillServiceParametersForEdit(request.getServiceParameters(), serviceId, serviceDetailsId, errorMessages);
        return request;

    }

    private List<ServiceContractContractNumbersEditRequest> getContractNumbersEditList(List<String> contractNumbers) {
        if (!CollectionUtils.isEmpty(contractNumbers)) {
            List<ServiceContractContractNumbersEditRequest> returnList = new ArrayList<>();
            for (String item : contractNumbers) {
                ServiceContractContractNumbersEditRequest podsEditRequest = new ServiceContractContractNumbersEditRequest();
                podsEditRequest.setContractNumber(item);
                returnList.add(podsEditRequest);
            }
            return returnList;
        }
        return null;
    }

    private List<ServiceContractPodsEditRequest> getPodsForEditList(List<Long> podIds) {
        if (!CollectionUtils.isEmpty(podIds)) {
            List<ServiceContractPodsEditRequest> returnList = new ArrayList<>();
            for (Long item : podIds) {
                ServiceContractPodsEditRequest podsEditRequest = new ServiceContractPodsEditRequest();
                podsEditRequest.setPodId(item);
                returnList.add(podsEditRequest);
            }
            return returnList;
        }
        return null;
    }


    private List<ServiceContractUnrecognizedPodsEditRequest> getUnrecognizedPodsEditList(List<String> unrecognizedPods) {
        if (!CollectionUtils.isEmpty(unrecognizedPods)) {
            List<ServiceContractUnrecognizedPodsEditRequest> returnList = new ArrayList<>();
            for (String item : unrecognizedPods) {
                ServiceContractUnrecognizedPodsEditRequest serviceContractUnrecognizedPodsEditRequest = new ServiceContractUnrecognizedPodsEditRequest();
                serviceContractUnrecognizedPodsEditRequest.setPodName(item);
                returnList.add(serviceContractUnrecognizedPodsEditRequest);
            }
            return returnList;
        }
        return null;
    }

    private Boolean checkContractVersionWithStartDate(ServiceContracts serviceContract, LocalDate contractStartDate, List<String> errorMessages) {
        if (contractStartDate == null) {
            errorMessages.add("Start Date can't be null;");
            return null;
        }
        return serviceContractDetailsRepository.existsByContractIdAndStartDate(serviceContract.getId(), contractStartDate);
    }

    public ServiceContractDetails getLatestContractDetailsId(Long id, List<String> errorMessages) {
        List<ServiceContractDetails> version = serviceContractDetailsRepository.findAllByContractIdOrderByStartDateDesc(id);
        if (!CollectionUtils.isEmpty(version)) {
            return version.get(0);
        } else return null;
    }
}
