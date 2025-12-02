package bg.energo.phoenix.service.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentTerms;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.CreateInterimAdvancePaymentRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.CreateInterimAdvancePaymentTermRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.EditInterimAdvancePaymentRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.EditInterimAdvancePaymentTermRequest;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentTermsResponse;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentTermsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterimAdvancePaymentTermsService {

    private final CalendarRepository calendarRepository;

    private final InterimAdvancePaymentTermsRepository interimAdvancePaymentTermsRepository;

    /**
     * Create Interim Advance Payment Terms model based on provided request and persist it {@link InterimAdvancePaymentTerms}
     * Add error messages if any.
     *
     * @param interimAdvancePaymentRequest - used to create Interim Advance Payment Terms models
     * @param interimAdvancePayment        - Interim Advance payment which is related to created Interim Advance Payment Terms {@link InterimAdvancePayment}
     * @param errorMessage                 - used to store messages of errors
     * @return If success - Response which contains complete data about created Interim Advance Payment Terms {@link InterimAdvancePaymentTermsResponse}
     * If error or not created any Interim Advance Payment Terms returns null.
     */
    public InterimAdvancePaymentTermsResponse create(CreateInterimAdvancePaymentRequest interimAdvancePaymentRequest, InterimAdvancePayment interimAdvancePayment, StringBuilder errorMessage) {
        if (interimAdvancePaymentRequest.getInterimAdvancePaymentTerm() != null) {
            CreateInterimAdvancePaymentTermRequest request = interimAdvancePaymentRequest.getInterimAdvancePaymentTerm();
            InterimAdvancePaymentTerms interimAdvancePaymentTerms = new InterimAdvancePaymentTerms();
            interimAdvancePaymentTerms.setInterimAdvancePayment(interimAdvancePayment);
            interimAdvancePaymentTerms.setCalendarType(request.getCalendarType());
            interimAdvancePaymentTerms.setCalendar(getCalendar(request.getCalendarId(), errorMessage));
            interimAdvancePaymentTerms.setValue(request.getValue());
            interimAdvancePaymentTerms.setValueFrom(request.getValueFrom());
            interimAdvancePaymentTerms.setValueTo(request.getValueTo());
            interimAdvancePaymentTerms.setExcludeHolidays(request.getExcludeHolidays());
            interimAdvancePaymentTerms.setExcludeWeekends(request.getExcludeWeekends());
            interimAdvancePaymentTerms.setName(request.getName());
            interimAdvancePaymentTerms.setDueDateChange(request.getDueDateChange());
            interimAdvancePaymentTerms.setStatus(InterimAdvancePaymentSubObjectStatus.ACTIVE);

            return errorMessage.isEmpty() ? new InterimAdvancePaymentTermsResponse(interimAdvancePaymentTermsRepository.save(interimAdvancePaymentTerms)) : null;
        } else return null;

    }

    /**
     * Find calendar by id and return it. If not Found add  error message. {@link Calendar}
     *
     * @param id           - id of calendar
     * @param errorMessage - used to store error messages
     * @return Calendar {@link Calendar}
     */
    private Calendar getCalendar(Long id, StringBuilder errorMessage) {
        Optional<Calendar> optionalCalendar = calendarRepository.findByIdAndStatusIsIn(id, List.of(NomenclatureItemStatus.ACTIVE));
        if (optionalCalendar.isEmpty()) {
            errorMessage.append("interimAdvancePaymentTermRequest.calendarId-Calendar not found by id: ").append(id).append(";");
            return null;
        } else return optionalCalendar.get();
    }

    private Calendar getCalendarOnUpdate(Long id, Calendar oldCalendar, StringBuilder errorMessage) {
        List<NomenclatureItemStatus> statuses = new ArrayList<>(List.of(NomenclatureItemStatus.ACTIVE));

        if (Objects.nonNull(oldCalendar) && Objects.equals(oldCalendar.getId(), id)) {
            statuses.add(NomenclatureItemStatus.INACTIVE);
        }

        Optional<Calendar> optionalCalendar = calendarRepository.findByIdAndStatusIsIn(id, statuses);
        if (optionalCalendar.isEmpty()) {
            errorMessage.append("interimAdvancePaymentTermRequest.calendarId-Calendar not found by id: ").append(id).append(";");
            return null;
        } else return optionalCalendar.get();
    }

    /**
     * Find Interim Advance Payment Terms by Interim Advance Payments id and provided entity statuses
     *
     * @param id       - id of Interim Advance Payment {@link InterimAdvancePayment}
     * @param statuses List of Interim Advance Payment Sub-Object Statuses {@link InterimAdvancePaymentSubObjectStatus}
     * @return Terms Response {@link InterimAdvancePaymentTermsResponse}
     */
    public InterimAdvancePaymentTermsResponse findByInterimAdvancePaymentIdAndStatusIn(Long id, List<InterimAdvancePaymentSubObjectStatus> statuses) {
        InterimAdvancePaymentTerms interimAdvancePaymentTerms = interimAdvancePaymentTermsRepository.findByInterimAdvancePaymentIdAndStatusInOrderByCreateDate(id, statuses)
                .orElse(null);
        return new InterimAdvancePaymentTermsResponse(interimAdvancePaymentTerms);
    }
    public InterimAdvancePaymentTermsResponse findByInterimAdvancePaymentIdAndStatusInForServiceContract(Long id, List<InterimAdvancePaymentSubObjectStatus> statuses) {
        InterimAdvancePaymentTerms interimAdvancePaymentTerms = interimAdvancePaymentTermsRepository.findByInterimAdvancePaymentIdAndStatusInOrderByCreateDate(id, statuses)
                .orElse(null);
        return new InterimAdvancePaymentTermsResponse(interimAdvancePaymentTerms);
    }

    /**
     * Delete Interim Advance Payment Terms by Interim Advance Payments id
     *
     * @param interimAdvancePaymentId - id of Interim Advance Payment
     */
    public void deleteByInterimAdvancePaymentId(Long interimAdvancePaymentId) {
        Optional<InterimAdvancePaymentTerms> optionalInterimAdvancePaymentTerms = interimAdvancePaymentTermsRepository.findByInterimAdvancePaymentIdAndStatusInOrderByCreateDate(interimAdvancePaymentId, List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
        if (optionalInterimAdvancePaymentTerms.isPresent()) {
            InterimAdvancePaymentTerms interimAdvancePaymentTerms = optionalInterimAdvancePaymentTerms.get();
            interimAdvancePaymentTerms.setStatus(InterimAdvancePaymentSubObjectStatus.DELETED);
            interimAdvancePaymentTermsRepository.save(interimAdvancePaymentTerms);
        }
    }

    /**
     * Edit Interim Advance Payment Terms which are related to provided Interim Advance Payment {@link InterimAdvancePaymentTerms}, {@link InterimAdvancePayment}
     * If id is provided in request find and edit existed Interim Advance Payment Terms
     * If id is not provided in request create new Interim Advance Payment Terms
     * Finally, if id of already existed Terms is not provided in request it is deleted
     *
     * @param interimAdvancePaymentRequest - used to edit Interim Advance Payment Terms {@link EditInterimAdvancePaymentRequest}
     * @param interimAdvancePayment        - Interim Advance Payment which is connected to Interim Advance Payment Date of month
     * @param errorMessage                 - used to store error messages if any.
     * @return Response of created Interim Advance Payment Terms {@link InterimAdvancePaymentTermsResponse}
     * return null if any error happened or there is no created Interim Advance Payment Terms
     */
    public InterimAdvancePaymentTermsResponse edit(EditInterimAdvancePaymentRequest interimAdvancePaymentRequest, InterimAdvancePayment interimAdvancePayment, StringBuilder errorMessage) {
        EditInterimAdvancePaymentTermRequest interimAdvancePaymentTermRequest = interimAdvancePaymentRequest.getInterimAdvancePaymentTerm();
        if (interimAdvancePaymentTermRequest != null) {
            InterimAdvancePaymentTerms interimAdvancePaymentTerms = getInterimAdvancePaymentTerms(interimAdvancePaymentTermRequest.getId(), interimAdvancePayment, errorMessage);
            interimAdvancePaymentTerms.setInterimAdvancePayment(interimAdvancePayment);
            interimAdvancePaymentTerms.setCalendarType(interimAdvancePaymentTermRequest.getCalendarType());
            interimAdvancePaymentTerms.setCalendar(getCalendarOnUpdate(interimAdvancePaymentTermRequest.getCalendarId(), interimAdvancePaymentTerms.getCalendar(), errorMessage));
            interimAdvancePaymentTerms.setValue(interimAdvancePaymentTermRequest.getValue());
            interimAdvancePaymentTerms.setValueFrom(interimAdvancePaymentTermRequest.getValueFrom());
            interimAdvancePaymentTerms.setValueTo(interimAdvancePaymentTermRequest.getValueTo());
            interimAdvancePaymentTerms.setExcludeHolidays(interimAdvancePaymentTermRequest.getExcludeHolidays());
            interimAdvancePaymentTerms.setExcludeWeekends(interimAdvancePaymentTermRequest.getExcludeWeekends());
            interimAdvancePaymentTerms.setName(interimAdvancePaymentTermRequest.getName());
            interimAdvancePaymentTerms.setDueDateChange(interimAdvancePaymentTermRequest.getDueDateChange());
            interimAdvancePaymentTerms.setStatus(InterimAdvancePaymentSubObjectStatus.ACTIVE);

            return errorMessage.isEmpty() ? new InterimAdvancePaymentTermsResponse(interimAdvancePaymentTermsRepository.save(interimAdvancePaymentTerms)) : null;
        } else {
            deleteByInterimAdvancePaymentId(interimAdvancePayment.getId());
            return null;
        }
    }

    /**
     * Find Interim Advance Payment Terms by id and Interim Advance Payment id and return it. {@link InterimAdvancePaymentTerms}
     * If not Found add error message and return new Interim Advance Payment Terms Object
     *
     * @param id                    - id of Interim Advance Payment Terms
     * @param interimAdvancePayment - Interim Advance Payment which is used to search Interim Advance Payment Terms {@link InterimAdvancePayment}
     * @param errorMessage          - used to store error messages
     * @return Interim Advance Payment Terms {@link InterimAdvancePaymentTerms}
     */
    private InterimAdvancePaymentTerms getInterimAdvancePaymentTerms(Long id, InterimAdvancePayment interimAdvancePayment, StringBuilder errorMessage) {
        InterimAdvancePaymentTerms interimAdvancePaymentTerms;
        if (id != null) {
            interimAdvancePaymentTerms = interimAdvancePaymentTermsRepository.findByIdAndInterimAdvancePaymentIdAndStatusIn(id, interimAdvancePayment.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE))
                    .orElseGet(() -> {
                        errorMessage.append("Interim Advance Payment Terms not found by id: ").append(id).append(" and interim advance payment id: ").append(interimAdvancePayment.getId()).append(";");
                        return new InterimAdvancePaymentTerms();
                    });
        } else {
            deleteByInterimAdvancePaymentId(interimAdvancePayment.getId());
            interimAdvancePaymentTerms = new InterimAdvancePaymentTerms();
        }
        return interimAdvancePaymentTerms;
    }

}
