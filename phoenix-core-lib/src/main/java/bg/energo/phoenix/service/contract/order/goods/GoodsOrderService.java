package bg.energo.phoenix.service.contract.order.goods;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.contract.OrderType;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderListingSearchFields;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderListingSortFields;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderStatus;
import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.shared.InvoicedOptions;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.request.contract.order.goods.*;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.contract.order.goods.*;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.model.entity.Template;
import bg.energo.phoenix.process.repository.TemplateRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.product.goods.GoodsDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.customer.statusChangeEvent.CustomerStatusChangeEventPublisher;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.contract.goods.GoodsOrderStatusChainUtil;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import bg.energo.phoenix.util.order.OrderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsOrderService {
    private final ContractTemplateRepository contractTemplateRepository;
    private final GoodsOrderBasicParametersService basicParametersService;
    private final GoodsOrderGoodsParametersService goodsParametersService;
    private final GoodsOrderRepository goodsOrderRepository;
    private final GoodsOrderGoodsImportService importService;
    private final GoodsDetailsRepository goodsDetailsRepository;
    private final GoodsOrderMapperService goodsOrderMapperService;
    private final PermissionService permissionService;
    private final FileService fileService;
    private final TemplateRepository templateRepository;
    private final GoodsOrderActivityService goodsOrderActivityService;
    private final OrderUtils orderUtils;
    private final CustomerStatusChangeEventPublisher customerStatusChangeEventPublisher;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final InvoiceRepository invoiceRepository;


    /**
     * Preview Goods Order by id
     *
     * @param id - Goods Order ID
     * @return {@link GoodsOrderViewResponse}
     */
    public GoodsOrderViewResponse view(Long id) {
        log.debug("View goods order with id:%s;".formatted(id));
        GoodsOrderViewResponse response = new GoodsOrderViewResponse();

        GoodsOrder goodsOrder = goodsOrderRepository
                .findByIdAndStatusIn(id, goodsOrderMapperService.getStatuses())
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find active goods order;"));

        response.setIsLockedByInvoice(isLocked(goodsOrder));
        response.setBasicParametersResponse(goodsOrderMapperService.mapGoodsOrderToBasicParametersResponse(goodsOrder));
        response.setGoodsParametersResponse(goodsOrderMapperService.mapGoodsOrderToGoodsParameterResponse(goodsOrder));
        return response;
    }

    /**
     * Creates new Goods Order
     *
     * @param request - {@link GoodsOrderCreateRequest}
     * @return - id of created Goods Order
     */
    @Transactional
    public Long create(GoodsOrderCreateRequest request) {
        log.debug("Creating goods order: {}", request);

        List<String> errorMessages = new ArrayList<>();
        checkCustomerStatus(request.getBasicParameters().getCustomerDetailId(), errorMessages);
        validateGoodsTemplates(request.getBasicParameters(), errorMessages);
        validateGoodsOrderGoodsParametersAndThrowExceptionIfApplicable(request.getGoodsParameters(), errorMessages);
        GoodsOrder goodsOrder = basicParametersService.createGoodsOrderFromRequest(request, orderUtils.getNextOrderNumber(), errorMessages);
        validateAssistingEmployees(goodsOrder.getEmployeeId(), request.getBasicParameters().getAssistingEmployees(), errorMessages);
        goodsOrderRepository.saveAndFlush(goodsOrder);

        goodsParametersService.createGoodsOrderGoodsParameters(goodsOrder, request.getGoodsParameters(), errorMessages);
        basicParametersService.createSubObjects(request, goodsOrder, errorMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        GoodsOrder save = goodsOrderRepository.saveAndFlush(goodsOrder);
        if (goodsOrder.getCustomerDetailId() != null)
            customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(List.of(goodsOrder.getCustomerDetailId()));
        log.debug("Saving goods order entity");
        return save.getId();
    }

    private void validateGoodsTemplates(GoodsOrderBasicParametersCreateRequest basicParameters, List<String> errorMessages) {
        if (basicParameters.getEmailTemplateId() != null && !contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(basicParameters.getEmailTemplateId(), ContractTemplatePurposes.INVOICE, ContractTemplateType.EMAIL, LocalDate.now())) {
            errorMessages.add("basicParameters.emailTemplateId-Email template id was not found or has wrong purpose!;");
        }
        if (basicParameters.getInvoiceTemplateId() != null && !contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(basicParameters.getInvoiceTemplateId(), ContractTemplatePurposes.INVOICE, ContractTemplateType.DOCUMENT, LocalDate.now())) {
            errorMessages.add("basicParameters.invoiceTemplateId-Invoice template id was not found or has wrong purpose!;");
        }
    }

    private void validateGoodsOrderGoodsParametersAndThrowExceptionIfApplicable(GoodsOrderGoodsParametersRequest goodsParameters, List<String> errorMessages) {
        List<GoodsOrderGoodsParametersTableItem> goodsWithEmptyNumberOfIncome = goodsParameters.getGoods()
               .stream().filter(good -> good.getNumberOfIncomingAccount() == null || good.getNumberOfIncomingAccount().isEmpty()).toList();

        List<GoodsOrderGoodsParametersTableItem> goodsWithEmptyCostCenter = goodsParameters.getGoods()
                .stream().filter(good -> good.getCostCenterOrControllingOrder() == null || good.getCostCenterOrControllingOrder().isEmpty()).toList();

        if(!goodsWithEmptyNumberOfIncome.isEmpty() && (goodsParameters.getNumberOfIncomeAccount() == null || Objects.equals(goodsParameters.getNumberOfIncomeAccount(), ""))) {
            errorMessages.add("goodsParameters.numberOfIncomeAccount-Number is mandatory when one of the good numberOfIncomeAccount is empty;");
        }
        if(!goodsWithEmptyCostCenter.isEmpty() && (goodsParameters.getCostCenterOrControllingOrder() == null || Objects.equals(goodsParameters.getCostCenterOrControllingOrder(), ""))) {
            errorMessages.add("goodsParameters.getCostCenterOrControllingOrder-CostCenterOrControllingOrder is mandatory when one of the good CostCenterOrControllingOrder is empty;");
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
    }

    private void validateAssistingEmployees(Long employeeId, List<Long> assistingEmployees, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(assistingEmployees) && assistingEmployees.contains(employeeId)) {
            errorMessages.add("basicParameters.assistingEmployees-Assisting employee should not match employee;");
        }
    }

    private void checkCustomerStatus(Long customerDetailId, List<String> errorMessages) {
        if (customerDetailId != null) {
            Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findById(customerDetailId);
            if (customerDetailsOptional.isEmpty()) {
                throw new DomainEntityNotFoundException("Customer details not found;");
            } else {
                if (customerDetailsOptional.get().getStatus().equals(CustomerDetailStatus.POTENTIAL)) {
                    errorMessages.add("basicParameters.customerDetailId-not allowed create order with potential customer;");
                    EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
                }
            }
        }
    }

    /**
     * Edit Goods Order
     *
     * @param id      - Goods Order ID
     * @param request - {@link GoodsOrderEditRequest}
     * @return - ID of edited Goods Order
     */
    @Transactional
    public Long edit(Long id, GoodsOrderEditRequest request) {
        log.debug("Editing goods order: {}", request);
        List<String> errorMessages = new ArrayList<>();
        checkCustomerStatus(request.getBasicParameters().getCustomerDetailId(), errorMessages);
        GoodsOrder goodsOrder = goodsOrderRepository
                .findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Goods Order with presented id: [%s] not found;".formatted(id)));
        Long oldCustomerId = goodsOrder.getCustomerDetailId();

        validateUpdatePermissions(request, goodsOrder, errorMessages);
        goodsOrder.setOrderStatus(request.getOrderStatus());
        goodsOrder.setStatusModifyDate(request.getStatusModifyDate());

        basicParametersService.editGoodsOrderFromRequest(request, goodsOrder, errorMessages);
        goodsParametersService.editGoodsOrderFromRequest(request, goodsOrder, errorMessages);
        basicParametersService.updateSubObjects(request, goodsOrder, errorMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        goodsOrder = goodsOrderRepository.saveAndFlush(goodsOrder);
        Set<Long> customerDetailIDsTOCheck = new HashSet<>();
        if (oldCustomerId != null)
            customerDetailIDsTOCheck.add(oldCustomerId);
        if (goodsOrder.getCustomerDetailId() != null)
            customerDetailIDsTOCheck.add(goodsOrder.getCustomerDetailId());
        if (!customerDetailIDsTOCheck.isEmpty())
            customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerDetailIDsTOCheck.stream().toList());

        return goodsOrder.getId();
    }

    /**
     * Update Goods Order Status
     *
     * @param id                      - Goods Order ID
     * @param requestGoodsOrderStatus - Requested Status
     */
    @Transactional
    public Long updateStatus(Long id, GoodsOrderStatus requestGoodsOrderStatus, LocalDate targetStatusModifyDate) {
        log.debug("Updating goods order status with ID {}", id);

        GoodsOrder goodsOrder = goodsOrderRepository
                .findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Goods Order with presented id: [%s] not found;".formatted(id)));

        GoodsOrderStatus currentStatus = goodsOrder.getOrderStatus();

        if (!requestGoodsOrderStatus.equals(currentStatus)) {
            if (!GoodsOrderStatusChainUtil.canBeChangedManually(currentStatus, requestGoodsOrderStatus)) {
                log.error("Unable to change the goods order status from %s to %s".formatted(currentStatus, requestGoodsOrderStatus));
                throw new OperationNotAllowedException("Unable to change the goods order status from %s to %s".formatted(currentStatus, requestGoodsOrderStatus));
            }
        }

        goodsOrder.setOrderStatus(requestGoodsOrderStatus);
        goodsOrder.setStatusModifyDate(targetStatusModifyDate);
        goodsOrderRepository.save(goodsOrder);
        return id;
    }

    public Page<GoodsOrderListingResponse> list(GoodsOrderListingRequest request) {

        return goodsOrderRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        EPBStringUtils.fromPromptToQueryParameter(request.getGoodsName()),
                        Objects.requireNonNullElse(request.getGoodsIds(), new ArrayList<>()),
                        Objects.requireNonNullElse(request.getGoodsSupplierIds(), new ArrayList<>()),
                        request.getDateOfOrderCreationFrom(),
                        request.getDateOfOrderCreationTo(),
                        request.getInvoiceMaturityDateFrom(),
                        request.getInvoiceMaturityDateTo(),
                        InvoicedOptions.fromOptions(request.getInvoicePaid()),
                        request.getDirectDebit(),
                        Objects.requireNonNullElse(request.getAccountManagerIds(), new ArrayList<>()),
                        Objects.requireNonNullElse(request.getSearchBy(), GoodsOrderListingSearchFields.ALL).name(),
                        extractGoodsNameDirection(request),
                        extractGoodsSupplierDirection(request),
                        getStatusesForListing().stream().map(Enum::name).toList(),
                        extractPageable(request)
                ).map(GoodsOrderListingResponse::new);
    }

    /**
     * Deletes Goods Order by presented id
     *
     * @param id - Goods Order ID
     */
    @Transactional
    public void delete(Long id) {
        GoodsOrder goodsOrder = goodsOrderRepository
                .findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Goods Order With presented id: [%s] not found".formatted(id)));

        if (goodsOrderRepository.hasConnectionToActivity(id)) {
            log.error("Goods Order with id: {} has connection to activity.", id);
            throw new OperationNotAllowedException("You cannot delete goods order because it is connected to an activity.");
        }

        if (goodsOrderRepository.hasConnectionToProductContract(id)) {
            log.debug("Goods Order with id: {} has connection to Product Contract", id);
            throw new OperationNotAllowedException("You cannot delete goods order because it is connected to Product Contract.");
        }

        if (isLocked(goodsOrder)) {
            throw new OperationNotAllowedException("Can't delete because it is connected to invoice");
        }

        if (goodsOrderRepository.hasConnectionToServiceContract(id)) {
            log.debug("Goods Order with id: {} has connection to Service Contract", id);
            throw new OperationNotAllowedException("You cannot delete goods order because it is connected to Service Contract.");
        }

        if (goodsOrderRepository.hasConnectionToServiceOrder(id)) {
            log.debug("Goods Order with id: {} has connection to another Service Order", id);
            throw new OperationNotAllowedException("You cannot delete goods order because it is connected to another Service Order.");
        }

        if (goodsOrderRepository.hasConnectionToGoodsOrder(id)) {
            log.debug("Goods Order with id: {} has connection to Goods Order", id);
            throw new OperationNotAllowedException("You cannot delete goods order because it is connected to Goods Order.");
        }

        if (goodsOrderRepository.hasConnectionToTask(id)) {
            log.debug("Goods Order with id: {} has connection to Task", id);
            throw new OperationNotAllowedException("You cannot delete goods order because it is connected to Task.");
        }

        goodsOrder.setStatus(EntityStatus.DELETED);
        goodsOrderRepository.save(goodsOrder);
    }

    /**
     * List Goods for second tab
     *
     * @param request {@link GoodsSearchRequest}
     * @return - List of {@link GoodsSearchShortResponse}
     */
    public Page<GoodsSearchShortResponse> searchGoods(GoodsSearchRequest request) {
        return goodsDetailsRepository.searchGoodsForGoodsOrder(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getPrompt(),
                PageRequest.of(request.getPage(), request.getSize())
        );
    }

    /**
     * Validating presented XML for adding Goods Parameters manually
     *
     * @param file - XML Document
     * @return - List of {@link GoodsOrderGoodsParametersTableItem}
     * @throws bg.energo.phoenix.exception.ClientException if any violations happened
     */
    public List<GoodsOrderGoodsParametersTableItemResponse> uploadGoods(MultipartFile file) {
        return importService.validateFileContentAndMapToResponse(file);
    }

    private void validateUpdatePermissions(GoodsOrderEditRequest request, GoodsOrder goodsOrder, List<String> errorMessages) {
        GoodsOrderStatus currentGoodsOrderStatus = goodsOrder.getOrderStatus();

        if (!request.getOrderStatus().equals(goodsOrder.getOrderStatus())) {
            if (!hasPermission(PermissionEnum.GOODS_ORDER_EDIT_STATUS)) {
                log.error("You are not allowed to edit goods order status;");
                errorMessages.add("You are not allowed to edit goods order status;");
                return;
            }
        }

        if (isLocked(goodsOrder) && !hasPermission(PermissionEnum.GOODS_ORDER_EDIT_LOCKED)) {
            log.error("You are not allowed to edit locked goods order");
            errorMessages.add("You are not allowed to edit locked goods order");
            return;
        }

        if (currentGoodsOrderStatus.equals(GoodsOrderStatus.REQUESTED)) {
            if (!hasPermission(PermissionEnum.GOODS_ORDER_EDIT_REQUESTED)) {
                log.error("You are not allowed to edit goods order with status %s".formatted(currentGoodsOrderStatus));
                errorMessages.add("You are not allowed to edit goods order with status %s".formatted(currentGoodsOrderStatus));
                return;
            }
        }

        if (currentGoodsOrderStatus.equals(GoodsOrderStatus.CONFIRMED)) {
            if (!hasPermission(PermissionEnum.GOODS_ORDER_EDIT_CONFIRMED)) {
                log.error("You are not allowed to edit goods order with status %s".formatted(currentGoodsOrderStatus));
                errorMessages.add("You are not allowed to edit goods order with status %s".formatted(currentGoodsOrderStatus));
                return;
            }
        }

        if (!request.getOrderStatus().equals(goodsOrder.getOrderStatus())) {
            if (!GoodsOrderStatusChainUtil.canBeChangedManually(goodsOrder.getOrderStatus(), request.getOrderStatus())) {
                log.error("Unable to change the goods order status from %s to %s".formatted(goodsOrder.getOrderStatus(), request.getOrderStatus()));
                errorMessages.add("Unable to change the goods order status from %s to %s".formatted(goodsOrder.getOrderStatus(), request.getOrderStatus()));
            }
        }
    }

    private boolean isLocked(GoodsOrder goodsOrder) {
        return invoiceRepository.existsInvoiceByInvoiceStatusAndGoodsOrderId(InvoiceStatus.REAL, goodsOrder.getId());
    }

    private boolean hasPermission(PermissionEnum permission) {
        return permissionService
                .permissionContextContainsPermissions(PermissionContextEnum.GOODS_ORDERS,
                        List.of(permission));
    }

    private List<EntityStatus> getStatusesForListing() {
        List<EntityStatus> permittedStatuses = new ArrayList<>();

        if (permissionService
                .permissionContextContainsPermissions(PermissionContextEnum.GOODS_ORDERS, List.of(PermissionEnum.GOODS_ORDER_VIEW))) {
            permittedStatuses.add(EntityStatus.ACTIVE);
        }

        if (permissionService
                .permissionContextContainsPermissions(PermissionContextEnum.GOODS_ORDERS, List.of(PermissionEnum.GOODS_ORDER_VIEW_DELETED))) {
            permittedStatuses.add(EntityStatus.DELETED);
        }

        return permittedStatuses;
    }

    private Pageable extractPageable(GoodsOrderListingRequest request) {
        return PageRequest.of(request.getPage(),
                request.getSize(),
                JpaSort.unsafe(
                        Objects.requireNonNullElse(request.getDirection(), Sort.Direction.ASC),
                        Objects.requireNonNullElse(request.getSortBy(), GoodsOrderListingSortFields.NUMBER).getColumn()
                )
        );
    }

    private String extractGoodsNameDirection(GoodsOrderListingRequest request) {
        if (request.getSortBy() == null) {
            return Sort.Direction.ASC.name();
        }

        if (request.getSortBy().equals(GoodsOrderListingSortFields.GOODS)) {
            return Objects.requireNonNullElse(request.getDirection(), Sort.Direction.ASC).name();
        }

        return Sort.Direction.ASC.name();
    }

    private String extractGoodsSupplierDirection(GoodsOrderListingRequest request) {
        if (request.getSortBy() == null) {
            return Sort.Direction.ASC.name();
        }

        if (request.getSortBy().equals(GoodsOrderListingSortFields.GOODS_SUPPLIER)) {
            return Objects.requireNonNullElse(request.getDirection(), Sort.Direction.ASC).name();
        }

        return Sort.Direction.ASC.name();
    }

    /**
     * Download Goods Order Goods upload template
     *
     * @return XML template
     */
    public GoodsOrderGoodsTemplateContent downloadGoodsOrderGoodsTemplate() {
        Template template = templateRepository
                .findById(EPBFinalFields.IMPORT_TEMPLATE_ID)
                .orElseThrow(() -> new DomainEntityNotFoundException("Template for Goods Order Goods not found;"));

        return new GoodsOrderGoodsTemplateContent(template.getTemplateName(), fileService.downloadFile(template.getFileUrl()));
    }

    public List<TaskShortResponse> getTasksById(Long id) {
        return basicParametersService.getTasks(id);
    }


    /**
     * Retrieves all activities for a goods order.
     *
     * @param id ID of the goods order
     * @return List of {@link SystemActivityShortResponse}
     */
    public List<SystemActivityShortResponse> getActivitiesById(Long id) {
        return goodsOrderActivityService.getActivitiesByConnectedObjectId(id);
    }

    public Page<OrderInvoiceViewResponse> viewGeneratedInvoiceForDraftInvoicesTab(Long id, OrderInvoicesRequest request) {
        return invoiceRepository.getInvoiceByOrderId(id,
                OrderType.GOODS_ORDER.name(),
                EPBStringUtils.fromPromptToQueryParameter(request.prompt()),
                true,
                PageRequest.of(request.page(), request.size())
        );
    }

    public Page<OrderInvoiceViewResponse> viewGeneratedInvoiceForPdfTab(Long id, OrderInvoicesRequest request) {
        return invoiceRepository.getInvoiceByOrderId(id,
                OrderType.GOODS_ORDER.name(),
                EPBStringUtils.fromPromptToQueryParameter(request.prompt()),
                false,
                PageRequest.of(request.page(), request.size())
        );
    }
}
