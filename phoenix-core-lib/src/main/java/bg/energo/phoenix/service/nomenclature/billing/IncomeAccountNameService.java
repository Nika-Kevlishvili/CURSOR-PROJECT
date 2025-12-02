package bg.energo.phoenix.service.nomenclature.billing;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.billing.IncomeAccountName;
import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.billing.IncomeAccountNameRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.billing.IncomeAccountNameResponse;
import bg.energo.phoenix.repository.nomenclature.billing.IncomeAccountNameRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.INCOME_ACCOUNT_NUMBER;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncomeAccountNameService implements NomenclatureBaseService {

    private final IncomeAccountNameRepository incomeAccountNameRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.INCOME_ACCOUNT_NUMBER;
    }


    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = INCOME_ACCOUNT_NUMBER, permissions = {NOMENCLATURE_VIEW}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering IncomeAccountNumbers list with statuses: {}", request);
        return incomeAccountNameRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }


    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = INCOME_ACCOUNT_NUMBER, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in IncomeAccountNumbers to top", request.getId());

        IncomeAccountName IncomeAccountNumber = incomeAccountNameRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-IncomeAccountNumber not found"));

        Long start;
        Long end;
        List<IncomeAccountName> IncomeAccountNumbers;

        if (IncomeAccountNumber.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = IncomeAccountNumber.getOrderingId();
            IncomeAccountNumbers = incomeAccountNameRepository.findInOrderingIdRange(start, end, IncomeAccountNumber.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (IncomeAccountName b : IncomeAccountNumbers) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = IncomeAccountNumber.getOrderingId();
            end = request.getOrderingId();
            IncomeAccountNumbers = incomeAccountNameRepository.findInOrderingIdRange(start, end, IncomeAccountNumber.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (IncomeAccountName b : IncomeAccountNumbers) {
                b.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        IncomeAccountNumber.setOrderingId(request.getOrderingId());
        IncomeAccountNumbers.add(IncomeAccountNumber);
        incomeAccountNameRepository.saveAll(IncomeAccountNumbers);
    }

    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = INCOME_ACCOUNT_NUMBER, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the IncomeAccountNumbers alphabetically");
        List<IncomeAccountName> IncomeAccountNumbers = incomeAccountNameRepository.orderByName();
        long orderingId = 1;

        for (IncomeAccountName b : IncomeAccountNumbers) {
            b.setOrderingId(orderingId);
            orderingId++;
        }

        incomeAccountNameRepository.saveAll(IncomeAccountNumbers);
    }


    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = INCOME_ACCOUNT_NUMBER, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing IncomeAccountNumber with ID: {}", id);
        IncomeAccountName IncomeAccountNumber = incomeAccountNameRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-IncomeAccountNumber not found"));

        if (IncomeAccountNumber.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        IncomeAccountNumber.setStatus(DELETED);
        incomeAccountNameRepository.save(IncomeAccountNumber);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return incomeAccountNameRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return incomeAccountNameRepository.findByIdIn(ids);
    }


    @Transactional
    public IncomeAccountNameResponse add(IncomeAccountNameRequest request) {
        log.debug("Adding IncomeAccountNumber: {}", request.toString());
        List<String> errorMessages = new ArrayList<>();

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (incomeAccountNameRepository.countNumberByStatusAndName(request.getNumber(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-IncomeAccountNumber with the same name already exists;");
            throw new OperationNotAllowedException("number-IncomeAccountNumber with the same name already exists;");
        }

        if (request.getStatus().equals(INACTIVE) && CollectionUtils.isNotEmpty(request.getDefaultAssignmentType())) {
            throw new OperationNotAllowedException("defaultAssignmentType-Can not set default assignment type when nomenclature is inactive!;");
        }

        Long lastSortOrder = incomeAccountNameRepository.findLastOrderingId();
        IncomeAccountName IncomeAccountNumber = new IncomeAccountName(request);
        IncomeAccountNumber.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        validateDefaultAssignmentType(request, errorMessages);

        IncomeAccountName IncomeAccountNumberEntity = incomeAccountNameRepository.save(IncomeAccountNumber);
        return new IncomeAccountNameResponse(IncomeAccountNumberEntity);
    }

    private void validateDefaultAssignmentType(IncomeAccountNameRequest request, List<String> errorMessages) {
        List<DefaultAssignmentType> defaultAssignmentType = request.getDefaultAssignmentType();
        if (CollectionUtils.isNotEmpty(defaultAssignmentType)) {
            boolean isAllSelected = false;
            if (defaultAssignmentType.contains(DefaultAssignmentType.ALL)) {
                isAllSelected = true;
            } else {
                for (DefaultAssignmentType type : defaultAssignmentType) {
                    String incomeAccountName = incomeAccountNameRepository.findByDefaultAssignmentType(type.name());
                    if (incomeAccountName != null) {
                        errorMessages.add("Income Account Name with type %s already exists for Income Account Number with Name %s".formatted(type.toString(), incomeAccountName));
                    }
                }
            }

            if (isAllSelected) {
                List<String> assignmentTypeIsNotNull = incomeAccountNameRepository.findWhereDefaultAssignmentTypeIsNotNull();
                if (!assignmentTypeIsNotNull.isEmpty()) {
                    errorMessages.add("It is not possible to select ALL because default assignment type is selected in Income Account with Name %s".formatted(assignmentTypeIsNotNull));
                }

                if (defaultAssignmentType.size() > 1) {
                    errorMessages.add("It is not possible to pass another type when ALL is selected");
                }
            }
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
    }


    public IncomeAccountNameResponse view(Long id) {
        log.debug("Fetching IncomeAccountNumber with ID: {}", id);
        IncomeAccountName IncomeAccountNumber = incomeAccountNameRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("IncomeAccountNumber not found"));
        return new IncomeAccountNameResponse(IncomeAccountNumber);
    }


    @Transactional
    public IncomeAccountNameResponse edit(Long id, IncomeAccountNameRequest request) {
        List<String> errorMessages = new ArrayList<>();
        log.debug("Editing incomeAccountNumber: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        IncomeAccountName incomeAccountNumber = incomeAccountNameRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-incomeAccountNumber not found"));

        if (incomeAccountNameRepository.countNumberByStatusAndName(request.getNumber(), List.of(ACTIVE, INACTIVE)) > 0
                && !incomeAccountNumber.getNumber().equals(request.getNumber().trim())) {
            log.error("number-incomeAccountNumber with the same name already exists;");
            throw new OperationNotAllowedException("number-incomeAccountNumber with the same name already exists;");
        }

        if (incomeAccountNumber.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item");
        }

        if (request.getStatus().equals(INACTIVE) && CollectionUtils.isNotEmpty(request.getDefaultAssignmentType())) {
            throw new OperationNotAllowedException("defaultAssignmentType-Can not set default assignment type when nomenclature is inactive!;");
        }

        validateDefaultAssignmentType(request, errorMessages);

        incomeAccountNumber.setDefaultAssignmentType(request.getDefaultAssignmentType());
        incomeAccountNumber.setName(request.getName().trim());
        incomeAccountNumber.setNumber(request.getNumber().trim());
        incomeAccountNumber.setStatus(request.getStatus());
        return new IncomeAccountNameResponse(incomeAccountNameRepository.save(incomeAccountNumber));
    }


    public Page<IncomeAccountNameResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering IncomeAccountNumbers list with: {}", request.toString());
        Page<IncomeAccountName> page = incomeAccountNameRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                PageRequest.of(request.getPage(), request.getSize())
        );
        return page.map(IncomeAccountNameResponse::new);
    }
}
