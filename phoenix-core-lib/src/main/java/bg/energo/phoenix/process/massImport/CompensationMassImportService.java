package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.process.model.enums.ProcessType;
import org.springframework.stereotype.Service;

@Service
public class CompensationMassImportService extends MassImportBaseService {

    private final String FILE_UPLOAD_PATH = "/government_compensation_mass_import";
    @Override
    public DomainType getDomainType() {
        return DomainType.GOVERNMENT_COMPENSATION;
    }

    @Override
    protected EventType getEventType() {
        return EventType.GOVERNMENT_COMPENSATION_MASS_IMPORT_PROCESS;
    }

    @Override
    protected ProcessType getProcessType() {
        return ProcessType.GOVERNMENT_COMPENSATION_MASS_IMPORT;
    }

    @Override
    protected String getFileUploadPath() {
        return FILE_UPLOAD_PATH;
    }

    @Override
    protected PermissionContextEnum getPermissionContext() {
        return PermissionContextEnum.GOVERNMENT_COMPENSATION_MI;
    }
}
