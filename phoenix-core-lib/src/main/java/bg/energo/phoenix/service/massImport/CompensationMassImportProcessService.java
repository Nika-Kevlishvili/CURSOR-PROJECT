package bg.energo.phoenix.service.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.request.billing.compensations.CompensationRequest;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.service.billing.compensations.CompensationService;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class CompensationMassImportProcessService extends AbstractMassImportProcessService {

    @Value("${app.cfg.compensation.massImport.numberOfRowsPerTask}")
    private Integer numberOfRowsPerTsk;
    @Value("${app.cfg.compensation.massImport.numberOfTasksPerThread}")
    private Integer numberOfCallablesPerThread;
    @Value("${app.cfg.compensation.massImport.numberOfThreads}")
    private Integer numberOfThreads;
    private final CompensationService compensationService;
    private final CompensationExcelMapper compensationExcelMapper;

    public CompensationMassImportProcessService(ProcessRepository processRepository,
                                                ProcessedRecordInfoRepository processRecordInfoRepository,
                                                NotificationEventPublisher notificationEventPublisher,
                                                CompensationService compensationService,
                                                CompensationExcelMapper compensationExcelMapper) {
        super(processRepository, processRecordInfoRepository, notificationEventPublisher);
        this.compensationService = compensationService;
        this.compensationExcelMapper = compensationExcelMapper;
    }

    @Override
    protected String processRow(Row row, Set<String> permissions, String processSysUserId, LocalDate date, Long processRecordInfo) {
        List<String> errorMessages = new ArrayList<>();
        if (!permissions.contains(PermissionEnum.GOVERNMENT_COMPENSATION_MI_CREATE.getId())) {
            throw new ClientException("Not enough permission for creating Government Compensation", ErrorCode.ACCESS_DENIED);
        }
        CompensationRequest compensationRequest = compensationExcelMapper.getCompensationRequest(row, errorMessages);
        validateRequest(errorMessages, compensationRequest);
        return String.valueOf(compensationService.create(compensationRequest));
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
    public boolean supports(EventType eventType) {
        return eventType.equals(EventType.GOVERNMENT_COMPENSATION_MASS_IMPORT_PROCESS);
    }
}
