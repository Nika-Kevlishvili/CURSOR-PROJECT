package bg.energo.phoenix.service.massImport;


import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerAccountManager;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.request.customer.CreateCustomerRequest;
import bg.energo.phoenix.model.request.customer.EditCustomerRequest;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerAccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.service.customer.CustomerService;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
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
import java.util.stream.Collectors;

@Service
@Slf4j
public class CustomerMassImportProcessService extends AbstractMassImportProcessService {
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final ExcelMapper excelMapper;
    private final DatabaseMapper databaseMapper;
    private final CustomerAccountManagerRepository customerAccountManagerRepository;
    private final AccountManagerRepository accountManagerRepository;

    public CustomerMassImportProcessService(
            ProcessRepository processRepository,
            ProcessedRecordInfoRepository processRecordInfoRepository,
            CustomerRepository customerRepository,
            CustomerService customerService,
            ExcelMapper excelMapper,
            DatabaseMapper databaseMapper,
            CustomerAccountManagerRepository customerAccountManagerRepository,
            AccountManagerRepository accountManagerRepository,
            NotificationEventPublisher notificationEventPublisher
    ) {
        super(processRepository, processRecordInfoRepository, notificationEventPublisher);
        this.customerRepository = customerRepository;
        this.customerService = customerService;
        this.excelMapper = excelMapper;
        this.databaseMapper = databaseMapper;
        this.customerAccountManagerRepository = customerAccountManagerRepository;
        this.accountManagerRepository = accountManagerRepository;
    }

    @Value("${app.cfg.customer.massImport.numberOfRowsPerTask}")
    private Integer numberOfRowsPerTsk;
    @Value("${app.cfg.customer.massImport.numberOfTasksPerThread}")
    private Integer numberOfCallablesPerThread;
    @Value("${app.cfg.customer.massImport.numberOfThreads}")
    private Integer numberOfThreads;

    @Override
    protected String getIdentifier(Row row) {
        int columnNumber = 7;
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.STRING);
            return row.getCell(columnNumber).getStringCellValue();
        }
        return null;
    }

    @Override
    protected String processRow(
            Row row,
            Set<String> permissions,
            String processSysUserId,
            LocalDate date,
            Long processRecordInfo
    ) {
        List<String> errorMessages = new ArrayList<>();
        if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
            if (!permissions.contains(PermissionEnum.MI_CREATE.getId())) {
                throw new ClientException("Not enough permission for creating user", ErrorCode.ACCESS_DENIED);
            }
            CreateCustomerRequest createCustomerRequest = excelMapper.convertToCreateCustomerRequest(
                    new CreateCustomerRequest(),
                    row,
                    errorMessages
            );
            //Validate request
            Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
            Set<ConstraintViolation<CreateCustomerRequest>> violations = validator.validate(createCustomerRequest);
            validateRequest(errorMessages, createCustomerRequest);

            return String.valueOf(customerService.create(createCustomerRequest, permissions).getLastCustomerDetailId());
        } else {
            ((XSSFCell) row.getCell(0)).setCellType(CellType.STRING);
            Long customerNumber = Long.valueOf(row.getCell(0).getStringCellValue());
            Optional<Customer> customer = customerRepository.getCustomerByStatusAndCustomerNumber(
                    CustomerStatus.ACTIVE,
                    customerNumber
            );
            if (customer.isPresent()) {
                List<CustomerAccountManager> customerAccountManagers = customerAccountManagerRepository.getByCustomerDetailsIdAndStatus(
                        customer.get().getLastCustomerDetailId(),
                        Status.ACTIVE
                );

                List<String> accountManagerUserNames = new ArrayList<>();

                for (CustomerAccountManager customerAccountManager : customerAccountManagers) {
                    Optional<AccountManager> optionalAccountManager = accountManagerRepository.findByIdAndStatus(
                            customerAccountManager.getManagerId(),
                            List.of(Status.ACTIVE)
                    );
                    optionalAccountManager.ifPresent(accountManager -> accountManagerUserNames.add(accountManager.getUserName()));
                }

                if ((!permissions.contains(PermissionEnum.MI_EDIT.getId()) && (!permissions.contains(PermissionEnum.MI_EDIT_AM.getId()) ||
                        !accountManagerUserNames.contains(processSysUserId)))) {
                    throw new ClientException("Not enough permission for updating user", ErrorCode.ACCESS_DENIED);
                }

                EditCustomerRequest editCustomerRequest = excelMapper.convertToEditCustomerRequest(
                        databaseMapper.convertToEditCustomerRequest(
                                customer.get(),
                                row,
                                errorMessages
                        ),
                        row,
                        errorMessages
                );

                if (editCustomerRequest.getSegmentIds() != null) {
                    editCustomerRequest.setSegmentIds(
                            editCustomerRequest
                                    .getSegmentIds()
                                    .stream()
                                    .distinct()
                                    .collect(Collectors.toList())
                    );
                }
                if (editCustomerRequest.getBankingDetails() != null && editCustomerRequest.getBankingDetails().getPreferenceIds() != null) {
                    editCustomerRequest.getBankingDetails().setPreferenceIds(
                            editCustomerRequest
                                    .getBankingDetails()
                                    .getPreferenceIds().stream()
                                    .distinct()
                                    .collect(Collectors.toList())
                    );
                }
                //Validate request
                validateRequest(errorMessages, editCustomerRequest);

                return String.valueOf(customerService.update(customer.get().getId(), editCustomerRequest, permissions).getLastCustomerDetailId());
            } else {
                throw new ClientException(
                        "Not found customer with customer number: " + customerNumber + ";",
                        ErrorCode.CONFLICT
                );
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
        return EventType.CUSTOMER_MASS_IMPORT_PROCESS.equals(eventType);
    }

}
