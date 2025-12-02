package bg.energo.phoenix.service.product.goods;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsGroups;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsSuppliers;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsUnits;
import bg.energo.phoenix.model.entity.product.goods.*;
import bg.energo.phoenix.model.enums.copy.group.CopyDomainWithVersion;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.*;
import bg.energo.phoenix.model.request.copy.group.CopyDomainWithVersionBaseRequest;
import bg.energo.phoenix.model.request.product.goods.CreateGoodsRequest;
import bg.energo.phoenix.model.request.product.goods.edit.EditGoodsRequest;
import bg.energo.phoenix.model.request.product.goods.edit.GoodsSalesAreaEditRequest;
import bg.energo.phoenix.model.request.product.goods.edit.GoodsSalesChannelsEditRequest;
import bg.energo.phoenix.model.request.product.goods.edit.GoodsSegmentsEditRequest;
import bg.energo.phoenix.model.request.product.product.GoodsListRequest;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import bg.energo.phoenix.model.response.goods.*;
import bg.energo.phoenix.model.response.product.goods.GoodsListResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import bg.energo.phoenix.repository.nomenclature.product.goods.GoodsGroupsRepository;
import bg.energo.phoenix.repository.nomenclature.product.goods.GoodsSuppliersRepository;
import bg.energo.phoenix.repository.nomenclature.product.goods.GoodsUnitsRepository;
import bg.energo.phoenix.repository.product.goods.*;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.copy.group.CopyDomainWithVersionBaseService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.ACTIVE;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;
import static bg.energo.phoenix.permissions.PermissionContextEnum.GOODS;
import static bg.energo.phoenix.permissions.PermissionEnum.GOODS_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.GOODS_VIEW_DELETED;


