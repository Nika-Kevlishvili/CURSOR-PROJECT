package bg.energo.phoenix.service.massImport.supplyActivation;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.request.contract.pod.MassImportDeactivationRequest;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.service.contract.pod.ContractPodService;
import bg.energo.phoenix.service.massImport.AbstractMassImportProcessService;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

@Service
@Slf4j
public class SupplyAutomaticDeactivationMassImportProcessService extends AbstractMassImportProcessService {
    private final ContractPodService contractPodService;
    @Value("${app.cfg.supplyAutomaticDeactivation.massImport.numberOfRowsPerTask}")
    private Integer numberOfRowsPerTsk;
    @Value("${app.cfg.supplyAutomaticDeactivation.massImport.numberOfTasksPerThread}")
    private Integer numberOfCallablesPerThread;
    @Value("${app.cfg.supplyAutomaticDeactivation.massImport.numberOfThreads}")
    private Integer numberOfThreads;

    public SupplyAutomaticDeactivationMassImportProcessService(ContractPodService contractPodService,
                                                               ProcessRepository processRepository,
                                                               ProcessedRecordInfoRepository processedRecordInfoRepository,
                                                               NotificationEventPublisher notificationEventPublisher) {
        super(processRepository, processedRecordInfoRepository, notificationEventPublisher);
        this.contractPodService = contractPodService;
    }

    @Override
    protected String processRow(Row row, Set<String> permissions, String processSysUserId, LocalDate date, Long processRecordInfo) {
        if (!permissions.contains(PermissionEnum.ACTIVATION_DEACTIVATION_MI.getId()))
            throw new ClientException("Not enough permission for creating POD", ErrorCode.ACCESS_DENIED);
        if (row.getCell(0) != null || row.getCell(0).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(0)).setCellType(CellType.STRING);
            Boolean hasEditLockPermission = permissions
                    .contains(
                            PermissionEnum.PRODUCT_CONTRACT_EDIT_LOCKED.getId()
                    );
            this.contractPodService.deactivate(
                    new MassImportDeactivationRequest(
                            row.getCell(0).getStringCellValue(),
                            date),
                    hasEditLockPermission);
            return "Version1";
        } else {
            throw new ClientException("Row %s is empty!;".formatted(0), ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    @Override
    public boolean supports(EventType eventType) {
        return EventType.SUPPLY_AUTOMATIC_DEACTIVATION_MASS_IMPORT_PROCESS.equals(eventType);
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
        return this.numberOfThreads;
    }

    @Override
    protected int getNumberOfCallablesPerThread() {
        return this.numberOfCallablesPerThread;
    }

    @Override
    protected int getNumberOfRowsPerTask() {
        return this.numberOfRowsPerTsk;
    }
}
