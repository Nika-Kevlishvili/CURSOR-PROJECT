package bg.energo.phoenix.service.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.request.pod.pod.PodCreateRequest;
import bg.energo.phoenix.model.request.pod.pod.PodUpdateRequest;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import bg.energo.phoenix.service.pod.pod.PointOfDeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class PodMassImportProcessService extends AbstractMassImportProcessService {

    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final PointOfDeliveryService pointOfDeliveryService;
    private final PodExcelMapper podExcelMapper;
    @Value("${app.cfg.pod.massImport.numberOfRowsPerTask}")
    private Integer numberOfRowsPerTsk;
    @Value("${app.cfg.pod.massImport.numberOfTasksPerThread}")
    private Integer numberOfCallablesPerThread;
    @Value("${app.cfg.pod.massImport.numberOfThreads}")
    private Integer numberOfThreads;

    public PodMassImportProcessService(PointOfDeliveryRepository pointOfDeliveryRepository,
                                       PointOfDeliveryService pointOfDeliveryService,
                                       PodExcelMapper podExcelMapper,
                                       ProcessRepository processRepository,
                                       ProcessedRecordInfoRepository processedRecordInfoRepository,
                                       NotificationEventPublisher notificationEventPublisher) {
        super(processRepository, processedRecordInfoRepository, notificationEventPublisher);
        this.pointOfDeliveryRepository = pointOfDeliveryRepository;
        this.pointOfDeliveryService = pointOfDeliveryService;
        this.podExcelMapper = podExcelMapper;
    }

    @Override
    protected String processRow(Row row, Set<String> permissions, String processSysUserId, LocalDate date, Long processRecordInfo) {
        List<String> errorMessages = new ArrayList<>();
        if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
            if (!permissions.contains(PermissionEnum.POD_MI_CREATE.getId()))
                throw new ClientException("Not enough permission for creating POD", ErrorCode.ACCESS_DENIED);
            PodCreateRequest podCreateRequest = podExcelMapper.toPODCreateRequest(new PodCreateRequest(), row, errorMessages);
            //Validate request
            validateRequest(errorMessages, podCreateRequest);

            return String.valueOf(pointOfDeliveryService.create(podCreateRequest, permissions.stream().toList()).getPodDetailId());
        } else {
            ((XSSFCell) row.getCell(3)).setCellType(CellType.STRING);
            String identifier = row.getCell(3).getStringCellValue();
            Optional<PointOfDelivery> pod = pointOfDeliveryRepository.findByIdentifierAndStatus(identifier, PodStatus.ACTIVE);
            if (pod.isPresent()) {
                if (!permissions.contains(PermissionEnum.POD_MI_UPDATE.getId()))
                    throw new ClientException("Not enough permission for updating user", ErrorCode.ACCESS_DENIED);

                PodUpdateRequest request = podExcelMapper.toPODUpdateRequest(pod.get(), row, errorMessages);

                //Validate request
                validateRequest(errorMessages, request);

                return String.valueOf(pointOfDeliveryService.edit(pod.get().getId(), request, permissions.stream().toList()).getPodDetailId());
            } else {
                throw new ClientException("Not found pod with id : " + identifier + ";", ErrorCode.CONFLICT);
            }
        }
    }

    @Override
    protected String getIdentifier(Row row) {
        int columnNumber = 3;
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

    @Override
    public boolean supports(EventType eventType) {
        return EventType.POD_MASS_IMPORT_PROCESS.equals(eventType);
    }
}
