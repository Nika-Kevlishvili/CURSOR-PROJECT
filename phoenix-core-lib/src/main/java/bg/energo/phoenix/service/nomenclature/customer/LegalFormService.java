package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.apis.model.ApisCustomer;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.legalForm.LegalForm;
import bg.energo.phoenix.model.entity.nomenclature.customer.legalForm.LegalFormTransliterated;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.legalForm.CreateLegalFormRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.legalForm.EditLegalFormRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.legalForm.EditLegalFormTranRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.legalForm.LegalFormTranRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.LegalFormResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.LegalFormTranResponse;
import bg.energo.phoenix.repository.nomenclature.customer.legalForm.LegalFormRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.*;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalFormService implements NomenclatureBaseService {

    private final LegalFormRepository legalFormRepo;


    /**
     * Adds {@link LegalForm} at the end with the highest ordering ID.
     * If the request asks to save {@link LegalForm} as a default and a default {@link LegalForm} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link LegalForm}
     * @return {@link LegalFormResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public LegalFormResponse add(CreateLegalFormRequest request) {
        log.debug("Saving  legal form: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add Legal Form with status DELETED");
            throw new ClientException("status-Cannot add Legal Form with status DELETED", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (legalFormRepo.countLegalFormByStatusDescriptionAndName(request.getName(), request.getDescription(), List.of(ACTIVE, INACTIVE), null) > 0) {
            log.error("name-LegalForm with the same name and description already exists;");
            throw new OperationNotAllowedException("name-LegalForm with the same name and description already exists;");
        }

        List<String> names = new ArrayList<>();
        for (LegalFormTranRequest legalFormTranRequest : request.getLegalFormsTransliterated()) {
            names.add(legalFormTranRequest.getName());
        }

        Set<String> duplicates = getDuplicates(names);
        if (!duplicates.isEmpty()) {
            String error = "";
            for (int i = 0; i < names.size(); i++) {
                if (duplicates.contains(names.get(i))) {
                    error = error.concat(String.format("Legal Form - name[%s] - Legal Form (Transliterated) must be unique;", i));
                }
            }
            log.error("name-Legal Form (Transliterated) must be unique;");
            throw new OperationNotAllowedException(error);
        }

        LegalForm legalForm = new LegalForm(request);

        request.getLegalFormsTransliterated().forEach(
                legalFormTranRequest -> {
                    legalFormTranRequest.setName(legalFormTranRequest.getName().trim());
                    legalFormTranRequest.setDescription(legalFormTranRequest.getDescription().trim());
                }
        );

        List<LegalFormTransliterated> legalFormTransliterated = request
                .getLegalFormsTransliterated()
                .stream()
                .map(LegalFormTransliterated::new)
                .toList();

        Long topId = legalFormRepo.findTopId();
        NomenclatureItemStatus status = request.getStatus();
        legalFormTransliterated.forEach(x -> {
            x.setStatus(status);
            x.setLegalForm(legalForm);
        });
        legalForm.setOrderingId(topId == null ? 1 : topId + 1);

        if (request.getDefaultSelection()) {
            Optional<LegalForm> defaultLegalForm = legalFormRepo.findByDefaultSelectionTrue();
            if (defaultLegalForm.isPresent()) {
                LegalForm form = defaultLegalForm.get();
                form.setDefaultSelection(false);
                legalFormRepo.save(form);
            }
        }

        legalForm.setLegalFormTransliterated(legalFormTransliterated);
        LegalForm savedLegalForm = legalFormRepo.save(legalForm);


        return new LegalFormResponse(savedLegalForm);
    }

    /**
     * Edit the requested {@link LegalForm}.
     * If the request asks to save {@link LegalForm} as a default and a default {@link LegalForm} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link LegalForm}
     * @param request {@link EditLegalFormRequest}
     * @return {@link LegalFormResponse}
     * @throws DomainEntityNotFoundException if {@link LegalForm} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link LegalForm} is deleted.
     */
    @Transactional
    public LegalFormResponse edit(Long id, EditLegalFormRequest request) {
        log.debug("Editing legal form: {}", request);
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot edit Legal Form with status DELETED");
            throw new ClientException("status-Cannot edit Legal Form with status DELETED", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        LegalForm legalForm = legalFormRepo
                .findById(id)
                .orElseThrow(() -> new ClientException("legal form not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (legalFormRepo.countLegalFormByStatusDescriptionAndName(request.getName(), request.getDescription(), List.of(ACTIVE, INACTIVE), legalForm.getId()) > 0) {
            log.error("name-LegalForm with the same name and description already exists;");
            throw new OperationNotAllowedException("name-LegalForm with the same name and description already exists;");
        }

        if (legalForm.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        List<String> names = new ArrayList<>();
        for (EditLegalFormTranRequest editLegalFormTranRequest : request.getLegalFormsTransliterated()) {
            names.add(editLegalFormTranRequest.getName());
        }

        Set<String> duplicates = getDuplicates(names);
        if (!duplicates.isEmpty()) {
            String error = "";
            for (int i = 0; i < names.size(); i++) {
                if (duplicates.contains(names.get(i))) {
                    error = error.concat(String.format("Legal Form - name[%s] - Legal Form (Transliterated) must be unique;", i));
                }
            }
            log.error("name-Legal Form (Transliterated) must be unique;");
            throw new OperationNotAllowedException(error);
        }

        if (request.getDefaultSelection() && !legalForm.getDefaultSelection()) {
            Optional<LegalForm> defaultLegalForm = legalFormRepo.findByDefaultSelectionTrue();
            if (defaultLegalForm.isPresent()) {
                LegalForm form = defaultLegalForm.get();
                form.setDefaultSelection(false);
                legalFormRepo.save(form);
            }
        }
        legalForm.setName(request.getName().trim());
        legalForm.setDescription(request.getDescription().trim());
        legalForm.setDefaultSelection(request.getDefaultSelection());
        NomenclatureItemStatus status = request.getStatus();
        legalForm.setStatus(status);

        List<EditLegalFormTranRequest> lfTraRequest = request.getLegalFormsTransliterated();
        List<LegalFormTransliterated> formTransliterated = legalForm.getLegalFormTransliterated();

        createTranslForm(legalForm, status, lfTraRequest, formTransliterated);
        Map<Long, EditLegalFormTranRequest> collect = lfTraRequest.stream().filter(x -> x.getId() != null).collect(Collectors.toMap(EditLegalFormTranRequest::getId, lf -> lf));

        deleteTranslFormIfNotExist(status, formTransliterated, collect);


        LegalForm save = legalFormRepo.save(legalForm);
        return new LegalFormResponse(save);
    }

    private void createTranslForm(LegalForm legalForm, NomenclatureItemStatus status, List<EditLegalFormTranRequest> lfTraRequest, List<LegalFormTransliterated> formTransliterated) {
        //Creates new transliterated forms if provided id is null
        lfTraRequest.stream().filter(v -> v.getId() == null).forEach(v -> {
            LegalFormTransliterated legalFormTransliterated = new LegalFormTransliterated();
            legalFormTransliterated.setName(v.getName().trim());
            legalFormTransliterated.setDescription(v.getDescription().trim());
            legalFormTransliterated.setStatus(status);
            legalFormTransliterated.setLegalForm(legalForm);
            formTransliterated.add(legalFormTransliterated);
        });
    }

    private void deleteTranslFormIfNotExist(NomenclatureItemStatus status, List<LegalFormTransliterated> formTransliterated, Map<Long, EditLegalFormTranRequest> collect) {
        //Deletes transliterated forms if they are not in the request
        formTransliterated.forEach(x -> {
            EditLegalFormTranRequest tranReq = collect.get(x.getId());
            if (tranReq == null && x.getId() != null) {
                x.setStatus(NomenclatureItemStatus.DELETED);
            } else if (tranReq != null) {
                x.setName(tranReq.getName().trim());
                x.setDescription(tranReq.getDescription().trim());
                x.setStatus(status);
            }
            collect.remove(x.getId());
        });
    }

    /**
     * Deletes {@link LegalForm} if the validations are passed.
     *
     * @param id ID of the {@link LegalForm}
     * @throws DomainEntityNotFoundException if {@link LegalForm} is not found.
     * @throws OperationNotAllowedException  if the {@link LegalForm} is already deleted.
     * @throws OperationNotAllowedException  if the {@link LegalForm} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = LEGAL_FORMS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        LegalForm legalForm = legalFormRepo
                .findById(id)
                .orElseThrow(() -> new ClientException("id-legal form not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (legalForm.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        if (legalFormRepo.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }

        legalForm.setStatus(NomenclatureItemStatus.DELETED);
        legalForm.getLegalFormTransliterated().forEach(x -> x.setStatus(NomenclatureItemStatus.DELETED));
        legalFormRepo.save(legalForm);
        log.debug("Removed legal form with id: {}", id);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return legalFormRepo.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return legalFormRepo.findByIdIn(ids);
    }

    /**
     * Retrieves detailed information about {@link LegalForm} by ID
     *
     * @param id ID of {@link LegalForm}
     * @return {@link LegalFormResponse}
     * @throws DomainEntityNotFoundException if no {@link LegalForm} was found with the provided ID.
     */
    public LegalFormResponse view(Long id) {
        log.debug("Viewing legal form with id: {}", id);
        LegalForm legalForm = legalFormRepo
                .findById(id)
                .orElseThrow(() -> new ClientException("id-legal form not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        return new LegalFormResponse(legalForm);
    }

    public LegalFormTranResponse getLegalFormTransliterated(Long id) {
        log.debug("Getting legal form transliterated value with id: {}", id);
        return legalFormRepo.getLegalFormByLegalFormTransliteratedId(id);
    }

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.LEGAL_FORMS;
    }

    /**
     * Filters {@link LegalForm} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link LegalForm}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = LEGAL_FORMS, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
                    @PermissionMapping(context = EXPRESS_CONTRACT, permissions = {
                            EXPRESS_CONTRACT_CREATE}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering legal forms: {}", request);

        return legalFormRepo
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link LegalForm} item in the LegalForm list to a specified position.
     * The method retrieves the {@link LegalForm} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link LegalForm} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link LegalForm} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = LEGAL_FORMS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of legal forms: {}", request);
        LegalForm legalForm = legalFormRepo
                .findByIdAndStatus(request.getId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new RuntimeException("id-Legal form not found"));

        Long start;
        Long end;
        List<LegalForm> legalForms;

        if (legalForm.getOrderingId() > request.getOrderingId()) {
            start = request.getOrderingId();
            end = legalForm.getOrderingId();
            legalForms = legalFormRepo.findInOrderingIdRange(start, end, legalForm.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (LegalForm p : legalForms) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else {
            start = legalForm.getOrderingId();
            end = request.getOrderingId();
            legalForms = legalFormRepo.findInOrderingIdRange(start, end, legalForm.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (LegalForm p : legalForms) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        legalForm.setOrderingId(request.getOrderingId());
        legalFormRepo.save(legalForm);
        legalFormRepo.saveAll(legalForms);
    }

    /**
     * Sorts all {@link LegalForm} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = LEGAL_FORMS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting legal forms alphabetically");
        List<LegalForm> legalForms = legalFormRepo.orderByName();
        long orderId = 1;
        for (LegalForm legalForm : legalForms) {
            legalForm.setOrderingId(orderId);
            orderId++;
        }
        legalFormRepo.saveAll(legalForms);
    }

    /**
     * Filters {@link LegalForm} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link LegalForm}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<LegalFormResponse> Page&lt;LegalFormResponse&gt;} containing detailed information
     */
    public Page<LegalFormResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering legal forms: {}", request);
        return legalFormRepo
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize()))
                .map(LegalFormResponse::new);
    }

    public List<ApisCustomer> searchInDescriptions(List<ApisCustomer> apisCustomers) {
        List<String> searchStrings = new ArrayList<>();
        for (ApisCustomer customer : apisCustomers) {
            searchStrings.add(StringUtils.lowerCase(customer.getType()));
        }
        Optional<List<LegalForm>> legalFormOptional = legalFormRepo.searchInDescriptions(searchStrings, List.of(ACTIVE));
        List<LegalForm> legalForms;
        if (legalFormOptional.isPresent()) {
            legalForms = legalFormOptional.get();
            apisCustomers.stream().forEach(customer -> {
                legalForms.stream().forEach(lf -> {
                    String type = customer.getType();
                    String description = lf.getDescription();
                    if(type != null && description != null){
                        if (StringUtils.lowerCase(type).equals(StringUtils.lowerCase(description))) {
                            if (customer.getLegalForms() == null) {
                                customer.setLegalForms(new LegalFormResponse(lf));
                            }
                        }
                    }
                });
            });
        }
        return apisCustomers;
    }

    public Set<String> getDuplicates(List<String> names) {
        Set<String> namesTransliteratedSet = new HashSet<>();
        return names
                .stream()
                .filter(name -> !namesTransliteratedSet.add(name))
                .collect(Collectors.toSet());
    }
}
