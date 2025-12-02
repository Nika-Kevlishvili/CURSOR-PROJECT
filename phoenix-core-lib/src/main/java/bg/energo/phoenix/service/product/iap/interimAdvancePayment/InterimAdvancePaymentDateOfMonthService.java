package bg.energo.phoenix.service.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentDateOfMonth;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.CreateInterimAdvancePaymentRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.EditInterimAdvancePaymentRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.DateOfMonthBaseRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDateOfMonthRequest;
import bg.energo.phoenix.model.response.interimAdvancePayment.periodical.DateOfMonthResponse;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentDateOfMonthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterimAdvancePaymentDateOfMonthService {

    private final InterimAdvancePaymentDateOfMonthRepository dateOfMonthRepository;

    /**
     * Create Interim Advance Payment Date of months models based on provided request and persist them {@link InterimAdvancePaymentDateOfMonth}
     * Add error messages if any.
     *
     * @param request - used to create Interim Advance Payment Date of months models
     * @param interimAdvancePayment - Interim Advance payment which is related to created Interim Advance Payment Date of months {@link InterimAdvancePayment}
     * @param errorMessage - used to store messages of errors
     * @return If success - List of Responses which contains complete data about created Interim Advance Payment Date of months {@link DateOfMonthResponse}
     *         If error or not created any Interim Advance Payment Date of month returns null.
     */
    public List<DateOfMonthResponse> createDateOfMonths(CreateInterimAdvancePaymentRequest request, InterimAdvancePayment interimAdvancePayment, StringBuilder errorMessage) {
        if (request.getDayOfWeekAndPeriodOfYearAndDateOfMonth() != null && request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDateOfMonths() != null) {
            Set<DateOfMonthBaseRequest> dateOfMonthList = request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDateOfMonths();
            List<InterimAdvancePaymentDateOfMonth> interimAdvancePaymentDateOfMonths = new ArrayList<>();

            for (DateOfMonthBaseRequest dateOfMonth : dateOfMonthList) {
                interimAdvancePaymentDateOfMonths.add(createDateOfMonth(dateOfMonth, interimAdvancePayment));
            }

            return errorMessage.isEmpty() ? dateOfMonthRepository.saveAll(interimAdvancePaymentDateOfMonths)
                    .stream()
                    .map(DateOfMonthResponse::new)
                    .toList()
                    : null;

        }else return  null;
    }

    /**
     * Create and return Interim Advance Payment Date of month based on provided request {@link InterimAdvancePaymentDateOfMonth}, {@link DateOfMonthBaseRequest}
     *
     * @param dateOfMonth - request used to create model {@link DateOfMonthBaseRequest}
     * @param interimAdvancePayment - Interim Advance Payment which is related to created Interim Advance Payment Date of month {@link InterimAdvancePayment}
     * @return Interim Advance Payment Date of month {@link InterimAdvancePaymentDateOfMonth}
     */
    private InterimAdvancePaymentDateOfMonth createDateOfMonth(DateOfMonthBaseRequest dateOfMonth, InterimAdvancePayment interimAdvancePayment){
        InterimAdvancePaymentDateOfMonth interimAdvancePaymentDateOfMonth = new InterimAdvancePaymentDateOfMonth();
        interimAdvancePaymentDateOfMonth.setInterimAdvancePayment(interimAdvancePayment);
        interimAdvancePaymentDateOfMonth.setMonth(dateOfMonth.getMonth());
        interimAdvancePaymentDateOfMonth.setMonthNumbers(dateOfMonth.getMonthNumbers().stream().toList());
        interimAdvancePaymentDateOfMonth.setStatus(InterimAdvancePaymentSubObjectStatus.ACTIVE);
        return interimAdvancePaymentDateOfMonth;
    }

    /**
     * Find Interim Advance Payment Date of months by Interim Advance Payments id and provided entity statuses
     *
     * @param id - id of Interim Advance Payment {@link InterimAdvancePayment}
     * @param statuses List of Interim Advance Payment Sub-Object Statuses {@link InterimAdvancePaymentSubObjectStatus}
     * @return List of Date of Month Responses {@link DateOfMonthResponse}
     */
    public List<DateOfMonthResponse> findByInterimAdvancePaymentIdAndStatusIn(Long id, List<InterimAdvancePaymentSubObjectStatus> statuses) {
        return dateOfMonthRepository.findByInterimAdvancePaymentIdAndStatusIn(id, statuses)
                .stream()
                .map(DateOfMonthResponse::new)
                .toList();
    }
    public List<InterimAdvancePaymentDateOfMonth> findByInterimAdvancePaymentIdAndStatusInEntity(Long id, List<InterimAdvancePaymentSubObjectStatus> statuses) {
        return dateOfMonthRepository.findByInterimAdvancePaymentIdAndStatusIn(id, statuses);
    }

    /**
     * Delete Interim Advance Payment Date of months by Interim Advance Payments id
     *
     * @param interimAdvancePaymentId - id of Interim Advance Payment
     */
    public void deleteByInterimAdvancePaymentId(Long interimAdvancePaymentId) {
        List<InterimAdvancePaymentDateOfMonth> dateOfMonths = dateOfMonthRepository.findByInterimAdvancePaymentIdAndStatusIn(interimAdvancePaymentId, List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE))
                .stream()
                .peek(interimAdvancePaymentDateOfMonth -> interimAdvancePaymentDateOfMonth.setStatus(InterimAdvancePaymentSubObjectStatus.DELETED))
                .toList();
        dateOfMonthRepository.saveAll(dateOfMonths);
    }

    /**
     * Edit Interim Advance Payment Date of months which are related to provided Interim Advance Payment {@link InterimAdvancePaymentDateOfMonth}, {@link InterimAdvancePayment}
     * Used Map to store already existed Interim Advance Payment Date of months and remove elements from it if edited.
     * If id is provided in request find and edit existed Interim Advance Payment Date of month and remove it from Map
     * If id is not provided in request create new Interim Advance Payment Date of month
     * Finally, if elements are left in Map, they must be deleted, because their ids were not in current request
     *
     * @param request - used to edit Interim Advance Payment Date of months {@link EditInterimAdvancePaymentRequest}
     * @param interimAdvancePayment - Interim Advance Payment which is connected to Interim Advance Payment Date of month
     * @param errorMessage - used to store error messages if any.
     * @return List of Responses of created Interim Advance Payment Date of months {@link DateOfMonthResponse}
     *         return null if any error happened or there is no created Interim Advance Payment Date of months
     */
    public List<DateOfMonthResponse> editDateOfMonths(EditInterimAdvancePaymentRequest request, InterimAdvancePayment interimAdvancePayment, StringBuilder errorMessage) {
        if (request.getDayOfWeekAndPeriodOfYearAndDateOfMonth() != null
            && request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDateOfMonths() != null) {

            Set<EditDateOfMonthRequest> newDateOfMonths = request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDateOfMonths();
            Map<Long, InterimAdvancePaymentDateOfMonth> existedDateOfMonths = dateOfMonthRepository
                    .findByInterimAdvancePaymentIdAndStatusIn(interimAdvancePayment.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE))
                    .stream()
                    .collect(Collectors.toMap(
                            InterimAdvancePaymentDateOfMonth::getId,
                            interimAdvancePaymentDateOfMonth -> interimAdvancePaymentDateOfMonth
                    ));

            List<InterimAdvancePaymentDateOfMonth> createdOrUpdatedDateOfMonths = new ArrayList<>();

            for (EditDateOfMonthRequest dateOfMonth : newDateOfMonths) {
                InterimAdvancePaymentDateOfMonth interimAdvancePaymentDateOfMonth =
                        findEditedinterimAdvancePaymentDateOfMonth(dateOfMonth, existedDateOfMonths, interimAdvancePayment, errorMessage);
                createdOrUpdatedDateOfMonths.add(editDateOfMonth(dateOfMonth, interimAdvancePaymentDateOfMonth, interimAdvancePayment));
            }

            if (!errorMessage.isEmpty()) return null;
            deleteOldDateOfMonths(existedDateOfMonths);
            return dateOfMonthRepository.saveAll(createdOrUpdatedDateOfMonths)
                    .stream()
                    .map(DateOfMonthResponse::new)
                    .toList();
        }else{
            deleteByInterimAdvancePaymentId(interimAdvancePayment.getId());
            return null;
        }
    }

    /**
     * If current request contains id of Interim Advance Payment Date of month, search it in Map of already existed Interim Advance Payment Date of months
     * If found return it and remove from Map, If Not found add message in error messages and return new Interim Advance Payment Date of month
     *
     * @param dateOfMonth  - request used to edit Interim Advance Payment Date of month {@link EditDateOfMonthRequest}
     * @param existedDateOfMonths - Map of already existed Interim Advance Payment Date of months {@link InterimAdvancePaymentDateOfMonth}
     * @param interimAdvancePayment - Interim Advance Payment which is connected to Interim Advance Payment Date of month {@link InterimAdvancePayment}
     * @param errorMessage - used to store error messages if any
     * @return Interim Advance Payment Date of month {@link InterimAdvancePaymentDateOfMonth}
     */
    private InterimAdvancePaymentDateOfMonth findEditedinterimAdvancePaymentDateOfMonth(
            EditDateOfMonthRequest dateOfMonth,
            Map<Long, InterimAdvancePaymentDateOfMonth> existedDateOfMonths,
            InterimAdvancePayment interimAdvancePayment,
            StringBuilder errorMessage
    ){
        InterimAdvancePaymentDateOfMonth interimAdvancePaymentDateOfMonth;
        if (dateOfMonth.getId() != null) {
            if (existedDateOfMonths.containsKey(dateOfMonth.getId())) {
                interimAdvancePaymentDateOfMonth = existedDateOfMonths.get(dateOfMonth.getId());
                existedDateOfMonths.remove(dateOfMonth.getId());
            } else {
                errorMessage.append("Interim Advance Payment Date Of Month not found by id: ")
                        .append(dateOfMonth.getId()).append(" and interim advance payment id: ")
                        .append(interimAdvancePayment.getId()).append(";");
                interimAdvancePaymentDateOfMonth = new InterimAdvancePaymentDateOfMonth();
            }
        } else interimAdvancePaymentDateOfMonth = new InterimAdvancePaymentDateOfMonth();
        return interimAdvancePaymentDateOfMonth;
    }

    /**
     * Edit provided Interim Advance Payment Date of month based on received request
     *
     * @param dateOfMonth - request used to edit Interim Advance Payment Date of month {@link EditDateOfMonthRequest}
     * @param interimAdvancePaymentDateOfMonth - Interim Advance Payment Date of month which is being edited {@link InterimAdvancePaymentDateOfMonth}
     * @param interimAdvancePayment - Interim Advance Payment which is connected to Interim Advance Payment Date of month {@link InterimAdvancePayment}
     * @return Edited Interim Advance Payment Date of month {@link InterimAdvancePaymentDateOfMonth}
     */
    private InterimAdvancePaymentDateOfMonth editDateOfMonth(EditDateOfMonthRequest dateOfMonth,
                                                             InterimAdvancePaymentDateOfMonth interimAdvancePaymentDateOfMonth,
                                                             InterimAdvancePayment interimAdvancePayment){
        interimAdvancePaymentDateOfMonth.setInterimAdvancePayment(interimAdvancePayment);
        interimAdvancePaymentDateOfMonth.setMonth(dateOfMonth.getMonth());
        interimAdvancePaymentDateOfMonth.setMonthNumbers(dateOfMonth.getMonthNumbers().stream().toList());
        interimAdvancePaymentDateOfMonth.setStatus(InterimAdvancePaymentSubObjectStatus.ACTIVE);
        return interimAdvancePaymentDateOfMonth;
    }

    /**
     * Delete received Interim Advance Payment Date of months
     *
     * @param leftDateOfMonths - Map of Interim Advance Payment Date of months which must be deleted
     */
    private void deleteOldDateOfMonths(Map<Long, InterimAdvancePaymentDateOfMonth> leftDateOfMonths) {
        List<InterimAdvancePaymentDateOfMonth> left = leftDateOfMonths.values().stream()
                .peek(interimAdvancePaymentDateOfMonth -> interimAdvancePaymentDateOfMonth.setStatus(InterimAdvancePaymentSubObjectStatus.DELETED))
                .toList();
        dateOfMonthRepository.saveAll(left);
    }


    public void saveAll(List<InterimAdvancePaymentDateOfMonth> dateOfMonthsToSave) {
        dateOfMonthRepository.saveAll(dateOfMonthsToSave);
    }
}
