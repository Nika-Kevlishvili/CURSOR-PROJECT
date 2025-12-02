package bg.energo.phoenix.service.product.service;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractAdditionalParams;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractInterimAdvancePayments;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractPriceComponents;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentTerms;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroup;
import bg.energo.phoenix.model.entity.product.service.ServiceAdditionalParams;
import bg.energo.phoenix.model.entity.product.service.ServiceContractTerm;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.term.terms.InvoicePaymentTerms;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentGroupStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.PaymentType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponentGroup.PriceComponentGroupStatus;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermPeriodType;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.enums.product.term.terms.*;
import bg.energo.phoenix.model.request.product.service.EditServiceRequest;
import bg.energo.phoenix.model.request.product.service.subObject.contractTerm.EditServiceContractTermRequest;
import bg.energo.phoenix.repository.contract.service.ServiceContractAdditionalParamsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractInterimAdvancePaymentsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractPriceComponentsRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentTermsRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentFormulaVariableRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.price.priceComponentGroup.PriceComponentGroupRepository;
import bg.energo.phoenix.repository.product.service.ServiceAdditionalParamsRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServiceContractTermRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServiceInterimAndAdvancePaymentGroupRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServicePriceComponentGroupRepository;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import bg.energo.phoenix.service.contract.newVersionEvent.serviceContract.ServiceContractCreateNewVersionEvent;
import bg.energo.phoenix.service.contract.newVersionEvent.serviceContract.ServiceContractCreateNewVersionEventPublisher;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ServiceRelatedContractUpdateService {
    private final TermsRepository termsRepository;
    private final PriceComponentRepository priceComponentRepository;
    private final PriceComponentGroupRepository priceComponentGroupRepository;
    private final ServiceContractTermRepository serviceContractTermRepository;
    private final InvoicePaymentTermsRepository invoicePaymentTermsRepository;
    private final InterimAdvancePaymentRepository interimAdvancePaymentRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final ServicePriceComponentGroupRepository servicePriceComponentGroupRepository;
    private final InterimAdvancePaymentTermsRepository interimAdvancePaymentTermsRepository;
    private final PriceComponentFormulaVariableRepository priceComponentFormulaVariableRepository;
    private final ServiceContractPriceComponentsRepository serviceContractPriceComponentsRepository;
    private final ServiceContractAdditionalParamsRepository serviceContractAdditionalParamsRepository;
    private final ServiceInterimAndAdvancePaymentGroupRepository serviceInterimAndAdvancePaymentGroupRepository;
    private final ServiceContractInterimAdvancePaymentsRepository serviceContractInterimAdvancePaymentsRepository;
    private final ServiceAdditionalParamsRepository serviceAdditionalParamsRepository;
    private final ServiceContractCreateNewVersionEventPublisher eventPublisher;


    /**
     * Updates the service contracts for a given service.
     *
     * @param serviceDetails                            The service details for which to update the contracts.
     * @param updatedServiceDetailsWithRelatedContracts A list of IDs of the updated service details with related contracts.
     * @param exceptionContext                          A list to collect any exception messages during the update.
     */
    public void updateServiceContracts(ServiceDetails serviceDetails,
                                       List<Long> updatedServiceDetailsWithRelatedContracts,
                                       List<String> exceptionContext) {
        List<String> relatedServiceContractsExceptionMessagesContext = new ArrayList<>();

        if (hasServiceDetailFixedParameters(serviceDetails, relatedServiceContractsExceptionMessagesContext)) {
            updateRelatedContracts(serviceDetails, updatedServiceDetailsWithRelatedContracts, relatedServiceContractsExceptionMessagesContext);
        }

        exceptionContext.addAll(relatedServiceContractsExceptionMessagesContext);
    }

    /**
     * Updates the related contracts for a service.
     *
     * @param serviceDetails                                  The service details for which to update the contracts.
     * @param updatedServiceDetailsWithRelatedContracts       A list of IDs of the updated service details with related contracts.
     * @param relatedServiceContractsExceptionMessagesContext A list to collect any exception messages during the update.
     */
    public void updateRelatedContracts(ServiceDetails serviceDetails,
                                       List<Long> updatedServiceDetailsWithRelatedContracts,
                                       List<String> relatedServiceContractsExceptionMessagesContext) {
        List<ServiceContractDetails> serviceContractDetails = serviceContractDetailsRepository
                .findAllActiveServiceContractByServiceDetailIds(updatedServiceDetailsWithRelatedContracts);
        Terms terms = fetchValidTerm(serviceDetails, relatedServiceContractsExceptionMessagesContext);
        if (terms == null) {
            return;
        }

        for (ServiceContractDetails serviceContractDetail : serviceContractDetails) {
            Boolean isContractCurrent = serviceContractDetailsRepository.isServiceContractCurrent(serviceContractDetail.getId(), serviceContractDetail.getContractId());
            if (isContractCurrent != null && isContractCurrent && !serviceContractDetail.getStartDate().equals(LocalDate.now()) ) {
                eventPublisher.publishServiceContractCreateNewVersionEvent(new ServiceContractCreateNewVersionEvent(serviceDetails, relatedServiceContractsExceptionMessagesContext,
                        terms,  serviceContractDetail.getContractId(),
                        serviceContractDetail.getVersionId(), serviceContractDetail.getCustomerDetailId()));
            } else {
                updateSpecificServiceContractDetail(serviceDetails, serviceContractDetail, terms, relatedServiceContractsExceptionMessagesContext);
            }
        }
    }

    private void findRedundantAdditionalParamsInContract(List<ServiceAdditionalParams> serviceAdditionalParams,
                                                         List<ServiceContractAdditionalParams> paramsFromContract) {

        if (CollectionUtils.isEmpty(paramsFromContract)) {
            return;
        }
        List<ServiceContractAdditionalParams> redundantParamsInContract = new ArrayList<>();

        List<Long> serviceAdditionalParamsIdsFromService = serviceAdditionalParams
                .stream()
                .map(ServiceAdditionalParams::getId)
                .toList();
        for (ServiceContractAdditionalParams paramFromContract : paramsFromContract) {
            if (!serviceAdditionalParamsIdsFromService.contains(paramFromContract.getServiceAdditionalParamId())) {
                redundantParamsInContract.add(paramFromContract);
            }
        }
        if (CollectionUtils.isNotEmpty(redundantParamsInContract)) {
            paramsFromContract.removeAll(redundantParamsInContract);
            serviceContractAdditionalParamsRepository.deleteAll(redundantParamsInContract);
        }
    }

    /**
     * Updates specific service contract detail with the provided information.
     *
     * @param serviceDetails                                  the new service details
     * @param serviceContractDetails                          the service contract details to update
     * @param terms                                           the new terms
     * @param relatedServiceContractsExceptionMessagesContext a list to store any related service contracts exception messages
     */
    public void updateSpecificServiceContractDetail(ServiceDetails serviceDetails,
                                                    ServiceContractDetails serviceContractDetails,
                                                    Terms terms,
                                                    List<String> relatedServiceContractsExceptionMessagesContext) {
        List<ServiceContractTerm> serviceContractTerms = serviceContractTermRepository
                .findByServiceDetailsIdAndStatusIn(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

        serviceContractDetails.setServiceDetailId(serviceDetails.getId());
        if (!serviceContractTerms.isEmpty()) {
            serviceContractDetails.setServiceContractTermId(serviceContractTerms.get(0).getId());
        }

        List<InvoicePaymentTerms> invoicePaymentTerms = invoicePaymentTermsRepository
                .findInvoicePaymentTermsByTermIdAndStatusIn(terms.getId(), List.of(PaymentTermStatus.ACTIVE));

        serviceContractDetails.setInvoicePaymentTermId(invoicePaymentTerms.get(0).getId());
        serviceContractDetails.setInvoicePaymentTermValue(invoicePaymentTerms.get(0).getValue());

        List<ServiceAdditionalParams> serviceAdditionalParams = serviceAdditionalParamsRepository
                .findServiceAdditionalParamsByServiceDetailId(serviceDetails.getId())
                .stream()
                .filter(serPar -> serPar.getLabel() != null)
                .toList();


        List<ServiceContractAdditionalParams> paramsFromContract = serviceContractAdditionalParamsRepository
                .findAllByContractDetailId(serviceDetails.getId());

        findRedundantAdditionalParamsInContract(serviceAdditionalParams, paramsFromContract);

        for (ServiceContractAdditionalParams contractParam : paramsFromContract) {
                            serviceAdditionalParams
                                    .stream()
                    .filter(prPar ->
                            prPar.getId().equals(contractParam.getServiceAdditionalParamId())
                                    && prPar.getValue() != null)
                    .findAny()
                    .ifPresent(filledPar -> contractParam.setValue(filledPar.getValue()));
            serviceContractAdditionalParamsRepository.save(contractParam);
        }

        serviceContractDetails.setPaymentGuarantee(serviceDetails.getPaymentGuarantees().get(0));

        serviceContractPriceComponentsRepository
                .findByContractDetailIdAndStatusIn(serviceContractDetails.getId(), List.of(ContractSubObjectStatus.ACTIVE))
                .forEach(scpc -> scpc.setStatus(ContractSubObjectStatus.DELETED));

        List<InterimAdvancePayment> validInterimAdvancePayments = new ArrayList<>(
                interimAdvancePaymentRepository
                        .findByServiceDetailIdAndStatusIn(serviceDetails.getId(), List.of(InterimAdvancePaymentStatus.ACTIVE))
        );

        serviceInterimAndAdvancePaymentGroupRepository
                .findAllByServiceDetailsIdAndStatusIn(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE))
                .forEach(siap -> {
                    Optional<InterimAdvancePayment> respectiveByInterimAdvancePaymentGroup = interimAdvancePaymentRepository
                            .findRespectiveByInterimAdvancePaymentGroupId(
                                    siap.getAdvancedPaymentGroup().getId(),
                                    List.of(AdvancedPaymentGroupStatus.ACTIVE),
                                    PageRequest.of(0, 1)
                            );
                    if (respectiveByInterimAdvancePaymentGroup.isEmpty()) {
                        relatedServiceContractsExceptionMessagesContext.add("Cannot find respective Interim Advance Payment while trying to determinate from Interim Advance Payment Group with id: [%s]".formatted(siap.getAdvancedPaymentGroup().getId()));
                    } else {
                        validInterimAdvancePayments.add(respectiveByInterimAdvancePaymentGroup.get());
                    }
                });

        serviceContractInterimAdvancePaymentsRepository
                .findAllByContractDetailIdAndStatusIn(serviceContractDetails.getId(), List.of(ContractSubObjectStatus.ACTIVE))
                .forEach(scap -> scap.setStatus(ContractSubObjectStatus.DELETED));

        serviceContractInterimAdvancePaymentsRepository
                .saveAll(
                        validInterimAdvancePayments
                                .stream()
                                .map(vap -> new ServiceContractInterimAdvancePayments(
                                        null,
                                        vap.getDateOfIssueValue(),
                                        vap.getValue(),
                                        vap.getId(),
                                        ContractSubObjectStatus.ACTIVE,
                                        serviceContractDetails.getId(),
                                        null
                                ))
                                .toList()
                );

        List<Long> validPriceComponents = new ArrayList<>(
                priceComponentRepository
                        .findActivePriceComponentByServiceDetailId(serviceDetails.getId())
        );

        for (InterimAdvancePayment iap : validInterimAdvancePayments) {
            PriceComponent priceComponent = iap.getPriceComponent();
            if (priceComponent != null) {
                validPriceComponents.add(priceComponent.getId());
            }
        }

        servicePriceComponentGroupRepository
                .findByServiceDetailsIdAndStatusIn(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE))
                .forEach(pcg -> {
                    Optional<PriceComponent> respectivePriceComponentByGroup = priceComponentRepository
                            .findRespectivePriceComponentByGroup(pcg.getPriceComponentGroup().getId(), PageRequest.of(0, 1));
                    if (respectivePriceComponentByGroup.isEmpty()) {
                        relatedServiceContractsExceptionMessagesContext.add("Cannot find respective Price Component while trying to determinate from Price Component Group with id: [%s]".formatted(pcg.getPriceComponentGroup().getId()));
                    } else {
                        validPriceComponents.add(respectivePriceComponentByGroup.get().getId());
                    }
                });

        List<PriceComponentFormulaVariable> priceComponentFormulaVariables = priceComponentFormulaVariableRepository
                .findAllByPriceComponentIdIn(validPriceComponents);

        serviceContractPriceComponentsRepository
                .saveAll(
                        priceComponentFormulaVariables
                                .stream()
                                .map(priceComponentFormulaVariable -> new ServiceContractPriceComponents(
                                        null,
                                        priceComponentFormulaVariable.getValue(),
                                        priceComponentFormulaVariable.getId(),
                                        ContractSubObjectStatus.ACTIVE,
                                        serviceContractDetails.getId()
                                ))
                                .toList()
                );

        serviceContractDetails.setEntryIntoForce(terms.getContractEntryIntoForces().get(0));
        serviceContractDetails.setEntryIntoForce(terms.getContractEntryIntoForces().get(0));
        serviceContractDetails.setStartOfContractInitialTerm(terms.getStartsOfContractInitialTerms().get(0));
        serviceContractDetails.setEqualMonthlyInstallmentAmount(serviceDetails.getAmount());
        serviceContractDetails.setEqualMonthlyInstallmentNumber(serviceDetails.getInstallmentNumber());

        Currency cashDepositCurrency = serviceDetails.getCashDepositCurrency();
        if (Objects.nonNull(cashDepositCurrency)) {
            serviceContractDetails.setCashDepositCurrency(cashDepositCurrency.getId());
            serviceContractDetails.setCashDepositAmount(serviceDetails.getCashDepositAmount());
        }

        Currency bankGuaranteeCurrency = serviceDetails.getBankGuaranteeCurrency();
        if (Objects.nonNull(bankGuaranteeCurrency)) {
            serviceContractDetails.setBankGuaranteeCurrencyId(bankGuaranteeCurrency.getId());
            serviceContractDetails.setBankGuaranteeAmount(serviceDetails.getBankGuaranteeAmount());
        }

        validateAdditionalParams(serviceDetails, relatedServiceContractsExceptionMessagesContext);
    }

    /**
     * Validates the additional parameters of an EditServiceRequest.
     *
     * @param request           The EditServiceRequest to validate.
     * @param exceptionMessages The list to collect any exception messages.
     */
    private void validateAdditionalParams(EditServiceRequest request,
                                          List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(request.getAdditionalSettings().getServiceAdditionalParams())) {
            request
                    .getAdditionalSettings()
                    .getServiceAdditionalParams()
                    .stream()
                    .filter(sai -> Objects.isNull(sai.value()))
                    .forEach(sai -> exceptionMessages.add("Cannot update related Service Contracts, Service Additional Information with ordering id: [%s] value is not defined;".formatted(sai.orderingId())));
        }
    }

    private Terms fetchValidTerm(ServiceDetails serviceDetails,
                                 List<String> relatedServiceContractsExceptionMessagesContext) {
        if (Objects.nonNull(serviceDetails.getTerms())) {
            return serviceDetails.getTerms();
        } else if (Objects.nonNull(serviceDetails.getTermsGroups())) {
            Optional<Terms> respectiveTerm = termsRepository
                    .findRespectiveTermsByTermsGroupId(serviceDetails.getTermsGroups().getId(), LocalDateTime.now(), PageRequest.of(0, 1));
            if (respectiveTerm.isEmpty()) {
                relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Terms not found;");
            } else {
                return respectiveTerm.get();
            }
        } else {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Terms not found;");
        }
        return null;
    }

    private void validateAdditionalParams(ServiceDetails serviceDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(serviceAdditionalParamsRepository
                .findServiceAdditionalParamsByServiceDetailId(serviceDetails.getId()))) {
            serviceAdditionalParamsRepository
                    .findServiceAdditionalParamsByServiceDetailId(serviceDetails.getId())
                    .stream()
                    .filter(sap -> Objects.isNull(sap.getValue()) && Objects.nonNull(sap.getLabel()))
                    .forEach(pai -> exceptionMessages.add("Cannot update related Service Contracts, service Additional Information with ordering id: [%s] value is not defined;".formatted(pai.getOrderingId())));
        }
    }

    public boolean hasServiceDetailFixedParameters(ServiceDetails serviceDetails,
                                                   List<String> relatedServiceContractsExceptionMessagesContext) {
        Terms terms = defineValidTerm(serviceDetails, relatedServiceContractsExceptionMessagesContext);

        validateStatus(serviceDetails.getStatus(), relatedServiceContractsExceptionMessagesContext);
        validateAvailableForSale(LocalDateTime.now(), serviceDetails.getAvailableForSale(), serviceDetails.getAvailableFrom(), serviceDetails.getAvailableTo(), relatedServiceContractsExceptionMessagesContext);
        validateIndividualService(serviceDetails.getService().getCustomerIdentifier(), relatedServiceContractsExceptionMessagesContext);
        validateContractTerms(serviceDetails.getId(), relatedServiceContractsExceptionMessagesContext);

        Currency cashDepositCurrency = serviceDetails.getCashDepositCurrency();
        Currency bankGuaranteeCurrency = serviceDetails.getBankGuaranteeCurrency();
        validatePaymentGuarantees(serviceDetails.getPaymentGuarantees(), serviceDetails.getCashDepositAmount(), cashDepositCurrency == null ? null : cashDepositCurrency.getId(), serviceDetails.getBankGuaranteeAmount(), bankGuaranteeCurrency == null ? null : bankGuaranteeCurrency.getId(), relatedServiceContractsExceptionMessagesContext);

        if (relatedServiceContractsExceptionMessagesContext.isEmpty()) {
            validateInvoicePaymentTerms(terms, relatedServiceContractsExceptionMessagesContext);
            validateEnteringIntoForce(terms, relatedServiceContractsExceptionMessagesContext);
            validateStartOfInitialTerm(terms, relatedServiceContractsExceptionMessagesContext);
            validateSupplyActivation(terms, relatedServiceContractsExceptionMessagesContext);
            validateEqualMonthlyInstallments(serviceDetails.getEqualMonthlyInstallmentsActivation(), serviceDetails.getInstallmentNumber(), serviceDetails.getAmount(), relatedServiceContractsExceptionMessagesContext);
        }

        List<InterimAdvancePayment> validInterimAdvancePayments = new ArrayList<>(interimAdvancePaymentRepository
                .findByServiceDetailIdAndStatusIn(serviceDetails.getId(), List.of(InterimAdvancePaymentStatus.ACTIVE)));
        serviceInterimAndAdvancePaymentGroupRepository
                .findAllByServiceDetailsIdAndStatusIn(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE))
                .forEach(siap -> {
                    Optional<InterimAdvancePayment> respectiveByInterimAdvancePaymentGroup = interimAdvancePaymentRepository
                            .findRespectiveByInterimAdvancePaymentGroupId(
                                    siap.getAdvancedPaymentGroup().getId(),
                                    List.of(AdvancedPaymentGroupStatus.ACTIVE),
                                    PageRequest.of(0, 1)
                            );
                    if (respectiveByInterimAdvancePaymentGroup.isEmpty()) {
                        relatedServiceContractsExceptionMessagesContext.add("Cannot find respective Interim Advance Payment while trying to determinate from Interim Advance Payment Group with id: [%s]".formatted(siap.getAdvancedPaymentGroup().getId()));
                    } else {
                        validInterimAdvancePayments.add(respectiveByInterimAdvancePaymentGroup.get());
                    }
                });

        validateInterimAdvancePayments(validInterimAdvancePayments, relatedServiceContractsExceptionMessagesContext);

        List<Long> validPriceComponentIds = new ArrayList<>(priceComponentRepository
                .findActivePriceComponentByServiceDetailId(serviceDetails.getId()));
        servicePriceComponentGroupRepository
                .findByServiceDetailsIdAndStatusIn(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE))
                .forEach(spc -> {
                    Optional<PriceComponent> respectivePriceComponentByGroup = priceComponentRepository
                            .findRespectivePriceComponentByGroup(spc.getPriceComponentGroup().getId(), PageRequest.of(0, 1));
                    if (respectivePriceComponentByGroup.isEmpty()) {
                        relatedServiceContractsExceptionMessagesContext.add("Cannot find respective Price Component while trying to determinate from Price Component Group with id: [%s]".formatted(spc.getPriceComponentGroup().getId()));
                    } else {
                        validPriceComponentIds.add(respectivePriceComponentByGroup.get().getId());
                    }
                });
        validInterimAdvancePayments
                .stream()
                .map(InterimAdvancePayment::getPriceComponent)
                .filter(Objects::nonNull)
                .map(PriceComponent::getId)
                .forEach(validPriceComponentIds::add);

        validatePriceComponents(validPriceComponentIds, relatedServiceContractsExceptionMessagesContext);

        return relatedServiceContractsExceptionMessagesContext.isEmpty();
    }

    public boolean hasServiceDetailFixedParameters(EditServiceRequest serviceRequest,
                                                   List<String> relatedServiceContractsExceptionMessagesContext) {
        Terms terms = defineValidTerm(serviceRequest, relatedServiceContractsExceptionMessagesContext);

        validateStatus(serviceRequest.getBasicSettings().getServiceDetailStatus(), relatedServiceContractsExceptionMessagesContext);
        validateAvailableForSale(LocalDateTime.now(), serviceRequest.getBasicSettings().getAvailableForSale(), serviceRequest.getBasicSettings().getAvailableFrom(), serviceRequest.getBasicSettings().getAvailableTo(), relatedServiceContractsExceptionMessagesContext);
        validateIndividualService(serviceRequest.getBasicSettings().getCustomerIdentifier(), relatedServiceContractsExceptionMessagesContext);
        validateContractTermsOnEdit(serviceRequest.getContractTerms(), relatedServiceContractsExceptionMessagesContext);

        validatePaymentGuarantees(serviceRequest.getBasicSettings().getPaymentGuarantees().stream().toList(), serviceRequest.getBasicSettings().getCashDepositAmount(), serviceRequest.getBasicSettings().getCashDepositCurrencyId(), serviceRequest.getBasicSettings().getBankGuaranteeAmount(), serviceRequest.getBasicSettings().getBankGuaranteeCurrencyId(), relatedServiceContractsExceptionMessagesContext);

        if (relatedServiceContractsExceptionMessagesContext.isEmpty()) {
            validateInvoicePaymentTerms(terms, relatedServiceContractsExceptionMessagesContext);
            validateEnteringIntoForce(terms, relatedServiceContractsExceptionMessagesContext);
            validateStartOfInitialTerm(terms, relatedServiceContractsExceptionMessagesContext);
            validateSupplyActivation(terms, relatedServiceContractsExceptionMessagesContext);
            validateEqualMonthlyInstallments(serviceRequest.getPriceSettings().getEqualMonthlyInstallmentsActivation(), serviceRequest.getPriceSettings().getInstallmentNumber(), serviceRequest.getPriceSettings().getAmount(), relatedServiceContractsExceptionMessagesContext);
        }

        List<InterimAdvancePayment> interimAdvancePayments = validateInterimAdvancePayments(serviceRequest, relatedServiceContractsExceptionMessagesContext);
        validatePriceComponents(serviceRequest, interimAdvancePayments, relatedServiceContractsExceptionMessagesContext);
        validateAdditionalParams(serviceRequest, relatedServiceContractsExceptionMessagesContext);

        return relatedServiceContractsExceptionMessagesContext.isEmpty();
    }

    private List<InterimAdvancePayment> validateInterimAdvancePayments(EditServiceRequest request,
                                                                       List<String> relatedServiceContractsExceptionMessagesContext) {
        List<InterimAdvancePayment> validInterimAdvancePaymentContext = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(request.getInterimAdvancePayments())) {
            request
                    .getInterimAdvancePayments()
                    .forEach(iapId -> {
                        Optional<InterimAdvancePayment> interimAdvancePaymentOptional = interimAdvancePaymentRepository
                                .findByIdAndStatus(iapId, InterimAdvancePaymentStatus.ACTIVE);

                        if (interimAdvancePaymentOptional.isEmpty()) {
                            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Interim Advance Payment with presented id: [%s] not found;".formatted(iapId));
                        } else {
                            validInterimAdvancePaymentContext.add(interimAdvancePaymentOptional.get());
                        }
                    });
        }

        if (CollectionUtils.isNotEmpty(request.getInterimAdvancePaymentGroups())) {
            request
                    .getInterimAdvancePaymentGroups()
                    .forEach(iapGroupId -> {
                        Optional<InterimAdvancePayment> respectiveInterimAdvancePaymentOptional = interimAdvancePaymentRepository
                                .findRespectiveByInterimAdvancePaymentGroupId(iapGroupId, List.of(AdvancedPaymentGroupStatus.ACTIVE), PageRequest.of(0, 1));

                        if (respectiveInterimAdvancePaymentOptional.isEmpty()) {
                            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Respective Interim Advance Payment Group for presented group id: [%s] not found;".formatted(iapGroupId));
                        } else {
                            validInterimAdvancePaymentContext.add(respectiveInterimAdvancePaymentOptional.get());
                        }
                    });
        }

        validInterimAdvancePaymentContext
                .forEach(iap -> {
                    if (Objects.equals(iap.getPaymentType(), PaymentType.AT_LEAST_ONE)) {
                        relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Interim Advance Payment with id: [%s] has Payment Type [AT_LEAST_ONE];".formatted(iap.getId()));
                    }

                    if (iap.getValueType() != null) {
                        switch (iap.getValueType()) {
                            case PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT, EXACT_AMOUNT -> {
                                if (Objects.isNull(iap.getValue())) {
                                    relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Interim Advance Payment with id: [%s] value has not defined;".formatted(iap.getId()));
                                }
                            }
                        }
                    }

                    if (iap.getDateOfIssueType() != null) {
                        switch (iap.getDateOfIssueType()) {
                            case DATE_OF_THE_MONTH, WORKING_DAYS_AFTER_INVOICE_DATE -> {
                                if (Objects.isNull(iap.getDateOfIssueValue())) {
                                    relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Interim Advance Payment with id: [%s] date of issue value has not defined;".formatted(iap.getId()));
                                }
                            }
                        }
                    }

                    Optional<InterimAdvancePaymentTerms> interimAdvancePaymentTermsOptional = interimAdvancePaymentTermsRepository
                            .findByInterimAdvancePaymentIdAndStatusInOrderByCreateDate(iap.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
                    if (interimAdvancePaymentTermsOptional.isEmpty()) {
                        if (!iap.getMatchTermOfStandardInvoice()) {
                            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Interim Advance Payment with id: [%s] payment term is not defined;".formatted(iap.getId()));
                        }
                    } else {
                        InterimAdvancePaymentTerms interimAdvancePaymentTerms = interimAdvancePaymentTermsOptional.get();
                        if (Objects.isNull(interimAdvancePaymentTerms.getValue())) {
                            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Interim Advance Payment with id: [%s] payment term value is not defined;".formatted(iap.getId()));
                        }
                    }
                });

        return validInterimAdvancePaymentContext;
    }

    private void validatePriceComponents(EditServiceRequest request,
                                         List<InterimAdvancePayment> interimAdvancePayments,
                                         List<String> relatedServiceContractsExceptionMessagesContext) {
        List<Long> validPriceComponentsContext = new ArrayList<>();

        interimAdvancePayments
                .stream()
                .map(InterimAdvancePayment::getPriceComponent)
                .filter(Objects::nonNull)
                .map(PriceComponent::getId)
                .forEach(validPriceComponentsContext::add);

        if (CollectionUtils.isNotEmpty(request.getPriceComponents())) {
            request
                    .getPriceComponents()
                    .forEach(pcId -> {
                        Optional<PriceComponent> priceComponentOptional = priceComponentRepository
                                .findByIdAndStatusIn(pcId, List.of(PriceComponentStatus.ACTIVE));

                        if (priceComponentOptional.isEmpty()) {
                            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Price Component with presented id: [%s] not found;".formatted(pcId));
                        } else {
                            validPriceComponentsContext.add(pcId);
                        }
                    });
        }

        if (CollectionUtils.isNotEmpty(request.getPriceComponentGroups())) {
            request
                    .getPriceComponentGroups()
                    .forEach(pcgId -> {
                        Optional<PriceComponentGroup> priceComponentGroupOptional = priceComponentGroupRepository
                                .findByIdAndStatusIn(pcgId, List.of(PriceComponentGroupStatus.ACTIVE));
                        if (priceComponentGroupOptional.isEmpty()) {
                            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Price Component Group with presented id: [%s] not found;".formatted(pcgId));
                        } else {
                            Optional<Long> respectivePriceComponentByGroupId = priceComponentRepository
                                    .findRespectivePriceComponentByGroupId(priceComponentGroupOptional.get().getId(), PageRequest.of(0, 1));
                            if (respectivePriceComponentByGroupId.isEmpty()) {
                                relatedServiceContractsExceptionMessagesContext.add("Cannot found respective Price Component while trying to determinate from Price Component Group with id: [%s]".formatted(pcgId));
                            } else {
                                validPriceComponentsContext.add(respectivePriceComponentByGroupId.get());
                            }
                        }
                    });
        }

        List<PriceComponentFormulaVariable> priceComponentFormulaVariables = priceComponentFormulaVariableRepository
                .findAllByPriceComponentIdIn(validPriceComponentsContext);

        priceComponentFormulaVariables
                .stream()
                .filter(pcf -> Objects.isNull(pcf.getValue()))
                .forEach(pcf -> relatedServiceContractsExceptionMessagesContext.add("Value in Price Component with id: [%s-%s] is not defined;".formatted(pcf.getPriceComponent().getId(), pcf.getPriceComponent().getName())));
    }

    private void validateInterimAdvancePayments(List<InterimAdvancePayment> interimAdvancePayments,
                                                List<String> relatedServiceContractsExceptionMessagesContext) {
        if (interimAdvancePayments
                .stream()
                .anyMatch(interimAdvancePayment -> Objects.equals(interimAdvancePayment.getPaymentType(), PaymentType.AT_LEAST_ONE))
        ) {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, interim advance payment with payment type [AT_LEAST_ONE] is selected;");
        } else if (interimAdvancePayments.stream().anyMatch(interimAdvancePayment -> Objects.isNull(interimAdvancePayment.getPaymentType()))) {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, interim advance payment without payment type is selected;");
        } else {
            interimAdvancePayments
                    .forEach(interimAdvancePayment -> {
                        if (Objects.equals(interimAdvancePayment.getPaymentType(), PaymentType.AT_LEAST_ONE)) {
                            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Interim Advance Payment with id: [%s] has Payment Type [AT_LEAST_ONE];".formatted(interimAdvancePayment.getId()));
                        }

                        if (interimAdvancePayment.getValueType() != null) {
                            switch (interimAdvancePayment.getValueType()) {
                                case PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT, EXACT_AMOUNT -> {
                                    if (Objects.isNull(interimAdvancePayment.getValue())) {
                                        relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Interim Advance Payment with id: [%s] value has not defined;".formatted(interimAdvancePayment.getId()));
                                    }
                                }
                            }
                        }

                        if (interimAdvancePayment.getDateOfIssueType() != null) {
                            switch (interimAdvancePayment.getDateOfIssueType()) {
                                case DATE_OF_THE_MONTH, WORKING_DAYS_AFTER_INVOICE_DATE -> {
                                    if (Objects.isNull(interimAdvancePayment.getDateOfIssueValue())) {
                                        relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Interim Advance Payment with id: [%s] date of issue value has not defined;".formatted(interimAdvancePayment.getId()));
                                    }
                                }
                            }
                        }

                        Optional<InterimAdvancePaymentTerms> interimAdvancePaymentTermsOptional = interimAdvancePaymentTermsRepository
                                .findByInterimAdvancePaymentIdAndStatusInOrderByCreateDate(interimAdvancePayment.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
                        if (interimAdvancePaymentTermsOptional.isEmpty()) {
                            if (!interimAdvancePayment.getMatchTermOfStandardInvoice()) {
                                relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Interim Advance Payment with id: [%s] payment term is not defined;".formatted(interimAdvancePayment.getId()));
                            }
                        } else {
                            InterimAdvancePaymentTerms interimAdvancePaymentTerms = interimAdvancePaymentTermsOptional.get();
                            if (Objects.isNull(interimAdvancePaymentTerms.getValue())) {
                                relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, Interim Advance Payment with id: [%s] payment term value is not defined;".formatted(interimAdvancePayment.getId()));
                            }
                        }
                    });
        }
    }

    private void validatePriceComponents(List<Long> priceComponentIds,
                                         List<String> relatedServiceContractsExceptionMessagesContext) {
        List<PriceComponentFormulaVariable> priceComponentFormulaVariables = priceComponentFormulaVariableRepository
                .findAllByPriceComponentIdIn(priceComponentIds);

        priceComponentFormulaVariables
                .stream()
                .filter(priceComponentFormulaVariable -> Objects.isNull(priceComponentFormulaVariable.getValue()))
                .forEach(priceComponentFormulaVariable -> relatedServiceContractsExceptionMessagesContext.add("Value in Price Component with id: [%s] is not defined;"));
    }

    private void validateEqualMonthlyInstallments(Boolean equalMonthlyInstallmentsActivation,
                                                  Short installmentNumber,
                                                  BigDecimal amount,
                                                  List<String> relatedServiceContractsExceptionMessagesContext) {
        if (Boolean.TRUE.equals(equalMonthlyInstallmentsActivation)) {
            if (Objects.isNull(installmentNumber)) {
                relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service equal monthly installments number should be defined;");
            }

            if (Objects.isNull(amount)) {
                relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service equal monthly installments amount should be defined;");
            }
        }
    }

    private void validateSupplyActivation(Terms terms,
                                          List<String> relatedServiceContractsExceptionMessagesContext) {
        List<SupplyActivation> supplyActivations = terms.getSupplyActivations();
        if (supplyActivations.size() != 1) {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service term supply activations must be fixed for single option only;");
        } else {
            SupplyActivation supplyActivation = supplyActivations.get(0);

            switch (supplyActivation) {
                case FIRST_DAY_OF_MONTH -> {
                    List<WaitForOldContractTermToExpire> waitForOldContractTermToExpires = terms.getWaitForOldContractTermToExpires();
                    if (waitForOldContractTermToExpires.size() != 1) {
                        relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service term supply activation wait for old contract term to expire must be fixed for single option only;");
                    }
                }
                case EXACT_DATE ->
                        relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service term supply activations must be [FIRST_DAY_OF_MONTH, MANUAL];");
            }
        }
    }

    private void validateStartOfInitialTerm(Terms terms, List<String> relatedServiceContractsExceptionMessagesContext) {
        List<StartOfContractInitialTerm> startsOfContractInitialTerms = terms.getStartsOfContractInitialTerms();
        if (startsOfContractInitialTerms.size() != 1) {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service term start of initial terms must be fixed for single option only;");
        } else {
            StartOfContractInitialTerm startOfContractInitialTerm = startsOfContractInitialTerms.get(0);

            switch (startOfContractInitialTerm) {
                case EXACT_DATE, MANUAL ->
                        relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service term start of initial terms type must be [SIGNING, DATE_OF_CHANGE_OF_CBG, FIRST_DELIVERY];");
            }
        }
    }

    private void validateEnteringIntoForce(Terms terms,
                                           List<String> relatedServiceContractsExceptionMessagesContext) {
        List<ContractEntryIntoForce> contractEntryIntoForces = terms.getContractEntryIntoForces();
        if (contractEntryIntoForces.size() != 1) {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service term entering into force must be fixed for single option only;");
        } else {
            ContractEntryIntoForce contractEntryIntoForce = contractEntryIntoForces.get(0);
            switch (contractEntryIntoForce) {
                case EXACT_DAY, MANUAL ->
                        relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service term entry into force type must be [SIGNING, DATE_OF_CHANGE_OF_CBG, FIRST_DELIVERY];");
            }
        }
    }

    private void validatePaymentGuarantees(List<PaymentGuarantee> paymentGuarantees,
                                           BigDecimal cashDepositAmount,
                                           Long cashDepositCurrencyId,
                                           BigDecimal bankGuaranteeAmount,
                                           Long bankGuaranteeCurrencyId,
                                           List<String> relatedServiceContractsExceptionMessagesContext) {
        if (paymentGuarantees.size() != 1) {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service payment guarantees must be fixed for single option only;");
        } else {
            PaymentGuarantee paymentGuarantee = paymentGuarantees.get(0);

            switch (paymentGuarantee) {
                case CASH_DEPOSIT -> {
                    if (Objects.isNull(cashDepositAmount)) {
                        relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service cash deposit amount should be defined while payment guarantee is defined;");
                    }

                    if (Objects.isNull(cashDepositCurrencyId)) {
                        relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service cash deposit currency should be defined while payment guarantee is defined;");
                    }
                }
                case BANK -> {
                    if (Objects.isNull(bankGuaranteeCurrencyId)) {
                        relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service bank guarantee currency should be defined while payment guarantee is defined;");
                    }

                    if (Objects.isNull(bankGuaranteeAmount)) {
                        relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service bank guarantee amount should be defined while payment guarantee is defined;");
                    }
                }
                case CASH_DEPOSIT_AND_BANK -> {
                    if (Stream.of(bankGuaranteeCurrencyId, cashDepositCurrencyId).anyMatch(Objects::isNull)) {
                        relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service cash deposit and bank guarantee currency should be defined while payment guarantee is defined;");
                    }

                    if (Stream.of(bankGuaranteeAmount, cashDepositAmount).anyMatch(Objects::isNull)) {
                        relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service cash deposit and bank guarantee amount should be defined while payment guarantee is defined;");
                    }
                }
            }
        }
    }

    private Terms defineValidTerm(ServiceDetails serviceDetails,
                                  List<String> relatedServiceContractsExceptionMessagesContext) {
        if (Objects.nonNull(serviceDetails.getTerms())) {
            return serviceDetails.getTerms();
        } else if (Objects.nonNull(serviceDetails.getTermsGroups())) {
            return termsRepository
                    .findRespectiveTermsByTermsGroupId(
                            serviceDetails.getTermsGroups().getId(),
                            LocalDateTime.now(),
                            PageRequest.of(0, 1)
                    )
                    .orElseGet(Terms::new);
        } else {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update Service Contracts, terms/terms group is not defined;");
            return new Terms();
        }
    }

    private Terms defineValidTerm(EditServiceRequest request,
                                  List<String> relatedServiceContractsExceptionMessagesContext) {
        if (Objects.nonNull(request.getTerm())) {
            return termsRepository
                    .findById(request.getTerm())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Term not found;"));
        } else if (Objects.nonNull(request.getTermGroup())) {
            return termsRepository
                    .findRespectiveTermsByTermsGroupId(
                            request.getTermGroup(),
                            LocalDateTime.now(),
                            PageRequest.of(0, 1)
                    )
                    .orElseThrow(() -> new DomainEntityNotFoundException("Term not found"));
        } else {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update Service Contracts, terms/terms group is not defined;");
            return new Terms();
        }
    }

    private void validateInvoicePaymentTerms(Terms terms,
                                             List<String> relatedServiceContractsExceptionMessagesContext) {
        List<InvoicePaymentTerms> activeInvoicePaymentTerms = invoicePaymentTermsRepository
                .findInvoicePaymentTermsByTermIdAndStatusIn(terms.getId(), List.of(PaymentTermStatus.ACTIVE));

        if (activeInvoicePaymentTerms.size() != 1) {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service terms invoice payment terms must be fixed for single option only;");
        } else {
            InvoicePaymentTerms invoicePaymentTerms = activeInvoicePaymentTerms.get(0);

            if (Objects.isNull(invoicePaymentTerms.getValue())) {
                relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service terms invoice payment terms value should be defined;");
            }
        }
    }

    private void validateContractTerms(Long serviceDetailId,
                                       List<String> relatedServiceContractsExceptionMessagesContext) {
        List<ServiceContractTerm> contractTerms = serviceContractTermRepository
                .findByServiceDetailsIdAndStatusIn(serviceDetailId, List.of(ServiceSubobjectStatus.ACTIVE));
        if (contractTerms.size() != 1) {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service contract terms must be fixed for single option only;");
        } else {
            ServiceContractTerm serviceContractTerm = contractTerms.get(0);

            if (Objects.equals(serviceContractTerm.getPeriodType(), ServiceContractTermPeriodType.CERTAIN_DATE)) {
                relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service contract term type must be [PERIOD, WITHOUT_TERM, OTHER];");
            }
        }
    }

    private void validateContractTermsOnEdit(List<EditServiceContractTermRequest> contractTerms,
                                             List<String> relatedServiceContractsExceptionMessagesContext) {
        if (contractTerms.size() != 1) {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service contract terms must be fixed for single option only;");
        } else {
            EditServiceContractTermRequest editServiceContractTermRequest = contractTerms.get(0);

            if (Objects.equals(editServiceContractTermRequest.getPeriodType(), ServiceContractTermPeriodType.CERTAIN_DATE)) {
                relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service contract term type must be [PERIOD, WITHOUT_TERM, OTHER];");
            }
        }
    }

    private void validateIndividualService(String customerIdentifier,
                                           List<String> relatedServiceContractsExceptionMessagesContext) {
        if (StringUtils.isNotBlank(customerIdentifier)) {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service is individual;");
        }
    }

    private void validateAvailableForSale(LocalDateTime now,
                                          Boolean availableForSale,
                                          LocalDateTime availableFrom,
                                          LocalDateTime availableTo,
                                          List<String> relatedServiceContractsExceptionMessagesContext) {
        if (!Boolean.TRUE.equals(availableForSale)) {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service is not available for sale;");
        } else {
            if (Objects.nonNull(availableFrom) && (availableFrom.isAfter(now))) {
                relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service available from is defined in future;");
            }
            if (Objects.nonNull(availableTo) && (availableTo.isBefore(now))) {
                relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service available to is defined in past;");
            }
        }
    }

    private void validateStatus(ServiceDetailStatus status,
                                List<String> relatedServiceContractsExceptionMessagesContext) {
        if (!ServiceDetailStatus.ACTIVE.equals(status)) {
            relatedServiceContractsExceptionMessagesContext.add("Cannot update related Service Contracts, current Service is not [ACTIVE];");
        }
    }
}
