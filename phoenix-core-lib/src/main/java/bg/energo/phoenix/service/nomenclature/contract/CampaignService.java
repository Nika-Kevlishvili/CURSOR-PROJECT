package bg.energo.phoenix.service.nomenclature.contract;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.contract.Campaign;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.contract.CampaignRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.CampaignResponse;
import bg.energo.phoenix.repository.nomenclature.contract.CampaignRepository;
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

import static bg.energo.phoenix.exception.ErrorCode.DOMAIN_ENTITY_NOT_FOUND;
import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.CAMPAIGNS;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService implements NomenclatureBaseService {
    private final CampaignRepository campaignRepository;

    /**
     * @return {@link Nomenclature} type
     */
    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.CAMPAIGNS;
    }

    /**
     * Filters {@link Campaign} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Campaign}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CAMPAIGNS, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering campaigns nomenclature with request: {}", request.toString());
        return campaignRepository.filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()), request.getStatuses(), PageRequest.of(request.getPage(), request.getSize()));
    }

    /**
     * Changes the ordering of a {@link Campaign} item in the campaigns list to a specified position.
     * The method retrieves the campaign item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the campaign item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link Campaign} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CAMPAIGNS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of campaign item with ID: {} in campaigns to place: {}", request.getId(), request.getOrderingId());

        Campaign campaign = campaignRepository.findById(request.getId()).orElseThrow(() -> new DomainEntityNotFoundException("id-Campaign not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<Campaign> campaigns;

        if (campaign.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = campaign.getOrderingId();

            campaigns = campaignRepository.findInOrderingIdRange(start, end, campaign.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (Campaign c : campaigns) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = campaign.getOrderingId();
            end = request.getOrderingId();

            campaigns = campaignRepository.findInOrderingIdRange(start, end, campaign.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (Campaign c : campaigns) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        campaign.setOrderingId(request.getOrderingId());
        campaigns.add(campaign);
        campaignRepository.saveAll(campaigns);
    }

    /**
     * Sorts all {@link Campaign} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CAMPAIGNS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the campaigns alphabetically");
        List<Campaign> campaigns = campaignRepository.orderByName();
        long orderingId = 1;

        for (Campaign c : campaigns) {
            c.setOrderingId(orderingId);
            orderingId++;
        }
        campaignRepository.saveAll(campaigns);
    }

    /**
     * Deletes {@link Campaign} if the validations are passed.
     *
     * @param id ID of the {@link Campaign}
     * @throws DomainEntityNotFoundException if {@link Campaign} is not found.
     * @throws OperationNotAllowedException  if the {@link Campaign} is already deleted.
     * @throws OperationNotAllowedException  if the {@link Campaign} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CAMPAIGNS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing campaign with ID: {}", id);
        Campaign campaign = campaignRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Campaign not found, ID: " + id));

        if (campaign.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        if (campaignRepository.hasActiveConnectionsWithProductContract(id)) {
            log.error("You can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        if (campaignRepository.hasActiveConnectionsWithServiceContracts(id)) {
            log.error("You can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        if (campaignRepository.hasActiveConnectionsWithServiceOrder(id)) {
            log.error("You can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        if (campaignRepository.hasActiveConnectionsWithGoodsOrder(id)) {
            log.error("You can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        campaign.setStatus(DELETED);

        campaignRepository.save(campaign);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return campaignRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return campaignRepository.findByIdIn(ids);
    }

    /**
     * Filters the list of campaigns based on the given filter request parameters.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Campaign}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return a Page of CampaignResponse objects containing the filtered list of campaigns.
     */
    public Page<CampaignResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering campaigns list with request: {}", request.toString());
        Page<Campaign> page = campaignRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(CampaignResponse::new);
    }

    /**
     * Adds {@link Campaign} at the end with the highest ordering ID.
     * If the request asks to save {@link Campaign} as a default and a default {@link Campaign} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link CampaignRequest}
     * @return {@link CampaignResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public CampaignResponse add(CampaignRequest request) {
        log.debug("Adding camppaign: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (campaignRepository.countCampaignByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-Campaign with the same name already exists;");
            throw new OperationNotAllowedException("name-Campaign with the same name already exists;");
        }

        Long lastSortOrder = campaignRepository.findLastOrderingId();
        Campaign campaign = new Campaign(request);
        campaign.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<Campaign> currentDefaultCampaignOptional = campaignRepository.findByDefaultSelectionTrue();
            if (currentDefaultCampaignOptional.isPresent()) {
                Campaign currentDefaultCampaign = currentDefaultCampaignOptional.get();
                currentDefaultCampaign.setDefaultSelection(false);
                campaignRepository.save(currentDefaultCampaign);
            }
        }
        Campaign campaignEntity = campaignRepository.save(campaign);
        return new CampaignResponse(campaignEntity);
    }

    /**
     * Retrieves detailed information about {@link Campaign} by ID
     *
     * @param id ID of {@link Campaign}
     * @return {@link CampaignResponse}
     * @throws DomainEntityNotFoundException if no {@link Campaign} was found with the provided ID.
     */
    public CampaignResponse view(Long id) {
        log.debug("Fetching campaign with ID: {}", id);
        Campaign campaign = campaignRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Campaign not found, ID: " + id));
        return new CampaignResponse(campaign);
    }


    /**
     * Edits the {@link Campaign}.
     * If the request asks to save {@link Campaign} as a default and a default {@link Campaign} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link Campaign}
     * @param request {@link CampaignRequest}
     * @return {@link CampaignResponse}
     * @throws DomainEntityNotFoundException if {@link Campaign} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link Campaign} is deleted.
     */
    @Transactional
    public CampaignResponse edit(Long id, CampaignRequest request) {
        log.debug("Editing campaign: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Campaign campaign = campaignRepository
                .findById(id)
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));

        if (campaignRepository.countCampaignByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !campaign.getName().equals(request.getName().trim())) {
            log.error("name-Campaign with the same name already exists;");
            throw new OperationNotAllowedException("name-Campaign with the same name already exists;");
        }

        if (campaign.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !campaign.isDefaultSelection()) {
            Optional<Campaign> currentDefaultCampaignOptional = campaignRepository.findByDefaultSelectionTrue();
            if (currentDefaultCampaignOptional.isPresent()) {
                Campaign currentDefaultCampaign = currentDefaultCampaignOptional.get();
                currentDefaultCampaign.setDefaultSelection(false);
                campaignRepository.save(currentDefaultCampaign);
            }
        }
        campaign.setDefaultSelection(request.getDefaultSelection());

        campaign.setName(request.getName().trim());
        campaign.setStatus(request.getStatus());
        return new CampaignResponse(campaignRepository.save(campaign));
    }
}
