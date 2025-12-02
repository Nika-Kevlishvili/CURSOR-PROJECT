package bg.energo.phoenix.service.pod.discount;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.pod.discount.Discount;
import bg.energo.phoenix.model.entity.pod.discount.DiscountPointOfDeliveries;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.discount.DiscountParameterColumnName;
import bg.energo.phoenix.model.enums.pod.discount.DiscountParameterFilterField;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.enums.shared.InvoicedOptions;
import bg.energo.phoenix.model.request.pod.discount.DiscountListRequest;
import bg.energo.phoenix.model.request.pod.discount.DiscountRequest;
import bg.energo.phoenix.model.response.pod.discount.DiscountListResponse;
import bg.energo.phoenix.model.response.pod.discount.DiscountPointOfDeliveryShortResponse;
import bg.energo.phoenix.model.response.pod.discount.DiscountResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.pod.discount.DiscountPointOfDeliveryRepository;
import bg.energo.phoenix.repository.pod.discount.DiscountRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.pod.discount.mappers.DiscountMapperService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiscountService {
    private final DiscountRepository discountRepository;
    private final CurrencyRepository currencyRepository;
    private final DiscountPointOfDeliveryRepository discountPointOfDeliveryRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final DiscountMapperService discountMapperService;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final PermissionService permissionService;

    public DiscountResponse preview(Long id) {
        Discount discount = discountRepository.findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE, EntityStatus.DELETED))
                .orElseThrow(() ->
                        new DomainEntityNotFoundException("id-Active Discount not found with presented id;"));

        Currency currency = currencyRepository.findById(discount.getCurrencyId()).orElse(null);

        List<DiscountPointOfDeliveryShortResponse> allActivePointOfDeliveriesByDiscount =
                discountPointOfDeliveryRepository
                        .findAllActiveDiscountPointOfDeliveriesByDiscountId(discount.getId());

        Customer customer = customerRepository
                .findByIdAndStatuses(discount.getCustomerId(), List.of(CustomerStatus.ACTIVE))
                .orElse(null);

        CustomerDetails customerDetails = null;
        if (customer != null) {
            customerDetails = customerDetailsRepository
                    .findByCustomerIdAndVersionId(
                            customer.getId(),
                            customerDetailsRepository
                                    .findMaxVersionIdByCustomerId(customer.getId()).orElse(1L)
                    ).orElse(null);
        }

        return discountMapperService.mapFromEntity(discount, customer, customerDetails, currency, allActivePointOfDeliveriesByDiscount);
    }

    /**
     * Creates {@link Discount} based on discount request
     *
     * @param request - request
     * @return {@link Discount#id Discount ID}
     */
    public Long create(DiscountRequest request) {
        List<String> exceptionMessages = new ArrayList<>();

        checkIfCurrencyExists(request.getCurrencyId(), exceptionMessages);
        checkIfCustomerExists(request.getCustomerId(), exceptionMessages);

        List<Long> requestedPointOfDeliveryIds = request.getPointOfDeliveryIds().stream().toList();
        List<PointOfDelivery> existingPointOfDeliveries =
                pointOfDeliveryRepository
                        .findPointOfDeliveryByIdInAndStatusIn(requestedPointOfDeliveryIds.stream().toList(), List.of(PodStatus.ACTIVE));

        List<Long> existingPointOfDeliveryIds = existingPointOfDeliveries
                .stream()
                .map(PointOfDelivery::getId)
                .toList();

        checkIfPodsExists(requestedPointOfDeliveryIds, existingPointOfDeliveryIds, exceptionMessages);
        checkIfDiscountIsOverlappingExistingDiscount(request, exceptionMessages, existingPointOfDeliveries);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        Discount discount = discountRepository.save(discountMapperService.mapToEntity(request));

        requestedPointOfDeliveryIds.forEach(podId -> {
            discountPointOfDeliveryRepository.save(new DiscountPointOfDeliveries(null, discount.getId(), podId, PodStatus.ACTIVE));
        });

        return discount.getId();
    }

    public Long edit(Long id, DiscountRequest request) {
        List<String> exceptionMessages = new ArrayList<>();

        Discount discount = discountRepository
                .findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Active discount with presented id: [%s] not found;".formatted(id)));

        if (Boolean.TRUE.equals(discount.getInvoiced())) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.DISCOUNT, List.of(PermissionEnum.DISCOUNT_EDIT_LOCKED))) {
                throw new ClientException("id -[id] Can't edit because it is connected to invoice", ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }

        checkIfCurrencyExistsOnEdit(request.getCurrencyId(), discount.getCurrencyId(), exceptionMessages);
        checkIfCustomerExists(request.getCustomerId(), exceptionMessages);

        List<Long> requestedPointOfDeliveryIds = request.getPointOfDeliveryIds().stream().toList();
        List<PointOfDelivery> existingPointOfDeliveries =
                pointOfDeliveryRepository
                        .findPointOfDeliveryByIdInAndStatusIn(requestedPointOfDeliveryIds.stream().toList(), List.of(PodStatus.ACTIVE));

        List<Long> existingPointOfDeliveryIds = existingPointOfDeliveries
                .stream()
                .map(PointOfDelivery::getId)
                .toList();

        checkIfPodsExists(requestedPointOfDeliveryIds, existingPointOfDeliveryIds, exceptionMessages);
        checkIfDiscountIsOverlappingExistingDiscountOnUpdate(discount.getId(), request, exceptionMessages, existingPointOfDeliveries);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        discountRepository.save(discountMapperService.updateDiscount(discount, request));

        List<DiscountPointOfDeliveries> activePointOfDeliveriesByDiscountId =
                discountPointOfDeliveryRepository.findAllActivePointOfDeliveriesByDiscountId(discount.getId());
        List<Long> activePointOfDeliveryIds = activePointOfDeliveriesByDiscountId.stream().map(DiscountPointOfDeliveries::getPointOfDeliveryId).toList();

        requestedPointOfDeliveryIds.forEach(podId -> {
            if (!activePointOfDeliveryIds.contains(podId)) {
                discountPointOfDeliveryRepository.save(new DiscountPointOfDeliveries(null, discount.getId(), podId, PodStatus.ACTIVE));
            } else {
                activePointOfDeliveriesByDiscountId
                        .stream()
                        .filter(active ->
                                active
                                        .getPointOfDeliveryId()
                                        .equals(podId))
                        .toList()
                        .forEach(active ->
                                active
                                        .setStatus(PodStatus.ACTIVE));
            }
        });

        List<DiscountPointOfDeliveries> nonMatchingPointOfDeliveries = activePointOfDeliveriesByDiscountId
                .stream()
                .filter(active -> !requestedPointOfDeliveryIds.contains(active.getPointOfDeliveryId()))
                .toList();

        nonMatchingPointOfDeliveries.forEach(pod -> pod.setStatus(PodStatus.DELETED));
        discountPointOfDeliveryRepository.saveAll(nonMatchingPointOfDeliveries);

        return discount.getId();
    }

    public Page<DiscountListResponse> list(DiscountListRequest discountListRequest) {
        DiscountParameterFilterField discountParameterFilterField = discountListRequest.getSearchBy();
        if (discountParameterFilterField == null) {
            discountParameterFilterField = DiscountParameterFilterField.ALL;
        }

        Sort.Direction columnDirection = discountListRequest.getSortDirection();
        if (columnDirection == null) {
            columnDirection = Sort.Direction.ASC;
        }

        DiscountParameterColumnName discountParameterColumnName = discountListRequest.getSortBy();
        if (discountParameterColumnName == null) {
            discountParameterColumnName = DiscountParameterColumnName.DATE_OF_CREATION;
        }

        return discountRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(discountListRequest.getPrompt()),
                discountListRequest.getDateFromBegin(),
                discountListRequest.getDateFromEnd(),
                discountListRequest.getDateToBegin(),
                discountListRequest.getDateToEnd(),
                InvoicedOptions.fromOptions(discountListRequest.getInvoiced()),
                discountParameterFilterField.getValue(),
                getEntityStatusesByPermission(),
                PageRequest.of(discountListRequest.getPage(), discountListRequest.getSize(), Sort.by(columnDirection, discountParameterColumnName.getValue())));
    }


    /**
     * @return list of {@link EntityStatus} by permission
     */
    private List<EntityStatus> getEntityStatusesByPermission() {
        List<EntityStatus> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.DISCOUNT, List.of(PermissionEnum.DISCOUNT_VIEW_BASIC))) {
            statuses.add(EntityStatus.ACTIVE);
        }

        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.DISCOUNT, List.of(PermissionEnum.DISCOUNT_VIEW_DELETED))) {
            statuses.add(EntityStatus.DELETED);
        }
        return statuses;
    }


    public Long delete(Long id) {
        Discount discount = discountRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Discount with presented id not found;"));

        if (Boolean.TRUE.equals(discount.getInvoiced())) {
            throw new ClientException("id -[id] Can't delete because it is connected to invoice", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        if (discount.getStatus() == EntityStatus.DELETED) {
            log.error("id-Discount with presented id: {} is already DELETED;", id);
            throw new OperationNotAllowedException("id-Discount with presented id: %s is already DELETED;".formatted(id));
        }

        if (hasActiveConnection(discount)) {
            throw new OperationNotAllowedException("id-Discount with presented id: %s has active connection;".formatted(id));
        }

        discount.setStatus(EntityStatus.DELETED);
        discountRepository.save(discount);

        return discount.getId();
    }

    boolean hasActiveConnection(Discount discount) {
        return false; // todo
    }

    private void checkIfDiscountIsOverlappingExistingDiscount(DiscountRequest request, List<String> exceptionMessages, List<PointOfDelivery> existingPointOfDeliveries) {
        existingPointOfDeliveries
                .forEach(pointOfDelivery -> {
                    List<Discount> activeDiscountsOnPod = discountRepository
                            .findActiveDiscountByPointOfDeliveryId(pointOfDelivery.getId());

                    List<Discount> overlappingDiscounts = activeDiscountsOnPod
                            .stream()
                            .filter(d ->
                                    validateOverlap(
                                            d.getDateFrom(),
                                            d.getDateTo(),
                                            request.getDateFrom(),
                                            request.getDateTo())
                            ).toList();

                    overlappingDiscounts.forEach(d -> {
                        exceptionMessages.add("dateFrom-Discount has an overlapping period and it is unable to create it;");
                        exceptionMessages.add("dateTo-Discount has an overlapping period and it is unable to create it;");
                        exceptionMessages.add("Current discount is overlapping discount with id: [%s], startDate: [%s], endDate [%s];"
                                .formatted(d.getId(), d.getDateFrom().toString().replace("-", "/"), d.getDateTo().toString().replace("-", "/")));
                    });
                });
    }

    private void checkIfDiscountIsOverlappingExistingDiscountOnUpdate(Long discountId, DiscountRequest request, List<String> exceptionMessages, List<PointOfDelivery> existingPointOfDeliveries) {
        existingPointOfDeliveries
                .forEach(pointOfDelivery -> {
                    List<Discount> activeDiscountsOnPod = discountRepository
                            .findActiveDiscountByPointOfDeliveryId(pointOfDelivery.getId());

                    List<Discount> overlappingDiscounts = activeDiscountsOnPod
                            .stream()
                            .filter(d ->
                                    (!Objects.equals(d.getId(), discountId)) && (
                                            validateOverlap(
                                                    d.getDateFrom(),
                                                    d.getDateTo(),
                                                    request.getDateFrom(),
                                                    request.getDateTo()
                                            )
                                    )
                            ).toList();

                    overlappingDiscounts.forEach(d -> {
                        exceptionMessages.add("dateFrom-Discount has an overlapping period and it is unable to create it;");
                        exceptionMessages.add("dateTo-Discount has an overlapping period and it is unable to create it;");
                        exceptionMessages.add("Current discount is overlapping discount with id: [%s], startDate: [%s], endDate [%s];"
                                .formatted(d.getId(), d.getDateFrom().toString().replace("-", "/"), d.getDateTo().toString().replace("-", "/")));
                    });
                });
    }

    private void checkIfPodsExists(List<Long> requestedPointOfDeliveryIds, List<Long> existingPointOfDeliveryIds, List<String> exceptionMessages) {
        for (int i = 0; i < requestedPointOfDeliveryIds.size(); i++) {
            Long id = requestedPointOfDeliveryIds.get(i);
            if (!existingPointOfDeliveryIds.contains(id)) {
                log.error("Point of delivery with presented id {} not found;", id);
                exceptionMessages.add("pointOfDeliveryIds[%s]-Point of Delivery with presented id [%s] not found;".formatted(i, id));
            }
        }
    }

    private void checkIfCustomerExists(Long customerId, List<String> exceptionMessages) {
        Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(customerId, List.of(CustomerStatus.ACTIVE));
        if (customerOptional.isEmpty()) {
            log.error("Customer with presented id {} not found;", customerId);
            exceptionMessages.add("customerId-Customer with presented id [%s] not found;".formatted(customerId));
        }
    }

    private void checkIfCurrencyExists(Long currencyId, List<String> exceptionMessages) {
        Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE));
        if (currencyOptional.isEmpty()) {
            log.error("Currency with presented id {} not found;", currencyId);
            exceptionMessages.add("currencyId-Currency with presented id [%s] not found;".formatted(currencyId));
        }
    }

    private void checkIfCurrencyExistsOnEdit(Long currencyId, Long oldCurrencyId, List<String> exceptionMessages) {
        Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        if (currencyOptional.isEmpty()) {
            log.error("Currency with presented id {} not found;", currencyId);
            exceptionMessages.add("currencyId-Currency with presented id [%s] not found;".formatted(currencyId));
        } else {
            Currency currency = currencyOptional.get();
            if (currency.getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
                if (!currencyId.equals(oldCurrencyId)) {
                    log.error("Inactive currency cannot be assigned to discount;");
                    exceptionMessages.add("currencyId-Currency with presented id [%s] is INACTIVE and cannot be assigned to discount;".formatted(currencyId));
                }
            }
        }
    }

    /**
     * Compares two dates
     *
     * @param startDate1 - start of date 1
     * @param endDate1   - end of date 1
     * @param startDate2 - start of date 2
     * @param endDate2   - end of date 2
     * @return if date2 overlapping date 1, returns true else false
     */
    public boolean validateOverlap(LocalDate startDate1, LocalDate endDate1, LocalDate startDate2, LocalDate endDate2) {
        return ((startDate1.compareTo(startDate2) < 1) && (startDate2.compareTo(endDate1) < 1))
                || ((startDate2.compareTo(startDate1) < 1) && (endDate2.compareTo(startDate1) > 0));
    }

    @Transactional
    public void checkInvoicedDiscounts(List<Long> invoicesToCheckDiscount) {
        discountRepository.markDiscountsAsInvoiced(invoicesToCheckDiscount);
    }
}
