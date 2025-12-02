package bg.energo.phoenix.service.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.ProcessPeriodicityTimeIntervals;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO.ProcessPeriodicityTimeIntervalDto;
import bg.energo.phoenix.repository.billing.processPeriodicity.ProcessPeriodicityTimeIntervalsRepository;
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
public class ProcessPeriodicityTimeIntervalsService {
    private final ProcessPeriodicityMapper processPeriodicityMapper;
    private final ProcessPeriodicityTimeIntervalsRepository processPeriodicityTimeIntervalsRepository;


    public void validateTimeIntervals(Long processPeriodicityId, List<ProcessPeriodicityTimeIntervalDto> timeIntervalsFromReq, List<String> exceptionMessages) {
        List<ProcessPeriodicityTimeIntervals> tempList = new ArrayList<>();
        List<ProcessPeriodicityTimeIntervals> timeIntervalsFromDb = processPeriodicityTimeIntervalsRepository.findAllByProcessPeriodicityIdAndStatus(processPeriodicityId, EntityStatus.ACTIVE);
        if (CollectionUtils.isEmpty(timeIntervalsFromReq) && CollectionUtils.isNotEmpty(timeIntervalsFromDb)) {
            deactivateAll(timeIntervalsFromDb, tempList);
            return;
        }
        if (CollectionUtils.isNotEmpty(timeIntervalsFromReq)) {
            findRedundant(timeIntervalsFromDb, timeIntervalsFromReq, tempList);
            findNew(timeIntervalsFromReq, tempList, processPeriodicityId);
            findEdited(timeIntervalsFromReq, tempList, exceptionMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        }
        if (CollectionUtils.isNotEmpty(tempList)) {
            processPeriodicityTimeIntervalsRepository.saveAll(tempList);
        }
    }

    private void deactivateAll(List<ProcessPeriodicityTimeIntervals> timeIntervalsFromDb,
                               List<ProcessPeriodicityTimeIntervals> tempList) {
        timeIntervalsFromDb.forEach(dbEntity -> dbEntity.setStatus(EntityStatus.DELETED));
        tempList.addAll(timeIntervalsFromDb);
        processPeriodicityTimeIntervalsRepository.saveAll(tempList);
    }

    private void findRedundant(List<ProcessPeriodicityTimeIntervals> timeIntervalsFromDb,
                               List<ProcessPeriodicityTimeIntervalDto> timeIntervalsFromReq,
                               List<ProcessPeriodicityTimeIntervals> tempList) {
        List<Long> idsFromReq = timeIntervalsFromReq.stream().map(ProcessPeriodicityTimeIntervalDto::getId).toList();
        for (ProcessPeriodicityTimeIntervals fromDb : timeIntervalsFromDb) {
            if (!idsFromReq.contains(fromDb.getId())) {
                fromDb.setStatus(EntityStatus.DELETED);
                tempList.add(fromDb);
            }
        }
    }

    private void findNew(List<ProcessPeriodicityTimeIntervalDto> timeIntervalsFromReq,
                         List<ProcessPeriodicityTimeIntervals> tempList,
                         Long processPeriodicityId) {

        List<ProcessPeriodicityTimeIntervalDto> newIntervalsList = timeIntervalsFromReq.stream()
                .filter(obj -> obj.getId() == null)
                .toList();
        for (ProcessPeriodicityTimeIntervalDto dto : newIntervalsList) {
            tempList.add(processPeriodicityMapper.mapToTimeIntervals(dto, processPeriodicityId));
        }
    }

    private void findEdited(List<ProcessPeriodicityTimeIntervalDto> timeIntervalsFromReq,
                            List<ProcessPeriodicityTimeIntervals> tempList,
                            List<String> exceptionMessages) {
        List<Long> idsFromReq = timeIntervalsFromReq.stream().map(ProcessPeriodicityTimeIntervalDto::getId).toList();
        for (int i = 0; i < idsFromReq.size(); i++) {
            Long idFromRequest = idsFromReq.get(i);
            if (idFromRequest != null) {
                Optional<ProcessPeriodicityTimeIntervals> fromDB = processPeriodicityTimeIntervalsRepository.findById(idFromRequest);
                if (fromDB.isPresent()) {
                    ProcessPeriodicityTimeIntervalDto dto = timeIntervalsFromReq
                            .stream()
                            .filter(ob -> ob.getId().equals(idFromRequest))
                            .findFirst().get();
                    tempList.add(processPeriodicityMapper.fillUpdatedTimeIntervals(fromDB.get(), dto));
                } else {
                    exceptionMessages.add("startTimeIntervals[%s].id-interval with id [%s] does not exist;".formatted(i, idFromRequest));
                }
            }
        }
    }

}
