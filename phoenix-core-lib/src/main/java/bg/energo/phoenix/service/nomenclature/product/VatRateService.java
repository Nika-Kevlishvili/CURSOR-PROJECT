package bg.energo.phoenix.service.nomenclature.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.VatRateRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.VAT_RATES;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class VatRateService implements NomenclatureBaseService {
    private final VatRateRepository vatRateRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.VAT_RATES;
    }

    /**
     * Filters {@link VatRate} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link VatRate}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = VAT_RATES, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering vat rates list with statuses: {}", request);
        return vatRateRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link VatRate} item in the vat rate list to a specified position.
     * The method retrieves the {@link VatRate} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link VatRate} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link VatRate} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = VAT_RATES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of Vat rates item with ID: {} to place {}", request.getId(), request.getOrderingId());
        VatRate vatRate = vatRateRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate not found"));

        Long start;
        Long end;
        List<VatRate> vatRates;

        if (vatRate.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = vatRate.getOrderingId();
            vatRates = vatRateRepository.findInOrderingIdRange(start, end, vatRate.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (VatRate b : vatRates) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = vatRate.getOrderingId();
            end = request.getOrderingId();
            vatRates = vatRateRepository.findInOrderingIdRange(start, end, vatRate.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (VatRate b : vatRates) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        vatRate.setOrderingId(request.getOrderingId());
        vatRates.add(vatRate);
        vatRateRepository.saveAll(vatRates);
    }

    /**
     * Sorts all {@link VatRate} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = VAT_RATES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the vat rates alphabetically");
        List<VatRate> vatRates = vatRateRepository.orderByName();
        long orderingId = 1;

        for (VatRate b : vatRates) {
            b.setOrderingId(orderingId);
            orderingId++;
        }
        vatRateRepository.saveAll(vatRates);
    }

    /**
     * Deletes {@link VatRate} if the validations are passed.
     *
     * @param id ID of the {@link VatRate}
     * @throws DomainEntityNotFoundException if {@link VatRate} is not found.
     * @throws OperationNotAllowedException  if the {@link VatRate} is already deleted.
     * @throws OperationNotAllowedException  if the {@link VatRate} is last Global vat rate.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = VAT_RATES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing vat Rate  with ID: {}", id);
        VatRate vatRate = vatRateRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Vat rate not found"));

        if (vatRate.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("Item is already deleted.");
        }
        Long globalVatRatesQuantity = vatRateRepository.countByGlobalVatRateAndStatus();
        if(globalVatRatesQuantity < 2 && vatRate.getGlobalVatRate() && vatRate.getStatus() == NomenclatureItemStatus.ACTIVE){
            log.error("Cannot delete last global vat rate");
            throw new OperationNotAllowedException("Cannot delete last global vat rate");
        }

        Long activeConnections = vatRateRepository.activeConnectionCount(
                id,
                List.of(ProductDetailStatus.ACTIVE,ProductDetailStatus.INACTIVE),
                List.of(ServiceDetailStatus.ACTIVE, ServiceDetailStatus.INACTIVE),
                List.of(GoodsDetailStatus.ACTIVE,GoodsDetailStatus.INACTIVE),
                List.of(PriceComponentStatus.ACTIVE),
                List.of(BillingStatus.CANCELLED,BillingStatus.DELETED)
        );

        if (activeConnections > 0){
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        vatRate.setStatus(DELETED);
        vatRateRepository.save(vatRate);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return vatRateRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return vatRateRepository.findByIdIn(ids);
    }

    private void trimRequest(VatRateRequest vatRateRequest){
        vatRateRequest.setName(vatRateRequest.getName() == null ? null : vatRateRequest.getName().trim());
    }

    /**
     * Adds {@link VatRate} at the end with the highest ordering ID.
     * @param request {@link VatRateRequest}
     * @return {@link VatRateResponse}
     * @throws ClientException if there is another {@link VatRate} with same start date and global enabled .
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws ClientException if there is another {@link VatRate} with same name.
     */
    @Transactional
    public VatRateResponse add(VatRateRequest request) {
        log.debug("Adding vat rate: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot add item with status DELETED;");
            throw new ClientException(ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        trimRequest(request);

        if (vatRateRepository.countVatRateByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-Vat Rate with the same name already exists");
            throw new ClientException("name-Vat Rate with the same name already exists", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
//        Optional<VatRate> withSameName = vatRateRepository.findVatRateByNameAndStatus(request.getName(), NomenclatureItemStatus.ACTIVE);
//        if (withSameName.isPresent()) {
//            log.error("name-Vat Rate with the same name already exists");
//            throw new ClientException("name-Vat Rate with the same name already exists", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
//        }

        if (request.getStartDate() != null){
            Optional<VatRate> withSameStartDateAndActiveStatus = vatRateRepository.findVatRateByStartDateAndGlobalVatRateAndStatus(request.getStartDate(), request.getGlobalVatRate(), NomenclatureItemStatus.ACTIVE);
            if (withSameStartDateAndActiveStatus.isPresent()) {
                log.error("startDate-Vat Rate with the same start date and global vat rate enabled already exists");
                throw new ClientException("startDate-Vat Rate with the same start date and global vat rate enabled already exists", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        Long lastSortOrder = vatRateRepository.findLastOrderingId();
        VatRate vatRate = new VatRate(request);
        vatRate.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);

        VatRate vatRateEntity = vatRateRepository.save(vatRate);
        return new VatRateResponse(vatRateEntity);
    }

    /**
     * Edit the requested {@link VatRate}.
     * The one can not save {@link VatRate} with already existing name and status Active,
     * The one can not add the global {@link VatRate} with same start date.
     *
     * @param id      ID of {@link VatRate}
     * @param request {@link VatRateRequest}
     * @return {@link VatRateResponse}
     * @throws DomainEntityNotFoundException if {@link VatRate} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link VatRate} is deleted.
     */
    @Transactional
    public VatRateResponse edit(Long id, VatRateRequest request) {
        log.debug("Editing vat rate: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot set DELETED status to item");
            throw new ClientException(ILLEGAL_ARGUMENTS_PROVIDED);
        }
        trimRequest(request);
//        Optional<VatRate> withSameName = vatRateRepository.findVatRateByNameAndStatus(request.getName(), NomenclatureItemStatus.ACTIVE);
//        if (withSameName.isPresent() && !withSameName.get().getId().equals(id)) {
//            log.error("name-Vat Rate with the same name already exists");
//            throw new ClientException("name-Vat Rate with the same name already exists", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
//        }

        if (request.getStartDate() != null){
            Optional<VatRate> withSameStartDateAndActiveStatus = vatRateRepository.findVatRateByStartDateAndGlobalVatRateAndStatus(request.getStartDate(), request.getGlobalVatRate(), NomenclatureItemStatus.ACTIVE);
            if (withSameStartDateAndActiveStatus.isPresent() && !withSameStartDateAndActiveStatus.get().getId().equals(id)) {
                log.error("startDate-Vat Rate with the same start date and global vat rate enabled already exists");
                throw new ClientException("startDate-Vat Rate with the same start date and global vat rate enabled already exists", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        VatRate vatRate = vatRateRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate not found"));


        if (vatRateRepository.countVatRateByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !vatRate.getName().equals(request.getName().trim())) {
            log.error("name-Vat Rate with the same name already exists");
            throw new ClientException("name-Vat Rate with the same name already exists", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (vatRate.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("Cannot edit DELETED item.");
        }

        vatRate.setName(request.getName());
        vatRate.setValueInPercent(request.getValueInPercent());
        vatRate.setGlobalVatRate(request.getGlobalVatRate());
        vatRate.setStartDate(request.getGlobalVatRate() ? request.getStartDate() : null);
        vatRate.setStatus(request.getStatus());
        return new VatRateResponse(vatRateRepository.save(vatRate));
    }

    /**
     * Filters {@link VatRate} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link VatRate}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<VatRateResponse> Page&lt;VatRateResponse&gt;} containing detailed information
     */

    public Page<VatRateResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering vat rates list with : {}" + request.toString());

        Page<VatRate> page = vatRateRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                PageRequest.of(request.getPage(), request.getSize())
        );
        return page.map(VatRateResponse::new);
    }

    /**
     * Retrieves detailed information about {@link VatRate} by ID
     *
     * @param id ID of {@link VatRate}
     * @return {@link VatRateResponse}
     * @throws DomainEntityNotFoundException if no {@link VatRate} was found with the provided ID.
     */
    public VatRateResponse view(Long id) {
        log.debug("Fetching Vat Rate with ID: {}", id);
        VatRate vatRate = vatRateRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Vat rate not found"));
        return new VatRateResponse(vatRate);
    }
}
