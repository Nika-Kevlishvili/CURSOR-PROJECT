package bg.energo.phoenix.service.billing.compensations;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.compensation.Compensations;
import bg.energo.phoenix.model.enums.billing.compensation.CompensationStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.request.billing.compensations.CompensationListingRequest;
import bg.energo.phoenix.model.request.billing.compensations.CompensationRequest;
import bg.energo.phoenix.model.response.billing.compensations.CompensationListingResponse;
import bg.energo.phoenix.model.response.billing.compensations.CompensationResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.compensation.CompensationRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompensationService {
    private final CustomerRepository customerRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final CompensationRepository compensationRepository;
    private final PermissionService permissionService;
    private final CurrencyRepository currencyRepository;

    @Transactional
    public Long create(CompensationRequest request) {
        List<String> errorMessages = new ArrayList<>();

        validateCustomer(request.getCustomerId(), null, errorMessages);
        validatePod(request.getPodId(), null, errorMessages);
        validateCurrency(request.getDocumentCurrencyId(), null, errorMessages);
        validateRecipient(request.getRecipientId(), null, errorMessages);
        validateCustomerAndRecipient(request.getRecipientId(), request.getCustomerId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        return compensationRepository.save(
                        Compensations.builder()
                                .compensationDocumentNumber(request.getNumber())
                                .compensationDocumentAmount(request.getDocumentAmount())
                                .compensationDocumentCurrencyId(request.getDocumentCurrencyId())
                                .compensationDocumentPeriod(request.getDocumentPeriod())
                                .compensationReason(request.getReason())
                                .compensationDocumentVolumes(request.getVolumes())
                                .compensationDocumentDate(request.getDate())
                                .compensationDocumentPrice(request.getPrice())
                                .customerId(request.getCustomerId())
                                .podId(request.getPodId())
                                .recipientId(request.getRecipientId())
                                .compensationStatus(CompensationStatus.UNINVOICED)
                                .status(EntityStatus.ACTIVE)
                                .build()
                )
                .getId();
    }

    /**
     * Retrieves a list of compensation details based on the specified filtering and pagination criteria.
     *
     * @param request the request containing the filtering, sorting, and pagination parameters
     * @return a list of responses encapsulating compensation details that match the criteria
     */
    public Page<CompensationListingResponse> listing(CompensationListingRequest request) {
        List<EntityStatus> validStatuses = new ArrayList<>();

        if (hasPermission(PermissionEnum.GOVERNMENT_COMPENSATION_VIEW)) {
            validStatuses.add(EntityStatus.ACTIVE);
        }

        if (hasPermission(PermissionEnum.GOVERNMENT_COMPENSATION_VIEW_DELETED)) {
            validStatuses.add(EntityStatus.DELETED);
        }

        return compensationRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.prompt()),
                        EPBListUtils.convertEnumListToDBEnumArray(request.statuses()),
                        CollectionUtils.isEmpty(request.currencies()) ? new ArrayList<>() : request.currencies(),
                        request.date(),
                        ObjectUtils.defaultIfNull(
                                request.searchBy(),
                                CompensationListingRequest.CompensationListingSearchByFields.ALL
                        ).name(),
                        EPBListUtils.convertEnumListToDBEnumArray(validStatuses),
                        PageRequest.of(
                                ObjectUtils.defaultIfNull(
                                        request.page(),
                                        0
                                ),
                                request.size(),
                                ObjectUtils.defaultIfNull(
                                        request.direction(),
                                        Sort.Direction.DESC
                                ),
                                ObjectUtils.defaultIfNull(
                                        request.sortBy(),
                                        CompensationListingRequest.CompensationListingSortBy.CREATE_DATE
                                ).getColumn()
                        )
                ).map(CompensationListingResponse::new);
    }

    @Transactional
    public Long edit(Long id, CompensationRequest request) {
        Compensations compensations = compensationRepository.findByIdAndStatusInAndCompensationStatusIn(id,
                        List.of(EntityStatus.ACTIVE), List.of(CompensationStatus.UNINVOICED))
                .orElseThrow(() ->
                        new DomainEntityNotFoundException("Government Compensation not found with given id - %s or cannot be updated".formatted(id)));

        List<String> errorMessages = new ArrayList<>();

        validateCustomer(request.getCustomerId(), compensations.getCustomerId(), errorMessages);
        validatePod(request.getPodId(), compensations.getPodId(), errorMessages);
        validateCurrency(request.getDocumentCurrencyId(), null, errorMessages);
        validateRecipient(request.getRecipientId(), compensations.getRecipientId(), errorMessages);
        validateCustomerAndRecipient(request.getRecipientId(), request.getCustomerId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        compensations.setCompensationDocumentNumber(request.getNumber());
        compensations.setCompensationDocumentAmount(request.getDocumentAmount());
        compensations.setCompensationDocumentCurrencyId(request.getDocumentCurrencyId());
        compensations.setCompensationDocumentPeriod(request.getDocumentPeriod());
        compensations.setCompensationReason(request.getReason());
        compensations.setCompensationDocumentVolumes(request.getVolumes());
        compensations.setCompensationDocumentDate(request.getDate());
        compensations.setCompensationDocumentPrice(request.getPrice());
        compensations.setCustomerId(request.getCustomerId());
        compensations.setPodId(request.getPodId());
        compensations.setRecipientId(request.getRecipientId());

        return compensationRepository.save(compensations).getId();
    }

    public CompensationResponse view(Long id) {
        Compensations compensations = compensationRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Government Compensation not found by id %s".formatted(id)));
        if (compensations.getStatus() == EntityStatus.DELETED &&
            !hasPermission(PermissionEnum.GOVERNMENT_COMPENSATION_VIEW_DELETED)) {
            throw new ClientException("You don't have permission to view Government Compensation;", ErrorCode.ACCESS_DENIED);
        }

        return compensationRepository.view(id);
    }

    @Transactional
    public Long delete(Long id) {
        Compensations compensations = compensationRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Government compensation not found by id %s;".formatted(id)));
        if (compensations.getCompensationStatus().equals(CompensationStatus.INVOICED)) {
            log.error("Government Compensation with id: {} is Invoiced and cannot be deleted;", id);
            throw new OperationNotAllowedException("Government Compensation is Invoiced and cannot be deleted;");
        }
        if (compensations.getStatus().equals(EntityStatus.DELETED)) {
            log.error("Government Compensation with id: {} is already deleted;", id);
            throw new OperationNotAllowedException("Government Compensation is already deleted;");
        }
        compensations.setStatus(EntityStatus.DELETED);
        return compensations.getId();
    }

    private boolean hasPermission(PermissionEnum permission) {
        return permissionService.permissionContextContainsPermissions(PermissionContextEnum.GOVERNMENT_COMPENSATION, List.of(permission));
    }


    private void validateRecipient(Long recipientId, Long prevRecipientId, List<String> errorMessages) {
        if (Objects.equals(recipientId, prevRecipientId))
            return;
        if (!customerRepository.existsByIdAndStatusIn(recipientId, List.of(CustomerStatus.ACTIVE))) {
            log.error("recipientId-[recipientId] recipient customer with id {} not found;", recipientId);
            errorMessages.add("recipientId-[recipientId] recipient customer with id %s not found;".formatted(recipientId));
        }
    }

    private void validatePod(Long podId, Long prevPodId, List<String> errorMessages) {
        if (Objects.equals(podId, prevPodId))
            return;

        if (pointOfDeliveryRepository.findByIdAndStatusIn(podId, List.of(PodStatus.ACTIVE)).isEmpty()) {
            log.error("podId-[podId] point of delivery with id {} not found;", podId);
            errorMessages.add("podId-[podId] point of delivery with id %s not found;".formatted(podId));
        }
    }

    private void validateCustomer(Long customerId, Long prevCustomerId, List<String> errorMessages) {
        if (Objects.equals(customerId, prevCustomerId))
            return;
        if (!customerRepository.existsByIdAndStatusIn(customerId, List.of(CustomerStatus.ACTIVE))) {
            log.error("customerId-[customerId] customer with id {} not found;", customerId);
            errorMessages.add("customerId-[customerId] customer with id %s not found;".formatted(customerId));
        }
    }


    private void validateCustomerAndRecipient(Long recipientId, Long customerId, List<String> errorMessages) {
        if (Objects.equals(recipientId, customerId)) {
            log.error("customerId-[customerId] You cannot attach same customer as recipient;");
            errorMessages.add("customerId-[customerId] You cannot attach same customer as recipient;");
        }
    }

    private void validateCurrency(Long documentCurrencyId, Long prevCurrencyId, List<String> errorMessages) {
        if (Objects.equals(documentCurrencyId, prevCurrencyId))
            return;
        if (!currencyRepository.existsByIdAndStatusIn(documentCurrencyId, List.of(NomenclatureItemStatus.ACTIVE))) {
            log.error("documentCurrencyId-[documentCurrencyId] Currency with id {} not found;", documentCurrencyId);
            errorMessages.add("documentCurrencyId-[documentCurrencyId] Currency with id %s not found;".formatted(documentCurrencyId));
        }
    }

}
