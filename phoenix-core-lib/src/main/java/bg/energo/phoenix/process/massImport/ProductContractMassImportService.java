package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.process.model.enums.ProcessType;
import org.springframework.stereotype.Service;

@Service
public class ProductContractMassImportService extends MassImportBaseService {
    private final String FILE_UPLOAD_PATH = "/product_contract_mass_import";

    @Override
    public DomainType getDomainType() {
        return DomainType.PRODUCT_CONTRACTS;
    }

    @Override
    protected EventType getEventType() {
        return EventType.PRODUCT_CONTRACT_MASS_IMPORT_PROCESS;
    }

    @Override
    protected ProcessType getProcessType() {
        return ProcessType.PRODUCT_CONTRACT_MASS_IMPORT;
    }

    @Override
    protected String getFileUploadPath() {
        return FILE_UPLOAD_PATH;
    }

    @Override
    protected PermissionContextEnum getPermissionContext() {
        return PermissionContextEnum.PRODUCT_CONTRACTS_MI;
    }
}
