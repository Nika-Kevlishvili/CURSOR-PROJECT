package bg.energo.phoenix.service.massImport;


import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.customer.UnwantedCustomer;
import bg.energo.phoenix.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;
import bg.energo.phoenix.model.request.customer.unwantedCustomer.UnwantedCustomerCreateRequest;
import bg.energo.phoenix.model.request.customer.unwantedCustomer.UnwantedCustomerEditRequest;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.repository.customer.UnwantedCustomerRepository;
import bg.energo.phoenix.service.customer.UnwantedCustomerService;
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
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class UnwantedCustomerMassImportProcessService extends AbstractMassImportProcessService {
    private final UnwantedCustomerRepository unwantedCustomerRepository;
    private final UnwantedCustomerService unwantedCustomerService;
    private final UnwantedCustomerExcelMapper excelMapper;
    @Value("${app.cfg.unwantedCustomer.massImport.numberOfRowsPerTask}")
    private Integer numberOfRowsPerTsk;
    @Value("${app.cfg.unwantedCustomer.massImport.numberOfTasksPerThread}")
    private Integer numberOfCallablesPerThread;
    @Value("${app.cfg.unwantedCustomer.massImport.numberOfThreads}")
    private Integer numberOfThreads;

    public UnwantedCustomerMassImportProcessService(UnwantedCustomerRepository unwantedCustomerRepository,
                                                    UnwantedCustomerService unwantedCustomerService,
                                                    UnwantedCustomerExcelMapper excelMapper,
                                                    ProcessRepository processRepository,
                                                    ProcessedRecordInfoRepository processedRecordInfoRepository,
                                                    NotificationEventPublisher notificationEventPublisher) {
        super(processRepository, processedRecordInfoRepository, notificationEventPublisher);
        this.unwantedCustomerRepository = unwantedCustomerRepository;
        this.unwantedCustomerService = unwantedCustomerService;
        this.excelMapper = excelMapper;
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
    protected String processRow(Row row, Set<String> permissions, String processSysUserId, LocalDate date, Long processRecordInfo) {

        List<String> errorMessages = new ArrayList<>();
        if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
            if (!permissions.contains(PermissionEnum.UNWANTED_CUSTOMER_MI_CREATE.getId()))
                throw new ClientException("Not enough permission for creating user", ErrorCode.ACCESS_DENIED);
            UnwantedCustomerCreateRequest request = excelMapper.toCreateCustomerRequest(row, errorMessages);
            //Validate request
            validateRequest(errorMessages, request);

            return String.valueOf(unwantedCustomerService.createUnwantedCustomer(request).getId());

        } else {

            ((XSSFCell) row.getCell(0)).setCellType(CellType.STRING);
            Long id = Long.valueOf(row.getCell(0).getStringCellValue());
            Optional<UnwantedCustomer> customer = unwantedCustomerRepository.findByIdAndStatuses(id, List.of(UnwantedCustomerStatus.ACTIVE));
            if (customer.isPresent()) {

                if (!permissions.contains(PermissionEnum.UNWANTED_CUSTOMER_MI_EDIT.getId()))
                    throw new ClientException("Not enough permission for updating user", ErrorCode.ACCESS_DENIED);

                UnwantedCustomerEditRequest request = excelMapper.createEditRequest(customer.get(), row, errorMessages);

                //Validate request
                validateRequest(errorMessages, request);

                return String.valueOf(unwantedCustomerService.edit(id, request).getId());
            } else {
                throw new ClientException("Not found unwanted customer with id : " + id + ";", ErrorCode.CONFLICT);
            }
        }
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
        return EventType.UNWANTED_CUSTOMER_MASS_IMPORT_PROCESS.equals(eventType);
    }

}
