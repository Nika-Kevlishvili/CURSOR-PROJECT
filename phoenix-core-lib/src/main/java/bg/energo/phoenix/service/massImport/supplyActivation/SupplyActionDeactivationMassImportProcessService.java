package bg.energo.phoenix.service.massImport.supplyActivation;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.repository.ProcessContractPodRepository;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.service.massImport.AbstractMassImportProcessService;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class SupplyActionDeactivationMassImportProcessService extends AbstractMassImportProcessService {
    @Value("${app.cfg.supplyActionDeactivation.massImport.numberOfRowsPerTask}")
    private Integer numberOfRowsPerTsk;
    @Value("${app.cfg.supplyActionDeactivation.massImport.numberOfTasksPerThread}")
    private Integer numberOfCallablesPerThread;
    @Value("${app.cfg.supplyActionDeactivation.massImport.numberOfThreads}")
    private Integer numberOfThreads;
    private final ActionSupplyActivationService supplyActivationService;
    private final ProcessContractPodRepository processContractPodRepository;
    private final TransactionTemplate transactionTemplate;

    public SupplyActionDeactivationMassImportProcessService(ProcessRepository processRepository,
                                                            ProcessedRecordInfoRepository processRecordInfoRepository,
                                                            ProcessContractPodRepository processContractPodRepository,
                                                            ActionSupplyActivationService actionSupplyActivationService,
                                                            TransactionTemplate transactionTemplate,
                                                            NotificationEventPublisher notificationEventPublisher) {
        super(processRepository, processRecordInfoRepository, notificationEventPublisher);
        this.processContractPodRepository = processContractPodRepository;
        this.supplyActivationService = actionSupplyActivationService;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public boolean supports(EventType eventType) {
        return eventType.equals(EventType.SUPPLY_ACTION_DEACTIVATION_MASS_IMPORT_PROCESS);
    }

    @Override
    protected String processRow(Row row, Set<String> permissions, String processSysUserId, LocalDate date, Long recordInfoId) {
        if (!permissions.contains(PermissionEnum.ACTIVATION_DEACTIVATION_MI.getId())) {
            throw new ClientException("Not enough permission for deactivating pod", ErrorCode.ACCESS_DENIED);
        }
        if (row.getCell(0) != null || row.getCell(0).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(0)).setCellType(CellType.STRING);
            Boolean hasEditLockPermission = permissions
                    .contains(
                            PermissionEnum.PRODUCT_CONTRACT_EDIT_LOCKED.getId()
                    );
            supplyActivationService.deactivateWithActionNew(
                    row.getCell(0).getStringCellValue(),
                    date,
                    recordInfoId,
                    hasEditLockPermission);
            return "Success";
        } else {
            throw new ClientException("Row %s is empty!;".formatted(0), ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    @Override
    protected String getIdentifier(Row row) {
        int columnNumber = 0;
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.STRING);
            return row.getCell(columnNumber).getStringCellValue();
        }
        return null;
    }

    @Override
    protected int getNumberOfThreads() {
        return numberOfThreads;
    }

    @Override
    protected int getNumberOfCallablesPerThread() {
        return numberOfCallablesPerThread;
    }

    @Override
    protected int getNumberOfRowsPerTask() {
        return numberOfRowsPerTsk;
    }

    @Override
    protected void onFinish(Process process) {
        log.debug("Executed supply action deactivation on finish");
        try {
            log.debug("Starting supply action deactivation process");
            Map<String, List<Long>> stringListMap = supplyActivationService.onComplete(process.getId(), process.getDate());
            if (stringListMap.isEmpty()) {
                log.debug("Supply action deactivation data is empty");
                return;
            }

            transactionTemplate.executeWithoutResult((j) -> {
                stringListMap.forEach((key, value) -> {
                    processRecordInfoRepository.findProcessInfoStreamByIds(value)
                            .forEach(x -> {
                                x.setSuccess(false);
                                x.setErrorMessage(key);
                            });
                });
            });
        } catch (ClientException e) {
            log.debug("Exception handled on supply action deactivation", e);
            transactionTemplate.executeWithoutResult((j) -> processRecordInfoRepository.updateAllByProcessId(process.getId(), e.getMessage()));
        } catch (Exception e) {
            log.debug("Exception handled on supply action deactivation", e);
            transactionTemplate.executeWithoutResult((j) -> processRecordInfoRepository.updateAllByProcessId(process.getId(), "Unexpected error contact developer;"));
        } finally {
            log.debug("Finalizing supply action deactivation");
            transactionTemplate.executeWithoutResult((j) -> processContractPodRepository.deleteAllByProcessId(process.getId()));
        }
    }
}
