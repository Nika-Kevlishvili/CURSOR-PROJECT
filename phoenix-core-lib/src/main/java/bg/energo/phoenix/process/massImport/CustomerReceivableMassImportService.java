package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.process.model.enums.ProcessType;
import org.springframework.stereotype.Service;

import static bg.energo.phoenix.event.EventType.CUSTOMER_RECEIVABLE_MASS_IMPORT_PROCESS;
import static bg.energo.phoenix.process.model.enums.ProcessType.PROCESS_CUSTOMER_RECEIVABLE_MASS_IMPORT;

@Service
public class CustomerReceivableMassImportService extends MassImportBaseService{
    private final String FILE_UPLOAD_PATH = "/customer_receivable_mass_import";
    @Override
    public DomainType getDomainType() {
        return DomainType.CUSTOMER_RECEIVABLE;
    }

    @Override
    protected EventType getEventType() {
        return CUSTOMER_RECEIVABLE_MASS_IMPORT_PROCESS;
    }

    @Override
    protected ProcessType getProcessType() {
        return PROCESS_CUSTOMER_RECEIVABLE_MASS_IMPORT;
    }

    @Override
    protected String getFileUploadPath() {
        return FILE_UPLOAD_PATH;
    }

    @Override
    protected PermissionContextEnum getPermissionContext() {
        return PermissionContextEnum.CUSTOMER_RECEIVABLE_MI;
    }
}
