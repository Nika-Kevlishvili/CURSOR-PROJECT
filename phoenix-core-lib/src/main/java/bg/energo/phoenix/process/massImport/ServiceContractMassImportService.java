package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.process.model.enums.ProcessType;
import org.springframework.stereotype.Service;

@Service
public class ServiceContractMassImportService  extends MassImportBaseService{

    private static final String FILE_UPLOAD_PATH = "/service_contract_mass_import";
    @Override
    public DomainType getDomainType() {
        return DomainType.SERVICE_CONTRACT;
    }

    @Override
    protected EventType getEventType() {
        return EventType.SERVICE_CONTRACT_MASS_IMPORT_PROCESS;
    }

    @Override
    protected ProcessType getProcessType() {
        return ProcessType.PROCESS_SERVICE_CONTRACT_MASS_IMPORT;
    }

    @Override
    protected String getFileUploadPath() {
        return FILE_UPLOAD_PATH;
    }

    @Override
    protected PermissionContextEnum getPermissionContext() {
        return PermissionContextEnum.SERVICE_CONTRACT_MI;
    }
}
