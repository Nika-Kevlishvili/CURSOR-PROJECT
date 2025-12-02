package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.process.model.enums.ProcessType;
import org.springframework.stereotype.Service;

@Service
public class SupplyActionDeactivationMassImportService extends MassImportBaseService{
    private final String FILE_UPLOAD_PATH = "/supply_action_deactivation_mass_import";
    @Override
    public DomainType getDomainType() {
        return DomainType.SUPPLY_ACTION_DEACTIVATIONS;
    }

    @Override
    protected EventType getEventType() {
        return EventType.SUPPLY_ACTION_DEACTIVATION_MASS_IMPORT_PROCESS;
    }

    @Override
    protected ProcessType getProcessType() {
        return ProcessType.PROCESS_SUPPLY_ACTION_DEACTIVATION_MASS_IMPORT;
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
