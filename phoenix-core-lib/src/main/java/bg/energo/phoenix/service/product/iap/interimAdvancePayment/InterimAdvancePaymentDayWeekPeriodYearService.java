package bg.energo.phoenix.service.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentDayWeekPeriodYear;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.CreateInterimAdvancePaymentRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.EditInterimAdvancePaymentRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.DayOfWeekBaseRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDayOfWeekRequest;
import bg.energo.phoenix.model.response.interimAdvancePayment.periodical.DayOfWeekResponse;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentDayWeekPeriodYearRepository;
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
public class InterimAdvancePaymentDayWeekPeriodYearService {

    private final InterimAdvancePaymentDayWeekPeriodYearRepository dayWeekPeriodYearRepository;

    /**
     * Create Interim Advance Payment Day Week Period Year models based on provided request and persist them {@link InterimAdvancePaymentDayWeekPeriodYear}
     * Add error messages if any.
     *
     * @param request - used to create Interim Advance Payment Day Week Period Years
     * @param interimAdvancePayment - Interim Advance payment which is related to created Interim Advance Payment Day Week Period Years {@link InterimAdvancePayment}
     * @param errorMessage - used to store messages of errors
     * @return If success - List of Responses which contains complete data about created Interim Advance Payment Day Week Period Years {@link DayOfWeekResponse}
     *         If error or not created any Interim Advance Payment Day Week Period Year returns null.
     */
    public List<DayOfWeekResponse> createDaysOfWeek(CreateInterimAdvancePaymentRequest request, InterimAdvancePayment interimAdvancePayment, StringBuilder errorMessage) {
        if (request.getDayOfWeekAndPeriodOfYearAndDateOfMonth() != null
            && request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear() != null
            && request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear().getDaysOfWeek() != null) {

            Set<DayOfWeekBaseRequest> daysOfWeek = request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear().getDaysOfWeek();
            List<InterimAdvancePaymentDayWeekPeriodYear> interimAdvancePaymentDayWeekPeriodYears = new ArrayList<>();
            for (DayOfWeekBaseRequest dayOfWeek : daysOfWeek) {
                interimAdvancePaymentDayWeekPeriodYears.add(createDayWeekPeriodYear(interimAdvancePayment, dayOfWeek));
            }

            return errorMessage.isEmpty()
            ?dayWeekPeriodYearRepository.saveAll(interimAdvancePaymentDayWeekPeriodYears)
                    .stream()
                    .map(DayOfWeekResponse::new)
                    .toList()
                    : null;

        }else  return null;

    }

    /**
     * Create and return Interim Advance Payment Day Week Period Year based on provided request {@link InterimAdvancePaymentDayWeekPeriodYear}, {@link DayOfWeekBaseRequest}
     *
     * @param dayOfWeek - request used to create model {@link DayOfWeekBaseRequest}
     * @param interimAdvancePayment - Interim Advance Payment which is related to created Interim Advance Payment Day Week Period Year {@link InterimAdvancePayment}
     * @return Interim Advance Payment Day Week Period Year {@link InterimAdvancePaymentDayWeekPeriodYear}
     */
    private InterimAdvancePaymentDayWeekPeriodYear createDayWeekPeriodYear(InterimAdvancePayment interimAdvancePayment, DayOfWeekBaseRequest dayOfWeek){
        InterimAdvancePaymentDayWeekPeriodYear interimAdvancePaymentDayWeekPeriodYear = new InterimAdvancePaymentDayWeekPeriodYear();
        interimAdvancePaymentDayWeekPeriodYear.setInterimAdvancePayment(interimAdvancePayment);
        interimAdvancePaymentDayWeekPeriodYear.setWeek(dayOfWeek.getWeek());
        interimAdvancePaymentDayWeekPeriodYear.setDays(dayOfWeek.getDays().stream().toList());
        interimAdvancePaymentDayWeekPeriodYear.setStatus(InterimAdvancePaymentSubObjectStatus.ACTIVE);
        return interimAdvancePaymentDayWeekPeriodYear;
    }

    /**
     * Find Interim Advance Payment Day Week Period Years by Interim Advance Payments id and provided entity statuses
     *
     * @param id - id of Interim Advance Payment {@link InterimAdvancePayment}
     * @param statuses List of Interim Advance Payment Sub-Object Statuses {@link InterimAdvancePaymentSubObjectStatus}
     * @return List of Day Week Period Year Responses {@link DayOfWeekResponse}
     */
    public List<DayOfWeekResponse> findByInterimAdvancePaymentIdAndStatusIn(Long id, List<InterimAdvancePaymentSubObjectStatus> statuses) {
        return dayWeekPeriodYearRepository.findByInterimAdvancePaymentIdAndStatusIn(id, statuses)
                .stream()
                .map(DayOfWeekResponse::new)
                .toList();
    }
    public List<InterimAdvancePaymentDayWeekPeriodYear> findByInterimAdvancePaymentIdAndStatusInEntity(Long id, List<InterimAdvancePaymentSubObjectStatus> statuses) {
        return dayWeekPeriodYearRepository.findByInterimAdvancePaymentIdAndStatusIn(id, statuses);
    }

