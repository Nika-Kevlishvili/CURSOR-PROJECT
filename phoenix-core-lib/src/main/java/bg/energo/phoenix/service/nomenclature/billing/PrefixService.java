package bg.energo.phoenix.service.nomenclature.billing;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.billing.Prefix;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.billing.PrefixRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.billing.PrefixResponse;
import bg.energo.phoenix.repository.nomenclature.billing.PrefixRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.PREFIXES;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrefixService implements NomenclatureBaseService {
    private final PrefixRepository prefixRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.PREFIXES;
    }

    public Page<PrefixResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering prefixes list with request: {}", request.toString());
        Page<Prefix> page = prefixRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(PrefixResponse::new);
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PREFIXES, permissions = {NOMENCLATURE_VIEW}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering prefixes nomenclature with request: {}", request.toString());
        return prefixRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

//    @Transactional
//    public PrefixResponse add(PrefixRequest request) {
//        log.debug("Adding prefix: {}", request.toString());
//
//        if (request.getStatus().equals(DELETED)) {
//            log.error("Cannot add item with status DELETED");
//            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
//        }
//
//        if (prefixRepository.countPrefixByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
//            log.error("name-prefix with the same name already exists;");
//            throw new OperationNotAllowedException("name-prefix with the same name already exists;");
//        }
//
//        Long lastSortOrder = prefixRepository.findLastOrderingId();
//        Prefix prefixes = new Prefix(request);
//        prefixes.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
//        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), prefixes);
//        prefixes.setIsHardCoded(false);
//        Prefix prefix = prefixRepository.save(prefixes);
//        return new PrefixResponse(prefix);
//    }

    public PrefixResponse view(Long id) {
        log.debug("Fetching prefix with ID: {}", id);
        Prefix prefix = prefixRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Prefix not found, ID: " + id));
        return new PrefixResponse(prefix);
    }

    @Transactional
    public PrefixResponse edit(Long id, PrefixRequest request) {
        log.debug("Editing prefix: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        Prefix prefix = prefixRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Prefix with given id %s not found!;", id)));
        if (prefix.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }
        if (!prefix.getName().equals(request.getName())) {
            if (prefixRepository.countPrefixByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
                log.error("Prefix with ID {} is not unique and cannot be modified", prefix.getId());
                throw new ClientException("name-Prefix with the same name already exists;", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }
        if (!prefix.getPrefixType().equals(request.getPrefixType())) {
            if (prefixRepository.countPrefixByStatusAndPrefixType(request.getPrefixType(), List.of(ACTIVE, INACTIVE)) > 0) {
                log.error("Prefix with ID: {} have Prefix Type which is already used", prefix.getId());
                throw new ClientException("prefixType-Prefix with the same prefixType already exists;", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }
        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), prefix);
        prefix.setName(request.getName());
        return new PrefixResponse(prefixRepository.save(prefix));
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PREFIXES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of prefix item with ID: {} in prefixes to place: {}", request.getId(), request.getOrderingId());

        Prefix prefix = prefixRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Prefix not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<Prefix> prefixes;

        if (prefix.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = prefix.getOrderingId();

            prefixes = prefixRepository.findInOrderingIdRange(
                    start,
                    end,
                    prefix.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (Prefix c : prefixes) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = prefix.getOrderingId();
            end = request.getOrderingId();

            prefixes = prefixRepository.findInOrderingIdRange(
                    start,
                    end,
                    prefix.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (Prefix c : prefixes) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        prefix.setOrderingId(request.getOrderingId());
        prefixes.add(prefix);
        prefixRepository.saveAll(prefixes);
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PREFIXES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the prefixes alphabetically");
        List<Prefix> prefixes = prefixRepository.orderByName();
        long orderingId = 1;

        for (Prefix c : prefixes) {
            c.setOrderingId(orderingId);
            orderingId++;
        }

        prefixRepository.saveAll(prefixes);
    }

//    @Override
//    @Transactional
//    @PermissionValidator(
//    @PermissionValidator(
//            permissions = {
//                    @PermissionMapping(context = PREFIXES, permissions = {NOMENCLATURE_EDIT})
//            }
//    )
//    public void delete(Long id) {
//        log.debug("Removing prefix with ID: {}", id);
//        Prefix prefix = prefixRepository
//                .findById(id)
//                .orElseThrow(() -> new DomainEntityNotFoundException("id-Prefix not found, ID: " + id));
//
//        if (prefix.getStatus().equals(DELETED)) {
//            log.error("Prefix with ID {} is already deleted", prefix.getId());
//            throw new OperationNotAllowedException("status-Item is already deleted.");
//        }
//
//        if (prefix.getIsHardCoded()) {
//            log.error("Prefix with ID {} is hard-coded and cannot be deleted", prefix.getId());
//            throw new OperationNotAllowedException("name- Hardcoded nomenclature can't be deleted.;");
//        }
//
//        prefix.setStatus(DELETED);
//
//        prefixRepository.save(prefix);
//    }


    @Override
    public void delete(Long id) {

    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return prefixRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return prefixRepository.findByIdIn(ids);
    }

    private void assignDefaultSelection(NomenclatureItemStatus status, Boolean defaultSelection, Prefix prefix) {
        if (status.equals(INACTIVE)) {
            prefix.setDefault(false);
        } else {
            if (defaultSelection) {
                Optional<Prefix> currentDefaultPrefixOptional = prefixRepository.findByIsDefaultTrue();
                if (currentDefaultPrefixOptional.isPresent()) {
                    Prefix currentDefaultPrefix = currentDefaultPrefixOptional.get();
                    currentDefaultPrefix.setDefault(false);
                    prefixRepository.save(currentDefaultPrefix);
                }
                prefix.setDefault(true);
            } else {
                prefix.setDefault(false);
            }
        }
    }
}
