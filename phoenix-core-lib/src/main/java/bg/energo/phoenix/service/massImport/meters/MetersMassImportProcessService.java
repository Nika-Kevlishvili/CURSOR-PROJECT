package bg.energo.phoenix.service.massImport.meters;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.CacheObjectForParent;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.meter.MeterStatus;
import bg.energo.phoenix.model.request.pod.meter.MeterRequest;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.repository.nomenclature.product.priceComponent.ScalesRepository;
import bg.energo.phoenix.repository.pod.meter.MeterRepository;
import bg.energo.phoenix.service.massImport.AbstractMassImportProcessService;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import bg.energo.phoenix.service.pod.meter.MeterService;
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
public class MetersMassImportProcessService extends AbstractMassImportProcessService {
    private final MeterRepository meterRepository;
    private final MeterService meterService;
    private final MetersExcelMapper metersExcelMapper;
    private final ScalesRepository scalesRepository;

    @Value("${app.cfg.meter.massImport.numberOfRowsPerTask}")
    private Integer numberOfRowsPerTsk;
    @Value("${app.cfg.meter.massImport.numberOfTasksPerThread}")
    private Integer numberOfCallablesPerThread;
    @Value("${app.cfg.meter.massImport.numberOfThreads}")
    private Integer numberOfThreads;


    public MetersMassImportProcessService(ProcessRepository processRepository,
                                          ProcessedRecordInfoRepository processRecordInfoRepository,
                                          MeterRepository meterRepository,
                                          MeterService meterService,
                                          MetersExcelMapper metersExcelMapper,
                                          ScalesRepository scalesRepository,
                                          NotificationEventPublisher notificationEventPublisher) {
        super(processRepository, processRecordInfoRepository, notificationEventPublisher);
        this.meterRepository = meterRepository;
        this.meterService = meterService;
        this.metersExcelMapper = metersExcelMapper;
        this.scalesRepository = scalesRepository;
    }

    private List<CacheObjectForParent> getScales() {
        return scalesRepository.getCacheObjectByStatus(List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
    }

    @Override
    protected String processRow(Row row, Set<String> permissions, String processSysUserId, LocalDate date, Long processRecordInfo) {

        List<String> errorMessages = new ArrayList<>();

        MeterRequest meterRequest = metersExcelMapper.toMetersCreateRequest(row, getScales(), errorMessages);
        validateRequest(errorMessages, meterRequest);

        if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
            return String.valueOf(meterService.create(meterRequest));
        } else {
            Long meterId = getId(row);
            if (meterId != null) {
                List<Long> inactivesForMeter = scalesRepository.findInactivesForMeter(meterId);
                Optional<CacheObject> cacheObject = meterRepository.getCacheObjectByNameAndStatus(meterId, MeterStatus.ACTIVE);
                meterRequest.getMeterScales().addAll(inactivesForMeter);
                if (cacheObject.isPresent()) {
                    return String.valueOf(meterService.update(meterRequest, cacheObject.get().getId()));
                } else {
                    throw new ClientException(String.format("Meter with id: %s not found;", meterId), ErrorCode.CONFLICT);
                }
            } else {
                throw new ClientException("Meter id is provided in wrong format;", ErrorCode.CONFLICT);
            }

        }
    }

    private Long getId(Row row) {
        if (row.getCell(0) != null && row.getCell(0).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(0)).setCellType(CellType.STRING);
            return Long.parseLong(row.getCell(0).getStringCellValue());
        }
        return null;
    }

    @Override
    protected String getIdentifier(Row row) {
        int columnNumber = 1;
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
        return eventType.equals(EventType.METER_MASS_IMPORT_PROCESS);
    }
}
