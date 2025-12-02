package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.process.model.enums.ProcessType;
import org.springframework.stereotype.Service;

import static bg.energo.phoenix.event.EventType.CUSTOMER_LIABILITY_MASS_IMPORT_PROCESS;
import static bg.energo.phoenix.process.model.enums.ProcessType.PROCESS_CUSTOMER_LIABILITY_MASS_IMPORT;

@Service
public class CustomerLiabilityMassImportService extends MassImportBaseService{
    private final String FILE_UPLOAD_PATH = "/customer_liability_mass_import";
    @Override
    public DomainType getDomainType() {
        return DomainType.CUSTOMER_LIABILITY;
    }

    @Override
    protected EventType getEventType() {
        return CUSTOMER_LIABILITY_MASS_IMPORT_PROCESS;
    }

    @Override
    protected ProcessType getProcessType() {
        return PROCESS_CUSTOMER_LIABILITY_MASS_IMPORT;
    }

    @Override
    protected String getFileUploadPath() {
        return FILE_UPLOAD_PATH;
    }

    @Override
    protected PermissionContextEnum getPermissionContext() {
        return PermissionContextEnum.CUSTOMER_LIABILITY_MI;
    }
}
