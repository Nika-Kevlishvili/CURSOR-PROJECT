package bg.energo.phoenix.service.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.ProcessPeriodicityIncompatibleProcesses;
import bg.energo.phoenix.model.enums.billing.billings.BillingRunPeriodicity;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.repository.billing.billingRun.BillingProcessPeriodicityRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.repository.billing.processPeriodicity.ProcessPeriodicityIncompatibleProcessesRepository;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPeriodicityIncompatibleProcessesService {
    private final BillingRunRepository billingRunRepository;
    private final ProcessPeriodicityIncompatibleProcessesRepository processPeriodicityIncompatibleProcessesRepository;
    private final BillingProcessPeriodicityRepository billingProcessPeriodicityRepository;

    private final ProcessPeriodicityMapper processPeriodicityMapper;

    public void validateIncompatibleBillingRunList(Long processPeriodicityId, List<Long> incompatibleProcessesFromReq, List<String> exceptionMessages) {
        validateAccessibility(incompatibleProcessesFromReq, exceptionMessages, processPeriodicityId);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        List<ProcessPeriodicityIncompatibleProcesses> tempList = new ArrayList<>();
        List<ProcessPeriodicityIncompatibleProcesses> incompatibleProcessesFromDB = processPeriodicityIncompatibleProcessesRepository.findAllByProcessPeriodicityIdAndStatus(processPeriodicityId, EntityStatus.ACTIVE);
        if (CollectionUtils.isEmpty(incompatibleProcessesFromReq) && CollectionUtils.isNotEmpty(incompatibleProcessesFromDB)) {
            deactivateAll(incompatibleProcessesFromDB, tempList);
            return;
        }
        if (CollectionUtils.isNotEmpty(incompatibleProcessesFromReq)) {
            findRedundant(incompatibleProcessesFromReq, incompatibleProcessesFromDB, tempList);
            findNew(incompatibleProcessesFromReq, incompatibleProcessesFromDB, tempList, processPeriodicityId);
        }
        if (CollectionUtils.isNotEmpty(tempList)) {
            processPeriodicityIncompatibleProcessesRepository.saveAll(tempList);
        }
    }

    private void validateAccessibility(List<Long> incompatibleProcessesFromReq, List<String> exceptionMessages, Long processPeriodicityId) {
        for (int i = 0; i < incompatibleProcessesFromReq.size(); i++) {
            Long incompatibleProcessId = incompatibleProcessesFromReq.get(i);
            validateBillingRunDependency(processPeriodicityId, incompatibleProcessId, exceptionMessages, "incompatibleProcesses[%s]".formatted(i));
            if (incompatibleProcessId != null && billingRunRepository.findBillingRunByIdAndRunPeriodicityAndStatusIsNot(incompatibleProcessId, BillingRunPeriodicity.PERIODIC, BillingStatus.DELETED).isEmpty()) {
                exceptionMessages.add("incompatibleProcesses[%s]-incompatibleProcessId with id [%s] cannot be selected;".formatted(i, incompatibleProcessId));
            }
        }
    }

    public void validateStartAfterBillingRun(Long processPeriodicityId, Long startAfterBillingRunId, List<String> exceptionMessages) {
        validateBillingRunDependency(processPeriodicityId, startAfterBillingRunId, exceptionMessages, "startAfterProcessId");
        if (startAfterBillingRunId != null && billingRunRepository.findBillingRunByIdAndRunPeriodicityAndStatusIsNot(startAfterBillingRunId, BillingRunPeriodicity.PERIODIC, BillingStatus.DELETED).isEmpty()) {
            exceptionMessages.add("startAfterProcessId-startAfterProcess with id [%s] cannot be selected;".formatted(startAfterBillingRunId));
        }
    }

    private void validateBillingRunDependency(Long processPeriodicityId, Long billingRunId, List<String> exceptionMessages, String message) {
        if (CollectionUtils.isNotEmpty(billingProcessPeriodicityRepository.findAllByBillingIdAndProcessPeriodicityIdAndStatus(billingRunId, processPeriodicityId, EntityStatus.ACTIVE))) {
            exceptionMessages.add("%s-process with id [%s] has dependency on this object;".formatted(message, billingRunId));
        }
    }

    private void deactivateAll(List<ProcessPeriodicityIncompatibleProcesses> incompatibleProcessesFromDB, List<ProcessPeriodicityIncompatibleProcesses> tempList) {
        incompatibleProcessesFromDB.forEach(dbEntity -> dbEntity.setStatus(EntityStatus.DELETED));
        tempList.addAll(incompatibleProcessesFromDB);
        processPeriodicityIncompatibleProcessesRepository.saveAll(tempList);
    }

    private void findRedundant(List<Long> incompatibleProcessesFromReq, List<ProcessPeriodicityIncompatibleProcesses> incompatibleProcessesFromDB, List<ProcessPeriodicityIncompatibleProcesses> tempList) {
        for (ProcessPeriodicityIncompatibleProcesses fromDb : incompatibleProcessesFromDB) {
            if (!incompatibleProcessesFromReq.contains(fromDb.getIncompatibleBillingId())) {
                fromDb.setStatus(EntityStatus.DELETED);
                tempList.add(fromDb);
            }
        }
    }

    private void findNew(List<Long> incompatibleProcessesFromReq, List<ProcessPeriodicityIncompatibleProcesses> incompatibleProcessesFromDB, List<ProcessPeriodicityIncompatibleProcesses> tempList, Long processPeriodicityId) {
        List<Long> idsFromDb = incompatibleProcessesFromDB.stream().map(ProcessPeriodicityIncompatibleProcesses::getIncompatibleBillingId).toList();
        for (Long idFromReq : incompatibleProcessesFromReq) {
            if (!idsFromDb.contains(idFromReq)) {
                ProcessPeriodicityIncompatibleProcesses incompatibleProcesses = processPeriodicityMapper.mapToProcessPeriodicityIncompatibleProcesses(idFromReq);
                incompatibleProcesses.setProcessPeriodicityId(processPeriodicityId);
                tempList.add(incompatibleProcesses);
            }
        }
    }

}
