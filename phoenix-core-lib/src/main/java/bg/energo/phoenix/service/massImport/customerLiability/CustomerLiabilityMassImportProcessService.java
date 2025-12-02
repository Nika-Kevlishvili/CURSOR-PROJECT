package bg.energo.phoenix.service.massImport.customerLiability;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.customAnotations.receivable.customerLiability.CustomerLiabilityPostRequest;
import bg.energo.phoenix.model.customAnotations.receivable.customerLiability.CustomerLiabilityPutRequest;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.request.receivable.customerLiability.CustomerLiabilityRequest;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.service.massImport.AbstractMassImportProcessService;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityService;
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
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class CustomerLiabilityMassImportProcessService extends AbstractMassImportProcessService {

    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final CustomerLiabilityService customerLiabilityService;
    private final CustomerLiabilityExcelMapper customerLiabilityExcelMapper;
    @Value("${app.cfg.customerLiability.massImport.numberOfRowsPerTask}")
    private Integer numberOfRowsPerTsk;
    @Value("${app.cfg.customerLiability.massImport.numberOfTasksPerThread}")
    private Integer numberOfCallablesPerThread;
    @Value("${app.cfg.customerLiability.massImport.numberOfThreads}")
    private Integer numberOfThreads;

    public CustomerLiabilityMassImportProcessService(CustomerLiabilityRepository customerLiabilityRepository,
                                                     CustomerLiabilityService customerLiabilityService,
                                                     CustomerLiabilityExcelMapper customerLiabilityExcelMapper,
                                                     NotificationEventPublisher notificationEventPublisher,
                                       ProcessRepository processRepository, ProcessedRecordInfoRepository processedRecordInfoRepository) {
        super(processRepository, processedRecordInfoRepository, notificationEventPublisher);
        this.customerLiabilityRepository = customerLiabilityRepository;
        this.customerLiabilityService = customerLiabilityService;
        this.customerLiabilityExcelMapper = customerLiabilityExcelMapper;
    }

    @Override
    protected String processRow(Row row, Set<String> permissions, String processSysUserId, LocalDate date, Long processRecordInfo) {
        List<String> errorMessages = new ArrayList<>();
        if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
            if (!permissions.contains(PermissionEnum.CUSTOMER_LIABILITY_MI_CREATE.getId()))
                throw new ClientException("Not enough permission for creating Customer Liability", ErrorCode.ACCESS_DENIED);
            CustomerLiabilityRequest request = customerLiabilityExcelMapper.toCustomerLiabilityCreateRequest(new CustomerLiabilityRequest(), row, errorMessages);
            //Validate request
            validateRequest(errorMessages, request, CustomerLiabilityPostRequest.class);

            return String.valueOf(customerLiabilityService.create(request, permissions));
        } else {
            ((XSSFCell) row.getCell(0)).setCellType(CellType.STRING);
            String identifier = row.getCell(0).getStringCellValue();
            Optional<CustomerLiability> customerLiability = customerLiabilityRepository.findByLiabilityNumberAndStatus(identifier,  EntityStatus.ACTIVE);
            if (customerLiability.isPresent()){
                if (!permissions.contains(PermissionEnum.CUSTOMER_LIABILITY_MI_EDIT.getId()))
                    throw new ClientException("Not enough permission for updating Customer Liability", ErrorCode.ACCESS_DENIED);

                CustomerLiabilityRequest request = customerLiabilityExcelMapper.toCustomerLiabilityUpdateRequest(customerLiability.get(), row, errorMessages);

                //Validate request
                validateRequest(errorMessages, request, CustomerLiabilityPutRequest.class);

                return String.valueOf(customerLiabilityService.update(customerLiability.get().getId(), request, permissions));
            }else{
                throw new ClientException("Not found Customer Liability with liability number : " + identifier + ";", ErrorCode.CONFLICT);
            }
        }
    }

    protected void validateRequest(List<String> errorMessages, CustomerLiabilityRequest request, Class<?> group) {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<CustomerLiabilityRequest>> violations = validator.validate(request, group);
        if (!violations.isEmpty() || !errorMessages.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String error : errorMessages) {
                stringBuilder.append(error);
            }
            for (ConstraintViolation<CustomerLiabilityRequest> violation : violations) {
                stringBuilder.append(violation.getMessage());
            }
            throw new ClientException(stringBuilder.toString(), ErrorCode.CONFLICT);
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
        return EventType.CUSTOMER_LIABILITY_MASS_IMPORT_PROCESS.equals(eventType);
    }


}
