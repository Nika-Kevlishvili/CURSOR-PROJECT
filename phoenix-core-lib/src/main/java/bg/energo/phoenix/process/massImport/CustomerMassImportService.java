package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.process.model.enums.ProcessType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

import static bg.energo.phoenix.event.EventType.CUSTOMER_MASS_IMPORT_PROCESS;
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER_MI;
import static bg.energo.phoenix.permissions.PermissionEnum.*;
import static bg.energo.phoenix.process.model.enums.ProcessType.PROCESS_CUSTOMER_MASS_IMPORT;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerMassImportService extends MassImportBaseService {

    private static final String FILE_UPLOAD_PATH = "/customer_mass_import";

    @Override
    public DomainType getDomainType() {
        return DomainType.CUSTOMERS;
    }

    @Override
    protected EventType getEventType() {
        return CUSTOMER_MASS_IMPORT_PROCESS;
    }

    @Override
    protected ProcessType getProcessType() {
        return PROCESS_CUSTOMER_MASS_IMPORT;
    }

    @Override
    protected String getFileUploadPath() {
        return FILE_UPLOAD_PATH;
    }

    @Override
    protected PermissionContextEnum getPermissionContext() {
        return PermissionContextEnum.CUSTOMER_MI;
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = CUSTOMER_MI,
                            permissions = {
                                    MI_CREATE,
                                    MI_EDIT,
                                    MI_EDIT_AM
                            })
            }
    )
    public void uploadMassImportFile(MultipartFile file, LocalDate date,Long collectionChannelId,Boolean currencyFromCollectionChannel) {
        super.uploadMassImportFile(file, date,collectionChannelId,false);
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = CUSTOMER_MI,
                            permissions = {
                                    MI_CREATE,
                                    MI_EDIT,
                                    MI_EDIT_AM
                            })
            }
    )
    public byte[] getMassImportTemplate() {
        return super.getMassImportTemplate();
    }
}