@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsService implements CopyDomainWithVersionBaseService {
    private final PermissionService permissionService;

    private final GoodsRepository goodsRepository;
    private final GoodsDetailsService goodsDetailsService;
    private final GoodsSalesAreasService goodsSalesAreasService;
    private final GoodsSalesChannelsService goodsSalesChannelsService;
    private final GoodsSegmentsService goodsSegmentsService;
    private final GoodsDetailsRepository goodsDetailsRepository;
    private final GoodsSalesAreasRepository goodsSalesAreasRepository;
    private final GoodsSalesChannelsRepository goodsSalesChannelsRepository;
    private final GoodsSegmentsRepository goodsSegmentsRepository;
    private final GoodsGroupsRepository goodsGroupsRepository;
    private final GoodsSuppliersRepository goodsSuppliersRepository;
    private final CurrencyRepository currencyRepository;
    private final GoodsUnitsRepository goodsUnitsRepository;
    private final VatRateRepository vatRateRepository;


    /**
     * Creates a new {@link Goods} object with the provided data if validations are passed.
     *
     * @param request {@link CreateGoodsRequest} object with data
     * @return {@link GoodsResponse} object
     */
    @Transactional
    public GoodsResponse create(CreateGoodsRequest request) {
        log.debug("Creating Goods with the following request: {}", request);

        List<String> exceptionMessages = new ArrayList<>();
        Goods goods = createGoods();
        GoodsDetails goodsDetails = goodsDetailsService.createGoodsDetails(request, goods, exceptionMessages);

        if (request.getSalesAreasIds() != null) {
            goodsSalesAreasService.createGoodsSalesAreas(request.getSalesAreasIds(), goodsDetails, exceptionMessages);
        }

        if (request.getSalesChannelsIds() != null) {
            goodsSalesChannelsService.createGoodsSalesChannels(request.getSalesChannelsIds(), goodsDetails, exceptionMessages);
        }

        if (request.getSegmentsIds() != null) {
            goodsSegmentsService.createGoodsSegments(request.getSegmentsIds(), goodsDetails, exceptionMessages);
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        goods.setLastGoodsDetailsId(goodsDetails.getId());

        return new GoodsResponse(goodsRepository.save(goods));
    }


    /**
     * function creates Goods object with the {@link GoodsStatus#ACTIVE} status and saves it in database
     */
    private Goods createGoods() {
        Goods goods = new Goods();
        goods.setGoodsStatusEnum(GoodsStatus.ACTIVE);
        return goodsRepository.saveAndFlush(goods);
    }


    /**
     * Returns a page of {@link GoodsListResponse} objects based on the criteria specified in the provided {@link GoodsListRequest}.
     * The method also checks the permissions of the current user before fetching the goods list.
     *
     * @param request the {@link GoodsListRequest} object containing the search, sort, and pagination parameters
     * @return a Page of {@link GoodsListResponse} objects containing information about the goods that match the search criteria
     */
    public Page<GoodsListResponse> list(GoodsListRequest request) {
        log.debug("Fetching goods list for the following request: {}", request);

        String salesChannelsDirection = null;

        String sortBy = GoodsTableColumn.G_ID.getValue();
        if (request.getSortBy() != null && StringUtils.isNotEmpty(request.getSortBy().getValue())) {
            sortBy = request.getSortBy().getValue();
            if (sortBy.equals(GoodsTableColumn.GD_SALES_CHANNELS.getValue())) {
                if (request.getSortDirection() != null) {
                    salesChannelsDirection = request.getSortDirection().name();
                }
            }
        }

        String searchBy = GoodsSearchField.ALL.getValue();
        if (request.getSearchBy() != null && StringUtils.isNotEmpty(request.getSearchBy().getValue())) {
            searchBy = request.getSearchBy().getValue();
        }

        Sort sortOrder = Sort.by(new Sort.Order(
                Objects.requireNonNullElse(request.getSortDirection(), Sort.Direction.DESC),
                sortBy
        ));

        String excludeOldVersion = String.valueOf(Boolean.TRUE.equals(request.getExcludeOldVersions()));

        return goodsRepository
                .findAll(
                        searchBy,
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        CollectionUtils.isNotEmpty(request.getGoodsDetailStatuses()) ? request.getGoodsDetailStatuses() : null,
                        CollectionUtils.isNotEmpty(request.getGroupIds()) ? request.getGroupIds() : null,
                        CollectionUtils.isNotEmpty(request.getSupplierIds()) ? request.getSupplierIds() : null,
                        CollectionUtils.isNotEmpty(request.getSalesChannelsIds()) ? request.getSalesChannelsIds() : null,
                        CollectionUtils.isNotEmpty(request.getSegmentIds()) ? request.getSegmentIds() : null,
                        getStatusesByPermission(),
                        request.getGlobalSalesChannel(),
                        request.getGlobalSegment(),
                        salesChannelsDirection,
                        excludeOldVersion,
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                sortOrder
                        )
                );
    }


    /**
     * if version is not provided - then it retrieves the latest version of {@link GoodsDetails} object
     * if there is version it takes {@link GoodsDetails} accordingly.
     *
     * @param id      id of the goods
     * @param version version of the goods
     * @return {@link GoodsViewResponse} object
     */
    public GoodsViewResponse view(Long id, Long version) {
        log.debug("Viewing Goods with id: {}", id);

        GoodsDetails goodsDetails;
        Goods goods = goodsRepository
                .findByIdAndGoodsStatusEnumIn(id, getStatusesByPermission())
                .orElseThrow(() -> new DomainEntityNotFoundException("Goods not found with this id: " + id));

        if (version != null) {
            goodsDetails = goodsDetailsRepository
                    .findByGoodsIdAndVersionId(id, version)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Goods Details with this version id not found;"));
        } else {
            goodsDetails = goodsDetailsRepository
                    .findFirstByGoodsId(id, Sort.by(Sort.Direction.DESC, "createDate"))
                    .orElseThrow(() -> new DomainEntityNotFoundException("Goods Details not found;"));
        }

        List<GoodsVersionsResponse> versions = goodsDetailsRepository.getVersions(goods.getId(), List.of(GoodsDetailStatus.ACTIVE, GoodsDetailStatus.INACTIVE));
        List<GoodsSalesAreas> goodsSalesAreasList = goodsSalesAreasRepository.findByGoodsDetails_IdAndStatus(goodsDetails.getId(), GoodsSubObjectStatus.ACTIVE);
        List<GoodsSalesChannels> goodsSalesChannelsList = goodsSalesChannelsRepository.findByGoodsDetails_IdAndStatus(goodsDetails.getId(), GoodsSubObjectStatus.ACTIVE);
        List<GoodsSegments> goodsSegmentsList = goodsSegmentsRepository.findByGoodsDetails_IdAndStatus(goodsDetails.getId(), GoodsSubObjectStatus.ACTIVE);

        return new GoodsViewResponse(new GoodsResponse(goods), new GoodsDetailsResponse(goodsDetails, versions), goodsSalesAreasList.stream().map(GoodsSalesAreaResponse::new).toList(), goodsSalesChannelsList.stream().map(GoodsSalesChannelsResponse::new).toList(), goodsSegmentsList.stream().map(GoodsSegmentsResponse::new).toList(),
                checkForBoundObjectsForPreview(goodsDetails));
    }


    /**
     * @return list of {@link GoodsStatus} according to the permissions of the user
     */
    private List<GoodsStatus> getStatusesByPermission() {
        List<GoodsStatus> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(GOODS, List.of(GOODS_VIEW_BASIC))) {
            statuses.add(GoodsStatus.ACTIVE);
        }
        if (permissionService.permissionContextContainsPermissions(GOODS, List.of(GOODS_VIEW_DELETED))) {
            statuses.add(GoodsStatus.DELETED);
        }
        return statuses;
    }


    /**
     * Deletes a {@link Goods} object with the provided id if validations are passed.
     *
     * @param id id of a record to delete
     * @return deleted record
     */
    public Long delete(Long id) {
        log.debug("Deleting Goods with id: {}", id);

        Goods goods = goodsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Goods not found by Id: " + id + ";"));

        if (goods.getGoodsStatusEnum().equals(GoodsStatus.DELETED)) {
            log.error("Goods is already deleted;");
            throw new ClientException("Goods is already deleted;", ErrorCode.APPLICATION_ERROR);
        }

        if (goodsRepository.hasActiveConnectionToGoodsOrder(id)) {
            log.error("You can’t delete the goods with ID: [%s] because it is connected to the Goods Order".formatted(id));
            throw new OperationNotAllowedException("You can’t delete the goods with ID: [%s] because it is connected to the Goods Order".formatted(id));
        }

        goods.setGoodsStatusEnum(GoodsStatus.DELETED);
        goodsRepository.save(goods);
        return goods.getId();
    }

    /**
     * Creates a new version or updates an existing one of a {@link Goods} object with the provided id if validations are passed.
     *
     * @param id      id of the goods
     * @param request {@link EditGoodsRequest} object with data
     * @return id of the updated goods object
     */
    @Transactional
    public Long edit(Long id, EditGoodsRequest request) {
        log.debug("Editing Goods with id: {}", id);
        List<String> exceptionMessages = new ArrayList<>();

        Goods goods = goodsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Goods with this id: " + id + " not exists;"));

        if (goods.getGoodsStatusEnum().equals(GoodsStatus.DELETED)) {
            log.error("Unable to edit Deleted Goods;");
            throw new ClientException("Unable to edit Deleted Goods;", ErrorCode.APPLICATION_ERROR);
        }

        GoodsDetails goodsDetails = getGoodsDetails(id, request.getVersionId());
        if (request.getUpdateExistingVersion()) {
            return updateExistingVersion(request, exceptionMessages, goods, goodsDetails);
        } else {
            return createNewVersion(request, exceptionMessages, goods, goodsDetails);
        }
    }

    /**
     * function creates new version of goods details and subObjects
     *
     * @param request           {@link EditGoodsRequest} object with data
     * @param exceptionMessages list of exception messages to be populated in case of errors
     * @param goods             {@link Goods} object
     * @param goodsDetails      {@link GoodsDetails} object
     * @return id of the new version
     */
    private Long createNewVersion(EditGoodsRequest request, List<String> exceptionMessages, Goods goods, GoodsDetails goodsDetails) {
        GoodsDetails newGoodsDetails = goodsDetailsService.createGoodsDetails(request, goods, goodsDetails, exceptionMessages);


        if (newGoodsDetails == null) {
            log.error("Cant create new version of goodsDetails: " + goodsDetails.getId() + ";");
            exceptionMessages.add("Cant create new version of goodsDetails: " + goodsDetails.getId() + ";");
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        if (!request.getGlobalVatRate()) {
            newGoodsDetails.setGlobalVatRate(false);

            VatRate oldVatRate = goodsDetails.getVatRate();
            List<NomenclatureItemStatus> statusList = (oldVatRate != null && oldVatRate.getId().equals(request.getVatRateId()))
                    ? List.of(ACTIVE, NomenclatureItemStatus.INACTIVE)
                    : List.of(ACTIVE);

            Optional<VatRate> requestedVatRateOptional = fetchVatRate(request.getVatRateId(), statusList);

            if (requestedVatRateOptional.isPresent()) {
                newGoodsDetails.setVatRate(requestedVatRateOptional.get());
            } else {
                exceptionMessages.add("vatRateId-Vat rate with presented id: [%s] not found".formatted(request.getVatRateId()));
            }
        } else {
            newGoodsDetails.setGlobalVatRate(true);
            newGoodsDetails.setVatRate(null);
        }

        if (!request.getGlobalSalesArea()) {
            newGoodsDetails.setGlobalSalesArea(false);
            goodsSalesAreasService.createGoodsSalesAreasWithEqualsCheck(goodsDetails, request.getSalesAreasIds().stream().map(GoodsSalesAreaEditRequest::getSalesAreaId).collect(Collectors.toList()), newGoodsDetails, exceptionMessages);
        } else {
            newGoodsDetails.setGlobalSalesArea(true);
        }

        if (!request.getGlobalSalesChannel()) {
            newGoodsDetails.setGlobalSalesChannel(false);
            goodsSalesChannelsService.createGoodsSalesChannelsEqualsCheck(goodsDetails, request.getSalesChannelsIds().stream().map(GoodsSalesChannelsEditRequest::getSalesChannelsId).collect(Collectors.toList()), newGoodsDetails, exceptionMessages);
        } else {
            newGoodsDetails.setGlobalSalesChannel(true);
        }

        if (!request.getGlobalSegment()) {
            newGoodsDetails.setGlobalSegment(false);
            goodsSegmentsService.createGoodsSegmentsWithEqualsCheck(goodsDetails, request.getSegmentsIds().stream().map(GoodsSegmentsEditRequest::getSegmentId).collect(Collectors.toList()), newGoodsDetails, exceptionMessages);
        } else {
            newGoodsDetails.setGlobalSegment(true);
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        goods.setLastGoodsDetailsId(newGoodsDetails.getId());
        goodsRepository.save(goods);
        return goods.getId();
    }

    /**
     * function updates version of goods details and subObjects
     *
     * @param request           {@link EditGoodsRequest} object with data
     * @param exceptionMessages list of exception messages to be populated in case of errors
     * @param goods             {@link Goods} object
     * @param goodsDetails      {@link GoodsDetails} object
     * @return id of the existing version
     */
    private Long updateExistingVersion(EditGoodsRequest request, List<String> exceptionMessages, Goods goods, GoodsDetails goodsDetails) {
        checkForBoundObjects(goodsDetails);
        editGoodsDetails(request, goodsDetails, exceptionMessages);
        fillSubObjects(goods, goodsDetails, request, exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        return goods.getId();
    }

    private void checkForBoundObjects(GoodsDetails goodsDetails) {
        List<GoodsDetails> boundGoods = goodsDetailsRepository.checkForBoundObjects(goodsDetails.getId());//TODO SQL NEEDED
        if (CollectionUtils.isNotEmpty(boundGoods)) {
            if (!checkIfHasLockedPermission()) {
                throw new ClientException("You can't edit Goods because it is connected to the Goods Order;", ErrorCode.CONFLICT);
            }
        }
    }

    private boolean checkForBoundObjectsForPreview(GoodsDetails goodsDetails) {
        List<GoodsDetails> boundGoods = goodsDetailsRepository.checkForBoundObjects(goodsDetails.getId());//TODO SQL NEEDED
        return CollectionUtils.isNotEmpty(boundGoods);
    }

    private boolean checkIfHasLockedPermission() {
        List<String> customerContext = permissionService.getPermissionsFromContext(GOODS);
        return customerContext.contains(PermissionEnum.GOODS_EDIT_LOCKED.getId());
    }

    /**
     * function map data from  {@link EditGoodsRequest} to {@link GoodsDetails} object
     * and then it saves {@link GoodsDetails} to the database
     *
     * @param request           {@link EditGoodsRequest} object with data
     * @param goodsDetails      {@link GoodsDetails} object
     * @param exceptionMessages
     */
    private void editGoodsDetails(EditGoodsRequest request, GoodsDetails goodsDetails, List<String> exceptionMessages) {
//        GoodsDetailsEditRequest requestGoodsDetails = createDetailsFromRequest(request);
        goodsDetails.setName(request.getName().trim());
        goodsDetails.setNameTransl(request.getNameTransl().trim());
        goodsDetails.setPrintingName(request.getPrintingName().trim());
        goodsDetails.setPrintingNameTransl(request.getPrintingNameTransl().trim());
        goodsDetails.setGoodsGroups(getGoodsGroupsById(request.getGoodsGroupsId(), goodsDetails.getGoodsGroups(), exceptionMessages));
        goodsDetails.setOtherSystemConnectionCode(request.getOtherSystemConnectionCode() == null ? null : request.getOtherSystemConnectionCode().trim());
        goodsDetails.setGoodsSuppliers(getGoodsSuppliers(request.getGoodsSuppliersId(), goodsDetails.getGoodsSuppliers(), exceptionMessages));
        goodsDetails.setManufacturerCodeNumber(request.getManufacturerCodeNumber() == null ? null : request.getManufacturerCodeNumber().trim());
        goodsDetails.setStatus(request.getGoodsDetailStatus());
        goodsDetails.setPrice(request.getPrice());
        goodsDetails.setCurrency(getCurrency(request.getCurrencyId(), goodsDetails.getCurrency(), exceptionMessages));
        goodsDetails.setGoodsUnits(getGoodsUnits(request.getGoodsUnitId(), goodsDetails.getGoodsUnits(), exceptionMessages));
        goodsDetails.setVatRate(request.getVatRateId() == null ? null : getVatRate(request.getVatRateId(), goodsDetails.getVatRate(), exceptionMessages));
        goodsDetails.setIncomeAccountNumbers(request.getIncomeAccountNumbers() == null ? null : request.getIncomeAccountNumbers().trim());
        goodsDetails.setControllingOrderId(request.getControllingOrderId() == null ? null : request.getControllingOrderId().trim());
        goodsDetails.setGlobalVatRate(request.getGlobalVatRate());
        goodsDetails.setGlobalSalesArea(request.getGlobalSalesArea());
        goodsDetails.setGlobalSalesChannel(request.getGlobalSalesChannel());
        goodsDetails.setGlobalSegment(request.getGlobalSegment());
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        goodsDetailsRepository.save(goodsDetails);
    }

    /**
     * function gets vatRate nomenclature by id
     *
     * @param vatRateId         id of the vatRate
     * @param vatRate
     * @param exceptionMessages
     * @return {@link VatRate}
     */
    private VatRate getVatRate(Long vatRateId, VatRate vatRate, List<String> exceptionMessages) {
        if (vatRate != null) {
            if (vatRate.getId().equals(vatRateId)) {
                Optional<VatRate> vatRateOptional = vatRateRepository.findByIdAndStatus(vatRateId, List.of(ACTIVE, INACTIVE));
                if (vatRateOptional.isPresent()) {
                    return vatRateOptional.get();
                } else {
                    log.error("Can't find active or inactive nomenclature with id:%s;".formatted(vatRateId));
                    exceptionMessages.add("vatRateId-Can't find active or inactive nomenclature with id:%s;".formatted(vatRateId));
                    return null;
                }
            } else {
                return getVatRateFromDb(vatRateId, exceptionMessages);
            }
        } else {
            return getVatRateFromDb(vatRateId, exceptionMessages);
        }
    }

    public VatRate getVatRateFromDb(Long vatRateId, List<String> exceptionMessages) {
        Optional<VatRate> vatRateOptional = vatRateRepository.findByIdAndStatus(vatRateId, List.of(ACTIVE));
        if (vatRateOptional.isPresent()) {
            return vatRateOptional.get();
        } else {
            log.error("Vat Rate with this id " + vatRateId + " doesn't exists;");
            exceptionMessages.add("vatRateId-Can't find active nomenclature with id:%s;".formatted(vatRateId));
            return null;
        }
    }

    private Optional<VatRate> fetchVatRate(Long vatRateId, List<NomenclatureItemStatus> statuses) {
        return vatRateRepository
                .findByIdAndStatus(vatRateId, statuses);
    }

    /**
     * function gets goodsUnits nomenclature by id
     *
     * @param goodsUnitsId      id of the goodsUnits
     * @param goodsUnits
     * @param exceptionMessages
     * @return {@link GoodsUnits}   object
     */
    private GoodsUnits getGoodsUnits(Long goodsUnitsId, GoodsUnits goodsUnits, List<String> exceptionMessages) {
        if (goodsUnits.getId().equals(goodsUnitsId)) {
            Optional<GoodsUnits> goodsUnitsOptional = goodsUnitsRepository.findByIdAndStatus(goodsUnitsId, List.of(ACTIVE, INACTIVE));
            if (goodsUnitsOptional.isPresent()) {
                return goodsUnitsOptional.get();
            } else {
                log.error("goodsUnitId-Can't find active or inactive goodsUnits with id:%s".formatted(goodsUnitsId));
                exceptionMessages.add("goodsUnitId-Can't find active or inactive goodsUnits with id:%s".formatted(goodsUnitsId));
                return null;
            }
        } else {
            Optional<GoodsUnits> goodsUnitsOptional = goodsUnitsRepository.findByIdAndStatus(goodsUnitsId, List.of(ACTIVE));
            if (goodsUnitsOptional.isPresent()) {
                return goodsUnitsOptional.get();
            } else {
                log.error("Goods Units with this id " + goodsUnitsId + " doesn't exists;");
                exceptionMessages.add("goodsUnitId-Goods Units with this id " + goodsUnitsId + " doesn't exists;");
                return null;
            }
        }
    }

    /**
     * function gets currency nomenclature by id
     *
     * @param currencyId        id of the currency
     * @param currency
     * @param exceptionMessages
     * @return {@link Currency} object
     */
    private Currency getCurrency(Long currencyId, Currency currency, List<String> exceptionMessages) {
        if (currency.getId().equals(currencyId)) {
            Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
            if (currencyOptional.isPresent()) {
                return currencyOptional.get();
            } else {
                log.error("Active or inactive currency with this id " + currencyId + " doesn't exists;");
                exceptionMessages.add("currencyId-Active or inactive currency currency with this id " + currencyId + " doesn't exists;");
                return null;
            }
        } else {
            Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE));
            if (currencyOptional.isPresent()) {
                return currencyOptional.get();
            } else {
                log.error("Currency with this id " + currencyId + " doesn't exists;");
                exceptionMessages.add("currencyId-Currency with this id " + currencyId + " doesn't exists;");
                return null;
            }
        }
    }

    /**
     * function gets GoodsSuppliers nomenclature by id
     *
     * @param goodsSuppliersId  id of the goodsSuppliers
     * @param goodsSuppliers
     * @param exceptionMessages
     * @return {@link GoodsSuppliers}   object
     */
    private GoodsSuppliers getGoodsSuppliers(Long goodsSuppliersId, GoodsSuppliers goodsSuppliers, List<String> exceptionMessages) {
        if (goodsSuppliers.getId().equals(goodsSuppliersId)) {
            Optional<GoodsSuppliers> goodsSuppliersOptional = goodsSuppliersRepository.findByIdAndStatus(goodsSuppliersId, List.of(ACTIVE, INACTIVE));
            if (goodsSuppliersOptional.isPresent()) {
                return goodsSuppliersOptional.get();
            } else {
                log.error("Goods suppliers with this id " + goodsSuppliersId + " doesn't exists;");
                exceptionMessages.add("goodsSuppliersId-Can't find active or inactive goods suppliers with id:%s;".formatted(goodsSuppliersId));
                return null;
            }
        } else {
            Optional<GoodsSuppliers> goodsSuppliersOptional = goodsSuppliersRepository.findByIdAndStatus(goodsSuppliersId, List.of(ACTIVE));
            if (goodsSuppliersOptional.isPresent()) {
                return goodsSuppliersOptional.get();
            } else {
                log.error("Goods suppliers with this id " + goodsSuppliersId + " doesn't exists;");
                exceptionMessages.add("Goods suppliers with this id " + goodsSuppliersId + " doesn't exists;");
                return null;
            }
        }
    }

    /**
     * <h1>getGoodsGroupsById</h1>
     * function gets GoodsGroups nomenclature by id
     * if there is no data throws exception
     *
     * @param goodsGroupsId     id of the goodsGroups
     * @param goodsGroups
     * @param exceptionMessages
     * @return {@link GoodsGroups}  object
     */
    private GoodsGroups getGoodsGroupsById(Long goodsGroupsId, GoodsGroups goodsGroups, List<String> exceptionMessages) {
        if (goodsGroups.getId().equals(goodsGroupsId)) {
            Optional<GoodsGroups> goodsGroupsOptional = goodsGroupsRepository.findByIdAndStatus(goodsGroupsId, List.of(ACTIVE, INACTIVE));
            if (goodsGroupsOptional.isPresent()) {
                return goodsGroupsOptional.get();
            } else {
                log.error("Can't find active or inactive goods groups nomenclature with id:%s;".formatted(goodsGroupsId));
                exceptionMessages.add("goodsGroupsId-Can't find active or inactive goods groups nomenclature with id:%s;".formatted(goodsGroupsId));
                return null;
            }
        } else {
            Optional<GoodsGroups> goodsGroupsOptional = goodsGroupsRepository.findById(goodsGroupsId);
            if (goodsGroupsOptional.isPresent()) {
                return goodsGroupsOptional.get();
            } else {
                log.error("Goods groups with this id " + goodsGroupsId + " doesn't exists;");
                exceptionMessages.add("Goods groups with this id " + goodsGroupsId + " doesn't exists;");
                return null;
            }
        }
    }

    /**
     * function calls services of subObjects and creates them
     *
     * @param goods             {@link Goods} object
     * @param goodsDetails      {@link GoodsDetails} object
     * @param request           {@link EditGoodsRequest} object with data
     * @param exceptionMessages list of exception messages to be populated in case of errors
     */
    @Transactional
    public void fillSubObjects(Goods goods, GoodsDetails goodsDetails, EditGoodsRequest request, List<String> exceptionMessages) {
        goodsSalesAreasService.createOrEditGoodsSalesAreas(goods, goodsDetails, request, exceptionMessages);
        goodsSalesChannelsService.createOrEditGoodsSalesChannels(goods, goodsDetails, request, exceptionMessages);
        goodsSegmentsService.createOrEditGoodsSegments(goods, goodsDetails, request, exceptionMessages);
    }

    /**
     * function gets goodsDetails object
     *
     * @param id        id of the goods
     * @param versionId version of the goods
     */
    private GoodsDetails getGoodsDetails(Long id, Long versionId) {
        Optional<GoodsDetails> goodsDetailsOptional = goodsDetailsRepository.findByGoodsIdAndVersionId(id, versionId);
        if (goodsDetailsOptional.isPresent()) {
            return goodsDetailsOptional.get();
        } else {
            throw new ClientException("cant find GoodsDetails by id and version;", ErrorCode.APPLICATION_ERROR);
        }
    }

    @Override
    public CopyDomainWithVersion getGroupType() {
        return CopyDomainWithVersion.GOODS;
    }

    @Override
    public Page<CopyDomainWithVersionBaseResponse> findGroups(CopyDomainWithVersionBaseRequest request) {
        log.debug("Fetching goods list for the following request: {}", request);
        return goodsRepository.findByCopyDomainWithVersionBaseRequestAdnStatusIn(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                List.of(GoodsStatus.ACTIVE),
                PageRequest.of(request.getPage(), request.getSize())
        );
    }

    @Override
    public List<CopyDomainWithVersionMiddleResponse> findGroupVersions(Long goodsId) {
        return goodsDetailsRepository.findByGoodsId(goodsId);

    }

    public GoodsViewResponse viewForCopy(Long goodsId, Long version) {

        Goods goods = goodsRepository.findByIdAndGoodsStatusEnumIn(goodsId, List.of(GoodsStatus.ACTIVE))
                .orElseThrow(() -> new ClientException("Goods not found by id:" + goodsId + ";", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        GoodsDetails goodsDetails = goodsDetailsRepository.findByGoodsIdAndVersionId(goodsId, version)
                .orElseThrow(() -> new ClientException("Goods Details not found by goods id: " + goodsId + " and version: " + version + ";", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        List<GoodsVersionsResponse> goodsVersionsResponseList = goodsDetailsRepository.getVersions(goodsId, List.of(GoodsDetailStatus.ACTIVE));

        List<GoodsSalesAreaResponse> goodsSalesAreaResponses = goodsSalesAreasRepository.findByGoodsDetailsIdAndStatusInAndWithActiveSubObjects(goodsDetails.getId(), List.of(GoodsSubObjectStatus.ACTIVE), List.of(ACTIVE))
                .stream()
                .map(GoodsSalesAreaResponse::new)
                .toList();

        List<GoodsSalesChannelsResponse> goodsSalesChannelsResponses = goodsSalesChannelsRepository.findByGoodsDetailsIdAndStatusInAndWithActiveSubObjects(goodsDetails.getId(), List.of(GoodsSubObjectStatus.ACTIVE), List.of(ACTIVE))
                .stream()
                .map(GoodsSalesChannelsResponse::new)
                .toList();

        List<GoodsSegmentsResponse> goodsSegmentsResponses = goodsSegmentsRepository.findByGoodsDetailsIdAndStatusInAndWithActiveSubObjects(goodsDetails.getId(), List.of(GoodsSubObjectStatus.ACTIVE), List.of(ACTIVE))
                .stream()
                .map(GoodsSegmentsResponse::new)
                .toList();

        GoodsDetailsResponse goodsDetailsResponseForCopy = goodsDetailsResponseForCopy(new GoodsDetailsResponse(goodsDetails, goodsVersionsResponseList));
        if (goodsDetailsResponseForCopy.getGlobalVatRate() && !vatRateRepository.existsByGlobalVatRateAndStatusIn(true, List.of(ACTIVE))) {
            goodsDetailsResponseForCopy.setGlobalVatRate(false);
        }
        return new GoodsViewResponse(
                new GoodsResponse(goods),
                goodsDetailsResponseForCopy,
                goodsSalesAreaResponses,
                goodsSalesChannelsResponses,
                goodsSegmentsResponses,
                null
        );
    }

    private GoodsDetailsResponse goodsDetailsResponseForCopy(GoodsDetailsResponse initial) {
        if (initial.getGoodsGroups() != null && initial.getGoodsGroups().getStatus() != ACTIVE)
            initial.setGoodsGroups(null);
        if (initial.getGoodsSuppliers() != null && initial.getGoodsSuppliers().getStatus() != ACTIVE)
            initial.setGoodsSuppliers(null);
        if (initial.getCurrency() != null && initial.getCurrency().getStatus() != ACTIVE)
            initial.setCurrency(null);
        if (initial.getGoodsUnits() != null && initial.getGoodsUnits().getStatus() != ACTIVE)
            initial.setGoodsUnits(null);
        if (initial.getVatRate() != null && initial.getVatRate().getStatus() != ACTIVE)
            initial.setVatRate(null);
        return initial;
    }
}
