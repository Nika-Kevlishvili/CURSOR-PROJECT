package bg.energo.phoenix.service.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.ProcessPeriodicityDayOfMonths;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO.DateOfMonthDto;
import bg.energo.phoenix.repository.billing.processPeriodicity.ProcessPeriodicityDayOfMonthsRepository;
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
public class ProcessPeriodicityDayOfMonthsService {
    private final ProcessPeriodicityMapper processPeriodicityMapper;
    private final ProcessPeriodicityDayOfMonthsRepository processPeriodicityDayOfMonthsRepository;


    public void deleteDayOfMonths(Long processPeriodicityId) {
        List<ProcessPeriodicityDayOfMonths> dayOfMonthList = processPeriodicityDayOfMonthsRepository.findAllByProcessPeriodicityIdAndStatus(processPeriodicityId, EntityStatus.ACTIVE);
        dayOfMonthList.forEach(dbObj -> dbObj.setStatus(EntityStatus.DELETED));
    }

    public void validateDayOfMonth(Long processPeriodicityId, List<DateOfMonthDto> dateOfMonthsFromReq, List<String> exceptionMessages) {
        List<ProcessPeriodicityDayOfMonths> tempList = new ArrayList<>();
        List<ProcessPeriodicityDayOfMonths> fromDb = processPeriodicityDayOfMonthsRepository.findAllByProcessPeriodicityIdAndStatus(processPeriodicityId, EntityStatus.ACTIVE);
        findRedundant(fromDb, dateOfMonthsFromReq, tempList);
        findNew(dateOfMonthsFromReq, processPeriodicityId, tempList);
        findEdited(dateOfMonthsFromReq, tempList, exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        if (CollectionUtils.isNotEmpty(tempList)) {
            processPeriodicityDayOfMonthsRepository.saveAll(tempList);
        }
    }

    private void findRedundant(List<ProcessPeriodicityDayOfMonths> fromDb, List<DateOfMonthDto> dateOfMonthsFromReq, List<ProcessPeriodicityDayOfMonths> tempList) {
        List<Long> idsFromReq = dateOfMonthsFromReq.stream().map(DateOfMonthDto::getId).toList();
        for (ProcessPeriodicityDayOfMonths dbObj : fromDb) {
            if (!idsFromReq.contains(dbObj.getId())) {
                dbObj.setStatus(EntityStatus.DELETED);
                tempList.add(dbObj);
            }
        }
    }

    private void findNew(List<DateOfMonthDto> dateOfMonthsFromReq, Long ProcessPeriodicityId, List<ProcessPeriodicityDayOfMonths> tempList) {
        List<DateOfMonthDto> newDateOfMonth = dateOfMonthsFromReq
                .stream()
                .filter(obj -> obj.getId() == null)
                .toList();
        for (DateOfMonthDto dto : newDateOfMonth) {
            tempList.add(processPeriodicityMapper.mapToDayOfMonths(dto, ProcessPeriodicityId));
        }
    }

    private void findEdited(List<DateOfMonthDto> dateOfMonthsFromReq, List<ProcessPeriodicityDayOfMonths> tempList, List<String> exceptionMessages) {
        List<Long> idsFromReq = dateOfMonthsFromReq.stream().map(DateOfMonthDto::getId).toList();
        for (int i = 0; i < idsFromReq.size(); i++) {
            Long idFromRequest = idsFromReq.get(i);
            if (idFromRequest != null) {
                Optional<ProcessPeriodicityDayOfMonths> fromDB = processPeriodicityDayOfMonthsRepository.findById(idFromRequest);
                if (fromDB.isPresent()) {
                    DateOfMonthDto dto = dateOfMonthsFromReq
                            .stream()
                            .filter(ob -> ob.getId().equals(idFromRequest))
                            .findFirst().get();
                    tempList.add(processPeriodicityMapper.fillUpdatedToDayOfMonths(fromDB.get(), dto));
                } else {
                    exceptionMessages.add("processPeriodicityPeriodOptionsDto.dateOfMonths[%s].id-dateOfMonth with id [%s] does not exist;".formatted(i, idFromRequest));
                }
            }
        }
    }
}
