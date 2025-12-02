package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.process.model.enums.ProcessType;
import org.springframework.stereotype.Service;

@Service
public class PodMassImportService extends MassImportBaseService{
    private final String FILE_UPLOAD_PATH = "/pod_mass_import";
    @Override
    public DomainType getDomainType() {
        return DomainType.PODS;
    }

    @Override
    protected EventType getEventType() {
        return EventType.POD_MASS_IMPORT_PROCESS;
    }

    @Override
    protected ProcessType getProcessType() {
        return ProcessType.PROCESS_POD_MASS_IMPORT;
    }

    @Override
    protected String getFileUploadPath() {
        return this.FILE_UPLOAD_PATH;
    }

    @Override
    protected PermissionContextEnum getPermissionContext() {
        return PermissionContextEnum.POD_MI;
    }
}
