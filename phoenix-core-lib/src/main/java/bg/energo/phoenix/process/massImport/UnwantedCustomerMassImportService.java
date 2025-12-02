package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.process.model.enums.ProcessType;
import org.springframework.stereotype.Service;

import static bg.energo.phoenix.event.EventType.UNWANTED_CUSTOMER_MASS_IMPORT_PROCESS;
import static bg.energo.phoenix.process.model.enums.ProcessType.PROCESS_UNWANTED_CUSTOMER_MASS_IMPORT;

@Service
public class UnwantedCustomerMassImportService extends MassImportBaseService{

    private static final String FILE_UPLOAD_PATH = "/unwanted_customer_mass_import";

    @Override
    public DomainType getDomainType() {
        return DomainType.UNWANTED_CUSTOMERS;
    }

    @Override
    protected EventType getEventType() {
        return UNWANTED_CUSTOMER_MASS_IMPORT_PROCESS;
    }

    @Override
    protected ProcessType getProcessType() {
        return PROCESS_UNWANTED_CUSTOMER_MASS_IMPORT;
    }

    @Override
    protected String getFileUploadPath() {
        return FILE_UPLOAD_PATH;
    }

    @Override
    protected PermissionContextEnum getPermissionContext() {
        return PermissionContextEnum.UNWANTED_CUSTOMER_MI;
    }
}
