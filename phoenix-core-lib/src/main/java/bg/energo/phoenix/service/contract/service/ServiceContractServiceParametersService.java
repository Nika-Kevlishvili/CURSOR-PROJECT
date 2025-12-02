package bg.energo.phoenix.service.contract.service;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractInterimAdvancePaymentResponse;
import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractPriceComponentFormula;
import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractPriceComponentFormulaVariables;
import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractThirdPageFields;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.service.*;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetails;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentTerms;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.service.ServiceContractTerm;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceInterimAndAdvancePayment;
import bg.energo.phoenix.model.entity.product.service.ServicePriceComponent;
import bg.energo.phoenix.model.entity.product.term.terms.InvoicePaymentTerms;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.*;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermPeriodType;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.PaymentTermStatus;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.request.contract.express.ExpressContractRequest;
import bg.energo.phoenix.model.request.contract.express.ExpressContractServiceParametersRequest;
import bg.energo.phoenix.model.request.contract.product.PriceComponentContractFormula;
import bg.energo.phoenix.model.request.contract.service.*;
import bg.energo.phoenix.model.request.contract.service.edit.*;
import bg.energo.phoenix.model.response.contract.priceComponent.PriceComponentForContractResponse;
import bg.energo.phoenix.model.response.contract.productContract.ContractPriceComponentResponse;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.*;
import bg.energo.phoenix.model.response.contract.serviceContract.*;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentTermsResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.ApplicationModelResponse;
import bg.energo.phoenix.model.response.service.ServiceContractTermShortResponse;
import bg.energo.phoenix.model.response.terms.InvoicePaymentTermsResponse;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.*;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentTermsRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentFormulaVariableRepository;
import bg.energo.phoenix.repository.product.service.ServiceAdditionalParamsRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServiceContractTermRepository;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupTermsRepository;
import bg.energo.phoenix.service.product.iap.interimAdvancePayment.InterimAdvancePaymentTermsService;
import bg.energo.phoenix.service.product.price.priceComponent.applicationModel.ApplicationModelService;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Range;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceContractServiceParametersService {
    private final TermsRepository termsRepository;
    private final TermsGroupTermsRepository termsGroupTermsRepository;
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final ProductContractRepository productContractRepository;
    private final ServiceContractTermRepository serviceContractTermRepository;
    private final InvoicePaymentTermsRepository invoicePaymentTermsRepository;
    private final InterimAdvancePaymentTermsService interimAdvancePaymentTermsService;
    private final ServiceContractsRepository serviceContractRepository;
    private final ServiceContractValidatorService validatorService;
    private final ServiceContractPodsRepository serviceContractPodsRepository;
    private final ServiceUnrecognizedPodsRepository serviceUnrecognizedPodsRepository;
    private final ServiceContractSubObjectRepository serviceContractSubObjectRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    private final ContractLinkedServiceContractRepository contractLinkedServiceContractRepository;
    private final ContractLinkedProductContractRepository contractLinkedProductContractRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final CurrencyRepository currencyRepository;
    private final CalendarRepository calendarRepository;
    private final ServiceContractPriceComponentsRepository serviceContractPriceComponentsRepository;
    private final PriceComponentFormulaVariableRepository priceComponentFormulaVariableRepository;
    private final ServiceContractInterimAdvancePaymentsRepository contractInterimAdvancePaymentsRepository;
    private final InterimAdvancePaymentRepository advancePaymentRepository;
    private final ServiceContractInterimPriceFormulaRepository serviceContractInterimPriceFormulaRepository;
    private final InterimAdvancePaymentTermsRepository interimAdvancePaymentTermsRepository;
    private final ApplicationModelService applicationModelService;
    private final ServiceAdditionalParamsRepository serviceAdditionalParamsRepository;
    private final ServiceContractAdditionalParamsRepository serviceContractAdditionalParamsRepository;
    @Value("${contract.without_term.value}")
    private String maxDate;

    @Transactional
    public boolean create(ServiceContracts serviceContracts, ServiceContractCreateRequest request, ServiceDetails serviceDetails, ServiceContractDetails serviceContractDetails, ServiceContractThirdPageFields sourceView, List<String> errorMessages) {
        ServiceContractServiceParametersCreateRequest serviceParameters = request.getServiceParameters();

        validatorService.validateCreateRequest(request.getBasicParameters().getEntryIntoForceDate(), request.getBasicParameters().getSignInDate(), serviceParameters, sourceView, errorMessages);
        fillContractDetails(serviceContracts, serviceDetails, serviceContractDetails, serviceParameters, sourceView, request.getBasicParameters().getStartOfTheInitialTermOfTheContract(), errorMessages);
        validateDates(serviceContracts, errorMessages);
        return true;
    }

    //Todo should be removed
    private void fillContractTermEndDate(ServiceContractServiceParametersCreateRequest serviceParameters, ServiceContractThirdPageFields sourceView, ServiceContracts serviceContracts, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        Map<Long, ServiceContractTermShortResponse> collect = sourceView.getServiceContractTerms().stream().collect(Collectors.toMap(ServiceContractTermShortResponse::getId, j -> j));
        ServiceContractTermShortResponse termsResponse = collect.get(serviceParameters.getContractTermId());
        if (termsResponse != null) {
            if (termsResponse.getPeriodType().equals(ServiceContractTermPeriodType.PERIOD)) {
                if (serviceContracts.getContractInitialTermStartDate() != null) {
                    LocalDate date = serviceContracts.getContractInitialTermStartDate().plus(termsResponse.getValue(), termsResponse.getTermType().getUnit()).minusDays(1);
                    if (serviceParameters.getEntryIntoForceDate() != null) {
                        if (date.isAfter(serviceParameters.getEntryIntoForceDate())) {
                            serviceContracts.setContractTermEndDate(checkForDateRanges(date));
                        } else {
                            errorMessages.add("calculate contract term end date should be more or equal to entry into force date;");
                        }
                    } else serviceContracts.setContractTermEndDate(date);
                }
            } else if (termsResponse.getPeriodType().equals(ServiceContractTermPeriodType.CERTAIN_DATE)) {
                if (serviceContracts.getContractInitialTermStartDate() == null) {
                    serviceContracts.setContractTermEndDate(serviceParameters.getContractTermEndDate());
                }
                serviceContractDetails.setContractTermEndDate(serviceParameters.getContractTermEndDate());
            } else if (termsResponse.getPeriodType().equals(ServiceContractTermPeriodType.WITHOUT_TERM) /*&& serviceContracts.getContractInitialTermStartDate() == null*/) { //this change came from express contract - should be discussed if change appears here
                serviceContracts.setContractTermEndDate(LocalDate.of(2090, 12, 31));
            }
        }

    }

    public void fillServiceAdditionalParams(ServiceContractServiceParametersCreateRequest serviceParameters, boolean isSavingAsNewVersion, ServiceDetails serviceDetails, ServiceContractDetails serviceContractDetails, List<String> messages) {
        List<ServiceContractAdditionalParamsResponse> paramsFromService = serviceAdditionalParamsRepository
                .findServiceFilledAdditionalParamsByServiceDetailId(serviceDetails.getId());

        List<ContractServiceAdditionalParamsRequest> serviceAdditionalParams = serviceParameters.getContractServiceAdditionalParamsRequests();
        if (!isSavingAsNewVersion) {
            List<ServiceContractAdditionalParams> paramsFromContract = serviceContractAdditionalParamsRepository
                    .findAllByContractDetailId(serviceDetails.getId());
            findRedundantAdditionalParamsInContract(paramsFromService, paramsFromContract);
        }
        if (CollectionUtils.isEmpty(serviceAdditionalParams)) {
            for (ServiceContractAdditionalParamsResponse fromService : paramsFromService) {
                serviceAdditionalParams.add(new ContractServiceAdditionalParamsRequest(fromService.id(), fromService.value()));
            }
        }

        int index = 0;
        for (ContractServiceAdditionalParamsRequest paramsRequest : serviceAdditionalParams) {
            ServiceContractAdditionalParamsResponse serviceContractAdditionalParamsResponse = paramsFromService
                    .stream()
                    .filter(s -> s.id().equals(paramsRequest.getId()))
                    .findFirst()
                    .orElse(null);
            if (serviceContractAdditionalParamsResponse == null) {
                messages.add("serviceParameters.serviceAdditionalParams[%s]-additional params with id %s not found".formatted(index, paramsRequest.getId()));
            } else {
                Optional<ServiceContractAdditionalParams> additionalParam = serviceContractAdditionalParamsRepository.findByContractDetailIdAndServiceAdditionalParamId(serviceContractDetails.getId(), paramsRequest.getId());
                if (additionalParam.isEmpty() || isSavingAsNewVersion) {
                    ServiceContractAdditionalParams additionalParams = getContractProductAdditionalParams(serviceContractDetails, paramsRequest, serviceContractAdditionalParamsResponse);
                    serviceContractAdditionalParamsRepository.save(additionalParams);
                } else {
                    ServiceContractAdditionalParams serviceContractAdditionalParams = additionalParam.get();
                    serviceContractAdditionalParams.setValue(paramsRequest.getValue());
                    serviceContractAdditionalParamsRepository.save(serviceContractAdditionalParams);
                }
            }
            index++;
        }
    }

    private void findRedundantAdditionalParamsInContract(List<ServiceContractAdditionalParamsResponse> paramsFromService,
                                                         List<ServiceContractAdditionalParams> paramsFromContract) {

        if (org.apache.commons.collections4.CollectionUtils.isEmpty(paramsFromContract)) {
            return;
        }
        List<ServiceContractAdditionalParams> redundantParamsInContract = new ArrayList<>();

        List<Long> productAdditionalParamsIdsFromProduct = paramsFromService
                .stream()
                .map(ServiceContractAdditionalParamsResponse::id)
                .toList();
        for (ServiceContractAdditionalParams paramFromContract : paramsFromContract) {
            if (!productAdditionalParamsIdsFromProduct.contains(paramFromContract.getServiceAdditionalParamId())) {
                redundantParamsInContract.add(paramFromContract);
            }
        }
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(redundantParamsInContract)) {
            serviceContractAdditionalParamsRepository.deleteAll(redundantParamsInContract);
        }
    }

    private ServiceContractAdditionalParams getContractProductAdditionalParams(ServiceContractDetails serviceContractDetails, ContractServiceAdditionalParamsRequest paramsRequest, ServiceContractAdditionalParamsResponse serviceContractAdditionalParamsResponse) {
        ServiceContractAdditionalParams additionalParams = new ServiceContractAdditionalParams();
        additionalParams.setContractDetailId(serviceContractDetails.getId());
        additionalParams.setServiceAdditionalParamId(paramsRequest.getId());
        additionalParams.setValue(paramsRequest.getValue());
        additionalParams.setLabel(serviceContractAdditionalParamsResponse.label());
        return additionalParams;
    }

    public LocalDate checkForDateRanges(LocalDate localDate) {
        if (localDate.isAfter(LocalDate.of(2090, 12, 31))) {
            return LocalDate.of(2090, 12, 31);
        } else return localDate;
    }

    private void fillContractTermEndDateForEdit(ServiceContractEditRequest request, ServiceContractThirdPageFields sourceView, ServiceContracts serviceContract, ServiceContractDetails contractDetails, List<String> errorMessages) {
      /*  Map<Long, ServiceContractTermShortResponse> collect = sourceView.getServiceContractTerms().stream().collect(Collectors.toMap(ServiceContractTermShortResponse::getId, j -> j));
        ServiceContractTermShortResponse termsResponse = collect.get(request.getServiceParameters().getContractTermId());
        if (termsResponse.getPeriodType().equals(ServiceContractTermPeriodType.PERIOD)) {
            if (serviceContracts.getContractInitialTermStartDate() != null && serviceContracts.getContractTermEndDate() == null) {
                LocalDate date = serviceContracts.getContractInitialTermStartDate().plus(termsResponse.getValue(), termsResponse.getTermType().getUnit());
                if (request.getServiceParameters().getEntryIntoForceDate() != null) {
                    if (date.isAfter(request.getServiceParameters().getEntryIntoForceDate())) {
                        serviceContracts.setContractTermEndDate(date);
                    } else {
                        errorMessages.add("calculate contract term end date should be more or equal to entry into force date;");
                    }
                }

            }
        } else if (termsResponse.getPeriodType().equals(ServiceContractTermPeriodType.CERTAIN_DATE)) {
            if (serviceContracts.getContractTermEndDate() == null) {
                serviceContracts.setContractTermEndDate(request.getServiceParameters().getContractTermEndDate());
            }
            contractDetails.setContractTermEndDate(request.getServiceParameters().getContractTermEndDate());
        }*/
        Map<Long, ServiceContractTermShortResponse> collect = sourceView.getServiceContractTerms().stream().collect(Collectors.toMap(ServiceContractTermShortResponse::getId, j -> j));
        ServiceContractTermShortResponse termShortResponse = collect.get(request.getServiceParameters().getContractTermId());
        if (request.getBasicParameters().getContractTermEndDate() == null) {
            if (termShortResponse.getPeriodType().equals(ServiceContractTermPeriodType.PERIOD) /*&& serviceContract.getContractTermEndDate() == null*/) {
                if (serviceContract.getContractInitialTermStartDate() != null) {
                    LocalDate termEndDate = serviceContract.getContractInitialTermStartDate().plus(termShortResponse.getValue(), termShortResponse.getTermType().getUnit());
                    LocalDate maximumDate = LocalDate.parse(maxDate);
                    serviceContract.setContractTermEndDate(termEndDate.isBefore(maximumDate) ? termEndDate : maximumDate);
                }
            } else if (termShortResponse.getPeriodType().equals(ServiceContractTermPeriodType.CERTAIN_DATE)) {
                if (serviceContract.getContractTermEndDate() == null) {
                    serviceContract.setContractTermEndDate(request.getServiceParameters().getContractTermEndDate());
                }
                serviceContract.setContractTermEndDate(request.getServiceParameters().getContractTermEndDate());
            }
        }
    }


    @Transactional
    public void createSubObjects(ServiceContracts serviceContract, ServiceContractServiceParametersCreateRequest serviceParameters, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {

        //ServiceContractServiceParametersCreateRequest serviceParameters = request.getServiceParameters();

        createServiceAndProductContractSubObjects(serviceContract, serviceContractDetails, serviceParameters.getContractNumbers(), errorMessages);

        createPodSubObjects(serviceContract, serviceContractDetails, serviceParameters);

        createUnrecognisedPodsSubObjects(serviceContractDetails, serviceParameters);
    }

    @Transactional
    public void updateSubObjects(ServiceContracts serviceContract, ServiceContractEditRequest request, ServiceContractDetails serviceContractDetails, ServiceContractDetails contractDetails, List<String> errorMessages) {

        ServiceContractServiceParametersCreateRequest serviceParameters = request.getServiceParameters();
        List<ServiceContractContractNumbersEditRequest> contractNumbersEditList = request.getServiceParameters().getContractNumbersEditList();
        List<ServiceContractPodsEditRequest> podsEditList = request.getServiceParameters().getPodsEditList();
        List<ServiceContractUnrecognizedPodsEditRequest> unrecognizedPodsEditList = request.getServiceParameters().getUnrecognizedPodsEditList();

        editServiceAndProductContractSubObjects(contractNumbersEditList, serviceContract, serviceContractDetails, serviceParameters, errorMessages);

        if (!request.isSavingAsNewVersion()) {
            editPodSubObjects(podsEditList, serviceContract, serviceContractDetails, serviceParameters);
            updateUnrecognisedPodsSubObjects(unrecognizedPodsEditList, serviceContract, serviceContractDetails, serviceParameters, contractDetails);
        } else {
            createUnrecognisedPodsSubObjects(serviceContractDetails, serviceParameters);
            createPodSubObjects(serviceContract, serviceContractDetails, serviceParameters);
        }

        updateFormulaVariables(serviceContractDetails, serviceParameters);
        updateInterimAdvancePayments(serviceContractDetails, serviceParameters, contractDetails, errorMessages);
    }

    private void updateInterimAdvancePayments(ServiceContractDetails serviceContractDetails, ServiceContractServiceParametersCreateRequest serviceParameters, ServiceContractDetails contractDetails, List<String> errorMessages) {
        List<ServiceContractInterimAdvancePaymentsRequest> interimAdvancePaymentsRequests = serviceParameters.getInterimAdvancePaymentsRequests();
        List<ServiceContractInterimAdvancePayments> interims =
                contractInterimAdvancePaymentsRepository.findAllByContractDetailIdAndStatusIn(serviceContractDetails.getId(), List.of(ContractSubObjectStatus.ACTIVE));
        List<ServiceContractInterimAdvancePayments> updatedContractInterims = new ArrayList<>();
        checkInterimAdvancePayments(contractDetails.getServiceDetailId(), interims.stream().map(ServiceContractInterimAdvancePayments::getInterimAdvancePaymentId).toList(), serviceParameters, errorMessages);
        Map<Long, ServiceContractInterimAdvancePayments> interimMap = interims
                .stream()
                .collect(
                        Collectors
                                .toMap(ServiceContractInterimAdvancePayments::getInterimAdvancePaymentId, j -> j)
                );
        List<ServiceContractInterimPriceFormula> formulasToSave = new ArrayList<>();
        for (ServiceContractInterimAdvancePaymentsRequest interimAdvancePayment : interimAdvancePaymentsRequests) {
            ServiceContractInterimAdvancePayments iap = interimMap.remove(interimAdvancePayment.getInterimAdvancePaymentId());
            if (iap == null) {
                ServiceContractInterimAdvancePayments contractIap = createContractIap(serviceContractDetails, interimAdvancePayment, errorMessages);
                contractInterimAdvancePaymentsRepository.save(contractIap);
                createIapFormula(formulasToSave, interimAdvancePayment, contractIap);
            } else {
                iap.setValue(interimAdvancePayment.getValue());
                iap.setIssueDate(interimAdvancePayment.getIssueDate());
                iap.setTermValue(interimAdvancePayment.getTermValue());
                updateIapFormula(formulasToSave, interimAdvancePayment, iap);
                updatedContractInterims.add(iap);
            }
        }
        Collection<ServiceContractInterimAdvancePayments> values = interimMap.values();
        List<Long> contractInterimIds = new ArrayList<>();
        for (ServiceContractInterimAdvancePayments value : values) {
            value.setStatus(ContractSubObjectStatus.DELETED);
            contractInterimIds.add(value.getId());
            updatedContractInterims.add(value);
        }
        deleteIapFormulas(contractInterimIds, formulasToSave);
        serviceContractInterimPriceFormulaRepository.saveAll(formulasToSave);
        contractInterimAdvancePaymentsRepository.saveAll(updatedContractInterims);
    }

    private void checkInterimAdvancePayments(Long serviceDetailId, List<Long> list, ServiceContractServiceParametersCreateRequest serviceParameters, List<String> errorMessages) {
        List<ServiceContractInterimAdvancePaymentResponse> thirdTabIaps = getIapsForThirdTab(getServiceDetail(serviceDetailId));
        if (!CollectionUtils.isEmpty(thirdTabIaps)) {
            for (ServiceContractInterimAdvancePaymentResponse item : thirdTabIaps) {
                PaymentType paymentType = item.getPaymentType();
                if (paymentType != null) {
                    if (paymentType.equals(PaymentType.AT_LEAST_ONE)) {
                        if (!CollectionUtils.isEmpty(list)) {
                            for (Long id : list) {
                                Optional<InterimAdvancePayment> interimAdvancePaymentOptional = advancePaymentRepository.findById(id);
                                if (interimAdvancePaymentOptional.isPresent()) {
                                    InterimAdvancePayment interimAdvancePayment = interimAdvancePaymentOptional.get();
                                    if (interimAdvancePayment.getPaymentType() != null) {
                                        if (interimAdvancePayment.getPaymentType().equals(PaymentType.AT_LEAST_ONE)) {
                                            return;
                                        }
                                    }
                                }
                            }
                            errorMessages.add("serviceParameters.interimAdvancePaymentsRequests-should have at least one iap with AT_LEAST_ONE parameter checked;");
                        }
                    }
                }
            }
        }
    }

    private void deleteIapFormulas(List<Long> contractInterimIds, List<ServiceContractInterimPriceFormula> formulasToSave) {
        List<ServiceContractInterimPriceFormula> allById = serviceContractInterimPriceFormulaRepository.findAllByContractInterimAdvancePaymentIdInAndStatusIn(contractInterimIds, List.of(EntityStatus.ACTIVE));
        for (ServiceContractInterimPriceFormula formula : allById) {
            formula.setStatus(EntityStatus.DELETED);
            formulasToSave.add(formula);
        }
    }

    private void updateIapFormula(List<ServiceContractInterimPriceFormula> formulasToSave, ServiceContractInterimAdvancePaymentsRequest request, ServiceContractInterimAdvancePayments contractIaps) {
        List<ServiceContractInterimPriceFormula> contractFormulas =
                serviceContractInterimPriceFormulaRepository.findByContractIapIdAndStatus(contractIaps.getId(), List.of(EntityStatus.ACTIVE));
        Map<Long, ServiceContractInterimPriceFormula> formulaMap = contractFormulas.stream().collect(Collectors.toMap(ServiceContractInterimPriceFormula::getFormulaId, j -> j));
        List<PriceComponentContractFormula> formulaRequests = request.getContractFormulas();
        for (PriceComponentContractFormula formulaRequest : formulaRequests) {
            ServiceContractInterimPriceFormula formula = formulaMap.remove(formulaRequest.getFormulaVariableId());
            if (formula == null) {
                formulasToSave.add(new ServiceContractInterimPriceFormula(formulaRequest.getValue(), formulaRequest.getFormulaVariableId(), contractIaps.getId()));
            } else {
                formula.setValue(formulaRequest.getValue());
                formulasToSave.add(formula);
            }
        }
        Collection<ServiceContractInterimPriceFormula> values = formulaMap.values();
        for (ServiceContractInterimPriceFormula value : values) {
            value.setStatus(EntityStatus.DELETED);
            formulasToSave.add(value);
        }
    }

    private void updateFormulaVariables(ServiceContractDetails serviceContractDetails, ServiceContractServiceParametersCreateRequest serviceParameters) {
        List<PriceComponentContractFormula> contractFormulas = serviceParameters.getContractFormulas();
        List<ServiceContractPriceComponents> contractPriceComponents =
                serviceContractPriceComponentsRepository.findByContractDetailIdAndStatusIn(serviceContractDetails.getId(), List.of(ContractSubObjectStatus.ACTIVE));

        List<ServiceContractPriceComponents> updatePriceComponents = new ArrayList<>();
        Map<Long, ServiceContractPriceComponents> priceComponentsMap = contractPriceComponents.stream().collect(Collectors.toMap(ServiceContractPriceComponents::getPriceComponentFormulaVariableId, j -> j));
        for (PriceComponentContractFormula contractFormula : contractFormulas) {
            ServiceContractPriceComponents priceComponents = priceComponentsMap.remove(contractFormula.getFormulaVariableId());
            if (priceComponents == null) {
                ServiceContractPriceComponents newPriceComponents = createContractPriceComponent(serviceContractDetails, contractFormula);
                updatePriceComponents.add(newPriceComponents);
            } else {
                priceComponents.setValue(contractFormula.getValue());
                updatePriceComponents.add(priceComponents);
            }
        }
        Collection<ServiceContractPriceComponents> deletedValues = priceComponentsMap.values();
        for (ServiceContractPriceComponents value : deletedValues) {
            value.setStatus(ContractSubObjectStatus.DELETED);
            updatePriceComponents.add(value);
        }
        serviceContractPriceComponentsRepository.saveAll(updatePriceComponents);
    }

    @Transactional
    public void createUnrecognisedPodsSubObjects(ServiceContractDetails serviceContractDetails, ServiceContractServiceParametersCreateRequest serviceParameters) {
        List<String> unrecognizedPods = serviceParameters.getUnrecognizedPods();
        if (!CollectionUtils.isEmpty(unrecognizedPods)) {
            List<ServiceUnrecognizedPods> unrecognizedPodsToSave = new ArrayList<>();
            for (String unrecognizedPod : unrecognizedPods) {
                unrecognizedPodsToSave.add(createUnrecognizedPod(unrecognizedPod, serviceContractDetails));
            }
            serviceUnrecognizedPodsRepository.saveAll(unrecognizedPodsToSave);
        }
    }

    @Transactional
    public void updateUnrecognisedPodsSubObjects(List<ServiceContractUnrecognizedPodsEditRequest> unrecognizedPodsEditList, ServiceContracts serviceContract, ServiceContractDetails serviceContractDetails, ServiceContractServiceParametersCreateRequest serviceParameters, ServiceContractDetails contractDetails) {
        if (!CollectionUtils.isEmpty(unrecognizedPodsEditList)) {
            List<ServiceUnrecognizedPods> dbPods =
                    serviceUnrecognizedPodsRepository.findByContractDetailsIdAndStatus(contractDetails.getId(), ServiceSubobjectStatus.ACTIVE);
            List<ServiceContractUnrecognizedPodsEditRequest> podsTobeCreated = new ArrayList<>();
            List<ServiceUnrecognizedPods> podsToBeUpdated = new ArrayList<>();
            for (ServiceContractUnrecognizedPodsEditRequest item : unrecognizedPodsEditList) {
                if (item.getId() == null) {
                    podsTobeCreated.add(item);
                } else {
                    Optional<ServiceUnrecognizedPods> serviceUnrecognizedPodsOptional =
                            serviceUnrecognizedPodsRepository.findByIdAndStatusAndContractDetailsId(item.getId(), ServiceSubobjectStatus.ACTIVE, contractDetails.getId());
                    if (serviceUnrecognizedPodsOptional.isPresent()) {
                        podsToBeUpdated.add(serviceUnrecognizedPodsOptional.get());
                    } else {
                        throw new DomainEntityNotFoundException("Can't find unrecognized pod with id:%s;".formatted(item.getId()));
                    }
                }
            }
            List<Long> ids = unrecognizedPodsEditList.stream().map(ServiceContractUnrecognizedPodsEditRequest::getId).filter(Objects::nonNull).toList();
            List<ServiceUnrecognizedPods> podsToBeDeleted =
                    serviceUnrecognizedPodsRepository.findByContractDetailsIdAndStatusAndIdNotIn(
                            serviceContractDetails.getId(),
                            ServiceSubobjectStatus.ACTIVE,
                            ids);
            if (!CollectionUtils.isEmpty(podsToBeDeleted)) {
                for (ServiceUnrecognizedPods item : podsToBeDeleted) {
                    item.setStatus(ServiceSubobjectStatus.DELETED);
                    serviceUnrecognizedPodsRepository.save(item);
                }
            }
            if (CollectionUtils.isEmpty(podsToBeUpdated)) {
                for (ServiceUnrecognizedPods item : dbPods) {
                    item.setStatus(ServiceSubobjectStatus.DELETED);
                    serviceUnrecognizedPodsRepository.save(item);
                }
            }
            if (!CollectionUtils.isEmpty(podsTobeCreated)) {
                for (ServiceContractUnrecognizedPodsEditRequest item : podsTobeCreated) {
                    serviceUnrecognizedPodsRepository.save(createUnrecognizedPod(item.getPodName(), serviceContractDetails));
                }
            }
        } else {
            List<ServiceUnrecognizedPods> dbPods =
                    serviceUnrecognizedPodsRepository.findByContractDetailsIdAndStatus(serviceContractDetails.getId(), ServiceSubobjectStatus.ACTIVE);
            if (!CollectionUtils.isEmpty(dbPods)) {
                for (ServiceUnrecognizedPods item : dbPods) {
                    item.setStatus(ServiceSubobjectStatus.DELETED);
                    serviceUnrecognizedPodsRepository.save(item);
                }
            }
        }
    }

    @Transactional
    public void createPodSubObjects(ServiceContracts serviceContract, ServiceContractDetails serviceContractDetails, ServiceContractServiceParametersCreateRequest serviceParameters) {
        List<Long> podIds = serviceParameters.getPodIds();
        if (!CollectionUtils.isEmpty(podIds)) {
            List<ServiceContractPods> contractPodsToSave = new ArrayList<>();
            for (Long id : podIds) {
                PointOfDelivery pointOfDelivery = pointOfDeliveryRepository.findByIdAndStatusIn(id, List.of(PodStatus.ACTIVE))
                        .orElseThrow(() -> new DomainEntityNotFoundException("Can't find active point of delivery with id: %s".formatted(id)));
                contractPodsToSave.add(createContractPod(serviceContract, pointOfDelivery, serviceContractDetails));
            }
            serviceContractPodsRepository.saveAll(contractPodsToSave);
        }
    }

    @Transactional
    public void editPodSubObjects(List<ServiceContractPodsEditRequest> podsEditList, ServiceContracts serviceContract, ServiceContractDetails serviceContractDetails, ServiceContractServiceParametersCreateRequest serviceParameters) {
        if (!CollectionUtils.isEmpty(podsEditList)) {
            List<ServiceContractPods> dbPods = serviceContractPodsRepository.findByContractDetailIdAndStatus(serviceContractDetails.getId(), ContractSubObjectStatus.ACTIVE);
            Set<ServiceContractPods> podsToBeUpdated = new HashSet<>();
            List<ServiceContractPodsEditRequest> podsToBeCreated = new ArrayList<>();
            for (ServiceContractPodsEditRequest item : podsEditList) {
                if (item.getId() == null) {
                    podsToBeCreated.add(item);
                } else {
                    Optional<ServiceContractPods> serviceContractPodsOptional = serviceContractPodsRepository.findByIdAndContractDetailIdAndStatus(item.getId(), serviceContractDetails.getId(), ContractSubObjectStatus.ACTIVE);
                    if (serviceContractPodsOptional.isPresent()) {
                        podsToBeUpdated.add(serviceContractPodsOptional.get());
                    } else {
                        throw new DomainEntityNotFoundException("serviceContractPod with Id:%s can't be found;".formatted(item.getId()));
                    }
                }
            }
            List<Long> ids = podsEditList.stream().map(ServiceContractPodsEditRequest::getId).filter(Objects::nonNull).toList();
            List<ServiceContractPods> dbPodsToDelete =
                    serviceContractPodsRepository.findByContractDetailIdAndStatusAndIdNotIn(serviceContractDetails.getId(),
                            ContractSubObjectStatus.ACTIVE, ids);
            if (!CollectionUtils.isEmpty(dbPodsToDelete)) {
                for (ServiceContractPods item : dbPodsToDelete) {
                    item.setStatus(ContractSubObjectStatus.DELETED);
                    serviceContractPodsRepository.save(item);
                }
            }
            if (CollectionUtils.isEmpty(podsToBeUpdated)) {
                for (ServiceContractPods item : dbPods) {
                    item.setStatus(ContractSubObjectStatus.DELETED);
                    serviceContractPodsRepository.save(item);
                }
            }
            if (!podsToBeCreated.isEmpty()) {
                for (ServiceContractPodsEditRequest item : podsToBeCreated) {
                    List<ServiceContractPods> contractPodsToSave = new ArrayList<>();
                    PointOfDelivery pointOfDelivery = pointOfDeliveryRepository.findByIdAndStatusIn(item.getPodId(), List.of(PodStatus.ACTIVE))
                            .orElseThrow(() -> new DomainEntityNotFoundException("Can't find active point of delivery with id: %s".formatted(item.getPodId())));
                    contractPodsToSave.add(createContractPod(serviceContract, pointOfDelivery, serviceContractDetails));
                    serviceContractPodsRepository.saveAll(contractPodsToSave);
                }
            }
        } else {
            List<ServiceContractPods> podsToBeDeleted = serviceContractPodsRepository.findByContractDetailIdAndStatus(serviceContractDetails.getId(), ContractSubObjectStatus.ACTIVE);
            if (!CollectionUtils.isEmpty(podsToBeDeleted)) {
                for (ServiceContractPods item : podsToBeDeleted) {
                    item.setStatus(ContractSubObjectStatus.DELETED);
                    serviceContractPodsRepository.save(item);
                }
            }
        }
    }

    private void createServiceAndProductContractSubObjects(ServiceContracts serviceContract, ServiceContractDetails serviceContractDetails, List<String> contractNumbers, List<String> errorMessages) {
        //List<String> contractNumbers = serviceParameters.getContractNumbers();
        if (!CollectionUtils.isEmpty(contractNumbers)) {
            List<ContractLinkedServiceContract> contractLinkedServiceContracts = new ArrayList<>();
            List<ContractLinkedProductContract> contractLinkedProductContracts = new ArrayList<>();
            for (int i = 0; i < contractNumbers.size(); i++) {
                String contractNumber = contractNumbers.get(i);
                Optional<ServiceContracts> serviceContractOptional = serviceContractRepository.findByContractNumberAndStatus(contractNumber, EntityStatus.ACTIVE);
                Optional<ProductContract> productContractOptional = productContractRepository.findByContractNumberAndStatus(contractNumber, ProductContractStatus.ACTIVE);
                ServiceContracts serviceContracts = null;
                ProductContract productContract = null;
                if (serviceContractOptional.isEmpty() && productContractOptional.isEmpty()) {
                    throw new DomainEntityNotFoundException("contractNumbers[%s]-Can't find active contract with contract number: %s".formatted(i, contractNumber));
                }
                if (serviceContractOptional.isPresent()) {
                    serviceContracts = serviceContractOptional.get();
                    contractLinkedServiceContracts.add(createServiceContractSubObject(serviceContract, serviceContracts, serviceContractDetails.getId(), errorMessages, i));
                }
                if (productContractOptional.isPresent()) {
                    productContract = productContractOptional.get();
                    contractLinkedProductContracts.add(createProductContractSubObject(serviceContract, productContract, serviceContractDetails.getId()));
                }
            }
            if (!CollectionUtils.isEmpty(contractLinkedServiceContracts)) {
                if (errorMessages.isEmpty())
                    contractLinkedServiceContractRepository.saveAllAndFlush(contractLinkedServiceContracts);
            }
            if (!CollectionUtils.isEmpty(contractLinkedProductContracts)) {
                contractLinkedProductContractRepository.saveAllAndFlush(contractLinkedProductContracts);
            }
        }
    }

    private void editServiceAndProductContractSubObjects(List<ServiceContractContractNumbersEditRequest> contractNumbersEditList, ServiceContracts serviceContract, ServiceContractDetails serviceContractDetails, ServiceContractServiceParametersCreateRequest serviceParameters, List<String> errorMessages) {
        //contractNumbersEditList = List.of(new ServiceContractContractNumbersEditRequest(1L,"EPES2308000167"),new ServiceContractContractNumbersEditRequest(1L,"EPES2308000455"));
    /*    if(!hasSameContractNumbers(serviceContract.getContractNumber(),contractNumbersEditList)){
            errorMessages.add("paymentGuarantee.contractNumbersEditList- [contractNumbersEditList] cant have same contract;");
        }*/
        if (!CollectionUtils.isEmpty(contractNumbersEditList)) {
            if (!hasUniqueContractNumbers(contractNumbersEditList)) {
                errorMessages.add("paymentGuarantee.contractNumbersEditList- [contractNumbersEditList] should have unique contracts;");
            }
        }
        if (!CollectionUtils.isEmpty(contractNumbersEditList)) {
            List<ProductContract> productContractsFromRequest = new ArrayList<>();
            List<ServiceContracts> serviceContractsFromRequest = new ArrayList<>();
            List<ContractLinkedProductContract> linkedProductContractsShouldBeEdited = new ArrayList<>();
            List<ContractLinkedServiceContract> linkedServiceContractsShouldBeEdited = new ArrayList<>();
            List<String> linkedProductContractsShouldBeCreated = new ArrayList<>();
            List<String> linkedServiceContractsShouldBeCreated = new ArrayList<>();
            for (ServiceContractContractNumbersEditRequest item : contractNumbersEditList) {
                if (!StringUtils.isEmpty(item.getContractNumber())) {
                    Optional<ProductContract> productContractsOptional = productContractRepository.findByContractNumberAndStatus(item.getContractNumber(), ProductContractStatus.ACTIVE);
                    Optional<ServiceContracts> serviceContractsOptional = serviceContractRepository.findByContractNumberAndStatus(item.getContractNumber(), EntityStatus.ACTIVE);
                    productContractsOptional.ifPresent(productContractsFromRequest::add);
                    serviceContractsOptional.ifPresent(serviceContractsFromRequest::add);
                    if (productContractsOptional.isPresent()) {
                        ProductContract productContract = productContractsOptional.get();
                        productContractsFromRequest.add(productContract);
                        Optional<ContractLinkedProductContract> linkedProductContractOptional =
                                contractLinkedProductContractRepository.findByLinkedProductContractIdAndContractIdAndStatus(productContract.getId(), serviceContract.getId(), ContractSubObjectStatus.ACTIVE);
                        if (linkedProductContractOptional.isPresent()) {
                            linkedProductContractsShouldBeEdited.add(linkedProductContractOptional.get());
                        } else {
                            linkedProductContractsShouldBeCreated.add(productContract.getContractNumber());
                        }
                    }
                    if (serviceContractsOptional.isPresent()) {
                        ServiceContracts serviceContracts = serviceContractsOptional.get();
                        serviceContractsFromRequest.add(serviceContracts);
                        Optional<ContractLinkedServiceContract> linkedServiceContractOptional =
                                contractLinkedServiceContractRepository.findByLinkedServiceContractIdAndContractIdAndStatus(serviceContracts.getId(), serviceContract.getId(), ContractSubObjectStatus.ACTIVE);
                        if (linkedServiceContractOptional.isPresent()) {
                            linkedServiceContractsShouldBeEdited.add(linkedServiceContractOptional.get());
                        } else {
                            linkedServiceContractsShouldBeCreated.add(serviceContracts.getContractNumber());
                        }
                    }
                }
            }
            //TODO add DELETE FUNCTIONALITY
            List<ContractLinkedProductContract> likedProductsToBeDeleted =
                    contractLinkedProductContractRepository.findAllByContractIdAndStatusAndIdNotIn(
                            serviceContract.getId(),
                            ContractSubObjectStatus.ACTIVE,
                            linkedProductContractsShouldBeEdited.stream().map(ContractLinkedProductContract::getId).toList());
            if (!CollectionUtils.isEmpty(likedProductsToBeDeleted)) {
                for (ContractLinkedProductContract item : likedProductsToBeDeleted) {
                    item.setStatus(ContractSubObjectStatus.DELETED);
                    contractLinkedProductContractRepository.save(item);
                }
            }
            List<ContractLinkedServiceContract> linkedServiceContractsToBeDeleted =
                    contractLinkedServiceContractRepository.findByContractIdAndStatusAndIdNotIn(
                            serviceContract.getId(),
                            ContractSubObjectStatus.ACTIVE,
                            linkedServiceContractsShouldBeEdited.stream().map(ContractLinkedServiceContract::getId).toList());

            if (!CollectionUtils.isEmpty(linkedServiceContractsToBeDeleted)) {
                for (ContractLinkedServiceContract item : linkedServiceContractsToBeDeleted) {
                    item.setStatus(ContractSubObjectStatus.DELETED);
                    contractLinkedServiceContractRepository.save(item);
                }
            }
            if (!CollectionUtils.isEmpty(linkedProductContractsShouldBeCreated)) {
                createServiceAndProductContractSubObjects(serviceContract, serviceContractDetails, linkedProductContractsShouldBeCreated, errorMessages);
            }
            if (!CollectionUtils.isEmpty(linkedServiceContractsShouldBeCreated)) {
                createServiceAndProductContractSubObjects(serviceContract, serviceContractDetails, linkedServiceContractsShouldBeCreated, errorMessages);
            }
        } else {
            List<ContractLinkedProductContract> dbProductLinkedContracts = contractLinkedProductContractRepository.findAllByContractIdAndStatus(serviceContract.getId(), ContractSubObjectStatus.ACTIVE);
            List<ContractLinkedServiceContract> dbServiceLinkedContracts = contractLinkedServiceContractRepository.findAllByContractIdAndStatus(serviceContract.getId(), ContractSubObjectStatus.ACTIVE);
            if (!CollectionUtils.isEmpty(dbProductLinkedContracts)) {
                for (ContractLinkedProductContract item : dbProductLinkedContracts) {
                    item.setStatus(ContractSubObjectStatus.DELETED);
                    contractLinkedProductContractRepository.save(item);
                }
            }
            if (!CollectionUtils.isEmpty(dbServiceLinkedContracts)) {
                for (ContractLinkedServiceContract item : dbServiceLinkedContracts) {
                    item.setStatus(ContractSubObjectStatus.DELETED);
                    contractLinkedServiceContractRepository.save(item);
                }
            }
        }
    }

    public boolean hasUniqueContractNumbers(List<ServiceContractContractNumbersEditRequest> contractNumbersEditList) {
        Set<String> seenContractNumbers = new HashSet<>();
        for (ServiceContractContractNumbersEditRequest item : contractNumbersEditList) {
            String contractNumber = item.getContractNumber();
            if (seenContractNumbers.contains(contractNumber)) {
                return false;
            }
            seenContractNumbers.add(contractNumber);
        }
        return true;
    }
   /* public boolean hasSameContractNumbers(String contractsNumber,List<ServiceContractContractNumbersEditRequest> contractNumbersEditList) {
        for (ServiceContractContractNumbersEditRequest item : contractNumbersEditList) {
            String contractNumber = item.getContractNumber();
            if(contractsNumber.equals(contractNumber)){
                return false;
            }
        }
        return true;
    }*/

    private ContractLinkedServiceContract createServiceContractSubObject(ServiceContracts serviceContract, ServiceContracts serviceContracts, Long id, List<String> errorMessages, int i) {
        ContractLinkedServiceContract serviceContractToSave = new ContractLinkedServiceContract();
        serviceContractToSave.setContractId(serviceContract.getId());
        Long linkedServiceContractId = serviceContractDetailsRepository.findByContractIdAndLatestDetailsWithoutStatuses(serviceContracts.getContractNumber());
        Optional<ServiceContracts> linkedServiceContractOptional = serviceContractRepository.findById(linkedServiceContractId);
        if (linkedServiceContractOptional.isPresent()) {
            ServiceContracts linkedServiceContract = linkedServiceContractOptional.get();
            if (!linkedServiceContract.getContractNumber().equals(serviceContract.getContractNumber())) {
                //serviceContract.setLinkedContractDetailId(getContractDetailId(serviceContracts.getId()));
                serviceContractToSave.setLinkedServiceContractId(linkedServiceContractId);//TODO witch version should choose
                serviceContractToSave.setStatus(ContractSubObjectStatus.ACTIVE);
                return serviceContractToSave;
            } else {
                errorMessages.add("contractNumbers[%s]-Can't link contract to itself;".formatted(i));
                return null;
            }
        } else return null;
    }

    private Long getContractDetailId(Long id) {
        return null;
    }

    private ContractLinkedProductContract createProductContractSubObject(ServiceContracts serviceContract, ProductContract serviceContracts, Long id) {
        ContractLinkedProductContract productContract = new ContractLinkedProductContract();
        productContract.setContractId(serviceContract.getId());
        //productContract.setLinkedContractDetailId(serviceContracts.getId());
        productContract.setLinkedProductContractId(productContractDetailsRepository.findByContractIdAndLatestDetailWithoutStatuses(serviceContracts.getContractNumber()));//TODO witch version should choose
        productContract.setStatus(ContractSubObjectStatus.ACTIVE);
        return productContract;
    }


    private ServiceUnrecognizedPods createUnrecognizedPod(String unrecognizedPodString, ServiceContractDetails serviceContractDetails) {
        ServiceUnrecognizedPods unrecognizedPod = new ServiceUnrecognizedPods();
        unrecognizedPod.setPodIdentifier(unrecognizedPodString);
        unrecognizedPod.setStatus(ServiceSubobjectStatus.ACTIVE);
        unrecognizedPod.setContractDetailsId(serviceContractDetails.getId());
        return unrecognizedPod;
    }

    private ServiceContractPods createContractPod(ServiceContracts serviceContract, PointOfDelivery pointOfDelivery, ServiceContractDetails serviceContractDetails) {
        PointOfDeliveryDetails pointOfDeliveryDetails = pointOfDeliveryDetailsRepository.findById(pointOfDelivery.getLastPodDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Active pointOfDelivery details by id:%s".formatted(pointOfDelivery.getLastPodDetailId())));
        ServiceContractPods contractPod = new ServiceContractPods();
//        contractPod.setContractId(serviceContract.getId());
        contractPod.setContractDetailId(serviceContractDetails.getId());
        contractPod.setPodId(pointOfDelivery.getId());
        contractPod.setStatus(ContractSubObjectStatus.ACTIVE);
        return contractPod;
    }


    private LocalDate getContractTermEndDate(LocalDate startDate, ServiceDetails serviceDetails, ServiceContractDetails details, ServiceContractServiceParametersCreateRequest request, LocalDate contractTermEndDate, ServiceContractThirdPageFields sourceView) {
        List<ServiceContractTerm> serviceContractTerm = serviceContractTermRepository.findByServiceDetailsIdAndStatusInOrderByCreateDate(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));
        for (ServiceContractTerm item : serviceContractTerm) {
            if (item.getPeriodType().equals(ServiceContractTermPeriodType.PERIOD)) {
                if (startDate != null) {
                    switch (item.getTermType()) {
                        case DAY_DAYS:
                            return addDaysToEndDate(item, startDate);
                        case MONTH_MONTHS:
                            return addMonthToEndDate(item, startDate);
                        case YEAR_YEARS:
                            return addYearsToEndDate(item, startDate);
                    }
                }
                return null;
            } else if (item.getPeriodType().equals(ServiceContractTermPeriodType.CERTAIN_DATE)) {
                //return contractTermEndDate;
                return request.getStartOfContractInitialTermDate();
            } else if (item.getPeriodType().equals(ServiceContractTermPeriodType.WITHOUT_TERM) /*|| item.getPeriodType().equals(ServiceContractTermPeriodType.OTHER)*/) {
                return LocalDate.of(2090, 12, 31);
            } else if (item.getPeriodType().equals(ServiceContractTermPeriodType.OTHER)) {
                return null;
            }
        }
        return null;
    }

    private LocalDate addYearsToEndDate(ServiceContractTerm item, LocalDate contractTermEndDate) {
        return contractTermEndDate.plusYears(item.getValue());
    }

    private LocalDate addMonthToEndDate(ServiceContractTerm item, LocalDate contractTermEndDate) {
        return contractTermEndDate.plusMonths(item.getValue());
    }

    private LocalDate addDaysToEndDate(ServiceContractTerm item, LocalDate contractTermEndDate) {
        return contractTermEndDate.plusDays(item.getValue());
    }

    public List<ServiceContractInterimAdvancePaymentResponse> getIapsForThirdTab(ServiceDetails details) {
        List<ServiceContractInterimAdvancePaymentResponse> interimAdvancePayments = new ArrayList<>();
        Comparator<InterimAdvancePayment> interimAdvancePaymentComparator = Comparator.comparingLong(InterimAdvancePayment::getId);
        interimAdvancePayments.addAll(
                getFromDirectIap(
                        details
                                .getInterimAndAdvancePayments()
                                .stream()
                                .filter(in ->
                                        in.getStatus() == ServiceSubobjectStatus.ACTIVE)
                                .map(ServiceInterimAndAdvancePayment::getInterimAndAdvancePayment)
                                .filter(e ->
                                        e.getStatus() == InterimAdvancePaymentStatus.ACTIVE)
                                .sorted(interimAdvancePaymentComparator)
                                .toList()));
        interimAdvancePayments.addAll(getFromGroupsIap(serviceContractRepository.getIapsByServiceDetailIdAndCurrentDate(details.getId())));
        return interimAdvancePayments;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ServiceContractThirdPageFields thirdPageFields(ServiceDetails details) {
        ServiceContractThirdPageFields result = new ServiceContractThirdPageFields();
        Terms terms = fetchServiceDetailsTerm(details, result);
        result.setPaymentGuarantees(details.getPaymentGuarantees());
        result.setContractEntryIntoForces(getContractEntryIntoForceForContractFields(terms));
        result.setStartOfContractInitialTermsForContractFields(getStartOfContractInitialTermsForContractFields(terms));
        //result.setSupplyActivationsForContractFields(getSupplyActivationsForContractFields(details.getTerms()));
        result.setWaitForOldContractTermToExpires(terms.getWaitForOldContractTermToExpires());
        result.setInstallmentForContractFields(new InstallmentForContractFields(details.getEqualMonthlyInstallmentsActivation(), details.getInstallmentNumber(), details.getInstallmentNumberFrom(), details.getInstallmentNumberTo(), details.getAmount(), details.getAmountFrom(), details.getAmountTo()));
        result.setServiceContractTerms(serviceContractTermRepository.findByServiceDetailsIdAndStatusInOrderByCreateDate(details.getId(), List.of(ServiceSubobjectStatus.ACTIVE)).stream().map(ServiceContractTermShortResponse::new).toList());
        result.setInvoicePaymentTerms(invoicePaymentTermsRepository.findDetailedByTermIdAndStatusInForServiceContract(terms.getId(), List.of(PaymentTermStatus.ACTIVE)));
        result.setDepositResponse(new DepositResponse(details.getCashDepositAmount(), details.getCashDepositCurrency(), details.getBankGuaranteeAmount(), details.getBankGuaranteeCurrency()));
        List<ServiceContractInterimAdvancePaymentResponse> interimAdvancePayments = new ArrayList<>();
        Comparator<InterimAdvancePayment> interimAdvancePaymentComparator = Comparator.comparingLong(InterimAdvancePayment::getId);
        interimAdvancePayments.addAll(
                getFromDirectIap(
                        details
                                .getInterimAndAdvancePayments()
                                .stream()
                                .filter(in ->
                                        in.getStatus() == ServiceSubobjectStatus.ACTIVE)
                                .map(ServiceInterimAndAdvancePayment::getInterimAndAdvancePayment)
                                .filter(e ->
                                        e.getStatus() == InterimAdvancePaymentStatus.ACTIVE)
                                .sorted(interimAdvancePaymentComparator)
                                .toList()));
        interimAdvancePayments.addAll(getFromGroupsIap(serviceContractRepository.getIapsByServiceDetailIdAndCurrentDate(details.getId())));
        result.setInterimAdvancePayments(interimAdvancePayments);

        List<ServiceContractPriceComponentFormula> formulaVariables = getFormulaVariables(details);
        result.setFormulaVariables(formulaVariables);
        result.setQuantityVisible(checkPerPieceComponentExists(formulaVariables, details.getId()));
        result.setServiceContractAdditionalParamsResponses(
                serviceAdditionalParamsRepository.findServiceFilledAdditionalParamsByServiceDetailId(details.getId()));
        return result;
    }

    private Terms fetchServiceDetailsTerm(ServiceDetails details, ServiceContractThirdPageFields result) {
        Terms terms;
        if (details.getTerms() != null) {
            terms = details.getTerms();
        } else {
            Long groupOfTermsId = details.getTermsGroups().getId();
            terms = termsRepository
                    .findById(
                            termsRepository
                                    .getTermIdFromCurrentTermGroup(groupOfTermsId))
                    .orElse(null);
            result.setTermFromGroup(true);
        }

        if (Objects.isNull(terms)) {
            throw new DomainEntityNotFoundException("Service Details Term not found;");
        }
        return terms;
    }

    private List<ServiceContractPriceComponentFormula> convertToPriceComponentFormulaFromGroups(Long id) {
        if (id == null) {
            return new ArrayList<>();
        }
        List<ServiceContractPriceComponentFormula> result = new ArrayList<>();
        List<PriceComponentForContractResponse> priceComponents = serviceContractRepository.getPriceComponentFromServicePriceComponentGroups(id);
        for (PriceComponentForContractResponse pc : priceComponents) {
            ServiceContractPriceComponentFormula priceComponentFormula = new ServiceContractPriceComponentFormula();
            priceComponentFormula.setPriceComponentId(pc.getId());
            List<ServiceContractPriceComponentFormulaVariables> variables = new ArrayList<>();
            List<PriceComponentFormulaVariable> formulaVariables = priceComponentFormulaVariableRepository.findAllByPriceComponentIdOrderByIdAsc(pc.getId());
            for (PriceComponentFormulaVariable v : formulaVariables) {
                ServiceContractPriceComponentFormulaVariables formulaVariable = new ServiceContractPriceComponentFormulaVariables();
                formulaVariable.setFormulaVariableId(v.getId());
                formulaVariable.setVariable(v.getVariable());
                formulaVariable.setValue(v.getValue());
                formulaVariable.setValueFrom(v.getValueFrom());
                formulaVariable.setValueTo(v.getValueTo());
                formulaVariable.setVariableDescription(v.getDescription());
                formulaVariable.setDisplayName(v.getDescription() + " (" + v.getVariable() + " from " + pc.getName() + ")");
                variables.add(formulaVariable);
            }
            priceComponentFormula.setVariables(variables);
            if (!variables.isEmpty()) {
                priceComponentFormula.setFromGroup(true);
                result.add(priceComponentFormula);
            }
        }
        return result;
    }

    private void getContractSubObjectDetails(ServiceContracts serviceContract, ServiceContractDetails details, String contractNumber, ServiceParametersPreview serviceParametersPreview) {
        List<SubObjectContractResponse> contractResponseList = new ArrayList<>();
        ServiceContracts serviceContracts = null;
        ProductContract productContract = null;
        Optional<ServiceContracts> serviceContractOptional = serviceContractRepository.findByContractNumberAndStatus(contractNumber, EntityStatus.ACTIVE);
        Optional<ProductContract> productContractOptional = productContractRepository.findByContractNumberAndStatus(contractNumber, ProductContractStatus.ACTIVE);
        List<ContractLinkedProductContract> dbProductLinkedContracts = contractLinkedProductContractRepository.findAllByContractIdAndStatus(serviceContract.getId(), ContractSubObjectStatus.ACTIVE);
        List<ContractLinkedServiceContract> dbServiceLinkedContracts = contractLinkedServiceContractRepository.findAllByContractIdAndStatus(serviceContract.getId(), ContractSubObjectStatus.ACTIVE);

        if (!CollectionUtils.isEmpty(dbProductLinkedContracts)) {
            for (ContractLinkedProductContract item : dbProductLinkedContracts) {
                SubObjectContractResponse subObjectContractResponse = new SubObjectContractResponse();
                subObjectContractResponse.setId(item.getLinkedProductContractId());
                subObjectContractResponse.setContractNumber(getProductContractWithId(item.getLinkedProductContractId()));
                subObjectContractResponse.setType(ContractType.PRODUCT_CONTRACT);
                contractResponseList.add(subObjectContractResponse);
            }
        }
        if (!CollectionUtils.isEmpty(dbServiceLinkedContracts)) {
            for (ContractLinkedServiceContract item : dbServiceLinkedContracts) {
                SubObjectContractResponse subObjectContractResponse = new SubObjectContractResponse();
                subObjectContractResponse.setId(item.getLinkedServiceContractId());
                subObjectContractResponse.setContractNumber(getServiceContractWithId(item.getLinkedServiceContractId()));
                subObjectContractResponse.setType(ContractType.SERVICE_CONTRACT);
                contractResponseList.add(subObjectContractResponse);
            }
        }
       /* if (serviceContractOptional.isPresent()) {
            serviceContracts = serviceContractOptional.get();
            SubObjectContractResponse subObjectContractResponse = new SubObjectContractResponse();
            subObjectContractResponse.setId(serviceContracts.getId());
           // subObjectContractResponse.setStatus(serviceContracts.getStatus());
            subObjectContractResponse.setContractNumber(serviceContracts.getContractNumber());
           // subObjectContractResponse.setContractStatus(serviceContracts.getContractStatus());
           // subObjectContractResponse.setStatusModifyDate(serviceContracts.getStatusModifyDate());
          //  subObjectContractResponse.setSubStatus(serviceContracts.getSubStatus());
            contractResponseList.add(subObjectContractResponse);
        }
        if (productContractOptional.isPresent()) {
            productContract = productContractOptional.get();
            SubObjectContractResponse subObjectContractResponse = new SubObjectContractResponse();
            subObjectContractResponse.setId(productContract.getId());
          //  subObjectContractResponse.setStatus(getStatus(productContract.getStatus()));
            subObjectContractResponse.setContractNumber(productContract.getContractNumber());
           // subObjectContractResponse.setContractStatus(getContractStatus(productContract.getContractStatus()));
           // subObjectContractResponse.setStatusModifyDate(productContract.getStatusModifyDate());
           // subObjectContractResponse.setSubStatus(getSubStatus(productContract.getSubStatus()));
            contractResponseList.add(subObjectContractResponse);
        }*/
        serviceParametersPreview.setContractResponseList(contractResponseList);
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

    private ServiceContractDetailsSubStatus getSubStatus(ContractDetailsSubStatus subStatus) {
        if (subStatus.equals(ContractDetailsSubStatus.DRAFT)) {
            return ServiceContractDetailsSubStatus.DRAFT;
        } else if (subStatus.equals(ContractDetailsSubStatus.READY)) {
            return ServiceContractDetailsSubStatus.READY;
        } else if (subStatus.equals(ContractDetailsSubStatus.SIGNED_BY_CUSTOMER)) {
            return ServiceContractDetailsSubStatus.SIGNED_BY_CUSTOMER;
        } else if (subStatus.equals(ContractDetailsSubStatus.SIGNED_BY_EPRES)) {
            return ServiceContractDetailsSubStatus.SIGNED_BY_EPRES;
        } else if (subStatus.equals(ContractDetailsSubStatus.IN_TERMINATION_BY_CUSTOMER)) {
            return ServiceContractDetailsSubStatus.IN_TERMINATION_BY_CUSTOMER;
        } else if (subStatus.equals(ContractDetailsSubStatus.IN_TERMINATION_BY_EPRES)) {
            return ServiceContractDetailsSubStatus.IN_TERMINATION_BY_EPRES;
        } else if (subStatus.equals(ContractDetailsSubStatus.IN_TERMINATION_BY_GO_DATA)) {
            return ServiceContractDetailsSubStatus.IN_TERMINATION_BY_GO_DATA;
        } else if (subStatus.equals(ContractDetailsSubStatus.DELIVERY)) {
            return ServiceContractDetailsSubStatus.DELIVERY;
        } else if (subStatus.equals(ContractDetailsSubStatus.FROM_CUSTOMER_WITH_NOTICE)) {
            return ServiceContractDetailsSubStatus.FROM_CUSTOMER_WITH_NOTICE;
        } else if (subStatus.equals(ContractDetailsSubStatus.FROM_CUSTOMER_WITHOUT_NOTICE)) {
            return ServiceContractDetailsSubStatus.FROM_CUSTOMER_WITHOUT_NOTICE;
        } else if (subStatus.equals(ContractDetailsSubStatus.FROM_EPRES_WITH_NOTICE)) {
            return ServiceContractDetailsSubStatus.FROM_EPRES_WITH_NOTICE;
        } else if (subStatus.equals(ContractDetailsSubStatus.FROM_EPRES_WITHOUT_NOTICE)) {
            return ServiceContractDetailsSubStatus.FROM_EPRES_WITHOUT_NOTICE;
        } else if (subStatus.equals(ContractDetailsSubStatus.BY_MUTUAL_AGREEMENT)) {
            return ServiceContractDetailsSubStatus.BY_MUTUAL_AGREEMENT;
        } else if (subStatus.equals(ContractDetailsSubStatus.NEW_CONTRACT_SIGNED)) {
            return ServiceContractDetailsSubStatus.NEW_CONTRACT_SIGNED;
        } else if (subStatus.equals(ContractDetailsSubStatus.EXPIRED)) {
            return ServiceContractDetailsSubStatus.EXPIRED;
        } else if (subStatus.equals(ContractDetailsSubStatus.DELETED_ID_DECEASED_PERSON)) {
            return ServiceContractDetailsSubStatus.DELETED_ID_DECEASED_PERSON;
        } else if (subStatus.equals(ContractDetailsSubStatus.FORCE_MAJEURE)) {
            return ServiceContractDetailsSubStatus.FORCE_MAJEURE;
        } else if (subStatus.equals(ContractDetailsSubStatus.UNSENT_TO_CUSTOMER)) {
            return ServiceContractDetailsSubStatus.UNSENT_TO_CUSTOMER;
        } else if (subStatus.equals(ContractDetailsSubStatus.INVALID_DATA)) {
            return ServiceContractDetailsSubStatus.INVALID_DATA;
        } else if (subStatus.equals(ContractDetailsSubStatus.REFUSAL_TO_SIGN_BY_CUSTOMER)) {
            return ServiceContractDetailsSubStatus.REFUSAL_TO_SIGN_BY_CUSTOMER;
        } else if (subStatus.equals(ContractDetailsSubStatus.REFUSAL_TO_SIGN_BY_EPRES)) {
            return ServiceContractDetailsSubStatus.REFUSAL_TO_SIGN_BY_EPRES;
        } else if (subStatus.equals(ContractDetailsSubStatus.TEST)) {
            return ServiceContractDetailsSubStatus.TEST;
        } else if (subStatus.equals(ContractDetailsSubStatus.CHANGED_WITH_AGREEMENT)) {
            return ServiceContractDetailsSubStatus.CHANGED_WITH_AGREEMENT;
        } else return null;
    }

    private ServiceContractDetailStatus getContractStatus(ContractDetailsStatus contractStatus) {
        if (contractStatus.equals(ContractDetailsStatus.DRAFT)) {
            return ServiceContractDetailStatus.DRAFT;
        } else if (contractStatus.equals(ContractDetailsStatus.READY)) {
            return ServiceContractDetailStatus.READY;
        } else if (contractStatus.equals(ContractDetailsStatus.SIGNED)) {
            return ServiceContractDetailStatus.SIGNED;
        } else if (contractStatus.equals(ContractDetailsStatus.ENTERED_INTO_FORCE)) {
            return ServiceContractDetailStatus.ENTERED_INTO_FORCE;
        } else if (contractStatus.equals(ContractDetailsStatus.ACTIVE_IN_TERM)) {
            return ServiceContractDetailStatus.ACTIVE_IN_TERM;
        } else if (contractStatus.equals(ContractDetailsStatus.ACTIVE_IN_PERPETUITY)) {
            return ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY;
        } else if (contractStatus.equals(ContractDetailsStatus.TERMINATED)) {
            return ServiceContractDetailStatus.DRAFT;
        } else if (contractStatus.equals(ContractDetailsStatus.CANCELLED)) {
            return ServiceContractDetailStatus.DRAFT;
        } else if (contractStatus.equals(ContractDetailsStatus.CHANGED_WITH_AGREEMENT)) {
            return ServiceContractDetailStatus.DRAFT;
        } else return null;

    }

    private EntityStatus getStatus(ProductContractStatus status) {
        if (status.equals(ProductContractStatus.ACTIVE)) {
            return EntityStatus.ACTIVE;
        }
        if (status.equals(ProductContractStatus.DELETED)) {
            return EntityStatus.DELETED;
        } else return null;
    }

    /*private List<PriceComponentFormula> convertToPriceComponentFormulaFromGroups(Long productDetailId) {

        if (productDetailId == null)
            return new ArrayList<>();
        List<PriceComponentFormula> result = new ArrayList<>();
        List<Long> priceComponentIds = serviceContractRepository.getPriceComponentFromProductPriceComponentGroups(productDetailId);
        for (Long pcId : priceComponentIds) {
            PriceComponentFormula priceComponentFormula = new PriceComponentFormula();
            priceComponentFormula.setPriceComponentId(pcId);
            List<PriceComponentFormulaVariables> variables = new ArrayList<>();
            List<PriceComponentFormulaVariable> formulaVariables = priceComponentFormulaVariableRepository.findAllByPriceComponentIdOrderByIdAsc(pcId);
            for (PriceComponentFormulaVariable v : formulaVariables) {
                PriceComponentFormulaVariables formulaVariable = new PriceComponentFormulaVariables();
                formulaVariable.setFormulaVariableId(v.getId());
                formulaVariable.setVariable(v.getVariable());
                formulaVariable.setValue(v.getValue());
                formulaVariable.setValueFrom(v.getValueFrom());
                formulaVariable.setValueTo(v.getValueTo());
                formulaVariable.setVariableDescription(v.getDescription());
                variables.add(formulaVariable);
            }
            priceComponentFormula.setVariables(variables);
            if (!variables.isEmpty())
                result.add(priceComponentFormula);
        }

        return result;
    }*/
    private List<ServiceContractPriceComponentFormula> convertToPriceComponentFormula(List<PriceComponent> priceComponents) {
        if (priceComponents == null) return new ArrayList<>();
        return priceComponents.stream().map(e -> {
            ServiceContractPriceComponentFormula priceComponentFormula = new ServiceContractPriceComponentFormula();
            priceComponentFormula.setPriceComponentId(e.getId());
            List<ServiceContractPriceComponentFormulaVariables> variables = new ArrayList<>();
            for (PriceComponentFormulaVariable v : e.getFormulaVariables()) {
                ServiceContractPriceComponentFormulaVariables formulaVariable = new ServiceContractPriceComponentFormulaVariables();
                formulaVariable.setFormulaVariableId(v.getId());
                formulaVariable.setVariable(v.getVariable());
                formulaVariable.setValue(v.getValue());
                formulaVariable.setValueFrom(v.getValueFrom());
                formulaVariable.setValueTo(v.getValueTo());
                formulaVariable.setVariableDescription(v.getDescription());
                formulaVariable.setDisplayName(v.getDescription() + " (" + v.getVariable() + " from " + e.getName() + ")");
                variables.add(formulaVariable);
            }
            priceComponentFormula.setVariables(variables);
            return priceComponentFormula;
        }).toList();
    }

    private List<ServiceContractInterimAdvancePaymentResponse> getFromGroupsIap(List<IapResponseFromNativeQuery> iaps) {
        if (iaps == null) return new ArrayList<>();
        List<ServiceContractInterimAdvancePaymentResponse> result = new ArrayList<>();
        for (IapResponseFromNativeQuery iap : iaps) {
            InterimAdvancePaymentTermsResponse interimAdvancePaymentTermsResponse = interimAdvancePaymentTermsService.findByInterimAdvancePaymentIdAndStatusIn(iap.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
            result.add(new ServiceContractInterimAdvancePaymentResponse(iap, interimAdvancePaymentTermsResponse));
        }
        return result;
    }

    private List<ServiceContractInterimAdvancePaymentResponse> getFromDirectIap(List<InterimAdvancePayment> iaps) {
        if (iaps == null) return new ArrayList<>();
        List<ServiceContractInterimAdvancePaymentResponse> result = new ArrayList<>();
        for (InterimAdvancePayment iap : iaps) {
            InterimAdvancePaymentTermsResponse interimAdvancePaymentTermsResponse = interimAdvancePaymentTermsService.findByInterimAdvancePaymentIdAndStatusInForServiceContract(iap.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
            result.add(new ServiceContractInterimAdvancePaymentResponse(iap, interimAdvancePaymentTermsResponse));
        }
        return result;
    }

    /*private SupplyActivationsForContractFields getSupplyActivationsForContractFields(Terms terms) {
        if (terms == null) return null;
        return new SupplyActivationsForContractFields(terms.getSupplyActivationExactDateStartDay(), terms.getSupplyActivations());
    }*/

    private ContractEntryIntoForceForContractFields getContractEntryIntoForceForContractFields(Terms terms) {
        return new ContractEntryIntoForceForContractFields(terms.getContractEntryIntoForceFromExactDayOfMonthStartDay(), terms.getContractEntryIntoForces());
    }

    private StartOfContractInitialTermsForContractFields getStartOfContractInitialTermsForContractFields(Terms terms) {
        return new StartOfContractInitialTermsForContractFields(terms.getStartDayOfInitialContractTerm(),terms.getFirstDayOfTheMonthOfInitialContractTerm(), terms.getStartsOfContractInitialTerms());
    }

    public ServiceParametersPreview thirdPagePreview(ServiceContracts serviceContracts, ServiceContractDetails details, ServiceDetails serviceInfo, String contractNumber) {
        ServiceParametersPreview serviceParametersPreview = fillPreviewResponse(details, serviceContracts);
        serviceParametersPreview.setEntryIntoForceValue(details.getEntryIntoForceValue());
        fillInvoicePaymentTermResponse(details, serviceParametersPreview);
        fillServiceContractResponse(details, serviceParametersPreview);
        fillFormulaVariables(details, serviceParametersPreview);
        serviceParametersPreview.setQuantity(details.getQuantity());
        serviceParametersPreview.setExecutionLevel(serviceInfo.getExecutionLevel());
        fillContractIaps(details, serviceParametersPreview);
        fillServiceContractServiceAdditionalParams(details, serviceParametersPreview);
        switch (serviceInfo.getExecutionLevel()) {
            case CONTRACT ->
                    getContractSubObjectDetails(serviceContracts, details, contractNumber, serviceParametersPreview);
            case POINT_OF_DELIVERY -> {
                getExistingPods(serviceContracts, details, serviceParametersPreview);
                getUnrecognizedPods(details, serviceParametersPreview);
            }
        }
        return serviceParametersPreview;
    }

    private void fillServiceContractServiceAdditionalParams(ServiceContractDetails details, ServiceParametersPreview serviceParametersPreview) {
        List<ServiceContractAdditionalParams> contractAdditionalParams = serviceContractAdditionalParamsRepository.findAllByContractDetailId(details.getId());

        List<ServiceContractAdditionalParamsResponse> paramsFromSer = serviceAdditionalParamsRepository
                .findServiceFilledAdditionalParamsByServiceDetailId(details.getServiceDetailId());

        List<ServiceContractServiceAdditionalParamsResponse> contractAdditionalParamsResp = new ArrayList<>();
        for (ServiceContractAdditionalParams fromContract : contractAdditionalParams) {
            Optional<ServiceContractAdditionalParamsResponse> fromServOptional = paramsFromSer
                    .stream()
                    .filter(prod ->
                            prod.id().equals(fromContract.getServiceAdditionalParamId())
                                    && prod.value() == null)
                    .findAny();
            if (fromServOptional.isPresent()) {
                contractAdditionalParamsResp.add(new ServiceContractServiceAdditionalParamsResponse(fromContract.getServiceAdditionalParamId(), fromContract.getValue()));
            }
        }
        serviceParametersPreview.setServiceContractServiceAdditionalParams(contractAdditionalParamsResp);
    }

    private void getUnrecognizedPods(ServiceContractDetails details, ServiceParametersPreview serviceParametersPreview) {
        List<ServiceUnrecognizedPods> unrecognizedPods = serviceUnrecognizedPodsRepository.findByContractDetailsIdAndStatus(details.getId(), ServiceSubobjectStatus.ACTIVE);
        List<SubObjectPodsResponse> unrecognizablePodsList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(unrecognizedPods)) {
            for (ServiceUnrecognizedPods item : unrecognizedPods) {
                SubObjectPodsResponse subObjectPodsResponse = new SubObjectPodsResponse();
                subObjectPodsResponse.setId(item.getId());
                subObjectPodsResponse.setPodName(item.getPodIdentifier());
                unrecognizablePodsList.add(subObjectPodsResponse);
            }
        }
        serviceParametersPreview.setUnrecognizablePodsList(unrecognizablePodsList);
    }

    private void getExistingPods(ServiceContracts serviceContracts, ServiceContractDetails details, ServiceParametersPreview serviceParametersPreview) {
        List<SubObjectPodsResponse> podsList = new ArrayList<>();
        List<ServiceContractPods> serviceContractPods = serviceContractPodsRepository.findByContractDetailIdAndStatus(details.getId(), ContractSubObjectStatus.ACTIVE);
        if (!serviceContractPods.isEmpty()) {
            for (ServiceContractPods item : serviceContractPods) {
                SubObjectPodsResponse subObjectPodsResponse = new SubObjectPodsResponse();
                PointOfDeliveryDetails podDetails = pointOfDeliveryDetailsRepository.findByPodId(item.getPodId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Can't find pod;"));
                subObjectPodsResponse.setId(item.getId());
                subObjectPodsResponse.setPodName(podDetails.getName());
                Optional<PointOfDelivery> pointOfDeliveryOptional = getPodIdentifier(podDetails.getPodId());
                if (pointOfDeliveryOptional.isPresent()) {
                    PointOfDelivery pointOfDelivery = pointOfDeliveryOptional.get();
                    subObjectPodsResponse.setPodId(pointOfDelivery.getId());
                    subObjectPodsResponse.setPodIdentifier(pointOfDelivery.getIdentifier());
                }
                podsList.add(subObjectPodsResponse);
            }
        }
        serviceParametersPreview.setPodsList(podsList);
    }

    private Optional<PointOfDelivery> getPodIdentifier(Long podId) {
        return pointOfDeliveryRepository.findById(podId);
    }

    private void fillContractIaps(ServiceContractDetails details, ServiceParametersPreview serviceParametersPreview) {
        Map<Long, ServiceContractInterimAdvancePayments> collect = contractInterimAdvancePaymentsRepository.findAllByContractDetailIdAndStatusIn(details.getId(), List.of(ContractSubObjectStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ServiceContractInterimAdvancePayments::getInterimAdvancePaymentId, x -> x));
        List<InterimAdvancePayment> iaps = advancePaymentRepository.findAllByIdIn(collect.keySet());
        List<ServiceContractIAPResponse> interimAdvancePayments = new ArrayList<>();
        for (InterimAdvancePayment iap : iaps) {
            ServiceContractInterimAdvancePayments contractInterimAdvancePayments = collect.get(iap.getId());
            ServiceContractIAPResponse response = new ServiceContractIAPResponse();
            response.setName(iap.getName());
            response.setContractIapId(contractInterimAdvancePayments.getId());
            response.setInterimAdvancePaymentId(iap.getId());
            response.setIssueDate(contractInterimAdvancePayments.getIssueDate());
            response.setValue(contractInterimAdvancePayments.getValue());
            response.setTermValue(contractInterimAdvancePayments.getTermValue());
            InterimAdvancePaymentTermsResponse termResponse = interimAdvancePaymentTermsService.findByInterimAdvancePaymentIdAndStatusIn(iap.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
            response.setIapTerms(termResponse);
            interimAdvancePayments.add(response);
            if (iap.getValueType().equals(ValueType.PRICE_COMPONENT)) {
                response.setFormulas(serviceContractInterimPriceFormulaRepository.getPriceComponentFormulaByContractIapIdAndStatus(contractInterimAdvancePayments.getId(), List.of(EntityStatus.ACTIVE)));
            }
        }
        serviceParametersPreview.setInterimAdvancePayments(interimAdvancePayments);
    }

    private void fillFormulaVariables(ServiceContractDetails details, ServiceParametersPreview serviceParametersPreview) {
        Map<Long, ServiceContractPriceComponents> collect = serviceContractPriceComponentsRepository.findByContractDetailIdAndStatusIn(details.getId(), List.of(ContractSubObjectStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ServiceContractPriceComponents::getPriceComponentFormulaVariableId, x -> x));
        List<ContractPriceComponentResponse> priceComponentResponses = new ArrayList<>();
        List<PriceComponentFormulaVariable> priceComponentFormulas = priceComponentFormulaVariableRepository.findAllByIdIn(collect.keySet());
        for (PriceComponentFormulaVariable priceComponentFormula : priceComponentFormulas) {
            ServiceContractPriceComponents contractPriceComponents = collect.get(priceComponentFormula.getId());
            ContractPriceComponentResponse response = new ContractPriceComponentResponse();
            response.setValue(contractPriceComponents.getValue());
            response.setFormulaVariableId(priceComponentFormula.getId());
            response.setVariableDescription(priceComponentFormula.getDescription());
            priceComponentResponses.add(response);
        }
        serviceParametersPreview.setPriceComponents(priceComponentResponses);
    }

    private void fillServiceContractResponse(ServiceContractDetails details, ServiceParametersPreview serviceParametersPreview) {
        ServiceContractTerm serviceContractTerms = serviceContractTermRepository.findById(details.getServiceContractTermId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service payment term not found!;"));
        serviceParametersPreview.setContractTerm(new ServiceContractTermsResponse(serviceContractTerms));
        serviceParametersPreview.setContractTermDate(details.getContractTermEndDate());
    }


    private void fillInvoicePaymentTermResponse(ServiceContractDetails details, ServiceParametersPreview thirdPagePreview) {
        Long invoicePaymentTermId = details.getInvoicePaymentTermId();
        if (invoicePaymentTermId != null) {
            InvoicePaymentTerms invoicePaymentTerms = invoicePaymentTermsRepository.findById(invoicePaymentTermId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Invoice not found!;"));
            Calendar calendar = calendarRepository.findByIdAndStatusIsIn(invoicePaymentTerms.getCalendarId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Calendar not found!"));
            thirdPagePreview.setInvoicePaymentTerm(new InvoicePaymentTermsResponse(invoicePaymentTerms, calendar));
            thirdPagePreview.setInvoicePaymentTermValue(details.getInvoicePaymentTermValue());
        }
    }

    private ServiceParametersPreview fillPreviewResponse(ServiceContractDetails details, ServiceContracts serviceContracts) {
        ServiceParametersPreview thirdPagePreview = new ServiceParametersPreview();
        thirdPagePreview.setContractType(details.getType());
        PaymentGuarantee paymentGuarantee = details.getPaymentGuarantee();
        thirdPagePreview.setPaymentGuarantee(paymentGuarantee);
        if (paymentGuarantee.equals(PaymentGuarantee.CASH_DEPOSIT) || paymentGuarantee.equals(PaymentGuarantee.CASH_DEPOSIT_AND_BANK)) {
            thirdPagePreview.setCashDeposit(details.getCashDepositAmount());
            Currency cashDepositCurrency = currencyRepository.findByIdAndStatus(details.getCashDepositCurrency(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Currency not found for cash deposit!;"));
            thirdPagePreview.setCashDepositCurrency(new CurrencyResponse(cashDepositCurrency));
        }
        if (paymentGuarantee.equals(PaymentGuarantee.BANK) || paymentGuarantee.equals(PaymentGuarantee.CASH_DEPOSIT_AND_BANK)) {
            thirdPagePreview.setBankGuarantee(details.getBankGuaranteeAmount());
            Currency bankGuarantee = currencyRepository.findByIdAndStatus(details.getBankGuaranteeCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Currency not found for Bank guarantee!;"));
            thirdPagePreview.setBankDepositCurrency(new CurrencyResponse(bankGuarantee));
        }
        thirdPagePreview.setGuaranteeInformation(details.getGuaranteeContractInfo());
        thirdPagePreview.setGuaranteeContract(details.getGuaranteeContract());
        thirdPagePreview.setEntryIntoForce(details.getEntryIntoForce());
        thirdPagePreview.setEntryIntoForceValue(serviceContracts.getEntryIntoForceDate());
        thirdPagePreview.setStartOfContractInitialTerm(details.getStartOfContractInitialTerm());
        thirdPagePreview.setStartOfContractInitialTermDate(details.getInitialTermStartValue());
        thirdPagePreview.setMonthlyInstallmentValue(details.getEqualMonthlyInstallmentNumber());
        thirdPagePreview.setMonthlyInstallmentAmount(details.getEqualMonthlyInstallmentAmount());
        return thirdPagePreview;
    }

    @Transactional
    public void update(ServiceContractThirdPageFields thirdPageFields, ServiceContractEditRequest request, ServiceContractDetails detailsUpdated, ServiceContracts serviceContract, ServiceDetails serviceDetails, List<String> errorMessages) {
        ServiceContractServiceParametersEditRequest serviceParameters = request.getServiceParameters();
        ServiceContractBasicParametersEditRequest basicParameters = request.getBasicParameters();
        validateAndSet(thirdPageFields, request, detailsUpdated, serviceContract, serviceParameters, basicParameters, serviceDetails, errorMessages);
        validateDates(serviceContract, errorMessages);
    }

    private void validateDates(ServiceContracts serviceContracts, List<String> messages) {
        validateEntryIntoForceDate(serviceContracts, messages);
        validateContractTermEndDate(serviceContracts, messages);
        validateContractTermStartDate(serviceContracts, messages);
    }

    private void validateContractTermStartDate(ServiceContracts serviceContracts, List<String> messages) {
        LocalDate signingDate = serviceContracts.getSigningDate();
        LocalDate startOfInitialTerm = serviceContracts.getContractInitialTermStartDate();
        LocalDate terminationDate = serviceContracts.getTerminationDate();
        LocalDate perpetuityDate = serviceContracts.getPerpetuityDate();
        if (startOfInitialTerm != null) {
            if (signingDate != null && startOfInitialTerm.isBefore(signingDate)) {
                messages.add("basicParameters.signInDate- Signing date should be after contract term start date!;");
            }

            if (terminationDate != null && startOfInitialTerm.isAfter(terminationDate)) {
                messages.add("basicParameters.terminationDate- terminationDate should be before contract term start date!;");
            }
            if (perpetuityDate != null && !startOfInitialTerm.isBefore(perpetuityDate)) {
                messages.add("basicParameters.signInDate- perpetuityDate should be before contract term start date!;");
            }

        }

    }

    private void validateEntryIntoForceDate(ServiceContracts serviceContracts, List<String> messages) {
        LocalDate signingDate = serviceContracts.getSigningDate();
        LocalDate entryIntoForceDate = serviceContracts.getEntryIntoForceDate();
        LocalDate contractTermEndDate = serviceContracts.getContractTermEndDate();
        LocalDate perpetuityDate = serviceContracts.getPerpetuityDate();
        LocalDate terminationDate = serviceContracts.getTerminationDate();
        LocalDate contractInitialTermStartDate = serviceContracts.getContractInitialTermStartDate();

        if (entryIntoForceDate != null) {
            if (signingDate != null && entryIntoForceDate.isBefore(signingDate)) {
                messages.add("basicParameters.entryIntoForceDate- Signing date can not be after Entry into force date!;");
            }
            if (contractTermEndDate != null && entryIntoForceDate.isAfter(contractTermEndDate)) {
                messages.add("basicParameters.entryIntoForceDate-entry into force date should be less or equal to contract term end date!;");
            }
            if (perpetuityDate != null && entryIntoForceDate.isAfter(perpetuityDate)) {
                messages.add("basicParameters.entryIntoForceDate-perpetuityDate date should be before or equal to contract term end date!;");
            }
            if (terminationDate != null && entryIntoForceDate.isAfter(terminationDate)) {
                messages.add("basicParameters.entryIntoForceDate-terminationDate date should be before or equal to contract term end date!;");
            }
            if (contractInitialTermStartDate != null) {
                if (!entryIntoForceDate.isEqual(contractInitialTermStartDate)) {
                    if (!entryIntoForceDate.isBefore(contractInitialTermStartDate)) {
                        messages.add("basicParameters.startOfInitialTerm- startOfInitialTerm should be after entryIntoForceDate!;");
                    }
                }
            }
        }

    }

    private void validateContractTermEndDate(ServiceContracts serviceContracts, List<String> messages) {
        LocalDate signingDate = serviceContracts.getSigningDate();
        LocalDate startOfInitialTerm = serviceContracts.getContractInitialTermStartDate();
        LocalDate contractTermEndDate = serviceContracts.getContractTermEndDate();
        LocalDate perpetuityDate = serviceContracts.getPerpetuityDate();
        if (contractTermEndDate != null) {
            if (signingDate != null && contractTermEndDate.isBefore(signingDate)) {
                messages.add("basicParameters.signInDate- Signing date should be less than contract term end date!;");
            }
//            if (startOfInitialTerm != null && !contractTermEndDate.isAfter(startOfInitialTerm)) {
//                messages.add("basicParameters.startOfTheInitialTermOfTheContract-contract term start date should be less than contract term end date!;");
//            }

            if (perpetuityDate != null && contractTermEndDate.isBefore(perpetuityDate)) {
                messages.add("basicParameters.perpetuityDate-Perpetuity date should be before or equal to contract term end date;");
            }
        }
    }

    private void fillContractDetails(ServiceContracts serviceContracts,
                                     ServiceDetails serviceDetails,
                                     ServiceContractDetails serviceContractDetails,
                                     ServiceContractServiceParametersCreateRequest request,
                                     ServiceContractThirdPageFields sourceView,
                                     LocalDate startOfTheInitialTermOfTheContract,
                                     List<String> errorMessages) {
        serviceContractDetails.setServiceContractTermId(request.getContractTermId());

        serviceContractDetails.setInvoicePaymentTermId(request.getInvoicePaymentTermId());
        serviceContractDetails.setInvoicePaymentTermValue(request.getInvoicePaymentTerm());
        serviceContractDetails.setPaymentGuarantee(request.getPaymentGuarantee());
        serviceContractDetails.setCashDepositAmount(request.getCashDepositAmount());
        serviceContractDetails.setCashDepositCurrency(request.getCashDepositCurrencyId());
        checkMonthlyInstallmentNumberAndAmount(serviceContractDetails, request.getMonthlyInstallmentAmount(), request.getMonthlyInstallmentNumber(), serviceDetails, errorMessages);
        serviceContractDetails.setBankGuaranteeAmount(request.getBankGuaranteeAmount());
        serviceContractDetails.setBankGuaranteeCurrencyId(request.getBankGuaranteeCurrencyId());
        serviceContractDetails.setGuaranteeContract(request.isGuaranteeContract());
        serviceContractDetails.setGuaranteeContractInfo(request.getGuaranteeContractInfo());
        serviceContractDetails.setInitialTermStartValue(request.getStartOfContractInitialTermDate());
        serviceContractDetails.setQuantity(getQuantityForPerPieceComponent(sourceView.getFormulaVariables(), serviceDetails.getId(), request.getQuantity(), errorMessages));

        serviceContractDetails.setEntryIntoForce(request.getEntryIntoForce());
        serviceContractDetails.setInvoicePaymentTermId(request.getInvoicePaymentTermId());
        serviceContractDetails.setInvoicePaymentTermValue(request.getInvoicePaymentTerm());
        serviceContractDetails.setStartOfContractInitialTerm(request.getStartOfContractInitialTerm());

    }

    private void checkMonthlyInstallmentNumberAndAmount(ServiceContractDetails serviceContractDetails, BigDecimal monthlyInstallmentAmount, Short monthlyInstallmentNumber, ServiceDetails serviceDetails, List<String> errorMessages) {
        if (serviceDetails.getEqualMonthlyInstallmentsActivation()) {
            if (Objects.isNull(monthlyInstallmentNumber)) {
                errorMessages.add("monthlyInstallmentNumber-Monthly Installment Number is mandatory while Service Monthly Installment is activated;");
            }

            if (Objects.isNull(monthlyInstallmentAmount)) {
                errorMessages.add("monthlyInstallmentAmount-Monthly Installment Amount is mandatory while Service Monthly Installment is activated;");
            }

            BigDecimal amountFrom = serviceDetails.getAmountFrom();
            BigDecimal amountTo = serviceDetails.getAmountTo();
            if (amountFrom != null && amountTo != null) {
                Range<BigDecimal> amountRange = Range.between(amountFrom, amountTo);
                if (!amountRange.contains(monthlyInstallmentAmount)) {
                    errorMessages.add("monthlyInstallmentAmount-Monthly Installment Amount must be in range defined in service: min [%s] and max [%s];".formatted(amountRange.getMinimum(), amountRange.getMaximum()));
                }
            } else if (amountFrom != null) {
                if (monthlyInstallmentAmount.compareTo(amountFrom) < 0) {
                    errorMessages.add("monthlyInstallmentAmountFrom-Monthly Installment Amount must be more than provided in service: [%s]".formatted(amountFrom));
                }
            } else if (amountTo != null) {
                if (monthlyInstallmentAmount.compareTo(amountTo) > 0) {
                    errorMessages.add("monthlyInstallmentAmountTo-Monthly Installment Amount must be less than provided in service: [%s]".formatted(amountTo));
                }
            }
            serviceContractDetails.setEqualMonthlyInstallmentAmount(monthlyInstallmentAmount);

            Short installmentNumberFrom = serviceDetails.getInstallmentNumberFrom();
            Short installmentNumberTo = serviceDetails.getInstallmentNumberTo();
            if (installmentNumberFrom != null && installmentNumberTo != null) {
                Range<Short> numberRange = Range.between(installmentNumberFrom, serviceDetails.getInstallmentNumberTo());
                if (!numberRange.contains(monthlyInstallmentNumber)) {
                    errorMessages.add("monthlyInstallmentNumber-Monthly Installment Number must be in range defined in service: min [%s] and max [%s];".formatted(numberRange.getMinimum(), numberRange.getMaximum()));
                }
            } else if (installmentNumberFrom != null) {
                if (monthlyInstallmentNumber.compareTo(installmentNumberFrom) < 0) {
                    errorMessages.add("monthlyInstallmentNumberFrom-Monthly Installment Number must be more than provided in service: [%s];".formatted(installmentNumberFrom));
                }
            } else if (installmentNumberTo != null) {
                if (monthlyInstallmentNumber.compareTo(installmentNumberTo) > 0) {
                    errorMessages.add("monthlyInstallmentNumberTo-Monthly Installment Number must be less than provided in service: [%s];".formatted(installmentNumberTo));
                }
            }
            serviceContractDetails.setEqualMonthlyInstallmentNumber(monthlyInstallmentNumber);
        } else {
            serviceContractDetails.setEqualMonthlyInstallmentAmount(monthlyInstallmentAmount);
            serviceContractDetails.setEqualMonthlyInstallmentNumber(monthlyInstallmentNumber);
        }
    }

    private void fillContractDetailsForEdit(ServiceContracts serviceContracts, ServiceContractEditRequest serviceContractCreateRequest, ServiceDetails serviceDetails, ServiceContractDetails serviceContractDetails, ServiceContractServiceParametersCreateRequest request, ServiceContractThirdPageFields sourceView, List<String> messages) {
        serviceContractDetails.setServiceContractTermId(request.getContractTermId());
        serviceContractDetails.setInvoicePaymentTermId(request.getInvoicePaymentTermId());
        serviceContractDetails.setInvoicePaymentTermValue(request.getInvoicePaymentTerm());
        serviceContractDetails.setPaymentGuarantee(request.getPaymentGuarantee());
        serviceContractDetails.setCashDepositAmount(request.getCashDepositAmount());
        serviceContractDetails.setCashDepositCurrency(request.getCashDepositCurrencyId());
        checkMonthlyInstallmentNumberAndAmount(serviceContractDetails, request.getMonthlyInstallmentAmount(), request.getMonthlyInstallmentNumber(), serviceDetails, messages);
        serviceContractDetails.setBankGuaranteeAmount(request.getBankGuaranteeAmount());
        serviceContractDetails.setBankGuaranteeCurrencyId(request.getBankGuaranteeCurrencyId());
        serviceContractDetails.setGuaranteeContract(request.isGuaranteeContract());
        serviceContractDetails.setGuaranteeContractInfo(request.getGuaranteeContractInfo());
        serviceContractDetails.setInitialTermStartValue(request.getStartOfContractInitialTermDate());
        serviceContractDetails.setEntryIntoForce(request.getEntryIntoForce());
//        serviceContractDetails.setQuantity(getQuantityForPerPieceComponent(sourceView.getFormulaVariables(), serviceDetails.getId(), request.getQuantity(), messages));
        serviceContractDetails.setQuantity(request.getQuantity() != null ? request.getQuantity().intValue() : null);

        serviceContractDetails.setEntryIntoForceValue(request.getEntryIntoForceDate());
        serviceContractDetails.setInvoicePaymentTermId(request.getInvoicePaymentTermId());
        serviceContractDetails.setInvoicePaymentTermValue(request.getInvoicePaymentTerm());
        serviceContractDetails.setStartOfContractInitialTerm(request.getStartOfContractInitialTerm());
    }

    private void validateAndSet(ServiceContractThirdPageFields sourceView,
                                ServiceContractEditRequest request,
                                ServiceContractDetails detailsUpdated,
                                ServiceContracts serviceContract,
                                ServiceContractServiceParametersEditRequest serviceParameters,
                                ServiceContractBasicParametersEditRequest basicParameters,
                                ServiceDetails serviceDetails,
                                List<String> errorMessages) {
        validatorService.validateEditRequest(request, serviceParameters, sourceView);
        fillContractDetailsForEdit(serviceContract, request, serviceDetails, detailsUpdated, serviceParameters, sourceView, errorMessages);

    }

    private void fillEntryIntoForce(ServiceContractServiceParametersCreateRequest serviceParameters,
                                    ServiceContracts serviceContract,
                                    ServiceContractDetails serviceContractDetails, List<String> messages) {
        ContractEntryIntoForce entryIntoForce = serviceParameters.getEntryIntoForce();
        serviceContractDetails.setEntryIntoForce(entryIntoForce);

        if (entryIntoForce.equals(ContractEntryIntoForce.EXACT_DAY)) {
            serviceContract.setEntryIntoForceDate(serviceParameters.getEntryIntoForceDate());
            serviceContractDetails.setEntryIntoForceValue(serviceParameters.getEntryIntoForceDate());
            if (serviceParameters.getEntryIntoForceDate() == null) {
                messages.add("serviceParameters.entryIntoForceDate-Entry into force date can not be null!;");
            } else if (!serviceParameters.getEntryIntoForceDate().isAfter(LocalDate.now())) {
                messages.add("serviceParameters.EntryIntoForceDate-Entry into force date should be in future!;");
            }
        }

    }

    //todo should be removed
    private void fillStartInitialTermOfTheContract(ServiceContractServiceParametersCreateRequest serviceParameters,
                                                   ServiceContractBasicParametersCreateRequest basicParameters,
                                                   ServiceContracts serviceContracts,
                                                   ServiceContractDetails serviceContract,
                                                   List<String> errorMessages) {
        StartOfContractInitialTerm startOfContractInitialTerm = serviceParameters.getStartOfContractInitialTerm();
        serviceContract.setStartOfContractInitialTerm(startOfContractInitialTerm);
        if (startOfContractInitialTerm.equals(StartOfContractInitialTerm.EXACT_DATE)) {
            serviceContracts.setContractInitialTermStartDate(serviceParameters.getStartOfContractInitialTermDate());
            serviceContract.setInitialTermStartValue(serviceParameters.getStartOfContractInitialTermDate());
            if (serviceParameters.getStartOfContractInitialTermDate() == null) {
                errorMessages.add("serviceParameters.startOfContractInitialTermDate-Start of contract term  date should be provided!;");
            }
        }
    }

    private void fillStartInitialTermOfTheContractForEdit(ServiceContractEditRequest request, ServiceContracts contract, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        StartOfContractInitialTerm startOfContractInitialTerm = request.getServiceParameters().getStartOfContractInitialTerm();
        serviceContractDetails.setStartOfContractInitialTerm(startOfContractInitialTerm);
        if (request.getBasicParameters().getStartOfTheInitialTermOfTheContract() != null) {
            contract.setContractInitialTermStartDate(request.getBasicParameters().getStartOfTheInitialTermOfTheContract());
        }

        if (Objects.nonNull(startOfContractInitialTerm)) {
            switch (startOfContractInitialTerm) {
                case SIGNING -> {
                    if (!Objects.nonNull(contract.getContractInitialTermStartDate()))
                        contract.setContractInitialTermStartDate(request.getBasicParameters().getSignInDate());
                }
                case EXACT_DATE -> {
                    //contract.setContractInitialTermStartDate(request.getServiceParameters().getStartOfContractInitialTermDate());
                    //serviceContractDetails.setInitialTermStartValue(request.getServiceParameters().getStartOfContractInitialTermDate());
                   /* if (Objects.nonNull(contract.getContractInitialTermStartDate())) {
                        contract.setContractInitialTermStartDate(request.getServiceParameters().getStartOfContractInitialTermDate());
                    }*/
                    if (contract.getContractInitialTermStartDate() == null && !request.getBasicParameters().getContractStatus().equals(ServiceContractDetailStatus.DRAFT)) {
                        contract.setContractInitialTermStartDate(request.getServiceParameters().getStartOfContractInitialTermDate());
                    }
                    serviceContractDetails.setInitialTermStartValue(request.getServiceParameters().getStartOfContractInitialTermDate());
                }
                case DATE_OF_CHANGE_OF_CBG, FIRST_DELIVERY -> {
                }
                case MANUAL ->
                        contract.setContractInitialTermStartDate(request.getBasicParameters().getStartOfTheInitialTermOfTheContract());
            }
        }

    }

    private void fillEntryIntoForceForEdit(ServiceContractEditRequest request, ServiceContracts serviceContract, ServiceContractDetails serviceContractDetails, List<String> messages) {
        ContractEntryIntoForce entryIntoForce = request.getServiceParameters().getEntryIntoForce();
        serviceContractDetails.setEntryIntoForce(entryIntoForce);
        if (request.getBasicParameters().getEntryIntoForceDate() != null) {
            serviceContract.setEntryIntoForceDate(request.getBasicParameters().getEntryIntoForceDate());
        }
        if (Objects.nonNull(entryIntoForce)) {
            switch (entryIntoForce) {
                case SIGNING -> serviceContract.setEntryIntoForceDate(request.getBasicParameters().getSignInDate());
                case EXACT_DAY -> {
                    if (serviceContract.getEntryIntoForceDate() == null && !request.getBasicParameters().getContractStatus().equals(ServiceContractDetailStatus.DRAFT)) {
                        serviceContract.setEntryIntoForceDate(request.getServiceParameters().getEntryIntoForceDate());
                    }
                    serviceContractDetails.setEntryIntoForceValue(request.getServiceParameters().getEntryIntoForceDate());
                }
                case DATE_CHANGE_OF_CBG, FIRST_DELIVERY -> {

                }
                case MANUAL ->
                        serviceContract.setEntryIntoForceDate(request.getBasicParameters().getEntryIntoForceDate());
            }
        }
    }

    public void fillSubActivityDetails(ServiceContractServiceParametersCreateRequest serviceParameters, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        saveFormulaVariables(serviceContractDetails, serviceParameters);
        saveInterimAdvancePayments(serviceContractDetails, serviceParameters, errorMessages);
    }

    private void saveInterimAdvancePayments(ServiceContractDetails serviceContractDetails, ServiceContractServiceParametersCreateRequest serviceParameters, List<String> errorMessages) {
        List<ServiceContractInterimAdvancePaymentsRequest> interimAdvancePayments = serviceParameters.getInterimAdvancePaymentsRequests();
        checkInterimAdvancePayments(serviceContractDetails.getServiceDetailId(), interimAdvancePayments.stream().map(ServiceContractInterimAdvancePaymentsRequest::getInterimAdvancePaymentId).toList(), serviceParameters, errorMessages);
        List<ServiceContractInterimPriceFormula> formulasToSave = new ArrayList<>();
        for (ServiceContractInterimAdvancePaymentsRequest interimAdvancePayment : interimAdvancePayments) {
            ServiceContractInterimAdvancePayments advancePayments = contractInterimAdvancePaymentsRepository.save(createContractIap(serviceContractDetails, interimAdvancePayment, errorMessages));
            createIapFormula(formulasToSave, interimAdvancePayment, advancePayments);
        }
        serviceContractInterimPriceFormulaRepository.saveAll(formulasToSave);
    }

    private void createIapFormula(List<ServiceContractInterimPriceFormula> formulasToSave, ServiceContractInterimAdvancePaymentsRequest interimAdvancePayment, ServiceContractInterimAdvancePayments advancePayments) {
        List<PriceComponentContractFormula> contractFormulas = interimAdvancePayment.getContractFormulas();
        if (!CollectionUtils.isEmpty(contractFormulas)) {
            for (PriceComponentContractFormula contractFormula : contractFormulas) {
                formulasToSave.add(new ServiceContractInterimPriceFormula(contractFormula.getValue(), contractFormula.getFormulaVariableId(), advancePayments.getId()));
            }
        }
    }

    private ServiceContractInterimAdvancePayments createContractIap(ServiceContractDetails productContractDetails, ServiceContractInterimAdvancePaymentsRequest interimAdvancePayment, List<String> errorMessages) {
        ServiceContractInterimAdvancePayments contractInterimAdvancePayments = new ServiceContractInterimAdvancePayments();
        Optional<InterimAdvancePayment> interimAdvancePaymentTermsOptional = advancePaymentRepository.findById(interimAdvancePayment.getInterimAdvancePaymentId());
        if (interimAdvancePaymentTermsOptional.isPresent()) {
            InterimAdvancePayment dbInterimAdvancePayment = interimAdvancePaymentTermsOptional.get();
            if (dbInterimAdvancePayment.getValueFrom() != null && dbInterimAdvancePayment.getValueTo() != null && interimAdvancePayment.getValue() != null) {
                if (compareIntToDecimal(interimAdvancePayment.getValue(), dbInterimAdvancePayment.getValueFrom()) && compareIntToDecimalForValueTo(interimAdvancePayment.getValue(), dbInterimAdvancePayment.getValueTo())) {
                    contractInterimAdvancePayments.setValue(interimAdvancePayment.getValue());
                } else {
                    errorMessages.add("serviceParameters.value-[value] wrong issue date value;");
                }
            }
            Optional<InterimAdvancePaymentTerms> PaymentTermsOptional = interimAdvancePaymentTermsRepository.findByInterimAdvancePaymentIdAndStatusInOrderByCreateDate(dbInterimAdvancePayment.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
            if (PaymentTermsOptional.isPresent()) {
                InterimAdvancePaymentTerms paymentTerms = PaymentTermsOptional.get();
                if (paymentTerms.getValueFrom() != null && paymentTerms.getValueTo() != null && interimAdvancePayment.getTermValue() != null) {
                    if (interimAdvancePayment.getTermValue() >= paymentTerms.getValueFrom() && interimAdvancePayment.getTermValue() <= paymentTerms.getValueTo()) {
                        contractInterimAdvancePayments.setTermValue(interimAdvancePayment.getTermValue());
                    } else {
                        errorMessages.add("serviceParameters.termValue-[termValue] wrong issue date value;");
                    }
                }
            }
        }

        contractInterimAdvancePayments.setStatus(ContractSubObjectStatus.ACTIVE);
        contractInterimAdvancePayments.setInterimAdvancePaymentId(interimAdvancePayment.getInterimAdvancePaymentId());
        contractInterimAdvancePayments.setContractDetailId(productContractDetails.getId());
        contractInterimAdvancePayments.setIssueDate(interimAdvancePayment.getIssueDate());
        contractInterimAdvancePayments.setValue(interimAdvancePayment.getValue());
        contractInterimAdvancePayments.setTermValue(interimAdvancePayment.getTermValue());
        return contractInterimAdvancePayments;
    }

    private boolean compareIntToDecimalForValueTo(BigDecimal value, BigDecimal valueTo) {
        return value.compareTo(valueTo) <= 0;
    }

    private boolean compareIntToDecimal(BigDecimal value, BigDecimal valueFrom) {
        return value.compareTo(valueFrom) >= 0;
    }

    private void saveFormulaVariables(ServiceContractDetails serviceContractDetails, ServiceContractServiceParametersCreateRequest serviceParameters) {
        List<PriceComponentContractFormula> contractFormulas = serviceParameters.getContractFormulas();
        List<ServiceContractPriceComponents> priceComponents = new ArrayList<>();
        for (PriceComponentContractFormula contractFormula : contractFormulas) {
            ServiceContractPriceComponents contractPriceComponents = createContractPriceComponent(serviceContractDetails, contractFormula);
            priceComponents.add(contractPriceComponents);
        }
        serviceContractPriceComponentsRepository.saveAll(priceComponents);
    }

    private ServiceContractPriceComponents createContractPriceComponent(ServiceContractDetails productContractDetails, PriceComponentContractFormula contractFormula) {
        ServiceContractPriceComponents contractPriceComponents = new ServiceContractPriceComponents();
        contractPriceComponents.setContractDetailId(productContractDetails.getId());
        contractPriceComponents.setValue(contractFormula.getValue());
        contractPriceComponents.setStatus(ContractSubObjectStatus.ACTIVE);
        contractPriceComponents.setPriceComponentFormulaVariableId(contractFormula.getFormulaVariableId());
        return contractPriceComponents;
    }

    public Boolean createForExpress(ExpressContractRequest request, ServiceContractDetails serviceContractDetails, ServiceContracts serviceContracts, List<String> errorMessages) {
        ExpressContractServiceParametersRequest serviceParametersRequest = request.getServiceParameters();
        ServiceDetails serviceDetail = getServiceDetail(serviceContractDetails.getServiceDetailId());
        ServiceContractThirdPageFields sourceView = thirdPageFields(serviceDetail);
        List<String> messages = new ArrayList<>();
        ServiceContractCreateRequest serviceContractCreateRequest = new ServiceContractCreateRequest();
        serviceContractCreateRequest.setServiceParameters(serviceParametersRequest);
        validateIssueDate(sourceView, serviceParametersRequest, messages);
        validatorService.validateCreateRequest(null, null, serviceParametersRequest, sourceView, errorMessages);
        validatorService.validateMonthlyInstallment(sourceView, messages, serviceParametersRequest);
        ServiceContractBasicParametersCreateRequest basicParameters = new ServiceContractBasicParametersCreateRequest();
        basicParameters.setSignInDate(request.getExpressContractParameters().getSigningDate());
        basicParameters.setContractStatus(ServiceContractDetailStatus.DRAFT);//in express contract there can't be other statuses

        fillEntryIntoForce(request.getServiceParameters(), serviceContracts, serviceContractDetails, errorMessages);
        fillStartInitialTermOfTheContract(request.getServiceParameters(), basicParameters, serviceContracts, serviceContractDetails, errorMessages);
        fillContractTermEndDate(request.getServiceParameters(), sourceView, serviceContracts, serviceContractDetails, errorMessages);
        fillContractDetails(serviceContracts,
                getServiceDetail(serviceContractDetails.getServiceDetailId()),
                serviceContractDetails,
                serviceParametersRequest,
                sourceView,
                null,
                errorMessages);
        return true;
    }

    private void validateIssueDate(ServiceContractThirdPageFields sourceView, ExpressContractServiceParametersRequest productParameters, List<String> messages) {
        Map<Long, ServiceContractInterimAdvancePaymentResponse> collected = sourceView.getInterimAdvancePayments()
                .stream()
                .collect(Collectors.toMap(ServiceContractInterimAdvancePaymentResponse::getId, j -> j));
        List<ServiceContractInterimAdvancePaymentsRequest> interimAdvancePayments = productParameters.getInterimAdvancePaymentsRequests();
        int index = 0;
        for (ServiceContractInterimAdvancePaymentsRequest interimAdvancePayment : interimAdvancePayments) {
            ServiceContractInterimAdvancePaymentResponse paymentResponse = collected.get(interimAdvancePayment.getInterimAdvancePaymentId());
            if (paymentResponse.getDateOfIssueType() == DateOfIssueType.WORKING_DAYS_AFTER_INVOICE_DATE &&
                    (paymentResponse.getDateOfIssueValueTo() != null || paymentResponse.getDateOfIssueValueFrom() != null
                            || paymentResponse.getDateOfIssueValue() == null)) {
                messages.add("productParameters.InterimAdvancePayments[%s]-product with interim and advance payment by id [%s] not available".formatted(index, paymentResponse.getId()));
            }
            index++;
        }
    }

    private ServiceDetails getServiceDetail(Long serviceDetailId) {
        Optional<ServiceDetails> serviceDetailsOptional = serviceDetailsRepository.findById(serviceDetailId);
        return serviceDetailsOptional.orElse(null);
    }

    public List<ServiceContractPriceComponentFormula> getFormulaVariables(ServiceDetails details) {
        List<ServiceContractPriceComponentFormula> formulaVariables = new ArrayList<>();
        formulaVariables.addAll(convertToPriceComponentFormula(details.getPriceComponents().stream().filter(pc -> pc.getStatus() == ServiceSubobjectStatus.ACTIVE).map(ServicePriceComponent::getPriceComponent).collect(Collectors.toList())));
        formulaVariables.addAll(new ArrayList<>());
        formulaVariables.addAll(convertToPriceComponentFormulaFromGroups(details.getId()));
        return formulaVariables;
    }

    public Integer getQuantityForPerPieceComponent(List<ServiceContractPriceComponentFormula> priceComponents, Long serviceDetailsId, BigDecimal quantity, List<String> errorMessages) {
        boolean perPieceComponentExists = checkPerPieceComponentExists(priceComponents, serviceDetailsId);

        if (perPieceComponentExists) {
            if (quantity != null) {
                return quantity.intValue();
            } else {
                errorMessages.add("serviceParameters.quantity-[quantity] is mandatory for per-piece components;");
                return null;
            }
        } else {
            if (quantity != null) {
                errorMessages.add("serviceParameters.quantity-[quantity] should be null when no per-piece component exists;");
            }
            return null;
        }
    }

    public boolean checkPerPieceComponentExists(List<ServiceContractPriceComponentFormula> priceComponents, Long serviceDetailsId) {
        for (ServiceContractPriceComponentFormula priceComponent : priceComponents) {
            ApplicationModelResponse view = applicationModelService.view(priceComponent.getPriceComponentId());

            if (view.getPerPieceResponse() != null)
                return true;
        }

        List<PriceComponentForContractResponse> priceComponentsFromGroups = serviceContractRepository.getPriceComponentFromServicePriceComponentCurrentAndFutureGroups(serviceDetailsId);
        for (PriceComponentForContractResponse priceComponent : priceComponentsFromGroups) {
            ApplicationModelResponse view = applicationModelService.view(priceComponent.getId());

            if (view.getPerPieceResponse() != null)
                return true;
        }

        return false;
    }
}
