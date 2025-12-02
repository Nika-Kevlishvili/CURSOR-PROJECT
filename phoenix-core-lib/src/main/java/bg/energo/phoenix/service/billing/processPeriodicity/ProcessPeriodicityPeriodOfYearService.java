package bg.energo.phoenix.service.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.ProcessPeriodicityPeriodOfYear;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO.DayOfWeekDto;
import bg.energo.phoenix.repository.billing.processPeriodicity.ProcessPeriodicityPeriodOfYearRepository;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPeriodicityPeriodOfYearService {
    private final ProcessPeriodicityMapper processPeriodicityMapper;
    private final ProcessPeriodicityPeriodOfYearRepository processPeriodicityPeriodOfYearRepository;

    public void deleteProcessPeriodicityPeriodOfYear(Long processPeriodicityId) {
        List<ProcessPeriodicityPeriodOfYear> periodicityPeriodOfYears = processPeriodicityPeriodOfYearRepository.findAllByProcessPeriodicityIdAndStatus(processPeriodicityId, EntityStatus.ACTIVE);
        periodicityPeriodOfYears.forEach(dbObj -> dbObj.setStatus(EntityStatus.DELETED));
    }

    public void validatePeriodOfYear(Long processPeriodicityId, List<DayOfWeekDto> daysOfWeekFromReq, List<String> exceptionMessages) {
        List<ProcessPeriodicityPeriodOfYear> tempList = new ArrayList<>();
        List<ProcessPeriodicityPeriodOfYear> fromDb = processPeriodicityPeriodOfYearRepository.findAllByProcessPeriodicityIdAndStatus(processPeriodicityId, EntityStatus.ACTIVE);
        findRedundant(fromDb, daysOfWeekFromReq, tempList);
        findNew(daysOfWeekFromReq, processPeriodicityId, tempList);
        findEdited(daysOfWeekFromReq, tempList, exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        if (CollectionUtils.isNotEmpty(tempList)) {
            processPeriodicityPeriodOfYearRepository.saveAll(tempList);
        }
    }

    private void findRedundant(List<ProcessPeriodicityPeriodOfYear> fromDb, List<DayOfWeekDto> daysOfWeekFromReq, List<ProcessPeriodicityPeriodOfYear> tempList) {
        List<Long> idsFromReq = daysOfWeekFromReq.stream().map(DayOfWeekDto::getId).toList();
        for (ProcessPeriodicityPeriodOfYear dbObj : fromDb) {
            if (!idsFromReq.contains(dbObj.getId())) {
                dbObj.setStatus(EntityStatus.DELETED);
                tempList.add(dbObj);
            }
        }
    }

    private void findNew(List<DayOfWeekDto> daysOfWeekFromReq, Long processPeriodicityId, List<ProcessPeriodicityPeriodOfYear> tempList) {
        List<DayOfWeekDto> newDaysOfWeek = daysOfWeekFromReq
                .stream()
                .filter(obj -> obj.getId() == null)
                .toList();
        for (DayOfWeekDto dto : newDaysOfWeek) {
            tempList.add(processPeriodicityMapper.mapToPeriodOfYear(dto, processPeriodicityId));
        }
    }

    private void findEdited(List<DayOfWeekDto> daysOfWeekFromReq, List<ProcessPeriodicityPeriodOfYear> tempList, List<String> exceptionMessages) {
        List<Long> idsFromReq = daysOfWeekFromReq.stream().map(DayOfWeekDto::getId).toList();
        for (int i = 0; i < idsFromReq.size(); i++) {
            Long idFromRequest = idsFromReq.get(i);
            if (idFromRequest != null) {
                Optional<ProcessPeriodicityPeriodOfYear> fromDB = processPeriodicityPeriodOfYearRepository.findById(idFromRequest);
                if (fromDB.isPresent()) {
                    DayOfWeekDto dto = daysOfWeekFromReq
                            .stream()
                            .filter(ob -> ob.getId().equals(idFromRequest))
                            .findFirst().get();
                    tempList.add(processPeriodicityMapper.fillUpdatedPeriodOfYear(fromDB.get(), dto));
                } else {
                    exceptionMessages.add("processPeriodicityPeriodOptionsDto.dayOfWeekAndPeriodOfYearDto.daysOfWeek[%s].id-daysOfWeek with id [%s] does not exist;".formatted(i, idFromRequest));
                }
            }
        }
    }

}
