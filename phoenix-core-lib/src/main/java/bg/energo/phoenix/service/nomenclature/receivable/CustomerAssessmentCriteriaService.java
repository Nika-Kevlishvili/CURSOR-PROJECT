package bg.energo.phoenix.service.nomenclature.receivable;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.receivable.CustomerAssessmentCriteria;
import bg.energo.phoenix.model.enums.nomenclature.CustomerAssessmentCriteriaType;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.Assessment;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.receivable.CustomerAssessmentCriteriaRequest;
import bg.energo.phoenix.model.request.nomenclature.receivable.CustomerAssessmentCriteriaTypeRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.receivable.CustomerAssessmentCriteriaEditResponse;
import bg.energo.phoenix.model.response.nomenclature.receivable.CustomerAssessmentCriteriaResponse;
import bg.energo.phoenix.repository.nomenclature.receivable.CustomerAssessmentCriteriaRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static bg.energo.phoenix.model.enums.nomenclature.Nomenclature.CUSTOMER_ASSESSMENTS_CRITERIA;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.ACTIVE;
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER_ASSESSMENT_CRITERIA;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAssessmentCriteriaService implements NomenclatureBaseService {

    private final CustomerAssessmentCriteriaRepository customerAssessmentCriteriaRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return CUSTOMER_ASSESSMENTS_CRITERIA;
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = CUSTOMER_ASSESSMENT_CRITERIA,
                            permissions = {NOMENCLATURE_VIEW}
                    )
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering customer assessment criteria nomenclature with request: {}", request.toString());
        return customerAssessmentCriteriaRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );
    }

    public Page<CustomerAssessmentCriteriaResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering customer assessment criteria nomenclature with request: {}", request);
        Page<CustomerAssessmentCriteria> customerAssessmentCriteria = customerAssessmentCriteriaRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );

        return customerAssessmentCriteria.map(this::nomenclatureResponseFromEntity);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = CUSTOMER_ASSESSMENT_CRITERIA,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in customer assessment criteria ", request.getId());

        CustomerAssessmentCriteria customerAssessmentCriteria = customerAssessmentCriteriaRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-customer assessment criteria not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<CustomerAssessmentCriteria> customerAssessmentCriteriaList;

        if (customerAssessmentCriteria.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = customerAssessmentCriteria.getOrderingId();
            customerAssessmentCriteriaList = customerAssessmentCriteriaRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            customerAssessmentCriteria.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (CustomerAssessmentCriteria cp : customerAssessmentCriteriaList) {
                cp.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = customerAssessmentCriteria.getOrderingId();
            end = request.getOrderingId();
            customerAssessmentCriteriaList = customerAssessmentCriteriaRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            customerAssessmentCriteria.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (CustomerAssessmentCriteria cp : customerAssessmentCriteriaList) {
                cp.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        customerAssessmentCriteria.setOrderingId(request.getOrderingId());
        customerAssessmentCriteriaList.add(customerAssessmentCriteria);
        customerAssessmentCriteriaRepository.saveAll(customerAssessmentCriteriaList);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = CUSTOMER_ASSESSMENT_CRITERIA,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void sortAlphabetically() {
        log.debug("Sorting the customer assessment criteria alphabetically");

        List<CustomerAssessmentCriteria> customerAssessmentCriteriaList = customerAssessmentCriteriaRepository.orderByName();
        long orderingId = 1;

        for (CustomerAssessmentCriteria customerAssessmentCriteria : customerAssessmentCriteriaList) {
            customerAssessmentCriteria.setOrderingId(orderingId);
            orderingId++;
        }

        customerAssessmentCriteriaRepository.saveAll(customerAssessmentCriteriaList);
    }

    @Override
    public void delete(Long id) {

    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return customerAssessmentCriteriaRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return customerAssessmentCriteriaRepository.findByIdIn(ids);
    }

    public CustomerAssessmentCriteriaResponse view(Long id) {
        log.debug("Fetching customer assessment criteria with ID: {}", id);
        CustomerAssessmentCriteria customerAssessmentCriteria = customerAssessmentCriteriaRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-customer assessment criteria not found, ID: " + id));

        return new CustomerAssessmentCriteriaResponse(customerAssessmentCriteria);
    }

    public Assessment getAssessmentByCriteriaIdAndValue(Long id, String value) {
        CustomerAssessmentCriteriaResponse customerAssessmentCriteriaResponse = view(id);
        if (isNotValidSingleSelectValue(value)) {
            throw new IllegalArgumentException("parameterValue-parameterValue must be 'Yes' or 'No' for criteria with id %s;".formatted(id));
        }

        Boolean valueFromCriteria = customerAssessmentCriteriaResponse.value();
        Boolean valueFromRequest = value.equals(Assessment.YES.name());

        return valueFromCriteria.equals(valueFromRequest) ? Assessment.YES : Assessment.NO;
    }

    @Transactional
    public CustomerAssessmentCriteriaEditResponse edit(CustomerAssessmentCriteriaRequest request) {
        log.debug("Editing customer assessment criteria: {}", request);
        List<String> errorMessages = new ArrayList<>();
        List<CustomerAssessmentCriteriaTypeRequest> requestList = request.getCriteriaTypeList();
        validateEditRequest(requestList, errorMessages);

        List<CustomerAssessmentCriteria> criteriaEntities = customerAssessmentCriteriaRepository
                .findAllByStatusAndCriteriaTypeIn(ACTIVE, EPBListUtils.transform(requestList, CustomerAssessmentCriteriaTypeRequest::getCriteriaType));

        for (CustomerAssessmentCriteriaTypeRequest criteriaRequest : requestList) {
            for (CustomerAssessmentCriteria criteriaEntity : criteriaEntities) {
                if (criteriaRequest.getCriteriaType().equals(criteriaEntity.getCriteriaType())) {
                    if (isValueRangeCriteriaType(criteriaRequest.getCriteriaType())) {
                        if (criteriaRequest.getValue() != null) {
                            errorMessages.add("acceptableValue-acceptableValue must be null for value range criteria type: %s".formatted(criteriaRequest.getCriteriaType().name()));
                        } else {
                            criteriaEntity.setValueTo(criteriaRequest.getValueTo());
                            criteriaEntity.setValueFrom(criteriaRequest.getValueFrom());
                        }
                    } else if (isYesNoCriteriaType(criteriaRequest.getCriteriaType())) {
                        if (criteriaRequest.getValueTo() != null || criteriaRequest.getValueFrom() != null) {
                            errorMessages.add("valueTo and valueFrom must be null for Yes/No criteria type: %s".formatted(criteriaRequest.getCriteriaType().name()));
                        } else {
                            criteriaEntity.setValue(criteriaRequest.getValue());
                        }
                    }
                }
            }
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        customerAssessmentCriteriaRepository.saveAll(criteriaEntities);

        return new CustomerAssessmentCriteriaEditResponse(EPBListUtils.transform(criteriaEntities, CustomerAssessmentCriteria::getId));
    }

    private void validateEditRequest(List<CustomerAssessmentCriteriaTypeRequest> requestList, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(requestList)) {
            errorMessages.add("criteriaTypeList-criteriaTypeList must not be empty");
            return;
        }

        for (CustomerAssessmentCriteriaTypeRequest criteriaTypeRequest : requestList) {
            CustomerAssessmentCriteriaType criteriaType = criteriaTypeRequest.getCriteriaType();

            BigDecimal valueTo = criteriaTypeRequest.getValueTo();
            BigDecimal valueFrom = criteriaTypeRequest.getValueFrom();
            if (Objects.nonNull(valueTo) && Objects.nonNull(valueFrom) && valueFrom.compareTo(valueTo) > 0) {
                errorMessages.add("value to of %s must be greater than value from".formatted(criteriaType.name()));
            }
        }
    }

    public CustomerAssessmentCriteriaResponse nomenclatureResponseFromEntity(CustomerAssessmentCriteria customerAssessmentCriteria) {
        return new CustomerAssessmentCriteriaResponse(customerAssessmentCriteria);
    }

    private boolean isValueRangeCriteriaType(CustomerAssessmentCriteriaType criteriaType) {
        return List.of(
                CustomerAssessmentCriteriaType.REMINDERS,
                CustomerAssessmentCriteriaType.REQUESTS_FOR_DISCONNECTION_OF_THE_POWER_SUPPLY,
                CustomerAssessmentCriteriaType.RESCHEDULING_AGREEMENTS_FOR_THE_LAST_TWELVE_MONTHS,
                CustomerAssessmentCriteriaType.ACTIVE_REQUEST_FOR_DISCONNECTION
        ).contains(criteriaType);
    }

    private boolean isYesNoCriteriaType(CustomerAssessmentCriteriaType criteriaType) {
        return List.of(
                CustomerAssessmentCriteriaType.ACTIVE_RESCHEDULING_AGREEMENT,
                CustomerAssessmentCriteriaType.EXISTENCE_OF_LAWSUITS_WITH_THE_CUSTOMER,
                CustomerAssessmentCriteriaType.LIABILITY_FROM_A_PF,
                CustomerAssessmentCriteriaType.CHANGE_OF_OWNERSHIP_IN_THE_LAST_TWELVE_MONTHS,
                CustomerAssessmentCriteriaType.INSOLVENCY_AND_LIQUIDATION_CLOSURE
        ).contains(criteriaType);
    }

    private boolean isNotValidSingleSelectValue(String value) {
        return value == null || (!value.equalsIgnoreCase("Yes") && !value.equalsIgnoreCase("No"));
    }

}
