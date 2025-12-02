package bg.energo.phoenix.service.massImport.customerReceivable;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.customAnotations.receivable.customerReceivables.CustomerReceivablePostGroup;
import bg.energo.phoenix.model.customAnotations.receivable.customerReceivables.CustomerReceivablePutGroup;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.request.receivable.customerReceivable.CustomerReceivableRequest;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.repository.receivable.customerReceivables.CustomerReceivableRepository;
import bg.energo.phoenix.service.massImport.AbstractMassImportProcessService;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import bg.energo.phoenix.service.receivable.customerReceivables.CustomerReceivableService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
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
public class CustomerReceivableMassImportProcessService extends AbstractMassImportProcessService {

    private final CustomerReceivableExcelMapper customerReceivableExcelMapper;
    private final CustomerReceivableService customerReceivableService;
    private final CustomerReceivableRepository customerReceivableRepository;

    @Value("${app.cfg.customerReceivable.massImport.numberOfRowsPerTask}")
    private Integer numberOfRowsPerTask;

    @Value("${app.cfg.customerReceivable.massImport.numberOfTasksPerThread}")
    private Integer numberOfTasksPerThread;

    @Value("${app.cfg.customerReceivable.massImport.numberOfThreads}")
    private Integer numberOfThreads;

    public CustomerReceivableMassImportProcessService(ProcessRepository processRepository,
                                                      ProcessedRecordInfoRepository processRecordInfoRepository,
                                                      CustomerReceivableExcelMapper customerReceivableExcelMapper,
                                                      CustomerReceivableService customerReceivableService,
                                                      CustomerReceivableRepository customerReceivableRepository,
                                                      NotificationEventPublisher notificationEventPublisher) {
        super(processRepository, processRecordInfoRepository, notificationEventPublisher);
        this.customerReceivableExcelMapper = customerReceivableExcelMapper;
        this.customerReceivableService = customerReceivableService;
        this.customerReceivableRepository = customerReceivableRepository;
    }

    @Override
    protected String processRow(Row row, Set<String> permissions, String processSysUserId, LocalDate date, Long processRecordInfo) {
        List<String> errorMessages = new ArrayList<>();
        if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
            if (!permissions.contains(PermissionEnum.CUSTOMER_RECEIVABLE_MI_CREATE.getId()))
                throw new ClientException("Not enough permission for creating Customer Receivable", ErrorCode.ACCESS_DENIED);
            CustomerReceivableRequest customerReceivableRequest = customerReceivableExcelMapper.toCustomerReceivableCreateRequest(new CustomerReceivableRequest(), row, errorMessages);
            validate(customerReceivableRequest, errorMessages, CustomerReceivablePostGroup.class);

            return String.valueOf(customerReceivableService.create(customerReceivableRequest, permissions));
        }
        ((XSSFCell) row.getCell(0)).setCellType(CellType.STRING);
        String identifier = row.getCell(0).getStringCellValue();
        CustomerReceivable customerReceivable = customerReceivableRepository.findByReceivableNumberAndStatus(identifier, EntityStatus.ACTIVE)
                .orElseThrow(() -> new ClientException("Not found Customer Receivable with ID" + identifier, ErrorCode.CONFLICT));
        if (!permissions.contains(PermissionEnum.CUSTOMER_RECEIVABLE_MI_EDIT.getId())) {
            throw new ClientException("Not enough permission for updating Customer Receivable", ErrorCode.ACCESS_DENIED);
        }

        CustomerReceivableRequest customerReceivableRequest = customerReceivableExcelMapper.toCustomerReceivableUpdateRequest(customerReceivable, row, errorMessages);
        validate(customerReceivableRequest, errorMessages, CustomerReceivablePutGroup.class);

        return String.valueOf(customerReceivableService.update(customerReceivable.getId(), customerReceivableRequest, permissions));
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
        return numberOfTasksPerThread;
    }

    @Override
    protected int getNumberOfRowsPerTask() {
        return numberOfRowsPerTask;
    }

    @Override
    public boolean supports(EventType eventType) {
        return EventType.CUSTOMER_RECEIVABLE_MASS_IMPORT_PROCESS.equals(eventType);
    }

    private void validate(CustomerReceivableRequest customerReceivableRequest, List<String> errorMessages, Class<?> group) {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<CustomerReceivableRequest>> violations = validator.validate(customerReceivableRequest, group);
        if (!violations.isEmpty() || !errorMessages.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String error : errorMessages) {
                stringBuilder.append(error);
            }
            for (ConstraintViolation<CustomerReceivableRequest> violation : violations) {
                stringBuilder.append(violation.getMessage());
            }
            throw new ClientException(stringBuilder.toString(), ErrorCode.CONFLICT);
        }
    }
}
