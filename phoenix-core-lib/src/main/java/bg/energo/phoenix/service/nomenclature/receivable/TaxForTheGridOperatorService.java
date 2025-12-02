package bg.energo.phoenix.service.nomenclature.receivable;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.nomenclature.receivable.TaxesForTheGridOperator;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.receivable.taxForTheGridOperator.TaxForTheGridOperatorRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.receivable.TaxForTheGridOperatorResponse;
import bg.energo.phoenix.model.response.nomenclature.receivable.TaxForTheGridOperatorViewResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.TaxForTheGriOperatorRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static bg.energo.phoenix.exception.ErrorCode.DOMAIN_ENTITY_NOT_FOUND;
import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxForTheGridOperatorService implements NomenclatureBaseService {
    private final TaxForTheGriOperatorRepository taxForTheGriOperatorRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final CurrencyRepository currencyRepository;
    private final ContractTemplateRepository contractTemplateRepository;

    @Transactional
    public TaxForTheGridOperatorResponse add(TaxForTheGridOperatorRequest request) {
        log.debug("Adding Tax For The Grid Operator: {}", request);
        request.setDisconnectionType(request.getDisconnectionType().trim());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-[status]Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        List<TaxesForTheGridOperator> taxesForTheGridOperators = taxForTheGriOperatorRepository.findByNameAndStatuses(request.getDisconnectionType(), List.of(ACTIVE, INACTIVE));
        if (!taxesForTheGridOperators.isEmpty()) {
            log.error("tax for the grid operator with presented name already exists");
            throw new ClientException("name-Tax for the grid operator with presented name already exists;", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        validateTemplate(request.getDocumentTemplateId(), ContractTemplatePurposes.EMAIL, ContractTemplateType.DOCUMENT, "Document template with id %s does not exist or has different purpose!;");
        validateTemplate(request.getEmailTemplateId(), ContractTemplatePurposes.EMAIL, ContractTemplateType.EMAIL, "Email template with id %s does not exist or has different purpose!;");
        Long lastOrderingId = taxForTheGriOperatorRepository.findLastOrderingId();
        TaxesForTheGridOperator taxesForTheGridOperator = new TaxesForTheGridOperator(request);
        taxesForTheGridOperator.setDisconnectionType(request.getDisconnectionType().trim());
        taxesForTheGridOperator.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);
        checkCurrentDefaultSelection(request, taxesForTheGridOperator);

        validateTaxesForGridOperator(request, taxesForTheGridOperator);

        TaxesForTheGridOperator savedTaxesForTheGridOperator = taxForTheGriOperatorRepository.save(taxesForTheGridOperator);

        return new TaxForTheGridOperatorResponse(savedTaxesForTheGridOperator);
    }

    public void validateTemplate(Long templateId, ContractTemplatePurposes purpose, ContractTemplateType templateType, String errorMessage) {
        if (!contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(templateId, purpose, templateType, LocalDate.now())) {
            throw new DomainEntityNotFoundException(errorMessage.formatted(templateId));
        }
    }

    @Transactional
    public TaxForTheGridOperatorResponse edit(Long id, TaxForTheGridOperatorRequest request) {
        log.debug("Adding Tax For The Grid Operator: {}", request);
        request.setDisconnectionType(request.getDisconnectionType().trim());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot edit with status DELETED");
            throw new ClientException("status-[status]Cannot edit with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        TaxesForTheGridOperator taxesForTheGridOperator = taxForTheGriOperatorRepository
                .findById(id)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("id - Taxes for the grid operator with presented id not found: %s".formatted(id))
                );

        if (!taxesForTheGridOperator.getDisconnectionType().trim().equalsIgnoreCase(request.getDisconnectionType().trim())) {
            List<TaxesForTheGridOperator> taxesForTheGridOperators = taxForTheGriOperatorRepository.findByNameAndStatuses(request.getDisconnectionType(), List.of(ACTIVE, INACTIVE));
            if (!taxesForTheGridOperators.isEmpty()) {
                log.error("tax for the grid operator with presented name already exists");
                throw new ClientException("name-Tax for the grid operator with presented name already exists;", ILLEGAL_ARGUMENTS_PROVIDED);
            } else {
                taxesForTheGridOperator.setDisconnectionType(request.getDisconnectionType());
            }
        }

        checkCurrentDefaultSelection(request, taxesForTheGridOperator);
        validateTaxesForGridOperator(request, taxesForTheGridOperator);

        if (taxesForTheGridOperator.getEmailTemplateId() != null && !taxesForTheGridOperator.getEmailTemplateId().equals(request.getEmailTemplateId())) {
            validateTemplate(request.getEmailTemplateId(), ContractTemplatePurposes.EMAIL, ContractTemplateType.EMAIL, "Email template with id %s does not exist or has different purpose!;");
            taxesForTheGridOperator.setEmailTemplateId(request.getEmailTemplateId());
        }

        if (taxesForTheGridOperator.getDocumentTemplateId() != null && !taxesForTheGridOperator.getDocumentTemplateId().equals(request.getDocumentTemplateId())) {
            validateTemplate(request.getDocumentTemplateId(), ContractTemplatePurposes.EMAIL, ContractTemplateType.DOCUMENT, "Document template with id %s does not exist or has different purpose!;");
            taxesForTheGridOperator.setDocumentTemplateId(request.getDocumentTemplateId());
        }

        taxesForTheGridOperator.setSupplierType(request.getSupplierType());
        taxesForTheGridOperator.setTaxForReconnection(request.getTaxForReconnection());
        taxesForTheGridOperator.setTaxForReconnectionExpress(request.getTaxForExpressReconnection());
        taxesForTheGridOperator.setDefaultForPodWithMeasurementTypeSlp(request.isDefaultForPodWithMeasurementTypeSlp());
        taxesForTheGridOperator.setDefaultForPodWithMeasurementTypeBySettlementPeriod(request.isDefaultForPodWithMeasurementTypeBySettlementPeriod());
        taxesForTheGridOperator.setRemoveTaxInCancel(request.isRemoveTaxInCancel());
        taxesForTheGridOperator.setNumberOfIncomeAccount(request.getNumberOfIncomeAccount());
        taxesForTheGridOperator.setCostCenterControllingOrder(request.getCostCenterControllingOrder());
        taxesForTheGridOperator.setBasisForIssuing(request.getBasisForIssuing());
        taxesForTheGridOperator.setPriceComponentOrPriceComponentGroupOrItem(request.getPriceComponentOrPriceComponentGroupOrItem());

        taxesForTheGridOperator.setStatus(request.getStatus());
        if (request.getStatus().equals(INACTIVE)) {
            taxesForTheGridOperator.setDefaultSelection(false);
        }
        return new TaxForTheGridOperatorResponse(taxForTheGriOperatorRepository.save(taxesForTheGridOperator));
    }

    private void checkCurrentDefaultSelection(TaxForTheGridOperatorRequest request, TaxesForTheGridOperator taxes) {
        if (request.getStatus().equals(INACTIVE)) {
            taxes.setDefaultSelection(false);
        } else {
            if (request.isDefaultSelection()) {
                Optional<TaxesForTheGridOperator> currentDefaultTaxesForGrid = taxForTheGriOperatorRepository.findByDefaultSelectionTrue();
                if (currentDefaultTaxesForGrid.isPresent()) {
                    TaxesForTheGridOperator defaultTaxesForTheGridOperator = currentDefaultTaxesForGrid.get();
                    defaultTaxesForTheGridOperator.setDefaultSelection(false);
                    taxForTheGriOperatorRepository.save(defaultTaxesForTheGridOperator);
                }
            }
            taxes.setDefaultSelection(request.isDefaultSelection());
        }
    }

    private void validateTaxesForGridOperator(TaxForTheGridOperatorRequest request, TaxesForTheGridOperator taxesForTheGridOperator) {
        if (!gridOperatorRepository.existsByIdAndStatusIn(request.getGridOperator(), List.of(ACTIVE))) {
            log.error("Grid operator with that id doesn't exists: %s".formatted(request.getGridOperator()));
            throw new DomainEntityNotFoundException("Grid operator with that id doesn't exists: %s".formatted(request.getGridOperator()));
        } else {
            taxesForTheGridOperator.setGridOperator(request.getGridOperator());
        }

        if (!currencyRepository.existsByIdAndStatusIn(request.getCurrency(), List.of(ACTIVE))) {
            log.error("Currency with that id doesn't exists: %s".formatted(request.getCurrency()));
            throw new DomainEntityNotFoundException("Currency with that id doesn't exists: %s".formatted(request.getCurrency()));
        } else {
            taxesForTheGridOperator.setCurrency(request.getCurrency());
        }
    }

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.TAX_FOR_THE_GRID_OPERATOR;
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.TAXES_FOR_THE_GRID_OPERATOR, permissions = NOMENCLATURE_VIEW)
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering tax for the grid operator list with request: {}", request);
        return taxForTheGriOperatorRepository.filterNomenclature(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                PageRequest.of(request.getPage(), request.getSize()));
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.TAXES_FOR_THE_GRID_OPERATOR, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in tax for the grid operator to top", request.getId());
        TaxesForTheGridOperator taxesForTheGridOperator = taxForTheGriOperatorRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id - tax for the grid operator not found", DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<TaxesForTheGridOperator> taxesForTheGridOperatorList;

        if (taxesForTheGridOperator.getOrderingId() > request.getOrderingId()) {
            start = request.getOrderingId();
            end = taxesForTheGridOperator.getOrderingId();
            taxesForTheGridOperatorList = taxForTheGriOperatorRepository.findInOrderingIdRange(
                    start,
                    end,
                    taxesForTheGridOperator.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );
            long tempOrderingId = request.getOrderingId() + 1;
            for (TaxesForTheGridOperator taxes : taxesForTheGridOperatorList) {
                taxes.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else {
            start = taxesForTheGridOperator.getOrderingId();
            end = request.getOrderingId();
            taxesForTheGridOperatorList = taxForTheGriOperatorRepository.findInOrderingIdRange(
                    start,
                    end,
                    taxesForTheGridOperator.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (TaxesForTheGridOperator taxes : taxesForTheGridOperatorList) {
                taxes.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }
        taxesForTheGridOperator.setOrderingId(request.getOrderingId());
        taxForTheGriOperatorRepository.save(taxesForTheGridOperator);
        taxForTheGriOperatorRepository.saveAll(taxesForTheGridOperatorList);

    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.TAXES_FOR_THE_GRID_OPERATOR, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting tax for the grid operator alphabetically");
        List<TaxesForTheGridOperator> taxesForTheGridOperators = taxForTheGriOperatorRepository.orderByName();
        long orderingId = 1;

        for (TaxesForTheGridOperator taxes : taxesForTheGridOperators) {
            taxes.setOrderingId(orderingId);
            orderingId++;
        }
        taxForTheGriOperatorRepository.saveAll(taxesForTheGridOperators);
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.TAXES_FOR_THE_GRID_OPERATOR, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing tax for the grid operator with ID: {}", id);
        TaxesForTheGridOperator taxesForTheGridOperator = taxForTheGriOperatorRepository
                .findById(id)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("id - tax for the grid operator not found")
                );
        if (taxesForTheGridOperator.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new ClientException("id-Item is already deleted.", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        taxesForTheGridOperator.setDefaultSelection(false);
        taxesForTheGridOperator.setStatus(DELETED);
        taxForTheGriOperatorRepository.save(taxesForTheGridOperator);
    }

    public TaxForTheGridOperatorViewResponse view(Long id) {
        log.debug("Fetching Tax for grid operator with ID: {}", id);
        TaxesForTheGridOperator taxesForTheGridOperator = taxForTheGriOperatorRepository.findById(id).orElseThrow(
                () -> new DomainEntityNotFoundException("id - tax for grid Operator id not found: %s".formatted(id))
        );
        GridOperator gridOperator = gridOperatorRepository.findById(taxesForTheGridOperator.getGridOperator()).orElseThrow(
                () -> new DomainEntityNotFoundException("Can't find grid operator with id: %s".formatted(taxesForTheGridOperator.getGridOperator()))
        );
        TaxForTheGridOperatorViewResponse taxForTheGridOperatorViewResponse = new TaxForTheGridOperatorViewResponse(taxesForTheGridOperator, gridOperator);
        contractTemplateRepository.findTemplateResponseById(taxesForTheGridOperator.getEmailTemplateId(), LocalDate.now()).ifPresent(taxForTheGridOperatorViewResponse::setEmailTemplateResponse);
        contractTemplateRepository.findTemplateResponseById(taxesForTheGridOperator.getDocumentTemplateId(), LocalDate.now()).ifPresent(taxForTheGridOperatorViewResponse::setDocumentTemplateResponse);
        return taxForTheGridOperatorViewResponse;
    }

    public Page<TaxForTheGridOperatorResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering tax for the grid operator list with request: {}", request.toString());
        return taxForTheGriOperatorRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                request.getIncludedItemIds(),
                PageRequest.of(request.getPage(), request.getSize())
        ).map(TaxForTheGridOperatorResponse::new);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return taxForTheGriOperatorRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return taxForTheGriOperatorRepository.findByIdIn(ids);
    }
}
