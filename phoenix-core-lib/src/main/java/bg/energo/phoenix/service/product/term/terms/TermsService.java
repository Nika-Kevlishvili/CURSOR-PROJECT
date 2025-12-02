package bg.energo.phoenix.service.product.term.terms;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.product.term.terms.InvoicePaymentTerms;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.enums.copy.domain.CopyDomain;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.term.terms.*;
import bg.energo.phoenix.model.enums.product.term.terms.filter.TermsListColumns;
import bg.energo.phoenix.model.enums.product.term.terms.filter.TermsParameterFilterField;
import bg.energo.phoenix.model.request.contract.product.ProductParameterBaseRequest;
import bg.energo.phoenix.model.request.copy.domain.CopyDomainBaseRequest;
import bg.energo.phoenix.model.request.product.term.terms.CreateTermsRequest;
import bg.energo.phoenix.model.request.product.term.terms.EditTermsRequest;
import bg.energo.phoenix.model.request.product.term.terms.TermsListRequest;
import bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse;
import bg.energo.phoenix.model.response.terms.*;
import bg.energo.phoenix.model.response.terms.copy.InvoicePaymentTermsCopyResponse;
import bg.energo.phoenix.model.response.terms.copy.TermsCopyResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.copy.domain.CopyDomainBaseService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionEnum.TERMS_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.TERMS_VIEW_DELETED;

@Slf4j
@Service
@RequiredArgsConstructor
public class TermsService implements CopyDomainBaseService {

    private final TermsRepository termsRepository;
    private final InvoicePaymentTermsService invoicePaymentTermsService;
    private final PermissionService permissionService;
    private final CalendarRepository calendarRepository;
    private final InvoicePaymentTermsRepository invoicePaymentTermsRepository;

    /**
     * Creates terms according to create terms request {@link Terms}, {@link CreateTermsRequest}
     *
     * @param createTermsRequest request used to create terms
     * @return returns the terms view response {@link TermsViewResponse}
     * @throws ClientException with accumulated error messages from whole process
     */
    @Transactional
    public TermsViewResponse createTerms(CreateTermsRequest createTermsRequest) {
        List<String> exceptionMessages = new ArrayList<>();

        checkTerms(createTermsRequest, null, exceptionMessages);

        Terms terms = new Terms();
        terms.setName(createTermsRequest.getName());
        terms.setContractDeliveryActivationValue(createTermsRequest.getContractDeliveryActivationValue());
        terms.setContractDeliveryActivationType(createTermsRequest.getContractDeliveryActivationType());
        terms.setContractDeliveryActivationAutoTermination(createTermsRequest.getContractDeliveryActivationAutoTermination());
        terms.setResigningDeadlineValue(createTermsRequest.getResigningDeadlineValue());
        terms.setResigningDeadlineType(createTermsRequest.getResigningDeadlineType());
        terms.setSupplyActivations(createTermsRequest.getSupplyActivations().stream().toList());
        terms.setSupplyActivationExactDateStartDay(createTermsRequest.getSupplyActivationExactDateStartDay());
        terms.setGeneralNoticePeriodValue(createTermsRequest.getGeneralNoticePeriodValue());
        terms.setGeneralNoticePeriodType(createTermsRequest.getGeneralNoticePeriodType());
        terms.setNoticeTermPeriodValue(createTermsRequest.getNoticeTermPeriodValue());
        terms.setNoticeTermPeriodType(createTermsRequest.getNoticeTermPeriodType());
        terms.setNoticeTermDisconnectionPeriodValue(createTermsRequest.getNoticeTermDisconnectionPeriodValue());
        terms.setNoticeTermDisconnectionPeriodType(createTermsRequest.getNoticeTermDisconnectionPeriodType());
        terms.setContractEntryIntoForces(createTermsRequest.getContractEntryIntoForces().stream().toList());
        terms.setContractEntryIntoForceFromExactDayOfMonthStartDay(createTermsRequest.getContractEntryIntoForceFromExactDayOfMonthStartDay());
        terms.setNoInterestOnOverdueDebts(createTermsRequest.getNoInterestOnOverdueDebts());
        terms.setStatus(TermStatus.ACTIVE);
        terms.setStartsOfContractInitialTerms(createTermsRequest.getStartsOfContractInitialTerms().stream().toList());
        terms.setStartDayOfInitialContractTerm(createTermsRequest.getStartDayOfInitialContractTerm());
        terms.setFirstDayOfTheMonthOfInitialContractTerm(createTermsRequest.getFirstDayOfTheMonthOfInitialContractTerm());
        terms.setWaitForOldContractTermToExpires(createTermsRequest.getWaitForOldContractTermToExpires().stream().toList());

        terms = termsRepository.save(terms);

        List<InvoicePaymentTermsResponse> invoicePaymentTerms = invoicePaymentTermsService
                .createInvoicePaymentTerms(terms.getId(), createTermsRequest.getInvoicePaymentTerms(), exceptionMessages);
        throwExceptionIfRequired(exceptionMessages);
        return new TermsViewResponse(terms, invoicePaymentTerms);
    }

