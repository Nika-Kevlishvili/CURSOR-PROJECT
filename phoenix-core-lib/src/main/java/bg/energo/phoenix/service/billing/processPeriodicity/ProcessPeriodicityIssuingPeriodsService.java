package bg.energo.phoenix.service.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.ProcessPeriodicityIssuingPeriods;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO.PeriodOfYearDto;
import bg.energo.phoenix.repository.billing.processPeriodicity.ProcessPeriodicityIssuingPeriodsRepository;
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
public class ProcessPeriodicityIssuingPeriodsService {
    private final ProcessPeriodicityMapper processPeriodicityMapper;
    private final ProcessPeriodicityIssuingPeriodsRepository processPeriodicityIssuingPeriodsRepository;


    public void deleteProcessPeriodicityIssuingPeriods(Long processPeriodicityId) {
        List<ProcessPeriodicityIssuingPeriods> issuingPeriods = processPeriodicityIssuingPeriodsRepository.findAllByProcessPeriodicityIdAndStatus(processPeriodicityId, EntityStatus.ACTIVE);
        issuingPeriods.forEach(dbObj -> dbObj.setStatus(EntityStatus.DELETED));
    }

    public void validateIssuingPeriods(Long processPeriodicityId, List<PeriodOfYearDto> issuingPeriodsFromReq, List<String> exceptionMessages) {
        List<ProcessPeriodicityIssuingPeriods> tempList = new ArrayList<>();
        List<ProcessPeriodicityIssuingPeriods> fromDb = processPeriodicityIssuingPeriodsRepository.findAllByProcessPeriodicityIdAndStatus(processPeriodicityId, EntityStatus.ACTIVE);
        if (CollectionUtils.isEmpty(issuingPeriodsFromReq) && CollectionUtils.isNotEmpty(fromDb)) {
            deleteProcessPeriodicityIssuingPeriods(processPeriodicityId);
            return;
        }
        if (CollectionUtils.isNotEmpty(issuingPeriodsFromReq)) {
            findRedundant(fromDb, issuingPeriodsFromReq, tempList);
            findNew(issuingPeriodsFromReq, processPeriodicityId, tempList);
            findEdited(issuingPeriodsFromReq, tempList, exceptionMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        }
        if (CollectionUtils.isNotEmpty(tempList)) {
            processPeriodicityIssuingPeriodsRepository.saveAll(tempList);
        }
    }

    private void findRedundant(List<ProcessPeriodicityIssuingPeriods> fromDb, List<PeriodOfYearDto> issuingPeriodsFromReq, List<ProcessPeriodicityIssuingPeriods> tempList) {
        List<Long> idsFromReq = issuingPeriodsFromReq.stream().map(PeriodOfYearDto::getId).toList();
        for (ProcessPeriodicityIssuingPeriods dbObj : fromDb) {
            if (!idsFromReq.contains(dbObj.getId())) {
                dbObj.setStatus(EntityStatus.DELETED);
                tempList.add(dbObj);
            }
        }
    }

    private void findNew(List<PeriodOfYearDto> issuingPeriodsFromReq, Long processPeriodicityId, List<ProcessPeriodicityIssuingPeriods> tempList) {
        List<PeriodOfYearDto> newIssuingPeriods = issuingPeriodsFromReq
                .stream()
                .filter(obj -> obj.getId() == null)
                .toList();
        for (PeriodOfYearDto dto : newIssuingPeriods) {
            tempList.add(processPeriodicityMapper.mapIssuingPeriods(dto, processPeriodicityId));
        }
    }

    private void findEdited(List<PeriodOfYearDto> issuingPeriodsFromReq, List<ProcessPeriodicityIssuingPeriods> tempList, List<String> exceptionMessages) {
        List<Long> idsFromReq = issuingPeriodsFromReq.stream().map(PeriodOfYearDto::getId).toList();
        for (int i = 0; i < idsFromReq.size(); i++) {
            Long idFromRequest = idsFromReq.get(i);
            if (idFromRequest != null) {
                Optional<ProcessPeriodicityIssuingPeriods> fromDB = processPeriodicityIssuingPeriodsRepository.findById(idFromRequest);
                if (fromDB.isPresent()) {
                    PeriodOfYearDto dto = issuingPeriodsFromReq
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