    /**
     * Delete Interim Advance Payment Day Week Period Years by Interim Advance Payments id
     *
     * @param interimAdvancePaymentId - id of Interim Advance Payment
     */
    public void deleteByInterimAdvancePaymentId(Long interimAdvancePaymentId) {
        List<InterimAdvancePaymentDayWeekPeriodYear> dayWeekPeriodYears = dayWeekPeriodYearRepository.findByInterimAdvancePaymentIdAndStatusIn(interimAdvancePaymentId, List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE))
                .stream()
                .peek(interimAdvancePaymentDayWeekPeriodYear -> interimAdvancePaymentDayWeekPeriodYear.setStatus(InterimAdvancePaymentSubObjectStatus.DELETED))
                .toList();
        dayWeekPeriodYearRepository.saveAll(dayWeekPeriodYears);
    }

    /**
     * Edit Interim Advance Payment Day Week Period Years which are related to provided Interim Advance Payment {@link InterimAdvancePaymentDayWeekPeriodYear}, {@link InterimAdvancePayment}
     * Used Map to store already existed Interim Advance Payment Day Week Period Years and remove elements from it if edited.
     * If id is provided in request find and edit existed Interim Advance Payment Day Week Period Year and remove it from Map
     * If id is not provided in request create new Interim Advance Payment Day Week Period Year
     * Finally, if elements are left in Map, they must be deleted, because their ids were not in current request
     *
     * @param request - used to edit Interim Advance Payment Day Week Period Years {@link EditInterimAdvancePaymentRequest}
     * @param interimAdvancePayment - Interim Advance Payment which is connected to Interim Advance Payment Day Week Period Year
     * @param errorMessage - used to store error messages if any.
     * @return List of Responses of created Interim Advance Payment Day Week Period Years {@link DayOfWeekResponse}
     *         return null if any error happened or there is no created Interim Advance Payment Day Week Period Years
     */
    public List<DayOfWeekResponse> editDaysOfWeek(EditInterimAdvancePaymentRequest request, InterimAdvancePayment interimAdvancePayment, StringBuilder errorMessage) {
        if (request.getDayOfWeekAndPeriodOfYearAndDateOfMonth() != null
            && request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear() != null
            && request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear().getDaysOfWeek() != null) {

            Set<EditDayOfWeekRequest> daysOfWeek = request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear().getDaysOfWeek();
            Map<Long, InterimAdvancePaymentDayWeekPeriodYear> existedDaysOfWeek = dayWeekPeriodYearRepository.findByInterimAdvancePaymentIdAndStatusIn(interimAdvancePayment.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE))
                    .stream()
                    .collect(Collectors.toMap(
                            InterimAdvancePaymentDayWeekPeriodYear::getId,
                            interimAdvancePaymentDayWeekPeriodYear -> interimAdvancePaymentDayWeekPeriodYear
                    ));

            List<InterimAdvancePaymentDayWeekPeriodYear> interimAdvancePaymentDayWeekPeriodYears = new ArrayList<>();

            for (EditDayOfWeekRequest dayOfWeek : daysOfWeek) {
                InterimAdvancePaymentDayWeekPeriodYear interimAdvancePaymentDayWeekPeriodYear =
                        findEditedDayWeekPeriodYear(dayOfWeek, existedDaysOfWeek, interimAdvancePayment, errorMessage);
                interimAdvancePaymentDayWeekPeriodYears.add(editDayWeekPeriodYear(interimAdvancePaymentDayWeekPeriodYear,interimAdvancePayment, dayOfWeek));
            }

            if (!errorMessage.isEmpty()) return null;
            deleteOldDaysOfWeek(existedDaysOfWeek);
            return dayWeekPeriodYearRepository.saveAll(interimAdvancePaymentDayWeekPeriodYears)
                    .stream()
                    .map(DayOfWeekResponse::new)
                    .toList();
        }else{
            deleteByInterimAdvancePaymentId(interimAdvancePayment.getId());
            return null;
        }
    }

    /**
     * If current request contains id of Interim Advance Payment Day Week Period Year, search it in Map of already existed Interim Advance Payment Day Week Period Years
     * If found return it and remove from Map, If Not found add message in error messages and return new Interim Advance Payment Day Week Period Year
     *
     * @param dayOfWeek  - request used to edit Interim Advance Payment Day Week Period Year {@link EditDayOfWeekRequest}
     * @param existedDaysOfWeek - Map of already existed Interim Advance Payment Day Week Period Years {@link InterimAdvancePaymentDayWeekPeriodYear}
     * @param interimAdvancePayment - Interim Advance Payment which is connected to Interim Advance Payment Day Week Period Year {@link InterimAdvancePayment}
     * @param errorMessage - used to store error messages if any
     * @return Interim Advance Payment Day Week Period Year {@link InterimAdvancePaymentDayWeekPeriodYear}
     */
    private InterimAdvancePaymentDayWeekPeriodYear findEditedDayWeekPeriodYear(EditDayOfWeekRequest dayOfWeek,
                                                                               Map<Long, InterimAdvancePaymentDayWeekPeriodYear> existedDaysOfWeek,
                                                                               InterimAdvancePayment interimAdvancePayment,
                                                                               StringBuilder errorMessage){
        InterimAdvancePaymentDayWeekPeriodYear interimAdvancePaymentDayWeekPeriodYear;
        if (dayOfWeek.getId() != null) {
            if (existedDaysOfWeek.containsKey(dayOfWeek.getId())) {
                interimAdvancePaymentDayWeekPeriodYear = existedDaysOfWeek.get(dayOfWeek.getId());
                existedDaysOfWeek.remove(dayOfWeek.getId());
            } else {
                errorMessage.append("Interim Advance Payment Day Week Period Year not found by id: ")
                        .append(dayOfWeek.getId()).append(" and interim advance payment id: ")
                        .append(interimAdvancePayment.getId()).append(";");
                interimAdvancePaymentDayWeekPeriodYear = new InterimAdvancePaymentDayWeekPeriodYear();
            }
        } else interimAdvancePaymentDayWeekPeriodYear = new InterimAdvancePaymentDayWeekPeriodYear();
        return interimAdvancePaymentDayWeekPeriodYear;
    }

    /**
     * Edit provided Interim Advance Payment Day Week Period Year based on received request
     *
     * @param dayOfWeek - request used to edit Interim Advance Payment Day Week Period Year {@link EditDayOfWeekRequest}
     * @param interimAdvancePaymentDayWeekPeriodYear - Interim Advance Payment Day Week Period Year which is being edited {@link InterimAdvancePaymentDayWeekPeriodYear}
     * @param interimAdvancePayment - Interim Advance Payment which is connected to Interim Advance Payment Day Week Period Year {@link InterimAdvancePayment}
     * @return Edited Interim Advance Payment Day Week Period Year {@link InterimAdvancePaymentDayWeekPeriodYear}
     */
    private InterimAdvancePaymentDayWeekPeriodYear editDayWeekPeriodYear(InterimAdvancePaymentDayWeekPeriodYear interimAdvancePaymentDayWeekPeriodYear,
                                                                         InterimAdvancePayment interimAdvancePayment,
                                                                         EditDayOfWeekRequest dayOfWeek){

        interimAdvancePaymentDayWeekPeriodYear.setInterimAdvancePayment(interimAdvancePayment);
        interimAdvancePaymentDayWeekPeriodYear.setWeek(dayOfWeek.getWeek());
        interimAdvancePaymentDayWeekPeriodYear.setDays(dayOfWeek.getDays().stream().toList());
        interimAdvancePaymentDayWeekPeriodYear.setStatus(InterimAdvancePaymentSubObjectStatus.ACTIVE);
        return interimAdvancePaymentDayWeekPeriodYear;
    }

    /**
     * Delete received Interim Advance Payment Day Week Period Years
     *
     * @param leftDaysOfWeek - Map of Interim Advance Payment Day Week Period Years which must be deleted
     */
    private void deleteOldDaysOfWeek(Map<Long, InterimAdvancePaymentDayWeekPeriodYear> leftDaysOfWeek) {
        List<InterimAdvancePaymentDayWeekPeriodYear> left = leftDaysOfWeek.values().stream()
                .peek(interimAdvancePaymentDayWeekPeriodYear -> interimAdvancePaymentDayWeekPeriodYear.setStatus(InterimAdvancePaymentSubObjectStatus.DELETED))
                .toList();
        dayWeekPeriodYearRepository.saveAll(left);
    }


    public void saveAll(List<InterimAdvancePaymentDayWeekPeriodYear> dayOfWeekToSave) {
        dayWeekPeriodYearRepository.saveAll(dayOfWeekToSave);
    }
}