    /**
     * Edits already existed terms {@link Terms} according to provided request {@link EditTermsRequest}
     * Calls invoice payment terms service's edit method to edit invoice payment terms {@link InvoicePaymentTermsService}, {@link InvoicePaymentTerms}
     *
     * @param editTermsRequest request used to edit terms
     * @return returns the response {@link TermsViewResponse}
     * @throws ClientException if term can't be found by provided id
     * @throws ClientException if errors are added in error messages list from invoice payment terms service {@link InvoicePaymentTermsService}
     */
    @Transactional
    public TermsViewResponse editTerms(EditTermsRequest editTermsRequest) {
        List<String> exceptionMessages = new ArrayList<>();

        if (termsRepository.hasLockedConnection(editTermsRequest.getId()) && !hasEditLockedPermission()) {
            throw new OperationNotAllowedException("You can't edit price component because it is connected to the product contract, service contract or service order!;");
        }

        checkTerms(null, editTermsRequest, exceptionMessages);

        Terms terms = termsRepository
                .findByIdAndStatusIn(editTermsRequest.getId(), List.of(TermStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Terms not found with id: %s;".formatted(editTermsRequest.getId())));

        Optional<Long> termsId = termsRepository.findTermsByIdWhichIsPartOfTheService(terms.getId());

        if (termsId.isPresent() && checkTermsContractEntryIntoForcesAndStartsOfContractInitialTerms(
                editTermsRequest.getContractEntryIntoForces(),
                editTermsRequest.getStartsOfContractInitialTerms())
        ) {
            log.error("Terms has ‘From first delivery' or ‘From date of change of CBG’ option selected and is added directly to service or added to group of terms which is added in service;");
            exceptionMessages.add("id-Terms has ‘From first delivery' or ‘From date of change of CBG’ option selected and is added directly to service or added to group of terms which is added in service;");
        }

        terms.setName(editTermsRequest.getName());
        terms.setContractDeliveryActivationValue(editTermsRequest.getContractDeliveryActivationValue());
        terms.setContractDeliveryActivationType(editTermsRequest.getContractDeliveryActivationType());
        terms.setContractDeliveryActivationAutoTermination(editTermsRequest.getContractDeliveryActivationAutoTermination());
        terms.setResigningDeadlineValue(editTermsRequest.getResigningDeadlineValue());
        terms.setResigningDeadlineType(editTermsRequest.getResigningDeadlineType());
        terms.setSupplyActivations(editTermsRequest.getSupplyActivations().stream().toList());
        terms.setSupplyActivationExactDateStartDay(editTermsRequest.getSupplyActivationExactDateStartDay());
        terms.setGeneralNoticePeriodValue(editTermsRequest.getGeneralNoticePeriodValue());
        terms.setGeneralNoticePeriodType(editTermsRequest.getGeneralNoticePeriodType());
        terms.setNoticeTermPeriodValue(editTermsRequest.getNoticeTermPeriodValue());
        terms.setNoticeTermPeriodType(editTermsRequest.getNoticeTermPeriodType());
        terms.setNoticeTermDisconnectionPeriodValue(editTermsRequest.getNoticeTermDisconnectionPeriodValue());
        terms.setNoticeTermDisconnectionPeriodType(editTermsRequest.getNoticeTermDisconnectionPeriodType());
        terms.setContractEntryIntoForces(editTermsRequest.getContractEntryIntoForces().stream().toList());
        terms.setContractEntryIntoForceFromExactDayOfMonthStartDay(editTermsRequest.getContractEntryIntoForceFromExactDayOfMonthStartDay());
        terms.setNoInterestOnOverdueDebts(editTermsRequest.getNoInterestOnOverdueDebts());
        terms.setStatus(TermStatus.ACTIVE);
        terms.setStartsOfContractInitialTerms(editTermsRequest.getStartsOfContractInitialTerms().stream().toList());
        terms.setStartDayOfInitialContractTerm(editTermsRequest.getStartDayOfInitialContractTerm());
        terms.setFirstDayOfTheMonthOfInitialContractTerm(editTermsRequest.getFirstDayOfTheMonthOfInitialContractTerm());
        terms.setWaitForOldContractTermToExpires(editTermsRequest.getWaitForOldContractTermToExpires().stream().toList());
        termsRepository.save(terms);

        // passes validations, including constraints editing invoice payment terms when terms is connected to a group
        List<InvoicePaymentTermsResponse> invoicePaymentTerms = invoicePaymentTermsService.editInvoicePaymentTerms(
                terms,
                editTermsRequest.getInvoicePaymentTerms(),
                exceptionMessages
        );

        throwExceptionIfRequired(exceptionMessages);
        return new TermsViewResponse(terms, invoicePaymentTerms);
    }


    /**
     * Throws exception if exception messages list is not empty
     *
     * @param exceptionMessages List which contains error messages accumulated in request
     * @throws ClientException if exception messages list is not empty
     */
    private void throwExceptionIfRequired(List<String> exceptionMessages) {
        if (!exceptionMessages.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String message : exceptionMessages) {
                stringBuilder.append(message);
            }
            throw new ClientException(stringBuilder.toString(), ErrorCode.CONFLICT);
        }
    }


    /**
     * Terms Listing
     * filters data and returns the list of {@link TermsListingResponse} objects
     *
     * @param request {@link TermsListRequest} object
     * @return list of {@link TermsListingResponse} object
     */
    public Page<TermsListResponse> list(@Valid TermsListRequest request) {
        log.debug("Listing terms: {}", request);
        TermsAvailability availability = request.getAvailability();
        Sort.Order order = new Sort.Order(checkColumnDirection(request), checkSortField(request));
        return termsRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        getSearchField(request),
                        getTermStatuses().stream().map(TermStatus::getValue).toList(),
                        availability == TermsAvailability.ALL ? null : availability.toString(),
                        PageRequest.of(request.getPage(), request.getSize(), Sort.by(order))
                );
    }


