package bg.energo.phoenix.service.product.penalty.penalty;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.product.penalty.penalty.Penalty;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyActionTypes;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyPaymentTerm;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyPaymentTermExclude;
import bg.energo.phoenix.model.request.product.penalty.penalty.PenaltyRequest;
import bg.energo.phoenix.model.request.product.term.terms.paymentTerm.PenaltyPaymentTermRequest;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarResponse;
import bg.energo.phoenix.model.response.penalty.PenaltyResponse;
import bg.energo.phoenix.model.response.penalty.copy.PenaltyPaymentTermsCopyResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PenaltyDataMapper {

    public PenaltyPaymentTerm fromRequest(Long penaltyId, PenaltyPaymentTermRequest request) {
        Set<PenaltyPaymentTermExclude> penaltyPaymentTermExcludeSet = getPenaltyPaymentTermExcludes(request);

        return PenaltyPaymentTerm.builder()
                .name(request.getName())
                .calendarId(request.getCalendarId())
                .dueDateChange(request.getDueDateChange())
                .penaltyPaymentTermExcludes(penaltyPaymentTermExcludeSet.stream().toList())
                .penaltyId(penaltyId)
                .calendarType(request.getCalendarType())
                .valueFrom(request.getValueFrom())
                .value(request.getValue())
                .valueTo(request.getValueTo())
                .status(EntityStatus.ACTIVE)
                .build();
    }

    public PenaltyPaymentTermsCopyResponse toResponse(PenaltyPaymentTerm entity, Calendar calendar) {
        var excludeWeekends = false;
        var excludeHolidays = false;
        List<PenaltyPaymentTermExclude> penaltyPaymentTermExcludes = entity.getPenaltyPaymentTermExcludes();
        if (CollectionUtils.isNotEmpty(penaltyPaymentTermExcludes)) {
            excludeWeekends = penaltyPaymentTermExcludes.contains(PenaltyPaymentTermExclude.WEEKENDS);
            excludeHolidays = penaltyPaymentTermExcludes.contains(PenaltyPaymentTermExclude.HOLIDAYS);
        }

        PenaltyPaymentTermsCopyResponse build = PenaltyPaymentTermsCopyResponse.builder()
                .name(entity.getName())
                .dueDateChange(entity.getDueDateChange())
                .excludeWeekends(excludeWeekends)
                .excludeHolidays(excludeHolidays)
                .calendarType(entity.getCalendarType())
                .valueFrom(entity.getValueFrom())
                .value(entity.getValue())
                .valueTo(entity.getValueTo())
                .status(EntityStatus.ACTIVE)
                .build();
        if (calendar != null) {
            build.setCalendar(new CalendarResponse(calendar));
        }
        return build;
    }

    public Penalty fromRequest(PenaltyRequest request) {
        return Penalty.builder()
                .name(request.getName().trim())
                .additionalInfo(request.getAdditionalInformation())
                .applicability(request.getPenaltyApplicability())
                .automaticSubmission(request.getAutomaticSubmission())
                .contractClauseNumber(request.getContractClauseNumber())
                .maxAmount(request.getMaxAmount())
                .minAmount(request.getMinAmount())
                .partyReceivingPenalties(request.getPenaltyReceivingParties().stream().toList())
                .processId(request.getProcessId())
                .processStartCode(request.getProcessStartCode())
                .amountCalculationFormula(request.getAmountFormula())
                .currencyId(request.getCurrencyId())
                .status(EntityStatus.ACTIVE)
                .noInterestOnOverdueDebts(BooleanUtils.isTrue(request.getNoInterestOnOverdueDebts()))
                .build();
    }

    public void updatePenalty(Penalty penalty, PenaltyRequest request) {
        penalty.setName(request.getName().trim());
        penalty.setAdditionalInfo(request.getAdditionalInformation());
        penalty.setApplicability(request.getPenaltyApplicability());
        penalty.setAutomaticSubmission(request.getAutomaticSubmission());
        penalty.setContractClauseNumber(request.getContractClauseNumber());
        penalty.setMaxAmount(request.getMaxAmount());
        penalty.setMinAmount(request.getMinAmount());
        penalty.setPartyReceivingPenalties(request.getPenaltyReceivingParties().stream().toList());
        penalty.setProcessId(request.getProcessId());
        penalty.setProcessStartCode(request.getProcessStartCode());
        penalty.setAmountCalculationFormula(request.getAmountFormula());
        penalty.setCurrencyId(request.getCurrencyId());
        penalty.setStatus(EntityStatus.ACTIVE);
        penalty.setNoInterestOnOverdueDebts(BooleanUtils.isTrue(request.getNoInterestOnOverdueDebts()));
    }

    public PenaltyResponse toResponse(Penalty entity, String currencyName) {
        PenaltyResponse build = PenaltyResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .additionalInfo(entity.getAdditionalInfo())
                .applicability(entity.getApplicability())
                .automaticSubmission(entity.isAutomaticSubmission())
                .contractClauseNumber(entity.getContractClauseNumber())
                .maxAmount(entity.getMaxAmount())
                .minAmount(entity.getMinAmount())
                .currencyName(currencyName)
                .penaltyReceivingParties(new HashSet<>(entity.getPartyReceivingPenalties()))
                .processId(entity.getProcessId())
                .processStartCode(entity.getProcessStartCode())
                .amountCalculationFormula(entity.getAmountCalculationFormula())
                .currencyId(entity.getCurrencyId())
                .status(entity.getStatus())
                .build();
        if (entity.getNoInterestOnOverdueDebts() != null) {
            build.setNoInterestOnOverdueDebts(entity.getNoInterestOnOverdueDebts());
        }
        return build;

    }

    public void updatePaymentTerm(PenaltyPaymentTerm paymentTerm, PenaltyPaymentTermRequest request) {
        Set<PenaltyPaymentTermExclude> penaltyPaymentTermExcludeSet = getPenaltyPaymentTermExcludes(request);

        paymentTerm.setName(request.getName());
        paymentTerm.setCalendarId(request.getCalendarId());
        paymentTerm.setDueDateChange(request.getDueDateChange());
        paymentTerm.setPenaltyPaymentTermExcludes(penaltyPaymentTermExcludeSet.stream().toList());
        paymentTerm.setCalendarType(request.getCalendarType());
        paymentTerm.setValueFrom(request.getValueFrom());
        paymentTerm.setValue(request.getValue());
        paymentTerm.setValueTo(request.getValueTo());
        paymentTerm.setStatus(EntityStatus.ACTIVE);

    }

    private Set<PenaltyPaymentTermExclude> getPenaltyPaymentTermExcludes(PenaltyPaymentTermRequest request) {
        Set<PenaltyPaymentTermExclude> penaltyPaymentTermExcludeSet = new HashSet<>();
        if (BooleanUtils.isTrue(request.getExcludeHolidays())) {
            penaltyPaymentTermExcludeSet.add(PenaltyPaymentTermExclude.HOLIDAYS);
        }
        if (BooleanUtils.isTrue(request.getExcludeWeekends())) {
            penaltyPaymentTermExcludeSet.add(PenaltyPaymentTermExclude.WEEKENDS);
        }
        return penaltyPaymentTermExcludeSet;
    }

    public Penalty fromPenalty(Penalty request) {
        return Penalty.builder()
                .name(request.getName())
                .additionalInfo(request.getAdditionalInfo())
                .applicability(request.getApplicability())
                .automaticSubmission(request.isAutomaticSubmission())
                .contractClauseNumber(request.getContractClauseNumber())
                .maxAmount(request.getMaxAmount())
                .minAmount(request.getMinAmount())
                .partyReceivingPenalties(request.getPartyReceivingPenalties())
                .processId(request.getProcessId())
                .processStartCode(request.getProcessStartCode())
                .amountCalculationFormula(request.getAmountCalculationFormula())
                .currencyId(request.getCurrencyId())
                .status(EntityStatus.ACTIVE)
                .build();

    }

    public PenaltyPaymentTerm fromPaymentTerm(Long penaltyId, PenaltyPaymentTerm request) {
        return PenaltyPaymentTerm.builder()
                .name(request.getName())
                .calendarId(request.getCalendarId())
                .dueDateChange(request.getDueDateChange())
                .penaltyPaymentTermExcludes(request.getPenaltyPaymentTermExcludes())
                .penaltyId(penaltyId)
                .calendarType(request.getCalendarType())
                .valueFrom(request.getValueFrom())
                .value(request.getValue())
                .valueTo(request.getValueTo())
                .status(EntityStatus.ACTIVE)
                .build();

    }

    public List<PenaltyActionTypes> toPenaltyActionTypeList(List<Long> actionTypeList, Long penaltyId) {
        return actionTypeList.stream()
                .map(actionTypeId -> PenaltyActionTypes.builder()
                        .penaltyId(penaltyId)
                        .actionTypeId(actionTypeId)
                        .status(EntityStatus.ACTIVE)
                        .build())
                .collect(Collectors.toList());
    }
}
