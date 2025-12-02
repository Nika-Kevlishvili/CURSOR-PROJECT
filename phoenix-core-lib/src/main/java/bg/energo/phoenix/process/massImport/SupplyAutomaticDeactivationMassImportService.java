package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.process.model.enums.ProcessType;
import org.springframework.stereotype.Service;

@Service
public class SupplyAutomaticDeactivationMassImportService extends MassImportBaseService{
    private final String FILE_UPLOAD_PATH = "/supply_automatic_deactivation_mass_import";
    @Override
    public DomainType getDomainType() {
        return DomainType.SUPPLY_AUTOMATIC_DEACTIVATIONS;
    }

    @Override
    protected EventType getEventType() {
        return EventType.SUPPLY_AUTOMATIC_DEACTIVATION_MASS_IMPORT_PROCESS;
    }

    @Override
    protected ProcessType getProcessType() {
        return ProcessType.PROCESS_SUPPLY_AUTOMATIC_DEACTIVATION_MASS_IMPORT;
    }

    @Override
    protected String getFileUploadPath() {
        return this.FILE_UPLOAD_PATH;
    }

    @Override
    protected PermissionContextEnum getPermissionContext() {
        return PermissionContextEnum.SUPPLY_AUTOMATIC_ACTIVATION_DEACTIVATION_MI;
    }
}
