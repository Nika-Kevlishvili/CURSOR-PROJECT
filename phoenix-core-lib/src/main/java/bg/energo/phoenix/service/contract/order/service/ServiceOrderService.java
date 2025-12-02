package bg.energo.phoenix.service.contract.order.service;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.contract.OrderType;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderSearchField;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderStatus;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderTableColumn;
import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.shared.InvoicedOptions;
import bg.energo.phoenix.model.request.contract.order.goods.OrderInvoicesRequest;
import bg.energo.phoenix.model.request.contract.order.service.ServiceOrderCreateRequest;
import bg.energo.phoenix.model.request.contract.order.service.ServiceOrderListRequest;
import bg.energo.phoenix.model.request.contract.order.service.ServiceOrderUpdateRequest;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.contract.order.goods.OrderInvoiceViewResponse;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderListResponse;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderResponse;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderServiceParametersFields;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.customer.statusChangeEvent.CustomerStatusChangeEventPublisher;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.ServiceOrderStatusChainUtil;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import bg.energo.phoenix.util.order.OrderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

import static bg.energo.phoenix.permissions.PermissionContextEnum.SERVICE_ORDERS;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOrderService {
    private final ServiceOrderBasicParametersService basicParametersService;
    private final ServiceOrderServiceParametersService serviceParametersService;
    private final ServiceOrderRepository serviceOrderRepository;
    private final PermissionService permissionService;
    private final AccountManagerRepository accountManagerRepository;
    private final ServiceOrderActivityService serviceOrderActivityService;
    private final TaskService taskService;
    private final OrderUtils orderUtils;
    private final CustomerStatusChangeEventPublisher customerStatusChangeEventPublisher;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Creates a new service order with the given parameters.
     *
     * @param request The request containing the parameters for the service order.
     * @return The ID of the newly created service order.
     */
    @Transactional
    public Long create(ServiceOrderCreateRequest request) {
        log.debug("Creating service order: {}", request);

        List<String> errorMessages = new ArrayList<>();
        checkCustomerStatus(request.getBasicParameters().getCustomerDetailId(), errorMessages);
        basicParametersService.validateBasicParametersOnCreate(request.getBasicParameters(), errorMessages);
        serviceParametersService.validateServiceParameters(request.getServiceParameters(), request.getBasicParameters().getServiceDetailId(), errorMessages);
        Long employeeId = getEmployeeOnCreate(errorMessages);
        validateAssistingEmployees(employeeId, request.getBasicParameters().getAssistingEmployees(), errorMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        ServiceOrder serviceOrder = ServiceOrderMapper.fromCreateRequestToEntity(request, orderUtils.getNextOrderNumber(), employeeId);
        serviceOrderRepository.saveAndFlush(serviceOrder);

        basicParametersService.createBasicParametersSubObjects(request.getBasicParameters(), serviceOrder, errorMessages);
        serviceParametersService.createServiceParametersSubObjects(request.getBasicParameters().getServiceDetailId(), request.getServiceParameters(), serviceOrder, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        if (serviceOrder.getCustomerDetailId() != null)
            customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(List.of(serviceOrder.getCustomerDetailId()));
        return serviceOrder.getId();
    }


    private void checkCustomerStatus(Long customerDetailId, List<String> errorMessages) {
        if (customerDetailId != null) {
            Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findById(customerDetailId);
            if (customerDetailsOptional.isEmpty()) {
                throw new DomainEntityNotFoundException("Customer details not found;");
            } else {
                if (customerDetailsOptional.get().getStatus().equals(CustomerDetailStatus.POTENTIAL)) {
                    errorMessages.add("basicParameters.customerDetailId-not allowed create service order with potential customer;");
                    EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
                }
            }
        }
    }


    /**
     * Gets the ID of the employee who is creating the service order.
     *
     * @param errorMessages list of error messages to be populated if the employee is not found
     * @return ID of the employee who is creating the service order
     */
    private Long getEmployeeOnCreate(List<String> errorMessages) {
        String loggedInUserName = permissionService.getLoggedInUserId();
        Optional<AccountManager> employeeOptional = accountManagerRepository.findByUserNameAndStatusIn(loggedInUserName, List.of(Status.ACTIVE));
        if (employeeOptional.isEmpty()) {
            log.error("basicParameters.employeeId-Unable to find employee with username %s;".formatted(loggedInUserName));
            errorMessages.add("basicParameters.employeeId-Unable to find employee with username %s;".formatted(loggedInUserName));
            return null;
        }
        return employeeOptional.get().getId();
    }

    private void validateAssistingEmployees(Long employeeId, List<Long> assistingEmployees, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(assistingEmployees) && assistingEmployees.contains(employeeId)) {
            errorMessages.add("basicParameters.assistingEmployees-Assisting employee should not match employee;");
        }
    }


    /**
     * Updates the service order with the given ID if the validations pass.
     *
     * @param id      The ID of the service order to be updated.
     * @param request The request containing the parameters for the service order.
     * @return The ID of the updated service order.
     */
    @Transactional
    public Long update(Long id, ServiceOrderUpdateRequest request) {
        log.debug("Updating service order with ID {}", id);
        List<String> errorMessages = new ArrayList<>();
        checkCustomerStatus(request.getBasicParameters().getCustomerDetailId(), errorMessages);
        ServiceOrder serviceOrder = serviceOrderRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Service order not found with ID %s".formatted(id)));
        if (serviceOrder.getStatus().equals(EntityStatus.DELETED)) {
            throw new DomainEntityNotFoundException("Cannot update deleted Service order");
        }
        Long oldCustomerId = serviceOrder.getCustomerDetailId();

        validateUpdatePermissions(request, serviceOrder, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        basicParametersService.validateBasicParametersOnUpdate(serviceOrder, request.getBasicParameters(), errorMessages);
        serviceParametersService.validateServiceParameters(request.getServiceParameters(), request.getBasicParameters().getServiceDetailId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        basicParametersService.updateBasicParametersSubObjects(request.getBasicParameters(), serviceOrder, errorMessages);
        serviceParametersService.updateServiceParametersSubObjects(request, serviceOrder, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        ServiceOrderMapper.fromUpdateRequestToEntity(serviceOrder, request);
        serviceOrderRepository.saveAndFlush(serviceOrder);
        Set<Long> customerDetailIDsTOCheck = new HashSet<>();
        if (oldCustomerId != null)
            customerDetailIDsTOCheck.add(oldCustomerId);
        if (serviceOrder.getCustomerDetailId() != null)
            customerDetailIDsTOCheck.add(serviceOrder.getCustomerDetailId());

        if (!customerDetailIDsTOCheck.isEmpty())
            customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerDetailIDsTOCheck.stream().toList());
        return serviceOrder.getId();
    }


    /**
     * Validates if the user has permissions to update the service order. Permissions are modular and are checked against specific cases:
     * <ul>
     *     <li>if a user has a permission to update a service order with status requested/confirmed, they can update the status or status modify date too</li>
     *     <li>if a service order is locked and user has a permission to edit locked orders, they can edit any field</li>
     *     <li>if user has a permission to update status only, he cannot update other fields except for status or status modify date</li>
     * </ul>
     *
     * @param request       The request containing the parameters for the service order.
     * @param serviceOrder  The service order to be updated.
     * @param errorMessages list of error messages to be populated if the user does not have permissions
     */
    private void validateUpdatePermissions(ServiceOrderUpdateRequest request, ServiceOrder serviceOrder, List<String> errorMessages) {
        ServiceOrderStatus requestedOrderStatus = request.getBasicParameters().getOrderStatus();

        if (isLocked(serviceOrder) && !permissionService.permissionContextContainsPermissions(SERVICE_ORDERS, List.of(SERVICE_ORDER_EDIT_LOCKED))) {
            log.error("You are not allowed to edit locked service order.");
            errorMessages.add("You are not allowed to edit locked service order.");
            return;
        }

        if (serviceOrder.getOrderStatus().equals(ServiceOrderStatus.REQUESTED)) {
            if (!permissionService.permissionContextContainsPermissions(SERVICE_ORDERS, List.of(SERVICE_ORDER_EDIT_REQUESTED))) {
                log.error("You are not allowed to edit the service order with status %s.".formatted(serviceOrder.getOrderStatus()));
                errorMessages.add("You are not allowed to edit the service order with status %s.".formatted(serviceOrder.getOrderStatus()));
                return;
            }
        }

        if (serviceOrder.getOrderStatus().equals(ServiceOrderStatus.CONFIRMED)) {
            if (!permissionService.permissionContextContainsPermissions(SERVICE_ORDERS, List.of(SERVICE_ORDER_EDIT_CONFIRMED))) {
                log.error("You are not allowed to edit the service order with status %s.".formatted(serviceOrder.getOrderStatus()));
                errorMessages.add("You are not allowed to edit the service order with status %s.".formatted(serviceOrder.getOrderStatus()));
                return;
            }
        }

        // each permission allowed to enter this method, has a permission to edit status
        if (!requestedOrderStatus.equals(serviceOrder.getOrderStatus())) {
            if (!ServiceOrderStatusChainUtil.canBeChangedManually(serviceOrder.getOrderStatus(), requestedOrderStatus)) {
                log.error("Unable to change the service order status from %s to %s".formatted(serviceOrder.getOrderStatus(), requestedOrderStatus));
                errorMessages.add("Unable to change the service order status from %s to %s".formatted(serviceOrder.getOrderStatus(), requestedOrderStatus));
            }
        }
    }


    /**
     * Checks if the service order is locked.
     *
     * @param serviceOrder The service order to be checked.
     * @return True if the service order is locked, false otherwise.
     */
    private boolean isLocked(ServiceOrder serviceOrder) {
        return invoiceRepository.existsInvoiceByInvoiceStatusAndServiceOrderId(InvoiceStatus.REAL, serviceOrder.getId());
    }


    /**
     * Deletes the service order with the given ID if the validations pass.
     *
     * @param id The ID of the service order to be deleted.
     * @return The ID of the deleted service order.
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting service order with ID {}", id);

        ServiceOrder serviceOrder = serviceOrderRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Service order not found with ID %s".formatted(id)));

        if (serviceOrder.getStatus().equals(EntityStatus.DELETED)) {
            log.error("Service order with ID {} is already deleted.", id);
            throw new OperationNotAllowedException("Service order with ID %s is already deleted.".formatted(id));
        }

        if (isLocked(serviceOrder)) {
            throw new OperationNotAllowedException("Can't delete because it is connected to invoice");
        }

        if (!List.of(ServiceOrderStatus.REQUESTED, ServiceOrderStatus.CONFIRMED).contains(serviceOrder.getOrderStatus())) {
            log.error("Deleting operation is allowed only if the status of the order is Requested or Confirmed.");
            throw new OperationNotAllowedException("Deleting operation is allowed only if the status of the order is Requested or Confirmed.");
        }

        if (serviceOrderRepository.hasConnectionToActivity(id)) {
            log.error("Service Order with id: {} has connection to activity.", id);
            throw new OperationNotAllowedException("You cannot delete service order because it is connected to an activity.");
        }

        if (serviceOrderRepository.hasConnectionToProductContract(id)) {
            log.debug("Service Order with id: {} has connection to Product Contract", id);
            throw new OperationNotAllowedException("You cannot delete service order because it is connected to Product Contract.");
        }

        if (serviceOrderRepository.hasConnectionToServiceContract(id)) {
            log.debug("Service Order with id: {} has connection to Service Contract", id);
            throw new OperationNotAllowedException("You cannot delete service order because it is connected to Service Contract.");
        }

        if (serviceOrderRepository.hasConnectionToServiceOrder(id)) {
            log.debug("Service Order with id: {} has connection to another Service Order", id);
            throw new OperationNotAllowedException("You cannot delete service order because it is connected to another Service Order.");
        }

        if (serviceOrderRepository.hasConnectionToGoodsOrder(id)) {
            log.debug("Service Order with id: {} has connection to Goods Order", id);
            throw new OperationNotAllowedException("You cannot delete service order because it is connected to Goods Order.");
        }

        if (serviceOrderRepository.hasConnectionToTask(id)) {
            log.debug("Service Order with id: {} has connection to Task", id);
            throw new OperationNotAllowedException("You cannot delete service order because it is connected to Task.");
        }

        serviceOrder.setStatus(EntityStatus.DELETED);
        serviceOrderRepository.save(serviceOrder);

        return serviceOrder.getId();
    }


    /**
     * Gets the service order with the given ID.
     *
     * @param id The ID of the service order to be retrieved.
     * @return The service order with the given ID.
     */
    public ServiceOrderResponse get(Long id) {
        log.debug("Getting service order with ID {}", id);

        ServiceOrder serviceOrder = serviceOrderRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Service order not found with ID %s".formatted(id)));

        ServiceOrderResponse response = new ServiceOrderResponse();
        response.setIsLockedByInvoice(isLocked(serviceOrder));
        response.setBasicParameters(basicParametersService.getBasicParametersResponse(serviceOrder));
        response.setServiceParameters(serviceParametersService.getServiceParametersResponse(serviceOrder));

        return response;
    }


    /**
     * Fetches the service orders matching the given request.
     *
     * @param request The request containing the parameters for the service order list.
     * @return The service orders matching the given request.
     */
    public Page<ServiceOrderListResponse> list(ServiceOrderListRequest request) {
        log.debug("Listing service orders: {}", request);

        ServiceOrderSearchField searchBy = request.getSearchBy();
        if (searchBy == null) {
            searchBy = ServiceOrderSearchField.ALL;
        }

        ServiceOrderTableColumn sortBy = request.getSortBy();
        if (sortBy == null) {
            sortBy = ServiceOrderTableColumn.DATE_OF_CREATION;
        }

        Sort.Direction sortDirection = request.getSortDirection();
        if (sortDirection == null) {
            sortDirection = Sort.Direction.DESC;
        }

        return serviceOrderRepository.list(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                searchBy.name(),
                sortDirection.name(),
                CollectionUtils.isEmpty(request.getServiceDetailIds()) ? new ArrayList<>() : request.getServiceDetailIds(),
                request.getDateOfCreationFrom(),
                request.getDateOfCreationTo(),
                request.getInvoiceMaturityDateFrom(),
                request.getInvoiceMaturityDateTo(),
                InvoicedOptions.fromOptions(request.getInvoicePaid()),
                request.getDirectDebit(),
                CollectionUtils.isEmpty(request.getAccountManagers()) ? new ArrayList<>() : request.getAccountManagers(),
                getEntityStatusesByPermission(),
                PageRequest.of(
                        request.getPage(),
                        request.getSize(),
                        Sort.by(sortDirection, sortBy.getValue())
                )
        );
    }


    /**
     * @return list of statuses that the user has permission to view
     */
    private List<String> getEntityStatusesByPermission() {
        List<EntityStatus> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(SERVICE_ORDERS, List.of(SERVICE_ORDER_VIEW))) {
            statuses.add(EntityStatus.ACTIVE);
        }
        if (permissionService.permissionContextContainsPermissions(SERVICE_ORDERS, List.of(SERVICE_ORDER_VIEW_DELETED))) {
            statuses.add(EntityStatus.DELETED);
        }
        return statuses.stream().map(EntityStatus::name).toList();
    }


    /**
     * Gets the service order service parameters fields for the service detail with the given ID
     * for dynamically rendering the service order creation's second page in UI. Also used in validation of the request.
     *
     * @param serviceDetailId The ID of the service detail for which to get the service order service parameters fields.
     * @return The service order service parameters fields for the service detail with the given ID.
     */
    public ServiceOrderServiceParametersFields getServiceOrderServiceParametersFields(Long serviceDetailId) {
        log.debug("Getting service order service parameters fields for service detail with ID {}", serviceDetailId);
        return serviceParametersService.getServiceParametersFields(serviceDetailId);
    }


    /**
     * Updates service order status. Should be used in case when the user only has a permission to update statuses.
     *
     * @param id                     The ID of the service order to be updated.
     * @param targetStatus           The new status of the service order.
     * @param targetStatusModifyDate The new status modify date of the service order.
     */
    public void updateStatus(Long id, ServiceOrderStatus targetStatus, LocalDate targetStatusModifyDate) {
        log.debug("Updating service order status with ID {}", id);

        ServiceOrder serviceOrder = serviceOrderRepository
                .findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Service order not found with ID %s".formatted(id)));

        ServiceOrderStatus currentStatus = serviceOrder.getOrderStatus();

        if (!targetStatus.equals(currentStatus)) {
            if (!ServiceOrderStatusChainUtil.canBeChangedManually(currentStatus, targetStatus)) {
                log.error("Unable to change the service order status from %s to %s".formatted(currentStatus, targetStatus));
                throw new OperationNotAllowedException("Unable to change the service order status from %s to %s".formatted(currentStatus, targetStatus));
            }
        }

        serviceOrder.setStatusModifyDate(targetStatusModifyDate);
        serviceOrder.setOrderStatus(targetStatus);
        serviceOrderRepository.save(serviceOrder);
    }


    /**
     * Gets the activities connected to the service order with the given ID.
     *
     * @param id The ID of the service order for which to get the activities.
     * @return The activities connected to the service order with the given ID.
     */
    public List<SystemActivityShortResponse> getActivitiesById(Long id) {
        return serviceOrderActivityService.getActivitiesByConnectedObjectId(id);
    }

    public List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksByServiceOrderId(id);
    }

    public Page<OrderInvoiceViewResponse> listInvoicesDraftsTab(Long id, OrderInvoicesRequest request) {
        return invoiceRepository.getInvoiceByOrderId(id,
                OrderType.SERVICE_ORDER.name(),
                EPBStringUtils.fromPromptToQueryParameter(request.prompt()),
                true,
                PageRequest.of(request.page(), request.size())
        );
    }

    public Page<OrderInvoiceViewResponse> listInvoicesPdfDocumentsTab(Long id, OrderInvoicesRequest request) {
        return invoiceRepository.getInvoiceByOrderId(id,
                OrderType.SERVICE_ORDER.name(),
                EPBStringUtils.fromPromptToQueryParameter(request.prompt()),
                false,
                PageRequest.of(request.page(), request.size())
        );
    }
}
