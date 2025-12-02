package bg.energo.phoenix.service.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentIssuingPeriod;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.CreateInterimAdvancePaymentRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.EditInterimAdvancePaymentRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.PeriodOfYearBaseRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditPeriodOfYearRequest;
import bg.energo.phoenix.model.response.interimAdvancePayment.periodical.PeriodOfYearResponse;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentIssuingPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterimAdvancePaymentIssuingPeriodService {

    private final InterimAdvancePaymentIssuingPeriodRepository interimAdvancePaymentIssuingPeriodRepository;


    /**
     * Create Interim Advance Payment Issuing Periods models based on provided request and persist them {@link InterimAdvancePaymentIssuingPeriod}
     * Add error messages if any.
     *
     * @param request - used to create Interim Advance Payment Issuing Periods models
     * @param interimAdvancePayment - Interim Advance payment which is related to created Interim Advance Payment Issuing Periods {@link InterimAdvancePayment}
     * @param errorMessage - used to store messages of errors
     * @return If success - List of Responses which contains complete data about created Interim Advance Payment Issuing Periods {@link PeriodOfYearResponse}
     *         If error or not created any Interim Advance Payment Issuing Period returns null.
     */
    public List<PeriodOfYearResponse> createPeriodsOfYear(CreateInterimAdvancePaymentRequest request, InterimAdvancePayment interimAdvancePayment, StringBuilder errorMessage) {
        if (request.getDayOfWeekAndPeriodOfYearAndDateOfMonth() !=  null
            && request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear() != null
            && request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear().getPeriodsOfYear() != null) {

            List<PeriodOfYearBaseRequest> periodOfYears = request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear().getPeriodsOfYear();
            List<InterimAdvancePaymentIssuingPeriod> interimAdvancePaymentIssuingPeriods = new ArrayList<>();
            for (PeriodOfYearBaseRequest periodOfYear : periodOfYears) {
                interimAdvancePaymentIssuingPeriods.add(createIssuingPeriod(interimAdvancePayment, periodOfYear));
            }
            if (!errorMessage.isEmpty()) return null;
            return interimAdvancePaymentIssuingPeriodRepository.saveAll(interimAdvancePaymentIssuingPeriods)
                    .stream()
                    .map(PeriodOfYearResponse::new)
                    .toList();
        }
        return null;
    }

    /**
     * Create and return Interim Advance Payment Issuing Period based on provided request {@link InterimAdvancePaymentIssuingPeriod}, {@link PeriodOfYearBaseRequest}
     *
     * @param periodOfYear - request used to create model {@link PeriodOfYearBaseRequest}
     * @param interimAdvancePayment - Interim Advance Payment which is related to created Interim Advance Payment Issuing Period {@link InterimAdvancePayment}
     * @return Interim Advance Payment Issuing Period {@link InterimAdvancePaymentIssuingPeriod}
     */
    private InterimAdvancePaymentIssuingPeriod createIssuingPeriod(InterimAdvancePayment interimAdvancePayment,
                                                                   PeriodOfYearBaseRequest periodOfYear){
        InterimAdvancePaymentIssuingPeriod interimAdvancePaymentIssuingPeriod = new InterimAdvancePaymentIssuingPeriod();
        interimAdvancePaymentIssuingPeriod.setInterimAdvancePayment(interimAdvancePayment);
        interimAdvancePaymentIssuingPeriod.setPeriodFrom(periodOfYear.getStartDate());
        interimAdvancePaymentIssuingPeriod.setPeriodTo(periodOfYear.getEndDate());
        interimAdvancePaymentIssuingPeriod.setStatus(InterimAdvancePaymentSubObjectStatus.ACTIVE);
        return interimAdvancePaymentIssuingPeriod;
    }

    /**
     * Find Interim Advance Payment Issuing Periods by Interim Advance Payments id and provided entity statuses
     *
     * @param id - id of Interim Advance Payment {@link InterimAdvancePayment}
     * @param statuses List of Interim Advance Payment Sub-Object Statuses {@link InterimAdvancePaymentSubObjectStatus}
     * @return List of Date of Month Responses {@link PeriodOfYearResponse}
     */
    public List<PeriodOfYearResponse> findByInterimAdvancePaymentIdAndStatusIn(Long id, List<InterimAdvancePaymentSubObjectStatus> statuses) {
        return interimAdvancePaymentIssuingPeriodRepository.findByInterimAdvancePaymentIdAndStatusIn(id, statuses)
                .stream()
                .map(PeriodOfYearResponse::new)
                .toList();
    }
    public List<InterimAdvancePaymentIssuingPeriod> findByInterimAdvancePaymentIdAndStatusInEntity(Long id, List<InterimAdvancePaymentSubObjectStatus> statuses) {
        return interimAdvancePaymentIssuingPeriodRepository.findByInterimAdvancePaymentIdAndStatusIn(id, statuses);
    }

    /**
     * Delete Interim Advance Payment Issuing Periods by Interim Advance Payments id
     *
     * @param interimAdvancePaymentId - id of Interim Advance Payment
     */
    public void deleteByInterimAdvancePaymentId(Long interimAdvancePaymentId) {
        List<InterimAdvancePaymentIssuingPeriod> issuingPeriods = interimAdvancePaymentIssuingPeriodRepository.findByInterimAdvancePaymentIdAndStatusIn(interimAdvancePaymentId, List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE))
                .stream()
                .peek(interimAdvancePaymentIssuingPeriod -> interimAdvancePaymentIssuingPeriod.setStatus(InterimAdvancePaymentSubObjectStatus.DELETED))
                .toList();
        interimAdvancePaymentIssuingPeriodRepository.saveAll(issuingPeriods);
    }

    /**
     * Edit Interim Advance Payment Issuing Periods which are related to provided Interim Advance Payment {@link InterimAdvancePaymentIssuingPeriod}, {@link InterimAdvancePayment}
     * Used Map to store already existed Interim Advance Payment Issuing Periods and remove elements from it if edited.
     * If id is provided in request find and edit existed Interim Advance Payment Issuing Period and remove it from Map
     * If id is not provided in request create new Interim Advance Payment Issuing Period
     * Finally, if elements are left in Map, they must be deleted, because their ids were not in current request
     *
     * @param request - used to edit Interim Advance Payment Issuing Periods {@link EditInterimAdvancePaymentRequest}
     * @param interimAdvancePayment - Interim Advance Payment which is connected to Interim Advance Payment Issuing Period
     * @param errorMessage - used to store error messages if any.
     * @return List of Responses of created Interim Advance Payment Issuing Periods {@link PeriodOfYearResponse}
     *         return null if any error happened or there is no created Interim Advance Payment Issuing Periods
     */
    public List<PeriodOfYearResponse> editPeriodsOfYear(EditInterimAdvancePaymentRequest request, InterimAdvancePayment interimAdvancePayment, StringBuilder errorMessage) {
        if (request.getDayOfWeekAndPeriodOfYearAndDateOfMonth() != null
            && request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear() != null
            && request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear().getPeriodsOfYear() != null) {

            List<EditPeriodOfYearRequest> periodsOfYear = request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear().getPeriodsOfYear();
            Map<Long, InterimAdvancePaymentIssuingPeriod> existedIssuingPeriods = interimAdvancePaymentIssuingPeriodRepository.findByInterimAdvancePaymentIdAndStatusIn(interimAdvancePayment.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE))
                    .stream()
                    .collect(Collectors.toMap(
                            InterimAdvancePaymentIssuingPeriod::getId,
                            interimAdvancePaymentIssuingPeriod -> interimAdvancePaymentIssuingPeriod
                    ));

            List<InterimAdvancePaymentIssuingPeriod> interimAdvancePaymentIssuingPeriods = new ArrayList<>();

            for (EditPeriodOfYearRequest periodOfYear : periodsOfYear) {
                InterimAdvancePaymentIssuingPeriod interimAdvancePaymentIssuingPeriod =
                        findEditedIssuingPeriod(periodOfYear, existedIssuingPeriods, interimAdvancePayment, errorMessage);
                interimAdvancePaymentIssuingPeriods.add(editIssuingPeriod(interimAdvancePaymentIssuingPeriod, interimAdvancePayment, periodOfYear));
            }

            if (!errorMessage.isEmpty()) return null;
            deleteOldPeriods(existedIssuingPeriods);
            return interimAdvancePaymentIssuingPeriodRepository.saveAll(interimAdvancePaymentIssuingPeriods)
                    .stream()
                    .map(PeriodOfYearResponse::new)
                    .toList();
        }else {
            deleteByInterimAdvancePaymentId(interimAdvancePayment.getId());
            return null;
        }
    }

    /**
     * If current request contains id of Interim Advance Payment Issuing Period, search it in Map of already existed Interim Advance Issuing Periods
     * If found return it and remove from Map, If Not found add message in error messages and return new Interim Advance Payment Issuing Period
     *
     * @param periodOfYear  - request used to edit Interim Advance Payment Issuing Period {@link EditPeriodOfYearRequest}
     * @param existedIssuingPeriods - Map of already existed Interim Advance Payment Issuing Periods {@link InterimAdvancePaymentIssuingPeriod}
     * @param interimAdvancePayment - Interim Advance Payment which is connected to Interim Advance Payment Issuing Period {@link InterimAdvancePayment}
     * @param errorMessage - used to store error messages if any
     * @return Interim Advance Payment Issuing Period {@link InterimAdvancePaymentIssuingPeriod}
     */
    private InterimAdvancePaymentIssuingPeriod findEditedIssuingPeriod(EditPeriodOfYearRequest periodOfYear,
                                                                       Map<Long, InterimAdvancePaymentIssuingPeriod> existedIssuingPeriods,
                                                                       InterimAdvancePayment interimAdvancePayment,
                                                                       StringBuilder errorMessage){
        InterimAdvancePaymentIssuingPeriod interimAdvancePaymentIssuingPeriod;
        if (periodOfYear.getId() != null) {
            if (existedIssuingPeriods.containsKey(periodOfYear.getId())) {
                interimAdvancePaymentIssuingPeriod = existedIssuingPeriods.get(periodOfYear.getId());
                existedIssuingPeriods.remove(periodOfYear.getId());
            } else {
                errorMessage.append("Interim Advance Payment Issuing Period not found by id: ").append(periodOfYear.getId()).append(" and interim advance payment id: ").append(interimAdvancePayment.getId()).append(";");
                interimAdvancePaymentIssuingPeriod = new InterimAdvancePaymentIssuingPeriod();
            }
        } else interimAdvancePaymentIssuingPeriod = new InterimAdvancePaymentIssuingPeriod();
        return interimAdvancePaymentIssuingPeriod;
    }

    /**
     * Edit provided Interim Advance Payment Issuing Period based on received request
     *
     * @param periodOfYear - request used to edit Interim Advance Payment Issuing Period {@link EditPeriodOfYearRequest}
     * @param interimAdvancePaymentIssuingPeriod - Interim Advance Payment Issuing Period which is being edited {@link InterimAdvancePaymentIssuingPeriod}
     * @param interimAdvancePayment - Interim Advance Payment which is connected to Interim Advance Payment Issuing Period {@link InterimAdvancePayment}
     * @return Edited Interim Advance Payment Issuing Period {@link InterimAdvancePaymentIssuingPeriod}
     */
    private InterimAdvancePaymentIssuingPeriod editIssuingPeriod(InterimAdvancePaymentIssuingPeriod interimAdvancePaymentIssuingPeriod,
                                                                 InterimAdvancePayment interimAdvancePayment,
                                                                 EditPeriodOfYearRequest periodOfYear){
        interimAdvancePaymentIssuingPeriod.setInterimAdvancePayment(interimAdvancePayment);
        interimAdvancePaymentIssuingPeriod.setPeriodFrom(periodOfYear.getStartDate());
        interimAdvancePaymentIssuingPeriod.setPeriodTo(periodOfYear.getEndDate());
        interimAdvancePaymentIssuingPeriod.setStatus(InterimAdvancePaymentSubObjectStatus.ACTIVE);
        return interimAdvancePaymentIssuingPeriod;
    }

    /**
     * Delete received Interim Advance Payment Issuing Periods
     *
     * @param leftPeriods - Map of Interim Advance Payment Issuing Periods which must be deleted
     */
    private void deleteOldPeriods(Map<Long, InterimAdvancePaymentIssuingPeriod> leftPeriods) {
        List<InterimAdvancePaymentIssuingPeriod> left = leftPeriods.values().stream()
                .peek(interimAdvancePaymentIssuingPeriod -> interimAdvancePaymentIssuingPeriod.setStatus(InterimAdvancePaymentSubObjectStatus.DELETED))
                .toList();
        interimAdvancePaymentIssuingPeriodRepository.saveAll(left);
    }


    public void saveAll(List<InterimAdvancePaymentIssuingPeriod> periodOfYearToSave) {
        interimAdvancePaymentIssuingPeriodRepository.saveAll(periodOfYearToSave);
    }
}
