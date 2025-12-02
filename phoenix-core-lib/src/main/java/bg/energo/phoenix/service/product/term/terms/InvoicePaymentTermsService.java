package bg.energo.phoenix.service.product.term.terms;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.product.term.terms.InvoicePaymentTerms;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.term.terms.PaymentTermStatus;
import bg.energo.phoenix.model.request.product.term.terms.paymentTerm.CreateInvoicePaymentTermRequest;
import bg.energo.phoenix.model.request.product.term.terms.paymentTerm.EditInvoicePaymentTermRequest;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarResponse;
import bg.energo.phoenix.model.response.terms.InvoicePaymentTermsResponse;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicePaymentTermsService {
    private final CalendarRepository calendarRepository;
    private final TermsRepository termsRepository;
    private final InvoicePaymentTermsRepository invoicePaymentTermsRepository;

    public List<InvoicePaymentTerms> findByTermIdAndStatusIn(Long id, List<PaymentTermStatus> statuses){
        return invoicePaymentTermsRepository.findByTermIdAndStatusIn(id,statuses)
                .orElseThrow(()-> new ClientException("id-Invoice payment term not found with this id: " + id, ErrorCode.APPLICATION_ERROR));
    }

    public List<InvoicePaymentTermsResponse> findDetailedByTermIdAndStatuses(Long termId, List<PaymentTermStatus> statuses){
        return invoicePaymentTermsRepository.findDetailedByTermIdAndStatusIn(termId, statuses);
    }

    /**
     * This method creates new {@link InvoicePaymentTerms} objects based on the provided list of {@link CreateInvoicePaymentTermRequest}
     * and the termId, and saves them to the repository. If there are any validation or other errors, an error message is added to the exceptionMessage list.
     *
     * @param termId A Long representing the ID of the term.
     * @param requests A List of CreateInvoicePaymentTermRequest objects that contain the details of the new InvoicePaymentTerms objects.
     * @param exceptionMessage A List of Strings to hold any exception messages.
     */
    @Transactional
    public List<InvoicePaymentTermsResponse> createInvoicePaymentTerms(Long termId, List<CreateInvoicePaymentTermRequest> requests, List<String> exceptionMessage) {
        if (CollectionUtils.isEmpty(requests)) {
            log.error("Payment term request list must not be empty;");
            exceptionMessage.add("invoicePaymentTerms-Payment term request list must not be empty;");
            return null;
        }

        if (termId == null) {
            log.error("Term ID is null, cannot create invoice payment term");
            exceptionMessage.add("invoicePaymentTerms-Term ID is null, cannot create invoice payment term;");
            return null;
        }

        if (!termsRepository.existsById(termId)) {
            log.error("Terms not found by ID: " + termId);
            exceptionMessage.add(String.format("invoicePaymentTerms-Terms not found by ID: %s;", termId));
            return null;
        }

        List<InvoicePaymentTerms> tempInvoicePaymentTerms = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            create(tempInvoicePaymentTerms, termId, requests.get(i), exceptionMessage, i);
        }

        if (exceptionMessage.isEmpty()) {
            invoicePaymentTermsRepository.saveAll(tempInvoicePaymentTerms);
            return tempInvoicePaymentTerms
                    .stream()
                    .map(this::responseFromEntity)
                    .toList();
        } else {
            return null;
        }
    }

    /**
     * Edits the payment terms of an invoice.
     *
     * @param terms the {@link Terms} object containing the payment terms to be edited
     * @param requests the list of {@link EditInvoicePaymentTermRequest} objects, which contain the details of the payment terms to be edited
     * @param exceptionMessage the list of error messages encountered during execution
     * @return the list of {@link InvoicePaymentTermsResponse} objects, which contain the details of the edited payment terms
     */
    public List<InvoicePaymentTermsResponse> editInvoicePaymentTerms(Terms terms, List<EditInvoicePaymentTermRequest> requests, List<String> exceptionMessage) {
        if (CollectionUtils.isEmpty(requests)) {
            log.error("Payment term request list must not be empty;");
            exceptionMessage.add("invoicePaymentTerms-Payment term request list must not be empty;");
            return null;
        }

        if (terms.getId() == null) {
            log.error("Term ID is null, cannot edit invoice payment term");
            exceptionMessage.add("invoicePaymentTerms-Term ID is null, cannot edit invoice payment term;");
            return null;
        }

        if (!termsRepository.existsById(terms.getId())) {
            log.error("Terms not found by ID: " + terms.getId());
            exceptionMessage.add(String.format("invoicePaymentTerms-Terms not found by ID: %s;", terms.getId()));
            return null;
        }

        if (terms.getGroupDetailId() != null) {
            if (requests.size() > 1) {
                log.error("Only one payment term should be attached to a term that is connected to a group;");
                exceptionMessage.add("invoicePaymentTerms-Only one payment term should be attached to a term that is connected to a group;");
                return null;
            }

            EditInvoicePaymentTermRequest termRequest = requests.get(0);
            if (termRequest.getValue() == null || termRequest.getValueFrom() != null || termRequest.getValueTo() != null) {
                log.error("Terms ID %s is connected to a group ID %s. Value ranges cannot be modified;".formatted(terms.getId(), terms.getGroupDetailId()));
                exceptionMessage.add("Terms [ID %s] is connected to a group [ID %s]. Value ranges cannot be modified;".formatted(terms.getId(), terms.getGroupDetailId()));
                return null;
            }
        }

        Optional<List<InvoicePaymentTerms>> dbInvoicePaymentTermsOptional = invoicePaymentTermsRepository
                .findByTermIdAndStatusIn(terms.getId(), List.of(PaymentTermStatus.ACTIVE)); // already in db

        List<InvoicePaymentTerms> tempInvoicePaymentTerms = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            EditInvoicePaymentTermRequest currentRequest = requests.get(i);
            if (currentRequest.getId() == null) {
                create(tempInvoicePaymentTerms, terms.getId(), EditInvoicePaymentTermRequest.toCreateRequest(currentRequest), exceptionMessage, i);
            } else {
                Optional<InvoicePaymentTerms> paymentTermsOptional = invoicePaymentTermsRepository.findById(currentRequest.getId());
                if (paymentTermsOptional.isEmpty()) {
                    log.error("Payment term not found by ID: {}", currentRequest.getId());
                    exceptionMessage.add(String.format("invoicePaymentTerms[%s].id-Payment term not found by ID: %s;", i, currentRequest.getId()));
                    continue;
                }

                edit(tempInvoicePaymentTerms, terms, paymentTermsOptional.get(), currentRequest, exceptionMessage, i);
            }
        }

        deleteRemovedInvoicePaymentTerms(requests, dbInvoicePaymentTermsOptional, tempInvoicePaymentTerms);

        if (exceptionMessage.isEmpty()) {
            invoicePaymentTermsRepository.saveAll(tempInvoicePaymentTerms);
            return tempInvoicePaymentTerms
                    .stream()
                    .map(this::responseFromEntity)
                    .toList();
        } else {
            return null;
        }
    }

    /**
     * Deletes the payment terms that have been removed from the list.
     *
     * @param requests the list of {@link EditInvoicePaymentTermRequest} objects, which contain the details of the payment terms to be edited
     * @param dbInvoicePaymentTermsOptional the Optional object containing the list of {@link InvoicePaymentTerms} objects that are already in the database
     * @param tempInvoicePaymentTerms the list of {@link InvoicePaymentTerms} objects that will be saved to the database after editing
     */
    private void deleteRemovedInvoicePaymentTerms(List<EditInvoicePaymentTermRequest> requests, Optional<List<InvoicePaymentTerms>> dbInvoicePaymentTermsOptional, List<InvoicePaymentTerms> tempInvoicePaymentTerms) {
        if (dbInvoicePaymentTermsOptional.isPresent() && !dbInvoicePaymentTermsOptional.get().isEmpty()) {
            List<InvoicePaymentTerms> dbInvoicePaymentTerms = dbInvoicePaymentTermsOptional.get();

            List<Long> requestTermsIds = requests.stream().map(EditInvoicePaymentTermRequest::getId).toList();

            for (InvoicePaymentTerms currTerm : dbInvoicePaymentTerms) {
                if (!requestTermsIds.contains(currTerm.getId())) {
                    if (!currTerm.getStatus().equals(PaymentTermStatus.DELETED)) {
                        delete(currTerm, tempInvoicePaymentTerms);
                    }
                }
            }
        }
    }

    /**
     * This method creates a new {@link InvoicePaymentTerms} object based on the provided
     * {@link CreateInvoicePaymentTermRequest} and other parameters.
     *
     * @param tempInvoicePaymentTerms A List of temporary {@link InvoicePaymentTerms}.
     * @param request A {@link CreateInvoicePaymentTermRequest} object that contains the details of the new {@link InvoicePaymentTerms} object.
     * @param exceptionMessage list of errors which is populated in case of exceptions or validation violations
     */
    private void create(List<InvoicePaymentTerms> tempInvoicePaymentTerms,
                        Long termId,
                        CreateInvoicePaymentTermRequest request,
                        List<String> exceptionMessage,
                        int index) {
        Optional<Calendar> calendarOptional = calendarRepository.findByIdAndStatusIsIn(request.getCalendarId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (calendarOptional.isEmpty()) {
            log.error("Active calendar not found by ID: {}", request.getCalendarId());
            exceptionMessage.add(String.format("invoicePaymentTerms[%s].calendarId-Active calendar not found by ID: %s;", index, request.getCalendarId()));
        }

        InvoicePaymentTerms invoicePaymentTerms = new InvoicePaymentTerms();
        invoicePaymentTerms.setCalendarType(request.getCalendarType());
        invoicePaymentTerms.setName(request.getName());
        invoicePaymentTerms.setValue(request.getValue());
        invoicePaymentTerms.setValueFrom(request.getValueFrom());
        invoicePaymentTerms.setValueTo(request.getValueTo());
        invoicePaymentTerms.setCalendarId(request.getCalendarId());
        invoicePaymentTerms.setExcludeWeekends(request.getExcludeWeekends());
        invoicePaymentTerms.setExcludeHolidays(request.getExcludeHolidays());
        invoicePaymentTerms.setDueDateChange(request.getDueDateChange());
        invoicePaymentTerms.setTermId(termId);
        invoicePaymentTerms.setStatus(PaymentTermStatus.ACTIVE); // active by default
        tempInvoicePaymentTerms.add(invoicePaymentTerms);
    }

    /**
     * Edits an invoice payment term based on the given request and updates the temporary list of payment terms.
     *
     * @param tempInvoicePaymentTerms The temporary list of payment terms to update.
     * @param terms The term to which belong the payment terms.
     * @param dbTerms The payment term to edit.
     * @param request The request containing the updated payment term information.
     * @param exceptionMessage The list of exception messages to add to in case of errors.
     * @param index The index of the payment term being edited.
     */
    private void edit(List<InvoicePaymentTerms> tempInvoicePaymentTerms,
                      Terms terms,
                      InvoicePaymentTerms dbTerms,
                      EditInvoicePaymentTermRequest request,
                      List<String> exceptionMessage,
                      int index) {
        if (terms.getGroupDetailId() != null) {
            if (request.getValueFrom() != null || request.getValueTo() != null || request.getValue() == null) {
                log.error("When editing payment term that is connected to a group term, valueFrom and valueTo should be empty and value should be present;");
                exceptionMessage.add(String.format("invoicePaymentTerms[%s].value-When editing payment term that is connected to a group term, " +
                        "valueFrom and valueTo should be empty and value should be present;", index));
                return;
            }
        }

        Optional<Calendar> calendarOptional = calendarRepository.findByIdAndStatusIsIn(
                request.getCalendarId(),
                List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE)
        );

        if (calendarOptional.isEmpty()) {
            log.error("Active or inactive calendar not found by ID: {}", request.getCalendarId());
            exceptionMessage.add(String.format("invoicePaymentTerms[%s].calendarId-Active or inactive calendar not found by ID: %s", index, request.getCalendarId()));
            return;
        }

        if (calendarOptional.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && !request.getCalendarId().equals(dbTerms.getCalendarId())) {
            log.error(String.format("invoicePaymentTerms[%s].calendarId-Cannot save invoice payment terms with different INACTIVE calendar;", index));
            exceptionMessage.add(String.format("invoicePaymentTerms[%s].calendarId-Cannot save invoice payment terms with different INACTIVE calendar;", index));
            return;
        }

        dbTerms.setCalendarType(request.getCalendarType());
        dbTerms.setName(request.getName());
        dbTerms.setValue(request.getValue());
        dbTerms.setValueFrom(request.getValueFrom());
        dbTerms.setValueTo(request.getValueTo());
        dbTerms.setCalendarId(request.getCalendarId());
        dbTerms.setExcludeWeekends(request.getExcludeWeekends());
        dbTerms.setExcludeHolidays(request.getExcludeHolidays());
        dbTerms.setDueDateChange(request.getDueDateChange());
        dbTerms.setTermId(terms.getId());
        tempInvoicePaymentTerms.add(dbTerms);
    }

    /**
     * Deletes the specified invoice payment term from the database by updating its status to DELETED
     * and adding it to the list of temporary invoice payment terms.
     *
     * @param dbTerm the invoice payment term to be deleted from the database
     * @param tempInvoicePaymentTerms the list of temporary invoice payment terms
     */
    public void delete(InvoicePaymentTerms dbTerm, List<InvoicePaymentTerms> tempInvoicePaymentTerms) {
        dbTerm.setStatus(PaymentTermStatus.DELETED);
        tempInvoicePaymentTerms.add(dbTerm);
    }

    /**
     * Maps an InvoicePaymentTerms entity to an InvoicePaymentTermsResponse object.
     *
     * @param entity the entity to map
     * @return the resulting {@link InvoicePaymentTermsResponse} object
     */
    private InvoicePaymentTermsResponse responseFromEntity(InvoicePaymentTerms entity) {
        // At this point of execution, we know that calendar is present in db for sure
        Calendar calendar = calendarRepository.findById(entity.getCalendarId()).get();

        InvoicePaymentTermsResponse build = InvoicePaymentTermsResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .calendarType(entity.getCalendarType())
                .value(entity.getValue())
                .valueFrom(entity.getValueFrom())
                .valueTo(entity.getValueTo())
                .excludeWeekends(entity.getExcludeWeekends())
                .excludeHolidays(entity.getExcludeHolidays())
                .dueDateChange(entity.getDueDateChange())
                .termId(entity.getTermId())
                .status(entity.getStatus())
                .build();
        build.setCalendar(new CalendarResponse(calendar));
        return build;
    }

    public void save(List<CreateInvoicePaymentTermRequest> invoicePaymentTerms, Long termId) {
        List<InvoicePaymentTerms> saveInvoicePaymentTerms = new ArrayList<>();
        for (CreateInvoicePaymentTermRequest item : invoicePaymentTerms) {
            InvoicePaymentTerms saveObj = new InvoicePaymentTerms();
            saveObj.setName(item.getName());
            saveObj.setCalendarType(item.getCalendarType());
            saveObj.setCalendarId(item.getCalendarId());
            saveObj.setValue(item.getValue());
            saveObj.setValueFrom(item.getValueFrom());
            saveObj.setValueTo(item.getValueTo());
            saveObj.setExcludeWeekends(item.getExcludeWeekends());
            saveObj.setExcludeHolidays(item.getExcludeHolidays());
            saveObj.setDueDateChange(item.getDueDateChange());
            saveObj.setTermId(termId);
            saveObj.setStatus(PaymentTermStatus.ACTIVE);
            saveInvoicePaymentTerms.add(saveObj);
        }
        invoicePaymentTermsRepository.saveAll(saveInvoicePaymentTerms);
    }
}
