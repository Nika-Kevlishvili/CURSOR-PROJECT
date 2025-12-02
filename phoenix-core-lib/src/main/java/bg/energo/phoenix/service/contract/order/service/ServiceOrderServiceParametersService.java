package bg.energo.phoenix.service.contract.order.service;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractPriceComponentFormula;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderPriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.service.ServiceContractTerm;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServicePriceComponent;
import bg.energo.phoenix.model.entity.product.term.terms.InvoicePaymentTerms;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceExecutionLevel;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.enums.product.term.terms.PaymentTermStatus;
import bg.energo.phoenix.model.request.contract.order.service.ServiceOrderFormulaVariableRequest;
import bg.energo.phoenix.model.request.contract.order.service.ServiceOrderServiceParametersRequest;
import bg.energo.phoenix.model.request.contract.order.service.ServiceOrderUpdateRequest;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderFormulaVariableResponse;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderLinkedContractShortResponse;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderServiceParametersFields;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderServiceParametersResponse;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.PriceComponentFormula;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.PriceComponentFormulaVariables;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractTermsResponse;
import bg.energo.phoenix.model.response.service.ServiceContractTermShortResponse;
import bg.energo.phoenix.model.response.terms.InvoicePaymentTermsResponse;
import bg.energo.phoenix.repository.contract.order.service.*;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentFormulaVariableRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServiceContractTermRepository;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import bg.energo.phoenix.service.contract.service.ServiceContractServiceParametersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOrderServiceParametersService {
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final ServiceOrderPriceComponentRepository serviceOrderPriceComponentRepository;
    private final PriceComponentFormulaVariableRepository priceComponentFormulaVariableRepository;
    private final InvoicePaymentTermsRepository invoicePaymentTermsRepository;
    private final TermsRepository termsRepository;
    private final PriceComponentRepository priceComponentRepository;
    private final ServiceOrderLinkedProductContractRepository serviceOrderLinkedProductContractRepository;
    private final ServiceOrderLinkedServiceContractRepository serviceOrderLinkedServiceContractRepository;
    private final ServiceOrderPodRepository serviceOrderPodRepository;
    private final ServiceOrderUnrecognizedPodRepository serviceOrderUnrecognizedPodRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceOrderLinkedObjectsService serviceOrderLinkedObjectsService;
    private final ServiceContractTermRepository serviceContractTermRepository;
    private final ServiceContractServiceParametersService serviceContractServiceParametersService;


    /**
     * Validates the service parameters.
     *
     * @param request         The request containing the service parameters.
     * @param serviceDetailId The ID of the service detail.
     * @param errorMessages   The list of error messages to be populated if any validation fails.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void validateServiceParameters(ServiceOrderServiceParametersRequest request, Long serviceDetailId, List<String> errorMessages) {
        log.debug("Validating service parameters: {}", request);
        ServiceOrderServiceParametersFields serviceParametersFields = getServiceParametersFields(serviceDetailId);
        validateInvoicePaymentTerm(serviceParametersFields, request, errorMessages);
        validateFormulaVariables(serviceParametersFields, request, errorMessages);
        validateSubObjectsAccordingToServiceExecutionLevel(request, serviceDetailId, errorMessages);
        validateQuantity(request, serviceDetailId, errorMessages);
    }


    /**
     * Validates selected invoice payment term compatibility dynamically according to the source fields.
     *
     * @param serviceParametersFields The source fields for cross validation.
     * @param request                 The request containing the service parameters.
     * @param errorMessages           The list of error messages to be populated if any validation fails.
     */
    private void validateInvoicePaymentTerm(ServiceOrderServiceParametersFields serviceParametersFields,
                                            ServiceOrderServiceParametersRequest request,
                                            List<String> errorMessages) {
        log.debug("Validating invoice payment term: {}", request);

        List<InvoicePaymentTermsResponse> invoicePaymentTermsList = serviceParametersFields
                .getInvoicePaymentTerms();
        if(!CollectionUtils.isEmpty(invoicePaymentTermsList)){
            Map<Long, InvoicePaymentTermsResponse> invoicePaymentTerms = invoicePaymentTermsList
                    .stream()
                    .collect(Collectors.toMap(InvoicePaymentTermsResponse::getId, o -> o));

            InvoicePaymentTermsResponse paymentTerm = invoicePaymentTerms.get(request.getInvoicePaymentTermId());
            if (paymentTerm == null) {
                errorMessages.add("serviceParameters.invoicePaymentTermValue-Selected invoice payment term is not valid;");
                return;
            }

            Integer selectedValue = request.getInvoicePaymentTermValue();

            if (selectedValue == null) {
                errorMessages.add("serviceParameters.invoicePaymentTermValue-Invoice payment term value should be provided;");
                return;
            }

            Integer value = paymentTerm.getValue();
            Integer valueFrom = paymentTerm.getValueFrom();
            Integer valueTo = paymentTerm.getValueTo();

            if (value != null && valueFrom == null && valueTo == null && !Objects.equals(selectedValue, value)) {
                errorMessages.add("serviceParameters.invoicePaymentTermValue-Invoice payment term value does not match;");
                return;
            }

            if (valueFrom != null && selectedValue < valueFrom) {
                errorMessages.add("serviceParameters.invoicePaymentTermValue-Invoice payment term value fails range validation;");
            }

            if (valueTo != null && selectedValue > valueTo) {
                errorMessages.add("serviceParameters.invoicePaymentTermValue-Invoice payment term value fails range validation;");
            }
        }

    }


    /**
     * Validates the formula variables according to the source fields.
     *
     * @param serviceParametersFields The source fields for cross validation.
     * @param request                 The request containing the service parameters.
     * @param errorMessages           The list of error messages to be populated if any validation fails.
     */
    private void validateFormulaVariables(ServiceOrderServiceParametersFields serviceParametersFields,
                                          ServiceOrderServiceParametersRequest request,
                                          List<String> errorMessages) {
        log.debug("Validating formula variables: {}", request);

        Map<Long, PriceComponentFormulaVariables> formulaVariables = serviceParametersFields
                .getFormulaVariables()
                .stream()
                .flatMap(v -> v.getVariables().stream())
                .collect(Collectors.toMap(PriceComponentFormulaVariables::getFormulaVariableId, o -> o));

        if (!formulaVariables.isEmpty()) {
            if (CollectionUtils.isEmpty(request.getFormulaVariables())) {
                log.error("Service has price component formula variables that are required;");
                errorMessages.add("Service has price component formula variables that are required;");
                return;
            } else if (formulaVariables.size() != request.getFormulaVariables().size()) {
                log.error("All price component variables, required and submitted ones, should match;");
                errorMessages.add("All price component variables, required and submitted ones, should match;");
                return;
            }
        } else {
            if (CollectionUtils.isNotEmpty(request.getFormulaVariables())) {
                log.error("Service has no required price component formula variables;");
                errorMessages.add("Service has no required price component formula variables;");
            }
            return;
        }

        List<Long> submittedFormulaVariableIds = request
                .getFormulaVariables()
                .stream()
                .map(ServiceOrderFormulaVariableRequest::getFormulaVariableId)
                .toList();

        if (!formulaVariables.keySet().containsAll(submittedFormulaVariableIds)) {
            log.error("All price component variables, required and submitted ones, should match;");
            errorMessages.add("All price component variables, required and submitted ones, should match;");
            return;
        }

        for (int i = 0; i < request.getFormulaVariables().size(); i++) {
            ServiceOrderFormulaVariableRequest formulaVariableRequest = request.getFormulaVariables().get(i);
            BigDecimal selectedValue = formulaVariableRequest.getValue();
            if (!formulaVariables.containsKey(formulaVariableRequest.getFormulaVariableId())) {
                errorMessages.add("serviceParameters.formulaVariables[%s].formulaVariableId-Formula variable with value %s is not valid;".formatted(i, selectedValue));
                continue;
            }

            PriceComponentFormulaVariables formulaVariable = formulaVariables.get(formulaVariableRequest.getFormulaVariableId());
            if (formulaVariable.getValueFrom() == null && formulaVariable.getValueTo() == null
                    && formulaVariable.getValue() != null && !Objects.equals(formulaVariable.getValue(), selectedValue)) {
                errorMessages.add("serviceParameters.formulaVariables[%s].value-Formula variable value does not match;".formatted(i));
                continue;
            }

            if (formulaVariable.getValueFrom() != null && selectedValue.compareTo(formulaVariable.getValueFrom()) < 0) {
                errorMessages.add("serviceParameters.formulaVariables[%s].value-Formula variable value fails range validation;".formatted(i));
            }

            if (formulaVariable.getValueTo() != null && selectedValue.compareTo(formulaVariable.getValueTo()) > 0) {
                errorMessages.add("serviceParameters.formulaVariables[%s].value-Formula variable value fails range validation;".formatted(i));
            }
        }
    }


    /**
     * Validates the sub objects according to the service execution level.
     * If the execution level is CUSTOMER, the request must contain nothing.
     * If the execution level is CONTRACT, the request must contain contract.
     * If the execution level is POINT_OF_DELIVERY, the request must contain either PODs or unrecognized PODs.
     *
     * @param serviceDetailId The ID of the service detail.
     */
    private void validateSubObjectsAccordingToServiceExecutionLevel(ServiceOrderServiceParametersRequest request,
                                                                    Long serviceDetailId,
                                                                    List<String> errorMessages) {
        log.debug("Validating sub objects according to service execution level: {}", request);

        ServiceDetails serviceDetail = serviceDetailsRepository
                .findById(serviceDetailId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Service detail not found with ID %s;".formatted(serviceDetailId)));

        ServiceExecutionLevel executionLevel = serviceDetail.getExecutionLevel();
        if (executionLevel.equals(ServiceExecutionLevel.CUSTOMER)) {
            if (CollectionUtils.isNotEmpty(request.getLinkedContracts())
                    || CollectionUtils.isNotEmpty(request.getPods())
                    || CollectionUtils.isNotEmpty(request.getUnrecognizedPods())) {
                errorMessages.add("Linked sub objects are not allowed when service execution level is %s;".formatted(executionLevel));
            }
        } else if (executionLevel.equals(ServiceExecutionLevel.CONTRACT)) {
            if (CollectionUtils.isNotEmpty(request.getPods()) || CollectionUtils.isNotEmpty(request.getUnrecognizedPods())) {
                errorMessages.add("Linked pods/unrecognized pods must be empty when service execution level is %s;".formatted(executionLevel));
            }

            if (CollectionUtils.isEmpty(request.getLinkedContracts()) && CollectionUtils.isEmpty(request.getLinkedContracts())) {
                errorMessages.add("Linked contracts must not be empty when service execution level is %s;".formatted(executionLevel));
            }
        } else if (executionLevel.equals(ServiceExecutionLevel.POINT_OF_DELIVERY)) {
            if (CollectionUtils.isNotEmpty(request.getLinkedContracts())) {
                errorMessages.add("Linked contracts must be empty when service execution level is %s;".formatted(executionLevel));
            }

            if (CollectionUtils.isEmpty(request.getPods()) && CollectionUtils.isEmpty(request.getUnrecognizedPods())) {
                errorMessages.add("Linked pods/unrecognized pods must not be empty when service execution level is %s;".formatted(executionLevel));
            }
        }
    }


    /**
     * Returns the service parameters response for the given service order.
     *
     * @param serviceOrder The service order entity.
     * @return The service parameters' response.
     */
    public ServiceOrderServiceParametersResponse getServiceParametersResponse(ServiceOrder serviceOrder) {
        log.debug("Getting service parameters response for service order with ID {}", serviceOrder.getId());

        ServiceOrderServiceParametersResponse response = new ServiceOrderServiceParametersResponse();
        response.setServiceParametersFields(getServiceParametersFields(serviceOrder.getServiceDetailId()));

        setInvoicePaymentTermsResponse(serviceOrder, response);
        response.setFormulaVariables(getFormulaVariablesResponse(serviceOrder));
        response.setQuantity(serviceOrder.getQuantity());
        response.setContractTermCertainDateValue(serviceOrder.getContractTermCertainDateValue());

        List<ServiceOrderLinkedContractShortResponse> linkedProductContracts = serviceOrderLinkedProductContractRepository.findByServiceOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));
        List<ServiceOrderLinkedContractShortResponse> linkedServiceContracts = serviceOrderLinkedServiceContractRepository.findByServiceOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));
        List<ServiceOrderLinkedContractShortResponse> linkedContracts = new ArrayList<>(CollectionUtils.union(linkedProductContracts, linkedServiceContracts));
        linkedContracts.sort(Comparator.comparing(ServiceOrderLinkedContractShortResponse::getCreateDate));
        response.setLinkedContracts(linkedContracts);

        response.setPods(serviceOrderPodRepository.findByServiceOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE)));
        response.setUnrecognizedPods(serviceOrderUnrecognizedPodRepository.findByServiceOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE)));
        if(serviceOrder.getServiceContractTermId() != null){
            ServiceContractTerm serviceContractTerms = serviceContractTermRepository.findById(serviceOrder.getServiceContractTermId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Service contract term not found!;"));
            response.setContractTerm(new ServiceContractTermsResponse(serviceContractTerms));

        }
        return response;
    }


    /**
     * If the service order has an invoice payment term values present, it means that at the moment of creation/update,
     * terms was attached to the service detail. In that case, the invoice payment term values are taken from the terms
     * (and will be error on further update if the terms is no longer valid for the service detail). If the values not present,
     * it means that at the moment of creation/update, terms group was attached to the service detail. In that case, the invoice
     * payment term values should be tried to dynamically be fetched from the service detail's current terms group's current invoice payment term.
     * If that fails (due to change in service and term group replaced by terms), then null will be returned.
     *
     * @param serviceOrder {@link ServiceOrder} entity
     * @param response     {@link ServiceOrderServiceParametersResponse} response
     */
    private void setInvoicePaymentTermsResponse(ServiceOrder serviceOrder, ServiceOrderServiceParametersResponse response) {
        if (serviceOrder.getInvoicePaymentTermId() != null) {
            InvoicePaymentTerms selectedInvoicePaymentTerm = invoicePaymentTermsRepository
                    .findById(serviceOrder.getInvoicePaymentTermId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Invoice payment term not found with ID %s".formatted(serviceOrder.getInvoicePaymentTermId())));

            response.setInvoicePaymentTermId(selectedInvoicePaymentTerm.getId());
            response.setInvoicePaymentTermName(selectedInvoicePaymentTerm.getName());
            response.setInvoicePaymentTermValue(serviceOrder.getInvoicePaymentTermValue());
        } else {
            ServiceDetails serviceDetails = serviceDetailsRepository
                    .findById(serviceOrder.getServiceDetailId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Service detail not found with ID %s".formatted(serviceOrder.getServiceDetailId())));

            if (serviceDetails.getTermsGroups() == null) {
                response.setInvoicePaymentTermId(null);
                response.setInvoicePaymentTermName(null);
                response.setInvoicePaymentTermValue(null);
            } else {
                Long currentGroupTermId = termsRepository.getTermIdFromCurrentTermGroup(serviceDetails.getTermsGroups().getId());
                Optional<List<InvoicePaymentTerms>> currentGroupInvoicePaymentTerms = invoicePaymentTermsRepository
                        .findByTermIdAndStatusIn(currentGroupTermId, List.of(PaymentTermStatus.ACTIVE));

                if (currentGroupInvoicePaymentTerms.isPresent()
                        && !currentGroupInvoicePaymentTerms.get().isEmpty()
                        && currentGroupInvoicePaymentTerms.get().size() == 1) {
                    // there should always be only 1 invoice payment term for a group of term
                    InvoicePaymentTerms invoicePaymentTerm = currentGroupInvoicePaymentTerms.get().get(0);
                    response.setInvoicePaymentTermId(invoicePaymentTerm.getId());
                    response.setInvoicePaymentTermName(invoicePaymentTerm.getName());
                    response.setInvoicePaymentTermValue(invoicePaymentTerm.getValue());
                }
            }
        }
    }


    /**
     * The persisted formula variable IDs should all belong to price components directly attached to the service detail,
     * according to the creation/edit service order business logic. So, except for the formula variables persisted in db,
     * we should search for any formula variables belonging to price components attached to the service detail from a group's
     * current version and return the values dynamically.
     *
     * @param serviceOrder {@link ServiceOrder} entity.
     * @return list of formula variables connected to the given service order
     */
    private List<ServiceOrderFormulaVariableResponse> getFormulaVariablesResponse(ServiceOrder serviceOrder) {
        List<ServiceOrderFormulaVariableResponse> respList = new ArrayList<>();

        // fetch groups' formula variables
        List<PriceComponent> pcGroupCurrentVersionPriceComponents = priceComponentRepository
                .findByIdIn(priceComponentRepository.getPriceComponentsFromCurrentServicePriceComponentGroup(serviceOrder.getServiceDetailId()));

        if (CollectionUtils.isNotEmpty(pcGroupCurrentVersionPriceComponents)) {
            List<PriceComponentFormulaVariable> belongingToPriceComponentGroups = pcGroupCurrentVersionPriceComponents
                    .stream()
                    .flatMap(pc -> pc.getFormulaVariables().stream())
                    .toList();

            for (PriceComponentFormulaVariable variable : belongingToPriceComponentGroups) {
                ServiceOrderFormulaVariableResponse resp = new ServiceOrderFormulaVariableResponse();
                resp.setFormulaVariableId(variable.getId());
                resp.setValue(variable.getValue()); // set value from the pc group's current version's record
                resp.setVariableDescription(variable.getDescription());
                respList.add(resp);
            }
        }

        // fetch directly attached price components' formula variables
        Map<Long, ServiceOrderPriceComponent> belongingToPriceComponents = serviceOrderPriceComponentRepository
                .findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE))
                .stream()
                .collect(Collectors.toMap(ServiceOrderPriceComponent::getPriceComponentFormulaVariableId, o -> o));

        List<PriceComponentFormulaVariable> formulaVariables = priceComponentFormulaVariableRepository.findAllByIdIn(belongingToPriceComponents.keySet());
        for (PriceComponentFormulaVariable variable : formulaVariables) {
            ServiceOrderPriceComponent orderPriceComponent = belongingToPriceComponents.get(variable.getId());
            ServiceOrderFormulaVariableResponse resp = new ServiceOrderFormulaVariableResponse();
            resp.setFormulaVariableId(orderPriceComponent.getPriceComponentFormulaVariableId());
            resp.setValue(orderPriceComponent.getValue()); // set value from the service order's record
            resp.setVariableDescription(variable.getDescription());
            respList.add(resp);
        }

        return respList;
    }


    /**
     * Dynamically returns the service parameters fields for the given service detail ID.
     *
     * @param serviceDetailId The ID of the service detail.
     * @return The service parameters fields.
     */
    public ServiceOrderServiceParametersFields getServiceParametersFields(Long serviceDetailId) {
        ServiceOrderServiceParametersFields fields = new ServiceOrderServiceParametersFields();

        ServiceDetails serviceDetails = serviceDetailsRepository
                .findById(serviceDetailId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Service detail not found with ID %s".formatted(serviceDetailId)));

        List<InvoicePaymentTermsResponse> invoicePaymentTerms = new ArrayList<>();
        if (serviceDetails.getTermsGroups() != null) {
            // this means that term group has been attached to the version
            invoicePaymentTerms.addAll(
                    invoicePaymentTermsRepository.findDetailedByTermIdAndStatusIn(
                            termsRepository.getTermIdFromCurrentTermGroup(serviceDetails.getTermsGroups().getId()),
                            List.of(PaymentTermStatus.ACTIVE)
                    )
            );
        } else if (serviceDetails.getTerms() != null) {
            invoicePaymentTerms.addAll(
                    invoicePaymentTermsRepository.findDetailedByTermIdAndStatusIn(
                            serviceDetails.getTerms().getId(),
                            List.of(PaymentTermStatus.ACTIVE)
                    )
            );
        }

        // collect price components and price component group current version's price components for the given service detail
        List<PriceComponent> serviceDetailPriceComponents = new ArrayList<>();
        serviceDetailPriceComponents.addAll(
                priceComponentRepository
                        .findByIdIn(
                                priceComponentRepository.getPriceComponentsFromCurrentServicePriceComponentGroup(serviceDetailId)
                        )
        );
        serviceDetailPriceComponents.addAll(
                serviceDetails
                        .getPriceComponents()
                        .stream()
                        .filter(pc -> pc.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                        .map(ServicePriceComponent::getPriceComponent)
                        .toList()
        );

        List<ServiceContractPriceComponentFormula> serviceContractPriceComponentFormulas = serviceContractServiceParametersService.getFormulaVariables(serviceDetails);
        fields.setQuantityVisible(serviceContractServiceParametersService.checkPerPieceComponentExists(serviceContractPriceComponentFormulas, serviceDetailId));

        fields.setExecutionLevel(serviceDetails.getExecutionLevel());
        fields.setInvoicePaymentTerms(invoicePaymentTerms);
        fields.setFormulaVariables(mapToFormulaVariablesResponse(serviceDetailPriceComponents));
        fields.setServiceContractTerms(serviceContractTermRepository.findByServiceDetailsIdAndStatusInOrderByCreateDate(serviceDetailId, List.of(ServiceSubobjectStatus.ACTIVE)).stream().map(ServiceContractTermShortResponse::new).toList());
        return fields;
    }


    /**
     * Maps the given price components to formula variables response.
     *
     * @param priceComponents The price components to be mapped.
     * @return The mapped formula variables response, or empty list if the given price components are null or empty.
     */
    private List<PriceComponentFormula> mapToFormulaVariablesResponse(List<PriceComponent> priceComponents) {
        if (CollectionUtils.isEmpty(priceComponents)) {
            return new ArrayList<>();
        }

        return priceComponents
                .stream()
                .map(ServiceOrderServiceParametersService::mapToVariableResponse)
                .toList();
    }


    /**
     * Maps the given price component to formula variables response.
     *
     * @param priceComponent The price component to be mapped.
     * @return The mapped formula variables response.
     */
    private static PriceComponentFormula mapToVariableResponse(PriceComponent priceComponent) {
        PriceComponentFormula priceComponentFormula = new PriceComponentFormula();
        priceComponentFormula.setPriceComponentId(priceComponent.getId());
        List<PriceComponentFormulaVariables> variables = new ArrayList<>();
        for (PriceComponentFormulaVariable variable : priceComponent.getFormulaVariables()) {
            variables.add(getPriceComponentFormulaVariable(variable, priceComponent.getName()));
        }
        priceComponentFormula.setVariables(variables);
        return priceComponentFormula;
    }


    /**
     * @param variable           {@link PriceComponentFormulaVariable} entity
     * @param priceComponentName String
     * @return {@link PriceComponentFormulaVariables} response
     */
    private static PriceComponentFormulaVariables getPriceComponentFormulaVariable(PriceComponentFormulaVariable variable, String priceComponentName) {
        PriceComponentFormulaVariables formulaVariable = new PriceComponentFormulaVariables();
        formulaVariable.setFormulaVariableId(variable.getId());
        formulaVariable.setVariable(variable.getVariable());
        formulaVariable.setValue(variable.getValue());
        formulaVariable.setValueFrom(variable.getValueFrom());
        formulaVariable.setValueTo(variable.getValueTo());
        formulaVariable.setVariableDescription(variable.getDescription());
        formulaVariable.setDisplayName(variable.getDescription() + " (" + variable.getVariable() + " from " + priceComponentName + ")");
        return formulaVariable;
    }


    /**
     * Creates the service parameters sub objects.
     * Validations according to business logic are performed in the validator method.
     *
     * @param request       The request containing the service parameters.
     * @param serviceOrder  The service order entity.
     * @param errorMessages The list of error messages to be populated if any validation fails.
     */
    public void createServiceParametersSubObjects(Long serviceDetailId, ServiceOrderServiceParametersRequest request, ServiceOrder serviceOrder, List<String> errorMessages) {
        log.debug("Creating service parameters sub objects: {}", request);
        serviceOrder.setServiceContractTermId(request.getContractTermId());
        serviceOrder.setContractTermCertainDateValue(request.getContractTermCertainDateValue());
        setInvoicePaymentFields(serviceDetailId, request, serviceOrder);
        createFormulaVariables(request, serviceOrder, errorMessages);
        serviceOrderLinkedObjectsService.createLinkedContracts(request, serviceOrder, errorMessages);
        serviceOrderLinkedObjectsService.createPods(request, serviceOrder, errorMessages);
        serviceOrderLinkedObjectsService.createUnrecognizedPods(request, serviceOrder, errorMessages);
    }


    /**
     * Updates the service parameters sub objects.
     * In case of changing the service detail, the old sub objects are cleared.
     *
     * @param request       The request containing the service parameters.
     * @param serviceOrder  The service order entity.
     * @param errorMessages The list of error messages to be populated if any validation fails.
     */
    public void updateServiceParametersSubObjects(ServiceOrderUpdateRequest request, ServiceOrder serviceOrder, List<String> errorMessages) {
        log.debug("Updating service parameters sub objects: {}", request);
        ServiceOrderServiceParametersRequest serviceParameters = request.getServiceParameters();
        if (!serviceOrder.getServiceDetailId().equals(request.getBasicParameters().getServiceDetailId())) {
            clearRemovedServiceParametersAndObjects(serviceOrder);
            createServiceParametersSubObjects(request.getBasicParameters().getServiceDetailId(), serviceParameters, serviceOrder, errorMessages);
        } else {
            serviceOrder.setServiceContractTermId(request.getServiceParameters().getContractTermId());
            serviceOrder.setContractTermCertainDateValue(request.getServiceParameters().getContractTermCertainDateValue());
            setInvoicePaymentFields(request.getBasicParameters().getServiceDetailId(), request.getServiceParameters(), serviceOrder);
            updateFormulaVariables(serviceOrder, errorMessages, serviceParameters);
            serviceOrderLinkedObjectsService.updateLinkedContracts(serviceOrder, errorMessages, serviceParameters);
            serviceOrderLinkedObjectsService.updatePods(serviceOrder, errorMessages, serviceParameters);
            serviceOrderLinkedObjectsService.updateUnrecognizedPods(serviceOrder, errorMessages, serviceParameters);
        }
    }


    /**
     * Updates formula variables.
     *
     * @param serviceOrder      The service order entity.
     * @param errorMessages     The list of error messages to be populated if any validation fails.
     * @param serviceParameters The service parameters request.
     */
    private void updateFormulaVariables(ServiceOrder serviceOrder, List<String> errorMessages, ServiceOrderServiceParametersRequest serviceParameters) {
        List<ServiceOrderFormulaVariableRequest> formulaVariables = serviceParameters.getFormulaVariables();

        List<ServiceOrderPriceComponent> persistedFormulaVariables = serviceOrderPriceComponentRepository
                .findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));

        List<Long> persistedFormulaVariableIds = persistedFormulaVariables
                .stream()
                .map(ServiceOrderPriceComponent::getPriceComponentFormulaVariableId)
                .toList();

        if (CollectionUtils.isEmpty(formulaVariables)) {
            formulaVariables = new ArrayList<>();
        }

        List<ServiceOrderPriceComponent> tempList = new ArrayList<>();
        List<Long> pcGroupFormulaVariableIds = getPcGroupFormulaVariableIds(serviceOrder, serviceParameters);

        for (ServiceOrderFormulaVariableRequest formulaVariableRequest : formulaVariables) {
            updateOrAddFormulaVariable(
                    serviceOrder,
                    formulaVariableRequest,
                    persistedFormulaVariableIds,
                    tempList,
                    pcGroupFormulaVariableIds,
                    persistedFormulaVariables
            );
        }

        deleteRemovedOrTransferredVariables(formulaVariables, pcGroupFormulaVariableIds, persistedFormulaVariables, tempList);

        if (CollectionUtils.isEmpty(errorMessages)) {
            serviceOrderPriceComponentRepository.saveAll(tempList);
        }
    }


    /**
     * Sets a deleted status to the formula variables that were removed or transferred from directly attached pc to a pc group.
     */
    private void deleteRemovedOrTransferredVariables(List<ServiceOrderFormulaVariableRequest> formulaVariables,
                                                     List<Long> pcGroupFormulaVariableIds,
                                                     List<ServiceOrderPriceComponent> persistedFormulaVariables,
                                                     List<ServiceOrderPriceComponent> tempList) {
        for (ServiceOrderPriceComponent persistedFormulaVariable : persistedFormulaVariables) {
            Long formulaVariableId = persistedFormulaVariable.getPriceComponentFormulaVariableId();
            if (formulaVariables.stream().noneMatch(fv -> fv.getFormulaVariableId().equals(formulaVariableId))
                    || pcGroupFormulaVariableIds.contains(formulaVariableId)) {
                persistedFormulaVariable.setStatus(EntityStatus.DELETED);
                tempList.add(persistedFormulaVariable);
            }
        }
    }


    /**
     * Returns the list of formula variable IDs belonging to price component group of the given service detail.
     */
    private List<Long> getPcGroupFormulaVariableIds(ServiceOrder serviceOrder, ServiceOrderServiceParametersRequest serviceParameters) {
        return priceComponentFormulaVariableRepository
                .findAllBelongingToPriceComponentGroupOfServiceDetailAndIdIn(
                        serviceOrder.getServiceDetailId(),
                        serviceParameters.getFormulaVariables()
                                .stream()
                                .map(ServiceOrderFormulaVariableRequest::getFormulaVariableId)
                                .toList()
                );
    }


    /**
     * Depending on whether the formula variable is already persisted or not, it should be updated or created.
     */
    private void updateOrAddFormulaVariable(ServiceOrder serviceOrder,
                                            ServiceOrderFormulaVariableRequest formulaVariableRequest,
                                            List<Long> persistedFormulaVariableIds,
                                            List<ServiceOrderPriceComponent> tempList,
                                            List<Long> pcGroupFormulaVariableIds,
                                            List<ServiceOrderPriceComponent> persistedFormulaVariables) {
        if (!persistedFormulaVariableIds.contains(formulaVariableRequest.getFormulaVariableId())) {
            createOrderFormulaVariable(serviceOrder, tempList, pcGroupFormulaVariableIds, formulaVariableRequest);
        } else {
            ServiceOrderPriceComponent orderPriceComponent = persistedFormulaVariables
                    .stream()
                    .filter(pfv -> pfv.getPriceComponentFormulaVariableId().equals(formulaVariableRequest.getFormulaVariableId()))
                    .findFirst().get();
            orderPriceComponent.setValue(formulaVariableRequest.getValue());
            tempList.add(orderPriceComponent);
        }
    }


    /**
     * Create a new service order price component formula variable object if it does not belong to a price component group.
     *
     * @param serviceOrder              The service order entity.
     * @param tempList                  The list of service order price component objects to be saved.
     * @param pcGroupFormulaVariableIds The list of formula variable IDs belonging to price component group.
     * @param formulaVariableRequest    The formula variable request.
     */
    private void createOrderFormulaVariable(ServiceOrder serviceOrder,
                                            List<ServiceOrderPriceComponent> tempList,
                                            List<Long> pcGroupFormulaVariableIds,
                                            ServiceOrderFormulaVariableRequest formulaVariableRequest) {
        if (pcGroupFormulaVariableIds.contains(formulaVariableRequest.getFormulaVariableId())) {
            return;
        }

        ServiceOrderPriceComponent orderPriceComponent = new ServiceOrderPriceComponent();
        orderPriceComponent.setOrderId(serviceOrder.getId());
        orderPriceComponent.setPriceComponentFormulaVariableId(formulaVariableRequest.getFormulaVariableId());
        orderPriceComponent.setValue(formulaVariableRequest.getValue());
        orderPriceComponent.setStatus(EntityStatus.ACTIVE);
        tempList.add(orderPriceComponent);
    }


    /**
     * Clears service parameters and sub objects depending on the service execution level.
     * Should be used in case of changing the service detail.
     *
     * @param serviceOrder The service order entity.
     */
    private void clearRemovedServiceParametersAndObjects(ServiceOrder serviceOrder) {
        serviceOrder.setInvoicePaymentTermId(null);
        serviceOrder.setInvoicePaymentTermValue(null);
        serviceOrderRepository.save(serviceOrder);

        List<ServiceOrderPriceComponent> persistedFormulaVariables = serviceOrderPriceComponentRepository.findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));
        for (ServiceOrderPriceComponent formulaVariable : persistedFormulaVariables) {
            formulaVariable.setStatus(EntityStatus.DELETED);
            serviceOrderPriceComponentRepository.save(formulaVariable);
        }

        serviceOrderLinkedObjectsService.clearRemovedServiceParametersAndObjects(serviceOrder);
    }


    /**
     * Creates the formula variables for the given service order.
     * If the formula variable represents a variable that belongs to the service detail's price component group,
     * then the value should not be saved on the order's side. It will be dynamically fetched from
     * the service version's price component group's current version's formula variable each time.
     *
     * @param request       The request containing the formula variables.
     * @param serviceOrder  The service order entity.
     * @param errorMessages The list of error messages to be populated if any validation fails.
     */
    private void createFormulaVariables(ServiceOrderServiceParametersRequest request, ServiceOrder serviceOrder, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(request.getFormulaVariables())) {
            return; // formula variables validation is handled before this method is called
        }

        List<Long> pcGroupFormulaVariableIds = getPcGroupFormulaVariableIds(serviceOrder, request);

        List<ServiceOrderPriceComponent> tempList = new ArrayList<>();

        for (ServiceOrderFormulaVariableRequest formulaVariableRequest : request.getFormulaVariables()) {
            createOrderFormulaVariable(serviceOrder, tempList, pcGroupFormulaVariableIds, formulaVariableRequest);
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            serviceOrderPriceComponentRepository.saveAll(tempList);
        }
    }


    /**
     * Sets invoice payment field values to service order. If the service detail has terms attached, then we should save the values
     * on the order's side. If the detail has terms group attached, then we are not saving these fields on the order's side, because
     * when being used in billing or on preview, they should be dynamically fetched from the service detail's term group's current version's invoice payment terms each time.
     *
     * @param serviceDetailId          The ID of the service detail.
     * @param serviceParametersRequest The service parameters request.
     * @param serviceOrder             The service order entity.
     */
    public void setInvoicePaymentFields(Long serviceDetailId,
                                        ServiceOrderServiceParametersRequest serviceParametersRequest,
                                        ServiceOrder serviceOrder) {
        ServiceDetails serviceDetails = serviceDetailsRepository
                .findById(serviceDetailId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Service detail not found with ID %s".formatted(serviceDetailId)));

        if (serviceDetails.getTerms() != null) {
            serviceOrder.setInvoicePaymentTermId(serviceParametersRequest.getInvoicePaymentTermId());
            serviceOrder.setInvoicePaymentTermValue(serviceParametersRequest.getInvoicePaymentTermValue());
        } else {
            serviceOrder.setInvoicePaymentTermId(null);
            serviceOrder.setInvoicePaymentTermValue(null);
        }
    }

    private void validateQuantity(ServiceOrderServiceParametersRequest request, Long serviceDetailId, List<String> errorMessages) {
        ServiceDetails serviceDetails = serviceDetailsRepository
                .findByIdAndStatus(serviceDetailId,ServiceDetailStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Service detail not found with ID %s;".formatted(serviceDetailId)));

        List<ServiceContractPriceComponentFormula> formulaVariables = serviceContractServiceParametersService.getFormulaVariables(serviceDetails);
        request.setQuantity(serviceContractServiceParametersService.getQuantityForPerPieceComponent(formulaVariables, serviceDetailId, request.getQuantity() != null ? BigDecimal.valueOf(request.getQuantity()) : null, errorMessages));
    }
}
