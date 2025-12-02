package bg.energo.phoenix.service.nomenclature.pod;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.pod.Profiles;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.billingByProfile.BillingByProfileStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.pod.ProfilesFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.pod.profiles.CreateProfilesRequest;
import bg.energo.phoenix.model.request.nomenclature.pod.profiles.EditProfilesRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.pod.ProfilesResponse;
import bg.energo.phoenix.repository.nomenclature.pod.ProfilesRepository;
import bg.energo.phoenix.repository.pod.billingByProfile.BillingByProfileRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.StringUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.exception.ErrorCode.DOMAIN_ENTITY_NOT_FOUND;
import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.DELETED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;
import static bg.energo.phoenix.permissions.PermissionContextEnum.PROFILES;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfilesService implements NomenclatureBaseService {

    private final ProfilesRepository profilesRepository;
    private final BillingByProfileRepository billingByProfileRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.PROFILES;
    }

    /**
     * Filters {@link Profiles} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Profiles}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<ProfilesResponse> Page&lt;ProfilesResponse&gt;} containing detailed information
     */
    public Page<ProfilesResponse> filter(ProfilesFilterRequest request) {
        log.debug("Filtering Profiles list with request: {}", request.toString());
        Page<Profiles> page = profilesRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(StringUtil.underscoreReplacer(request.getPrompt())),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        request.getIncludedItemIds(),
                        request.getExcludeHardcodedValues(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(ProfilesResponse::new);
    }

    /**
     * Filters {@link Profiles} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Profiles}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator( //TODO permissions should be added
            permissions = {
                    @PermissionMapping(context = PROFILES, permissions = {NOMENCLATURE_VIEW}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        return profilesRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds {@link Profiles} at the end with the highest ordering ID.
     * If the request asks to save {@link Profiles} as a default and a default {@link Profiles} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     * function also checks if request name is unique and if not returns exception
     *
     * @param request {@link Profiles}
     * @return {@link ProfilesResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public ProfilesResponse add(CreateProfilesRequest request) {
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Integer count = getExistingRecordsCountByName(request.getName());

        if (count > 0) {
            log.error("Profiles Name is not unique");
            throw new ClientException("name-Profile with the same name already exists;", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastSortOrder = profilesRepository.findLastOrderingId();

        Profiles profile = Profiles
                .builder()
                .name(request.getName())
                .status(request.getStatus())
                .timeZone(request.getTimeZone())
                .orderingId(lastSortOrder == null ? 1 : lastSortOrder + 1)
                .isHardCoded(false)
                .build();

        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), profile);

        Profiles savedProfile = profilesRepository.save(profile);
        return new ProfilesResponse(savedProfile);
    }

    /**
     * Retrieves detailed information about {@link Profiles} by ID
     *
     * @param id ID of {@link Profiles}
     * @return {@link ProfilesResponse}
     * @throws DomainEntityNotFoundException if no {@link Profiles} was found with the provided ID.
     */
    public ProfilesResponse view(Long id) {
        log.debug("Fetching profile with ID: {}", id);
        Profiles profiles = profilesRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));
        return new ProfilesResponse(profiles);
    }

    /**
     * Edit the requested {@link Profiles}.
     * If the request asks to save {@link Profiles} as a default and a default {@link Profiles} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link Profiles}
     * @param request {@link bg.energo.phoenix.model.request.nomenclature.pod.profiles.BaseProfilesRequest}
     * @return {@link ProfilesResponse}
     * @throws DomainEntityNotFoundException if {@link Profiles} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link Profiles} is deleted.
     */
    @Transactional
    public ProfilesResponse edit(Long id, EditProfilesRequest request) {
        log.debug("Editing profile: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Profiles profiles = profilesRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Profile with id: %s not found".formatted(id)));

        if (profiles.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (profiles.getIsHardCoded()) {
            log.debug("Profile with id: %s is hardcoded and can't be changed");
            throw new OperationNotAllowedException("name- Hardcoded nomenclature can't be changed.;");
        }

        if (!profiles.getName().equals(request.getName())) {
            if (getExistingRecordsCountByName(request.getName()) > 0) {
                log.error("Profiles Name is not unique");
                throw new ClientException("name-Profile with the same name already exists;", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        if (Objects.nonNull(request.getTimeZone())) {
            if (!Objects.equals(profiles.getTimeZone(), request.getTimeZone())) {
                boolean existsByProfileIdAndStatusIn = billingByProfileRepository.existsByProfileIdAndStatusIn(
                        profiles.getId(),
                        List.of(BillingByProfileStatus.ACTIVE)
                );

                if (existsByProfileIdAndStatusIn) {
                    log.debug("This profile is used in billing data by profile, you cannot edit time zone");
                    throw new IllegalArgumentsProvidedException("This profile is used in billing data by profile, you cannot edit time zone");
                }
            }
        }

        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), profiles);
        profiles.setName(request.getName());
        profiles.setStatus(request.getStatus());
        profiles.setTimeZone(request.getTimeZone());

        Profiles savedProfile = profilesRepository.save(profiles);
        return new ProfilesResponse(savedProfile);
    }


    /**
     * AssignDefaultSelection
     *
     * @param status
     * @param defaultSelection
     * @param profiles
     */
    private void assignDefaultSelection(NomenclatureItemStatus status, Boolean defaultSelection, Profiles profiles) {
        if (status.equals(INACTIVE)) {
            profiles.setIsDefault(false);
        } else {
            if (defaultSelection) {
                Optional<Profiles> currentDefaultProfileOptional = profilesRepository.findByIsDefaultTrue();
                if (currentDefaultProfileOptional.isPresent()) {
                    Profiles currentDefaultProfile = currentDefaultProfileOptional.get();
                    currentDefaultProfile.setIsDefault(false);
                    profilesRepository.save(currentDefaultProfile);
                }
                profiles.setIsDefault(true);
            } else {
                profiles.setIsDefault(false);
            }
        }
    }

    /**
     * <h1>Check Name For Uniqueness</h1>
     * function returns count of name in database, if name is > 0 it means that it's not unique
     *
     * @param name
     * @return Integer count of name
     */
    private Integer getExistingRecordsCountByName(String name) {
        return profilesRepository.getExistingRecordsCountByName(name.toLowerCase(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
    }

    /**
     * Changes the ordering of a {@link Profiles} item in the profiles list to a specified position.
     * The method retrieves the {@link Profiles} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link Profiles} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link Profiles} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator( //TODO add permissions
            permissions = {
                    @PermissionMapping(context = PROFILES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in profiles to top", request.getId());

        Profiles profiles = profilesRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<Profiles> profilesList;

        if (profiles.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = profiles.getOrderingId();
            profilesList = profilesRepository.findInOrderingIdRange(
                    start,
                    end,
                    profiles.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (Profiles p : profilesList) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = profiles.getOrderingId();
            end = request.getOrderingId();
            profilesList = profilesRepository.findInOrderingIdRange(
                    start,
                    end,
                    profiles.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (Profiles p : profilesList) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        profiles.setOrderingId(request.getOrderingId());
        profilesRepository.save(profiles);
        profilesRepository.saveAll(profilesList);
    }

    @Override
    @Transactional
    @PermissionValidator( //TODO add permissions
            permissions = {
                    @PermissionMapping(context = PROFILES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the Profiles alphabetically");
        List<Profiles> profilesList = profilesRepository.orderByName();
        long orderingId = 1;

        for (Profiles p : profilesList) {
            p.setOrderingId(orderingId);
            orderingId++;
        }

        profilesRepository.saveAll(profilesList);
    }


    /**
     * Deletes {@link Profiles} if the validations are passed.
     *
     * @param id ID of the {@link Profiles}
     * @throws DomainEntityNotFoundException if {@link Profiles} is not found.
     * @throws OperationNotAllowedException  if the {@link Profiles} is already deleted.
     * @throws OperationNotAllowedException  if the {@link Profiles} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(  //TODO add permissions
            permissions = {
                    @PermissionMapping(context = PROFILES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Profile with ID: {}", id);
        Profiles profiles = profilesRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));

        if (profiles.getIsHardCoded()) {
            log.error("Can't delete the hardcoded nomenclature");
            throw new OperationNotAllowedException("id-Can't delete the hardcoded nomenclature;");
        }

        Integer canDeleteProfile = profilesRepository.canDeleteProfile(profiles.getId()); // 1 means it has connections

        if (canDeleteProfile == 1) {
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        if (profiles.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }
        // TODO:Check if there is no connected object to this nomenclature item in system
        profiles.setStatus(DELETED);
        profilesRepository.save(profiles);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return profilesRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return profilesRepository.findByIdIn(ids);
    }

    public Page<ProfilesResponse> listForApplicationModel(NomenclatureItemsBaseFilterRequest request) {
        Page<Profiles> page = profilesRepository
                .listForApplicationModel(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(ProfilesResponse::new);
    }
}