    /**
     * Fetches available {@link Terms}, optionally filtered by name, based on the following criteria:
     * <ul>
     *     <li>Should not be attached to terms group or service or product and object is active</li>
     *     <li>Should have exactly one invoice payment term</li>
     *     <li>Invoice payment term must have value and must not have valueFrom or valueTo present</li>
     *     <li>Should be active</li>
     * </ul>
     *
     * @param request {@link SearchTermsForTermsGroupRequest} object with search criteria
     * @return page of {@link AvailableTermsResponse} object
     */
    public Page<AvailableTermsResponse> findAvailableTerms(@Valid SearchTermsForTermsGroupRequest request) {
        log.debug("Searching for available terms: {}", request);

        Sort.Order order = new Sort.Order(Sort.Direction.DESC, TermsListColumns.ID.getValue());
        return termsRepository.findAvailableTerms(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(order))
        );
    }


    /**
     * Returns list of {@link TermStatus} based on the permissions
     *
     * @return list of {@link TermStatus} object
     */
    private List<TermStatus> getTermStatuses() {
        List<String> context = permissionService.getPermissionsFromContext(PermissionContextEnum.TERMS);
        List<TermStatus> statuses = new ArrayList<>();
        if (context.stream().anyMatch(x -> Objects.equals(TERMS_VIEW_DELETED.getId(), x))) {
            statuses.add(TermStatus.DELETED);
        }

        if (context.stream().anyMatch(x -> Objects.equals(TERMS_VIEW_BASIC.getId(), x))) {
            statuses.add(TermStatus.ACTIVE);
        }

        return statuses;
    }

    /**
     * returns searchField values based on {@link TermsListRequest} request object
     * or return the defaul value of {@link TermsParameterFilterField#ALL}
     *
     * @return {@link TermsParameterFilterField} value
     */
    private String getSearchField(TermsListRequest request) {
        if (request.getTermsParameterFilterField() != null) {
            return request.getTermsParameterFilterField().getValue();
        }
        return TermsParameterFilterField.ALL.getValue();
    }


    /**
     * Returns {@link Sort.Direction} value based on {@link TermsListRequest} request object
     * or default value of ASC if not provided otherwise
     *
     * @param request {@link TermsListRequest} object
     * @return {@link Sort.Direction} value
     */
    private Sort.Direction checkColumnDirection(TermsListRequest request) {
        if (request.getColumnDirection() == null) {
            return Sort.Direction.ASC;
        }
        return request.getColumnDirection();
    }

    /**
     * Returns {@link TermsListRequest} value or returns default value of {@link TermsListColumns#ID}
     *
     * @param request {@link TermsListRequest} object with search criteria
     * @return {@link TermsListColumns} value
     */
    private String checkSortField(TermsListRequest request) {
        if (request.getTermsListColumns() == null) {
            return TermsListColumns.ID.getValue();
        }
        return request.getTermsListColumns().getValue();
    }

    /**
     * <h1>Terms View</h1>
     * Takes data from the database by id and status array according the permissions
     * if data is not found throws exceptions
     *
     * @param id id of the terms
     * @return {@link TermsViewResponse}
     */
    public TermsViewResponse view(Long id) {
        log.debug("Viewing Terms with id: {}", id);
        Terms terms = termsRepository
                .findByIdAndStatusIn(id, getTermStatuses())
                .orElseThrow(() -> new DomainEntityNotFoundException("Term not found with this id: %s;".formatted(id)));

        List<InvoicePaymentTermsResponse> invoicePaymentTerms = invoicePaymentTermsService
                .findDetailedByTermIdAndStatuses(terms.getId(), List.of(PaymentTermStatus.ACTIVE));
//        boolean hasConnections = terms.getGroupDetailId() != null
//                || termsRepository.hasConnectionToProduct(id, List.of(ProductStatus.ACTIVE))
//                || termsRepository.hasConnectionToService(id, List.of(ServiceStatus.ACTIVE));
//        return new TermsViewResponse(terms, invoicePaymentTerms, hasConnections);
        TermsViewResponse termsViewResponse = new TermsViewResponse(terms, invoicePaymentTerms);
        Optional<Long> termsId = termsRepository.findTermsByIdWhichIsPartOfTheService(terms.getId());

        if (termsId.isPresent()) termsViewResponse.setIsUsedInService(true);

        termsViewResponse.setIsLocked(termsRepository.hasLockedConnection(id));

        return termsViewResponse;
    }


    /**
     * Deletes terms {@link Terms} by provided id
     *
     * @param id id of term which is being deleting
     * @return returns the response {@link TermsViewResponse}
     * @throws ClientException if terms can't be found with provided id
     * @throws ClientException if term is connected to group
     */
    @Transactional
    public TermsViewResponse deleteTerms(Long id) {
        Terms terms = termsRepository.findByIdAndStatusIn(id, List.of(TermStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Terms not found with id: " + id + ";"));

        if (terms.getGroupDetailId() != null) {
            log.error("id-You can’t delete the term with ID [%s] because it is connected to the group of terms".formatted(id));
            throw new ClientException("id-You can’t delete the term with ID [%s] because it is connected to the group of terms".formatted(id), ErrorCode.CONFLICT);
        }

        if (termsRepository.hasConnectionToProduct(id, List.of(ProductStatus.ACTIVE))) {
            log.error("id-You can’t delete the term with ID [%s] because it is connected to the product".formatted(id));
            throw new ClientException("id-You can’t delete the term with ID [%s] because it is connected to the product".formatted(id), ErrorCode.CONFLICT);
        }

        if (termsRepository.hasConnectionToService(id, List.of(ServiceStatus.ACTIVE))) {
            log.error("id-You can’t delete the term with ID [%s] because it is connected to the service".formatted(id));
            throw new ClientException("id-You can’t delete the term with ID [%s] because it is connected to the service".formatted(id), ErrorCode.CONFLICT);
        }

        terms.setStatus(TermStatus.DELETED);
        terms = termsRepository.save(terms);
        return new TermsViewResponse(terms, null);
    }


    @Override
    public CopyDomain getDomain() {
        return CopyDomain.TERMS;
    }


    @Override
    public Page<CopyDomainListResponse> filterCopyDomain(CopyDomainBaseRequest request) {
        request.setPrompt(request.getPrompt() == null ? null : request.getPrompt().trim());
        Sort.Order order = new Sort.Order(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(order));
        return termsRepository.filterForCopy(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                List.of(TermStatus.ACTIVE),
                pageable);
    }


    public TermsCopyResponse viewForCopy(Long id) {
        Terms terms = termsRepository
                .findByIdAndStatusIn(id, List.of(TermStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Term not found with id: " + id + ";"));

        List<InvoicePaymentTermsResponse> InvoicePaymentTermsResponse = invoicePaymentTermsService
                .findDetailedByTermIdAndStatuses(terms.getId(), List.of(PaymentTermStatus.ACTIVE));

        List<InvoicePaymentTermsResponse> invoicePaymentTermsResponsesList = InvoicePaymentTermsResponse
                .stream()
                .filter((e) -> calendarRepository.findByIdAndStatusIsIn(e.getCalendar().getId(), List.of(NomenclatureItemStatus.ACTIVE)).isPresent())
                .toList();

        List<InvoicePaymentTermsCopyResponse> invoicePaymentTermsCopyResponseList = invoicePaymentTermsResponsesList
                .stream()
                .map(InvoicePaymentTermsCopyResponse::new)
                .collect(Collectors.toList());

        TermsCopyResponse termsCopyResponse = new TermsCopyResponse(terms, invoicePaymentTermsCopyResponseList);
        Optional<Long> termsId = termsRepository.findTermsByIdWhichIsPartOfTheService(terms.getId());

        if (termsId.isPresent()) termsCopyResponse.setIsUsedInService(true);

        return termsCopyResponse;
    }


    /**
     * Clones terms {@link Terms} by provided id with all invoice payment terms
     *
     * @param termId source {@link Terms} object ID
     * @return cloned {@link Terms} object
     */
    @Transactional
    public Terms cloneTerms(Long termId) {
        Terms source = termsRepository
                .findById(termId)
                .orElseThrow(() -> new DomainEntityNotFoundException(String.format("id-Term not found with id: %s", termId)));

        Terms clonedTerm = cloneBasicInformation(source);

        // invoice payment term is mandatory for Term and at least one active item should always be present
        invoicePaymentTermsRepository
                .findInvoicePaymentTermsByTermIdAndStatusIn(source.getId(), List.of(PaymentTermStatus.ACTIVE))
                .forEach(ipt -> cloneInvoicePaymentTerms(ipt, clonedTerm.getId()));

        return clonedTerm;
    }

    @Transactional
    public Terms cloneTermsForProductContract(Long termId, ProductParameterBaseRequest request) {
        Terms source = termsRepository
                .findById(termId)
                .orElseThrow(() -> new DomainEntityNotFoundException(String.format("id-Term not found with id: %s", termId)));

        Terms clonedTerm = cloneBasicInformation(source);

        // invoice payment term is mandatory for Term and at least one active item should always be present
        invoicePaymentTermsRepository
                .findInvoicePaymentTermsByTermIdAndStatusIn(source.getId(), List.of(PaymentTermStatus.ACTIVE))
                .forEach(ipt -> cloneInvoicePaymentTermsForProductContract(ipt, clonedTerm.getId(),request));

        return clonedTerm;
    }

    private Terms cloneBasicInformation(Terms source) {
        Terms clone = new Terms();
        clone.setName(source.getName());
        clone.setContractDeliveryActivationValue(source.getContractDeliveryActivationValue());
        clone.setContractDeliveryActivationAutoTermination(source.getContractDeliveryActivationAutoTermination());
        clone.setResigningDeadlineValue(source.getResigningDeadlineValue());
        clone.setContractDeliveryActivationType(source.getContractDeliveryActivationType());
        clone.setResigningDeadlineType(source.getResigningDeadlineType());
        clone.setSupplyActivationExactDateStartDay(source.getSupplyActivationExactDateStartDay());
        clone.setGeneralNoticePeriodValue(source.getGeneralNoticePeriodValue());
        clone.setGeneralNoticePeriodType(source.getGeneralNoticePeriodType());
        clone.setNoticeTermPeriodValue(source.getNoticeTermPeriodValue());
        clone.setNoticeTermPeriodType(source.getNoticeTermPeriodType());
        clone.setNoticeTermDisconnectionPeriodValue(source.getNoticeTermDisconnectionPeriodValue());
        clone.setNoticeTermDisconnectionPeriodType(source.getNoticeTermDisconnectionPeriodType());
        clone.setNoInterestOnOverdueDebts(source.getNoInterestOnOverdueDebts());
        clone.setContractEntryIntoForceFromExactDayOfMonthStartDay(source.getContractEntryIntoForceFromExactDayOfMonthStartDay());
        clone.setStatus(TermStatus.ACTIVE);
        clone.setGroupDetailId(null);
        clone.setStartsOfContractInitialTerms(source.getStartsOfContractInitialTerms());
        clone.setStartDayOfInitialContractTerm(source.getStartDayOfInitialContractTerm());
        clone.setFirstDayOfTheMonthOfInitialContractTerm(source.getFirstDayOfTheMonthOfInitialContractTerm());
        clone.setWaitForOldContractTermToExpires(source.getWaitForOldContractTermToExpires());
        clone.setSupplyActivations(source.getSupplyActivations());
        clone.setContractEntryIntoForces(source.getContractEntryIntoForces());
        Terms clonedTerm = termsRepository.saveAndFlush(clone);
        return clonedTerm;
    }


    /**
     * Clones list of invoice payment terms and sets provided term as the owner. Cloned invoice payment term is always active.
     * This is one step of term cloning process. Cloning term should be performed separately.
     *
     * @param source invoice payment terms to be cloned
     * @param termId owner of cloned invoice payment term
     */
    private void cloneInvoicePaymentTerms(InvoicePaymentTerms source, Long termId) {
        InvoicePaymentTerms clone = cloneInvoicePaymentTermBasicInformation(source, termId);
        invoicePaymentTermsRepository.save(clone);
    }

    private void cloneInvoicePaymentTermsForProductContract(InvoicePaymentTerms source, Long termId, ProductParameterBaseRequest request) {
        InvoicePaymentTerms clone = cloneInvoicePaymentTermBasicInformation(source, termId);

        InvoicePaymentTerms savedClone = invoicePaymentTermsRepository.save(clone);
        if (source.getId().equals(request.getInvoicePaymentTermId())) {
            request.setInvoicePaymentTermId(savedClone.getId());
        }
    }

    private InvoicePaymentTerms cloneInvoicePaymentTermBasicInformation(InvoicePaymentTerms source, Long termId) {
        InvoicePaymentTerms clone = new InvoicePaymentTerms();
        clone.setCalendarType(source.getCalendarType());
        clone.setValue(source.getValue());
        clone.setValueFrom(source.getValueFrom());
        clone.setValueTo(source.getValueTo());
        clone.setCalendarId(source.getCalendarId());
        clone.setExcludeWeekends(source.getExcludeWeekends());
        clone.setTermId(termId);
        clone.setStatus(PaymentTermStatus.ACTIVE);
        clone.setExcludeHolidays(source.getExcludeHolidays());
        clone.setDueDateChange(source.getDueDateChange());
        clone.setName(source.getName());
        return clone;
    }

    /**
     * Copies the terms object provided in the request
     *
     * @param terms the terms object to be copied
     * @return the copied terms object
     */

    public Terms copyTerms(Terms terms) {
        if (!termsRepository.existsById(terms.getId())) {
            log.error("Terms not found by ID: {};", terms.getId());
            throw new DomainEntityNotFoundException(String.format("Terms not found by ID: %s;", terms.getId()));
        }

        List<InvoicePaymentTerms> invoicePaymentTerms = invoicePaymentTermsService.findByTermIdAndStatusIn(terms.getId(), List.of(PaymentTermStatus.ACTIVE));
        if (!invoicePaymentTerms.isEmpty()) {
            List<InvoicePaymentTerms> newInvoicePaymentsTerms = new ArrayList<>();

            for (InvoicePaymentTerms paymentTerm : invoicePaymentTerms) {
                Optional<Calendar> calendar = calendarRepository.findByIdAndStatusIsIn(paymentTerm.getCalendarId(), List.of(NomenclatureItemStatus.ACTIVE));

                if (calendar.isPresent()) {
                    InvoicePaymentTerms newInvoicePaymentTerm = new InvoicePaymentTerms();
                    newInvoicePaymentTerm.setCalendarType(paymentTerm.getCalendarType());
                    newInvoicePaymentTerm.setName(paymentTerm.getName());
                    newInvoicePaymentTerm.setValue(paymentTerm.getValue());
                    newInvoicePaymentTerm.setValueFrom(paymentTerm.getValueFrom());
                    newInvoicePaymentTerm.setValueTo(paymentTerm.getValueTo());
                    newInvoicePaymentTerm.setCalendarId(paymentTerm.getCalendarId());
                    newInvoicePaymentTerm.setExcludeWeekends(paymentTerm.getExcludeWeekends());
                    newInvoicePaymentTerm.setExcludeHolidays(paymentTerm.getExcludeHolidays());
                    newInvoicePaymentTerm.setDueDateChange(paymentTerm.getDueDateChange());
                    newInvoicePaymentTerm.setStatus(PaymentTermStatus.ACTIVE);
                    newInvoicePaymentsTerms.add(newInvoicePaymentTerm);
                }
            }

            if (!newInvoicePaymentsTerms.isEmpty()) {
                Terms newTerms = createNewTerms(terms);
                for (InvoicePaymentTerms newInvoicePaymentTerm : newInvoicePaymentsTerms) {
                    newInvoicePaymentTerm.setTermId(newTerms.getId());
                }
                invoicePaymentTermsRepository.saveAll(newInvoicePaymentsTerms);
                return newTerms;
            }
        }
        return null;
    }

    /**
     * Creates a new terms object
     *
     * @param term the terms object to be copied
     * @return the new terms object
     */

    private Terms createNewTerms(Terms term) {
        Terms newTerms = new Terms();
        newTerms.setName(term.getName());
        newTerms.setContractDeliveryActivationValue(term.getContractDeliveryActivationValue());
        newTerms.setContractDeliveryActivationType(term.getContractDeliveryActivationType());
        newTerms.setContractDeliveryActivationAutoTermination(term.getContractDeliveryActivationAutoTermination());
        newTerms.setResigningDeadlineValue(term.getResigningDeadlineValue());
        newTerms.setResigningDeadlineType(term.getResigningDeadlineType());
        newTerms.setSupplyActivations(term.getSupplyActivations());
        newTerms.setSupplyActivationExactDateStartDay(term.getSupplyActivationExactDateStartDay());
        newTerms.setGeneralNoticePeriodValue(term.getGeneralNoticePeriodValue());
        newTerms.setGeneralNoticePeriodType(term.getGeneralNoticePeriodType());
        newTerms.setNoticeTermPeriodValue(term.getNoticeTermPeriodValue());
        newTerms.setNoticeTermPeriodType(term.getNoticeTermPeriodType());
        newTerms.setNoticeTermDisconnectionPeriodValue(term.getNoticeTermDisconnectionPeriodValue());
        newTerms.setNoticeTermDisconnectionPeriodType(term.getNoticeTermDisconnectionPeriodType());
        newTerms.setContractEntryIntoForces(term.getContractEntryIntoForces());
        newTerms.setContractEntryIntoForceFromExactDayOfMonthStartDay(term.getContractEntryIntoForceFromExactDayOfMonthStartDay());
        newTerms.setNoInterestOnOverdueDebts(term.getNoInterestOnOverdueDebts());
        newTerms.setStartsOfContractInitialTerms(term.getStartsOfContractInitialTerms());
        newTerms.setStartDayOfInitialContractTerm(term.getStartDayOfInitialContractTerm());
        newTerms.setFirstDayOfTheMonthOfInitialContractTerm(term.getFirstDayOfTheMonthOfInitialContractTerm());
        newTerms.setWaitForOldContractTermToExpires(term.getWaitForOldContractTermToExpires());
        newTerms.setStatus(TermStatus.ACTIVE);

        return termsRepository.saveAndFlush(newTerms);
    }

    private void checkTerms(CreateTermsRequest createTermsRequest, EditTermsRequest editTermsRequest, List<String> exceptionMessages) {
        if (createTermsRequest != null)
            checkTermsRequest(exceptionMessages, createTermsRequest.getGeneralNoticePeriodType(), createTermsRequest.getGeneralNoticePeriodValue(), createTermsRequest.getNoticeTermPeriodType(),
                    createTermsRequest.getNoticeTermPeriodValue(), createTermsRequest.getNoticeTermDisconnectionPeriodType(), createTermsRequest.getNoticeTermDisconnectionPeriodValue(), createTermsRequest);

        if (editTermsRequest != null)
            checkTermsRequest(exceptionMessages, editTermsRequest.getGeneralNoticePeriodType(), editTermsRequest.getGeneralNoticePeriodValue(), editTermsRequest.getNoticeTermPeriodType(),
                    editTermsRequest.getNoticeTermPeriodValue(), editTermsRequest.getNoticeTermDisconnectionPeriodType(), editTermsRequest.getNoticeTermDisconnectionPeriodValue(), createTermsRequest);

    }

    private void checkTermsRequest(List<String> exceptionMessages, GeneralNoticePeriodType generalNoticePeriodType, Integer generalNoticePeriodValue, NoticeTermPeriodType noticeTermPeriodType, Integer noticeTermPeriodValue, NoticeTermDisconnectionPeriodType noticeTermDisconnectionPeriodType, Integer noticeTermDisconnectionPeriodValue, CreateTermsRequest createTermsRequest) {
        if (generalNoticePeriodType != null &&
            generalNoticePeriodValue == null) {
            log.error("General notice period value can't be empty if general notice period type isn't empty;");
            exceptionMessages.add("generalNoticePeriodValue-General notice period value can't be empty if general notice period type isn't empty;");
        }

        if (noticeTermPeriodType != null &&
            noticeTermPeriodValue == null) {
            log.error("Notice term period value can't be empty if notice term period type isn't empty;");
            exceptionMessages.add("noticeTermPeriodValue-Notice term period value can't be empty if general notice term period type isn't empty;");
        }

        if (noticeTermDisconnectionPeriodType != null &&
            noticeTermDisconnectionPeriodValue == null) {
            log.error("Notice term disconnection period value can't be empty if notice term disconnection period type isn't empty;");
            exceptionMessages.add("noticeTermDisconnectionPeriodValue-Notice term disconnection period value can't be empty if notice term disconnection period type isn't empty;");
        }
    }

    private boolean checkTermsContractEntryIntoForcesAndStartsOfContractInitialTerms(Set<ContractEntryIntoForce> contractEntryIntoForces,
                                                                                     Set<StartOfContractInitialTerm> startOfContractInitialTerms) {
        return contractEntryIntoForces.contains(ContractEntryIntoForce.DATE_CHANGE_OF_CBG) ||
               contractEntryIntoForces.contains(ContractEntryIntoForce.FIRST_DELIVERY) ||
               startOfContractInitialTerms.contains(StartOfContractInitialTerm.DATE_OF_CHANGE_OF_CBG) ||
               startOfContractInitialTerms.contains(StartOfContractInitialTerm.FIRST_DELIVERY);
    }

    private boolean hasEditLockedPermission() {
        return permissionService.permissionContextContainsPermissions(PermissionContextEnum.TERMS, List.of(PermissionEnum.TERMS_EDIT_LOCKED));
    }

}
